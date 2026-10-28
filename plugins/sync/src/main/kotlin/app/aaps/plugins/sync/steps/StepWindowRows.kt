package app.aaps.plugins.sync.steps

import app.aaps.core.data.model.SC
import app.aaps.core.data.time.T
import app.aaps.core.interfaces.rx.weardata.EventData

/**
 * Builds the step rows that AAPS stores for one step sample.
 *
 * Layout: one row per window. The row for a window of N minutes carries the counts of every window
 * up to N and zero above it. This is the shape the watch path and the phone step sampler write; the
 * Garmin and AutoISF paths each write a single row of their own shape instead, so a consumer must
 * not assume it for every row in the table.
 *
 * All six rows have to exist even when the watch sent the counts in one message, because automation
 * looks up a row by its exact duration. The Boost algorithm takes the highest value per window over
 * all rows of the newest sample and the overview graph draws one bar per row from `steps5min`, so
 * neither of those changes when the rows come from one message instead of six.
 */
object StepWindowRows {

    /** Window lengths in minutes, one row each. */
    val WINDOW_MINUTES = listOf(5L, 10L, 15L, 30L, 60L, 180L)

    /**
     * @param windows step count per window, keyed by the window length in minutes. A missing window
     *   counts as zero.
     */
    fun build(timestamp: Long, device: String, windows: Map<Long, Int>): List<SC> {
        fun s(minutes: Long) = windows[minutes] ?: 0
        return WINDOW_MINUTES.map { window ->
            SC(
                duration = T.mins(window).msecs(),
                timestamp = timestamp,
                steps5min = s(5),
                steps10min = if (window >= 10) s(10) else 0,
                steps15min = if (window >= 15) s(15) else 0,
                steps30min = if (window >= 30) s(30) else 0,
                steps60min = if (window >= 60) s(60) else 0,
                steps180min = if (window >= 180) s(180) else 0,
                device = device
            )
        }
    }

    /**
     * Turns one step message from the watch into the rows to store.
     *
     * A watch that sends [EventData.ActionStepsRate.DURATION_ALL_WINDOWS_MS] puts every window in
     * one message and the rows are built here. An older watch sends one message per window; those
     * are stored exactly as they arrive, so a phone updated before the watch keeps working.
     */
    fun fromWearMessage(action: EventData.ActionStepsRate): List<SC> =
        if (action.duration == EventData.ActionStepsRate.DURATION_ALL_WINDOWS_MS)
            build(
                timestamp = action.timestamp,
                device = action.device,
                windows = mapOf(
                    5L to action.steps5min,
                    10L to action.steps10min,
                    15L to action.steps15min,
                    30L to action.steps30min,
                    60L to action.steps60min,
                    180L to action.steps180min
                )
            )
        else
            listOf(
                SC(
                    duration = action.duration,
                    timestamp = action.timestamp,
                    steps5min = action.steps5min,
                    steps10min = action.steps10min,
                    steps15min = action.steps15min,
                    steps30min = action.steps30min,
                    steps60min = action.steps60min,
                    steps180min = action.steps180min,
                    device = action.device
                )
            )
}
