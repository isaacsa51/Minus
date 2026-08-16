package com.serranoie.app.minus.presentation.ui.e2e.budget

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.presentation.ui.editor.sheets.BUDGET_PERIOD_FINISH_EARLY_BUTTON_TAG
import com.serranoie.app.minus.presentation.ui.editor.sheets.BUDGET_PERIOD_ROLLOVER_PREVIEW_BANNER_TAG
import com.serranoie.app.minus.presentation.ui.editor.sheets.BudgetPeriodSheet
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

class PeriodEndingE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun scenario(
        totalBudget: BigDecimal,
        totalSpent: BigDecimal,
        daysLeft: Int,
        strategy: RemainingBudgetStrategy = RemainingBudgetStrategy.ASK_ALWAYS,
    ): Pair<BudgetSettings, BudgetState> {
        val end = LocalDate.now().plusDays((daysLeft - 1).toLong())
        val start = end.minusDays(29)
        val settings = BudgetSettings(
            totalBudget = totalBudget,
            period = BudgetPeriod.MONTHLY,
            startDate = start,
            endDate = end,
            currencyCode = "USD",
            daysInPeriod = 30,
            remainingBudgetStrategy = strategy,
        )
        val state = BudgetState(
            remainingToday = (totalBudget - totalSpent).coerceAtLeast(BigDecimal.ZERO),
            totalSpentToday = BigDecimal.ZERO,
            dailyBudget = BigDecimal.ZERO,
            daysRemaining = daysLeft,
            progress = if (totalBudget > BigDecimal.ZERO) {
                totalSpent.divide(totalBudget, 2, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            },
            isOverBudget = totalSpent > totalBudget,
            totalBudget = totalBudget,
            totalSpentInPeriod = totalSpent,
        )
        return settings to state
    }
    private fun renderSheet(
        budgetSettings: BudgetSettings?,
        budgetState: BudgetState?,
        onFinishEarly: (() -> Unit)? = {},
    ) {
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeTestRule.setContent {
            MinusTheme {
                BudgetPeriodSheet(
                    budgetSettings = budgetSettings,
                    budgetState = budgetState,
                    selectedPeriod = BudgetPeriod.MONTHLY,
                    currencyCode = "USD",
                    onPeriodSelected = {},
                    onEditBudget = {},
                    onFinishEarly = onFinishEarly,
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun formattedUsd(amount: BigDecimal): String =
        symbolOnlyCurrencyFormat("USD").format(amount)

    @Test
    fun when_remaining_budget_and_period_ends_soon_with_split_equally_then_banner_shows_split_message() {
        val (settings, state) = scenario(
            totalBudget = BigDecimal("1000"),
            totalSpent = BigDecimal("700"),
            daysLeft = 2,
            strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
        )
        renderSheet(settings, state)

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_ROLLOVER_PREVIEW_BANNER_TAG)
            .assertIsDisplayed()
        val expected = composeTestRule.activity.getString(
            R.string.rollover_preview_split_equally,
            formattedUsd(BigDecimal("300")),
        )
        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun when_remaining_budget_and_period_ends_soon_with_add_to_first_day_then_banner_shows_carry_message() {
        val (settings, state) = scenario(
            totalBudget = BigDecimal("1000"),
            totalSpent = BigDecimal("700"),
            daysLeft = 2,
            strategy = RemainingBudgetStrategy.ADD_TO_FIRST_DAY,
        )
        renderSheet(settings, state)

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_ROLLOVER_PREVIEW_BANNER_TAG)
            .assertIsDisplayed()
        val expected = composeTestRule.activity.getString(
            R.string.rollover_preview_add_to_first_day,
            formattedUsd(BigDecimal("300")),
        )
        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun when_remaining_budget_and_period_ends_soon_with_ask_always_then_banner_shows_ask_message() {
        val (settings, state) = scenario(
            totalBudget = BigDecimal("1000"),
            totalSpent = BigDecimal("700"),
            daysLeft = 2,
            strategy = RemainingBudgetStrategy.ASK_ALWAYS,
        )
        renderSheet(settings, state)

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_ROLLOVER_PREVIEW_BANNER_TAG)
            .assertIsDisplayed()
        val expected = composeTestRule.activity.getString(
            R.string.rollover_preview_ask_always,
            formattedUsd(BigDecimal("300")),
        )
        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun when_remaining_budget_but_period_ends_in_ten_days_then_banner_is_hidden() {
        val (settings, state) = scenario(
            totalBudget = BigDecimal("1000"),
            totalSpent = BigDecimal("700"),
            daysLeft = 10,
        )
        renderSheet(settings, state)

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_ROLLOVER_PREVIEW_BANNER_TAG)
            .assertIsNotDisplayed()
    }

    @Test
    fun when_budget_exactly_spent_and_period_ends_soon_then_banner_is_hidden() {
        val (settings, state) = scenario(
            totalBudget = BigDecimal("1000"),
            totalSpent = BigDecimal("1000"),
            daysLeft = 2,
        )
        renderSheet(settings, state)

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_ROLLOVER_PREVIEW_BANNER_TAG)
            .assertIsNotDisplayed()
    }

    @Test
    fun when_overspent_and_period_ends_soon_then_banner_is_hidden() {
        val (settings, state) = scenario(
            totalBudget = BigDecimal("1000"),
            totalSpent = BigDecimal("1200"),
            daysLeft = 2,
        )
        renderSheet(settings, state)

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_ROLLOVER_PREVIEW_BANNER_TAG)
            .assertIsNotDisplayed()
    }

    @Test
    fun when_remaining_budget_then_finish_early_button_is_visible() {
        val (settings, state) = scenario(BigDecimal("1000"), BigDecimal("700"), daysLeft = 15)
        renderSheet(settings, state)

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_FINISH_EARLY_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun when_budget_exactly_spent_then_finish_early_button_is_visible() {
        val (settings, state) = scenario(BigDecimal("1000"), BigDecimal("1000"), daysLeft = 15)
        renderSheet(settings, state)

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_FINISH_EARLY_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun when_overspent_then_finish_early_button_is_visible() {
        val (settings, state) = scenario(BigDecimal("1000"), BigDecimal("1200"), daysLeft = 15)
        renderSheet(settings, state)

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_FINISH_EARLY_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun when_no_finish_early_callback_provided_then_button_is_hidden() {
        val (settings, state) = scenario(BigDecimal("1000"), BigDecimal("700"), daysLeft = 15)
        renderSheet(settings, state, onFinishEarly = null)

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_FINISH_EARLY_BUTTON_TAG).assertIsNotDisplayed()
    }

    @Test
    fun when_finish_early_tapped_then_confirmation_dialog_appears() {
        val (settings, state) = scenario(BigDecimal("1000"), BigDecimal("700"), daysLeft = 15)
        renderSheet(settings, state)

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_FINISH_EARLY_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        val question = composeTestRule.activity.getString(R.string.finalize_period_question)
        composeTestRule.onNodeWithText(question).assertIsDisplayed()
    }

    @Test
    fun when_finish_early_confirmed_then_callback_is_invoked() {
        var invoked = false
        val (settings, state) = scenario(BigDecimal("1000"), BigDecimal("700"), daysLeft = 15)
        renderSheet(settings, state, onFinishEarly = { invoked = true })

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_FINISH_EARLY_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        val confirmLabel = composeTestRule.activity.getString(R.string.finalize_action)
        composeTestRule.onNodeWithText(confirmLabel).performClick()
        composeTestRule.waitForIdle()

        assertThat(invoked).isTrue()
    }

    @Test
    fun when_finish_early_dialog_dismissed_then_callback_is_not_invoked() {
        var invoked = false
        val (settings, state) = scenario(BigDecimal("1000"), BigDecimal("700"), daysLeft = 15)
        renderSheet(settings, state, onFinishEarly = { invoked = true })

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_FINISH_EARLY_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        val cancelLabel = composeTestRule.activity.getString(R.string.cancel)
        composeTestRule.onNodeWithText(cancelLabel).performClick()
        composeTestRule.waitForIdle()

        assertThat(invoked).isFalse()
    }

    @Test
    fun when_finish_early_confirmed_on_overspent_period_then_callback_is_still_invoked() {
        var invoked = false
        val (settings, state) = scenario(BigDecimal("1000"), BigDecimal("1200"), daysLeft = 15)
        renderSheet(settings, state, onFinishEarly = { invoked = true })

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_FINISH_EARLY_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        val confirmLabel = composeTestRule.activity.getString(R.string.finalize_action)
        composeTestRule.onNodeWithText(confirmLabel).performClick()
        composeTestRule.waitForIdle()

        assertThat(invoked).isTrue()
    }
}
