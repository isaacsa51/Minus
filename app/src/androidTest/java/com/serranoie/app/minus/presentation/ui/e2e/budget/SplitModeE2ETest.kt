package com.serranoie.app.minus.presentation.ui.e2e.budget

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.presentation.ui.budget.BudgetUiState
import com.serranoie.app.minus.presentation.ui.editor.AnimState
import com.serranoie.app.minus.presentation.ui.editor.Editor
import com.serranoie.app.minus.presentation.ui.editor.sheets.BUDGET_PERIOD_APPLY_BUTTON_TAG
import com.serranoie.app.minus.presentation.ui.editor.sheets.BUDGET_PERIOD_CALCULATED_CARD_TAG
import com.serranoie.app.minus.presentation.ui.editor.sheets.BUDGET_PERIOD_EDIT_BUTTON_TAG
import com.serranoie.app.minus.presentation.ui.editor.sheets.BUDGET_PERIOD_SHEET_TAG
import com.serranoie.app.minus.presentation.ui.editor.sheets.BUDGET_PERIOD_SPLIT_MODE_ROW_TAG
import com.serranoie.app.minus.presentation.ui.editor.sheets.BudgetPeriodSheet
import com.serranoie.app.minus.presentation.ui.editor.sheets.budgetPeriodToggleTag
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate

/**
 * End-to-end coverage for the per-budget dynamic split mode feature.
 *
 * Anchored to the spec example: 16,644.45 budget, 200 spent, 30 days, with
 * 28 days remaining should yield a DAILY amount of 587.30 in DYNAMIC mode
 * and 554.82 in STATIC mode.
 *
 * The sheet is driven both standalone (via [renderSheet]) and through
 * [Editor] (via [renderEditorWithSheet]) which is the actual parent that
 * renders the [BudgetPeriodSheet] inside a `ModalBottomSheet`. We mount
 * [Editor] rather than [com.serranoie.app.minus.presentation.ui.home.MainScreenContent]
 * because the latter only emits the show-sheet intent — it never renders
 * the sheet itself.
 */
class SplitModeE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val totalBudget = BigDecimal("16644.45")
    private val totalSpent = BigDecimal("200.00")

    /**
     * 30-day window ending 27 days from "today", so `daysRemaining`
     * (computed in the sheet as `ChronoUnit.DAYS.between(today, endDate) + 1`)
     * resolves to 28 — matching the spec anchor case.
     */
    private fun dynamicWindow(): Pair<LocalDate, LocalDate> {
        val end = LocalDate.now().plusDays(27)
        val start = end.minusDays(29)
        return start to end
    }

    private fun dynamicSettings(splitMode: BudgetSplitMode = BudgetSplitMode.DYNAMIC) =
        dynamicWindow().let { (start, end) ->
            BudgetSettings(
                totalBudget = totalBudget,
                period = BudgetPeriod.DAILY,
                startDate = start,
                endDate = end,
                currencyCode = "USD",
                daysInPeriod = 30,
                splitMode = splitMode,
            )
        }

    private fun dynamicState() = BudgetState(
        remainingToday = BigDecimal("554.82"),
        totalSpentToday = BigDecimal("0.00"),
        dailyBudget = BigDecimal("554.82"),
        daysRemaining = 28,
        progress = (totalSpent.divide(totalBudget, 2, RoundingMode.HALF_UP).toFloat()).coerceIn(0f, 1f),
        isOverBudget = false,
        totalBudget = totalBudget,
        totalSpentInPeriod = totalSpent,
    )

    private fun formatExpected(
        value: BigDecimal,
        currencyCode: String = "USD",
        minimumFractionDigits: Int = 0,
        maximumFractionDigits: Int = 2,
    ): String {
        val deviceLocale = composeTestRule.activity.resources.configuration.locales[0]
        val symbol = when (currencyCode) {
            "USD" -> "$"
            else -> currencyCode
        }
        val numberFormatter = NumberFormat.getNumberInstance(deviceLocale).apply {
            this.maximumFractionDigits = maximumFractionDigits
            this.minimumFractionDigits = minimumFractionDigits
        }
        return "$symbol${numberFormatter.format(value)}"
    }

    private fun renderSheet(
        budgetSettings: BudgetSettings? = dynamicSettings(),
        budgetState: BudgetState? = dynamicState(),
        startInEditMode: Boolean = false,
        capturedIntents: MutableList<Any> = mutableListOf(),
    ) {
        composeTestRule.setContent {
            MinusTheme {
                BudgetPeriodSheet(
                    budgetSettings = budgetSettings,
                    budgetState = budgetState,
                    selectedPeriod = BudgetPeriod.DAILY,
                    currencyCode = "USD",
                    onPeriodSelected = { capturedIntents += "PeriodSelected:$it" },
                    onSaveBudget = { capturedIntents += it },
                    onEditBudget = { capturedIntents += "EditBudget" },
                    onFinishEarly = { capturedIntents += "FinishEarly" },
                    startInEditMode = startInEditMode,
                    pendingExpensesCount = 0,
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(400)
    }

    private fun renderEditorWithSheet(
        budgetSettings: BudgetSettings = dynamicSettings(),
        budgetState: BudgetState = dynamicState(),
        capturedIntents: MutableList<Any> = mutableListOf(),
    ) {
        val budgetUiState = BudgetUiState(
            budgetSettings = budgetSettings,
            budgetState = budgetState,
        )
        composeTestRule.setContent {
            CompositionLocalProvider {
                MinusTheme {
                    Editor(
                        uiState = budgetUiState,
                        animState = AnimState.IDLE,
                        modifier = Modifier.fillMaxSize(),
                        onFocus = {},
                        onOpenHistory = {},
                        onOpenSettings = { capturedIntents += "OpenSettings" },
                        onOpenAnalytics = { capturedIntents += "OpenAnalytics" },
                        onOpenWallet = { capturedIntents += "OpenWallet" },
                        openWalletOnStart = false,
                        showBudgetPeriodSheet = true,
                        forceBudgetPeriodSheetSetup = false,
                        selectedViewPeriod = BudgetPeriod.DAILY,
                        onShowBudgetPeriodSheet = { capturedIntents += "ShowBudgetPeriodSheet" },
                        onHideBudgetPeriodSheet = { capturedIntents += "HideBudgetPeriodSheet" },
                        onPeriodSelected = { capturedIntents += "PeriodSelected:$it" },
                        onCommentClick = {},
                        onCommentUpdate = { capturedIntents += "CommentUpdate:$it" },
                        onDeleteTag = { capturedIntents += "DeleteTag:$it" },
                        onRecurrentToggle = {},
                        onCreditToggle = {},
                        onDismissRecurrentDialog = {},
                        onDismissCreditCutoffDialog = {},
                        onRecurrentExpenseConfirm = { _, _, _, _ -> },
                        onCreditCutoffConfirm = {},
                        onSaveBudget = { capturedIntents += it },
                        directCategoryPopupEnabled = false,
                        categoryGridModeEnabled = false,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(800)
    }

    @Test
    fun when_dynamic_mode_with_spend_then_calculated_card_shows_587_30() {
        renderSheet(
            budgetSettings = dynamicSettings(BudgetSplitMode.DYNAMIC),
            budgetState = dynamicState(),
        )

        // 587.30 = (16644.45 - 200) / 28
        composeTestRule.onNodeWithTag(BUDGET_PERIOD_CALCULATED_CARD_TAG)
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithText(formatExpected(BigDecimal("587.30"))).onLast()
            .assertIsDisplayed()
    }

    @Test
    fun when_static_mode_then_calculated_card_shows_554_82() {
        renderSheet(
            budgetSettings = dynamicSettings(BudgetSplitMode.STATIC),
            budgetState = dynamicState(),
        )

        // 554.82 = 16644.45 / 30
        composeTestRule.onNodeWithTag(BUDGET_PERIOD_CALCULATED_CARD_TAG)
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithText(formatExpected(BigDecimal("554.82"))).onLast()
            .assertIsDisplayed()
    }

    @Test
    fun when_no_budget_state_then_calculated_card_still_renders_gracefully() {
        // budgetState=null -> totalSpent defaults to 0, so DYNAMIC degenerates
        // to (16644.45 / 28) = 594.44 for daily. The point of this test is to
        // verify the sheet doesn't crash and the card is still on screen.
        renderSheet(
            budgetSettings = dynamicSettings(BudgetSplitMode.DYNAMIC),
            budgetState = null,
        )

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_CALCULATED_CARD_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun when_edit_mode_then_split_mode_row_is_visible() {
        renderSheet(startInEditMode = true)

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_SPLIT_MODE_ROW_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun when_edit_mode_with_static_settings_then_split_mode_row_shows_static_label() {
        renderSheet(
            budgetSettings = dynamicSettings(BudgetSplitMode.STATIC),
            startInEditMode = true,
        )

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_SPLIT_MODE_ROW_TAG)
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Static").onLast().assertIsDisplayed()
    }

    @Test
    fun when_edit_mode_with_dynamic_settings_then_split_mode_row_shows_dynamic_label() {
        renderSheet(
            budgetSettings = dynamicSettings(BudgetSplitMode.DYNAMIC),
            startInEditMode = true,
        )

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_SPLIT_MODE_ROW_TAG)
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Dynamic").onLast().assertIsDisplayed()
    }

    @Test
    fun when_tap_split_mode_row_then_picker_dialog_opens() {
        renderSheet(startInEditMode = true)

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_SPLIT_MODE_ROW_TAG).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("How should we split your budget?")
            .onLast()
            .assertIsDisplayed()
    }

    @Test
    fun when_pick_dynamic_in_dialog_then_row_updates_to_dynamic_label() {
        renderSheet(
            budgetSettings = dynamicSettings(BudgetSplitMode.STATIC),
            startInEditMode = true,
        )

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_SPLIT_MODE_ROW_TAG).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("Dynamic").onLast().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("How should we split your budget?")
            .onLast()
            .assertIsNotDisplayed()
        composeTestRule.onAllNodesWithText("Dynamic").onLast().assertIsDisplayed()
    }

    @Test
    fun when_switch_to_dynamic_and_apply_then_save_intent_carries_dynamic_mode() {
        val captured = mutableListOf<Any>()
        renderSheet(
            budgetSettings = dynamicSettings(BudgetSplitMode.STATIC),
            startInEditMode = true,
            capturedIntents = captured,
        )

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_SPLIT_MODE_ROW_TAG).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Dynamic").onLast().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_APPLY_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        val saved = captured.filterIsInstance<BudgetSettings>().lastOrNull()
        Truth.assertThat(saved).isNotNull()
        Truth.assertThat(saved!!.splitMode).isEqualTo(BudgetSplitMode.DYNAMIC)
    }

    @Test
    fun when_apply_with_static_default_then_save_intent_carries_static_mode() {
        val captured = mutableListOf<Any>()
        renderSheet(
            budgetSettings = dynamicSettings(BudgetSplitMode.STATIC),
            startInEditMode = true,
            capturedIntents = captured,
        )

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_APPLY_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        val saved = captured.filterIsInstance<BudgetSettings>().lastOrNull()
        Truth.assertThat(saved).isNotNull()
        Truth.assertThat(saved!!.splitMode).isEqualTo(BudgetSplitMode.STATIC)
    }

    @Test
    fun when_editor_renders_with_dynamic_sheet_open_then_calculated_card_shows_dynamic_number() {
        renderEditorWithSheet(
            budgetSettings = dynamicSettings(BudgetSplitMode.DYNAMIC),
            budgetState = dynamicState(),
        )

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_SHEET_TAG).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(formatExpected(BigDecimal("587.30"))).onLast()
            .assertIsDisplayed()
    }

    @Test
    fun when_editor_renders_with_static_sheet_open_then_calculated_card_shows_static_number() {
        renderEditorWithSheet(
            budgetSettings = dynamicSettings(BudgetSplitMode.STATIC),
            budgetState = dynamicState(),
        )

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_SHEET_TAG).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(formatExpected(BigDecimal("554.82"))).onLast()
            .assertIsDisplayed()
    }

    @Test
    fun when_open_sheet_from_editor_then_edit_button_is_visible() {
        renderEditorWithSheet()

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_EDIT_BUTTON_TAG).assertIsDisplayed()
    }

    /**
     * Builds a (settings, state) pair with custom values, anchored to a
     * 30-day window ending in `daysLeft` days. Used by the additional
     * scenario tests below.
     */
    private fun customDynamicScenario(
        budget: BigDecimal,
        spent: BigDecimal,
        daysLeft: Int,
        splitMode: BudgetSplitMode = BudgetSplitMode.DYNAMIC,
    ): Pair<BudgetSettings, BudgetState> {
        val end = LocalDate.now().plusDays((daysLeft - 1).toLong())
        val start = end.minusDays(29)
        val settings = BudgetSettings(
            totalBudget = budget,
            period = BudgetPeriod.DAILY,
            startDate = start,
            endDate = end,
            currencyCode = "USD",
            daysInPeriod = 30,
            splitMode = splitMode,
        )
        val state = BudgetState(
            remainingToday = budget.subtract(spent).coerceAtLeast(BigDecimal.ZERO),
            totalSpentToday = BigDecimal.ZERO,
            dailyBudget = BigDecimal.ZERO, // not used by the calculated card
            daysRemaining = daysLeft,
            progress = if (budget > BigDecimal.ZERO) {
                spent.divide(budget, 2, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
            } else 0f,
            isOverBudget = spent > budget,
            totalBudget = budget,
            totalSpentInPeriod = spent,
        )
        return settings to state
    }

    @Test
    fun when_dynamic_mid_period_half_spent_then_card_shows_166_67() {
        // 5000 budget, 2500 spent, 15 days left -> 2500/15 = 166.67/day
        val (settings, state) = customDynamicScenario(
            budget = BigDecimal("5000"),
            spent = BigDecimal("2500"),
            daysLeft = 15,
        )

        renderSheet(budgetSettings = settings, budgetState = state)

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_CALCULATED_CARD_TAG)
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithText(formatExpected(BigDecimal("166.67"))).onLast()
            .assertIsDisplayed()
    }

    @Test
    fun when_dynamic_overspent_then_card_still_renders_without_crashing() {
        // 1000 budget, 1500 spent -> remaining is negative -> 0 (no negatives on screen)
        // The exact zero is pinned by the unit test. Here we just guard
        // against a render crash and confirm the card stays on screen.
        val (settings, state) = customDynamicScenario(
            budget = BigDecimal("1000"),
            spent = BigDecimal("1500"),
            daysLeft = 10,
        )

        renderSheet(budgetSettings = settings, budgetState = state)

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_CALCULATED_CARD_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun when_dynamic_last_day_full_budget_then_card_shows_full_amount() {
        // 1-day period, 0 spent, 1 day left -> 1000.00/day (edge: daysRemaining = 1)
        val (settings, state) = customDynamicScenario(
            budget = BigDecimal("1000"),
            spent = BigDecimal.ZERO,
            daysLeft = 1,
        )

        renderSheet(budgetSettings = settings, budgetState = state)

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_CALCULATED_CARD_TAG)
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithText(formatExpected(BigDecimal("1000.00"))).onLast()
            .assertIsDisplayed()
    }

    @Test
    fun when_dynamic_weekly_period_then_card_scales_daily_by_7() {
        // 16644.45 - 200 = 16444.45 / 28 = 587.30/day -> 587.30 * 7 = 4111.10/week
        val (settings, state) = customDynamicScenario(
            budget = totalBudget,
            spent = totalSpent,
            daysLeft = 28,
        )

        renderSheet(budgetSettings = settings, budgetState = state)

        // Switch the period chip from DAILY (the default) to WEEKLY
        composeTestRule.onNodeWithTag(budgetPeriodToggleTag(BudgetPeriod.WEEKLY))
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText(formatExpected(BigDecimal("4111.10"))).onLast()
            .assertIsDisplayed()
    }
}
