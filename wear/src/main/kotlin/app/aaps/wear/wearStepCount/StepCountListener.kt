package app.aaps.wear.wearStepCount

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.annotation.VisibleForTesting
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.core.interfaces.rx.weardata.EventData
import app.aaps.wear.comm.IntentWearToMobile
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

class StepCountListener(
    private val ctx: Context,
    private val aapsLogger: AAPSLogger,
    aapsSchedulers: AapsSchedulers
) : SensorEventListener, Disposable {

    private val samplingIntervalMillis = 40_000L

    // Written from the sensor thread, read from the io scheduler thread in send().
    // A plain map throws ConcurrentModificationException when a sensor event arrives
    // while getStepsInLastXMin() iterates over the entries.
    private val stepsMap: MutableMap<Long, Int> = ConcurrentHashMap()
    private val fiveMinutesInMs = 300000
    private val numOf5MinBlocksToKeep = 40

    // Written from the sensor thread, read from the io scheduler thread in send().
    @Volatile private var previousStepCount = -1
    private var schedule: Disposable? = null

    init {
        aapsLogger.info(LTag.WEAR, "Create ${javaClass.simpleName}")
        val sensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        if (sensorManager == null) {
            aapsLogger.warn(LTag.WEAR, "Cannot get sensor manager to get steps rate readings")
        } else {
            // Only the step counter is needed. The step detector and the accelerometer used to set a
            // "movement seen" flag, but that flag was already true after the first accelerometer
            // sample - the reading includes gravity, which is far above the threshold that was
            // tested - and nothing ever set it back to false. Both sensors ran all day for nothing;
            // the accelerometer alone woke the watch about five times a second.
            sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
        schedule = aapsSchedulers.io.schedulePeriodicallyDirect(
            ::send, samplingIntervalMillis, samplingIntervalMillis, TimeUnit.MILLISECONDS
        )
    }

    var sendStepsRate: (EventData.ActionStepsRate) -> Unit = { steps ->
        aapsLogger.info(LTag.WEAR, "sendStepsRate called")
        ctx.startService(IntentWearToMobile(ctx, steps))
    }

    override fun isDisposed() = schedule == null

    override fun dispose() {
        aapsLogger.info(LTag.WEAR, "Dispose ${javaClass.simpleName}")
        schedule?.dispose()
        (ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager?)?.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.i(LTag.WEAR.name, "onAccuracyChanged: Sensor: $sensor; accuracy: $accuracy")
    }

    private fun currentTimeIn5Min(): Long {
        return (System.currentTimeMillis() / fiveMinutesInMs.toDouble()).roundToLong()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor?.type == Sensor.TYPE_STEP_COUNTER && event.values.isNotEmpty()) {
            val now = currentTimeIn5Min()
            val stepCount = event.values[0].toInt()
            if (previousStepCount >= 0) {
                var recentStepCount = stepCount - previousStepCount
                if (stepsMap.containsKey(now)) {
                    recentStepCount += stepsMap.getValue(now)
                }
                stepsMap[now] = recentStepCount
            }
            previousStepCount = stepCount

            if (stepsMap.size > numOf5MinBlocksToKeep) {
                val removeBefore = now - numOf5MinBlocksToKeep
                stepsMap.entries.removeIf { it.key < removeBefore }
            }
        }
    }

    private fun send() {
        send(System.currentTimeMillis())
    }

    private fun getStepsInLast5Min(): Int {
        val now = currentTimeIn5Min() - 1
        return if (stepsMap.containsKey(now)) stepsMap.getValue(now) else 0
    }

    private fun getStepsInLast10Min(): Int {
        val tenMinAgo = currentTimeIn5Min() - 2
        return if (stepsMap.containsKey(tenMinAgo)) stepsMap.getValue(tenMinAgo) else 0
    }

    private fun getStepsInLast15Min(): Int {
        val fifteenMinAgo = currentTimeIn5Min() - 3
        return if (stepsMap.containsKey(fifteenMinAgo)) stepsMap.getValue(fifteenMinAgo) else 0
    }

    private fun getStepsInLast30Min(): Int {
        return getStepsInLastXMin(6)
    }

    private fun getStepsInLast60Min(): Int {
        return getStepsInLastXMin(12)
    }

    private fun getStepsInLast180Min(): Int {
        return getStepsInLastXMin(36)
    }

    /**
     * Sends one sample with every window.
     *
     * Nothing is sent before the step counter has reported at least once. A sample of zeros from a
     * pedometer that never spoke would look like a wearer sitting still, and the phone raises the
     * profile when it sees that. With no sample at all the phone knows the feed is not there.
     */
    @VisibleForTesting
    fun send(timestampMillis: Long) {
        if (previousStepCount < 0) {
            aapsLogger.debug(LTag.WEAR, "No step counter reading yet, nothing to send")
            return
        }
        val stepsInLast5Minutes = getStepsInLast5Min()
        val stepsInLast10Minutes = getStepsInLast10Min()
        val stepsInLast15Minutes = getStepsInLast15Min()
        val stepsInLast30Minutes = getStepsInLast30Min()
        val stepsInLast60Minutes = getStepsInLast60Min()
        val stepsInLast180Minutes = getStepsInLast180Min()

        aapsLogger.debug(LTag.WEAR, "Steps in last 5 minutes: $stepsInLast5Minutes")
        aapsLogger.debug(LTag.WEAR, "Steps in last 10 minutes: $stepsInLast10Minutes")
        aapsLogger.debug(LTag.WEAR, "Steps in last 15 minutes: $stepsInLast15Minutes")
        aapsLogger.debug(LTag.WEAR, "Steps in last 30 minutes: $stepsInLast30Minutes")
        aapsLogger.debug(LTag.WEAR, "Steps in last 60 minutes: $stepsInLast60Minutes")
        aapsLogger.debug(LTag.WEAR, "Steps in last 180 minutes: $stepsInLast180Minutes")

        val device = (Build.MANUFACTURER ?: "unknown") + " " + (Build.MODEL ?: "unknown")

        // One message with every window. The phone writes the per-window rows from it, so every
        // reader still finds the row it looks for. One field differs from the six-message shape:
        // the 60 minute row used to carry the 180 minute count as well, which nothing reads and
        // which the phone step sampler never wrote either.
        sendStepsRate(
            EventData.ActionStepsRate(
                duration = EventData.ActionStepsRate.DURATION_ALL_WINDOWS_MS,
                timestamp = timestampMillis,
                steps5min = stepsInLast5Minutes,
                steps10min = stepsInLast10Minutes,
                steps15min = stepsInLast15Minutes,
                steps30min = stepsInLast30Minutes,
                steps60min = stepsInLast60Minutes,
                steps180min = stepsInLast180Minutes,
                device = device
            )
        )
    }

    private fun getStepsInLastXMin(numberOf5MinIncrements: Int): Int {
        var stepCount = 0
        // One reading of the clock: two would land in different blocks at a block boundary.
        val now = currentTimeIn5Min()
        val from = now - numberOf5MinIncrements
        for (entry in stepsMap.entries) {
            if (entry.key in (from + 1)..now) {
                stepCount += entry.value
            }
        }
        return stepCount
    }

}
