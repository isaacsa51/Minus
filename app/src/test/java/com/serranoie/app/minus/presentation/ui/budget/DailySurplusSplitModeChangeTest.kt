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

/**
 * What the daily surplus does when the user switches split mode *in the middle* of a period.
 *
 * The split mode is not stored per day: [BudgetStateCalculator] re-derives the whole period from
 * the mode that is active right now. So switching is always retroactive — the history of spends
 * stays, only the reading of "what is left today" changes. These tests pin that reading for every
 * transition the behaviour sheet allows.
 *
 * Scenario: a 10-day period of 1000 (100/day), spending 60 on day 1 and 40 on day 2, looked at on
 * day 4. 200 of allowance went unused, which is exactly the number each mode treats differently.
 */
class DailySurplusSplitModeChangeTest {

    private val calculator = BudgetStateCalculator()

    private val start = LocalDate.of(2026, 7, 1)
    private val end = LocalDate.of(2026, 7, 10)
    private fun day(n: Int): LocalDate = LocalDate.of(2026, 7, n)

    private fun settings(
        splitMode: BudgetSplitMode,
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
        splitMode = splitMode,
        rollOverAppliedDate = rolloverAppliedDate,
    )

    private fun spend(amount: String, onDay: Int): Transaction = Transaction(
        id = onDay.toLong(),
        amount = BigDecimal(amount),
        date = day(onDay).atTime(12, 0),
        periodId = 1L,
    )

    /** 60 spent on day 1, 40 on day 2 — 200 of the first four days' allowance left unused. */
    private val underspentHistory = listOf(spend("60", 1), spend("40", 2))

    private fun stateOn(
        currentDay: Int,
        splitMode: BudgetSplitMode,
        transactions: List<Transaction> = underspentHistory,
        choices: Map<LocalDate, LeftoverChoice> = emptyMap(),
        settings: BudgetSettings = settings(splitMode),
    ): BudgetState = calculator.calculateBudgetState(
        settings = settings.copy(splitMode = splitMode),
        transactions = transactions,
        currentDate = day(currentDay),
        leftoverChoices = choices,
    )

    @Test
    fun `static ignores the unused allowance of earlier days - today is always a fresh daily budget`() {
        val static = stateOn(currentDay = 4, splitMode = BudgetSplitMode.STATIC)

        assertThat(static.dailyBudget).isEqualTo(BigDecimal("100.00"))
        assertThat(static.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(static.pendingLeftover).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `carry over hands the unused 200 straight to today`() {
        val carryOver = stateOn(currentDay = 4, splitMode = BudgetSplitMode.CARRY_OVER)

        assertThat(carryOver.dailyBudget).isEqualTo(BigDecimal("100.00"))
        assertThat(carryOver.remainingToday).isEqualTo(BigDecimal("300.00"))
        assertThat(carryOver.pendingLeftover).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `dynamic re-slices what is left over the days that remain instead of banking it`() {
        val dynamic = stateOn(currentDay = 4, splitMode = BudgetSplitMode.DYNAMIC)

        // 900 left across the 7 remaining days
        assertThat(dynamic.dailyBudget).isEqualTo(BigDecimal("128.57"))
        assertThat(dynamic.remainingToday).isEqualTo(BigDecimal("128.57"))
    }

    @Test
    fun `ask me parks the unused 200 as a pending leftover and leaves today untouched until the user decides`() {
        val askMe = stateOn(currentDay = 4, splitMode = BudgetSplitMode.ASK_ME)

        assertThat(askMe.dailyBudget).isEqualTo(BigDecimal("100.00"))
        assertThat(askMe.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(askMe.pendingLeftover).isEqualTo(BigDecimal("200.00"))
    }

    @Test
    fun `switching static to carry over mid-period retroactively releases every unused day`() {
        val before = stateOn(currentDay = 4, splitMode = BudgetSplitMode.STATIC)
        val after = stateOn(currentDay = 4, splitMode = BudgetSplitMode.CARRY_OVER)

        assertThat(after.remainingToday.subtract(before.remainingToday))
            .isEqualTo(BigDecimal("200.00"))
    }

    @Test
    fun `switching carry over back to static mid-period drops the banked surplus, it is not spendable any more`() {
        val before = stateOn(currentDay = 4, splitMode = BudgetSplitMode.CARRY_OVER)
        val after = stateOn(currentDay = 4, splitMode = BudgetSplitMode.STATIC)

        assertThat(before.remainingToday).isEqualTo(BigDecimal("300.00"))
        assertThat(after.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(after.dailyBudget).isEqualTo(before.dailyBudget)
    }

    @Test
    fun `switching static to dynamic mid-period raises the daily budget without giving today a lump sum`() {
        val before = stateOn(currentDay = 4, splitMode = BudgetSplitMode.STATIC)
        val after = stateOn(currentDay = 4, splitMode = BudgetSplitMode.DYNAMIC)

        assertThat(after.dailyBudget).isGreaterThan(before.dailyBudget)
        assertThat(after.remainingToday).isEqualTo(after.dailyBudget)
    }

    @Test
    fun `switching carry over to dynamic mid-period spreads the surplus instead of banking it on today`() {
        val carryOver = stateOn(currentDay = 4, splitMode = BudgetSplitMode.CARRY_OVER)
        val dynamic = stateOn(currentDay = 4, splitMode = BudgetSplitMode.DYNAMIC)

        assertThat(carryOver.remainingToday).isEqualTo(BigDecimal("300.00"))
        assertThat(dynamic.remainingToday).isEqualTo(BigDecimal("128.57"))
        assertThat(carryOver.totalBudget.subtract(carryOver.totalSpentInPeriod))
            .isEqualTo(dynamic.totalBudget.subtract(dynamic.totalSpentInPeriod))
    }

    @Test
    fun `switching carry over to ask me mid-period moves the banked surplus into the pending prompt`() {
        val carryOver = stateOn(currentDay = 4, splitMode = BudgetSplitMode.CARRY_OVER)
        val askMe = stateOn(currentDay = 4, splitMode = BudgetSplitMode.ASK_ME)

        assertThat(askMe.remainingToday.add(askMe.pendingLeftover))
            .isEqualTo(carryOver.remainingToday)
    }

    @Test
    fun `switching ask me to carry over mid-period settles the pending prompt onto today`() {
        val askMe = stateOn(currentDay = 4, splitMode = BudgetSplitMode.ASK_ME)
        val carryOver = stateOn(currentDay = 4, splitMode = BudgetSplitMode.CARRY_OVER)

        assertThat(askMe.pendingLeftover).isEqualTo(BigDecimal("200.00"))
        assertThat(carryOver.pendingLeftover).isEqualTo(BigDecimal.ZERO)
        assertThat(carryOver.remainingToday).isEqualTo(BigDecimal("300.00"))
    }

    @Test
    fun `ask me with carry picked every day is the same reading as carry over`() {
        val everyDayCarried = (1..4).associate { day(it) to LeftoverChoice.CARRY }

        val askMe = stateOn(
            currentDay = 4,
            splitMode = BudgetSplitMode.ASK_ME,
            choices = everyDayCarried,
        )
        val carryOver = stateOn(currentDay = 4, splitMode = BudgetSplitMode.CARRY_OVER)

        assertThat(askMe.remainingToday).isEqualTo(carryOver.remainingToday)
        assertThat(askMe.dailyBudget).isEqualTo(carryOver.dailyBudget)
        assertThat(askMe.pendingLeftover).isEqualTo(BigDecimal("0.00"))
    }

    @Test
    fun `ask me with spread picked today lands on the same daily budget as dynamic`() {
        val askMe = stateOn(
            currentDay = 4,
            splitMode = BudgetSplitMode.ASK_ME,
            choices = mapOf(day(4) to LeftoverChoice.SPREAD),
        )
        val dynamic = stateOn(currentDay = 4, splitMode = BudgetSplitMode.DYNAMIC)

        assertThat(askMe.dailyBudget).isEqualTo(dynamic.dailyBudget)
        assertThat(askMe.remainingToday).isEqualTo(dynamic.remainingToday)
        assertThat(askMe.pendingLeftover).isEqualTo(BigDecimal("0.00"))
    }

    @Test
    fun `switching static to ask me mid-period surfaces every skipped day at once as one pending prompt`() {
        val static = stateOn(currentDay = 4, splitMode = BudgetSplitMode.STATIC)
        val askMe = stateOn(currentDay = 4, splitMode = BudgetSplitMode.ASK_ME)

        assertThat(static.pendingLeftover).isEqualTo(BigDecimal.ZERO)
        assertThat(askMe.pendingLeftover).isEqualTo(BigDecimal("200.00"))
        assertThat(askMe.remainingToday).isEqualTo(static.remainingToday)
    }

    @Test
    fun `switching ask me to static mid-period discards the pending prompt entirely`() {
        val askMe = stateOn(currentDay = 4, splitMode = BudgetSplitMode.ASK_ME)
        val static = stateOn(currentDay = 4, splitMode = BudgetSplitMode.STATIC)

        assertThat(askMe.pendingLeftover).isEqualTo(BigDecimal("200.00"))
        assertThat(static.pendingLeftover).isEqualTo(BigDecimal.ZERO)
        assertThat(static.remainingToday).isEqualTo(BigDecimal("100.00"))
    }

    @Test
    fun `switching ask me to static then back to ask me restores the pending prompt, nothing was consumed`() {
        val before = stateOn(currentDay = 4, splitMode = BudgetSplitMode.ASK_ME)
        stateOn(currentDay = 4, splitMode = BudgetSplitMode.STATIC)
        val after = stateOn(currentDay = 4, splitMode = BudgetSplitMode.ASK_ME)

        assertThat(after.pendingLeftover).isEqualTo(before.pendingLeftover)
        assertThat(after.remainingToday).isEqualTo(before.remainingToday)
    }

    @Test
    fun `an ask me choice made before the switch away still applies when the user switches back`() {
        val choices = mapOf(day(3) to LeftoverChoice.SPREAD)

        val beforeSwitch = stateOn(currentDay = 4, splitMode = BudgetSplitMode.ASK_ME, choices = choices)
        stateOn(currentDay = 4, splitMode = BudgetSplitMode.CARRY_OVER, choices = choices)
        val afterSwitchBack = stateOn(currentDay = 4, splitMode = BudgetSplitMode.ASK_ME, choices = choices)

        assertThat(afterSwitchBack.dailyBudget).isEqualTo(beforeSwitch.dailyBudget)
        assertThat(afterSwitchBack.remainingToday).isEqualTo(beforeSwitch.remainingToday)
    }

    @Test
    fun `no mode switch changes the money actually spent, the period total or the days left`() {
        val readings = BudgetSplitMode.entries.map { stateOn(currentDay = 4, splitMode = it) }

        readings.forEach { reading ->
            assertThat(reading.totalSpentInPeriod).isEqualTo(BigDecimal("100"))
            assertThat(reading.totalBudget).isEqualTo(BigDecimal("1000"))
            assertThat(reading.daysRemaining).isEqualTo(7)
            assertThat(reading.periodTotalDays).isEqualTo(10)
            assertThat(reading.progress).isEqualTo(0.1f)
            assertThat(reading.isOverBudget).isFalse()
        }
    }

    @Test
    fun `no mode switch changes today's spend, even when today already has transactions`() {
        val withSpendToday = underspentHistory + spend("25", 4)

        val readings = BudgetSplitMode.entries.map {
            stateOn(currentDay = 4, splitMode = it, transactions = withSpendToday)
        }

        readings.forEach { assertThat(it.totalSpentToday).isEqualTo(BigDecimal("25")) }
    }

    @Test
    fun `today's spend comes off today's allowance in every mode`() {
        fun remainingWith(splitMode: BudgetSplitMode, spentToday: String) = stateOn(
            currentDay = 4,
            splitMode = splitMode,
            transactions = underspentHistory + spend(spentToday, 4),
        ).remainingToday

        assertThat(remainingWith(BudgetSplitMode.STATIC, "25")).isEqualTo(BigDecimal("75.00"))
        assertThat(remainingWith(BudgetSplitMode.CARRY_OVER, "25")).isEqualTo(BigDecimal("275.00"))
        assertThat(remainingWith(BudgetSplitMode.ASK_ME, "25")).isEqualTo(BigDecimal("75.00"))
        // dynamic adds today's spend back before re-slicing, so the rate stays 128.57 all day
        // and only today's 25 comes off it
        assertThat(remainingWith(BudgetSplitMode.DYNAMIC, "25")).isEqualTo(BigDecimal("103.57"))
    }

    @Test
    fun `yesterday's overspend follows the user in carry over and ask me, but not in static`() {
        val overspent = listOf(spend("150", 1))

        val static = stateOn(2, BudgetSplitMode.STATIC, overspent)
        val carryOver = stateOn(2, BudgetSplitMode.CARRY_OVER, overspent)
        val askMe = stateOn(2, BudgetSplitMode.ASK_ME, overspent)

        assertThat(static.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(carryOver.remainingToday).isEqualTo(BigDecimal("50.00"))
        assertThat(askMe.remainingToday).isEqualTo(BigDecimal("50.00"))
    }

    @Test
    fun `switching to ask me after an overspend never parks a debt as a pending prompt, it is taken today`() {
        val overspent = listOf(spend("150", 1))

        val askMe = stateOn(2, BudgetSplitMode.ASK_ME, overspent)

        assertThat(askMe.pendingLeftover).isEqualTo(BigDecimal("0.00"))
        assertThat(askMe.remainingToday).isEqualTo(BigDecimal("50.00"))
    }

    @Test
    fun `switching to static after blowing the whole budget still reports the period as over budget`() {
        val blownBudget = listOf(spend("1200", 1))

        BudgetSplitMode.entries.forEach { mode ->
            val reading = stateOn(4, mode, blownBudget)
            assertThat(reading.isOverBudget).isTrue()
            assertThat(reading.progress).isEqualTo(1f)
        }
    }

    @Test
    fun `a debt larger than one day keeps eating into the following days in carry over and ask me`() {
        val hugeOverspend = listOf(spend("450", 1))

        assertThat(stateOn(2, BudgetSplitMode.CARRY_OVER, hugeOverspend).remainingToday)
            .isEqualTo(BigDecimal("-250.00"))
        assertThat(stateOn(2, BudgetSplitMode.ASK_ME, hugeOverspend).remainingToday)
            .isEqualTo(BigDecimal("-250.00"))
        assertThat(stateOn(5, BudgetSplitMode.CARRY_OVER, hugeOverspend).remainingToday)
            .isEqualTo(BigDecimal("50.00"))
        assertThat(stateOn(2, BudgetSplitMode.STATIC, hugeOverspend).remainingToday)
            .isEqualTo(BigDecimal("100.00"))
    }

    @Test
    fun `a first-day rollover survives every mode switch in the period total`() {
        val carried = settings(
            splitMode = BudgetSplitMode.STATIC,
            totalBudget = BigDecimal("1050"),
            carryForward = true,
            rolloverLimit = BigDecimal("50"),
        )

        BudgetSplitMode.entries.forEach { mode ->
            val reading = stateOn(2, mode, emptyList(), settings = carried)
            assertThat(reading.totalBudget).isEqualTo(BigDecimal("1050"))
            assertThat(reading.dailyBudget).isEqualTo(
                if (mode == BudgetSplitMode.DYNAMIC) BigDecimal("116.67") else BigDecimal("100.00"),
            )
        }
    }

    @Test
    fun `an unspent first-day rollover is banked by carry over, parked by ask me and dropped by static`() {
        val carried = settings(
            splitMode = BudgetSplitMode.STATIC,
            totalBudget = BigDecimal("1050"),
            carryForward = true,
            rolloverLimit = BigDecimal("50"),
        )

        val static = stateOn(2, BudgetSplitMode.STATIC, emptyList(), settings = carried)
        val carryOver = stateOn(2, BudgetSplitMode.CARRY_OVER, emptyList(), settings = carried)
        val askMe = stateOn(2, BudgetSplitMode.ASK_ME, emptyList(), settings = carried)

        assertThat(static.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(carryOver.remainingToday).isEqualTo(BigDecimal("250.00"))
        assertThat(askMe.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(askMe.pendingLeftover).isEqualTo(BigDecimal("150.00"))
    }

    @Test
    fun `a mid-period rollover only shows up from the day it was applied, whatever the mode`() {
        val appliedOnDay4 = settings(
            splitMode = BudgetSplitMode.STATIC,
            totalBudget = BigDecimal("1200"),
            carryForward = true,
            rolloverLimit = BigDecimal("200"),
            rolloverAppliedDate = day(4),
        )

        val staticBefore = stateOn(3, BudgetSplitMode.STATIC, emptyList(), settings = appliedOnDay4)
        val staticOnDay = stateOn(4, BudgetSplitMode.STATIC, emptyList(), settings = appliedOnDay4)
        val carryBefore = stateOn(3, BudgetSplitMode.CARRY_OVER, emptyList(), settings = appliedOnDay4)
        val carryOnDay = stateOn(4, BudgetSplitMode.CARRY_OVER, emptyList(), settings = appliedOnDay4)

        assertThat(staticBefore.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(staticOnDay.remainingToday).isEqualTo(BigDecimal("300.00"))
        assertThat(carryBefore.remainingToday).isEqualTo(BigDecimal("300.00"))
        assertThat(carryOnDay.remainingToday).isEqualTo(BigDecimal("600.00"))
    }

    @Test
    fun `on day one every mode agrees, there is no surplus to argue about yet`() {
        val readings = BudgetSplitMode.entries.associateWith { stateOn(1, it, emptyList()) }

        readings.values.forEach {
            assertThat(it.remainingToday).isEqualTo(BigDecimal("100.00"))
            assertThat(it.dailyBudget).isEqualTo(BigDecimal("100.00"))
            assertThat(it.pendingLeftover).isEqualTo(BigDecimal.ZERO.setScale(it.pendingLeftover.scale()))
        }
    }

    @Test
    fun `on the last day carry over and ask me hand over everything that is left, static still shows one day`() {
        val static = stateOn(10, BudgetSplitMode.STATIC, underspentHistory)
        val carryOver = stateOn(10, BudgetSplitMode.CARRY_OVER, underspentHistory)
        val askMeCarried = stateOn(
            10,
            BudgetSplitMode.ASK_ME,
            underspentHistory,
            choices = mapOf(day(10) to LeftoverChoice.CARRY),
        )

        assertThat(static.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(carryOver.remainingToday).isEqualTo(BigDecimal("900.00"))
        assertThat(askMeCarried.remainingToday).isEqualTo(BigDecimal("900.00"))
        assertThat(carryOver.daysRemaining).isEqualTo(1)
    }

    @Test
    fun `once the period is over dynamic stops offering a daily budget while the other modes report the leftover`() {
        val afterEnd = 11

        val static = stateOn(afterEnd, BudgetSplitMode.STATIC, underspentHistory)
        val dynamic = stateOn(afterEnd, BudgetSplitMode.DYNAMIC, underspentHistory)
        val carryOver = stateOn(afterEnd, BudgetSplitMode.CARRY_OVER, underspentHistory)
        val askMe = stateOn(afterEnd, BudgetSplitMode.ASK_ME, underspentHistory)

        assertThat(dynamic.dailyBudget).isEqualTo(BigDecimal.ZERO)
        assertThat(static.dailyBudget).isEqualTo(BigDecimal("100.00"))
        assertThat(carryOver.remainingToday).isEqualTo(BigDecimal("900.00"))
        assertThat(askMe.remainingToday).isEqualTo(BigDecimal("900.00"))
        assertThat(askMe.pendingLeftover).isEqualTo(BigDecimal("0.00"))
        assertThat(carryOver.daysRemaining).isEqualTo(0)
    }

    @Test
    fun `income received today lands on today in every mode and lifts the period total`() {
        val withIncome = underspentHistory + Transaction(
            id = 99L,
            amount = BigDecimal("-300"),
            date = day(4).atTime(9, 0),
            periodId = 1L,
        )

        val static = stateOn(4, BudgetSplitMode.STATIC, withIncome)
        val dynamic = stateOn(4, BudgetSplitMode.DYNAMIC, withIncome)
        val carryOver = stateOn(4, BudgetSplitMode.CARRY_OVER, withIncome)
        val askMe = stateOn(4, BudgetSplitMode.ASK_ME, withIncome)

        listOf(static, dynamic, carryOver, askMe).forEach {
            assertThat(it.totalBudget).isEqualTo(BigDecimal("1300"))
        }
        assertThat(static.remainingToday).isEqualTo(BigDecimal("400.00"))
        assertThat(carryOver.remainingToday).isEqualTo(BigDecimal("600.00"))
        // ask me keeps its rate at the plain daily share; the income is simply not an outflow today
        assertThat(askMe.dailyBudget).isEqualTo(BigDecimal("100.00"))
        assertThat(askMe.remainingToday).isEqualTo(BigDecimal("400.00"))
        assertThat(askMe.pendingLeftover).isEqualTo(BigDecimal("200.00"))
        // dynamic also re-slices it across the days that are left
        assertThat(dynamic.dailyBudget).isEqualTo(BigDecimal("171.43"))
        assertThat(dynamic.remainingToday).isEqualTo(BigDecimal("471.43"))
    }

    @Test
    fun `income received on an earlier day is parked behind the ask me prompt, not handed to today`() {
        val incomeOnDayTwo = underspentHistory + Transaction(
            id = 99L,
            amount = BigDecimal("-300"),
            date = day(2).atTime(9, 0),
            periodId = 1L,
        )

        val askMe = stateOn(4, BudgetSplitMode.ASK_ME, incomeOnDayTwo)
        val carryOver = stateOn(4, BudgetSplitMode.CARRY_OVER, incomeOnDayTwo)

        assertThat(askMe.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(askMe.pendingLeftover).isEqualTo(BigDecimal("500.00"))
        assertThat(carryOver.remainingToday).isEqualTo(BigDecimal("600.00"))
        assertThat(askMe.remainingToday.add(askMe.pendingLeftover))
            .isEqualTo(carryOver.remainingToday)
    }

    @Test
    fun `a one-day period reads the same in every mode`() {
        val singleDay = settings(BudgetSplitMode.STATIC).copy(endDate = start)

        BudgetSplitMode.entries.forEach { mode ->
            val reading = calculator.calculateBudgetState(
                settings = singleDay.copy(splitMode = mode),
                transactions = listOf(spend("250", 1)),
                currentDate = start,
            )
            assertThat(reading.remainingToday).isEqualTo(BigDecimal("750.00"))
            assertThat(reading.daysRemaining).isEqualTo(1)
        }
    }
}
