package com.serranoie.app.minus.presentation.ui.theme.component

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.SavingsPreferences
import com.serranoie.app.minus.domain.model.SavingsSplitPreset
import com.serranoie.app.minus.domain.model.Transaction
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class SavingsCardMetricsTest {

    private fun spend(amount: String, recurrent: Boolean = false) = Transaction(
        amount = BigDecimal(amount),
        isRecurrent = recurrent,
        date = LocalDateTime.now(),
    )

    private fun metrics(
        budget: String,
        recurring: List<Transaction> = emptyList(),
        oneTime: List<Transaction> = emptyList(),
        preferences: SavingsPreferences = SavingsPreferences.DEFAULT,
    ) = savingsCardMetrics(BigDecimal(budget), recurring, oneTime, preferences)

    @Test
    fun `no metrics without a budget period`() {
        assertThat(metrics("0")).isNull()
        assertThat(metrics("-500")).isNull()
    }

    @Test
    fun `percentages are shares of the budget and incomes are ignored`() {
        val result = metrics(
            budget = "20000",
            recurring = listOf(spend("6000", recurrent = true), spend("-1000", recurrent = true)),
            oneTime = listOf(spend("4000")),
        )!!

        assertThat(result.recurrentSpent).isEqualTo(BigDecimal("6000"))
        assertThat(result.variableSpent).isEqualTo(BigDecimal("4000"))
        assertThat(result.recurrentPct).isEqualTo(30)
        assertThat(result.variablePct).isEqualTo(20)
        assertThat(result.savings).isEqualTo(BigDecimal("10000"))
        assertThat(result.savingsPct).isEqualTo(50)
    }

    @Test
    fun `savings never go negative when over budget`() {
        val result = metrics(budget = "10000", oneTime = listOf(spend("13000")))!!
        assertThat(result.savings).isEqualTo(BigDecimal.ZERO)
        assertThat(result.savingsPct).isEqualTo(0)
        assertThat(result.variablePct).isEqualTo(130)
    }

    @Test
    fun `ideal savings and the six month example follow the savings preference`() {
        val balanced = metrics(budget = "20000")!!
        assertThat(balanced.idealSavingsPerPeriod).isEqualTo(BigDecimal("4000.00"))
        assertThat(balanced.projectedSavingsSixMonths).isEqualTo(BigDecimal("24000.00"))

        val aggressive = metrics(
            budget = "20000",
            preferences = SavingsPreferences.fromPreset(SavingsSplitPreset.AGGRESSIVE_SAVER),
        )!!
        assertThat(aggressive.idealSavingsPerPeriod).isEqualTo(BigDecimal("8000.00"))
        assertThat(aggressive.projectedSavingsSixMonths).isEqualTo(BigDecimal("48000.00"))
    }

    @Test
    fun `goalPerPeriod is null unless the goal is usable`() {
        assertThat(metrics(budget = "20000")!!.goalPerPeriod).isNull()

        val brokenGoal = metrics(
            budget = "20000",
            preferences = SavingsPreferences.DEFAULT.copy(
                savingsGoalAmount = BigDecimal("60000"),
                savingsGoalMonths = 0,
            ),
        )!!
        assertThat(brokenGoal.goalPerPeriod).isNull()

        val goal = metrics(
            budget = "20000",
            preferences = SavingsPreferences.DEFAULT.copy(
                savingsGoalAmount = BigDecimal("60000"),
                savingsGoalMonths = 12,
            ),
        )!!
        assertThat(goal.goalPerPeriod).isEqualTo(BigDecimal("5000.00"))
    }
}
