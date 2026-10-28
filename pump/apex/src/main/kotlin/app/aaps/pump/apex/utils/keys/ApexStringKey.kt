package app.aaps.pump.apex.utils.keys

import app.aaps.core.keys.interfaces.BooleanPreferenceKey
import app.aaps.core.keys.interfaces.StringPreferenceKey
import app.aaps.pump.apex.connectivity.FirmwareVersion
import app.aaps.pump.apex.connectivity.commands.pump.AlarmLength
import app.aaps.pump.apex.connectivity.commands.pump.BolusDeliverySpeed
import app.aaps.pump.apex.misc.BatteryType

enum class ApexStringKey(
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
    // Apex serial numbers are exactly 8 digits (the native pump app enforces the same); empty = not configured yet.
    // On this API version a key carries no validator, so the 8-digit check lives on the preference screen.
    SerialNumber("apex_serial_number", ""),

    /**
     * The pump's connection password: exactly six digits.
     *
     * Shown in clear, NOT as a password field, and that is deliberate. The pump has no recovery path -
     * the maker's own "forgot password" string exists in its resources and is referenced by no code -
     * so an owner who sets a password must be able to read back what they set. Masking it would make
     * a forgotten password unrecoverable, which is a worse outcome than a value on a settings screen
     * that only reaches the pump it belongs to.
     *
     * Kept in the settings export for the same reason: losing or replacing the phone is far more
     * likely than an encrypted export being stolen.
     *
     * The default is the maker's own convention for "no password set" - its app substitutes six zeros
     * when nothing is stored. With the toggle on and nothing typed, the driver therefore sends exactly
     * what the maker's app sends to a pump that has no password.
     *
     * The screen validator must REQUIRE six digits, not merely accept a well formed value: an empty
     * password would be built into a frame that is not six digits. The code that builds the frame
     * validates the length again, because a settings import writes preferences directly and never sees
     * the screen validator.
     */
    // Shown only while PumpHasPassword is on, but NOT through `dependency`: a settings screen rebuilds
    // the whole activity whenever the key that was just changed has something declaring a dependency on
    // it, and that throws the reader back to the top of a long screen. ApexPumpPlugin.preprocessPreferences
    // sets the same visibility in place instead, on the first build and after every change alike.
    PumpPassword("apex_pump_password", "000000"),

    // Both are device identity discovered at runtime, never user configuration, and both are left out
    // of a settings export.
    //
    // What that actually buys, honestly: on a SECOND phone the imported values would be wrong, and the
    // cached address short-circuits scanning, so that phone would keep dialling a pump it will never
    // reach. Leaving them out avoids that.
    //
    // What it does NOT buy: it is not a way of keeping the pump out of the export. The serial travels
    // anyway, in SerialNumber above and in the core's own active_pump_serial_number, and the pump
    // broadcasts the same serial as its BLE name to anything listening. Nothing here is a secret.
    //
    // What it costs: an import runs sp.clear(), and this flag does NOT protect a value from that, so
    // the values would be erased on the SAME phone too. `ApexPumpPlugin.beforeImport`/`afterImport`
    // now carry every non-exportable key here across the clear, which is what keeps the next connect
    // from treating a pump it has been talking to for months as a new one. A restored address cannot
    // get stuck: it is dropped on a serial change, on a mismatch at connect, and after three
    // connections that never identified as a pump.
    LastConnectedSerialNumber("apex_last_connected_serial_number", "", exportable = false),
    BluetoothAddress("apex_bt_address", "", exportable = false),

    /**
     * Epoch millis, as a decimal string, from which bolus history entries are keyed with the current
     * pumpId formula. Entries older than this keep the previous formula so that rows already in the
     * database are still matched instead of being inserted a second time. Set once, on the first
     * history entry processed after the formula changed; empty means "not yet stamped".
     */
    PumpIdFormatCutover("apex_pumpid_format_cutover", "", exportable = false),

    /**
     * A bolus that was sent to the pump but whose row has not been matched against the pump's own history
     * entry yet, as `temporaryId|timestamp|rawRequestedDose|bolusType`; empty means there is none.
     *
     * The match normally runs off `ApexPump.inProgressBolus`, which lives in memory only. If the process
     * dies in between - a link drop during a bolus is exactly when that happens - the match is skipped and
     * the history entry is stored under its pumpId as a NEW row, so the same insulin is counted twice in
     * IOB, for good. Persisting the few fields the match needs lets it still run after a restart.
     */
    PendingBolusSync("apex_pending_bolus_sync", "", exportable = false),

    // The three list keys below hold only their stored value here. Their entry lists are declared on
    // the preference screen on this API version.
    AlarmSoundLength("apex_alarm_length", AlarmLength.Short.name),
    BolusSpeed("apex_bolus_speed", BolusDeliverySpeed.Standard.name),
    // Hidden while battery-% calc is off. Done in ApexPumpPlugin.preprocessPreferences rather than with
    // `dependency`, because a declared dependency makes the settings screen rebuild the whole activity
    // when its switch is touched, which throws the reader back to the top - see [PumpPassword].
    CalcBatteryType("apex_battery_type", BatteryType.Custom.name),
    FirmwareVer("apex_fw_ver", FirmwareVersion.AUTO.name),
}
