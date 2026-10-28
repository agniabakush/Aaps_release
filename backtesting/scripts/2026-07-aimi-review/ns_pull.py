#!/usr/bin/env python3
"""Pull Nightscout entries/treatments/devicestatus for one site tag into local JSON.

Site base URL + token are read at RUNTIME from ~/.config/boost_backtest/sites.json.
Nothing identifying is ever written to stdout or to the output files' names.
Usage: ns_pull.py <TAG> <days_back> <outdir>
"""
import json, os, sys, time, urllib.request, urllib.error, urllib.parse
from datetime import datetime, timedelta, timezone

REG = os.path.expanduser("~/.config/boost_backtest/sites.json")
# Some sites sit behind Cloudflare with a bot-signature rule that 403s the default
# Python-urllib User-Agent (CF error 1010). A normal browser UA passes.
UA = ("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")


def site(tag):
    for s in json.load(open(REG))["sites"]:
        if s["tag"] == tag:
            return s
    raise SystemExit("tag not found")


def get(base, token, path, params, tries=5):
    q = dict(params)
    if token:
        q["token"] = token
    url = f"{base.rstrip('/')}/api/v1/{path}?" + urllib.parse.urlencode(q)
    for i in range(tries):
        try:
            req = urllib.request.Request(url, headers={"Accept": "application/json", "User-Agent": UA})
            with urllib.request.urlopen(req, timeout=120) as r:
                return json.loads(r.read().decode())
        except Exception as e:
            code = getattr(e, "code", None)
            print(f"  retry {i+1}/{tries} on {path} (err {code or type(e).__name__})", file=sys.stderr)
            time.sleep(2 ** i)
    return []


def windows(days_back, span=7):
    end = datetime.now(timezone.utc)
    start = end - timedelta(days=days_back)
    cur = start
    while cur < end:
        nxt = min(cur + timedelta(days=span), end)
        yield cur, nxt
        cur = nxt


def main():
    tag, days_back, outdir = sys.argv[1], int(sys.argv[2]), sys.argv[3]
    s = site(tag)
    base, token = s["base"], s.get("token", "")
    os.makedirs(outdir, exist_ok=True)
    for coll, path, tsfield in (
        ("entries", "entries/sgv.json", "dateString"),
        ("treatments", "treatments.json", "created_at"),
        ("devicestatus", "devicestatus.json", "created_at"),
    ):
        allrows, seen = [], set()
        for a, b in windows(days_back):
            rows = get(base, token, path, {
                f"find[{tsfield}][$gte]": a.strftime("%Y-%m-%dT%H:%M:%S.000Z"),
                f"find[{tsfield}][$lt]": b.strftime("%Y-%m-%dT%H:%M:%S.000Z"),
                "count": 100000,
            })
            for r in rows:
                k = r.get("_id") or json.dumps(r, sort_keys=True)
                if k not in seen:
                    seen.add(k)
                    allrows.append(r)
            print(f"{coll} {a:%Y-%m-%d}..{b:%Y-%m-%d}: {len(rows)}", file=sys.stderr)
            time.sleep(0.5)
        out = os.path.join(outdir, f"{coll}_{tag}.json")
        json.dump(allrows, open(out, "w"))
        print(f"{coll}: {len(allrows)} rows -> {out}")


if __name__ == "__main__":
    main()
