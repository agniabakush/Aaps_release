#!/usr/bin/env python3
"""User H: glycaemic outcomes on AIMI (from 2026-07-25) vs their Boost era.

Boost-era CGM comes from the local TimescaleDB (boost_cgm, user_id='H').
AIMI-era CGM comes from a Nightscout pull (entries_H.json produced by ns_pull.py),
because the DB extractor stops at the fork switch.

Units: everything internal is mg/dL (the user's display is mmol/L; conversions
shown in the report only). Day boundaries are local days at the site's UTC offset,
read at runtime from the private registry.

Uncertainty: day-level (block) bootstrap. Days are the resampling unit because
5-minute CGM samples are heavily autocorrelated and would give absurdly narrow
intervals. The AIMI era is only a handful of days, so intervals are wide by
construction — that is the point.

Usage: aimi_outcomes.py <dir-with-entries_H.json>
"""
import json, os, subprocess, sys
from datetime import datetime, timedelta, timezone

import numpy as np

REG = os.path.expanduser("~/.config/boost_backtest/sites.json")
TAG = "H"
DSN = "dbname=oref host=127.0.0.1 port=5432"

# The fork switch: first AIMI-versioned devicestatus was 2026-07-25T13:59Z.
# 2026-07-23..25 is the disrupted transition (two confirm-crash incidents, loop
# disabled, build swaps), so it is excluded from both eras.
BOOST_END = "2026-07-23"      # last local day counted as clean Boost
AIMI_START = "2026-07-26"     # first full local day on AIMI
TRANSITION = ("2026-07-24", "2026-07-25")


def tz_offset():
    for s in json.load(open(REG))["sites"]:
        if s["tag"] == TAG:
            return int(s.get("tz_offset_hours") or 0)
    raise SystemExit("tag not found")


def local_day(dt_utc, off):
    return (dt_utc + timedelta(hours=off)).strftime("%Y-%m-%d")


def db_cgm():
    q = ("select ts_utc, cgm_mgdl from boost_cgm where user_id='H' "
         "and cgm_mgdl is not null order by 1")
    out = subprocess.run(["psql", DSN, "-At", "-F", "\t", "-c", q],
                         capture_output=True, text=True, check=True).stdout
    rows = []
    for line in out.strip().split("\n"):
        ts, v = line.split("\t")
        rows.append((datetime.fromisoformat(ts).astimezone(timezone.utc), float(v)))
    return rows


def ns_cgm(path):
    rows = []
    for e in json.load(open(path)):
        v = e.get("sgv")
        if v is None or v < 20 or v > 500:
            continue
        dt = datetime.fromtimestamp(e["date"] / 1000.0, tz=timezone.utc)
        rows.append((dt, float(v)))
    rows.sort()
    return rows


def metrics(vals):
    v = np.asarray(vals, dtype=float)
    if len(v) == 0:
        return {}
    return {
        "n": len(v),
        "mean": v.mean(),
        "sd": v.std(ddof=1) if len(v) > 1 else 0.0,
        "cv": 100.0 * v.std(ddof=1) / v.mean() if len(v) > 1 else 0.0,
        "tir_70_180": 100.0 * np.mean((v >= 70) & (v <= 180)),
        "ting_63_140": 100.0 * np.mean((v >= 63) & (v <= 140)),
        "tbr_70": 100.0 * np.mean(v < 70),
        "tbr_54": 100.0 * np.mean(v < 54),
        "tar_180": 100.0 * np.mean(v > 180),
        "tar_250": 100.0 * np.mean(v > 250),
    }


KEYS = ["tir_70_180", "ting_63_140", "tbr_70", "tbr_54", "tar_180", "tar_250",
        "mean", "cv"]


def pooled(days, keep):
    vals = []
    for d in keep:
        vals.extend(days.get(d, []))
    return metrics(vals)


def boot_diff(days_a, keys_a, days_b, keys_b, iters=20000, seed=7):
    """Bootstrap the A-minus-B difference, resampling whole local days."""
    rng = np.random.default_rng(seed)
    ka, kb = np.array(keys_a), np.array(keys_b)
    out = {k: [] for k in KEYS}
    for _ in range(iters):
        sa = rng.choice(ka, size=len(ka), replace=True)
        sb = rng.choice(kb, size=len(kb), replace=True)
        va, vb = [], []
        for d in sa:
            va.extend(days_a[d])
        for d in sb:
            vb.extend(days_b[d])
        ma, mb = metrics(va), metrics(vb)
        for k in KEYS:
            out[k].append(ma[k] - mb[k])
    return {k: (float(np.percentile(v, 2.5)), float(np.percentile(v, 97.5)))
            for k, v in out.items()}


def main():
    d = sys.argv[1] if len(sys.argv) > 1 else "."
    off = tz_offset()

    # --- assemble per-local-day CGM series from both sources -----------------
    db_days, ns_days = {}, {}
    for dt, v in db_cgm():
        db_days.setdefault(local_day(dt, off), []).append(v)
    for dt, v in ns_cgm(os.path.join(d, f"entries_{TAG}.json")):
        ns_days.setdefault(local_day(dt, off), []).append(v)

    # --- source agreement on the overlap (sanity check, not a finding) -------
    ov = sorted(set(db_days) & set(ns_days))
    print("== source agreement, DB vs Nightscout, overlapping local days ==")
    print(f"overlap days: {len(ov)} ({ov[0]}..{ov[-1]})" if ov else "no overlap")
    for src, dd in (("DB", db_days), ("NS", ns_days)):
        m = pooled(dd, ov)
        print(f"  {src}: n={m['n']} mean={m['mean']:.1f} TIR={m['tir_70_180']:.1f}% "
              f"TBR70={m['tbr_70']:.2f}% CV={m['cv']:.1f}%")
    print()

    # --- era membership ------------------------------------------------------
    # Boost era: every clean Boost local day present in the DB up to BOOST_END.
    boost_all = sorted(x for x in db_days if x <= BOOST_END)
    # 28-day pre-switch window, the like-for-like recency-matched baseline
    cutoff = (datetime.strptime(BOOST_END, "%Y-%m-%d") - timedelta(days=27)).strftime("%Y-%m-%d")
    boost_28 = [x for x in boost_all if x >= cutoff]
    # AIMI era: full local days on the AIMI build, from NS. Drop any partial
    # trailing day (< 80% of the 288 possible 5-min samples).
    aimi = sorted(x for x in ns_days if x >= AIMI_START)
    aimi_full = [x for x in aimi if len(ns_days[x]) >= 0.80 * 288]
    dropped = [x for x in aimi if x not in aimi_full]

    print("== eras ==")
    print(f"Boost (DB, all clean days)   : {len(boost_all)} days {boost_all[0]}..{boost_all[-1]}")
    print(f"Boost (28d pre-switch window): {len(boost_28)} days {boost_28[0]}..{boost_28[-1]}")
    print(f"transition excluded          : {TRANSITION[0]}..{TRANSITION[1]}")
    print(f"AIMI (NS, full days)         : {len(aimi_full)} days "
          f"{aimi_full[0]}..{aimi_full[-1]}  (partial dropped: {dropped})")
    print()

    print("== per-day detail ==")
    for label, dd, keys in (("BOOST-28d", db_days, boost_28),
                            ("TRANSITION", ns_days, [x for x in ns_days if TRANSITION[0] <= x <= TRANSITION[1]]),
                            ("AIMI", ns_days, aimi_full)):
        for k in keys:
            m = metrics(dd[k])
            print(f"  {label:11s} {k} n={m['n']:3d} mean={m['mean']:6.1f} "
                  f"TIR={m['tir_70_180']:5.1f} TING={m['ting_63_140']:5.1f} "
                  f"TBR70={m['tbr_70']:5.2f} TBR54={m['tbr_54']:4.2f} "
                  f"TAR180={m['tar_180']:5.1f} CV={m['cv']:5.1f}")
        print()

    # --- headline comparison -------------------------------------------------
    for bl_label, bl_days, bl_keys in (("Boost 28d pre-switch", db_days, boost_28),
                                       ("Boost full DB era", db_days, boost_all)):
        ma = pooled(ns_days, aimi_full)
        mb = pooled(bl_days, bl_keys)
        ci = boot_diff(ns_days, aimi_full, bl_days, bl_keys)
        print(f"== AIMI ({len(aimi_full)}d) vs {bl_label} ({len(bl_keys)}d) ==")
        print(f"{'metric':12s} {'AIMI':>8s} {'Boost':>8s} {'diff':>8s} "
              f"{'95% CI on diff':>22s}  verdict")
        for k in KEYS:
            lo, hi = ci[k]
            diff = ma[k] - mb[k]
            v = "distinguishable" if (lo > 0 or hi < 0) else "UNPROVEN"
            print(f"{k:12s} {ma[k]:8.2f} {mb[k]:8.2f} {diff:+8.2f} "
                  f"[{lo:+8.2f}, {hi:+8.2f}]  {v}")
        print()


    # --- small-n framing: where does a 4-day AIMI block sit in the -----------
    #     distribution of every consecutive 4-day Boost block?
    L = len(aimi_full)
    ma = pooled(ns_days, aimi_full)
    blocks = []
    for i in range(len(boost_all) - L + 1):
        win = boost_all[i:i + L]
        # only consecutive calendar days
        d0 = datetime.strptime(win[0], "%Y-%m-%d")
        if [(d0 + timedelta(days=j)).strftime("%Y-%m-%d") for j in range(L)] != win:
            continue
        blocks.append((win[0], pooled(db_days, win)))
    print(f"== AIMI's {L}-day block vs all {len(blocks)} consecutive {L}-day Boost blocks ==")
    print(f"{'metric':12s} {'AIMI':>8s} {'Boost min':>10s} {'median':>8s} "
          f"{'max':>8s}  {'AIMI percentile':>16s}  outside Boost range?")
    for k in KEYS:
        vs = sorted(b[1][k] for b in blocks)
        pctile = 100.0 * sum(1 for v in vs if v < ma[k]) / len(vs)
        outside = "YES" if (ma[k] < vs[0] or ma[k] > vs[-1]) else "no"
        print(f"{k:12s} {ma[k]:8.2f} {vs[0]:10.2f} {vs[len(vs)//2]:8.2f} "
              f"{vs[-1]:8.2f}  {pctile:15.1f}%  {outside}")
    print()
    worst = sorted(blocks, key=lambda b: b[1]["tir_70_180"])[:3]
    print("worst 3 Boost blocks by TIR (for scale):")
    for start, m in worst:
        print(f"  from {start}: TIR={m['tir_70_180']:.1f} TBR70={m['tbr_70']:.2f} "
              f"TAR180={m['tar_180']:.1f} CV={m['cv']:.1f}")


if __name__ == "__main__":
    main()
