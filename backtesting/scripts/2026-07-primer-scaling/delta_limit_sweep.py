#!/usr/bin/env python3
"""What should the primer's delta limit be, to keep early dosing without firing on jitter?

Framing. The primer only has value if it acts EARLIER than the point the engine would
have committed anyway. In the observed 2026-07-28 meal, V6 reached CONFIRMED at
delta = 8.0 mg/dL/5min. So we use "delta >= 8" as the reference point for "the loop
was going to act", and for each candidate threshold T measure:

    lead    = minutes by which delta >= T precedes delta >= 8 within the same rise
    admits  = fires at delta >= T that are NOT in the run-up to any real rise
              (i.e. jitter fires - the cost side)
    P(real) = fraction of fires followed by a genuine +15 mg/dL within 30 min

Deltas are computed from the UKF-SMOOTHED series, because both live primer users now
run the UKF, so that is the signal the trigger actually reads. The sweep is then
repeated with the UKF rate gate ANDed in, to test whether the rate test lets us LOWER
the delta limit (keeping lead) without admitting jitter - which is the whole point of
having a separate jitter discriminator.

Confidence: PROVISIONAL. One user, 90 days, descriptive. Measures detection and
timing, not dosing benefit.
"""
import numpy as np, psycopg2, os, sys, json

DSN = "dbname=oref host=127.0.0.1 port=5432"
HERE = os.path.dirname(os.path.abspath(__file__))
USER, DAYS = "tim", 90
CONFIRM_DELTA = 8.0        # proxy for "the engine commits anyway"
RISE_MGDL, RISE_MIN = 30.0, 45
REAL_MGDL, REAL_MIN = 15.0, 30
MAX_LEAD_MIN = 60          # a fire only "leads" a confirm within this
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
                    "where user_id=%s and cgm_mgdl is not null and ts_utc >= "
                    "(select max(ts_utc) from boost_cgm where user_id=%s) - make_interval(days=>%s) "
                    "order by ts_utc", (USER, USER, DAYS))
        rows = cur.fetchall()
    ts = np.array([r[0] for r in rows], float)
    vraw = np.array([r[1] for r in rows], float)
    out = smooth_series("v4", [int(t * 1000) for t in ts], list(vraw))
    lvl = out["level_online"]
    v = np.array([lvl[i] if lvl[i] == lvl[i] else vraw[i] for i in range(len(ts))], float)
    r5 = np.array([(x if x == x else 0.0) * 5.0 for x in out["rate_online"]], float)
    n = len(v)
    print(f"{USER}: {n} points / {DAYS} days, deltas from UKF-smoothed series\n")

    d = np.zeros(n)
    for i in range(2, n):
        d[i], _ = deltas(ts, v, i)

    # real continuation (the precision label) - use RAW for outcome truth
    real = np.zeros(n, bool)
    for i in range(n):
        m = (ts > ts[i]) & (ts <= ts[i] + REAL_MIN * 60)
        if m.any() and vraw[m].max() - vraw[i] >= REAL_MGDL:
            real[i] = True

    # confirm points: cycles where delta first crosses CONFIRM_DELTA in a rise, deduped
    conf = []
    last_c = -1e9
    for i in range(n):
        if d[i] >= CONFIRM_DELTA and ts[i] - last_c > 90 * 60:
            conf.append(i)
            last_c = ts[i]
    conf = np.array(conf)
    print(f"confirm-equivalent points (delta>={CONFIRM_DELTA:.0f}, deduped 90min): {len(conf)}\n")

    def sweep(name, extra=None):
        print(name)
        print(f"  {'T':>5s} {'fires/100':>9s} {'P(real)':>8s} {'median lead':>12s} "
              f"{'confirms led':>13s} {'jitter fires/100':>17s}")
        res = {}
        for T in (1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 6.0, 8.0):
            f = d >= T
            if extra is not None:
                f = f & extra
            f = f.copy(); f[:3] = False; f[-7:] = False
            fires = np.where(f)[0]
            if len(fires) == 0:
                print(f"  {T:5.1f}  never fires")
                continue
            leads, led = [], 0
            inrun = np.zeros(n, bool)
            for ci in conf:
                w = fires[(fires <= ci) & (ts[ci] - ts[fires] <= MAX_LEAD_MIN * 60)]
                if len(w):
                    led += 1
                    leads.append((ts[ci] - ts[w.min()]) / 60.0)
                    inrun[w] = True
            jit = f & ~inrun & ~real
            prec = 100.0 * (f & real).sum() / len(fires)
            print(f"  {T:5.1f} {100.0*len(fires)/n:9.2f} {prec:7.1f}% "
                  f"{np.median(leads) if leads else float('nan'):11.1f}m "
                  f"{100.0*led/max(len(conf),1):12.0f}% {100.0*jit.sum()/n:16.2f}")
            res[T] = dict(fires_per_100=100.0*len(fires)/n, precision=prec,
                          median_lead=float(np.median(leads)) if leads else None,
                          confirms_led_pct=100.0*led/max(len(conf), 1),
                          jitter_per_100=100.0*jit.sum()/n)
        print()
        return res

    a = sweep("DELTA ALONE (smoothed)")
    b = sweep("DELTA & ukf rate5 >= 2", r5 >= 2.0)
    c_ = sweep("DELTA & ukf rate5 >= 3", r5 >= 3.0)

    json.dump(dict(user=USER, days=DAYS, confirm_delta=CONFIRM_DELTA,
                   n_confirms=len(conf), delta_alone=a, rate2=b, rate3=c_),
              open(os.path.join(HERE, "delta_limit_sweep.json"), "w"), indent=1, default=float)


if __name__ == "__main__":
    main()
