package app.aaps.wear.data

import app.aaps.core.interfaces.rx.weardata.EventData
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class WearDataContentTest {

    private fun bg(timeStamp: Long = 1_000L, sgv: Double = 100.0, sgvString: String = "5.6", slopeArrow: String = "->") =
        EventData.SingleBg(
            dataset = 0,
            timeStamp = timeStamp,
            sgvString = sgvString,
            slopeArrow = slopeArrow,
            sgv = sgv,
            high = 180.0,
            low = 70.0
        )

    @Test
    fun sameValuesCompareAsUnchanged() {
        assertThat(bg().sameContentAs(bg())).isTrue()
    }

    @Test
    fun aCorrectedValueAtTheSameTimeCountsAsChanged() {
        // This is the case the built in equals of SingleBg misses: it only looks at timeStamp and
        // color, so a smoothed or corrected reading would look unchanged and never reach the watch.
        assertThat(bg(sgv = 100.0) == bg(sgv = 120.0)).isTrue()
        assertThat(bg(sgv = 100.0).sameContentAs(bg(sgv = 120.0))).isFalse()
        assertThat(bg(sgvString = "5.6").sameContentAs(bg(sgvString = "6.7"))).isFalse()
        assertThat(bg(slopeArrow = "->").sameContentAs(bg(slopeArrow = "^"))).isFalse()
    }

    @Test
    fun aNewReadingCountsAsChanged() {
        assertThat(bg(timeStamp = 1_000L).sameContentAs(bg(timeStamp = 2_000L))).isFalse()
    }

    @Test
    fun listsCompareElementByElement() {
        assertThat(listOf(bg(), bg(timeStamp = 2_000L)).sameContentAs(listOf(bg(), bg(timeStamp = 2_000L)))).isTrue()
        assertThat(listOf(bg()).sameContentAs(listOf(bg(), bg(timeStamp = 2_000L)))).isFalse()
        assertThat(listOf(bg(sgv = 100.0)).sameContentAs(listOf(bg(sgv = 120.0)))).isFalse()
    }

    @Test
    fun treatmentsComparePredictionsByContent() {
        fun treatments(predictionSgv: Double) = EventData.TreatmentData(
            temps = ArrayList(),
            basals = ArrayList(),
            boluses = ArrayList(),
            predictions = arrayListOf(bg(sgv = predictionSgv))
        )
        assertThat(treatments(100.0).sameContentAs(treatments(100.0))).isTrue()
        assertThat(treatments(100.0).sameContentAs(treatments(120.0))).isFalse()
    }

    @Test
    fun aNewBolusCountsAsChanged() {
        fun treatments(boluses: List<EventData.TreatmentData.Treatment>) = EventData.TreatmentData(
            temps = ArrayList(),
            basals = ArrayList(),
            boluses = ArrayList(boluses),
            predictions = ArrayList()
        )

        val bolus = EventData.TreatmentData.Treatment(date = 1_000L, bolus = 1.0, carbs = 0.0, isSMB = false, isValid = true)
        assertThat(treatments(emptyList()).sameContentAs(treatments(listOf(bolus)))).isFalse()
        assertThat(treatments(listOf(bolus)).sameContentAs(treatments(listOf(bolus)))).isTrue()
    }
}
