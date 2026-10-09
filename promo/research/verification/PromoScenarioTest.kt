package com.serranoie.app.minus.presentation.ui.budget

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.LeftoverChoice
import com.serranoie.app.minus.domain.model.Transaction
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class PromoScenarioTest {

    private val calculator = BudgetStateCalculator()
    private fun day(n: Int): LocalDate = LocalDate.of(2026, 7, n)

    private fun settings(
        mode: BudgetSplitMode,
        total: String = "1400",
        surplus: String? = null,
        dayOneLump: Boolean = false,
    ) = BudgetSettings(
        totalBudget = BigDecimal(total),
        period = BudgetPeriod.DAILY,
        startDate = day(1),
        endDate = day(14),
        currencyCode = "USD",
        rollOverEnabled = surplus != null,
        rollOverLimit = surplus?.let(::BigDecimal),
        rollOverCarryForward = dayOneLump,
        splitMode = mode,
    )

    private fun spend(amount: String, onDay: Int) = Transaction(
        id = onDay.toLong(),
        amount = BigDecimal(amount),
        date = day(onDay).atTime(12, 0),
        periodId = 1L,
    )

    private fun dayTwo(mode: BudgetSplitMode, choice: LeftoverChoice? = null): BudgetState =
        calculator.calculateBudgetState(
            settings = settings(mode),
            transactions = listOf(spend("60", 1)),
            currentDate = day(2),
            leftoverChoices = choice?.let { mapOf(day(2) to it) } ?: emptyMap(),
        )

    private fun BudgetState.log(label: String) = println(
        "PROMO $label dailyBudget=$dailyBudget remainingToday=$remainingToday pending=$pendingLeftover"
    )

    @Test
    fun `day one spends 60 of 100 then day two differs per split mode`() {
        val static = dayTwo(BudgetSplitMode.STATIC).also { it.log("STATIC") }
        val dynamic = dayTwo(BudgetSplitMode.DYNAMIC).also { it.log("DYNAMIC") }
        val carry = dayTwo(BudgetSplitMode.CARRY_OVER).also { it.log("CARRY_OVER") }
        val askPending = dayTwo(BudgetSplitMode.ASK_ME).also { it.log("ASK_ME pending") }
        val askSpread = dayTwo(BudgetSplitMode.ASK_ME, LeftoverChoice.SPREAD).also { it.log("ASK_ME spread") }
        val askCarry = dayTwo(BudgetSplitMode.ASK_ME, LeftoverChoice.CARRY).also { it.log("ASK_ME carry") }

        assertThat(static.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(dynamic.remainingToday).isEqualTo(BigDecimal("103.08"))
        assertThat(carry.remainingToday).isEqualTo(BigDecimal("140.00"))
        assertThat(askPending.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(askPending.pendingLeftover).isEqualTo(BigDecimal("40.00"))
        assertThat(askSpread.remainingToday).isEqualTo(BigDecimal("103.08"))
        assertThat(askCarry.remainingToday).isEqualTo(BigDecimal("140.00"))
    }

    @Test
    fun `last period surplus of 70 either spreads or lands on day one`() {
        val spread = calculator.calculateBudgetState(
            settings = settings(BudgetSplitMode.STATIC, total = "1470", surplus = "70"),
            transactions = emptyList(),
            currentDate = day(1),
        ).also { it.log("SURPLUS spread day1") }
        val lumpDayOne = calculator.calculateBudgetState(
            settings = settings(BudgetSplitMode.STATIC, total = "1470", surplus = "70", dayOneLump = true),
            transactions = emptyList(),
            currentDate = day(1),
        ).also { it.log("SURPLUS lump day1") }
        val lumpDayTwo = calculator.calculateBudgetState(
            settings = settings(BudgetSplitMode.STATIC, total = "1470", surplus = "70", dayOneLump = true),
            transactions = emptyList(),
            currentDate = day(2),
        ).also { it.log("SURPLUS lump day2") }

        assertThat(spread.remainingToday).isEqualTo(BigDecimal("105.00"))
        assertThat(lumpDayOne.remainingToday).isEqualTo(BigDecimal("170.00"))
        assertThat(lumpDayTwo.remainingToday).isEqualTo(BigDecimal("100.00"))
    }
}
