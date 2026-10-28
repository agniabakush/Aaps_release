package app.aaps.wear.watchfaces.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class InteractiveUpdateRateTest {

    @Test
    fun secondsShownMeansOneRedrawPerSecond() {
        assertThat(interactiveDelayMs(true)).isEqualTo(1000L)
    }

    @Test
    fun noSecondsMeansOneRedrawPerMinute() {
        assertThat(interactiveDelayMs(false)).isEqualTo(60_000L)
    }

    @Test
    fun bothValuesAreAcceptedByTheWatchFaceLibrary() {
        // The library rejects anything above one minute.
        assertThat(interactiveDelayMs(true)).isAtMost(MAX_INTERACTIVE_UPDATE_RATE_MS)
        assertThat(interactiveDelayMs(false)).isAtMost(MAX_INTERACTIVE_UPDATE_RATE_MS)
    }
}
