package app.aaps.core.keys

import app.aaps.core.keys.interfaces.BooleanPreferenceKey
import app.aaps.core.keys.interfaces.StringPreferenceKey

enum class StringKey(
    override val key: String,
    override val defaultValue: String,
    override val defaultedBySM: Boolean = false,
    override val showInApsMode: Boolean = true,
    override val showInNsClientMode: Boolean = true,
    override val showInPumpControlMode: Boolean = true,
    override val dependency: BooleanPreferenceKey? = null,
    override val negativeDependency: BooleanPreferenceKey? = null,
    override val hideParentScreenIfHidden: Boolean = false,
    override val isPassword: Boolean = false,
    override val isPin: Boolean = false,
    override val exportable: Boolean = true
) : StringPreferenceKey {

    GeneralUnits("units", "mg/dl"),
    GeneralLanguage("language", "default", defaultedBySM = true),
    GeneralPatientName("patient_name", ""),
    GeneralSkin("skin", ""),
    GeneralDarkMode("use_dark_mode", "dark", defaultedBySM = true),

    AapsDirectoryUri("aaps_directory", ""),

    ProtectionMasterPassword("master_password", "", isPassword = true),
    ProtectionSettingsPassword("settings_password", "", isPassword = true),
    ProtectionSettingsPin("settings_pin", "", isPin = true),
    ProtectionApplicationPassword("application_password", "", isPassword = true),
    ProtectionApplicationPin("application_pin", "", isPin = true),
    ProtectionBolusPassword("bolus_password", "", isPassword = true),
    ProtectionBolusPin("bolus_pin", "", isPin = true),

    OverviewCopySettingsFromNs(key = "statuslights_copy_ns", "", dependency = BooleanKey.OverviewShowStatusLights),

    SafetyAge("age", "adult"),
    MaintenanceEmail("maintenance_logs_email", "logs@aaps.app", defaultedBySM = true),
    MaintenanceIdentification("email_for_crash_report", ""),
    AutomationLocation("location", "PASSIVE", hideParentScreenIfHidden = true),

    SmsAllowedNumbers("smscommunicator_allowednumbers", ""),
    SmsOtpPassword("smscommunicator_otp_password", "", dependency = BooleanKey.SmsAllowRemoteCommands, isPassword = true),

    VirtualPumpType("virtualpump_type", "Generic AAPS"),

    NsClientUrl("nsclientinternal_url", ""),
    NsClientApiSecret("nsclientinternal_api_secret", "", isPassword = true),
    NsClientWifiSsids("ns_wifi_ssids", "", dependency = BooleanKey.NsClientUseWifi),
    NsClientAccessToken("nsclient_token", "", isPassword = true),

    NightModeBegin("night_mode_begin", "22:00", dependency = BooleanKey.NightMode),
    NightModeEnd("night_mode_end", "06:00", dependency = BooleanKey.NightMode),
    GarminRequestKey(key = "garmin_aaps_key", defaultValue = ""),
    PumpCommonBolusStorage("pump_sync_storage_bolus", ""),
    PumpCommonTbrStorage("pump_sync_storage_tbr", ""),

    // Boost
    ApsBoostStartTime("boost_start_time", "07:00", defaultedBySM = true),
    ApsBoostEndTime("boost_end_time", "07:01", defaultedBySM = true),
    ApsBoostNightModeStart("boost_night_mode_start", "22:00", defaultedBySM = true),
    ApsBoostNightModeEnd("boost_night_mode_end", "07:00", defaultedBySM = true),

    // Boost V5 silent-shadow persisted state (JSON blob)
    ApsBoostV5State("boost_v5_state", "", defaultedBySM = true),

    // V7 shadow residual pools (JSON blob: pending IOB-only projections + regime-conditioned
    // residual samples, ~21-day window). Read/written by V7Shadow every Boost cycle; blank or
    // corrupt → cold start (sizer abstains until pools re-warm). See openAPSBoostV7/V7_SHADOW.md.
    ApsBoostV7ResidualPools("boost_v7_residual_pools", "", defaultedBySM = true),

    // Boost ISF shadow — persisted EMA(τ=3h) state
    ApsBoostIsfShadowState("boost_isf_shadow_state", "", defaultedBySM = true),
    ApsBoostVwaTddShadowState("boost_vwa_tdd_shadow_state", "", defaultedBySM = true),
    // Persist the last active Boost engine for the future V1/V6 settings reconciliation.
    // This key alone does not enable or change sensitivity behavior.
    ApsBoostLastEngineMode("boost_last_engine_mode", "", defaultedBySM = true, exportable = false),
    // Last observed TDD switch state; for a future, explicitly tested transition-aware reconcile.
    ApsBoostLastUseTdd("boost_last_use_tdd", "", defaultedBySM = true, exportable = false),

    // Sleep state machine persisted state (JSON blob: SleepState, hysteresis counters, entry ts)
    // Boost V5/V6 persisted state blobs (ported from boost_v6)
    ApsBoostSleepState("boost_sleep_state", "", defaultedBySM = true),
    ApsBoostSleepHistory("boost_sleep_history", "", defaultedBySM = true),
    ApsBoostMlRingBuffer("boost_ml_ring_buffer", "", defaultedBySM = true),
    ApsBoostMealTimeHistory("boost_meal_time_history", "", defaultedBySM = true),
    ApsBoostDailyStepHistory("boost_daily_step_history", "", defaultedBySM = true),
    ApsBoostIntradayStepBank("boost_intraday_step_bank", "", defaultedBySM = true),

    // Per-install randomisation seed for pre-registered trials (first written when a trial is
    // enrolled; never rewritten). Arm assignment is a pure function of (seed, local day index), so
    // the offline analysis can reproduce every day's arm exactly from this string — no need to
    // trust a logged flag. Clearing it re-randomises, which would break an in-flight trial.
    ApsBoostTrialSeed("boost_trial_seed", "", defaultedBySM = true),

    // 2026-08-03 periodic re-derivation (rev 2). JSON map {prefKey: derivedValue} of where the
    // DERIVATION sat at the last write. Re-derivation applies the derivation's MOVEMENT since this
    // baseline to whatever the knob is currently set to, so a user's own value is scaled rather
    // than overwritten and no notion of knob ownership is needed. Only advanced when a write
    // actually happens, so movement suppressed by the deadband accumulates instead of being lost.
    ApsBoostV5RedriveBaseline("boost_v5_redrive_baseline", "", defaultedBySM = true),
    ApsBoostV5ComputedWritten("boost_v5_computed_written", "", defaultedBySM = true),

    // 2026-08-03 re-derivation hysteresis for the QUANTISED knobs (aggression, hypoCaution). JSON
    // map {prefKey: value} of a new value seen ONCE. It is written only if the next cycle derives
    // the same value again, which stops boundary flapping across a threshold.
    ApsBoostV5AutoConfigPending("boost_v5_autoconfig_pending", "", defaultedBySM = true),

    // Breadcrumb for the LAST periodic re-derivation, replayed into the reason every cycle (as
    // autordv=) so Nightscout and boost_decisions always carry the current state, not just the one
    // cycle in seven where it ran. Display-only; never consulted for dosing.
    // Non-dosing status breadcrumb for V6 auto-configuration and insulin-need watch.
    ApsBoostV5AutoConfigSummary("boost_v5_autoconfig_summary", "", defaultedBySM = true),
    ApsBoostV5RedriveSummary("boost_v5_redrive_summary", "", defaultedBySM = true),

    // Rolling human-readable log of the last few re-derivation CHANGES (newest first, capped), so
    // "what has auto-config done to my settings" is answerable in-app after the notification has
    // been dismissed.
    ApsBoostV5RedriveHistory("boost_v5_redrive_history", "", defaultedBySM = true),
    ApsBoostHistorySyncSummary("boost_history_sync_summary", "", defaultedBySM = true),
}