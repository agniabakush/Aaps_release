package app.aaps.plugins.sync.metrics

import android.os.Build
import app.aaps.core.data.model.HR
import app.aaps.core.data.model.SC
import app.aaps.core.data.time.T
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.metrics.MetricsSampler
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.plugins.sync.steps.StepWindowRows
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.max
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-demand sampler honouring the source priority chain wear/Garmin -> Health Connect -> phone sensor.
 *
 * For steps it writes one [SC] row per time window (mirroring the AAPS Wear mechanism) so existing
 * consumers (overview graph, [app.aaps.plugins.automation] steps trigger, Nightscout) work unchanged.
 * Own writes are tagged via the [SC.device] / [HR.device] field so the standard mechanism is never
 * overwritten.
 */
@Singleton
class MetricsSamplerImpl @Inject constructor(
    private val aapsLogger: AAPSLogger,
    private val persistenceLayer: PersistenceLayer,
    private val dateUtil: DateUtil,
    private val healthConnectSource: HealthConnectSource,
    private val phoneStepSensor: PhoneStepSensor
) : MetricsSampler {

    private companion object {

        const val DEVICE_HEALTH_CONNECT = "Health Connect"
        val WINDOWS_MIN = StepWindowRows.WINDOW_MINUTES
        val SKIP_WINDOW_MS = T.mins(6).msecs()
        val BUCKET_MS = T.mins(5).msecs()
        val HISTORY_MS = T.mins(180).msecs()

        /**
         * Longest sampling gap still worth attributing. Beyond it every window we publish is stale
         * anyway, so the counter is simply re-baselined. Kept equal to [HISTORY_MS], the span the
         * increments are read back over.
         */
        val MAX_ATTRIBUTION_MS = HISTORY_MS

        /** Upper sanity bound for one ~5-min bucket delta. A sprinter tops out near ~3 steps/s
         *  (~900 in 5 min); anything past this is a glitching/garbage hardware counter, not walking.
         *  Deltas above it (including an Int-overflowing raw value) are dropped, not clamped, so the
         *  graph scale and the Boost step windows never see a spike. */
        const val MAX_PLAUSIBLE_BUCKET_STEPS = 1500L
    }

    private val phoneSensorDevice = "${Build.MANUFACTURER} ${Build.MODEL} sensor"
    private val ownStepsDevices = setOf(DEVICE_HEALTH_CONNECT, phoneSensorDevice)

    /** Baseline cumulative step-counter value for delta computation in the phone-sensor fallback. */
    private var lastCumulative: Long? = null

    /** Timestamp of the previous phone-sensor sample, to detect sampling gaps. */
    private var lastSampleTime: Long? = null

    override fun sampleNow() {
        runCatching { sampleSteps() }.onFailure { aapsLogger.error(LTag.CORE, "sampleSteps failed", it) }
        runCatching { sampleHeartRate() }.onFailure { aapsLogger.error(LTag.CORE, "sampleHeartRate failed", it) }
    }

    private fun sampleSteps() {
        val now = dateUtil.now()
        // 1. Standard mechanism (AAPS Wear) already provided a recent value -> leave it alone.
        if (persistenceLayer.getStepsCountFromTime(now - SKIP_WINDOW_MS).any { it.device !in ownStepsDevices }) {
            aapsLogger.debug(LTag.CORE, "Steps: standard mechanism present, sampler skips")
            return
        }
        // 2. PHONE STEP COUNTER — preferred over Health Connect (2026-07-31). HC aggregates every
        //    app that writes steps, with no dataOrigin filter, so on a phone with a vendor health
        //    app (Mi Fitness and friends) it reports that app's motion-fused count rather than the
        //    hardware pedometer. It also returns a non-null map of zeros whenever the permission is
        //    granted but nothing has been written, which used to short-circuit this branch entirely
        //    — once READ_STEPS was granted (the Boost activity shadow asks for it) the phone sensor
        //    was dead code. The phone owns the source whenever it has a counter at all; a
        //    re-baseline or a rejected sample writes nothing rather than handing the cycle to HC,
        //    so the two sources can never interleave.
        phoneStepSensor.readCumulativeSteps()?.let { cumulative ->
            samplePhoneSteps(now, cumulative)
            return
        }
        // 3. Health Connect fallback — no phone counter (absent, or ACTIVITY_RECOGNITION not
        //    granted). An all-zero map is treated as no data so a silent HC provider cannot bank
        //    authoritative-looking zeros.
        healthConnectSource.readSteps(Instant.ofEpochMilli(now), WINDOWS_MIN)
            ?.takeIf { hc -> hc.values.any { it > 0 } }
            ?.let { hc -> storeSteps(now, hc, DEVICE_HEALTH_CONNECT) }
    }

    /**
     * Steps from the phone's cumulative counter: bank this sample's increment and re-sum the windows.
     *
     * The windows are built by summing RAW INCREMENTS ([SC.DURATION_RAW_INCREMENT_MS]), never stored
     * window totals. Until 2026-07-31 this sample's increment was added to the previously stored
     * 5-min row, whose value is itself a window sum — so the quantity fed back into itself. Because
     * the sampler runs inside the MAIN_CALCULATION chain rather than on a timer, samples closer
     * together than five minutes are the norm, and there a single phantom step was carried forward
     * unchanged indefinitely (under ~2.5 min it compounded): the overnight "drizzle" of identical
     * bars on a motionless phone. Increments never overlap, so summing them is correct and a quiet
     * five minutes decays every window back to zero.
     */
    private fun samplePhoneSteps(now: Long, cumulative: Long) {
        val previous = lastCumulative
        val previousTime = lastSampleTime
        lastCumulative = cumulative
        lastSampleTime = now
        if (previous == null || cumulative < previous) return // first sample or reboot -> re-baseline only
        if (previousTime == null) return
        // The sampler is driven by the MAIN_CALCULATION chain, not a timer, and OEM background
        // freezing (ColorOS/MIUI) routinely stretches the gap well past one bucket: on the test
        // device four gaps of 7.7-23 min occurred in 2.5 h. Discarding the delta there — as this
        // did until 2026-08-01 — threw those steps away for good, leaving holes in the graph and a
        // count below what the phone's own health app shows. Attribute them instead, spread evenly
        // over the bucket slots the gap covers, so both the total and roughly the timing survive.
        // Only a gap longer than every window we publish is still worth re-baselining.
        val gapMs = now - previousTime
        if (gapMs > MAX_ATTRIBUTION_MS) {
            aapsLogger.debug(LTag.CORE, "Steps: gap ${gapMs / 60_000} min exceeds attribution window, re-baselining")
            return
        }
        // Reject an implausible forward jump before it can overflow Int or poison the windows: a
        // glitching hardware TYPE_STEP_COUNTER (seen reporting hundreds of millions) would otherwise
        // write garbage into the SC rows the graph and Boost engine read. The bound is per bucket,
        // so it scales with the gap and stays a walking-rate sanity check rather than a cap on how
        // long the app may have been frozen. Computed in Long so an out-of-Int-range delta is
        // caught, not silently wrapped to a negative.
        val slots = max(1, ceil(gapMs.toDouble() / BUCKET_MS).toInt())
        val deltaLong = cumulative - previous
        if (deltaLong > MAX_PLAUSIBLE_BUCKET_STEPS * slots) {
            aapsLogger.debug(LTag.CORE, "Steps: implausible delta $deltaLong over $slots bucket(s) from $phoneSensorDevice, re-baselining")
            return
        }
        val delta = deltaLong.toInt()
        // Spread across the gap, remainder into the most recent slots. With the usual sub-5-min
        // cadence slots == 1 and this is simply the increment stamped at `now`.
        val perSlot = delta / slots
        val remainder = delta % slots
        val increments = (0 until slots).map { i ->
            (now - (slots - 1 - i) * BUCKET_MS) to (perSlot + if (i >= slots - remainder) 1 else 0)
        }
        // Sum this sample's increments with those already banked in each window. Increments are
        // NON-OVERLAPPING by construction (see [SC.DURATION_RAW_INCREMENT_MS]), so the sums are true
        // rolling totals that decay to zero after a quiet five minutes. The value bound also drops
        // any garbage row left by an older build.
        val ownIncrements = persistenceLayer.getStepsCountFromTime(now - HISTORY_MS)
            .filter {
                it.device == phoneSensorDevice && it.duration == SC.DURATION_RAW_INCREMENT_MS &&
                    it.steps5min in 0..MAX_PLAUSIBLE_BUCKET_STEPS.toInt()
            }
        val windows = WINDOWS_MIN.associateWith { minutes ->
            val from = now - minutes * 60_000
            increments.filter { it.first > from }.sumOf { it.second } +
                ownIncrements.filter { it.timestamp > from }.sumOf { it.steps5min }
        }
        storeSteps(now, windows, phoneSensorDevice, increments)
        // raw= makes the OEM pedometer's behaviour readable straight from logcat: consecutive raw
        // values differ by exactly what the hardware counted, so a phone that ticks while lying
        // still can be measured rather than guessed at.
        aapsLogger.debug(LTag.CORE, "Steps: raw=$cumulative delta=$delta gap=${gapMs / 1000}s slots=$slots")
    }

    /**
     * Writes the shared per-window rows ([StepWindowRows]) plus — for the phone-sensor path —
     * the private raw-increment rows described at
     * [SC.DURATION_RAW_INCREMENT_MS]. An increment is always <= the 5-min window containing it and
     * the overview graph filters these rows out, so they can never draw a second bar or win a
     * `maxOf` in a consumer. A gap longer than one bucket yields several, stamped back across it.
     */
    private fun storeSteps(now: Long, w: Map<Long, Int>, device: String, rawIncrements: List<Pair<Long, Int>> = emptyList()) {
        fun s(min: Long) = w[min] ?: 0
        val rows = rawIncrements.map { (at, steps) ->
            SC(duration = SC.DURATION_RAW_INCREMENT_MS, timestamp = at, steps5min = steps, steps10min = 0, steps15min = 0, steps30min = 0, steps60min = 0, steps180min = 0, device = device)
        } + StepWindowRows.build(now, device, w)
        rows.forEach { persistenceLayer.insertOrUpdateStepsCount(it).blockingGet() }
        aapsLogger.debug(LTag.CORE, "Stored steps from $device: 5m=${s(5)} 15m=${s(15)} 30m=${s(30)} 60m=${s(60)}")
    }

    private fun sampleHeartRate() {
        val now = dateUtil.now()
        // 1. Standard mechanism (Garmin / AAPS Wear) already provided a recent value -> leave it alone.
        if (persistenceLayer.getHeartRatesFromTime(now - SKIP_WINDOW_MS).any { it.device != DEVICE_HEALTH_CONNECT }) {
            aapsLogger.debug(LTag.CORE, "HR: standard mechanism present, sampler skips")
            return
        }
        // 2. Health Connect average over the last 5 minutes.
        val bpm = healthConnectSource.readAverageHeartRate(Instant.ofEpochMilli(now), 5L) ?: return
        if (bpm <= 0) return
        val hr = HR(
            timestamp = now - BUCKET_MS,
            duration = BUCKET_MS,
            beatsPerMinute = bpm.toDouble(),
            device = DEVICE_HEALTH_CONNECT
        )
        persistenceLayer.insertOrUpdateHeartRate(hr).blockingGet()
        aapsLogger.debug(LTag.CORE, "Stored HR from Health Connect: $bpm BPM")
    }
}
