#!/usr/bin/env python3
"""How many hypos has the primer caused for tim (a UKF user)?

The primer was live for tim 2026-07-21 to 2026-07-27 (56 bolus fires). The v4 UKF
shipped to his build on 2026-07-11, so the ENTIRE primer era is UKF-covered - this
is a clean read on "does the primer cause lows on a smoothed CGM feed".

A raw "N fires were followed by a low" count is over-attribution: the primer fires
on rising BG, and rising-BG cycles have their own baseline low rate. So each fire
is compared against matched control cycles.

Two control sets:
  MATCHED   - same era, no primer within 90 min before, BG +/-10 mg/dL,
              IOB +/-0.5 U, hour-of-day +/-2. Controls for state, not for rise.
  NEAR-MISS - same era, delta > 0 and deltaAccl in [5, 10]: cycles that met every
              primer condition except falling just under the acceleration gate.
              Maximally comparable; this is the discontinuity-style control.

Outcome: minimum CGM within 120 min after the cycle. Low < 70, severe < 54 mg/dL.
Uncertainty: 2000-draw bootstrap over fires (and over controls) for the difference.

ASSOCIATIONAL. A matched baseline narrows confounding, it does not remove it -
there is no counterfactual trajectory for "same cycle without the primer".
"""
import numpy as np, psycopg2, json, os

DSN = "dbname=oref host=127.0.0.1 port=5432"
HERE = os.path.dirname(os.path.abspath(__file__))
RNG = np.random.default_rng(20260729)
NBOOT = 2000
HORIZON_MIN = 120
USER = "tim"
ERA0, ERA1 = "2026-07-21", "2026-07-28"


def q(sql, args=()):
    with psycopg2.connect(DSN) as c, c.cursor() as cur:
        cur.execute(sql, args)
        return cur.fetchall()


def main():
    cgm = np.array([(r[0], r[1]) for r in q(
        "select extract(epoch from ts_utc)::bigint, cgm_mgdl from boost_cgm "
        "where user_id=%s and cgm_mgdl is not null and ts_utc >= %s::timestamptz - interval '1 day' "
        "and ts_utc < %s::timestamptz + interval '1 day' order by ts_utc", (USER, ERA0, ERA1))], float)
    ct, cv = cgm[:, 0], cgm[:, 1]

    rows = q("""
        select extract(epoch from ts_utc)::bigint, cgm_mgdl, iob_iob,
               extract(hour from ts_utc)::int, delta_acceleration,
               substring(reason_text from 'Delta: (-?[0-9.]+)')::float,
               (reason_text like '%%primer=bolus%%') as fired,
               substring(reason_text from 'primer=bolus,([0-9.]+)U')::float
        from boost_decisions
        where user_id=%s and ts_utc >= %s and ts_utc < %s and cgm_mgdl is not null
        order by ts_utc""", (USER, ERA0, ERA1))

    def min_ahead(t):
        m = (ct > t) & (ct <= t + HORIZON_MIN * 60)
        return cv[m].min() if m.any() else np.nan

    fires, allc = [], []
    for t, bg, iob, hr, accl, delta, fired, amt in rows:
        rec = dict(t=t, bg=bg, iob=iob or 0.0, hr=hr, accl=accl, delta=delta,
                   fired=bool(fired), amt=amt or 0.0, nadir=min_ahead(t))
        allc.append(rec)
        if fired:
            fires.append(rec)

    fire_times = np.array([f["t"] for f in fires])

    def no_recent_primer(t):
        return not np.any((fire_times <= t) & (fire_times > t - 90 * 60))

    matched, nearmiss = [], []
    for r in allc:
        if r["fired"] or np.isnan(r["nadir"]) or not no_recent_primer(r["t"]):
            continue
        if r["accl"] is not None and r["delta"] is not None and r["delta"] > 0 and 5.0 <= r["accl"] <= 10.0:
            nearmiss.append(r)
        for f in fires:
            if (abs(r["bg"] - f["bg"]) <= 10 and abs(r["iob"] - f["iob"]) <= 0.5
                    and min(abs(r["hr"] - f["hr"]), 24 - abs(r["hr"] - f["hr"])) <= 2):
                matched.append(r)
                break

    fires = [f for f in fires if not np.isnan(f["nadir"])]

    def rate(rs, thr):
        return 100.0 * np.mean([r["nadir"] < thr for r in rs]) if rs else np.nan

    def boot_diff(a, b, thr):
        d = []
        for _ in range(NBOOT):
            sa = RNG.integers(0, len(a), len(a))
            sb = RNG.integers(0, len(b), len(b))
            d.append(100 * np.mean([a[i]["nadir"] < thr for i in sa])
                     - 100 * np.mean([b[i]["nadir"] < thr for i in sb]))
        return float(np.percentile(d, 2.5)), float(np.percentile(d, 97.5))

    out = {"n_fires": len(fires), "n_matched": len(matched), "n_nearmiss": len(nearmiss),
           "horizon_min": HORIZON_MIN, "era": [ERA0, ERA1]}
    print(f"tim, primer era {ERA0} .. {ERA1}  (UKF live since 2026-07-11)")
    print(f"  primer fires with usable follow-up : {len(fires)}")
    print(f"  matched controls                   : {len(matched)}")
    print(f"  near-miss controls (accl 5-10)     : {len(nearmiss)}")
    print(f"  total primer insulin               : {sum(f['amt'] for f in fires):.2f}U (U200)")

    for thr, name in ((70, "LOW  <70"), (54, "SEVERE <54")):
        rf = rate(fires, thr)
        nf = sum(1 for f in fires if f["nadir"] < thr)
        print(f"\n{name} within {HORIZON_MIN} min")
        print(f"  after a primer fire : {nf}/{len(fires)} = {rf:.1f}%")
        for label, ctrl in (("matched", matched), ("near-miss", nearmiss)):
            if len(ctrl) < 10:
                print(f"  {label:10s} control: n={len(ctrl)} too few")
                continue
            rc = rate(ctrl, thr)
            lo, hi = boot_diff(fires, ctrl, thr)
            excess = (rf - rc) / 100.0 * len(fires)
            elo, ehi = lo / 100.0 * len(fires), hi / 100.0 * len(fires)
            verdict = "distinguishable" if (lo > 0 or hi < 0) else "NOT distinguishable"
            print(f"  {label:10s} control : {rc:.1f}%  (n={len(ctrl)})   "
                  f"diff {rf-rc:+.1f} pp [{lo:+.1f}, {hi:+.1f}] -> {verdict}")
            print(f"  {'':10s}           attributable events: {excess:+.1f} [{elo:+.1f}, {ehi:+.1f}]")
            out[f"{name}_{label}"] = dict(rate_fire=rf, rate_ctrl=rc, diff=rf - rc,
                                          ci=[lo, hi], excess=excess, excess_ci=[elo, ehi])

    with open(os.path.join(HERE, "primer_hypo_attribution.json"), "w") as f:
        json.dump(out, f, indent=1, default=float)


if __name__ == "__main__":
    main()
