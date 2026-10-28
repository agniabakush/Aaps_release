package app.aaps.plugins.sync.metrics

import app.aaps.core.data.model.SC
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.shared.tests.TestBase
import com.google.common.truth.Truth.assertThat
import io.reactivex.rxjava3.core.Single
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import java.time.Instant

/**
 * Step-sampling regression tests for [MetricsSamplerImpl].
 *
 * The behaviour under test is the 2026-07-31 fix for the overnight "drizzle": before it, the value
 * stored in the 5-minute row was itself a window sum and was then re-read and re-added on the next
 * sample. Because the sampler runs inside the MAIN_CALCULATION chain rather than on a timer, gaps
 * under five minutes are normal, and there a single phantom step was carried forward unchanged
 * indefinitely (under ~2.5 min it compounded) with a completely motionless phone.
 */
class MetricsSamplerStepsTest : TestBase() {

    @Mock lateinit var persistenceLayer: PersistenceLayer
    @Mock lateinit var dateUtil: DateUtil
    @Mock lateinit var healthConnectSource: HealthConnectSource
    @Mock lateinit var phoneStepSensor: PhoneStepSensor

    private lateinit var sampler: MetricsSamplerImpl

    /** In-memory stand-in for the SC table. */
    private val stored = mutableListOf<SC>()
    private var clock = 1_700_000_000_000L
    private var cumulative = 10_000L

    private fun windowRow(min: Long): SC? =
        stored.filter { it.duration == min * 60_000L }.maxByOrNull { it.timestamp }

    /** Runs one sampler cycle [minutesLater] after the previous one, with [steps] new hardware steps. */
    private fun sample(minutesLater: Double, steps: Long) {
        clock += (minutesLater * 60_000).toLong()
        cumulative += steps
        whenever(phoneStepSensor.readCumulativeSteps()).thenReturn(cumulative)
        sampler.sampleNow()
    }

    @BeforeEach
    fun setup() {
        whenever(dateUtil.now()).thenAnswer { clock }
        whenever(persistenceLayer.getStepsCountFromTime(any())).thenAnswer { inv ->
            val from = inv.getArgument<Long>(0)
            stored.filter { it.timestamp >= from }
        }
        whenever(persistenceLayer.insertOrUpdateStepsCount(any())).thenAnswer { inv ->
            stored.add(inv.getArgument(0))
            Single.just(PersistenceLayer.TransactionResult<SC>())
        }
        // No heart-rate source and no Health Connect steps unless a test says otherwise.
        whenever(healthConnectSource.readAverageHeartRate(anyOrNull(), any())).thenReturn(null)
        whenever(healthConnectSource.readSteps(anyOrNull(), any())).thenReturn(null)
        sampler = MetricsSamplerImpl(aapsLogger, persistenceLayer, dateUtil, healthConnectSource, phoneStepSensor)
    }

    @Test
    fun `a single step does not drizzle - every window decays back to zero`() {
        sample(minutesLater = 0.0, steps = 0)   // first sample only re-baselines
        sample(minutesLater = 2.0, steps = 1)   // one phantom step
        assertThat(windowRow(5)!!.steps5min).isEqualTo(1)

        // Twenty motionless samples at the 2-minute cadence that used to latch the value forever.
        repeat(20) { sample(minutesLater = 2.0, steps = 0) }

        // 40 minutes on, the one step has aged out of the short windows in turn and is still
        // correctly present in the long ones — counted ONCE, not carried forward at every sample.
        assertThat(windowRow(5)!!.steps5min).isEqualTo(0)
        assertThat(windowRow(30)!!.steps30min).isEqualTo(0)
        assertThat(windowRow(60)!!.steps60min).isEqualTo(1)
        assertThat(windowRow(180)!!.steps180min).isEqualTo(1)

        // Past the hour it leaves the 60-minute window too.
        repeat(12) { sample(minutesLater = 2.0, steps = 0) }
        assertThat(windowRow(60)!!.steps60min).isEqualTo(0)
    }

    @Test
    fun `sub-two-minute cadence does not compound`() {
        sample(minutesLater = 0.0, steps = 0)
        sample(minutesLater = 1.0, steps = 3)
        repeat(15) { sample(minutesLater = 1.0, steps = 0) }
        // The old code grew this by roughly the golden ratio per sample; it must simply age out.
        assertThat(windowRow(5)!!.steps5min).isEqualTo(0)
        assertThat(windowRow(30)!!.steps30min).isEqualTo(3)
    }

    @Test
    fun `windows are true rolling sums of the increments, counted once each`() {
        sample(minutesLater = 0.0, steps = 0)
        sample(minutesLater = 2.0, steps = 10)
        sample(minutesLater = 2.0, steps = 20)
        sample(minutesLater = 2.0, steps = 30)
        val row = windowRow(60)!!
        assertThat(row.steps60min).isEqualTo(60)
        // All three increments are stamped inside the trailing 5 minutes, so each is counted once
        // and exactly once. The old code would have reported far more than the 60 steps taken.
        assertThat(row.steps5min).isEqualTo(60)
    }

    @Test
    fun `the phone sensor is preferred over Health Connect`() {
        whenever(healthConnectSource.readSteps(anyOrNull(), any()))
            .thenReturn(mapOf(5L to 900, 10L to 900, 15L to 900, 30L to 900, 60L to 900, 180L to 900))
        sample(minutesLater = 0.0, steps = 0)
        sample(minutesLater = 2.0, steps = 7)
        assertThat(stored.map { it.device }.toSet()).doesNotContain("Health Connect")
        assertThat(windowRow(5)!!.steps5min).isEqualTo(7)
    }

    @Test
    fun `Health Connect is used when the phone has no step counter`() {
        whenever(phoneStepSensor.readCumulativeSteps()).thenReturn(null)
        whenever(healthConnectSource.readSteps(anyOrNull(), any()))
            .thenReturn(mapOf(5L to 12, 10L to 20, 15L to 25, 30L to 40, 60L to 80, 180L to 90))
        sampler.sampleNow()
        assertThat(windowRow(5)!!.device).isEqualTo("Health Connect")
        assertThat(windowRow(60)!!.steps60min).isEqualTo(80)
    }

    @Test
    fun `an all-zero Health Connect map is treated as no data`() {
        whenever(phoneStepSensor.readCumulativeSteps()).thenReturn(null)
        whenever(healthConnectSource.readSteps(anyOrNull(), any()))
            .thenReturn(mapOf(5L to 0, 10L to 0, 15L to 0, 30L to 0, 60L to 0, 180L to 0))
        sampler.sampleNow()
        assertThat(stored).isEmpty()
    }

    @Test
    fun `steps taken across a long sampling gap are kept, not discarded`() {
        // The device freezes background work for minutes at a time, so gaps past one bucket are
        // routine; discarding them left holes in the graph and a total below the phone's own count.
        sample(minutesLater = 0.0, steps = 0)
        sample(minutesLater = 12.0, steps = 300)
        val increments = stored.filter { it.duration == SC.DURATION_RAW_INCREMENT_MS }
        assertThat(increments.sumOf { it.steps5min }).isEqualTo(300)   // nothing lost
        assertThat(increments).hasSize(3)                             // spread over the 12-min gap
        assertThat(windowRow(30)!!.steps30min).isEqualTo(300)
        // …and back-dated rather than dumped on the newest bucket, so the 5-min window stays sane.
        assertThat(windowRow(5)!!.steps5min).isLessThan(300)
    }

    @Test
    fun `a gap longer than every published window re-baselines`() {
        sample(minutesLater = 0.0, steps = 0)
        sample(minutesLater = 2.0, steps = 10)
        sample(minutesLater = 200.0, steps = 5000)
        assertThat(stored.filter { it.duration == SC.DURATION_RAW_INCREMENT_MS }.sumOf { it.steps5min }).isEqualTo(10)
    }

    @Test
    fun `raw-increment rows are marked so the graph can exclude them`() {
        sample(minutesLater = 0.0, steps = 0)
        sample(minutesLater = 2.0, steps = 9)
        val marker = stored.filter { it.duration == SC.DURATION_RAW_INCREMENT_MS }
        assertThat(marker).hasSize(1)
        // Never taller than the window bar it belongs to - it must not become the graph's maximum.
        assertThat(marker.single().steps5min).isAtMost(windowRow(5)!!.steps5min)
        // The marker must not collide with any published window duration.
        assertThat(listOf(5L, 10L, 15L, 30L, 60L, 180L).map { it * 60_000L })
            .doesNotContain(SC.DURATION_RAW_INCREMENT_MS)
    }

    @Test
    fun `an implausible counter jump is dropped, not banked`() {
        sample(minutesLater = 0.0, steps = 0)
        sample(minutesLater = 2.0, steps = 5)
        sample(minutesLater = 2.0, steps = 500_000)
        // Nothing new was written, and the last good window is untouched.
        assertThat(windowRow(5)!!.steps5min).isEqualTo(5)
    }

    @Test
    fun `a counter reset at reboot re-baselines instead of writing a negative or huge delta`() {
        sample(minutesLater = 0.0, steps = 0)
        sample(minutesLater = 2.0, steps = 40)
        cumulative = 3                                  // reboot: counter restarts near zero
        clock += 120_000
        whenever(phoneStepSensor.readCumulativeSteps()).thenReturn(cumulative)
        sampler.sampleNow()
        assertThat(windowRow(5)!!.steps5min).isEqualTo(40)  // unchanged, nothing new banked
        // Counting resumes from the new baseline: the 6 post-reboot steps are added to the 40 taken
        // before it, each counted exactly once - the restart neither lost nor duplicated anything.
        sample(minutesLater = 5.5, steps = 6)
        assertThat(stored.filter { it.duration == SC.DURATION_RAW_INCREMENT_MS }.sumOf { it.steps5min }).isEqualTo(46)
        assertThat(windowRow(30)!!.steps30min).isEqualTo(46)
    }
}
