package com.serranoie.app.minus.presentation.ui.e2e.budget

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.budget.BudgetStateCalculator
import com.serranoie.app.minus.presentation.ui.editor.sheets.BUDGET_PERIOD_APPLY_BUTTON_TAG
import com.serranoie.app.minus.presentation.ui.editor.sheets.BUDGET_PERIOD_NEXT_BUTTON_TAG
import com.serranoie.app.minus.presentation.ui.editor.sheets.BudgetPeriodSheet
import com.serranoie.app.minus.presentation.ui.editor.sheets.budgetSplitModeOptionTag
import com.serranoie.app.minus.presentation.ui.editor.sheets.budgetStrategyOptionTag
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.BUDGET_PERIOD_CALCULATED_CARD_TAG
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Changing how the daily budget is split *while a period is running*, from the budget sheet.
 *
 * The user opens the sheet, steps into the behaviour screen, picks another split mode (and/or
 * another end-of-period surplus strategy) and applies. Two things have to hold: the settings the
 * app is handed must carry the new choice while leaving the live period alone (same dates, same
 * total, same rollover already in flight), and the number on the pill has to be re-read under the
 * new mode.
 *
 * Fixture: a 10-day period of 1000, on day 4, having spent 100 — so 200 of past allowance is
 * unused and each mode disagrees about it.
 */
class SplitModeChangeMidPeriodE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val calculator = BudgetStateCalculator()

    private val today: LocalDate = LocalDate.now()
    private val periodStart: LocalDate = today.minusDays(3)
    private val periodEnd: LocalDate = today.plusDays(6)

    private fun midPeriodSettings(
        splitMode: BudgetSplitMode = BudgetSplitMode.STATIC,
        strategy: RemainingBudgetStrategy = RemainingBudgetStrategy.ASK_ALWAYS,
        totalBudget: BigDecimal = BigDecimal("1000.00"),
        rolloverLimit: BigDecimal? = null,
        carryForward: Boolean = false,
    ) = BudgetSettings(
        totalBudget = totalBudget,
        period = BudgetPeriod.DAILY,
        startDate = periodStart,
        endDate = periodEnd,
        currencyCode = "USD",
        daysInPeriod = 10,
        rollOverEnabled = carryForward,
        rollOverLimit = rolloverLimit,
        rollOverCarryForward = carryForward,
        remainingBudgetStrategy = strategy,
        splitMode = splitMode,
        rollOverAppliedDate = if (carryForward) periodStart else null,
    )

    /** 60 spent on day one and 40 on day two, seen from day four. */
    private val spentSoFar = listOf(
        Transaction(
            id = 1L,
            amount = BigDecimal("60.00"),
            date = periodStart.atTime(12, 0),
            periodId = 1L,
        ),
        Transaction(
            id = 2L,
            amount = BigDecimal("40.00"),
            date = periodStart.plusDays(1).atTime(12, 0),
            periodId = 1L,
        ),
    )

    private fun stateFor(settings: BudgetSettings): BudgetState = calculator.calculateBudgetState(
        settings = settings,
        transactions = spentSoFar,
        currentDate = today,
    )

    private fun usd(amount: BigDecimal): String = symbolOnlyCurrencyFormat("USD").format(amount)

    private fun renderSheet(
        settings: BudgetSettings,
        startInEditMode: Boolean = false,
        captured: MutableList<BudgetSettings> = mutableListOf(),
    ) {
        composeTestRule.setContent {
            MinusTheme {
                BudgetPeriodSheet(
                    budgetSettings = settings,
                    budgetState = stateFor(settings),
                    selectedPeriod = BudgetPeriod.DAILY,
                    currencyCode = "USD",
                    onPeriodSelected = {},
                    onSaveBudget = { captured += it },
                    onEditBudget = {},
                    onFinishEarly = {},
                    startInEditMode = startInEditMode,
                    pendingExpensesCount = 0,
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(600)
    }

    private fun openBehaviourStep() {
        composeTestRule.onNodeWithTag(BUDGET_PERIOD_NEXT_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(600)
    }

    private fun pickSplitMode(mode: BudgetSplitMode) {
        composeTestRule.onNodeWithTag(budgetSplitModeOptionTag(mode)).performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    private fun pickStrategy(strategy: RemainingBudgetStrategy) {
        composeTestRule.onNodeWithTag(budgetStrategyOptionTag(strategy)).performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    private fun apply() {
        composeTestRule.onNodeWithTag(BUDGET_PERIOD_APPLY_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()
    }

    /** Opens the behaviour step on a period already running in [from], switches to [to], applies. */
    private fun switchMidPeriod(
        from: BudgetSplitMode,
        to: BudgetSplitMode,
        settings: BudgetSettings = midPeriodSettings(splitMode = from),
    ): BudgetSettings {
        val captured = mutableListOf<BudgetSettings>()
        renderSheet(settings, startInEditMode = true, captured = captured)
        openBehaviourStep()
        pickSplitMode(to)
        apply()
        return captured.last()
    }

    // ---------------------------------------------------------------------------------------
    // The behaviour screen shows what is running now
    // ---------------------------------------------------------------------------------------

    @Test
    fun when_editing_mid_period_then_the_running_split_mode_is_the_selected_one() {
        renderSheet(midPeriodSettings(splitMode = BudgetSplitMode.CARRY_OVER), startInEditMode = true)
        openBehaviourStep()

        composeTestRule.onNodeWithTag(budgetSplitModeOptionTag(BudgetSplitMode.CARRY_OVER))
            .performScrollTo()
            .assertIsSelected()
        composeTestRule.onNodeWithTag(budgetSplitModeOptionTag(BudgetSplitMode.STATIC))
            .performScrollTo()
            .assertIsNotSelected()
    }

    @Test
    fun when_editing_mid_period_then_all_four_split_modes_are_offered() {
        renderSheet(midPeriodSettings(), startInEditMode = true)
        openBehaviourStep()

        BudgetSplitMode.entries.forEach { mode ->
            composeTestRule.onNodeWithTag(budgetSplitModeOptionTag(mode))
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun when_editing_mid_period_then_the_running_surplus_strategy_is_the_selected_one() {
        renderSheet(
            midPeriodSettings(strategy = RemainingBudgetStrategy.ADD_TO_FIRST_DAY),
            startInEditMode = true,
        )
        openBehaviourStep()

        composeTestRule.onNodeWithTag(budgetStrategyOptionTag(RemainingBudgetStrategy.ADD_TO_FIRST_DAY))
            .performScrollTo()
            .assertIsSelected()
        composeTestRule.onNodeWithTag(budgetStrategyOptionTag(RemainingBudgetStrategy.ASK_ALWAYS))
            .performScrollTo()
            .assertIsNotSelected()
    }

    // ---------------------------------------------------------------------------------------
    // Switching from one mode to another
    // ---------------------------------------------------------------------------------------

    @Test
    fun when_switching_static_to_carry_over_then_the_saved_settings_carry_the_new_mode() {
        val saved = switchMidPeriod(from = BudgetSplitMode.STATIC, to = BudgetSplitMode.CARRY_OVER)

        assertThat(saved.splitMode).isEqualTo(BudgetSplitMode.CARRY_OVER)
    }

    @Test
    fun when_switching_carry_over_to_static_then_the_saved_settings_carry_the_new_mode() {
        val saved = switchMidPeriod(from = BudgetSplitMode.CARRY_OVER, to = BudgetSplitMode.STATIC)

        assertThat(saved.splitMode).isEqualTo(BudgetSplitMode.STATIC)
    }

    @Test
    fun when_switching_static_to_ask_me_then_the_saved_settings_carry_the_new_mode() {
        val saved = switchMidPeriod(from = BudgetSplitMode.STATIC, to = BudgetSplitMode.ASK_ME)

        assertThat(saved.splitMode).isEqualTo(BudgetSplitMode.ASK_ME)
    }

    @Test
    fun when_switching_carry_over_to_ask_me_then_the_saved_settings_carry_the_new_mode() {
        val saved = switchMidPeriod(from = BudgetSplitMode.CARRY_OVER, to = BudgetSplitMode.ASK_ME)

        assertThat(saved.splitMode).isEqualTo(BudgetSplitMode.ASK_ME)
    }

    @Test
    fun when_switching_ask_me_to_carry_over_then_the_saved_settings_carry_the_new_mode() {
        val saved = switchMidPeriod(from = BudgetSplitMode.ASK_ME, to = BudgetSplitMode.CARRY_OVER)

        assertThat(saved.splitMode).isEqualTo(BudgetSplitMode.CARRY_OVER)
    }

    @Test
    fun when_switching_ask_me_to_dynamic_then_the_saved_settings_carry_the_new_mode() {
        val saved = switchMidPeriod(from = BudgetSplitMode.ASK_ME, to = BudgetSplitMode.DYNAMIC)

        assertThat(saved.splitMode).isEqualTo(BudgetSplitMode.DYNAMIC)
    }

    @Test
    fun when_tapping_through_several_modes_then_only_the_last_one_is_saved() {
        val captured = mutableListOf<BudgetSettings>()
        renderSheet(midPeriodSettings(), startInEditMode = true, captured = captured)
        openBehaviourStep()

        pickSplitMode(BudgetSplitMode.DYNAMIC)
        pickSplitMode(BudgetSplitMode.ASK_ME)
        pickSplitMode(BudgetSplitMode.CARRY_OVER)
        apply()

        assertThat(captured.single().splitMode).isEqualTo(BudgetSplitMode.CARRY_OVER)
    }

    @Test
    fun when_picking_a_mode_then_the_previous_one_is_deselected() {
        renderSheet(midPeriodSettings(splitMode = BudgetSplitMode.STATIC), startInEditMode = true)
        openBehaviourStep()

        pickSplitMode(BudgetSplitMode.ASK_ME)

        composeTestRule.onNodeWithTag(budgetSplitModeOptionTag(BudgetSplitMode.ASK_ME))
            .performScrollTo()
            .assertIsSelected()
        composeTestRule.onNodeWithTag(budgetSplitModeOptionTag(BudgetSplitMode.STATIC))
            .performScrollTo()
            .assertIsNotSelected()
    }

    @Test
    fun when_applying_without_touching_anything_then_the_running_mode_is_kept() {
        val captured = mutableListOf<BudgetSettings>()
        renderSheet(
            midPeriodSettings(splitMode = BudgetSplitMode.ASK_ME),
            startInEditMode = true,
            captured = captured,
        )
        openBehaviourStep()
        apply()

        assertThat(captured.single().splitMode).isEqualTo(BudgetSplitMode.ASK_ME)
    }

    // ---------------------------------------------------------------------------------------
    // A mid-period switch must not disturb the period itself
    // ---------------------------------------------------------------------------------------

    @Test
    fun when_switching_mode_mid_period_then_the_dates_and_the_total_are_untouched() {
        val saved = switchMidPeriod(from = BudgetSplitMode.STATIC, to = BudgetSplitMode.CARRY_OVER)

        assertThat(saved.startDate).isEqualTo(periodStart)
        assertThat(saved.endDate).isEqualTo(periodEnd)
        assertThat(saved.totalBudget).isEqualTo(BigDecimal("1000.00"))
        assertThat(saved.daysInPeriod).isEqualTo(10)
        assertThat(saved.currencyCode).isEqualTo("USD")
    }

    @Test
    fun when_switching_mode_mid_period_then_a_rollover_already_in_flight_is_preserved() {
        val withRollover = midPeriodSettings(
            splitMode = BudgetSplitMode.STATIC,
            totalBudget = BigDecimal("1200.00"),
            rolloverLimit = BigDecimal("200.00"),
            carryForward = true,
        )

        val saved = switchMidPeriod(
            from = BudgetSplitMode.STATIC,
            to = BudgetSplitMode.ASK_ME,
            settings = withRollover,
        )

        assertThat(saved.rollOverCarryForward).isTrue()
        assertThat(saved.rollOverLimit).isEqualTo(BigDecimal("200.00"))
        assertThat(saved.rollOverAppliedDate).isEqualTo(periodStart)
        assertThat(saved.totalBudget).isEqualTo(BigDecimal("1200.00"))
    }

    @Test
    fun when_switching_mode_mid_period_then_the_surplus_strategy_is_left_as_it_was() {
        val saved = switchMidPeriod(
            from = BudgetSplitMode.STATIC,
            to = BudgetSplitMode.CARRY_OVER,
            settings = midPeriodSettings(strategy = RemainingBudgetStrategy.SPLIT_EQUALLY),
        )

        assertThat(saved.remainingBudgetStrategy).isEqualTo(RemainingBudgetStrategy.SPLIT_EQUALLY)
    }

    // ---------------------------------------------------------------------------------------
    // Changing the end-of-period surplus strategy mid-period
    // ---------------------------------------------------------------------------------------

    @Test
    fun when_switching_the_surplus_strategy_to_spread_then_it_is_saved_without_touching_the_split_mode() {
        val captured = mutableListOf<BudgetSettings>()
        renderSheet(
            midPeriodSettings(
                splitMode = BudgetSplitMode.CARRY_OVER,
                strategy = RemainingBudgetStrategy.ASK_ALWAYS,
            ),
            startInEditMode = true,
            captured = captured,
        )
        openBehaviourStep()
        pickStrategy(RemainingBudgetStrategy.SPLIT_EQUALLY)
        apply()

        val saved = captured.single()
        assertThat(saved.remainingBudgetStrategy).isEqualTo(RemainingBudgetStrategy.SPLIT_EQUALLY)
        assertThat(saved.splitMode).isEqualTo(BudgetSplitMode.CARRY_OVER)
    }

    @Test
    fun when_switching_the_surplus_strategy_to_ask_me_then_it_is_saved() {
        val captured = mutableListOf<BudgetSettings>()
        renderSheet(
            midPeriodSettings(strategy = RemainingBudgetStrategy.SPLIT_EQUALLY),
            startInEditMode = true,
            captured = captured,
        )
        openBehaviourStep()
        pickStrategy(RemainingBudgetStrategy.ASK_ALWAYS)
        apply()

        assertThat(captured.single().remainingBudgetStrategy)
            .isEqualTo(RemainingBudgetStrategy.ASK_ALWAYS)
    }

    @Test
    fun when_changing_both_the_strategy_and_the_split_mode_then_both_are_saved_together() {
        val captured = mutableListOf<BudgetSettings>()
        renderSheet(
            midPeriodSettings(
                splitMode = BudgetSplitMode.STATIC,
                strategy = RemainingBudgetStrategy.ASK_ALWAYS,
            ),
            startInEditMode = true,
            captured = captured,
        )
        openBehaviourStep()
        pickStrategy(RemainingBudgetStrategy.ADD_TO_FIRST_DAY)
        pickSplitMode(BudgetSplitMode.ASK_ME)
        apply()

        val saved = captured.single()
        assertThat(saved.remainingBudgetStrategy).isEqualTo(RemainingBudgetStrategy.ADD_TO_FIRST_DAY)
        assertThat(saved.splitMode).isEqualTo(BudgetSplitMode.ASK_ME)
    }

    // ---------------------------------------------------------------------------------------
    // What the sheet reads back once the new mode is live
    // ---------------------------------------------------------------------------------------

    @Test
    fun when_the_period_runs_on_static_then_the_card_shows_the_plain_daily_share() {
        renderSheet(midPeriodSettings(splitMode = BudgetSplitMode.STATIC))

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_CALCULATED_CARD_TAG).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(usd(BigDecimal("100.00"))).onLast().assertIsDisplayed()
    }

    @Test
    fun when_the_period_runs_on_carry_over_then_the_card_shows_today_plus_what_was_banked() {
        renderSheet(midPeriodSettings(splitMode = BudgetSplitMode.CARRY_OVER))

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_CALCULATED_CARD_TAG).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(usd(BigDecimal("300.00"))).onLast().assertIsDisplayed()
    }

    @Test
    fun when_the_period_runs_on_dynamic_then_the_card_shows_what_is_left_over_the_days_that_remain() {
        renderSheet(midPeriodSettings(splitMode = BudgetSplitMode.DYNAMIC))

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_CALCULATED_CARD_TAG).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(usd(BigDecimal("128.57"))).onLast().assertIsDisplayed()
    }

    @Test
    fun when_the_period_runs_on_ask_me_then_the_card_shows_only_what_was_already_decided() {
        renderSheet(midPeriodSettings(splitMode = BudgetSplitMode.ASK_ME))

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_CALCULATED_CARD_TAG).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(usd(BigDecimal("100.00"))).onLast().assertIsDisplayed()
    }

    @Test
    fun when_the_mode_changes_then_the_same_history_reads_out_differently() {
        val static = stateFor(midPeriodSettings(splitMode = BudgetSplitMode.STATIC))
        val carryOver = stateFor(midPeriodSettings(splitMode = BudgetSplitMode.CARRY_OVER))
        val askMe = stateFor(midPeriodSettings(splitMode = BudgetSplitMode.ASK_ME))

        assertThat(static.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(carryOver.remainingToday).isEqualTo(BigDecimal("300.00"))
        assertThat(askMe.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(askMe.pendingLeftover).isEqualTo(BigDecimal("200.00"))
        // the money itself never moved
        listOf(static, carryOver, askMe).forEach {
            assertThat(it.totalSpentInPeriod).isEqualTo(BigDecimal("100.00"))
            assertThat(it.totalBudget).isEqualTo(BigDecimal("1000.00"))
        }
    }
}
