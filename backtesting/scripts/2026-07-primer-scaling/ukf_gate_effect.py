#!/usr/bin/env python3
"""Effect of v4 UKF smoothing on the primer's acceleration gate and scaling.

Answers: if the affected user (NoSmoothingPlugin, Dexcom G6) had run the UKF,
would the primer have fired and scaled as it did?

Validity
--------
This is a WITHIN-PIPELINE contrast. An earlier attempt to reproduce AAPS's own
logged delta from the Nightscout sgv series failed (72% match; AAPS logged bg=122
where the uploaded reading was 120 - the loop's internal bucketed series differs
from what it uploads). So absolute values here are NOT "what AAPS would have
logged". But both arms - raw and smoothed - run through the identical delta
pipeline, so the raw -> smoothed DIFFERENCE is a valid estimate of the smoother's
effect on the statistic, which is the question asked.

Smoother validation before use:
  - smoothers.selftest_v4() -> 9/9 PASS (parity against the shipped Kotlin)
  - constants verified identical to UnscentedKalmanFilterPlugin: 25.0/16.0/225.0
  - on synthetic truth + known gaussian noise it halves 2nd-diff jitter and cuts
    RMSE vs truth (sd=8: jitter 19.53->7.99, RMSE 7.82->5.32)
  - on THIS user's feed it barely acts: jitter 2.463 -> 2.283 (7%), mean change
    1.07 mg/dL. The G6 feed is already smoother (2.46) than the filter's own
    output floor on comparable noise (~2.6), so there is little left to remove.
"""
import json, math, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from ukf_vs_primer import deltas_at, delta_accl   # DeltaCalculator-faithful windows

GATE_ACCL = 10.0     # PRIMER_ACCEL_THRESHOLD
SAT_ACCL = 30.0      # where 1 + (accl-10)/20 hits the PRIMER_MAX_MULT=2.0 ceiling


def jitter(a):
    d = [a[i + 1] - 2 * a[i] + a[i - 1] for i in range(1, len(a) - 1)]
    return math.sqrt(sum(x * x for x in d) / len(d))


def main():
    scratch = sys.argv[1]
    sys.path.insert(0, os.path.join(scratch, "ukfpkg", "repeatable"))
    from smoothers import smooth_series, selftest_v4

    assert selftest_v4(verbose=False), "v4 UKF parity selftest FAILED - do not trust output"

    sg = json.load(open(os.path.join(scratch, "dd_sgv.json")))
    raw = sorted([(x["date"], float(x["sgv"])) for x in sg])
    ts = [t for t, _ in raw]
    vals = [v for _, v in raw]
    lvl = smooth_series("v4", ts, vals)["level_online"]
    sm = [(ts[i], lvl[i] if lvl[i] == lvl[i] else vals[i]) for i in range(len(ts))]

    smv = [v for _, v in sm]
    print(f"smoothing actually applied: mean |raw-smoothed| = "
          f"{sum(abs(vals[i]-smv[i]) for i in range(len(vals)))/len(vals):.3f} mg/dL")
    print(f"2nd-diff jitter: raw {jitter(vals):.3f} -> smoothed {jitter(smv):.3f}")

    R, U = [], []
    for i in range(9, len(raw)):
        dr, sr, _ = deltas_at(raw, i)
        du, su, _ = deltas_at(sm, i)
        R.append((dr, sr, delta_accl(dr, sr)))
        U.append((du, su, delta_accl(du, su)))

    n = len(R)
    gate = lambda d, a: a > GATE_ACCL and d > 0.0
    pct = lambda c: 100.0 * c / n

    print(f"\nevaluated at {n} cycle points, identical pipeline both arms\n")
    print(f"{'':26s} {'RAW':>8s} {'UKF':>8s}")
    print(f"{'gate open (accl>10, d>0)':26s} "
          f"{pct(sum(1 for d, s, a in R if gate(d, a))):7.1f}% "
          f"{pct(sum(1 for d, s, a in U if gate(d, a))):7.1f}%")
    print(f"{'scale saturated accl>=30':26s} "
          f"{pct(sum(1 for d, s, a in R if gate(d, a) and a >= SAT_ACCL)):7.1f}% "
          f"{pct(sum(1 for d, s, a in U if gate(d, a) and a >= SAT_ACCL)):7.1f}%")
    o = [a for d, s, a in R if gate(d, a)]
    u = [a for d, s, a in U if gate(d, a)]
    print(f"{'mean accl when open':26s} {sum(o)/len(o):8.1f} {sum(u)/len(u):8.1f}")
    print(f"{'median accl when open':26s} {sorted(o)[len(o)//2]:8.1f} {sorted(u)[len(u)//2]:8.1f}")

    # the harm regime: flat traces
    fr = [(d, s, a) for d, s, a in R if abs(s) < 1.5]
    fu = [(d, s, a) for d, s, a in U if abs(s) < 1.5]
    print(f"\nFLAT traces only (|shortAvgDelta| < 1.5)   raw n={len(fr)}  ukf n={len(fu)}")
    print(f"{'  gate open':26s} "
          f"{100.0*sum(1 for d,s,a in fr if gate(d,a))/max(len(fr),1):7.1f}% "
          f"{100.0*sum(1 for d,s,a in fu if gate(d,a))/max(len(fu),1):7.1f}%")
    print(f"{'  saturated':26s} "
          f"{100.0*sum(1 for d,s,a in fr if gate(d,a) and a>=SAT_ACCL)/max(len(fr),1):7.1f}% "
          f"{100.0*sum(1 for d,s,a in fu if gate(d,a) and a>=SAT_ACCL)/max(len(fu),1):7.1f}%")

    print("\nNOTE: the gate reduces to (delta - shortAvgDelta) > 0.2 mg/dL whenever\n"
          "|shortAvgDelta| <= 2.0 (the floor). CGM reports INTEGER mg/dL, so one\n"
          "quantisation step is 5x the threshold - no smoother on integer input can\n"
          "defend a 0.2 mg/dL margin.")


if __name__ == "__main__":
    main()
