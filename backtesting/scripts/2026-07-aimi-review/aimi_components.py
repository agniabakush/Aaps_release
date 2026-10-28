#!/usr/bin/env python3
"""Characterise the visible components of the AIMI fork from its devicestatus logs.

Reads devicestatus_H.json (produced by ns_pull.py), keeps only the AIMI-build
cycles, and tabulates which named modules fire, how often, and with what values.
Read-only text analysis: no site URL, token or identity is emitted.

Usage: aimi_components.py <dir-with-devicestatus_H.json>
"""
import collections, json, os, re, sys

AIMI_FROM = "2026-07-25T13:00"   # first AIMI-versioned devicestatus


def load(d):
    ds = json.load(open(os.path.join(d, "devicestatus_H.json")))
    ds.sort(key=lambda x: x.get("created_at", ""))
    out = []
    for x in ds:
        if x.get("created_at", "") < AIMI_FROM:
            continue
        sug = (x.get("openaps") or {}).get("suggested")
        if not sug:
            continue
        out.append(x)
    return out


def lines(sug):
    cl = sug.get("consoleLog")
    if isinstance(cl, list):
        return [str(s) for s in cl]
    if isinstance(cl, str):
        # sometimes serialised as a python-repr list
        try:
            v = json.loads(cl.replace("'", '"'))
            if isinstance(v, list):
                return [str(s) for s in v]
        except Exception:
            pass
        return cl.split("\n")
    return []


def num(s):
    try:
        return float(s.replace(",", "."))
    except Exception:
        return None


def pct(n, d):
    return f"{100.0*n/d:5.1f}%" if d else "  n/a"


def main():
    d = sys.argv[1] if len(sys.argv) > 1 else "."
    rows = load(d)
    N = len(rows)
    print(f"AIMI cycles with a suggestion: {N} "
          f"({rows[0]['created_at']} .. {rows[-1]['created_at']})\n")

    # ---- module presence ----------------------------------------------------
    tags = ["BASAL_UNIFIED_SCALING", "UnifiedReactivity", "BasalLearner",
            "PkPdEstimator", "PHYSIO_RT", "PAI:", "PKPD_OBS",
            "GATE_PKPD_MISSING", "PKPD predictions", "PKPD Debug: Config ENABLED is FALSE",
            "MAX_IOB_STATIC", "MAXSMB_SLOPE_HIGH", "Trajectory: Disabled",
            "TRAJECTORY STATUS", "CONTEXT MODULE", "PRED_SET", "PRED_PIPE",
            "AD_INTENT", "AUTODRIVE_APPLIED", "DYNAMIC_BASAL",
            "MEAL_PRIORITY_CONTEXT", "MEAL_PRIORITY_CHAIN", "REFRACTORY_RELAX",
            "GATE_REFRACTORY", "GATE_MAXIOB", "GATE_MAXSMB", "GATE_ABSORPTION",
            "GATE_PRED_MISSING", "SMB_CAP", "BASAL_GOV", "BOOST_BASAL_APPLIED",
            "HyperKicker", "REACTIVITY_LEARNER", "BASAL_LEARNER",
            "AIMI_SNAPSHOT", "Autosense ratio", "DIA Adjusted", "React: mode="]
    cnt = collections.Counter()
    for x in rows:
        blob = "\n".join(lines(x["openaps"]["suggested"]))
        rsn = str(x["openaps"]["suggested"].get("reason") or "")
        both = blob + "\n" + rsn
        for t in tags:
            if t in both:
                cnt[t] += 1
    print("== module / log-tag presence across AIMI cycles ==")
    for t in tags:
        print(f"  {t:42s} {cnt[t]:5d}  {pct(cnt[t], N)}")
    print()

    # ---- numeric distributions ---------------------------------------------
    pats = {
        "basal_learner_combined": r"BasalLearner: multiplier=([\d.]+)",
        "reactivity_combined": r"UnifiedReactivity: factor=([\d.]+)",
        "dynamic_basal_mult": r"DYNAMIC_BASAL .*Mult=([\d.]+)x",
        "autodrive_intent_U": r"AD_INTENT amount=([\d.]+)",
        "maxiob_pref": r"MAX_IOB_STATIC: Pref=([\d.]+)",
        "pkpd_eventual": r"PKPD predictions eventual=(-?[\d.]+) mg/dL",
        "peaktime_min": r"PeakTime=([\d.]+) min",
        "dia_adjusted_min": r"DIA Adjusted \(in minutes\) : ([\d.]+)",
        "smb_final_U": r"SMB result: raw=[-\d.]+ -> final=([\d.]+)",
        "mpc_alpha_pct": r"MPC utile: \d+% \(alpha=(\d+)%\)",
        "mpc_U": r"MPC predictive model: (-?[\d.,]+) U",
        "pi_U": r"PI physiological model: (-?[\d.,]+) U",
        "uam_U": r"UAM executed (-?[\d.,]+) U",
        "isf_fused_note": None,
    }
    vals = collections.defaultdict(list)
    for x in rows:
        sug = x["openaps"]["suggested"]
        both = "\n".join(lines(sug)) + "\n" + str(sug.get("reason") or "")
        for k, p in pats.items():
            if not p:
                continue
            for m in re.finditer(p, both):
                v = num(m.group(1))
                if v is not None:
                    vals[k].append(v)
    print("== numeric distributions (min / p25 / median / p75 / max, n) ==")
    import statistics as st
    for k in pats:
        v = sorted(vals.get(k, []))
        if not v:
            print(f"  {k:24s} absent")
            continue
        q = lambda f: v[min(len(v) - 1, int(f * len(v)))]
        print(f"  {k:24s} {v[0]:8.2f} {q(.25):8.2f} {st.median(v):8.2f} "
              f"{q(.75):8.2f} {v[-1]:8.2f}   n={len(v)}")
    print()

    # ---- PKPD health -------------------------------------------------------
    nan_isf = sum(1 for x in rows if "scale=NaN" in str(x["openaps"]["suggested"].get("reason") or ""))
    na_isf = sum(1 for x in rows if "ISF(fused)=n/a" in str(x["openaps"]["suggested"].get("reason") or ""))
    print("== PKPD module health ==")
    print(f"  reason says ISF(fused)=n/a : {na_isf} / {N}  {pct(na_isf, N)}")
    print(f"  reason says scale=NaN      : {nan_isf} / {N}  {pct(nan_isf, N)}")
    print()

    # ---- MPC-vs-PI disagreement -------------------------------------------
    dis = 0
    tot = 0
    both_pat = re.compile(r"MPC predictive model: (-?[\d.,]+) U \((\d+)%\).*?"
                          r"PI physiological model: (-?[\d.,]+) U \((\d+)%\)", re.S)
    for x in rows:
        r = str(x["openaps"]["suggested"].get("reason") or "")
        m = both_pat.search(r)
        if not m:
            continue
        tot += 1
        mpc, pi = num(m.group(1)), num(m.group(3))
        if mpc is not None and pi is not None and mpc * pi < 0:
            dis += 1
    print("== MPC vs PI blend ==")
    print(f"  cycles reporting both models       : {tot}")
    print(f"  opposite sign (models disagree)    : {dis}  {pct(dis, tot)}")
    print()

    # ---- dosing into highs -------------------------------------------------
    kick = 0
    kick_bg = []
    for x in rows:
        both = "\n".join(lines(x["openaps"]["suggested"])) + str(x["openaps"]["suggested"].get("reason") or "")
        if "HyperKicker" in both:
            kick += 1
            m = re.search(r"TICK ts=\d+ bg=([\d.]+)", both)
            if m:
                kick_bg.append(num(m.group(1)))
    print("== HyperKicker (adds basal/SMB into a high) ==")
    print(f"  cycles: {kick} {pct(kick, N)}")
    if kick_bg:
        kick_bg.sort()
        print(f"  BG at fire: min={kick_bg[0]:.0f} median={kick_bg[len(kick_bg)//2]:.0f} max={kick_bg[-1]:.0f} n={len(kick_bg)}")
    print()

    # ---- Autodrive reasons -------------------------------------------------
    ad = collections.Counter()
    for x in rows:
        both = "\n".join(lines(x["openaps"]["suggested"]))
        for m in re.finditer(r"AD_INTENT amount=[\d.]+ tbr=[\d.]+ reason=([^\n']+)", both):
            ad[m.group(1).strip()] += 1
    print("== Autodrive V2 trigger reasons ==")
    for k, v in ad.most_common(20):
        print(f"  {v:5d}  {k}")
    print()

    # ---- BASAL_GOV states --------------------------------------------------
    gov = collections.Counter()
    for x in rows:
        both = "\n".join(lines(x["openaps"]["suggested"]))
        for m in re.finditer(r"BASAL_GOV: action=(\w+)", both):
            gov[m.group(1)] += 1
    print("== BASAL_GOV actions ==")
    for k, v in gov.most_common():
        print(f"  {v:5d}  {k}")
    print()

    # ---- learner trajectory over time -------------------------------------
    print("== learner factors by local day (mean) ==")
    byday = collections.defaultdict(lambda: collections.defaultdict(list))
    for x in rows:
        day = x["created_at"][:10]
        both = "\n".join(lines(x["openaps"]["suggested"]))
        for key, p in (("basal", r"BasalLearner: multiplier=([\d.]+)"),
                       ("react", r"UnifiedReactivity: factor=([\d.]+)"),
                       ("dynbasal_mult", r"DYNAMIC_BASAL .*Mult=([\d.]+)x")):
            m = re.search(p, both)
            if m:
                byday[day][key].append(num(m.group(1)))
    for day in sorted(byday):
        b = byday[day]
        f = lambda k: (sum(b[k]) / len(b[k])) if b[k] else float("nan")
        print(f"  {day} basalLearner={f('basal'):.3f} reactivity={f('react'):.3f} "
              f"dynBasalMult={f('dynbasal_mult'):.2f}")


if __name__ == "__main__":
    main()
