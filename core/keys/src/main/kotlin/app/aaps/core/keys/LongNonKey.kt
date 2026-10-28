package app.aaps.core.keys

import app.aaps.core.keys.interfaces.LongNonPreferenceKey

enum class LongNonKey(
    override val key: String,
    override val defaultValue: Long,
    override val exportable: Boolean = true
) : LongNonPreferenceKey {

    LocalProfileLastChange("local_profile_last_change", 0L),
    BtWatchdogLastBark("bt_watchdog_last", 0L),
    ActivePumpChangeTimestamp("active_pump_change_timestamp", 0L),
    LastCleanupRun("last_cleanup_run", 0L),
    ApsBoostHealthConnectLastSyncMs("boost_health_connect_last_sync_ms", 0L),

    // 2026-08-03 auto-config periodic re-derivation — when it last RAN (epoch ms), regardless of
    // whether it changed anything. Drives the 7-day cadence.
    ApsBoostV5AutoConfigLastRedriveMs("boost_v5_autoconfig_last_redrive_ms", 0L),
    ApsBoostNeedWatchLastAboveMs("boost_need_watch_last_above_ms", 0L),
    ApsBoostNeedWatchLastBelowMs("boost_need_watch_last_below_ms", 0L),
    ApsBoostHistorySyncLastAttemptMs("boost_history_sync_last_attempt_ms", 0L),
    ApsBoostHistorySyncFirstSeenMs("boost_history_sync_first_seen_ms", 0L),
}

