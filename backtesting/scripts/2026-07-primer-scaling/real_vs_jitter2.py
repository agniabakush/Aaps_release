#!/usr/bin/env python3
"""Real acceleration vs jitter, framed correctly.

An earlier framing asked "does the detector fire BEFORE a labelled onset". That is
the wrong question for these detectors: they all key on the rise itself, so they
fire DURING a rise, which an onset-anticipation label scores as a miss. Every
candidate came out below chance purely from that mis-specification.

The question the primer actually needs answered is:
    when this fires, is BG genuinely going up, or is it sensor jitter?

REAL  = BG rises >= CONT_MGDL within CONT_MIN minutes AFTER the firing cycle.
chance = P(REAL) over all cycles. lift = P(REAL | fire) / chance.
"""
import numpy as np, psycopg2, os, sys, json

DSN = "dbname=oref host=127.0.0.1 port=5432"
HERE = os.path.dirname(os.path.abspath(__file__))
USER, DAYS = "tim", 90
CONT_MGDL, CONT_MIN = 15.0, 30
MIN_LAST, MAX_LAST = 2.5, 7.5
MIN_SHORT, MAX_SHORT = 2.5, 17.5

def deltas(ts, v, i):
    now_t, now_v = ts[i], v[i]
    last, short = [], []
    for j in range(i - 1, max(-1, i - 12), -1):
        mins = (now_t - ts[j]) / 60.0
        if mins > MAX_SHORT: break
        if mins <= 0: continue
        a = (now_v - v[j]) / mins * 5
        if MIN_LAST <= mins <= MAX_LAST: last.append(a)
        if MIN_SHORT <= mins <= MAX_SHORT: short.append(a)
    sh = float(np.mean(short)) if short else 0.0
    return (float(np.mean(last)) if last else sh), sh

def main():
    sys.path.insert(0, os.path.join(sys.argv[1], "ukfpkg", "repeatable"))
    from smoothers import smooth_series, selftest_v4
    assert selftest_v4(verbose=False)
    with psycopg2.connect(DSN) as c, c.cursor() as cur:
        cur.execute("select extract(epoch from ts_utc)::bigint, cgm_mgdl from boost_cgm "
                    "where user_id=%s and cgm_mgdl is not null and ts_utc >= "
                    "(select max(ts_utc) from boost_cgm where user_id=%s) - make_interval(days=>%s) "
                    "order by ts_utc", (USER, USER, DAYS))
        rows = cur.fetchall()
    ts = np.array([r[0] for r in rows], float); v = np.array([r[1] for r in rows], float)
    n = len(v)
    rate = np.array([r if r == r else 0.0 for r in
                     smooth_series("v4", [int(t*1000) for t in ts], list(v))["rate_online"]], float)
    r5 = rate * 5.0
    d = np.zeros(n); s = np.zeros(n)
    for i in range(2, n): d[i], s[i] = deltas(ts, v, i)
    accl = np.where(np.abs(s) > 0.001, 100.0*(d-s)/np.maximum(np.abs(s), 2.0), 0.0)

    real = np.zeros(n, bool)
    for i in range(n):
        m = (ts > ts[i]) & (ts <= ts[i] + CONT_MIN*60)
        if m.any() and v[m].max() - v[i] >= CONT_MGDL: real[i] = True
    chance = 100.0*real.sum()/n
    print(f"{USER}: {n} points / {DAYS} days")
    print(f"REAL = +{CONT_MGDL:.0f} mg/dL within {CONT_MIN} min after the cycle")
    print(f"CHANCE P(REAL) = {chance:.1f}%\n")

    dets = {
        "accl>10 (current)":      (accl > 10) & (d > 0),
        "delta>=3":               d >= 3,
        "delta>=5":               d >= 5,
        "delta>=8":               d >= 8,
        "accl>10 & delta>=5":     (accl > 10) & (d >= 5),
        "ukf rate5>=2":           r5 >= 2.0,
        "ukf rate5>=3":           r5 >= 3.0,
        "ukf rate5>=4":           r5 >= 4.0,
        "ukf rate5>=3 & accl>10": (r5 >= 3.0) & (accl > 10),
        "ukf rate5>=2 & delta>=5":(r5 >= 2.0) & (d >= 5),
    }
    print(f"{'detector':24s} {'fire/100':>8s} {'P(REAL|fire)':>12s} {'lift':>6s} {'recall':>7s}")
    res = {}
    for name, f in dets.items():
        f = f.copy(); f[:3] = False; f[-7:] = False
        k = f.sum()
        if k == 0:
            print(f"{name:24s} never fires"); continue
        prec = 100.0*(f & real).sum()/k
        rec = 100.0*(f & real).sum()/max(real.sum(), 1)
        print(f"{name:24s} {100.0*k/n:8.2f} {prec:11.1f}% {prec/chance:5.2f}x {rec:6.1f}%")
        res[name] = dict(fire_per_100=100.0*k/n, precision=prec, lift=prec/chance, recall=rec)
    json.dump(dict(user=USER, days=DAYS, n=n, chance=chance, cont_mgdl=CONT_MGDL,
                   cont_min=CONT_MIN, detectors=res),
              open(os.path.join(HERE, "real_vs_jitter2.json"), "w"), indent=1)

main()
