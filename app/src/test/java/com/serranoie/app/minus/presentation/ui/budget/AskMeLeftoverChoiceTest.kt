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

class AskMeLeftoverChoiceTest {

    private val calculator = BudgetStateCalculator()

    private val start = LocalDate.of(2026, 7, 1)
    private val end = LocalDate.of(2026, 7, 10)
    private fun day(n: Int): LocalDate = LocalDate.of(2026, 7, n)

    private fun askMeSettings(
        totalBudget: BigDecimal = BigDecimal("1000"),
        carryForward: Boolean = false,
        rolloverLimit: BigDecimal? = null,
        rolloverAppliedDate: LocalDate? = null,
    ) = BudgetSettings(
        totalBudget = totalBudget,
        period = BudgetPeriod.DAILY,
        startDate = start,
        endDate = end,
        currencyCode = "USD",
        daysInPeriod = 1,
        rollOverEnabled = carryForward,
        rollOverLimit = rolloverLimit,
        rollOverCarryForward = carryForward,
        splitMode = BudgetSplitMode.ASK_ME,
        rollOverAppliedDate = rolloverAppliedDate,
    )

    private fun spend(amount: String, onDay: Int): Transaction = Transaction(
        id = onDay.toLong(),
        amount = BigDecimal(amount),
        date = day(onDay).atTime(12, 0),
        periodId = 1L,
    )

    private fun onDay(
        currentDay: Int,
        choices: Map<Int, LeftoverChoice> = emptyMap(),
        spends: List<Pair<Int, String>> = emptyList(),
        settings: BudgetSettings = askMeSettings(),
    ): BudgetState = calculator.calculateBudgetState(
        settings = settings,
        transactions = spends.map { (onDay, amount) -> spend(amount, onDay) },
        currentDate = day(currentDay),
        leftoverChoices = choices.mapKeys { (dayNumber, _) -> day(dayNumber) },
    )

    @Test
    fun `an untouched day turns into a pending prompt the next morning`() {
        val dayTwo = onDay(currentDay = 2, spends = listOf(1 to "80"))

        assertThat(dayTwo.pendingLeftover).isEqualTo(BigDecimal("20.00"))
        assertThat(dayTwo.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(dayTwo.dailyBudget).isEqualTo(BigDecimal("100.00"))
    }

    @Test
    fun `nothing is pending on day one, there is no yesterday to ask about`() {
        val dayOne = onDay(currentDay = 1, spends = listOf(1 to "80"))

        assertThat(dayOne.pendingLeftover).isEqualTo(BigDecimal("0.00"))
        assertThat(dayOne.remainingToday).isEqualTo(BigDecimal("20.00"))
    }

    @Test
    fun `unanswered prompts stack up day after day instead of expiring`() {
        val spends = listOf(1 to "80", 2 to "60")

        assertThat(onDay(2, spends = spends).pendingLeftover).isEqualTo(BigDecimal("20.00"))
        assertThat(onDay(3, spends = spends).pendingLeftover).isEqualTo(BigDecimal("60.00"))
        assertThat(onDay(4, spends = spends).pendingLeftover).isEqualTo(BigDecimal("160.00"))
        assertThat(onDay(5, spends = spends).pendingLeftover).isEqualTo(BigDecimal("260.00"))
    }

    @Test
    fun `an unanswered prompt never inflates today's allowance`() {
        val spends = listOf(1 to "20", 2 to "10")

        listOf(3, 4, 5).forEach { dayNumber ->
            assertThat(onDay(dayNumber, spends = spends).remainingToday)
                .isEqualTo(BigDecimal("100.00"))
        }
    }

    @Test
    fun `carry hands the whole pending amount to today and clears the prompt`() {
        val carried = onDay(2, choices = mapOf(2 to LeftoverChoice.CARRY), spends = listOf(1 to "80"))

        assertThat(carried.remainingToday).isEqualTo(BigDecimal("120.00"))
        assertThat(carried.pendingLeftover).isEqualTo(BigDecimal("0.00"))
        assertThat(carried.dailyBudget).isEqualTo(BigDecimal("100.00"))
    }

    @Test
    fun `carry leaves the daily rate alone, tomorrow is a normal day again`() {
        val choices = mapOf(2 to LeftoverChoice.CARRY)
        val spends = listOf(1 to "80")

        val dayTwo = onDay(2, choices, spends)
        val dayThree = onDay(3, choices, spends)

        assertThat(dayTwo.dailyBudget).isEqualTo(BigDecimal("100.00"))
        assertThat(dayThree.dailyBudget).isEqualTo(BigDecimal("100.00"))
        // the 120 offered on day two went unspent, so day three asks about all of it again
        assertThat(dayThree.pendingLeftover).isEqualTo(BigDecimal("120.00"))
        assertThat(dayThree.remainingToday).isEqualTo(BigDecimal("100.00"))
    }

    @Test
    fun `carrying several days in a row walks the whole surplus forward`() {
        val spends = listOf(1 to "80", 2 to "80", 3 to "80")
        fun spentSoFar(dayNumber: Int) = spends.filter { (onDay, _) -> onDay <= dayNumber }
        val alwaysCarry = mapOf(
            2 to LeftoverChoice.CARRY,
            3 to LeftoverChoice.CARRY,
            4 to LeftoverChoice.CARRY,
        )

        assertThat(onDay(2, alwaysCarry, spentSoFar(2)).remainingToday).isEqualTo(BigDecimal("40.00"))
        assertThat(onDay(3, alwaysCarry, spentSoFar(3)).remainingToday).isEqualTo(BigDecimal("60.00"))
        assertThat(onDay(4, alwaysCarry, spentSoFar(4)).remainingToday).isEqualTo(BigDecimal("160.00"))
    }

    @Test
    fun `carry spends only what today actually costs, the rest returns to the prompt tomorrow`() {
        val choices = mapOf(2 to LeftoverChoice.CARRY)
        val spends = listOf(1 to "80", 2 to "50")

        val dayTwo = onDay(2, choices, spends)
        val dayThree = onDay(3, choices, spends)

        assertThat(dayTwo.remainingToday).isEqualTo(BigDecimal("70.00"))
        assertThat(dayThree.pendingLeftover).isEqualTo(BigDecimal("70.00"))
    }

    @Test
    fun `spread raises the daily rate for the rest of the period instead of today alone`() {
        val spread = onDay(2, choices = mapOf(2 to LeftoverChoice.SPREAD), spends = listOf(1 to "80"))

        assertThat(spread.dailyBudget).isEqualTo(BigDecimal("102.22"))
        assertThat(spread.remainingToday).isEqualTo(BigDecimal("102.22"))
        assertThat(spread.pendingLeftover).isEqualTo(BigDecimal("0.00"))
    }

    @Test
    fun `a spread on day two is still paying out on day six`() {
        val choices = mapOf(2 to LeftoverChoice.SPREAD)
        val spends = listOf(1 to "80")

        val daySix = onDay(6, choices, spends)

        assertThat(daySix.dailyBudget).isEqualTo(BigDecimal("102.22"))
        assertThat(daySix.remainingToday).isEqualTo(BigDecimal("102.22"))
    }

    @Test
    fun `spreading later in the period divides over fewer days, so each day gets more`() {
        val spends = listOf(1 to "80")

        val spreadOnDayTwo = onDay(2, mapOf(2 to LeftoverChoice.SPREAD), spends)
        val spreadOnDayFive = onDay(5, mapOf(5 to LeftoverChoice.SPREAD), spends)
        val spreadOnDayNine = onDay(9, mapOf(9 to LeftoverChoice.SPREAD), spends)

        assertThat(spreadOnDayTwo.dailyBudget).isEqualTo(BigDecimal("102.22"))
        assertThat(spreadOnDayFive.dailyBudget).isEqualTo(BigDecimal("153.33"))
        assertThat(spreadOnDayNine.dailyBudget).isEqualTo(BigDecimal("460.00"))
    }

    @Test
    fun `spread on the last day is the same as carrying, there is nowhere else to put it`() {
        val spends = listOf(1 to "80")

        val spread = onDay(10, mapOf(10 to LeftoverChoice.SPREAD), spends)
        val carry = onDay(10, mapOf(10 to LeftoverChoice.CARRY), spends)

        assertThat(spread.remainingToday).isEqualTo(BigDecimal("920.00"))
        assertThat(carry.remainingToday).isEqualTo(BigDecimal("920.00"))
    }

    @Test
    fun `spread reports the grown rate back as the split budget so the period pill adds up`() {
        val spread = onDay(2, mapOf(2 to LeftoverChoice.SPREAD), listOf(1 to "80"))
        val untouched = onDay(2, spends = listOf(1 to "80"))

        assertThat(untouched.splitBudget).isEqualTo(BigDecimal("1000.00"))
        assertThat(spread.splitBudget).isEqualTo(BigDecimal("1022.22"))
    }

    @Test
    fun `spreading twice compounds onto the already raised rate`() {
        val spends = listOf(1 to "80", 2 to "50")
        val twice = mapOf(2 to LeftoverChoice.SPREAD, 3 to LeftoverChoice.SPREAD)

        val dayThree = onDay(3, twice, spends)

        assertThat(dayThree.dailyBudget).isEqualTo(BigDecimal("108.75"))
        assertThat(dayThree.pendingLeftover).isEqualTo(BigDecimal("0.00"))
    }

    @Test
    fun `mixing spread then carry gives today the raised rate plus what was waiting`() {
        val spends = listOf(1 to "80", 2 to "50")
        val mixed = mapOf(2 to LeftoverChoice.SPREAD, 3 to LeftoverChoice.CARRY)

        val dayThree = onDay(3, mixed, spends)

        assertThat(dayThree.dailyBudget).isEqualTo(BigDecimal("102.22"))
        assertThat(dayThree.remainingToday).isEqualTo(BigDecimal("154.44"))
        assertThat(dayThree.pendingLeftover).isEqualTo(BigDecimal("0.00"))
    }

    @Test
    fun `mixing carry then spread shares only what is still unspent`() {
        val spends = listOf(1 to "80", 2 to "50")
        val mixed = mapOf(2 to LeftoverChoice.CARRY, 3 to LeftoverChoice.SPREAD)

        val dayThree = onDay(3, mixed, spends)

        assertThat(dayThree.dailyBudget).isEqualTo(BigDecimal("108.75"))
        assertThat(dayThree.remainingToday).isEqualTo(BigDecimal("108.75"))
    }

    @Test
    fun `answering after skipping days settles everything that piled up, not only yesterday`() {
        val spends = listOf(1 to "80", 2 to "80", 3 to "80")

        val carriedOnDayFour = onDay(4, mapOf(4 to LeftoverChoice.CARRY), spends)

        assertThat(carriedOnDayFour.remainingToday).isEqualTo(BigDecimal("160.00"))
        assertThat(carriedOnDayFour.pendingLeftover).isEqualTo(BigDecimal("0.00"))
    }

    @Test
    fun `spreading after skipping days shares the whole pile, not only yesterday`() {
        val spends = listOf(1 to "80", 2 to "80", 3 to "80")

        val spreadOnDayFour = onDay(4, mapOf(4 to LeftoverChoice.SPREAD), spends)

        assertThat(spreadOnDayFour.dailyBudget).isEqualTo(BigDecimal("108.57"))
        assertThat(spreadOnDayFour.pendingLeftover).isEqualTo(BigDecimal("0.00"))
    }

    @Test
    fun `an answer given on a past day still shapes what today looks like`() {
        val spends = listOf(1 to "80", 2 to "0")

        val withoutAnswer = onDay(5, spends = spends)
        val withPastSpread = onDay(5, mapOf(2 to LeftoverChoice.SPREAD), spends)

        assertThat(withoutAnswer.dailyBudget).isEqualTo(BigDecimal("100.00"))
        assertThat(withPastSpread.dailyBudget).isEqualTo(BigDecimal("102.22"))
    }

    @Test
    fun `an answer recorded for a day the user never reached does nothing yet`() {
        val spends = listOf(1 to "80")

        val today = onDay(2, mapOf(7 to LeftoverChoice.SPREAD), spends)

        assertThat(today.dailyBudget).isEqualTo(BigDecimal("100.00"))
        assertThat(today.pendingLeftover).isEqualTo(BigDecimal("20.00"))
    }

    @Test
    fun `an answer recorded before the period started is ignored`() {
        val spends = listOf(1 to "80")
        val staleChoice = mapOf(
            start.minusDays(3) to LeftoverChoice.CARRY,
            start.minusDays(1) to LeftoverChoice.SPREAD,
        )

        val today = calculator.calculateBudgetState(
            settings = askMeSettings(),
            transactions = spends.map { (onDay, amount) -> spend(amount, onDay) },
            currentDate = day(2),
            leftoverChoices = staleChoice,
        )

        assertThat(today.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(today.pendingLeftover).isEqualTo(BigDecimal("20.00"))
    }

    @Test
    fun `an overspend eats the waiting money first, without asking`() {
        val absorbed = onDay(3, spends = listOf(1 to "80", 2 to "110"))

        assertThat(absorbed.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(absorbed.pendingLeftover).isEqualTo(BigDecimal("10.00"))
    }

    @Test
    fun `an overspend bigger than the waiting money comes out of today, whatever the user would have picked`() {
        val spends = listOf(1 to "80", 2 to "130")

        val unanswered = onDay(3, spends = spends)
        val wouldHaveSpread = onDay(3, mapOf(3 to LeftoverChoice.SPREAD), spends)

        assertThat(unanswered.remainingToday).isEqualTo(BigDecimal("90.00"))
        assertThat(unanswered.pendingLeftover).isEqualTo(BigDecimal("0.00"))
        assertThat(wouldHaveSpread.remainingToday).isEqualTo(BigDecimal("90.00"))
        assertThat(wouldHaveSpread.dailyBudget).isEqualTo(BigDecimal("100.00"))
    }

    @Test
    fun `a debt that outlives one day keeps being taken from the days that follow`() {
        val spends = listOf(1 to "350")

        assertThat(onDay(2, spends = spends).remainingToday).isEqualTo(BigDecimal("-150.00"))
        assertThat(onDay(3, spends = spends).remainingToday).isEqualTo(BigDecimal("-50.00"))
        assertThat(onDay(4, spends = spends).remainingToday).isEqualTo(BigDecimal("50.00"))
        assertThat(onDay(4, spends = spends).pendingLeftover).isEqualTo(BigDecimal("0.00"))
    }

    @Test
    fun `no prompt is raised while the user is still paying off a debt`() {
        val spends = listOf(1 to "350")

        listOf(2, 3).forEach { dayNumber ->
            assertThat(onDay(dayNumber, spends = spends).pendingLeftover)
                .isEqualTo(BigDecimal("0.00"))
        }
    }

    @Test
    fun `a first-day rollover is offered on day one and joins the prompt if it is not spent`() {
        val carried = askMeSettings(
            totalBudget = BigDecimal("1050"),
            carryForward = true,
            rolloverLimit = BigDecimal("50"),
        )

        val dayOne = onDay(1, settings = carried)
        val dayTwo = onDay(2, settings = carried)

        assertThat(dayOne.remainingToday).isEqualTo(BigDecimal("150.00"))
        assertThat(dayTwo.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(dayTwo.pendingLeftover).isEqualTo(BigDecimal("150.00"))
    }

    @Test
    fun `a rollover applied mid-period only lands on the day it was applied`() {
        val appliedOnDayFour = askMeSettings(
            totalBudget = BigDecimal("1200"),
            carryForward = true,
            rolloverLimit = BigDecimal("200"),
            rolloverAppliedDate = day(4),
        )

        val dayThree = onDay(3, settings = appliedOnDayFour)
        val dayFour = onDay(4, settings = appliedOnDayFour)
        val dayFive = onDay(5, settings = appliedOnDayFour)

        assertThat(dayThree.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(dayFour.remainingToday).isEqualTo(BigDecimal("300.00"))
        assertThat(dayFive.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(dayFive.pendingLeftover).isEqualTo(BigDecimal("600.00"))
    }

    @Test
    fun `the rollover is kept out of the daily rate, it is a one-off not a raise`() {
        val carried = askMeSettings(
            totalBudget = BigDecimal("1050"),
            carryForward = true,
            rolloverLimit = BigDecimal("50"),
        )

        assertThat(onDay(1, settings = carried).dailyBudget).isEqualTo(BigDecimal("100.00"))
        assertThat(onDay(5, settings = carried).dailyBudget).isEqualTo(BigDecimal("100.00"))
    }

    @Test
    fun `once the period is over everything still waiting is folded into what is left`() {
        val spends = listOf(1 to "80")

        val lastDay = onDay(10, spends = spends)
        val afterTheEnd = onDay(11, spends = spends)

        assertThat(lastDay.pendingLeftover).isEqualTo(BigDecimal("820.00"))
        assertThat(lastDay.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(afterTheEnd.pendingLeftover).isEqualTo(BigDecimal("0.00"))
        assertThat(afterTheEnd.remainingToday).isEqualTo(BigDecimal("920.00"))
    }

    @Test
    fun `what is left after the end is the same whatever the user answered along the way`() {
        val spends = listOf(1 to "80", 4 to "120")
        val answers = listOf(
            emptyMap(),
            mapOf(2 to LeftoverChoice.CARRY, 5 to LeftoverChoice.CARRY),
            mapOf(2 to LeftoverChoice.SPREAD, 5 to LeftoverChoice.SPREAD),
            mapOf(3 to LeftoverChoice.SPREAD, 6 to LeftoverChoice.CARRY),
        )

        answers.forEach { choices ->
            assertThat(onDay(11, choices, spends).remainingToday).isEqualTo(BigDecimal("800.00"))
        }
    }

    @Test
    fun `a prompt raised on the last day can still be answered`() {
        val spends = listOf(1 to "80", 9 to "20")

        val unanswered = onDay(10, spends = spends)
        val carried = onDay(10, mapOf(10 to LeftoverChoice.CARRY), spends)

        assertThat(unanswered.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(unanswered.pendingLeftover).isEqualTo(BigDecimal("800.00"))
        assertThat(carried.remainingToday).isEqualTo(BigDecimal("900.00"))
        assertThat(carried.pendingLeftover).isEqualTo(BigDecimal("0.00"))
    }

    @Test
    fun `a transaction dated later in the period is charged against today, not against its own day`() {
        val bookedAhead = listOf(1 to "80", 5 to "200")

        val dayTwo = onDay(2, spends = bookedAhead)

        assertThat(dayTwo.remainingToday).isEqualTo(BigDecimal("-100.00"))
        assertThat(dayTwo.pendingLeftover).isEqualTo(BigDecimal("20.00"))
        assertThat(dayTwo.totalSpentInPeriod).isEqualTo(BigDecimal("280"))
        assertThat(dayTwo.totalSpentToday).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `a budget that does not divide evenly keeps its cents through a spread`() {
        val odd = askMeSettings(totalBudget = BigDecimal("1000.07"))

        val dayOne = onDay(1, settings = odd)
        val spreadOnDayTwo = onDay(2, mapOf(2 to LeftoverChoice.SPREAD), settings = odd)

        assertThat(dayOne.dailyBudget).isEqualTo(BigDecimal("100.01"))
        assertThat(spreadOnDayTwo.dailyBudget).isEqualTo(BigDecimal("111.12"))
        assertThat(spreadOnDayTwo.pendingLeftover).isEqualTo(BigDecimal("0.00"))
    }

    @Test
    fun `the period totals are untouched by whatever the user answers`() {
        val spends = listOf(1 to "80", 2 to "50")

        val carried = onDay(3, mapOf(3 to LeftoverChoice.CARRY), spends)
        val spread = onDay(3, mapOf(3 to LeftoverChoice.SPREAD), spends)

        listOf(carried, spread).forEach {
            assertThat(it.totalBudget).isEqualTo(BigDecimal("1000"))
            assertThat(it.totalSpentInPeriod).isEqualTo(BigDecimal("130"))
            assertThat(it.daysRemaining).isEqualTo(8)
            assertThat(it.isOverBudget).isFalse()
        }
    }
}
