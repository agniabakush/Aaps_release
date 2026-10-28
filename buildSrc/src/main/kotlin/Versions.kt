import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

@Suppress("ConstPropertyName")
object Versions {

    const val appVersion = "3.4.2.6"

    // Build label appended to the version number. The version number (appVersion) may change
    // freely (e.g. 3.4.2.1, 3.3.2.0); the label stays constant for this fork.
    // buildName is what shows up as the build name, in the APK file name and as the Google Drive folder.
    const val buildLabel = "Boost_Vladimir"
    val buildName = "${appVersion}_$buildLabel" // e.g. 3.4.2.6_Boost_Vladimir

    const val versionCode = 1500

    const val customPatchVersion = "beta"

    const val ndkVersion = "21.1.6352462"
    // Boost version, uploaded to Nightscout as apsConfiguration.boostVersion with the branch and
    // short hash appended at build time (e.g. 7.4.0+Boost-V7-shadow.9632514dfa). Numbering:
    // <engine>.<minor>.<patch>, engine 6 on dev/master (which share a number), 6.x.x.E on
    // Boost-endurance, 7 on the V7-shadow vehicle. Raise minor for any change to dosing behaviour,
    // patch for fixes and telemetry that leave dosing unchanged. Set by hand on each branch.
    const val boostVersion = "6.2.0"

    const val compileSdk = 36
    const val minSdk = 31
    const val targetSdk = 32
    const val wearMinSdk = 30
    const val wearTargetSdk = 30

    val javaVersion = JavaVersion.VERSION_21
    val jvmTarget = JvmTarget.JVM_21
    const val jacoco = "0.8.11"
}
