# Boost/dev → Dmitry: September–October 2026 port inventory

Source: `agniabakush/Boost-in-AAPS_3.4` (GitHub commit search results, **not branch-scoped**). Destination: `agniabakush/AAPS-3.4.2.3_Dmitry` branch `3.4.2.6-Boost_V6_091026` at `cdf72de2949a747a6440590533be879dab428ecf` when recorded.

**Coverage limitation:** search returned 100 recent repository-wide commits, of which 55 were timestamped 2026-09-01 or later (through 2026-10-20). Repository-wide commit search does not prove all 55 belong to **dev**, nor that older/other matching commits were all returned. GitHub SHA ancestry also cannot measure semantic adaptations to Dmitry. Therefore this is a *provisional audit checklist*, **not a certified number of outstanding commits**. No dosing change should be deemed complete merely because of a status here.

Counts (provisional categories): 20 present / adapted (prelim.); 12 requires file-by-file verification; 3 partial / dependencies (prelim.); 20 research / docs / repository organization. Each source commit stays an individual row; a source commit may require several adaptation commits, or none if already present.

| Source SHA | Date UTC | Preliminary status | Subject |
|---|---|---|---|
| [579fc0cd19](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/579fc0cd19a1f6a6a096f865143c1bfb48e7c8f7) | 2026-10-20 | present / adapted (prelim.) | README: Max allowed bolus is the bolus limit under V6; build with Branch CI |
| [063e8cec7f](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/063e8cec7f8ebb275de81fc7a8fe60fcaa7daabb) | 2026-10-10 | requires file-by-file verification | Remove personal names and the developer's username from code |
| [d16285b47b](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/d16285b47b140c0fd41cc5c6692e5b69e350e7c1) | 2026-10-08 | present / adapted (prelim.) | MainApp: skip an empty or malformed remote-config "defs" instead of crashing |
| [a3cc1af06d](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/a3cc1af06d31e9272068f4497fffa0b3dbeecd30) | 2026-10-08 | present / adapted (prelim.) | Boost 6.1.2: safety fixes from the 8 October dose-path audit |
| [813fda2166](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/813fda2166f0762308a69190c0de45d2576907c5) | 2026-10-08 | present / adapted (prelim.) | V1 engine: tests for the audit items 4, 5, 19 and 20 |
| [62bc7bfd68](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/62bc7bfd68ecac55517610f15e362dee2409b580) | 2026-10-08 | present / adapted (prelim.) | V1 engine: four dose-path fixes from the 8 October audit (items 4, 5, 19, 20) |
| [6ab6cf9e3d](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/6ab6cf9e3d98006b26b7c982b5e2adc3f76d38d5) | 2026-10-08 | present / adapted (prelim.) | V6 plugin: wire the meal-state reset triggers |
| [c237a4d081](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/c237a4d08133a491d204a89c74a63f248dcf6067) | 2026-10-08 | present / adapted (prelim.) | V6 engine: one confirm per meal, expiring meal evidence, gated primer |
| [a33752c9aa](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/a33752c9aa88f60f8ca75eee99d4056ba44c6b73) | 2026-10-08 | partial / dependencies (prelim.) | Boost seam: safety fixes from the 8 October dose-path audit (#1, #8 to #12, #18) |
| [4cb9872869](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/4cb98728691e3b8f106aa7f1c8ed527334c3282e) | 2026-10-08 | partial / dependencies (prelim.) | Boost: retire five shadows that scored nothing (6.1.1) |
| [89d52dee0c](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/89d52dee0c4a3d802629b0ca1d29a1498a2f8607) | 2026-10-08 | requires file-by-file verification | Boost: upload a Boost version with branch and hash (6.1.0) |
| [5a1c852a57](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/5a1c852a57336338c725c8be9bef56bf6c0fdff0) | 2026-10-08 | present / adapted (prelim.) | Boost sleep: a resume burst needs steps outside the morning window |
| [47a815aedf](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/47a815aedf307ef01403986fb3b60b339f00b4da) | 2026-10-08 | present / adapted (prelim.) | Boost: close the Boost gate overnight whatever the night-mode toggle says |
| [51449448bc](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/51449448bc656938d4ad6c794d73258066009ab4) | 2026-10-06 | present / adapted (prelim.) | workflow: prepare the basal chart after the loop |
| [05a0aa4c2d](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/05a0aa4c2de2e0803dd8fdf5c009e7024d9eb531) | 2026-10-06 | present / adapted (prelim.) | receivers: per-source inbox for xDrip, Dexcom and SMS (port of upstream DataInbox) |
| [74dbe7668d](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/74dbe7668dadb733167db2cf42e51699634a5ef7) | 2026-10-06 | present / adapted (prelim.) | Dash: handle command confirmations off Room's executor |
| [82f47416c5](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/82f47416c58aa18a091f0ac2d07bb5b946e0f537) | 2026-10-05 | present / adapted (prelim.) | V6: an announced meal confirms into COMMITTED, not the 1.8x CONFIRMED shot |
| [ddf0eae279](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/ddf0eae2792a332ba1d09aef7c1622979e544c33) | 2026-10-04 | partial / dependencies (prelim.) | nsclient: apply a history backfill's rewind to the round that carries the full-sync flag |
| [a0b2e91a19](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/a0b2e91a193464034cf7089c8730c52752d2a5ac) | 2026-10-03 | research / docs / repository organization | Move study material that is not about the Boost engine to other repositories |
| [637e4dd3fd](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/637e4dd3fd542cf59332f3e500a61db473af75b7) | 2026-10-01 | present / adapted (prelim.) | recovery: the hypo-rebound auto-cancel removes only Boost's own recovery target |
| [fc8baab08a](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/fc8baab08ac5aaea14f85acbcb23f9c9f72695bb) | 2026-09-28 | present / adapted (prelim.) | docs: correct what the models can do, where the bolus cap lives, and the zone-2 guard |
| [bc1ab13b3c](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/bc1ab13b3ccd8e5a35d8e12ba97080710b9aeec1) | 2026-09-28 | present / adapted (prelim.) | readme: describe exercise, activity and recovery, and the two learned models |
| [477e649338](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/477e649338b1caf06f95966ef2ce86a04c2a3d91) | 2026-09-28 | present / adapted (prelim.) | activity: withhold the inactivity insulin raise from heart-rate zone 2 |
| [3770ebc677](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/3770ebc6778568234d638f9b39f4851ef00fb2cc) | 2026-09-24 | present / adapted (prelim.) | engine: label the falling-branch dosing sensitivity by the BG it uses |
| [1c6358ac6c](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/1c6358ac6c9a3fc6ef44c9b44e7ba1aad205888d) | 2026-09-24 | present / adapted (prelim.) | engine: apply autosens to the ISF at target, so it reaches the dose |
| [9b674cee5c](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/9b674cee5c6bb7fcc18b13f08fd03743297956c5) | 2026-09-24 | requires file-by-file verification | engine: move the autosens ISF branch out of determine_basal, which failed verification |
| [f393d02e55](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/f393d02e557aa49d2d448b153f677078c0ca040f) | 2026-09-24 | requires file-by-file verification | boost: switching TDD-based ISF on sets BG impact on ISF back to 100% |
| [3d9d471dbb](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/3d9d471dbb02441b7b400cb1f86135d031d5e51b) | 2026-09-24 | requires file-by-file verification | boost: no sensitivity adjustment or BG impact on ISF without TDD-based ISF |
| [e2372c092b](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/e2372c092be8a71b5b28418f6967683d6277c3fd) | 2026-09-24 | requires file-by-file verification | engine: let autosens adjust ISF on a static profile, as stock does |
| [28b5d470f9](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/28b5d470f9a516d252fc3a10e2a66a1b0eb1c06b) | 2026-09-03 | research / docs / repository organization | report(exercise premeal): state how the mode is built in the app, not only what the evidence says |
| [99586d8d82](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/99586d8d82d66afda2dc158208aeced2ab992e5f) | 2026-09-02 | research / docs / repository organization | Correct what the documentation says about the ML models and the exercise claim |
| [6be41cb5b4](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/6be41cb5b49f782c3ee24d512cba208a5bc13eff) | 2026-09-02 | research / docs / repository organization | Four investigations: timing, exercise protocols, the LGBM methods, and what the corpora buy |
| [d454b326cc](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/d454b326cc2e837bdefccc51d3d789751d4cdf42) | 2026-09-02 | research / docs / repository organization | backtest(meal clock): synthesis of what the local scripts found |
| [74aa518679](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/74aa518679026a79359cc37475108b900fa30121) | 2026-09-02 | research / docs / repository organization | report(meal signal): correct what the extraction does to split entries |
| [1f5c4eb835](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/1f5c4eb835d199331253f5e99f3a55cef6d9544a) | 2026-09-02 | research / docs / repository organization | backtest(meal size): re-run readability against a correctly counted meal |
| [980b44f5ad](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/980b44f5ada82ed108c20cb04dd48d90bdfe0dd4) | 2026-09-02 | research / docs / repository organization | backtest(meal clock): entries in proximity whatever their size, and what a cluster totals |
| [909aba034f](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/909aba034f9e97031065db5083cc60173ede92cd) | 2026-09-02 | research / docs / repository organization | backtest(meal clock): repeated identical entries inside an hour, per participant |
| [746a2a4c37](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/746a2a4c37f0912926ff6679929715a988d74f14) | 2026-09-02 | research / docs / repository organization | backtest(meal clock): the same window analysis on the OpenAPS Data Commons |
| [cf0a1f588e](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/cf0a1f588e1e5d6593f6269dfd56c538d5820447) | 2026-09-02 | research / docs / repository organization | backtest(meal clock): split the window curves by study, and label the axis as a percentage |
| [b6199a21c5](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/b6199a21c575af5812d060e47d97fb08026e2562) | 2026-09-02 | research / docs / repository organization | backtest(meal clock): size distributions in the three meal windows |
| [831a15c8ff](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/831a15c8ffedc8e5eb5f829dda7e3700d3a6f12a) | 2026-09-02 | research / docs / repository organization | backtest(meal clock): the distribution of every reported carbohydrate entry |
| [f81166f46b](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/f81166f46b424e9f74b394aa5afc49d30c933f37) | 2026-09-01 | research / docs / repository organization | backtest(meal clock): the size distribution in every hour, not only its centre |
| [af51c9255c](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/af51c9255c29969935fe8a0c1d2424776f8f7a2e) | 2026-09-01 | research / docs / repository organization | backtest(meal clock): when carbohydrate is announced, and how much |
| [f8d4d86f5d](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/f8d4d86f5d3a53c465fb73f68d59fc43925262b6) | 2026-09-01 | research / docs / repository organization | report(meal signal): state what an announced meal actually is |
| [b613001f62](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/b613001f62dc0f0f0509544ca022ca6a6f41bc57) | 2026-09-01 | research / docs / repository organization | report(meal signal): recast for a clinical and independent technical reader |
| [7582a39819](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/7582a398190dcb96a14f535c2a696e3db9262352) | 2026-09-01 | research / docs / repository organization | report(meal signal): plainer register, shorter sentences |
| [d0feb6b2c6](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/d0feb6b2c6c96bb2a2594ebfcef60d473e89d315) | 2026-09-01 | research / docs / repository organization | report(meal signal): rewrite around what a reader needs to decide |
| [06febe2b99](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/06febe2b99c73a3b5f20b7a6117bdd32503dc597) | 2026-09-01 | present / adapted (prelim.) | fix(keys): drop the ApsBoostAnticipHistory declaration the merge duplicated |
| [f5944f7e12](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/f5944f7e124b2867de465ce31d1be8b090dba387) | 2026-09-01 | requires file-by-file verification | Merge the shadow line into dev |
| [96fd70a2a8](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/96fd70a2a8e82121371c1fac91d87f9c1e470ef8) | 2026-09-01 | research / docs / repository organization | docs(readme): restore the two dose caps to the settings section |
| [8fb53f824a](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/8fb53f824a3ba3f410fe25ed0ee7d792a4b5fac9) | 2026-09-01 | requires file-by-file verification | Merge the acceleration primer correction into dev |
| [ab0a1052dc](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/ab0a1052dc8460956279e897eb09e71299715f1f) | 2026-09-01 | requires file-by-file verification | fix(boost/v6): revert the primer TBR lock — a user override must always win |
| [ac018ec06a](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/ac018ec06a639787a9c1253acc456ad4ee17ac01) | 2026-09-01 | requires file-by-file verification | fix(boost/v6): hypo-prone users get a provisioned primer, LOCKED to retractable temp basal |
| [dd5e1214e3](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/dd5e1214e3b0979c3f3036966ce4374dd5dc3363) | 2026-09-01 | requires file-by-file verification | feat(boost/v6): raise the primer ceiling range + self-scaling auto-config clamp |
| [abd2e02c9b](https://github.com/agniabakush/Boost-in-AAPS_3.4/commit/abd2e02c9b347570b2f6648b9c7af7e0be3c5b92) | 2026-09-01 | requires file-by-file verification | fix(boost/v6): primer trigger + sizing rework — absolute delta floor, state-aware scaling |

## Outstanding verification priorities

1. `a33752c9aa`: audit #11 activity refactor and other seam differences; adapted in multiple commits but full source parity not yet validated.
2. `ddf0eae279`: NSClient backfill race; Dmitry lacks `requestHistoryBackfill` and associated pending rewind plumbing. Do not cherry-pick alone.
3. `4cb9872869`: retiring unused shadows spans engine wiring, keys, telemetry, and source files; requires a focused dependency audit.
4. `f393d02e55`, `3d9d471dbb`: TDD/ISF setting migration semantics; verify against Dmitry's engine before attempting a port.
5. Run Android Gradle compilation, targeted JUnit tests and CI before real-pump use. Test run status is **unverified**. Preserve `.github/workflows/branch-ci.yml` and the Google Drive APK upload step.

Statuses are a triage index and must be refined by comparing source patches to target behavior before editing.

## Follow-up audit — 2026-10-26

- Source `4cb9872869` (retired read-only shadows): the target now has separate commits removing the plugin invocations, anticipation preference, shadow implementations and corresponding tests; `RT.kt` shadow telemetry comment was updated in `0f2fde4`. The source's `boostVersion 6.1.0 → 6.1.1` is **not** applied because target already says `6.1.2`. The source Kotlin backtesting harness is absent from target. **Full build and reference audit remain unverified**.
- Source `d16285b47b` (malformed Firebase `defs`), `74dbe7668d` (Omnipod DASH `observeOn(aapsSchedulers.io)` at all three confirmation paths), `637e4dd3fd` (Boost-owned recovery TempTarget identity check and its test), `6ab6cf9e3d` (profile switch / running-mode reset triggers) were checked against target and their key behaviors are already present. Do not duplicate these edits.
- Source `62bc7bfd68` V1 dose-path audit rules already exist in target `BoostV1SafetyRules.kt` and are called from `DetermineBasalBoost.kt`; source `813fda2166` corresponding safety test files also exist. Existence and inspected call sites are **not** a substitute for executing regression tests.
- Source `ddf0eae279` Nightscout backfill race fix depends on a `requestHistoryBackfill` implementation and pending-backfill state which the target NSClientV3 plugin does not have. **Blocked on prerequisite feature parity**; do not transplant the race patch alone.
- Source `3d9d471dbb` (switching TDD off reconciles dynamic sensitivity settings) and `f393d02e55` (restoring BG-impact setting on TDD enable) introduce coupled preference, runtime reconciliation, persistence and unit-test behavior. In the target, `ApsBoostLastUseTdd`, `reconcileSensitivitySettings` and related test are absent. **Not ported**: changing one preference/UI string alone would misrepresent or partially change dosing semantics. Require complete adaptation plus compilation, targeted tests and ART verifier check before acceptance.

No Android build, instrumented ART validation, or clinical/pump safety verification was performed in this audit.

## Continuation audit — 2026-10-26 (after 32a0e66c54)

- Source `89d52dee0c` (Boost build identifier in Nightscout): verified that target `buildSrc/src/main/kotlin/Versions.kt` defines `boostVersion = "6.1.2"`, and `plugins/aps/build.gradle.kts` generates `BOOST_VERSION` with branch and hash. This part is **already present**, not a missing port. Runtime upload was not verified.
- Source `3d9d471dbb` and `f393d02e55` (TDD settings): target commits `271c0f020d`, `bc658b4b1e`, `c93b729f02`, `5bd3336351` and `32a0e66c54` now stage tests, state keys and runtime transition handling. This supersedes the earlier 'not ported' note above; status is **adapted but unverified**. Compilation, test execution and runtime ART validation remain pending. Do not treat as production-ready.
- Source `9b674cee5c` (ART-safe extraction of static-ISF autosens branch): source moved calculation into a helper after an inlined implementation failed Android ART verification. The target uses a different dosing code structure; **semantic parity and ART verification remain open**. Avoid blindly transplanting the source patch.
- Source `063e8cec7f` (anonymization): source patch touches a large collection of research scripts and reports, while target already contains incremental anonymization commits. **Full path-by-path comparison still open**; avoid claiming that a few matching comments prove completion.

This is an inventory-only commit. No changes to dosing runtime and no build/tests are claimed.

## Regression follow-up — 2026-10-26

- `Boost Regression Tests` run `40248906837` compiled APS and executed 825 tests: 823 passed, 2 failures in `ScoreStreakCadenceTest`. The fixture began at `OBSERVING ageCycles=1`, so a clock age tick let the ordinary `age >= 2` confirm path fire before the intended sustained-score timing assertion. Commit `b3d4457232` starts the fixture at age 0 to isolate the early-path boundary. **Re-run pending; this is not proof of dosing correctness.**
- Source `e2372c092b` is only **partially staged** in commits `37b429390b` and `26d4342375`: the profile now carries `dynIsfMode`, but `DetermineBasalBoost` does not yet implement static-profile autosens correction. Do not mark this source commit complete.
- Source `9b674cee5c` isolates that calculation outside `determine_basal` due to a reported ART register-verification failure when inlined. A future adapted port must preserve this method-boundary constraint and receive real release-build, unit, and on-device ART validation. Do not copy the old inlined implementation.

## Static ISF / Autosens parity decision — 2026-10-26

- Regression run `40251656306` **passed** at `281c999f5b` (ART-safe `baseSensitivity` helper remains unconnected).
- Unlike Boost/dev `9b674cee5c`, Dmitry's `OpenAPSBoostPlugin.calculateBoostIsf` already invokes `autosensAdjustedIsf` (when `ApsBoostAutosensWhenNoTdd` is enabled) **before** it constructs `variable_sens` and `sensNormalTarget`. Calling the source `baseSensitivity(profile, variable_sens, autosens_data.ratio)` unconditionally in `DetermineBasalBoost` would risk *double autosens adjustment* of the ISF. Do not connect the staged helper until the two paths are reconciled and parity tested.
- Commit `e77407ca32` adds regression assertions around upstream autosens adjustment and the TDD exclusion; full APS test run is pending. Existing static-ISF path is retained. No dosing runtime changes in this follow-up.

## Source-commit parity check — 2026-10-26 after regression 40252150829

GitHub Actions `Boost Regression Tests` run `40252150829` succeeded at `e77407ca32`. APK Branch CI run `40252354627` for `9cca1951dd` was in progress when checked; **do not infer release success**.

Verified target implementation against individual source commits, to avoid duplicate cherry-picks:
- `d16285b47b`: MainApp catches malformed Firebase `defs` via `runCatching`; **already adapted**.
- `51449448bc`: `CalculationWorkflowImpl` schedules `PrepareBasalDataWorker` after `UpdateWidgetWorker`; **already adapted**.
- `74dbe7668d`: all three Omnipod Dash confirmation call sites use `.observeOn(aapsSchedulers.io)`; **already present**.
- `05a0aa4c2d`: DataInbox and its tests exist in target; **implementation present**, full path parity still requires review.
- `3770ebc677`: falling-branch reason already says `lowest predicted BG`; **already present**.
- `813fda2166`: both BoostV1SafetyRulesTest and BoostV1SafetyEngineTest exist; **present**, test results included in the APS suite.
- `5a1c852a57`: SleepResumeWakeTest exists; **present**, behavioral parity should be checked alongside runtime code.

Source `063e8cec7f` needs a path-by-path privacy review. Some research scripts and reports do not exist on the destination and may contain workstation-only paths or pseudonyms even in source. They should **not** be blindly copied into the production repository.

Autosens path remains **partially staged**: the new baseSensitivity helper is not connected, because the Dmitry adapter already adjusts ISF upstream. This avoids an unreviewed second division of dosing sensitivity.


## Verification checkpoint — 2026-10-26, after Boost APS run 40257365523

- Boost APS regression workflow completed **successfully** at commit `ef1a2f09f677742cbdb2323cae638a4fb24374f0`. The workflow only executes the APS unit test suite; it is **not** an APK release build, ART device verifier test, or NSClient test.
- Boost/dev `ddf0eae279` cannot be copied alone: Dmitry's `NSClientV3Plugin` has neither `requestHistoryBackfill` nor `historyBackfillFrom` nor `beginRound`. The source fix modifies an existing backfill request chain that does not exist here. Dmitry currently uses `resetToFullSync()` and `executeLoop()` with `fullSyncRequested`/ `doingFullSync` under `fullSyncSemaphore`. Implementing a new backfill API would be a distinct feature and requires interface callers, data-acceptance semantics, tests, and replay.
- Boost/dev V6 `SeamSafetyGatesTest` and `V6StandDownTest` are **adapted in part**, not verbatim: the target has `boostNightGateOpen` and `sleepBoundaryHoldActive`, but not the same `boostGateOpen`/`sleepSignals`/`primerTbrAction` API. The tests added to the target cover compatible pieces; they are not proof of complete source behavior parity.
- Boost/dev static ISF autosens extraction is staged but deliberately **not wired** into `DetermineBasalBoost` because Dmitry already applies `autosensAdjustedIsf` upstream. Reapplying it risks double-adjustment. Dynamic ISF, static ISF, and TDD mode combinations require targeted review before wiring.
- Defaults differ: Boost/dev expects `ApsBoostAutosensWhenNoTdd` true; Dmitry currently defaults to false. The target pins its existing default in `BoostSensitivityDefaultsParityTest` instead of silently changing dosing.
- Source `063e8cec7f` privacy scrub and the source research/backtesting commits have not been exhaustively mapped file-by-file. Missing research-only files and private workstation paths must not be bulk-copied.
- **Status: partial/adapted port, not full completion**. Remaining validation: source diff parity, NSClient sync integration, APK release build, on-device ART validation, and dose-safety replay. Do not label as fully ported or ready for pump use based only on the APS regression result.


## V6 seam adaptation checkpoint — 2026-10-26 (after c5fc6d4f23)

The target has individual follow-up commits adapting the source `a33752c9a` seams without replacing the differing Dmitry activity/night model:
- `61079fa0ea`: a pure `mealSessionRecordable` decision is now called by the actual learner. The existing daytime-only / AWAKE-only condition is unchanged.
- `d4080d67be`: three regression cases for the same decision.
- `2bc3b67a0`: extracts the existing protective primer temp-basal comparison into source-shaped `primerTbrAction` branches while preserving the same thresholds and actions.
- `c5fc6d4f23`: four source-adapted protective primer temp tests.
- `9a464ad20f`: the six original source tests for wall-clock MealHypothesis age cadence.
- `36e6c4869b`: round-trip `mealHypothesisLastAgeMs` in V5StateStore. A comment-stripped comparison with source `dev` now matches all non-comment code lines in V5StateStore.
- `6122a02a0e`: zone-3 inactivity suppression boundary assertion.

Other active V6 safety decisions already implemented in Dmitry include night Boost gate closure independent of the mode toggle, fail-closed pre-meal target checks, limited SMB fallback, capped confirm-tranche release, and sleep-boundary hold. Some source tests require different helper signatures; target tests cover the adapted forms. No tests were run for this checkpoint: the Boost regression workflow remains `workflow_dispatch` only under the user's test-at-end instruction.

**Still not full parity:** target activity classification is an inline implementation instead of source `classifyBoostActivity`; static ISF autosens is pre-adjusted upstream in Dmitry and cannot safely also apply source `baseSensitivity` without a double-adjustment audit; many source-only research files and retired legacy BoostV3 algorithms are intentionally not automatically copied. Re-run APS and Sync tests, release APK build, and device-specific ART verification at final integration.


## October 26 final integration candidate — scope and deliberate exceptions

The September–October Boost/dev safety and NSClient history-backfill changes have been adapted into Dmitry as individual commits. This is a **semantic port**, not a byte-for-byte merge. The final test checkpoint must include the commits listed below; prior green APS/Sync runs do not cover them.

- NSClient now exposes a bounded history backfill request and applies its history cursor rewind at the same full-sync round boundary. Boost V6 connects the 14-day first-install history-gap policy to this API, with a 48-hour onboarding window, retry spacing and a three-attempt cap; unit regression suites cover the history floor, lifecycle and race.
- Source V6 safeguards were mapped to Dmitry's existing night-window and activity architecture: sleep boundary hold, pre-meal and meal-recording blocks, protective primer temp-basal decisions, capped confirm-tranche releases, stand-down SMB fallback, meal state ageing, HR and step feed protections. Additional original source regression cases have been adapted.
- After comparing the active source and target modules, `V5StateStore`, `HrActivityCalculator` and `MealTimeLearner` have code-equivalent implementations apart from comments/formatting. Some other Dmitry paths are intentionally **more defensive** than Boost/dev: non-finite ML feature normalization, risk-tree index and NaN bounds, step-counter spike guard, and finite check on tranche headroom. These target protections must not be overwritten by source copies.
- Boost/dev static ISF source helper `baseSensitivity` is intentionally **not called**: Dmitry adjusts Autosens/ISF upstream in `OpenAPSBoostPlugin` before `DetermineBasalBoost`. Calling both would double-apply sensitivity adjustments. Per user instruction the existing ordering and `ApsBoostAutosensWhenNoTdd=false` default are retained. TDD-only settings dependencies were brought into parity without changing the formula.
- The branch does **not** import old BoostV3/V3ML experimental plugins, backtesting corpora, personal-path source content or unrelated pump Bluetooth drivers. Those do not form the production Boost V6 migration target. The active Dmitry adapter remains the destination for adapted behavior.

**Validation gate:** run the APS and Sync unit suites and an Android APK compilation on the resulting head SHA. A green CI check is not a real-pump safety validation, and an Android ART verifier issue requires actual-device or emulator startup checks. Until validation succeeds, call this a final integration *candidate*, not a tested release.


## Branch-scoped Boost 6.2.0 audit — 2026-10-28

Source was pinned to `agniabakush/Boost-in-AAPS_3.4/dev` at
`8b45b031777fdc3942107f3c80561ed14d15c827`. GitHub's ancestry comparison from
`a3cc1af06d31e9272068f4497fffa0b3dbeecd30` to that head returns **21 commits**.
This supersedes the repository-wide-search coverage limitation for this specific
6.1.2 → 6.2.0 interval. Destination is `3.4.2.6-Boost_V6_261026`, initially
`3845c20b9f5ed59f9f7e6b903a14b7b707ced625`.

| Source commit | Destination evidence / disposition |
|---|---|
| `d16285b47b` | Malformed remote-config handling already adapted; covered by the earlier integration inventory. |
| `063e8cec7f` | All source-patch paths that exist in target were inspected. Runtime comments/test names were already scrubbed; four remaining research-script comment/label edits are completed with this audit. The cap-stepper already uses anonymous wording. Source-only research files are not introduced. |
| `579fc0cd19` | README bolus-limit and Branch CI guidance already adapted. |
| `6aad2c2302` | Per-cycle cap analyser fix and its regression already adapted in `acfab435`. |
| `6140b59ff1` | Personal tranche derivation, 45-day history collection, guarded onboarding/weekly application, primer route, preference descriptions and replay fixture adapted. Source lowering-enabled test hooks are not added to the production resolver. |
| `71910e67e4` | Live resolver never automatically lowers tranche threshold; target retains stricter protection of manual changes. |
| `1b270bd9e6` | Unmasked ledger/breadcrumb reads present; original Simple Mode regressions restored using target's `ApsBoostV5ComputedWritten` key. |
| `180d4b031d` | TBR loosening guards and neutral aggression ceiling adapted; original `RedriveLooseningGuardTest` matches source. |
| `8152d4be19` | Legacy ownership uses pre-redrive cap snapshots; writes update the ownership ledger; disabled primer and user limits remain protected. Original `RedriveUserValuesTest` restored. |
| `0224b41884` | Weekly insulin-adding switch resolution and persisted ownership/verdicts present; original switch regression suite restored. |
| `2221fcb96c` | Active/shadow aggressive early confirmation use fail-closed live TBR guard; source regression file matches. |
| `194501e0c7` | Lyumjev PD base/U100/U200 implementations and PD test file match source exactly. |
| `ba94f1727b` | Battery helper/tests match source; V2 fragment adapted to target. |
| `bab8232e22` | Button grid/helper tests and both layout files match source; fragment call sites adapted. |
| `a64ed98857` | Original Boost overview battery source adaptation already in `9bd2efec`. |
| `290348d59a` | Detector, StepFeed and lie-in tests match source. Target runs sleep evaluation before gates and supplies separate phone/watch day totals, combined 15-minute steps and sleepDbg telemetry. Target's configured night/sleep-in gates remain its deliberate safety adaptation. |
| `ff8d810201` | InsulinNeedWatch model/tests match source; scheduler integration and diagnostic-only notice adapted. |
| `7ad1c10699` | Dedicated notification ID, temporary profile exclusion and complete-day window integrated with the watch port. |
| `ec6bcb43f2` | DeviceAgeTag/helper tests match; reason/extractor integration adapted in `06ff2acf`. |
| `88850f1547` | First-fetch sentinel fix included; DeviceAgeTag matches source exactly. |
| `8b45b03177` | Boost version updated to `6.2.0` in `571265a`; fork application version/build label retained. |

### Changes made during this audit

- `571265a14906ddba3794297bc67177866fc43aba` restores upstream weekly primer-route,
  insulin-adding switch, computed-cap ownership and Simple Mode ledger tests.
  It also restores the synthetic shared Python/Kotlin derivation fixture and the
  episode-by-episode parity assertion that had been omitted from the target test.
- Tranche UI descriptions now reflect the actual target behavior: switch/fraction
  are user settings, only the threshold is derived, automatic lowering stays
  disabled, and manually changed values stay owned by the user.
- The regression workflow now checks the Lyumjev PD, overview battery and button
  suites and the analyser, in addition to APS, NSClient and fullDebug APK assembly.
  The existing signed Branch CI / Google Drive workflow is unchanged.

### Deliberate target differences retained

- `ApsBoostV5ComputedWritten` is the existing persisted ownership key; replacing it
  with the source's differently named ledger would lose installed ownership history.
- A changed value remains user-owned even when it equals a factory default.
  A stored zero primer is always kept. The target tranche resolver exposes no
  lowering-enabled runtime/test parameter.
- Tranche resolution is explicitly called by the onboarding/weekly scheduler,
  outside the ordinary cap/movement list. Missing evidence does not become a
  generic numeric-knob suggestion.
- Configured Dmitry night/sleep-in protections, upstream ISF pre-adjustment and
  its Autosens default remain as documented in the previous integration checkpoint.
- Historical participant replays, source-only experimental plugins and external
  data corpora are outside this production port. The restored fixture is synthetic.

Validation for the resulting head is performed by the automatic
**Boost Regression Tests** workflow. Its APS/NSClient tests, focused insulin/UI
tests, analyser tests and APK assembly must all succeed before calling this
integration verified. A successful build does not establish device ART startup
or real-pump behavior.
