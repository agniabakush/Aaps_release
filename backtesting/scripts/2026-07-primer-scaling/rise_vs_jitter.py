#!/usr/bin/env python3
"""Real acceleration vs sensor jitter: which discriminator actually separates them?

The current primer gate (delta_accl > 10) has no signal-to-noise test in it. On a
flat trace it reduces to (delta - shortAvgDelta) > 0.2 mg/dL, which is a fifth of
one CGM quantisation step, so it fires on jitter. This prices candidate
discriminators on real CGM.

Labels (objective, no dosing involved)
    ONSET  = first cycle of an episode where BG rises >= RISE_MGDL within RISE_MIN.
             Episodes are de-duplicated so one meal contributes one onset.
    QUIET  = cycles with no such rise ahead. False positives are counted here.

Detectors evaluated at every cycle, causally (no forward data):
    accl10        current gate: delta_accl > 10 and delta > 0
    delta5        absolute magnitude only
    accl10+delta5 conjunction
    persist2      two consecutive cycles with delta >= 3
    ukf_rate      the UKF's own filtered rate estimate > k mg/dL/5min. This is the
                  principled SNR test: a Kalman filter already separates signal
                  from measurement noise, and we were throwing that away by
                  feeding RAW deltas into a ratio.
    ukf_rate+d3   UKF rate gate plus a small absolute floor

Metrics
    precision  = fires that precede a real onset within LEAD_MAX min / all fires
    recall     = onsets caught (detector fired within LEAD_MAX before onset) / onsets
    lead       = median minutes by which a caught onset was anticipated
    fire rate  = fires per 100 cycles (the dosing-frequency cost)

Confidence: PROVISIONAL. Single user, descriptive over the window; no
cross-validation and no dosing outcome. It measures detection, not benefit.
"""
import numpy as np, psycopg2, os, sys, json

DSN = "dbname=oref host=127.0.0.1 port=5432"
HERE = os.path.dirname(os.path.abspath(__file__))
USER = "tim"
DAYS = 90
RISE_MGDL, RISE_MIN = 30.0, 45      # what counts as a real excursion
LEAD_MAX = 30                        # a fire "anticipates" an onset within this
MIN_LAST, MAX_LAST = 2.5, 7.5
MIN_SHORT, MAX_SHORT = 2.5, 17.5


def deltas(ts, v, i):
    now_t, now_v = ts[i], v[i]
    last, short = [], []
    for j in range(i - 1, max(-1, i - 12), -1):
        mins = (now_t - ts[j]) / 60.0
        if mins > MAX_SHORT:
            break
        if mins <= 0:
            continue
        a = (now_v - v[j]) / mins * 5
        if MIN_LAST <= mins <= MAX_LAST:
            last.append(a)
        if MIN_SHORT <= mins <= MAX_SHORT:
            short.append(a)
    sh = float(np.mean(short)) if short else 0.0
    return (float(np.mean(last)) if last else sh), sh


def main():
    sys.path.insert(0, os.path.join(sys.argv[1], "ukfpkg", "repeatable"))
    from smoothers import smooth_series, selftest_v4
    assert selftest_v4(verbose=False), "UKF selftest FAILED"

    with psycopg2.connect(DSN) as c, c.cursor() as cur:
        cur.execute("select extract(epoch from ts_utc)::bigint, cgm_mgdl from boost_cgm "
                    "where user_id=%s and cgm_mgdl is not null "
                    "and ts_utc >= (select max(ts_utc) from boost_cgm where user_id=%s) "
                    "  - make_interval(days => %s) order by ts_utc", (USER, USER, DAYS))
        rows = cur.fetchall()
    ts = np.array([r[0] for r in rows], float)
    v = np.array([r[1] for r in rows], float)
    print(f"{USER}: {len(v)} CGM points over {DAYS} days")

    out = smooth_series("v4", [int(t * 1000) for t in ts], list(v))
    rate = np.array([r if r == r else 0.0 for r in out["rate_online"]], float)

    # ---- labels
    n = len(v)
    fut_rise = np.zeros(n, bool)
    for i in range(n):
        m = (ts > ts[i]) & (ts <= ts[i] + RISE_MIN * 60)
        if m.any() and v[m].max() - v[i] >= RISE_MGDL:
            fut_rise[i] = True
    onset = np.zeros(n, bool)
    last = -1e9
    for i in range(n):
        if fut_rise[i] and ts[i] - last > 90 * 60:
            onset[i] = True
            last = ts[i]
    onset_idx = np.where(onset)[0]
    quiet = ~fut_rise
    print(f"labelled onsets: {onset.sum()}   quiet cycles: {quiet.sum()} "
          f"({100*quiet.sum()/n:.0f}%)\n")

    # ---- per-cycle features
    d = np.zeros(n); s = np.zeros(n)
    for i in range(2, n):
        d[i], s[i] = deltas(ts, v, i)
    accl = np.where(np.abs(s) > 0.001, 100.0 * (d - s) / np.maximum(np.abs(s), 2.0), 0.0)

    # rate_online is mg/dL per MINUTE (verified: a 120->114 fall over 10 min reads
    # about -0.6). Express thresholds per 5 min for comparability with delta.
    r5 = rate * 5.0

    dets = {
        "accl10 (current)": (accl > 10) & (d > 0),
        "delta>=5": d >= 5,
        "accl10 & delta>=5": (accl > 10) & (d >= 5),
        "persist2 (2x d>=3)": np.r_[False, (d[1:] >= 3) & (d[:-1] >= 3)],
        "ukf rate5>=2": r5 >= 2.0,
        "ukf rate5>=3": r5 >= 3.0,
        "ukf rate5>=4": r5 >= 4.0,
        "ukf rate5>=3 & accl10": (r5 >= 3.0) & (accl > 10),
        "ukf rate5>=3 & d>=5": (r5 >= 3.0) & (d >= 5),
    }

    # chance level: what fraction of cycles sit within LEAD_MAX before an onset?
    pre = np.zeros(n, bool)
    for oi in onset_idx:
        pre |= (ts >= ts[oi] - LEAD_MAX * 60) & (ts <= ts[oi])
    chance = 100.0 * pre.sum() / n
    print(f"CHANCE LEVEL: {chance:.1f}% of cycles are within {LEAD_MAX} min before an onset")
    print(f"  -> a detector with precision below {chance:.1f}% is WORSE THAN RANDOM\n")

    print(f"{'detector':22s} {'fire/100':>8s} {'precision':>10s} {'lift':>6s} {'recall':>7s} "
          f"{'lead(min)':>10s} {'FP/100 quiet':>13s}")
    res = {}
    for name, f in dets.items():
        f = f.copy(); f[:3] = False
        fires = np.where(f)[0]
        if len(fires) == 0:
            print(f"{name:22s} {'never fires':>8s}")
            continue
        good = 0
        for i in fires:
            w = onset_idx[(onset_idx >= i) & (ts[onset_idx] - ts[i] <= LEAD_MAX * 60)]
            if len(w):
                good += 1
        leads, caught = [], 0
        for oi in onset_idx:
            w = fires[(fires <= oi) & (ts[oi] - ts[fires] <= LEAD_MAX * 60)]
            if len(w):
                caught += 1
                leads.append((ts[oi] - ts[w].max()) / 60.0)
        prec = 100.0 * good / len(fires)
        rec = 100.0 * caught / max(len(onset_idx), 1)
        fp = 100.0 * (f & quiet).sum() / max(quiet.sum(), 1)
        print(f"{name:22s} {100*len(fires)/n:8.2f} {prec:9.1f}% {prec/chance:5.2f}x {rec:6.1f}% "
              f"{np.median(leads) if leads else float('nan'):10.1f} {fp:12.2f}")
        res[name] = dict(fire_per_100=100 * len(fires) / n, precision=prec, recall=rec,
                         median_lead=float(np.median(leads)) if leads else None,
                         fp_per_100_quiet=fp, lift=prec/chance)

    with open(os.path.join(HERE, "rise_vs_jitter.json"), "w") as fh:
        json.dump(dict(user=USER, days=DAYS, n=n, onsets=int(onset.sum()),
                       rise_mgdl=RISE_MGDL, rise_min=RISE_MIN, lead_max=LEAD_MAX,
                       detectors=res), fh, indent=1)


if __name__ == "__main__":
    main()
