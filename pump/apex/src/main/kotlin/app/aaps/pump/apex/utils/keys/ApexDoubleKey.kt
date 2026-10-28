package app.aaps.pump.apex.utils.keys

import app.aaps.core.keys.interfaces.BooleanPreferenceKey
import app.aaps.core.keys.interfaces.DoublePreferenceKey

enum class ApexDoubleKey(
    override val key: String,
    override val defaultValue: Double,
    override val min: Double,
    override val max: Double,
    override val defaultedBySM: Boolean = false,
    override val calculatedBySM: Boolean = false,
    override val showInApsMode: Boolean = true,
    override val showInNsClientMode: Boolean = true,
    override val showInPumpControlMode: Boolean = true,
    override val dependency: BooleanPreferenceKey? = null,
    override val negativeDependency: BooleanPreferenceKey? = null,
    override val hideParentScreenIfHidden: Boolean = false,
    override val exportable: Boolean = true
): DoublePreferenceKey {
    // 0 == uninitialized
    // Both mirror the pump: onStatusV1 copies the pump's own limits back into them on every frame that
    // differs, and updateSettings refuses to send a value below what the pump reports. The ceilings are
    // the pump's own, taken from the maker's editors: 35 U/h for the basal rate, 25 U for a bolus. The
    // basal one used to be 25 as well, which was below what the pump itself allows - a pump holding a
    // higher rate was read back into the preference and displayed, but could never be entered or
    // confirmed again, because the edit dialog is bounded by this ceiling.
    MaxBasal("apex_max_basal", 0.0, 0.0, 35.0),
    MaxBolus("apex_max_bolus", 0.0, 0.0, 25.0),
    // Manual battery voltages apply only when battery-% calc is on and a custom battery type is selected.
    // Both halves are applied by the preference screen, in ApexPumpPlugin.preprocessPreferences. Not by
    // `dependency`: a declared dependency makes the screen rebuild the whole activity when its switch is
    // touched, which throws the reader back to the top. Resolution needs no setting here either - on
    // this API version AdaptiveDoublePreference is a decimal text field, not a stepped slider, so the
    // two decimals these need are simply typed.
    //
    // Where the bottom of the scale really sits, measured on this pump: its own gauge turns Low at
    // about 1.23 V and Dead at 1.18 V. A custom low of 1.00 therefore maps a cell the pump is already
    // beeping about to ~46 %, which is why the default is 1.20 - at 1.20 the same 1.23 V reads 10 %.
    // ApexPump caps the published percentage on a Low or Dead gauge regardless of this setting, so a
    // stored 1.00 can no longer publish a healthy-looking number over a beeping cell; it still makes
    // everything above the cap optimistic.
    //
    // `min` stays 1.0 on purpose. Nothing clamps a stored value to the key's range on this API
    // version - PreferencesImpl.get(DoublePreferenceKey) does not, and an AdaptiveDoublePreference
    // built in code carries no range test (its validator comes from XML attributes, so it is
    // TEST_NOCHECK here) - so raising min without migrating the stored value would show 1.20 in the
    // UI while the calculation kept using the 1.00 that is already stored.
    BatteryLowVoltage("apex_low_batt_vtg", 1.2, 1.0, 1.8),
    // Keep the high end at 1.5 V. Raising it towards a fresh alkaline cell's ~1.6 V looks like the
    // obvious correction, but the field evidence says otherwise: 1.50 V is the highest value this
    // pump has ever been seen to report. A recorded three-hour session had all 32 StatusV2 frames on
    // 0x96 = 1.50 V with the coarse gauge on Full, and a later night log showed the byte moving in
    // 10 mV steps once the cell was below ~1.27 V - so it is a real reading with a firmware ceiling,
    // not a stuck decode. With high = 1.6 a brand-new battery would sit at 75 % forever, never reach
    // 100 %, and every pump-battery automation threshold would shift downward.
    BatteryHighVoltage("apex_high_batt_vtg", 1.5, 1.0, 1.8),
}
