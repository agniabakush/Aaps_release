package app.aaps.pump.apex.utils.keys

import app.aaps.core.keys.interfaces.BooleanPreferenceKey

enum class ApexBooleanKey(
    override val key: String,
    override val defaultValue: Boolean,
    override val calculatedDefaultValue: Boolean = false,
    override val engineeringModeOnly: Boolean = false,
    override val defaultedBySM: Boolean = false,
    override val showInApsMode: Boolean = true,
    override val showInNsClientMode: Boolean = true,
    override val showInPumpControlMode: Boolean = true,
    override val dependency: BooleanPreferenceKey? = null,
    override val negativeDependency: BooleanPreferenceKey? = null,
    override val hideParentScreenIfHidden: Boolean = false,
    override val exportable: Boolean = true
) : BooleanPreferenceKey {
    LogInsulinChange("apex_log_insulin_change", true, defaultedBySM = true),
    LogBatteryChange("apex_log_battery_change", true, defaultedBySM = true),
    CalculateBatteryPercentage("apex_calc_battery_percentage", true, defaultedBySM = true),
    HideSerial("apex_hide_serial", true, defaultedBySM = true),

    /**
     * Read the pump's fill history once per service run and write it to the log. Nothing else: no
     * therapy event, no database row, no card.
     *
     * Off by default and engineering-mode only, because the record layout has never been seen from a
     * real pump - it is reconstructed from the maker's app - and because the read costs time on the
     * connect path where an insulin command may be waiting. Switch it on, collect one log after a
     * cartridge change, switch it off again. What the log shows is what a real decoder would have to
     * be built on.
     */
    ReadFillHistory(
        "apex_read_fill_history", false,
        engineeringModeOnly = true,
    ),

    /**
     * Whether a connection password is set on the pump itself.
     *
     * Off means the driver does not open the password service at all - today's behaviour, byte for
     * byte. On means it sends [ApexStringKey.PumpPassword] during the handshake, before the data
     * channel is subscribed, because the pump refuses that subscription until it has seen a valid
     * password. It gates nothing else that this driver decides: dosing is never blocked on the reply
     * byte. A pump with a real password enforces the block itself by refusing the data channel; a pump
     * without one opens it regardless.
     */
    PumpHasPassword("apex_pump_has_password", false),
}
