#!/usr/bin/env python3
"""Live primer behaviour across the three users who have it: self, B, H.

Reads site base+token from the PRIVATE registry (~/.config/boost_backtest/sites.json)
at runtime - never hardcoded, never committed. Pulls Nightscout devicestatus
directly because the local DB lags by a couple of days and the primer went live
inside that lag for at least one user.

Reports, per user: smoothing plugin in force, primer mode (bolus vs retractable
TBR), fire count, and the delta / shortAvgDelta / delta_accl at each fire - i.e.
whether the fire was on a genuine rise or on jitter.
"""
import json, os, re, sys, urllib.request, urllib.parse, collections

REG = os.path.expanduser("~/.config/boost_backtest/sites.json")
WANT = {"self": "tim", "B": "user B", "H": "user H"}
COUNT = 500


def fetch(base, token, path, count):
    url = f"{base.rstrip('/')}/api/v1/{path}?" + urllib.parse.urlencode(
        {"token": token, "count": count})
    req = urllib.request.Request(url, headers={"User-Agent": "boost-backtest"})
    with urllib.request.urlopen(req, timeout=180) as r:
        return json.loads(r.read().decode())


def num(pat, s):
    m = re.search(pat, s)
    return float(m.group(1)) if m else None


def main():
    sites = {s["tag"]: s for s in json.load(open(REG))["sites"]}
    for tag, label in WANT.items():
        site = sites.get(tag)
        print("=" * 74)
        if not site:
            print(f"{label} ({tag}): NOT IN REGISTRY")
            continue
        try:
            ds = fetch(site["base"], site["token"], "devicestatus.json", COUNT)
        except Exception as e:
            print(f"{label} ({tag}): fetch failed -> {type(e).__name__}: {e}")
            continue

        cyc = {}
        smoothing = collections.Counter()
        ver = collections.Counter()
        for d in ds:
            cfg = d.get("configuration") or {}
            if cfg.get("smoothing"):
                smoothing[cfg["smoothing"]] += 1
                ver[cfg.get("version")] += 1
            s = d.get("openaps", {}).get("suggested")
            if s and s.get("timestamp"):
                cyc[str(s["timestamp"])[:16]] = s

        if not cyc:
            print(f"{label} ({tag}): no suggested payloads in {len(ds)} records")
            continue
        ks = sorted(cyc)
        print(f"{label} ({tag})   cycles={len(cyc)}   {ks[0][:16]} .. {ks[-1][:16]}")
        print(f"  smoothing: {dict(smoothing) or 'not reported in window'}   AAPS {dict(ver)}")

        bolus, tbr, other = [], [], []
        for k in ks:
            s = cyc[k]
            r = s.get("reason", "")
            if "primer=" not in r:
                continue
            m = re.search(r"primer=bolus,([0-9.]+)U", r)
            t = re.search(r"primer=tbr,([0-9.]+)U", r)
            rec = dict(t=k, bg=s.get("bg"), iob=s.get("IOB"), units=s.get("units") or 0,
                       accl=s.get("deltaAcceleration"),
                       delta=num(r"Delta: (-?[0-9.]+)", r),
                       short=num(r"ShortAvg: (-?[0-9.]+)", r),
                       state=s.get("boostV5_state"),
                       amt=float(m.group(1)) if m else (float(t.group(1)) if t else 0.0),
                       tag=re.search(r"primer=([a-z-]+)", r).group(1))
            (bolus if m else (tbr if t else other)).append(rec)

        allf = bolus + tbr + other
        print(f"  primer mentions: bolus={len(bolus)}  tbr={len(tbr)}  other/skipped={len(other)}")
        if not allf:
            print("  -> primer never fired in this window")
            continue
        print(f"  {'time':12s} {'mode':6s} {'U':>5s} {'bg':>4s} {'delta':>6s} {'short':>6s} "
              f"{'accl':>7s} {'IOB':>5s} {'state':10s} jitter?")
        for f in allf:
            d, sh = f["delta"], f["short"]
            jit = "JITTER (d<=2)" if (d is not None and d <= 2.0) else ""
            print(f"  {f['t'][5:16]:12s} {f['tag'][:6]:6s} {f['amt']:5.2f} {f['bg'] or 0:4.0f} "
                  f"{d if d is not None else -99:6.2f} {sh if sh is not None else -99:6.2f} "
                  f"{f['accl'] if f['accl'] is not None else -99:7.2f} {f['iob'] or 0:5.2f} "
                  f"{str(f['state'])[:10]:10s} {jit}")
        ds_ = [f["delta"] for f in allf if f["delta"] is not None]
        if ds_:
            print(f"  delta at fire: median {sorted(ds_)[len(ds_)//2]:.2f}   "
                  f"<=2 mg/dL on {sum(1 for x in ds_ if x <= 2)}/{len(ds_)}")
            print(f"  total primer insulin in window: {sum(f['amt'] for f in allf):.2f}U")


if __name__ == "__main__":
    main()
