package com.serranoie.app.minus.domain.calculator

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.SavingsPocket
import com.serranoie.app.minus.domain.model.SavingsPreferences
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.budget.BudgetStateCalculator
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class SavingsPocketsTest {

    private val trip = SavingsPocket(id = 1, name = "Japan trip", target = BigDecimal("30000"))
    private val emergency = SavingsPocket(id = 2, name = "Emergency")
    private val now = LocalDateTime.of(2099, 6, 10, 12, 0)

    private val settings = BudgetSettings(
        totalBudget = BigDecimal("1000"),
        period = BudgetPeriod.MONTHLY,
        startDate = LocalDate.of(2099, 6, 1),
        endDate = LocalDate.of(2099, 6, 30),
        currencyCode = "USD",
        splitMode = BudgetSplitMode.DYNAMIC,
    )

    @Test
    fun `balances net contributions against withdrawals per pocket and ignore untagged rows`() {
        val rows = listOf(
            pocketEntry(trip, BigDecimal("500"), withdraw = false, periodId = 7, date = now),
            pocketEntry(trip, BigDecimal("120.50"), withdraw = false, periodId = 7, date = now),
            pocketEntry(trip, BigDecimal("200"), withdraw = true, periodId = 7, date = now),
            pocketEntry(emergency, BigDecimal("80"), withdraw = false, periodId = 7, date = now),
            Transaction(amount = BigDecimal("999"), date = now),
            Transaction(amount = BigDecimal("50"), date = now, isAdjustment = true),
        )

        val balances = pocketBalances(rows)

        assertThat(balances).containsExactly(1L, BigDecimal("420.50"), 2L, BigDecimal("80"))
    }

    @Test
    fun `a contribution is a budget decrease tagged with the pocket, never spending`() {
        val entry = pocketEntry(trip, BigDecimal("200"), withdraw = false, periodId = 7, date = now)

        assertThat(entry.pocketId).isEqualTo(1L)
        assertThat(entry.comment).isEqualTo("Japan trip")
        assertThat(entry.categoryId).isNull()
        assertThat(entry.periodId).isEqualTo(7L)

        val state = BudgetStateCalculator().calculateBudgetState(
            settings = settings,
            transactions = listOf(entry),
            currentDate = now.toLocalDate(),
        )
        assertThat(state.totalSpentInPeriod).isEqualToIgnoringScale(BigDecimal.ZERO)
        assertThat(state.totalBudget).isEqualToIgnoringScale(BigDecimal("800"))
    }

    @Test
    fun `a withdrawal returns the money to the budget as income`() {
        val entry = pocketEntry(trip, BigDecimal("150"), withdraw = true, periodId = 7, date = now)

        assertThat(entry.amount).isEqualTo(BigDecimal("-150"))
        assertThat(entry.pocketId).isEqualTo(1L)

        val state = BudgetStateCalculator().calculateBudgetState(
            settings = settings,
            transactions = listOf(entry),
            currentDate = now.toLocalDate(),
        )
        assertThat(state.totalSpentInPeriod).isEqualToIgnoringScale(BigDecimal.ZERO)
        assertThat(state.totalBudget).isEqualToIgnoringScale(BigDecimal("1150"))
    }

    private val today = LocalDate.of(2099, 6, 10)
    private val noGoal = SavingsPreferences()
    private val goal = SavingsPreferences(savingsGoalAmount = BigDecimal("15000"), savingsGoalMonths = 6)

    @Test
    fun `a settings goal with no goal pocket creates an unnamed pocket with target and deadline`() {
        val created = goalPocketUpdate(existing = null, previous = null, updated = goal, today = today)

        assertThat(created).isNotNull()
        assertThat(created!!.id).isEqualTo(0L)
        assertThat(created.name).isEmpty()
        assertThat(created.target).isEqualToIgnoringScale(BigDecimal("15000"))
        assertThat(created.targetDate).isEqualTo(LocalDate.of(2099, 12, 10))
    }

    @Test
    fun `no goal and no goal pocket creates nothing`() {
        assertThat(goalPocketUpdate(existing = null, previous = null, updated = noGoal, today = today)).isNull()
    }

    @Test
    fun `changing the goal amount or months updates the goal pocket`() {
        val existing = SavingsPocket(id = 9, name = "", target = BigDecimal("15000"), targetDate = LocalDate.of(2099, 9, 1))
        val updated = goal.copy(savingsGoalAmount = BigDecimal("20000"), savingsGoalMonths = 12)

        val result = goalPocketUpdate(existing, previous = goal, updated = updated, today = today)

        assertThat(result).isEqualTo(existing.copy(target = BigDecimal("20000"), targetDate = LocalDate.of(2100, 6, 10)))
    }

    @Test
    fun `clearing the goal keeps the pocket but drops its target and deadline`() {
        val existing = SavingsPocket(id = 9, name = "", target = BigDecimal("15000"), targetDate = LocalDate.of(2099, 9, 1))

        val result = goalPocketUpdate(existing, previous = goal, updated = noGoal, today = today)

        assertThat(result).isEqualTo(existing.copy(target = null, targetDate = null))
    }

    @Test
    fun `unchanged goal fields leave the deadline alone`() {
        val existing = SavingsPocket(id = 9, name = "", target = BigDecimal("15000"), targetDate = LocalDate.of(2099, 9, 1))
        val presetOnly = goal.copy(needsPct = 40, wantsPct = 20, savingsPct = 40)

        assertThat(goalPocketUpdate(existing, previous = goal, updated = presetOnly, today = today)).isNull()
        assertThat(goalPocketUpdate(existing, previous = null, updated = goal, today = today)).isNull()
    }

    @Test
    fun `a goal pocket row is labelled with the localized name`() {
        val goalPocket = SavingsPocket(id = 9, name = "")

        val entry = pocketEntry(goalPocket, BigDecimal("100"), withdraw = false, periodId = 7, date = now, label = "Ahorro")

        assertThat(entry.comment).isEqualTo("Ahorro")
        assertThat(goalPocket.isGoalPocket).isTrue()
        assertThat(trip.isGoalPocket).isFalse()
    }

    @Test
    fun `period flow splits what went into pockets from what came out`() {
        val rows = listOf(
            pocketEntry(trip, BigDecimal("500"), withdraw = false, periodId = 7, date = now),
            pocketEntry(emergency, BigDecimal("80"), withdraw = false, periodId = 7, date = now),
            pocketEntry(trip, BigDecimal("150"), withdraw = true, periodId = 7, date = now),
            pocketEntry(trip, BigDecimal("999"), withdraw = false, periodId = 7, date = now).copy(isDeleted = true),
            Transaction(amount = BigDecimal("40"), date = now, isAdjustment = true),
            Transaction(amount = BigDecimal("-60"), date = now),
        )

        val flow = periodPocketFlow(rows)

        assertThat(flow).isNotNull()
        assertThat(flow!!.added).isEqualToIgnoringScale(BigDecimal("580"))
        assertThat(flow.withdrawn).isEqualToIgnoringScale(BigDecimal("150"))
    }

    @Test
    fun `a period without pocket movement has no flow`() {
        val rows = listOf(Transaction(amount = BigDecimal("40"), date = now, isAdjustment = true))

        assertThat(periodPocketFlow(rows)).isNull()
    }
}
