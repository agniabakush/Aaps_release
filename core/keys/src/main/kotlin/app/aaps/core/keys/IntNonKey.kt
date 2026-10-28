package app.aaps.core.keys

import app.aaps.core.keys.interfaces.IntNonPreferenceKey

@Suppress("SpellCheckingInspection")
enum class IntNonKey(
    override val key: String,
    override val defaultValue: Int,
    override val exportable: Boolean = true
) : IntNonPreferenceKey {

    ObjectivesManualEnacts("ObjectivesmanualEnacts", 0),
    RangeToDisplay("rangetodisplay", 6),
    BoostV5AutoConfigSchemaVersion("boost_v5_autoconfig_schema_version", 0),

    // Periodic re-derivation persistence schema. Bumped when stored re-derivation state written by
    // an EARLIER build must not be trusted by a later one. v2: rev 1 stamped the last-run clock and
    // returned from a dead code path without ever recording a baseline, which would have gated rev 2
    // out for a further 7 days on every existing install.
    BoostV5RedriveSchemaVersion("boost_v5_redrive_schema_version", 0),
    ApsBoostHistorySyncAttempts("boost_history_sync_attempts", 0),
    ApsBoostHistorySyncPreBg("boost_history_sync_pre_bg", 0),
    ApsBoostHistorySyncPreTreatments("boost_history_sync_pre_treatments", 0)
}