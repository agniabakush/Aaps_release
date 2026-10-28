package app.aaps.plugins.aps.openAPSBoostV5

import app.aaps.core.keys.DoubleKey
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/**
 * Confirms that the 45-day evidence reaches onboarding without weakening the
 * independent threshold ownership and lowering protections.
 */
class BoostV5TrancheAutoConfigTest {

    private fun tranche(
        small: Int,
        large: Int,
        threshold: Double?,
        j: Double? = null
    ) = TrancheThresholdDeriver.Derivation(
        episodes = emptyList(),
        nSmall = small,
        nLarge = large,
        threshold = threshold,
        youdenJ = j,
        shortfall = if (threshold == null)
            TrancheThresholdDeriver.Shortfall.TOO_FEW_LABELLED
        else TrancheThresholdDeriver.Shortfall.NONE
    )

    private fun suggestion(tr: TrancheThresholdDeriver.Derivation?) =
        BoostV5AutoConfig.compute(
            BoostV5AutoConfig.V1Profile(
                daysWithData = 28,
                bgReadingCount = 6000,
                tddMedianU = 40.0,
                manualBolusesU = List(20) { 4.0 },
                smbAmountsU = List(100) { 0.3 },
                tbrBelow70Pct = 1.0,
                timeBelow54Pct = 0.1,
                meanGlucoseMgdl = 130.0,
                currentMaxIobU = 6.0,
                currentMaxBolusU = 2.5,
                tranche = tr
            )
        )!!

    @Test
    fun `missing history is distinguished from insufficient labelled classes`() {
        val missing = suggestion(null)
        assertThat(missing.trancheThreshold).isNull()
        assertThat(missing.rationale.joinToString(" ")).contains("no confirm history gathered")

        val thin = suggestion(tranche(3, 2, null))
        assertThat(thin.trancheThreshold).isNull()
        assertThat(thin.rationale.joinToString(" ")).contains("3 small, 2 large")
    }

    @Test
    fun `available evidence carries both class counts and quality`() {
        val s = suggestion(tranche(21, 14, 0.64, 0.35))
        assertThat(s.trancheThreshold).isEqualTo(0.64)
        assertThat(s.rationale.joinToString(" ")).contains("21 small and 14 large")
        assertThat(s.rationale.joinToString(" ")).contains("J 0.35")

        val writes = mutableListOf<Double>()
        val recorded = mutableListOf<Double>()
        val r = BoostV5AutoConfigApply.resolveTrancheThreshold(
            suggestion = s,
            storedValue = DoubleKey.ApsBoostV5TrancheThreshold.defaultValue,
            lastWritten = null,
            put = { writes += it },
            recordWritten = { recorded += it }
        )
        assertThat(r.outcome).isEqualTo(BoostV5AutoConfigApply.Outcome.REDRIVEN)
        assertThat(writes).containsExactly(0.64)
        assertThat(recorded).containsExactly(0.64)
        assertThat(BoostV5AutoConfigApply.trancheBreadcrumb(r, s.tranche))
            .contains("/s=21/l=14/J=0.35")
    }

    @Test
    fun `a lower release threshold is never applied automatically`() {
        val s = suggestion(tranche(20, 12, 0.30, 0.25))
        val writes = mutableListOf<Double>()
        val r = BoostV5AutoConfigApply.resolveTrancheThreshold(
            suggestion = s,
            storedValue = 0.48,
            lastWritten = null,
            put = { writes += it },
            recordWritten = { writes += it }
        )
        assertThat(r.outcome).isEqualTo(BoostV5AutoConfigApply.Outcome.LOOSENING_HELD)
        assertThat(writes).isEmpty()
        assertThat(r.operativeValue).isEqualTo(0.48)
    }

    @Test
    fun `a manual override is preserved even when changed back to factory`() {
        val s = suggestion(tranche(21, 14, 0.68, 0.4))
        val writes = mutableListOf<Double>()
        val r = BoostV5AutoConfigApply.resolveTrancheThreshold(
            suggestion = s,
            storedValue = 0.48,
            lastWritten = 0.64,
            put = { writes += it },
            recordWritten = { writes += it }
        )
        assertThat(r.outcome).isEqualTo(BoostV5AutoConfigApply.Outcome.KEPT_USER_TUNED)
        assertThat(writes).isEmpty()
        assertThat(r.operativeValue).isEqualTo(0.48)
    }

    @Test
    fun `an insufficient episode history makes no preference writes`() {
        val s = suggestion(tranche(2, 3, null))
        val writes = mutableListOf<Double>()
        val r = BoostV5AutoConfigApply.resolveTrancheThreshold(
            suggestion = s,
            storedValue = null,
            lastWritten = null,
            put = { writes += it },
            recordWritten = { writes += it }
        )
        assertThat(r.outcome).isEqualTo(BoostV5AutoConfigApply.Outcome.INSUFFICIENT_EVIDENCE)
        assertThat(writes).isEmpty()
        assertThat(BoostV5AutoConfigApply.trancheBreadcrumb(r, s.tranche))
            .contains("d=-/op=0.48")
    }
}
