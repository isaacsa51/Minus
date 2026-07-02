package com.serranoie.app.minus.presentation.ui.e2e.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.presentation.LocalWindowInsets
import com.serranoie.app.minus.presentation.ui.budget.BudgetUiState
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetNumpadIntent
import com.serranoie.app.minus.presentation.ui.editor.AnimState
import com.serranoie.app.minus.presentation.ui.editor.Editor
import com.serranoie.app.minus.presentation.ui.editor.category.CategoryToolbar
import com.serranoie.app.minus.presentation.ui.editor.category.FocusController
import com.serranoie.app.minus.presentation.ui.editor.sheets.BUDGET_PERIOD_APPLY_BUTTON_TAG
import com.serranoie.app.minus.presentation.ui.editor.sheets.BUDGET_PERIOD_BUDGET_INPUT_TAG
import com.serranoie.app.minus.presentation.ui.editor.sheets.BUDGET_PERIOD_SHEET_TAG
import com.serranoie.app.minus.presentation.ui.editor.sheets.BudgetPeriodSheet
import com.serranoie.app.minus.presentation.ui.history.History
import com.serranoie.app.minus.presentation.ui.history.HistoryUiState
import com.serranoie.app.minus.presentation.ui.home.MainScreenContent
import com.serranoie.app.minus.presentation.ui.home.MainScreenUiIntent
import com.serranoie.app.minus.presentation.ui.home.MainScreenUiState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.BottomSheetScrollState
import com.serranoie.app.minus.presentation.ui.theme.component.LocalBottomSheetScrollState
import com.serranoie.app.minus.presentation.ui.theme.component.budget.BudgetPill
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditStage
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditorState
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.Numpad
import com.serranoie.app.minus.presentation.ui.tutorial.FirstLaunchTutorialStage
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditMode as NumpadEditMode

class MainScreenE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun sampleBudgetSettings(
        totalBudget: BigDecimal = BigDecimal("500.00"),
        period: BudgetPeriod = BudgetPeriod.DAILY,
        currencyCode: String = "USD",
        startDate: LocalDate = LocalDate.now(),
    ) = BudgetSettings(
        totalBudget = totalBudget,
        period = period,
        startDate = startDate,
        currencyCode = currencyCode,
    )

    private fun sampleBudgetState(
        totalBudget: BigDecimal = BigDecimal("500.00"),
        totalSpentInPeriod: BigDecimal = BigDecimal.ZERO,
        totalSpentToday: BigDecimal = BigDecimal.ZERO,
        dailyBudget: BigDecimal = BigDecimal("500.00"),
        remainingToday: BigDecimal = BigDecimal("500.00"),
        daysRemaining: Int = 1,
    ) = BudgetState(
        remainingToday = remainingToday,
        totalSpentToday = totalSpentToday,
        dailyBudget = dailyBudget,
        daysRemaining = daysRemaining,
        progress = if (totalBudget > BigDecimal.ZERO) {
            totalSpentInPeriod
                .divide(totalBudget, 2, java.math.RoundingMode.HALF_UP)
                .toFloat()
                .coerceIn(0f, 1f)
        } else {
            0f
        },
        isOverBudget = remainingToday < BigDecimal.ZERO,
        totalBudget = totalBudget,
        totalSpentInPeriod = totalSpentInPeriod,
    )

    private fun sampleBudgetUiState(
        numpadInput: String = "",
        isNumpadValid: Boolean = false,
        animState: AnimState = AnimState.IDLE,
        isCalculation: Boolean = false,
        budgetSettings: BudgetSettings? = sampleBudgetSettings(),
        budgetState: BudgetState? = sampleBudgetState(),
    ) = BudgetUiState(
        budgetSettings = budgetSettings,
        budgetState = budgetState,
        numpadInput = numpadInput,
        isNumpadValid = isNumpadValid,
        animState = animState,
        isCalculation = isCalculation,
    )

    private fun setMainScreenContent(
        budgetUiState: BudgetUiState,
        mainScreenState: MainScreenUiState = MainScreenUiState(),
        capturedIntents: MutableList<MainScreenUiIntent>,
        selectedViewPeriod: BudgetPeriod = BudgetPeriod.DAILY,
        showBudgetPeriodSheet: Boolean = false,
        onboardingCompleted: Boolean = true,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalBottomSheetScrollState provides BottomSheetScrollState(0.dp),
                LocalWindowInsets provides PaddingValues(0.dp),
            ) {
                MinusTheme {
                    MainScreenContent(
                        mainScreenState = mainScreenState,
                        budgetUiState = budgetUiState,
                        onboardingCompleted = onboardingCompleted,
                        tutorialStage = FirstLaunchTutorialStage.COMPLETED,
                        showCreditQuickToggleFeature = true,
                        onProcessIntent = { intent -> capturedIntents += intent },
                        onNavigateToAnalytics = {},
                        onNavigateToSettings = {},
                        onNavigateToWallet = {},
                        openWalletOnStart = false,
                        showBudgetPeriodSheet = showBudgetPeriodSheet,
                        forceBudgetPeriodSheetSetup = false,
                        selectedViewPeriod = selectedViewPeriod,
                        onPeriodSelected = {},
                        settingsDataStore = null,
                        undoSnackbarActionLabel = "Undo",
                    )
                }
            }
        }
    }

    private fun setEditorContent(
        budgetUiState: BudgetUiState,
        animState: AnimState,
        selectedViewPeriod: BudgetPeriod = BudgetPeriod.DAILY,
        showBudgetPeriodSheet: Boolean = false,
        capturedIntents: MutableList<Any> = mutableListOf(),
    ) {
        composeTestRule.setContent {
            MinusTheme {
                Editor(
                    uiState = budgetUiState,
                    animState = animState,
                    modifier = Modifier.fillMaxSize(),
                    onFocus = {},
                    onOpenHistory = {},
                    onOpenSettings = { capturedIntents += "OpenSettings" },
                    onOpenAnalytics = { capturedIntents += "OpenAnalytics" },
                    onOpenWallet = {},
                    openWalletOnStart = false,
                    showBudgetPeriodSheet = showBudgetPeriodSheet,
                    forceBudgetPeriodSheetSetup = false,
                    selectedViewPeriod = selectedViewPeriod,
                    onShowBudgetPeriodSheet = { capturedIntents += "ShowBudgetPeriodSheet" },
                    onHideBudgetPeriodSheet = {},
                    onCommentClick = {},
                    onCommentUpdate = { capturedIntents += "CommentUpdate:$it" },
                    onDeleteTag = { capturedIntents += "DeleteTag:$it" },
                    onRecurrentToggle = {},
                    onCreditToggle = {},
                    onDismissRecurrentDialog = {},
                    onDismissCreditCutoffDialog = {},
                    onRecurrentExpenseConfirm = { _, _, _ -> },
                    onCreditCutoffConfirm = {},
                    onSaveBudget = { capturedIntents += "SaveBudget" },
                )
            }
        }
    }

    private fun setBudgetPillContent(
        budgetState: BudgetState?,
        budgetSettings: BudgetSettings?,
        viewPeriod: BudgetPeriod = BudgetPeriod.DAILY,
        currencyCode: String = "USD",
        capturedIntents: MutableList<Any> = mutableListOf(),
    ) {
        composeTestRule.setContent {
            MinusTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.TopStart,
                ) {
                    BudgetPill(
                        budgetState = budgetState,
                        budgetSettings = budgetSettings,
                        viewPeriod = viewPeriod,
                        currencyCode = currencyCode,
                        onOpenBudgetSheet = { capturedIntents += "ShowBudgetPeriodSheet" },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    private fun setNumpadContent(
        editorState: EditorState,
        isCalculation: Boolean = false,
        capturedIntents: MutableList<Any> = mutableListOf(),
    ) {
        composeTestRule.setContent {
            MinusTheme {
                Numpad(
                    editorState = editorState,
                    isCalculation = isCalculation,
                    onNumberInput = { capturedIntents += "Number:$it" },
                    onDotInput = { capturedIntents += "Dot" },
                    onEqualsInput = { capturedIntents += "Equals" },
                    onBackspace = { capturedIntents += "Backspace" },
                    onBackspaceLongPress = { capturedIntents += "BackspaceLongPress" },
                    onDelete = { capturedIntents += "Delete" },
                    onApply = { capturedIntents += "Apply" },
                    onOperatorInput = { capturedIntents += "Operator:$it" },
                    modifier = Modifier
                        .fillMaxSize()
                        .height(400.dp),
                )
            }
        }
    }

    private fun setBudgetPeriodSheetContent(
        budgetSettings: BudgetSettings?,
        startInEditMode: Boolean = false,
        capturedIntents: MutableList<Any> = mutableListOf(),
    ) {
        composeTestRule.setContent {
            MinusTheme {
                BudgetPeriodSheet(
                    budgetSettings = budgetSettings,
                    budgetState = null,
                    selectedPeriod = BudgetPeriod.DAILY,
                    currencyCode = "USD",
                    onPeriodSelected = {},
                    onSaveBudget = { capturedIntents += "SaveBudget" },
                    onEditBudget = { capturedIntents += "EditBudget" },
                    onFinishEarly = { capturedIntents += "FinishEarly" },
                    startInEditMode = startInEditMode,
                    pendingExpensesCount = 0,
                )
            }
        }
    }

    private fun setCategoryToolbarContent(
        tags: List<String>,
        currentComment: String = "",
        capturedIntents: MutableList<Any> = mutableListOf(),
    ) {
        composeTestRule.setContent {
            MinusTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    CategoryToolbar(
                        tags = tags,
                        currentComment = currentComment,
                        stage = EditStage.EDIT_SPENT,
                        onCommentUpdate = { capturedIntents += "CommentUpdate:$it" },
                        editorFocusController = FocusController(),
                        onDeleteTag = { capturedIntents += "DeleteTag:$it" },
                        onSaveExpense = { capturedIntents += "SaveExpense" },
                        modifier = Modifier
                            .fillMaxSize()
                            .height(200.dp),
                    )
                }
            }
        }
    }

    private fun setHistoryContent(
        uiState: HistoryUiState = HistoryUiState(),
        capturedIntents: MutableList<Any> = mutableListOf(),
    ) {
        composeTestRule.setContent {
            MinusTheme {
                History(
                    uiState = uiState,
                    modifier = Modifier.fillMaxSize(),
                    onProcessIntent = { capturedIntents += "Intent:$it" },
                )
            }
        }
    }

    private fun numpadApplyButton(): SemanticsNodeInteraction =
        composeTestRule.onAllNodesWithContentDescription("Editor action").onLast()

    private fun formatExpected(
        value: BigDecimal,
        currencyCode: String = "USD",
        minimumFractionDigits: Int = 0,
        maximumFractionDigits: Int = 2,
    ): String {
        val deviceLocale = composeTestRule.activity.resources.configuration.locales[0]
        val symbol = SupportedCurrency.findByCode(currencyCode)?.symbol ?: "$"
        val numberFormatter = NumberFormat.getNumberInstance(deviceLocale).apply {
            this.maximumFractionDigits = maximumFractionDigits
            this.minimumFractionDigits = minimumFractionDigits
        }
        return "$symbol${numberFormatter.format(value)}"
    }

    @Test
    fun when_user_types_amount_on_numpad_then_editor_renders_typed_value() {
        setEditorContent(
            budgetUiState = sampleBudgetUiState(
                numpadInput = "123",
                isNumpadValid = true,
                animState = AnimState.EDITING,
            ),
            animState = AnimState.EDITING,
        )

        composeTestRule.onAllNodesWithText(formatExpected(BigDecimal("123"))).onLast()
            .assertIsDisplayed()
    }

    @Test
    fun when_user_types_decimal_amount_then_editor_renders_currency_formatted_value() {
        setEditorContent(
            budgetUiState = sampleBudgetUiState(
                numpadInput = "12.50",
                isNumpadValid = true,
                animState = AnimState.EDITING,
            ),
            animState = AnimState.EDITING,
        )

        composeTestRule
            .onAllNodesWithText(formatExpected(BigDecimal("12.50"), minimumFractionDigits = 0))
            .onLast()
            .assertIsDisplayed()
    }

    @Test
    fun when_user_taps_apply_button_then_apply_tapped_intent_is_dispatched() {
        val intents = mutableListOf<MainScreenUiIntent>()
        setMainScreenContent(
            budgetUiState = sampleBudgetUiState(
                numpadInput = "50",
                isNumpadValid = true,
                animState = AnimState.EDITING,
            ),
            capturedIntents = intents,
        )

        numpadApplyButton().performClick()
        composeTestRule.waitForIdle()

        Truth.assertThat(intents).contains(
            MainScreenUiIntent.ProcessBudgetNumpadIntent(BudgetNumpadIntent.ApplyTapped)
        )
    }

    @Test
    fun when_numpad_is_empty_then_both_backspace_and_apply_actions_are_present() {
        val intents = mutableListOf<MainScreenUiIntent>()
        setMainScreenContent(
            budgetUiState = sampleBudgetUiState(
                numpadInput = "",
                isNumpadValid = false,
                animState = AnimState.IDLE,
            ),
            capturedIntents = intents,
        )

        composeTestRule.onAllNodesWithContentDescription("Editor action").assertCountEquals(2)
    }

    @Test
    fun when_no_expenses_recorded_then_budget_pill_shows_total_budget_remaining() {
        setBudgetPillContent(
            budgetState = sampleBudgetState(
                totalBudget = BigDecimal("500.00"),
                totalSpentInPeriod = BigDecimal.ZERO,
                totalSpentToday = BigDecimal.ZERO,
                dailyBudget = BigDecimal("500.00"),
                remainingToday = BigDecimal("500.00"),
            ),
            budgetSettings = sampleBudgetSettings(totalBudget = BigDecimal("500.00")),
        )

        composeTestRule.onAllNodesWithText(formatExpected(BigDecimal("500.00"))).onLast()
            .assertIsDisplayed()
    }

    @Test
    fun when_expense_logged_then_budget_pill_shows_remaining_after_spend() {
        setBudgetPillContent(
            budgetState = sampleBudgetState(
                totalBudget = BigDecimal("500.00"),
                totalSpentInPeriod = BigDecimal("50.00"),
                totalSpentToday = BigDecimal("50.00"),
                dailyBudget = BigDecimal("500.00"),
                remainingToday = BigDecimal("450.00"),
            ),
            budgetSettings = sampleBudgetSettings(totalBudget = BigDecimal("500.00")),
        )

        composeTestRule.onAllNodesWithText(formatExpected(BigDecimal("450.00"))).onLast()
            .assertIsDisplayed()
    }

    @Test
    fun when_split_mode_is_weekly_then_budget_pill_shows_weekly_remaining() {
        setBudgetPillContent(
            budgetState = sampleBudgetState(
                totalBudget = BigDecimal("700.00"),
                totalSpentInPeriod = BigDecimal.ZERO,
                totalSpentToday = BigDecimal.ZERO,
                dailyBudget = BigDecimal("100.00"),
                remainingToday = BigDecimal("100.00"),
            ),
            budgetSettings = sampleBudgetSettings(
                totalBudget = BigDecimal("700.00"),
                period = BudgetPeriod.WEEKLY,
            ),
            viewPeriod = BudgetPeriod.WEEKLY,
        )

        composeTestRule.onAllNodesWithText(formatExpected(BigDecimal("700.00"))).onLast()
            .assertIsDisplayed()
    }

    @Test
    fun when_calculation_mode_is_disabled_then_equals_and_operator_buttons_are_hidden() {
        setNumpadContent(
            editorState = EditorState(
                mode = NumpadEditMode.ADD,
                rawSpentValue = "5",
                stage = EditStage.EDIT_SPENT,
                currentSpent = "5",
                currentComment = "",
                editedTransaction = null,
            ),
            isCalculation = false,
        )

        composeTestRule.onAllNodesWithText("=").fetchSemanticsNodes().forEach { /* no-op */ }
        composeTestRule.onNodeWithText("=").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("÷").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("×").assertIsNotDisplayed()
    }

    @Test
    fun when_calculation_mode_is_enabled_then_equals_and_operator_buttons_are_visible() {
        setNumpadContent(
            editorState = EditorState(
                mode = NumpadEditMode.ADD,
                rawSpentValue = "5+3",
                stage = EditStage.EDIT_SPENT,
                currentSpent = "5+3",
                currentComment = "",
                editedTransaction = null,
            ),
            isCalculation = true,
        )

        composeTestRule.onNodeWithText("=").assertIsDisplayed()
        composeTestRule.onNodeWithText("÷").assertIsDisplayed()
        composeTestRule.onNodeWithText("×").assertIsDisplayed()
        composeTestRule.onNodeWithText("+").assertIsDisplayed()
        composeTestRule.onNodeWithText("-").assertIsDisplayed()
    }

    @Test
    fun when_user_calculates_then_editor_displays_correct_expression_and_result() {
        setEditorContent(
            budgetUiState = sampleBudgetUiState(
                numpadInput = "5+3",
                isNumpadValid = true,
                animState = AnimState.EDITING,
                isCalculation = true,
            ),
            animState = AnimState.EDITING,
        )

        composeTestRule.onAllNodesWithText("5+3", substring = true).onLast().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("= ", substring = true).onLast().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("8", substring = true).onLast().assertIsDisplayed()
    }

    @Test
    fun when_user_multiplies_then_editor_displays_correct_result() {
        setEditorContent(
            budgetUiState = sampleBudgetUiState(
                numpadInput = "10×2",
                isNumpadValid = true,
                animState = AnimState.EDITING,
                isCalculation = true,
            ),
            animState = AnimState.EDITING,
        )

        composeTestRule.onAllNodesWithText("10×2", substring = true).onLast().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("= ", substring = true).onLast().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("20", substring = true).onLast().assertIsDisplayed()
    }

    @Test
    fun when_user_taps_budget_pill_then_show_budget_period_sheet_intent_is_dispatched() {
        val intents = mutableListOf<Any>()
        setBudgetPillContent(
            budgetState = sampleBudgetState(),
            budgetSettings = sampleBudgetSettings(),
            capturedIntents = intents,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)

        val todayLabel = composeTestRule.activity.getString(R.string.budget_period_label_today)
        composeTestRule.onAllNodesWithText(todayLabel, substring = true).onLast().performClick()
        composeTestRule.waitForIdle()

        Truth.assertThat(intents).contains("ShowBudgetPeriodSheet")
    }

    @Test
    fun when_show_budget_period_sheet_state_is_true_then_sheet_is_displayed() {
        setBudgetPeriodSheetContent(
            budgetSettings = sampleBudgetSettings(),
        )

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_SHEET_TAG).assertIsDisplayed()
    }

    @Test
    fun when_user_sequences_digits_then_editor_renders_full_input() {
        setEditorContent(
            budgetUiState = sampleBudgetUiState(
                numpadInput = "123",
                isNumpadValid = true,
                animState = AnimState.EDITING,
            ),
            animState = AnimState.EDITING,
        )

        composeTestRule.onAllNodesWithText(formatExpected(BigDecimal("123"))).onLast()
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithText("1", substring = true).onLast().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("2", substring = true).onLast().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("3", substring = true).onLast().assertIsDisplayed()
    }

    @Test
    fun when_user_edits_budget_period_values_then_apply_dispatches_save_budget_intent() {
        val intents = mutableListOf<Any>()
        setBudgetPeriodSheetContent(
            budgetSettings = BudgetSettings(
                totalBudget = BigDecimal("500.00"),
                period = BudgetPeriod.DAILY,
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusDays(7),
                currencyCode = "USD",
                daysInPeriod = 8,
            ),
            startInEditMode = true,
            capturedIntents = intents,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(400)

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_BUDGET_INPUT_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BUDGET_PERIOD_APPLY_BUTTON_TAG).assertIsDisplayed()

        composeTestRule.onNodeWithTag(BUDGET_PERIOD_APPLY_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        Truth.assertThat(intents).contains("SaveBudget")
    }

    @Test
    fun when_user_views_category_toolbar_with_no_tags_then_add_new_category_label_is_displayed() {
        setCategoryToolbarContent(
            tags = emptyList(),
            currentComment = "",
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)

        val addNewCategoryLabel = composeTestRule.activity.getString(R.string.add_new_category)
        composeTestRule.onAllNodesWithText(addNewCategoryLabel).onLast().assertIsDisplayed()
    }

    @Test
    fun when_user_types_new_category_in_toolbar_then_comment_update_intent_is_dispatched() {
        val intents = mutableListOf<Any>()
        setCategoryToolbarContent(
            tags = listOf("Food"),
            currentComment = "",
            capturedIntents = intents,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)

        val foodNode = composeTestRule.onAllNodesWithText("Food")
        Truth.assertThat(foodNode.fetchSemanticsNodes()).isNotEmpty()
        Truth.assertThat(intents).isEmpty()
    }

    @Test
    fun when_user_selects_existing_category_from_toolbar_then_comment_update_intent_is_dispatched() {
        val intents = mutableListOf<Any>()
        setCategoryToolbarContent(
            tags = listOf("Food", "Transport", "Shopping"),
            currentComment = "",
            capturedIntents = intents,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)

        composeTestRule.onAllNodesWithText("Food").onLast().performClick()
        composeTestRule.waitForIdle()

        Truth.assertThat(intents).contains("CommentUpdate:Food")
    }

    @Test
    fun when_user_taps_analytics_icon_then_open_analytics_intent_is_dispatched() {
        val intents = mutableListOf<Any>()
        setEditorContent(
            budgetUiState = sampleBudgetUiState(animState = AnimState.IDLE),
            animState = AnimState.IDLE,
            capturedIntents = intents,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)

        composeTestRule.onNodeWithContentDescription("Analytics").performClick()
        composeTestRule.waitForIdle()

        Truth.assertThat(intents).contains("OpenAnalytics")
    }

    @Test
    fun when_user_taps_settings_icon_then_open_settings_intent_is_dispatched() {
        val intents = mutableListOf<Any>()
        setEditorContent(
            budgetUiState = sampleBudgetUiState(animState = AnimState.IDLE),
            animState = AnimState.IDLE,
            capturedIntents = intents,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)

        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        Truth.assertThat(intents).contains("OpenSettings")
    }

    @Test
    fun when_history_is_empty_then_no_transactions_view_is_displayed() {
        setHistoryContent(uiState = HistoryUiState())

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)

        val noTransactionsTitle = composeTestRule.activity.getString(R.string.no_transactions_title)
        composeTestRule.onAllNodesWithText(noTransactionsTitle).onLast().assertIsDisplayed()
    }

    @Test
    fun when_history_has_budget_settings_then_budget_display_section_is_rendered() {
        setHistoryContent(
            uiState = HistoryUiState(
                budgetSettings = sampleBudgetSettings(),
                budgetState = sampleBudgetState(),
                transactions = emptyList(),
            ),
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)

        val totalBudgetLabel = composeTestRule.activity.getString(R.string.total_budget)
        composeTestRule.onAllNodesWithText(totalBudgetLabel, substring = true).onLast()
            .assertIsDisplayed()
    }
}
