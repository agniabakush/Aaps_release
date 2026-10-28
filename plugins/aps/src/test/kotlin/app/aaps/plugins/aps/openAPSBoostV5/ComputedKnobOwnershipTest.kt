package app.aaps.plugins.aps.openAPSBoostV5

import app.aaps.core.keys.DoubleKey
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

/** Ownership guard extracted from source Boost 6.2.0 audit 15. */
class ComputedKnobOwnershipTest {
    private val primer = DoubleKey.ApsBoostV5PrimerCapU
    private val cumulative = DoubleKey.ApsBoostCumulativeSmbCap60Min

    @Test fun `a manually disabled primer is never owned by auto-config`() {
        assertThat(BoostV5AutoConfigApply.computedKnobOwned(primer, 0.0, 0.75, listOf(0.75), 0.056)).isFalse()
    }

    @Test fun `primer zero without a ledger is not silently re-enabled`() {
        assertThat(BoostV5AutoConfigApply.computedKnobOwned(primer, 0.0, null, listOf(0.75), 0.056)).isFalse()
    }

    @Test fun `a factory-default value changed after an auto-config write is preserved`() {
        assertThat(BoostV5AutoConfigApply.computedKnobOwned(cumulative, 10.0, 5.0, listOf(5.0), 0.01)).isFalse()
    }

    @Test fun `an unchanged auto-written primer remains owned`() {
        assertThat(BoostV5AutoConfigApply.computedKnobOwned(primer, 0.75, 0.75, listOf(0.75), 0.056)).isTrue()
    }

    @Test fun `a manually lowered cumulative cap is preserved`() {
        assertThat(BoostV5AutoConfigApply.computedKnobOwned(cumulative, 2.0, 5.0, listOf(5.0), 0.01)).isFalse()
    }

    @Test fun `old auto-derived value may be recognised without a ledger`() {
        assertThat(BoostV5AutoConfigApply.computedKnobOwned(cumulative, 5.0, null, listOf(5.0), 0.01)).isTrue()
    }
}
