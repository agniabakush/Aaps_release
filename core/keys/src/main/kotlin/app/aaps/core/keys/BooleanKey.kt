package app.aaps.core.keys

import app.aaps.core.keys.interfaces.BooleanPreferenceKey

enum class BooleanKey(
    override val key: String,
    override val defaultValue: Boolean,
    override val calculatedDefaultValue: Boolean = false,
    override val defaultedBySM: Boolean = false,
    override val showInApsMode: Boolean = true,
    override val showInNsClientMode: Boolean = true,
    override val showInPumpControlMode: Boolean = true,
    override val dependency: BooleanPreferenceKey? = null,
    override val negativeDependency: BooleanPreferenceKey? = null,
    override val hideParentScreenIfHidden: Boolean = false,
    override val engineeringModeOnly: Boolean = false,
    override val exportable: Boolean = true
) : BooleanPreferenceKey {

    GeneralSimpleMode("simple_mode", true),
    GeneralSetupWizardProcessed("startupwizard_processed", false),
    OverviewKeepScreenOn("keep_screen_on", false, calculatedDefaultValue = true),
    OverviewShowTreatmentButton("show_treatment_button", false, defaultedBySM = true, hideParentScreenIfHidden = true),
    OverviewShowWizardButton("show_wizard_button", true, defaultedBySM = true),
    OverviewShowInsulinButton("show_insulin_button", true, defaultedBySM = true),
    OverviewShowCarbsButton("show_carbs_button", true, defaultedBySM = true),
    OverviewShowCgmButton("show_cgm_button", false, defaultedBySM = true, showInNsClientMode = false),
    OverviewShowCalibrationButton("show_calibration_button", false, defaultedBySM = true, showInNsClientMode = false),
    OverviewShortTabTitles("short_tabtitles", false, defaultedBySM = true),
    OverviewShowNotesInDialogs("show_notes_entry_dialogs", false, defaultedBySM = true),
    OverviewShowStatusLights("show_statuslights", true, defaultedBySM = true, hideParentScreenIfHidden = true),
    OverviewUseBolusAdvisor("use_bolus_advisor", true, defaultedBySM = true),
    OverviewUseBolusReminder("use_bolus_reminder", true, defaultedBySM = true),
    OverviewUseSuperBolus("key_usersuperbolus", false, defaultedBySM = true, hideParentScreenIfHidden = true),
    OverviewBasalIsAlwaysNotAbsolute("overview_basal_always_not_absolute", true),
    OverviewRecalcOnTempTarget("recalc_on_temp_target", false),

    PumpBtWatchdog("bt_watchdog", false, showInNsClientMode = false, hideParentScreenIfHidden = true),

    AlertMissedBgReading("enable_missed_bg_readings", false),
    AlertPumpUnreachable("enable_pump_unreachable_alert", true),
    AlertCarbsRequired("enable_carbs_required_alert_local", true),
    AlertUrgentAsAndroidNotification("raise_urgent_alarms_as_android_notification", true),
    AlertIncreaseVolume("gradually_increase_notification_volume", true),

    BgSourceUploadToNs("dexcomg5_nsupload", true, defaultedBySM = true, hideParentScreenIfHidden = true),
    BgSourceCreateSensorChange("dexcom_lognssensorchange", true, defaultedBySM = true),

    ApsUseDynamicSensitivity("use_dynamic_sensitivity", false),
    ApsUseAutosens("openapsama_useautosens", true, defaultedBySM = true, negativeDependency = ApsUseDynamicSensitivity), // change from default false
    ApsUseSmb("use_smb", true, defaultedBySM = true), // change from default false
    /**
     * Run the loop at the sensor's own cadence instead of once per five-minute bucket.
     *
     * OFF by default, and a no-op on a five-minute sensor, where the native series and the
     * bucketed series are the same object. On a one-minute sensor it is the difference between a
     * five-minute loop that happens to be reading one-minute data, and a loop that actually runs
     * every minute. Those are separate interventions and this is what separates them. (2026-08-09)
     */
    ApsLoopAtNativeCadence("loop_at_native_cadence", false),
    ApsUseSmbWithHighTt("enableSMB_with_high_temptarget", false, defaultedBySM = true, dependency = ApsUseSmb),
    ApsUseSmbAlways("enableSMB_always", true, defaultedBySM = true, dependency = ApsUseSmb), // change from default false
    ApsUseSmbWithCob("enableSMB_with_COB", true, defaultedBySM = true, dependency = ApsUseSmb), // change from default false
    ApsUseSmbWithLowTt("enableSMB_with_temptarget", true, defaultedBySM = true, dependency = ApsUseSmb), // change from default false
    ApsUseSmbAfterCarbs("enableSMB_after_carbs", true, defaultedBySM = true, dependency = ApsUseSmb), // change from default false
    ApsUseUam("use_uam", true, defaultedBySM = true), // change from default false
    ApsSensitivityRaisesTarget("sensitivity_raises_target", true, defaultedBySM = true),
    ApsResistanceLowersTarget("resistance_lowers_target", true, defaultedBySM = true), // change from default false
    ApsAlwaysUseShortDeltas("always_use_shortavg", false, defaultedBySM = true, hideParentScreenIfHidden = true),
    ApsDynIsfAdjustSensitivity("dynisf_adjust_sensitivity", false, defaultedBySM = true, dependency = ApsUseDynamicSensitivity), // change from default false
    ApsDynIsfUseProfileSens("dynisf_use_profile_sens", false, defaultedBySM = false, dependency = ApsUseDynamicSensitivity),
    ApsDynIsfProfilePercentage("dynisf_use_profile_percentage", true, defaultedBySM = true, dependency = ApsUseDynamicSensitivity, negativeDependency = ApsDynIsfUseProfileSens),
    ApsUseDynamicCarbRatio("use_dynamic_carb_ratio", false),
    ApsDynamicCrUseCustomPeakTime("dynamic_cr_use_custom_peak_time", false, dependency = ApsUseDynamicCarbRatio),
    ApsAutoCr("auto_cr", false, negativeDependency = ApsUseDynamicCarbRatio),
    ApsAmaAutosensAdjustTargets("autosens_adjust_targets", true, defaultedBySM = true),
    ApsAutoIsfHighTtRaisesSens("high_temptarget_raises_sensitivity", false, defaultedBySM = true),
    ApsAutoIsfLowTtLowersSens("low_temptarget_lowers_sensitivity", false, defaultedBySM = true),
    ApsUseAutoIsfWeights("openapsama_enable_autoISF", false, defaultedBySM = true),
    ApsAutoIsfSmbOnEvenTarget("Enable alternative activation of SMB always", false, defaultedBySM = true),   // profile target
    ApsAutoIsfExerciseMode("aisf_exercise_mode", false),

    // AutoISF 3.2.0 - activity detection & FSL calibration
    ApsActivityDetection("activity_detection", false, defaultedBySM = true),
    ActivityMonitorDetection("activity_detection", false, defaultedBySM = true),
    ActivityMonitorOvernight("ignore_inactivity_overnight", true, defaultedBySM = true, dependency = ActivityMonitorDetection),
    ActivityMonitorStepsActive("steps_activity_detected", false, defaultedBySM = true),
    ActivityMonitorStepsInactive("steps_inactivity_detected", false, defaultedBySM = true),
    ActivityMonitorShowStepsFromSmartphone("steps_graph_from_smartphone", true, defaultedBySM = true),
    FslCalibrationTrigger("calibration_stops_SMB", false, defaultedBySM = true),
    FslCalibrationEnd("calibration_end", false, defaultedBySM = true),

    MaintenanceEnableFabric("enable_fabric2", false, defaultedBySM = true, hideParentScreenIfHidden = true),

    MaintenanceEnableExportSettingsAutomation("enable_unattended_export", false, defaultedBySM = false),

    AutotuneAutoSwitchProfile("autotune_auto", false),
    AutotuneCategorizeUamAsBasal("categorize_uam_as_basal", false),
    AutotuneTuneInsulinCurve("autotune_tune_insulin_curve", false),
    AutotuneCircadianIcIsf("autotune_circadian_ic_isf", false),
    AutotuneAdditionalLog("autotune_additional_log", false),

    SmsAllowRemoteCommands("smscommunicator_remotecommandsallowed", false),
    SmsReportPumpUnreachable("smscommunicator_report_pump_unreachable", true),
    SmsReportProfileSwitch("smscommunicator_report_profile_switch", true),
    SmsAddPumpStatus("smscommunicator_add_pump_status", false, dependency = SmsAllowRemoteCommands),
    SmsEnableOtp("smscommunicator_enable_otp", true, dependency = SmsAllowRemoteCommands),
    SmsReportToAll("smscommunicator_report_to_all", true, dependency = SmsAllowRemoteCommands),

    VirtualPumpStatusUpload("virtualpump_uploadstatus", false, showInNsClientMode = false),
    NsClientUploadData("ns_upload", true, showInNsClientMode = false, hideParentScreenIfHidden = true),
    NsClientAcceptCgmData("ns_receive_cgm", false, showInNsClientMode = false, hideParentScreenIfHidden = true),
    NsClientAcceptProfileStore("ns_receive_profile_store", true, showInNsClientMode = false, hideParentScreenIfHidden = true),
    NsClientAcceptTempTarget("ns_receive_temp_target", false, showInNsClientMode = false, hideParentScreenIfHidden = true),
    NsClientAcceptProfileSwitch("ns_receive_profile_switch", false, showInNsClientMode = false, hideParentScreenIfHidden = true),
    NsClientAcceptInsulin("ns_receive_insulin", false, showInNsClientMode = false, hideParentScreenIfHidden = true),
    NsClientAcceptCarbs("ns_receive_carbs", false, showInNsClientMode = false, hideParentScreenIfHidden = true),
    NsClientAcceptTherapyEvent("ns_receive_therapy_events", false, showInNsClientMode = false, hideParentScreenIfHidden = true),
    NsClientAcceptRunningMode("ns_receive_running_mode", false, showInNsClientMode = false, hideParentScreenIfHidden = true),
    NsClientAcceptTbrEb("ns_receive_tbr_eb", false, showInNsClientMode = false, engineeringModeOnly = true),
    NsClientNotificationsFromAlarms("ns_alarms", false, calculatedDefaultValue = true),
    NsClientNotificationsFromAnnouncements("ns_announcements", false, calculatedDefaultValue = true),
    NsClientUseCellular("ns_cellular", true),
    NsClientUseRoaming("ns_allow_roaming", true, dependency = NsClientUseCellular),
    NsClientUseWifi("ns_wifi", true),
    NsClientUseOnBattery("ns_battery", true),
    NsClientUseOnCharging("ns_charging", true),
    NsClientLogAppStart("ns_log_app_started_event", false, calculatedDefaultValue = true),
    NsClientCreateAnnouncementsFromErrors("ns_create_announcements_from_errors", false, calculatedDefaultValue = true, showInNsClientMode = false),
    NsClientCreateAnnouncementsFromCarbsReq("ns_create_announcements_from_carbs_req", false, calculatedDefaultValue = true, showInNsClientMode = false),
    NsClientSlowSync("ns_sync_slow", false),
    NsClient3UseWs("ns_use_ws", true),
    NsClient3IgnoreErrors("ns_ignore_errors", false),
    OpenHumansWifiOnly("oh_wifi_only", true),
    OpenHumansChargingOnly("oh_charging_only", false),
    XdripSendStatus("xdrip_send_status", false),
    XdripSendDetailedIob("xdripstatus_detailediob", true, defaultedBySM = true, hideParentScreenIfHidden = true),
    XdripSendBgi("xdripstatus_showbgi", true, defaultedBySM = true, hideParentScreenIfHidden = true),
    WearControl(key = "wearcontrol", defaultValue = false),
    WearWizardBg(key = "wearwizard_bg", defaultValue = true, dependency = WearControl, hideParentScreenIfHidden = true),
    WearWizardTt(key = "wearwizard_tt", defaultValue = false, dependency = WearControl, hideParentScreenIfHidden = true),
    WearWizardTrend(key = "wearwizard_trend", defaultValue = false, dependency = WearControl, hideParentScreenIfHidden = true),
    WearWizardCob(key = "wearwizard_cob", defaultValue = true, dependency = WearControl, hideParentScreenIfHidden = true),
    WearWizardIob(key = "wearwizard_iob", defaultValue = true, dependency = WearControl, hideParentScreenIfHidden = true),
    WearWizardAlarm("wearwizard_alarm", true, dependency = WearControl, hideParentScreenIfHidden = true),
    WearCustomWatchfaceAuthorization(key = "wear_custom_watchface_autorization", defaultValue = false),
    WearNotifyOnSmb(key = "wear_notifySMB", defaultValue = true),
    WearBroadcastData(key = "wear_broadcast_data", defaultValue = false),

    WizardCalculationVisible("wizard_calculation_visible", defaultValue = false),
    WizardCorrectionPercent("wizard_correction_percent", defaultValue = false),
    WizardIncludeCob("wizard_include_cob", defaultValue = false),
    WizardIncludeTrend("wizard_include_trend_bg", defaultValue = false),
    SiteRotationManagePump("site_rotation_manage_pump", defaultValue = false),
    SiteRotationManageCgm("site_rotation_manage_cgm", defaultValue = false),

    AlwaysPromoteAdvancedFiltering("always_promote_advanced_filtering", false),
    DisableUpdatesChecker("skip_updates_check", false, defaultedBySM = false),
    EngineeringMode("force_engineering_mode", false, defaultedBySM = false),
    NightMode("night_mode", false),
    NightModeWithCOB("night_mode_cob", true, dependency = NightMode),
    NightModeLowTT("night_mode_low_tt", true, dependency = NightMode),
    AllowRecalculatedBGs("allow_recalc_bgs", false),
    LyumjevU200("lyumjev_u200", false),
    EnableSmbBgThreshold("enable_smb_bg_threshold", false),

    // Boost
    OverviewUseBoostOverview("use_boost_overview", false, defaultedBySM = true),
    OverviewUseBoostOverviewV2("use_boost_overview_v2", false, defaultedBySM = true),
    ApsBoostEnablePercentScale("enableBoostPercentScale", false, defaultedBySM = true),
    ApsBoostEnableCircadianIsf("enableCircadianISF", false, defaultedBySM = true),
    ApsBoostAllowWithHighTt("enableBoost_with_high_temptarget", false, defaultedBySM = true),
    ApsBoostUseTdd("boost_use_tdd", false, defaultedBySM = true),
    ApsBoostAdjustSensitivity("boost_adjust_sensitivity", false, defaultedBySM = true, dependency = ApsBoostUseTdd),
    ApsBoostAllowAllBgSources("boost_allow_all_bg_sources", true, defaultedBySM = true),
    ApsBoostNightModeEnabled("boost_night_mode_enabled", false, defaultedBySM = true),
    ApsBoostNightModeDisableWithCob("boost_night_mode_disable_with_cob", false, defaultedBySM = true),
    ApsBoostNightModeDisableWithLowTt("boost_night_mode_disable_with_low_tt", false, defaultedBySM = true),
    ApsBoostBypassVersionCheck("boost_bypass_version_check", true, defaultedBySM = true),
    ApsBoostPostExerciseRecoveryEnabled("boost_post_exercise_recovery_enabled", false, defaultedBySM = true),
    ApsBoostHrIntegrationEnabled("boost_hr_integration_enabled", false, defaultedBySM = true),
    ApsBoostHrStressDetection("boost_hr_stress_detection", false, defaultedBySM = true),

    // Boost V5/V6 (ported from boost_v6)
    ApsBoostNightModeAutoBySleep("boost_night_mode_auto_by_sleep", false, defaultedBySM = true),
    ApsBoostHealthConnectHrEnabled("boost_health_connect_hr_enabled", false, defaultedBySM = true),
    ApsBoostV5ActiveDosing("boost_v5_active_dosing", false, defaultedBySM = true),
    ApsBoostV6PreMealTarget("boost_v6_pre_meal_target", false, defaultedBySM = true),
    ApsBoostV5FastCarbConfirm("boost_v5_fast_carb_confirm", true, defaultedBySM = true),
    // 2026-08-27 confirm tranche — split the confirm commitment, part now and the rest ten minutes
    // later if the rise continues. The confirm shot is the same size whether the excursion reaches
    // 20 mg/dL or 100, and the trace separates those two ends at 0.730 at the confirming cycle
    // against 0.893 ten minutes on. Can only deliver LESS than without it, never more.
    // The switch and fraction are user settings; auto-config derives only the release threshold.
    ApsBoostV5ConfirmTranche("boost_v5_confirm_tranche", false, defaultedBySM = true),
    // 2026-07-17 aggressive early-confirm — shaves the sustained-score early-confirm path one more
    // cycle (age −2). ~28% of its candidates are fizzle-catches, so NOT a clean cohort default:
    // OPT-IN and AUTO-CONFIG MANAGED (enabled only for clearly well-controlled users). Default OFF.
    ApsBoostV5AggressiveEarlyConfirm("boost_v5_aggressive_early_confirm", false, defaultedBySM = true),
    // 2026-07 composed Phase-3 brake floor (F = 0.25) — when ON (and V6 active), the delivered dose
    // is floored at min(budget × 0.25, committedCapU) on meal-session high cycles. Default OFF;
    // PER-USER activation gated on trailing-14d TBR<70 < 3.5% ∧ <54 < 0.8%. All hard gates/caps apply.
    ApsBoostV5ComposedFloorActive("boost_v5_composed_floor_active", false, defaultedBySM = true),
    // 2026-07-17 velocity-budget floor — when ON (and V6 active), the delivered dose is floored at
    // min(0.5U, committedCapU) on the budget≈0 high tail (BG > 180 ∧ oref insulinReq ≈ 0 ∧ not
    // RECOVERING ∧ awake ∧ not post-rescue), cycle exempt from the non-meal seam cap. PER-USER opt-in,
    // fail-closed 14d-TBR gate (TBR<63 < 2.0% ∧ TBR<70 < 3.5%). committedCap + maxIOB bounded. Default OFF.
    ApsBoostV5VelocityBudgetActive("boost_v5_velocity_budget_active", false, defaultedBySM = true),
    // 2026-07-20 V1-acceleration primer — hypo-prone routing (AUTO-CONFIG MANAGED). When ON,
    // auto-config has classified the user hypo-prone and routes the early primer through a
    // RETRACTABLE temp-basal instead of a bolus (safe-by-unwinding rather than safe-by-size).
    // OFF (default) = bolus primer (well-controlled). Overridable by ApsBoostV5PrimerBolusMode.
    ApsBoostV5PrimerTbrFallback("boost_v5_primer_tbr_fallback", false, defaultedBySM = true),
    // 2026-07-20 V1-acceleration primer — USER OVERRIDE (NOT auto-config-managed). When ON, forces
    // the bolus primer even if auto-config routed this user to the temp-basal fallback
    // (ApsBoostV5PrimerTbrFallback). Default OFF = respect the auto-config routing. Floors + net-off
    // are unaffected by the override; it only changes bolus-vs-temp-basal delivery.
    ApsBoostV5PrimerBolusMode("boost_v5_primer_bolus_mode", false, defaultedBySM = true),
    // 2026-08-03 post-rescue tight-ramp trial — a pre-registered within-user crossover (see
    // backtesting/protocols/2026-08_postrescue_tight_ramp_PREREG.md). When enrolled, days are
    // randomised into a treatment arm that caps the post-rescue rebound scale at 0.60 and applies
    // it across the WHOLE window (today the guard is gated off at BG >= 170). Both arms are at or
    // below the shipped dose on every cycle — the trial can never deliver MORE insulin than today.
    // Cohort evidence is UNPROVEN (2026-08-03 ramp study: every candidate ramp's cluster-bootstrap
    // CI overlaps zero; only 2 of 8 users show favourable targeting), which is exactly why this is
    // a randomised trial and not a ship. Default OFF; NOT auto-config managed — enrolment is a
    // deliberate per-user act.
    ApsBoostPostRescueTightRampTrial("boost_postrescue_tight_ramp_trial", false, defaultedBySM = true),
    ApsBoostV5AutoConfigDone("boost_v5_autoconfig_done", false, defaultedBySM = true),
    ApsBoostActivityShadowEnabled("boost_activity_shadow_enabled", true, defaultedBySM = true),
    ApsBoostAutosensWhenNoTdd("boost_autosens_when_no_tdd", false, defaultedBySM = true),
}