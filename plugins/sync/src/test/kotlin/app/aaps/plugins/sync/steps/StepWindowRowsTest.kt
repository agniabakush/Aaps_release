package app.aaps.plugins.sync.steps

import app.aaps.core.data.time.T
import app.aaps.core.interfaces.rx.weardata.EventData
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class StepWindowRowsTest {

    private val windows = mapOf(5L to 11, 10L to 22, 15L to 33, 30L to 44, 60L to 55, 180L to 66)

    @Test
    fun oneRowPerWindowInOrder() {
        val rows = StepWindowRows.build(timestamp = 1_700_000_000_000L, device = "watch", windows = windows)

        assertThat(rows).hasSize(6)
        assertThat(rows.map { it.duration }).containsExactly(
            T.mins(5).msecs(), T.mins(10).msecs(), T.mins(15).msecs(),
            T.mins(30).msecs(), T.mins(60).msecs(), T.mins(180).msecs()
        ).inOrder()
        rows.forEach {
            assertThat(it.timestamp).isEqualTo(1_700_000_000_000L)
            assertThat(it.device).isEqualTo("watch")
        }
    }

    @Test
    fun aRowCarriesItsOwnWindowAndEveryShorterOne() {
        val rows = StepWindowRows.build(timestamp = 1L, device = "watch", windows = windows)
        val d5 = rows[0]
        val d10 = rows[1]
        val d15 = rows[2]
        val d30 = rows[3]
        val d60 = rows[4]
        val d180 = rows[5]

        assertThat(listOf(d5.steps5min, d5.steps10min, d5.steps15min, d5.steps30min, d5.steps60min, d5.steps180min))
            .containsExactly(11, 0, 0, 0, 0, 0).inOrder()
        assertThat(listOf(d10.steps5min, d10.steps10min, d10.steps15min, d10.steps30min, d10.steps60min, d10.steps180min))
            .containsExactly(11, 22, 0, 0, 0, 0).inOrder()
        assertThat(listOf(d15.steps5min, d15.steps10min, d15.steps15min, d15.steps30min, d15.steps60min, d15.steps180min))
            .containsExactly(11, 22, 33, 0, 0, 0).inOrder()
        assertThat(listOf(d30.steps5min, d30.steps10min, d30.steps15min, d30.steps30min, d30.steps60min, d30.steps180min))
            .containsExactly(11, 22, 33, 44, 0, 0).inOrder()
        assertThat(listOf(d60.steps5min, d60.steps10min, d60.steps15min, d60.steps30min, d60.steps60min, d60.steps180min))
            .containsExactly(11, 22, 33, 44, 55, 0).inOrder()
        assertThat(listOf(d180.steps5min, d180.steps10min, d180.steps15min, d180.steps30min, d180.steps60min, d180.steps180min))
            .containsExactly(11, 22, 33, 44, 55, 66).inOrder()
    }

    @Test
    fun aMissingWindowCountsAsZero() {
        val rows = StepWindowRows.build(timestamp = 1L, device = "watch", windows = mapOf(5L to 7))

        assertThat(rows.last().steps5min).isEqualTo(7)
        assertThat(rows.last().steps180min).isEqualTo(0)
    }

    private fun message(duration: Long) = EventData.ActionStepsRate(
        duration = duration,
        timestamp = 1_700_000_000_000L,
        steps5min = 11,
        steps10min = 22,
        steps15min = 33,
        steps30min = 44,
        steps60min = 55,
        steps180min = 66,
        device = "watch"
    )

    @Test
    fun oneMessageWithEveryWindowBecomesSixRows() {
        val rows = StepWindowRows.fromWearMessage(message(EventData.ActionStepsRate.DURATION_ALL_WINDOWS_MS))

        assertThat(rows).hasSize(6)
        assertThat(rows.map { it.duration }).containsExactly(
            T.mins(5).msecs(), T.mins(10).msecs(), T.mins(15).msecs(),
            T.mins(30).msecs(), T.mins(60).msecs(), T.mins(180).msecs()
        ).inOrder()
    }

    @Test
    fun aMessageFromAnOlderWatchIsStoredAsItArrives() {
        // An older watch sends one message per window. Each one must stay a single row, or a phone
        // updated before the watch would store six rows per message instead of one.
        StepWindowRows.WINDOW_MINUTES.forEach { window ->
            val duration = T.mins(window).msecs()
            val rows = StepWindowRows.fromWearMessage(message(duration))

            assertThat(rows).hasSize(1)
            assertThat(rows.single().duration).isEqualTo(duration)
            assertThat(rows.single().steps180min).isEqualTo(66)
        }
    }
}
