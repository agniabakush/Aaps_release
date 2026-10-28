# User H on AIMI — outcomes, and what is learnable

Data to 2026-07-29T23:13Z. First AIMI-versioned devicestatus **2026-07-25T13:59Z**,
build `3.4.2.2-Master.AIMI.160426`. Glucose in mg/dL throughout (their display is mmol/L).

Context: user H was a Boost V6 user who switched forks after two confirm-crash
incidents on 2026-07-23 disabled their loop.

## Part 1 — Outcomes

**Eras.** Boost full: 81 days, 2026-05-04..07-23 (local DB). Boost recency-matched:
28 days, 06-26..07-23. Transition 07-24/07-25 **excluded** (crash aftermath, build
swaps, four profile switches, an occlusion alarm). AIMI: **4 full days**, 07-26..07-29
(Nightscout).

DB and Nightscout agreed over the 26 overlapping days (mean 126.2 vs 126.1; TIR 91.9%
vs 91.8%), so mixing sources across eras is not driving the result.

Day-level block bootstrap, 20 000 iterations — days are the resampling unit because
5-min CGM is heavily autocorrelated and a per-point CI would be far too narrow.
AIMI (4d) vs Boost 28-day pre-switch:

| metric | AIMI | Boost | diff | 95% CI | verdict |
|---|---|---|---|---|---|
| TIR 70–180 | 77.1% | 93.9% | −16.8pp | [−24.9, −8.8] | **distinguishable** |
| TING 63–140 | 57.7% | 73.1% | −15.4pp | [−35.4, +2.2] | unproven |
| TBR <70 | 4.20% | 0.90% | +3.30pp | [−0.98, +7.60] | unproven |
| TBR <54 | 0.61% | 0.02% | +0.59pp | [−0.05, +1.81] | unproven |
| TAR >180 | 18.7% | 5.2% | +13.5pp | [+1.5, +25.8] | **distinguishable** |
| TAR >250 | 4.90% | 0.19% | +4.71pp | [−0.19, +9.86] | unproven |
| mean | 139.7 | 126.4 | +13.3 | [−8.9, +36.5] | unproven |
| CV | 37.2% | 23.2% | +14.0pp | [+7.0, +16.5] | **distinguishable** |

Against the full 81-day era the picture is the same, with TING also becoming
distinguishable (−18.3pp, [−37.0, −1.2]).

**Small-n framing.** Comparing 4 days against a 28-day mean is unfair, so the AIMI
block was placed in the distribution of all 78 consecutive 4-day Boost blocks. AIMI is
**outside the entire three-month Boost range** on mean (139.7 vs max 135.9), CV (37.2
vs max 29.9), TIR (77.1 vs min 86.3), TAR>180 (18.7 vs max 12.9) and TAR>250 (4.90 vs
max 1.30). TBR<70 sits at the 97th percentile and TBR<54 at the 95th, both still
*within* range. The worst 4-day Boost block in three months was TIR 86.3 / TAR 12.9 /
CV 27.7.

**Per day, with context:**

| day | era | carbs | TDD | nMeal | TIR | TBR<70 | TAR>180 | CV | mean |
|---|---|---|---|---|---|---|---|---|---|
| 07-23 | Boost | 0 g | 44.1 U | 5 | 75.7 | 1.74 | 22.6 | 40.2 | 139.0 |
| 07-24 | trans | 40 g | 15.3 U | 2 | 97.6 | 2.43 | 0.0 | 20.4 | 109.5 |
| 07-25 | trans | 26 g | 21.7 U | 2 | 86.1 | 5.21 | 8.7 | 39.6 | 116.5 |
| 07-26 | AIMI | 0 g | 19.4 U | 1 | 82.6 | **9.03** | 8.3 | 33.0 | 118.6 |
| 07-27 | AIMI | 48 g | 27.4 U | 2 | 86.4 | **7.67** (2.44 <54) | 5.9 | 28.2 | 117.6 |
| 07-28 | AIMI | 103 g | 31.2 U | 11 | 70.2 | 0.00 | **29.8** | 31.4 | 170.0 |
| 07-29 | AIMI | 70 g | 43.8 U | 11 | 68.9 | 0.00 | **31.1** | 37.6 | 153.2 |

Two low days then two high days — both directions inside one 4-day window, which is
why CV separates most robustly.

**Verdict: PROVISIONAL, leaning negative — and explicitly not an algorithm verdict.**
TIR, TAR>180 and CV are distinguishable and the block analysis puts AIMI outside the
whole Boost range on five of eight metrics. Hypoglycaemia roughly quadrupled in point
estimate but both TBR intervals straddle zero: **unproven**. Four confounds:

1. n = 4 days, n = 1 user; no cross-user replication possible.
2. **Hardware.** Occlusion announcements on 07-25 (×2), 07-26 (×1), 07-27 (×6); a set
   and insulin change 07-26 18:27; another set change 07-24. Impaired absorption gives
   exactly this signature — highs on rising TDD.
3. **Untuned migration.** `maxIOB` 7–8 U against TDD 20–44 U; AIMI's own `BASAL_GOV`
   never left `WARMUP`/`HOLD_CONSERVATIVE` in five days, i.e. its learners were cold.
4. **Behaviour.** 11 meal boluses/day on 07-28/29 against ~5 before, and the month's
   highest TDDs. Carb load alone does not explain it — Boost handled 223 g / 13 meal
   boluses on 07-11 at TIR 97.9 and 170 g on 07-20 at TIR 95.1 — but the eating
   pattern is unmatched.

Fair statement: their first four days are worse than any four-day Boost window on
mean, CV and TAR, distinguishably so; hypo is worse in point estimate but unproven;
none of it is attributable to the algorithm on four confounded days. Re-measure at
~28 days.

## Part 2 — What is learnable (3 492 AIMI cycles read)

**Measured distributions.** BasalLearner combined multiplier 1.00–1.22 (median 1.11).
UnifiedReactivity factor 0.00–1.50 (median 0.50). DYNAMIC_BASAL multiplier 0.00–9.98,
median **6.26**, `Brake=false` throughout. MPC proposal median **+1.00 U** (saturated
at cap, max 5.0); PI proposal median **−0.97 U** (min −8.72); alpha 50–90% clustered at
the endpoints; **MPC and PI opposite in sign in 60.7%** of the 1 897 cycles reporting
both. `DIA Adjusted` 288–720 min (median 453) while PKPD's own DIA reads 466–512 min in
the same cycles. `ISF(fused)=n/a` with `scale=NaN` in **28.2%**; `PKPD Debug: Config
ENABLED is FALSE` in 53.4%; `GATE_PKPD_MISSING` injects a fallback prediction in
**99.7%**. HyperKicker active 54.3%, firing at BG median 235 (min 120). Autodrive fired
48 times total on two triggers. `BASAL_GOV` returned only `HOLD_CONSERVATIVE` (1 065)
or `WARMUP` (295).

### Adopt

1. **Confidence-governed learner gating (`BASAL_GOV`) — adopt the pattern, telemetry
   only.** SOLID as description, PROVISIONAL as recommendation. AIMI emits
   `action / conf / n / hypo / high / mae` plus a human-readable reason every cycle and
   holds the learner at `WARMUP` until it has samples. Boost's auto-config already has
   n≥10, a cumulative clamp and a TBR raise-guard but publishes no per-cycle
   confidence/MAE/hypo-pressure record. Adding that to `BoostV5AutoConfig` costs nothing
   in the dose path and would have made several past migration questions answerable
   straight from the DB.
2. **Per-gate attribution — already converging, finish it.** SOLID. AIMI logs
   `proposed → baseLimit → safety → refr → throttle → tf → final` with every factor,
   plus an `AIMI_SNAPSHOT` JSON per decision. Boost added the per-stage equivalent in
   `a78bf7bb95`; what AIMI has and Boost still infers is *which gate bound the dose*
   (`GATE_MAXSMB`, `GATE_REFRACTORY`, `GATE_ABSORPTION`…).

### Worth an offline study, one direction only

3. **Dynamic peak-time / IOB-activity timing (`PAI`, `Calculation Dynamic PeakTime`).**
   PROVISIONAL. AIMI reshapes the activity curve per cycle: `Profile peak 75 →
   PeakTime=57.6 min`, with named modifiers `IOB Activity Acceleration −7.2 min`,
   `Activity ratio 0.69`, `SensorLag > Historic ×0.85`, `Bio-Sync: Deep Rest (HR 60)
   ×1.1`. This aims squarely at Boost's known blind spot — efficacy timing is invisible
   to IOB (`phase3-brake-compounding`) and is V7's efficacy-damper idea. Two problems:
   it is an *online* estimate feeding IOB and hence the dose (incompatible as built),
   and AIMI uses it to become *more* aggressive (`PAI: BG rising & IOB badly timed.
   AGGRESSIVE.` → ISF 8.7 → 5.2), which is backwards given the high tail is high-IOB.
   A Boost version would be an offline per-user peak-time estimate applied as a static
   auto-config value, withholding direction only.

### Reject

4. **`modelUAM.tflite`.** SOLID. A 4.4 KB pre-trained tflite emitting *units of
   insulin* (`UAM executed 0.32 U`). Pre-trained-at-inference so it does not breach
   hard rule #2 on its face, but it is a dose regressor over the user's own past SMBs —
   the behavioural-cloning objection that already killed `nightscout-ml`. Boost's two
   models output risk/probability consumed as dampers, which is the right shape.
5. **Online reactivity + basal learners — incompatible with hard rule #2.** SOLID that
   they are online; PROVISIONAL on the oscillation. `REACTIVITY_LEARNER` recomputes a
   global SMB multiplier from the last 24 h of TIR/CV/hypo count (`Reason: 3+ hypos
   factor 0.80, Variabilité élevée (CV=40%, Crossings=5) factor 0.93`) and applies it in
   the same cycle; `BASAL_LEARNER` does the same for basal. **The five days show the
   failure mode the rule exists to prevent:** mean reactivity factor by day
   0.49 → 0.45 → 0.43 → 0.48 → **1.32**. Two hypo-heavy days pushed it to ~0.43, then
   two high days pushed it to 1.32 — a 3× swing chasing outcomes it had just produced —
   while `BASAL_GOV` sat in `HOLD_CONSERVATIVE`, governing a learner that was already
   moving the dose. Four days is not proof of general instability, but it is a clean
   real-user exhibit for keeping Boost's learning offline. **The most useful thing AIMI
   gave us.**
6. **MPC + PI blend at computed alpha.** SOLID on measurement. `MPC: 1,00 U (90%) | PI:
   −1,54 U (10%) → Final SMB 0.75 U`. MPC is pinned at its cap, PI is usually negative,
   they disagree in sign 60.7% of the time, and the blend weights MPC 50–90%. A convex
   blend of a saturated insulin-adder and a withholder does not average opinions, it
   systematically overrides the conservative one. Boost's composed brake takes the
   **minimum** across guards, which is the correct composition under disagreement.
7. **`HyperKicker`.** SOLID. Active 54.3% of cycles at BG median 235;
   `MAXSMB_SLOPE_HIGH BG=244 slope=1.60 → maxSMBHB=1.00U (confirmed rise)` raises the
   SMB cap on a confirmed rise while high. All three variants already tested and
   rejected (`recovering-highs-smb-rejected-2026-07-03`). Do not revisit.
8. **Unbraked PD basal controller.** SOLID. Median multiplier 6.26, max 9.98,
   `Brake=false` in every observed cycle. ~10× basal authority with no brake term is
   what Boost's composed brake-floor exists to prevent, and that brake is ~90% correct
   on audit.
9. **PKPD DIA/Peak/Tail + fused ISF — reject in current form; it is broken.** SOLID.
   The idea overlaps what Boost's TDD blend and dynamic ISF already do. The
   implementation is half-wired: `ISF(fused)=n/a` with `scale=NaN` in 28.2%, config
   disabled in 53.4%, two disagreeing DIA estimates in the same cycle, and a fallback
   prediction injected into the dose pipeline in 99.7% of cycles. A warning about
   shipping a half-wired module inside the dose path, not a feature.

### Already covered by Boost

10. **`Autodrive V2`.** SOLID. Only two triggers, `Early: Bg120 & EffDelta2` (21 fires)
    and `Confirmed: Bg120 & EffDelta5 & Avg3` (27 fires) — a two-stage level+delta rule.
    Boost's OBSERVING→COMMITTED→CONFIRMED machine plus the accel/curvature primer is
    strictly richer, with per-user auto-config. Note also `AUTODRIVE_APPLIED intent=1.0
    actual=0.0` on the confirmed fires: the forced dose was gated away, so on this
    user's settings the trigger is largely decorative.
11. **`TRAJECTORY` score.** SOLID. `Trajectory: Disabled` in 50.9% of cycles,
    `trajectoryRelevanceScore = 0`. Boost's own signal-digging concluded value + delta +
    curvature is the whole short-horizon signal and shipped the curvature meal detector
    as a read-only shadow (`050d47ad51`). AIMI's `Coherence`/`Energy` terms are
    undocumented and non-firing.

## Bottom line

Nothing to adopt in the dose path. Borrow two telemetry patterns (#1, #2). One offline
investigation worth queueing (#3). Everything else is covered, already rejected on
Boost's own data, or forbidden by hard rule #2.

## Infrastructure note

This site sits behind a Cloudflare rule that 403s the default `Python-urllib`
User-Agent (CF error 1010). Sending a browser User-Agent fixes it; `ns_pull.py` does.
That block, not a 502, is what would stop a naive pull.
