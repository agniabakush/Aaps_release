package app.aaps.wear.wearStepCount

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.core.interfaces.rx.weardata.EventData.ActionStepsRate
import app.aaps.wear.AAPSLoggerTest
import com.google.common.truth.Truth.assertThat
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

/**
 * Tests for [StepCountListener].
 *
 * The listener reads [System.currentTimeMillis] directly, and its blocks are five minutes wide, so
 * within one test the current block is stable except at the rare boundary instant. Assertions that
 * depend on the current block are guarded with [sameBlockAround] and fall back to a structural check
 * if the boundary was crossed, so the suite never fails on timing.
 */
internal class StepCountListenerTest {

    private val fiveMinutesInMs = 300000L
    private val aapsLogger = AAPSLoggerTest()
    private val aapsSchedulers = object : AapsSchedulers {
        override val main: Scheduler = mock()
        override val io: Scheduler = mock()
        override val cpu: Scheduler = mock()
        override val newThread: Scheduler = mock()
    }
    private val schedule: Disposable = mock()
    private val device = "unknown unknown"
    private val emitted = mutableListOf<ActionStepsRate>()

    private val stepCounterSensor: Sensor = mock()
    private val stepDetectorSensor: Sensor = mock()
    private val accelerometerSensor: Sensor = mock()
    private val sensorManager: SensorManager = mock()

    private fun create(withSensorManager: Boolean = false): StepCountListener {
        val ctx: Context = mock()
        if (withSensorManager) {
            whenever(ctx.getSystemService(Context.SENSOR_SERVICE)).thenReturn(sensorManager)
            whenever(sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)).thenReturn(stepCounterSensor)
            whenever(sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)).thenReturn(stepDetectorSensor)
            whenever(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(accelerometerSensor)
        }
        // Without a sensor manager (the mock returns null) init logs a warning and skips registration.
        whenever(
            aapsSchedulers.io.schedulePeriodicallyDirect(
                any(), eq(40_000L), eq(40_000L), eq(TimeUnit.MILLISECONDS)
            )
        ).thenReturn(schedule)
        val listener = StepCountListener(ctx, aapsLogger, aapsSchedulers)
        listener.sendStepsRate = { steps -> emitted.add(steps) }
        return listener
    }

    /** Mirrors the private currentTimeIn5Min() so a test can detect a block boundary crossing. */
    private fun currentBlock(): Long = (System.currentTimeMillis() / fiveMinutesInMs.toDouble()).roundToLong()

    /** Runs [block] and reports whether the five-minute block stayed the same across the call. */
    private inline fun sameBlockAround(block: () -> Unit): Boolean {
        val before = currentBlock()
        block()
        return before == currentBlock()
    }

    private fun stepCounterEvent(cumulativeSteps: Int): SensorEvent =
        sensorEvent(Sensor.TYPE_STEP_COUNTER, floatArrayOf(cumulativeSteps.toFloat()))

    private fun sensorEvent(type: Int, values: FloatArray): SensorEvent {
        val sensor: Sensor = mock()
        whenever(sensor.type).thenReturn(type)
        // SensorEvent has no public constructor and its .sensor/.values are public framework fields
        // (not getters), so they cannot be stubbed with whenever(). Instantiate via Mockito (Objenesis,
        // no constructor call) and assign the fields reflectively; field reads are not intercepted.
        val event: SensorEvent = mock()
        setSensorEventField(event, "sensor", sensor)
        setSensorEventField(event, "values", values)
        return event
    }

    private fun setSensorEventField(event: SensorEvent, name: String, value: Any?) {
        val field = SensorEvent::class.java.getField(name)
        field.isAccessible = true
        field.set(event, value)
    }

    @BeforeEach
    fun before() {
        emitted.clear()
    }

    @Test
    fun onlyTheStepCounterIsRegistered() {
        val listener = create(withSensorManager = true)

        verify(sensorManager).registerListener(listener, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL)
        verify(sensorManager, never()).registerListener(listener, stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL)
        verify(sensorManager, never()).registerListener(listener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
        listener.dispose()
    }

    @Test
    fun nothingIsSentBeforeTheStepCounterReported() {
        val listener = create()

        listener.send(1_700_000_000_000L)

        // A sample of zeros from a pedometer that never spoke would look like a wearer sitting
        // still, and the phone raises the profile when it sees that.
        assertThat(emitted).isEmpty()
        listener.dispose()
    }

    @Test
    fun oneSampleCarriesEveryWindow() {
        val listener = create()
        val timestamp = 1_700_000_000_000L
        listener.onSensorChanged(stepCounterEvent(1000))

        listener.send(timestamp)

        assertThat(emitted).hasSize(1)
        val sample = emitted.single()
        assertThat(sample.duration).isEqualTo(ActionStepsRate.DURATION_ALL_WINDOWS_MS)
        assertThat(sample.timestamp).isEqualTo(timestamp)
        assertThat(sample.device).isEqualTo(device)
        listener.dispose()
    }

    @Test
    fun firstCounterReadingOnlySetsTheBaseline() {
        val listener = create()

        listener.onSensorChanged(stepCounterEvent(1000))
        listener.send(1L)

        val sample = emitted.single()
        assertThat(sample.steps5min).isEqualTo(0)
        assertThat(sample.steps30min).isEqualTo(0)
        assertThat(sample.steps60min).isEqualTo(0)
        assertThat(sample.steps180min).isEqualTo(0)
        listener.dispose()
    }

    @Test
    fun countingStartsWithoutAnyMovementSensor() {
        val listener = create()

        val stable = sameBlockAround {
            listener.onSensorChanged(stepCounterEvent(1000)) // baseline only
            listener.onSensorChanged(stepCounterEvent(1120)) // delta 120
            listener.send(1L)
        }

        val sample = emitted.single()
        // The 30 minute window includes the current block, so the delta shows there at once.
        if (stable) assertThat(sample.steps30min).isEqualTo(120)
        else assertThat(sample.steps30min).isAtLeast(0)
        listener.dispose()
    }

    @Test
    fun deltasInTheSameBlockAreAddedUp() {
        val listener = create()

        val stable = sameBlockAround {
            listener.onSensorChanged(stepCounterEvent(2000)) // baseline
            listener.onSensorChanged(stepCounterEvent(2010)) // +10
            listener.onSensorChanged(stepCounterEvent(2035)) // +25
            listener.onSensorChanged(stepCounterEvent(2100)) // +65
            listener.send(3L)
        }

        val sample = emitted.single()
        // 10 + 25 + 65 = 100 in the one current block.
        if (stable) assertThat(sample.steps180min).isEqualTo(100)
        else assertThat(sample.steps180min).isAtLeast(0)
        listener.dispose()
    }
}
