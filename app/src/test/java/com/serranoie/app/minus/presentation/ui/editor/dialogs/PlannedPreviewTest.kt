package com.serranoie.app.minus.presentation.ui.editor.dialogs

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class PlannedPreviewTest {

    private fun settings(mode: BudgetSplitMode) = BudgetSettings(
        totalBudget = BigDecimal("1000"),
        period = BudgetPeriod.DAILY,
        startDate = LocalDate.of(2099, 7, 1),
        endDate = LocalDate.of(2099, 7, 10),
        currencyCode = "USD",
        splitMode = mode,
    )

    private val state = BudgetState.EMPTY.copy(
        dailyBudget = BigDecimal("125.00"),
        daysRemaining = 8,
        totalBudget = BigDecimal("1000"),
        totalSpentInPeriod = BigDecimal("100"),
    )

    @Test
    fun `dynamic shows the daily budget before and after holding the amount from today`() {
        val preview = plannedPreview(settings(BudgetSplitMode.DYNAMIC), state, BigDecimal("300"), LocalDate.of(2099, 7, 8))

        assertThat(preview).isEqualTo(PlannedPreview.Daily(BigDecimal("125.00"), BigDecimal("87.50")))
        assertThat(preview!!.isNegative).isFalse()
    }

    @Test
    fun `other split modes show what is left for the period`() {
        val preview = plannedPreview(settings(BudgetSplitMode.CARRY_OVER), state, BigDecimal("300"), LocalDate.of(2099, 7, 8))

        assertThat(preview).isEqualTo(PlannedPreview.LeftInPeriod(BigDecimal("900"), BigDecimal("600")))
    }

    @Test
    fun `an amount bigger than what is left is negative`() {
        val preview = plannedPreview(settings(BudgetSplitMode.STATIC), state, BigDecimal("1200"), LocalDate.of(2099, 7, 8))

        assertThat(preview!!.isNegative).isTrue()
    }

    @Test
    fun `a day after the period end is held in a later period`() {
        val day = LocalDate.of(2099, 7, 15)

        assertThat(plannedPreview(settings(BudgetSplitMode.DYNAMIC), state, BigDecimal("300"), day))
            .isEqualTo(PlannedPreview.OtherPeriod(day))
    }

    @Test
    fun `no preview without a budget or an amount`() {
        assertThat(plannedPreview(null, state, BigDecimal("300"), LocalDate.of(2099, 7, 8))).isNull()
        assertThat(plannedPreview(settings(BudgetSplitMode.DYNAMIC), state, null, LocalDate.of(2099, 7, 8))).isNull()
    }
}
