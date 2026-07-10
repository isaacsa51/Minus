package com.serranoie.app.minus.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

class SavingsPreferencesTest {

    @Test
    fun `default split is the classic 50-30-20 rule`() {
        val prefs = SavingsPreferences.DEFAULT
        assertThat(prefs.preset).isEqualTo(SavingsSplitPreset.BALANCED)
        assertThat(prefs.needsPct).isEqualTo(50)
        assertThat(prefs.wantsPct).isEqualTo(30)
        assertThat(prefs.savingsPct).isEqualTo(20)
    }

    @Test
    fun `default needs-wants-savings percentages are derived from the BALANCED preset`() {
        val prefs = SavingsPreferences(preset = SavingsSplitPreset.BALANCED)
        assertThat(prefs.needsPct).isEqualTo(50)
        assertThat(prefs.wantsPct).isEqualTo(30)
        assertThat(prefs.savingsPct).isEqualTo(20)
    }

    @Test
    fun `default needs-wants-savings percentages are derived from AGGRESSIVE_SAVER preset`() {
        val prefs = SavingsPreferences(preset = SavingsSplitPreset.AGGRESSIVE_SAVER)
        assertThat(prefs.needsPct).isEqualTo(40)
        assertThat(prefs.wantsPct).isEqualTo(20)
        assertThat(prefs.savingsPct).isEqualTo(40)
    }

    @Test
    fun `default needs-wants-savings percentages are derived from CONSERVATIVE preset`() {
        val prefs = SavingsPreferences(preset = SavingsSplitPreset.CONSERVATIVE)
        assertThat(prefs.needsPct).isEqualTo(70)
        assertThat(prefs.wantsPct).isEqualTo(20)
        assertThat(prefs.savingsPct).isEqualTo(10)
    }

    @Test
    fun `CUSTOM preset allows arbitrary percentages`() {
        val prefs = SavingsPreferences(
            preset = SavingsSplitPreset.CUSTOM,
            needsPct = 35,
            wantsPct = 25,
            savingsPct = 40,
        )
        assertThat(prefs.preset).isEqualTo(SavingsSplitPreset.CUSTOM)
        assertThat(prefs.needsPct).isEqualTo(35)
        assertThat(prefs.wantsPct).isEqualTo(25)
        assertThat(prefs.savingsPct).isEqualTo(40)
    }

    @Test
    fun `preset tag is preserved regardless of whether the percentages match the preset`() {
        // Even if the user picked BALANCED, the percentages stay at the
        // preset's defaults — this is the normal "preset selected" state.
        val prefs = SavingsPreferences(preset = SavingsSplitPreset.BALANCED)
        assertThat(prefs.preset).isEqualTo(SavingsSplitPreset.BALANCED)
        assertThat(prefs.needsPct).isEqualTo(50)
    }

    @Test
    fun `spendingCeilingPct is needs plus wants`() {
        val prefs = SavingsPreferences(
            preset = SavingsSplitPreset.CUSTOM,
            needsPct = 40,
            wantsPct = 30,
            savingsPct = 30,
        )
        assertThat(prefs.spendingCeilingPct).isEqualTo(70)
    }

    @Test
    fun `spendingCeilingPct equals 80 for the default 50-30-20 split`() {
        assertThat(SavingsPreferences.DEFAULT.spendingCeilingPct).isEqualTo(80)
    }

    @Test
    fun `projectedPerPeriod divides the goal across the number of months`() {
        val prefs = SavingsPreferences(
            preset = SavingsSplitPreset.CUSTOM,
            savingsGoalAmount = BigDecimal("60000"),
            savingsGoalMonths = 12,
        )
        assertThat(prefs.projectedPerPeriod()).isEqualTo(BigDecimal("5000.00"))
    }

    @Test
    fun `projectedPerPeriod rounds half-up to 2 decimal places`() {
        val prefs = SavingsPreferences(
            preset = SavingsSplitPreset.CUSTOM,
            savingsGoalAmount = BigDecimal("100"),
            savingsGoalMonths = 3,
        )
        // 100 / 3 = 33.333... → 33.33
        assertThat(prefs.projectedPerPeriod()).isEqualTo(BigDecimal("33.33"))
    }

    @Test
    fun `projectedPerPeriod returns null when goal amount is missing`() {
        val prefs = SavingsPreferences(
            preset = SavingsSplitPreset.CUSTOM,
            savingsGoalAmount = null,
            savingsGoalMonths = 6,
        )
        assertThat(prefs.projectedPerPeriod()).isNull()
    }

    @Test
    fun `projectedPerPeriod returns null when goal months is missing`() {
        val prefs = SavingsPreferences(
            preset = SavingsSplitPreset.CUSTOM,
            savingsGoalAmount = BigDecimal("6000"),
            savingsGoalMonths = null,
        )
        assertThat(prefs.projectedPerPeriod()).isNull()
    }

    @Test
    fun `projectedPerPeriod returns null for zero months`() {
        val prefs = SavingsPreferences(
            preset = SavingsSplitPreset.CUSTOM,
            savingsGoalAmount = BigDecimal("6000"),
            savingsGoalMonths = 0,
        )
        assertThat(prefs.projectedPerPeriod()).isNull()
    }

    @Test
    fun `projectedPerPeriod returns null for negative months`() {
        val prefs = SavingsPreferences(
            preset = SavingsSplitPreset.CUSTOM,
            savingsGoalAmount = BigDecimal("6000"),
            savingsGoalMonths = -1,
        )
        assertThat(prefs.projectedPerPeriod()).isNull()
    }

    @Test
    fun `projectedPerPeriod returns null for zero or negative goal amount`() {
        assertThat(
            SavingsPreferences(
                preset = SavingsSplitPreset.CUSTOM,
                savingsGoalAmount = BigDecimal.ZERO,
                savingsGoalMonths = 6,
            ).projectedPerPeriod()
        ).isNull()
        assertThat(
            SavingsPreferences(
                preset = SavingsSplitPreset.CUSTOM,
                savingsGoalAmount = BigDecimal("-100"),
                savingsGoalMonths = 6,
            ).projectedPerPeriod()
        ).isNull()
    }

    @Test
    fun `fromPreset builds preferences with the preset's values and keeps goal independent`() {
        val prefs = SavingsPreferences.fromPreset(
            preset = SavingsSplitPreset.AGGRESSIVE_SAVER,
            savingsGoalAmount = BigDecimal("24000"),
            savingsGoalMonths = 12,
        )
        assertThat(prefs.preset).isEqualTo(SavingsSplitPreset.AGGRESSIVE_SAVER)
        assertThat(prefs.needsPct).isEqualTo(40)
        assertThat(prefs.wantsPct).isEqualTo(20)
        assertThat(prefs.savingsPct).isEqualTo(40)
        assertThat(prefs.savingsGoalAmount).isEqualTo(BigDecimal("24000"))
        assertThat(prefs.savingsGoalMonths).isEqualTo(12)
    }

    @Test
    fun `fromPreset defaults to no goal when goal fields are not provided`() {
        val prefs = SavingsPreferences.fromPreset(SavingsSplitPreset.CONSERVATIVE)
        assertThat(prefs.preset).isEqualTo(SavingsSplitPreset.CONSERVATIVE)
        assertThat(prefs.savingsGoalAmount).isNull()
        assertThat(prefs.savingsGoalMonths).isNull()
    }

    @Test
    fun `fromPreset round-trips through preset name`() {
        val presets = listOf(
            SavingsSplitPreset.BALANCED,
            SavingsSplitPreset.AGGRESSIVE_SAVER,
            SavingsSplitPreset.CONSERVATIVE,
        )
        presets.forEach { preset ->
            val prefs = SavingsPreferences.fromPreset(preset)
            assertThat(prefs.preset).isEqualTo(preset)
        }
    }

    @Test
    fun `SavingsSplitPreset fromValues never returns CUSTOM for known splits`() {
        assertThat(SavingsSplitPreset.fromValues(50, 30, 20))
            .isEqualTo(SavingsSplitPreset.BALANCED)
        assertThat(SavingsSplitPreset.fromValues(40, 20, 40))
            .isEqualTo(SavingsSplitPreset.AGGRESSIVE_SAVER)
        assertThat(SavingsSplitPreset.fromValues(70, 20, 10))
            .isEqualTo(SavingsSplitPreset.CONSERVATIVE)
    }

    @Test
    fun `SavingsSplitPreset fromValues returns CUSTOM for any other triple`() {
        assertThat(SavingsSplitPreset.fromValues(0, 100, 0))
            .isEqualTo(SavingsSplitPreset.CUSTOM)
        assertThat(SavingsSplitPreset.fromValues(60, 30, 10))
            .isEqualTo(SavingsSplitPreset.CUSTOM)
    }

    @Test
    fun `tapping Custom from a named preset flips the tag without changing the percentages`() {
        // Simulates the user flow: starts on Balanced (50/30/20), taps Custom.
        // The percentages should stay the same, but the preset should flip to
        // CUSTOM so the sliders become visible.
        val balanced = SavingsPreferences.fromPreset(SavingsSplitPreset.BALANCED)
        val custom = balanced.copy(preset = SavingsSplitPreset.CUSTOM)

        assertThat(custom.preset).isEqualTo(SavingsSplitPreset.CUSTOM)
        assertThat(custom.needsPct).isEqualTo(50)
        assertThat(custom.wantsPct).isEqualTo(30)
        assertThat(custom.savingsPct).isEqualTo(20)
    }

    @Test
    fun `tapping a named preset from Custom overrides any user-tuned percentages`() {
        // Simulates the user flow: starts on Custom (with hand-tuned 35/25/40),
        // taps Balanced. The percentages should snap to Balanced's defaults.
        val custom = SavingsPreferences(
            preset = SavingsSplitPreset.CUSTOM,
            needsPct = 35,
            wantsPct = 25,
            savingsPct = 40,
        )
        val balanced = SavingsPreferences.fromPreset(SavingsSplitPreset.BALANCED)

        assertThat(balanced.preset).isEqualTo(SavingsSplitPreset.BALANCED)
        assertThat(balanced.needsPct).isEqualTo(50)
        assertThat(balanced.wantsPct).isEqualTo(30)
        assertThat(balanced.savingsPct).isEqualTo(20)
    }
}
