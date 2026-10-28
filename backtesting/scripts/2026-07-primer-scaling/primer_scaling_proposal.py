#!/usr/bin/env python3
"""Price a state-aware primer scaling function against the shipped one.

Shipped scaling (DetermineBasalBoostV5.kt):
    primerScale = 1 + max(0, (deltaAccl - 10) / 20)          capped at 2x
    primerU     = min(primerCapU * primerScale, 2 * primerCapU)

That is proportional to delta_accl, which the 2026-07-29 investigation showed is
LARGEST when the signal is noise (69.5 on a flat trace vs 12.5 on a genuine
11 mg/dL/5min rise), because its denominator floors at 2.0. So the shipped
function hands the biggest dose to the least reliable signal.

Proposed: primerCapU becomes a true ceiling, multiplied by three factors in [0,1]:

    fRise = clamp((delta - dLo)/(dHi - dLo)) * clamp((accl - aLo)/aSpan)
        delta carries the MAGNITUDE, acceleration only CONFIRMS. Both must be
        present, so noise (low delta, high accl) and an already-established climb
        (high delta, low accl) both score low. This is the term that discriminates.

    fBg   = clamp((bg - (target + bgMargin))/bgSpan) * clamp((bgCeil - bg)/bgFade)
        room above target, fading out again at the top so the primer cannot add
        into a high-IOB recovering tail (the repeated source of lows).

    fIob  = clamp(1 - iob/iobFull)
        headroom guard.

fBg and fIob are SUPPRESSORS, not discriminators - they cannot tell a real onset
from noise (both look flat and benign at onset). Only fRise discriminates. They
exist to bound the dose in contexts where being wrong is expensive.
"""
import json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))

# ---- proposed constants
D_LO, D_HI = 1.5, 8.0        # delta mg/dL/5min: 1.5 -> 0, 8 -> full. Carries ALL magnitude.
A_LO = 10.0                  # delta_accl %: BOOLEAN confirmer only, never a scaler.
                             # Using it as a scaler re-imports the inversion, since accl is
                             # LARGEST on flat traces (33.5 at the 17:04 hypo vs 16.8 at a
                             # genuine onset) - it would again pay most for the worst signal.
BG_LO, BG_LO_SPAN = 90.0, 20.0    # suppress below 90, full by 110
BG_CEIL, BG_FADE = 220.0, 40.0    # fade 180 -> 220, zero above (no adding into a high tail)
IOB_FULL = 9.0               # implementation should pass maxIob; this user's observed max ~6.4

MAX_MULT = 2.0               # shipped


def clamp(x, lo=0.0, hi=1.0):
    return max(lo, min(hi, x))


def shipped(cap, accl):
    scale = 1.0 + max(0.0, (accl - A_LO) / 20.0)
    return min(cap * scale, MAX_MULT * cap)


def proposed(cap, delta, accl, bg, target, iob, iob_full=IOB_FULL):
    # delta carries magnitude; acceleration only confirms (0/1)
    f_rise = clamp((delta - D_LO) / (D_HI - D_LO)) * (1.0 if accl > A_LO else 0.0)
    # plateau with shoulders: the primer's legitimate band is mid-range, suppressed at both ends
    f_bg = clamp((bg - BG_LO) / BG_LO_SPAN) * clamp((BG_CEIL - bg) / BG_FADE)
    f_iob = clamp(1.0 - iob / iob_full)
    return cap * f_rise * f_bg * f_iob, f_rise, f_bg, f_iob


def main():
    scratch = sys.argv[1]
    ds = json.load(open(os.path.join(scratch, "dd_ds.json")))
    import re
    cyc = {}
    for d in ds:
        s = d.get("openaps", {}).get("suggested")
        if s and s.get("timestamp"):
            cyc[str(s["timestamp"])[:16]] = s

    def num(p, r):
        m = re.search(p, r)
        return float(m.group(1)) if m else None

    CAP = 0.6   # this user's primerCapU (inferred: shipped max 1.2 = 2 x cap)

    def row(k, s, mark=""):
        r = s.get("reason", "")
        delta = num(r"Delta: (-?[0-9.]+)", r)
        accl = s.get("deltaAcceleration")
        bg = s.get("bg")
        tgt = s.get("targetBG")
        iob = s.get("IOB") or 0.0
        if None in (delta, accl, bg, tgt):
            return None
        old = shipped(CAP, accl)
        new, fr, fb, fi = proposed(CAP, delta, accl, bg, tgt, iob)
        return (k[5:16], bg, delta, accl, iob, old, new, fr, fb, fi, mark)

    print("THE SIX PRIMER FIRES  (cap = %.2fU)" % CAP)
    hdr = f"{'time':12s} {'bg':>4s} {'delta':>6s} {'accl':>6s} {'IOB':>5s} | {'shipped':>8s} {'proposed':>9s} | {'fRise':>6s} {'fBg':>5s} {'fIob':>5s}"
    print(hdr)
    fires = []
    for k, s in sorted(cyc.items()):
        if "primer=bolus" not in s.get("reason", ""):
            continue
        rr = row(k, s)
        if rr:
            fires.append(rr)
            print(f"{rr[0]:12s} {rr[1]:4.0f} {rr[2]:6.1f} {rr[3]:6.2f} {rr[4]:5.2f} | "
                  f"{rr[5]:8.2f} {rr[6]:9.3f} | {rr[7]:6.3f} {rr[8]:5.2f} {rr[9]:5.2f}")
    print(f"\n  shipped total {sum(f[5] for f in fires):.2f}U   proposed total {sum(f[6] for f in fires):.2f}U")

    print("\n\nREAL MEAL ONSET 28 Jul 18:04-18:49 (CONFIRMED at 18:34)")
    print(hdr)
    for k in sorted(cyc):
        if not ("2026-07-28T18:0" <= k <= "2026-07-28T18:50"):
            continue
        rr = row(k, cyc[k])
        if rr:
            st = cyc[k].get("boostV5_state")
            print(f"{rr[0]:12s} {rr[1]:4.0f} {rr[2]:6.1f} {rr[3]:6.2f} {rr[4]:5.2f} | "
                  f"{rr[5]:8.2f} {rr[6]:9.3f} | {rr[7]:6.3f} {rr[8]:5.2f} {rr[9]:5.2f}  {st}")


if __name__ == "__main__":
    main()
