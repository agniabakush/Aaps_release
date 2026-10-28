#!/usr/bin/env python3
"""Does UKF smoothing fix the primer trigger?

The affected user runs NoSmoothingPlugin, so glucose_status.delta carries raw
Dexcom G6 jitter straight into delta_accl - whose denominator floors at 2.0, so
the primer's gate reduces to (delta - shortAvgDelta) > 0.2 mg/dL on a flat trace.

This is a SENSING question, not a policy one: smoothing transforms the signal the
trigger reads, so we can compute the counterfactual signal exactly. No dosing
counterfactual is required or claimed.

Method
------
1. Take the user's raw CGM.
2. Run the v4 adaptive UKF (`smoothers.V4UKF` via smooth_series("v4", ...)), the
   same Python port benchmarked against the shipped Kotlin plugin - constants
   verified identical (rInit 25.0 / rMin 16.0 / rMax 225.0).
3. Recompute delta / shortAvgDelta / longAvgDelta from BOTH raw and smoothed using
   DeltaCalculator's exact windows (last 2.5-7.5, short 2.5-17.5, long 17.5-42.5
   min, avgDel = change/minutesAgo*5).
4. Recompute delta_accl and re-evaluate the primer gate and both scaling functions.

Replication is validated by comparing raw-recomputed delta/shortAvgDelta against
the values AAPS itself logged in the reason string.
"""
import json, os, sys, re

HERE = os.path.dirname(os.path.abspath(__file__))

MIN_LAST, MAX_LAST = 2.5, 7.5
MIN_SHORT, MAX_SHORT = 2.5, 17.5
MIN_LONG, MAX_LONG = 17.5, 42.5


def deltas_at(series, idx):
    """series = chronological [(ts_ms, value)]; idx = 'now'. Mirrors DeltaCalculator."""
    now_t, now_v = series[idx]
    last, short, long_ = [], [], []
    for j in range(idx - 1, -1, -1):
        t, v = series[j]
        mins = (now_t - t) / 60000.0
        if mins > MAX_LONG:
            break
        avg = (now_v - v) / mins * 5 if mins > 0 else 0.0
        if MIN_LAST <= mins <= MAX_LAST:
            last.append(avg)
        if MIN_SHORT <= mins <= MAX_SHORT:
            short.append(avg)
        if MIN_LONG <= mins <= MAX_LONG:
            long_.append(avg)
    avg_or0 = lambda a: sum(a) / len(a) if a else 0.0
    sh = avg_or0(short)
    return (avg_or0(last) if last else sh), sh, avg_or0(long_)


def delta_accl(delta, short):
    if abs(short) <= 0.001:
        return 0.0
    return round(100.0 * (delta - short) / max(abs(short), 2.0), 2)


def clamp(x, lo=0.0, hi=1.0):
    return max(lo, min(hi, x))


def shipped_primer(cap, accl):
    return min(cap * (1.0 + max(0.0, (accl - 10.0) / 20.0)), 2.0 * cap)


def proposed_primer(cap, delta, accl, bg, iob, iob_full=9.0):
    f_rise = clamp((delta - 1.5) / 6.5) * (1.0 if accl > 10.0 else 0.0)
    f_bg = clamp((bg - 90.0) / 20.0) * clamp((220.0 - bg) / 40.0)
    f_iob = clamp(1.0 - iob / iob_full)
    return cap * f_rise * f_bg * f_iob


def main():
    scratch = sys.argv[1]
    sys.path.insert(0, os.path.join(scratch, 'ukfpkg', 'repeatable'))
    from smoothers import smooth_series

    sg = json.load(open(os.path.join(scratch, "dd_sgv.json")))
    ds = json.load(open(os.path.join(scratch, "dd_ds.json")))
    raw = sorted([(x["date"], float(x["sgv"])) for x in sg])
    ts = [t for t, _ in raw]
    vals = [v for _, v in raw]

    out = smooth_series("v4", ts, vals)
    lvl = out["level_online"]
    sm = [(ts[i], lvl[i] if lvl[i] == lvl[i] else vals[i]) for i in range(len(ts))]

    # index cycles by minute for lookup
    cyc = {}
    for d in ds:
        s = d.get("openaps", {}).get("suggested")
        if s and s.get("timestamp"):
            cyc[str(s["timestamp"])[:16]] = s

    def nearest(t_ms):
        best, bi = None, None
        for i, (t, _) in enumerate(raw):
            dt = abs(t - t_ms)
            if best is None or dt < best:
                best, bi = dt, i
        return bi

    import datetime as dt
    CAP = 0.6

    def evaluate(label, keys):
        print(f"\n{label}")
        print(f"{'time':7s} {'bg':>4s} | {'RAW':^26s} | {'UKF-SMOOTHED':^26s}")
        print(f"{'':7s} {'':4s} | {'delta':>6s} {'short':>6s} {'accl':>6s} {'fire':>4s} | "
              f"{'delta':>6s} {'short':>6s} {'accl':>6s} {'fire':>4s}")
        agg = dict(raw_ship=0.0, raw_prop=0.0, ukf_ship=0.0, ukf_prop=0.0, n=0, raw_fire=0, ukf_fire=0)
        for k in keys:
            s = cyc.get(k)
            if not s:
                continue
            t = dt.datetime.fromisoformat(str(s["timestamp"]).replace("Z", "+00:00"))
            i = nearest(int(t.timestamp() * 1000))
            if i is None or i < 9:
                continue
            bg = s.get("bg") or raw[i][1]
            iob = s.get("IOB") or 0.0
            dr, sr, _ = deltas_at(raw, i)
            du, su, _ = deltas_at(sm, i)
            ar, au = delta_accl(dr, sr), delta_accl(du, su)
            fr = ar > 10.0 and dr > 0.0
            fu = au > 10.0 and du > 0.0
            agg["n"] += 1
            agg["raw_fire"] += int(fr); agg["ukf_fire"] += int(fu)
            if fr:
                agg["raw_ship"] += shipped_primer(CAP, ar)
                agg["raw_prop"] += proposed_primer(CAP, dr, ar, bg, iob)
            if fu:
                agg["ukf_ship"] += shipped_primer(CAP, au)
                agg["ukf_prop"] += proposed_primer(CAP, du, au, bg, iob)
            print(f"{k[11:16]:7s} {bg:4.0f} | {dr:6.2f} {sr:6.2f} {ar:6.2f} {'YES' if fr else '-':>4s} | "
                  f"{du:6.2f} {su:6.2f} {au:6.2f} {'YES' if fu else '-':>4s}")
        print(f"  fires: raw {agg['raw_fire']}/{agg['n']}   UKF {agg['ukf_fire']}/{agg['n']}")
        print(f"  shipped scaling : raw {agg['raw_ship']:.2f}U   UKF {agg['ukf_ship']:.2f}U")
        print(f"  proposed scaling: raw {agg['raw_prop']:.3f}U  UKF {agg['ukf_prop']:.3f}U")

    fires = [k for k, s in sorted(cyc.items()) if "primer=bolus" in s.get("reason", "")]
    evaluate("THE SIX PRIMER FIRES (all false except 19:44)", fires)
    onset = [k for k in sorted(cyc) if "2026-07-28T18:0" <= k <= "2026-07-28T18:50"]
    evaluate("REAL MEAL ONSET 28 Jul (CONFIRMED at 18:34)", onset)

    # replication check
    print("\nREPLICATION CHECK - recomputed RAW vs what AAPS logged")
    ok = tot = 0
    for k, s in sorted(cyc.items()):
        r = s.get("reason", "")
        m = re.search(r"Delta: (-?[0-9.]+)", r)
        if not m:
            continue
        t = dt.datetime.fromisoformat(str(s["timestamp"]).replace("Z", "+00:00"))
        i = nearest(int(t.timestamp() * 1000))
        if i is None or i < 9:
            continue
        dr, _, _ = deltas_at(raw, i)
        tot += 1
        if abs(dr - float(m.group(1))) <= 1.0:
            ok += 1
    print(f"  delta within 1.0 mg/dL of logged on {ok}/{tot} cycles ({100*ok/max(tot,1):.0f}%)")


if __name__ == "__main__":
    main()
