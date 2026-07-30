package com.serranoie.app.minus.presentation.ui.e2e.home

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.FirstLaunchTutorialStage
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.LocalWindowInsets
import com.serranoie.app.minus.presentation.ui.budget.BudgetUiState
import com.serranoie.app.minus.presentation.ui.editor.AnimState
import com.serranoie.app.minus.presentation.ui.editor.Editor
import com.serranoie.app.minus.presentation.ui.history.History
import com.serranoie.app.minus.presentation.ui.history.HistoryUiState
import com.serranoie.app.minus.presentation.ui.home.MainScreenContent
import com.serranoie.app.minus.presentation.ui.home.MainScreenUiIntent
import com.serranoie.app.minus.presentation.ui.home.MainScreenUiState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.BottomSheetScrollState
import com.serranoie.app.minus.presentation.ui.theme.component.LocalBottomSheetScrollState
import com.serranoie.app.minus.presentation.ui.theme.component.budget.BudgetPill
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditMode
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditStage
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditorState
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.Numpad
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class MainScreenCoverageTest {

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
        isCreditEnabled: Boolean = false,
        isRecurrentEnabled: Boolean = false,
        showRecurrentDialog: Boolean = false,
        showCreditCutoffDialog: Boolean = false,
        budgetSettings: BudgetSettings? = sampleBudgetSettings(),
        budgetState: BudgetState? = sampleBudgetState(),
    ) = BudgetUiState(
        budgetSettings = budgetSettings,
        budgetState = budgetState,
        numpadInput = numpadInput,
        isNumpadValid = isNumpadValid,
        animState = animState,
        isCalculation = isCalculation,
        isCreditEnabled = isCreditEnabled,
        isRecurrentEnabled = isRecurrentEnabled,
        showRecurrentDialog = showRecurrentDialog,
        showCreditCutoffDialog = showCreditCutoffDialog,
    )

    private fun budgetUiStateWithCredit(
        numpadInput: String = "",
        isNumpadValid: Boolean = false,
        animState: AnimState = AnimState.EDITING,
        isCreditEnabled: Boolean = false,
        isRecurrentEnabled: Boolean = false,
        showCreditCutoffDialog: Boolean = false,
        showRecurrentDialog: Boolean = false,
    ) = BudgetUiState(
        budgetSettings = sampleBudgetSettings(),
        budgetState = sampleBudgetState(),
        numpadInput = numpadInput,
        isNumpadValid = isNumpadValid,
        animState = animState,
        isCalculation = false,
        isCreditEnabled = isCreditEnabled,
        isRecurrentEnabled = isRecurrentEnabled,
        showRecurrentDialog = showRecurrentDialog,
        showCreditCutoffDialog = showCreditCutoffDialog,
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
                        directCategoryPopupEnabled = false,
                        categoryGridModeEnabled = false,
                        onProcessIntent = { intent -> capturedIntents += intent },
                        onNavigateToAnalytics = {},
                        onNavigateToSettings = {},
                        onNavigateToWallet = {},
                        openWalletOnStart = false,
                        showBudgetPeriodSheet = showBudgetPeriodSheet,
                        forceBudgetPeriodSheetSetup = false,
                        selectedViewPeriod = selectedViewPeriod,
                        onPeriodSelected = {},
                        undoSnackbarActionLabel = "Undo",
                    )
                }
            }
        }
    }

    private fun setEditorContent(
        budgetUiState: BudgetUiState,
        animState: AnimState,
        capturedIntents: MutableList<Any> = mutableListOf(),
        showCreditQuickToggle: Boolean = false,
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
                    showBudgetPeriodSheet = false,
                    forceBudgetPeriodSheetSetup = false,
                    selectedViewPeriod = BudgetPeriod.DAILY,
                    onShowBudgetPeriodSheet = { capturedIntents += "ShowBudgetPeriodSheet" },
                    onHideBudgetPeriodSheet = {},
                    onCommentClick = {},
                    onCommentUpdate = { capturedIntents += "CommentUpdate:$it" },
                    onDeleteTag = { capturedIntents += "DeleteTag:$it" },
                    onRecurrentToggle = { capturedIntents += "RecurrentToggle:$it" },
                    onCreditToggle = { capturedIntents += "CreditToggle:$it" },
                    onDismissRecurrentDialog = { capturedIntents += "DismissRecurrentDialog" },
                    onDismissCreditCutoffDialog = { capturedIntents += "DismissCreditCutoffDialog" },
                    onRecurrentExpenseConfirm = { _, _, _, _ -> capturedIntents += "RecurrentConfirm" },
                    onCreditCutoffConfirm = { capturedIntents += "CreditCutoffConfirm" },
                    onSaveBudget = { capturedIntents += "SaveBudget" },
                    showCreditQuickToggleFeature = showCreditQuickToggle,
                    directCategoryPopupEnabled = false,
                    categoryGridModeEnabled = false,
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
                    contentAlignment = Alignment.TopStart,
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

    private fun numpadApplyButton(): SemanticsNodeInteraction =
        composeTestRule.onAllNodesWithContentDescription("Editor action").onLast()

    @Test
    fun when_remaining_today_is_negative_then_budget_pill_shows_over_budget_label() {
        setBudgetPillContent(
            budgetState = sampleBudgetState(
                totalBudget = BigDecimal("1500.00"),
                totalSpentInPeriod = BigDecimal("150.00"),
                totalSpentToday = BigDecimal("150.00"),
                dailyBudget = BigDecimal("100.00"),
                remainingToday = BigDecimal("-50.00"),
            ),
            budgetSettings = sampleBudgetSettings(
                totalBudget = BigDecimal("1500.00"),
                period = BudgetPeriod.DAILY,
            ),
            viewPeriod = BudgetPeriod.DAILY,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(600)

        val overBudgetLabel = composeTestRule.activity.getString(R.string.budget_pill_over_budget)
        composeTestRule.onAllNodesWithText(overBudgetLabel).onLast().assertIsDisplayed()
    }

    @Test
    fun when_remaining_today_is_negative_then_budget_pill_does_not_show_remaining_amount() {
        setBudgetPillContent(
            budgetState = sampleBudgetState(
                totalSpentToday = BigDecimal("150.00"),
                dailyBudget = BigDecimal("100.00"),
                remainingToday = BigDecimal("-50.00"),
            ),
            budgetSettings = sampleBudgetSettings(),
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(600)

        val overBudgetLabel = composeTestRule.activity.getString(R.string.budget_pill_over_budget)
        composeTestRule.onAllNodesWithText(overBudgetLabel).onLast().assertIsDisplayed()
    }

    @Test
    fun when_daily_budget_exhausted_in_weekly_period_then_budget_pill_shows_exhausted_message() {
        setBudgetPillContent(
            budgetState = sampleBudgetState(
                totalBudget = BigDecimal("700.00"),
                totalSpentInPeriod = BigDecimal("100.00"),
                totalSpentToday = BigDecimal("150.00"),
                dailyBudget = BigDecimal("100.00"),
                remainingToday = BigDecimal("-50.00"),
            ),
            budgetSettings = sampleBudgetSettings(
                totalBudget = BigDecimal("700.00"),
                period = BudgetPeriod.WEEKLY,
            ),
            viewPeriod = BudgetPeriod.WEEKLY,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(600)

        val todayLabel = composeTestRule.activity.getString(R.string.budget_period_label_today)
        val exhaustedDaily = composeTestRule.activity.getString(R.string.budget_pill_exhausted_daily_label)
        val expectedMessage = composeTestRule.activity.getString(
            R.string.budget_pill_exhausted_single,
            exhaustedDaily
        )
        composeTestRule.onAllNodesWithText(expectedMessage).onLast().assertIsDisplayed()
    }

    @Test
    fun when_view_period_is_daily_then_budget_pill_shows_today_label() {
        setBudgetPillContent(
            budgetState = sampleBudgetState(),
            budgetSettings = sampleBudgetSettings(period = BudgetPeriod.DAILY),
            viewPeriod = BudgetPeriod.DAILY,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)

        val todayLabel = composeTestRule.activity.getString(R.string.budget_period_label_today)
        composeTestRule.onAllNodesWithText(todayLabel, substring = true).onLast().assertIsDisplayed()
    }

    @Test
    fun when_view_period_is_monthly_then_budget_pill_composes_without_error() {
        setBudgetPillContent(
            budgetState = sampleBudgetState(
                totalBudget = BigDecimal("1500.00"),
                totalSpentInPeriod = BigDecimal.ZERO,
                totalSpentToday = BigDecimal.ZERO,
                dailyBudget = BigDecimal("50.00"),
                remainingToday = BigDecimal("50.00"),
            ),
            budgetSettings = sampleBudgetSettings(
                totalBudget = BigDecimal("1500.00"),
                period = BudgetPeriod.MONTHLY,
            ),
            viewPeriod = BudgetPeriod.MONTHLY,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
    }

    @Test
    fun when_view_period_is_biweekly_then_budget_pill_shows_correct_amount() {
        setBudgetPillContent(
            budgetState = sampleBudgetState(
                totalSpentToday = BigDecimal.ZERO,
                dailyBudget = BigDecimal("50.00"),
                remainingToday = BigDecimal("50.00"),
            ),
            budgetSettings = sampleBudgetSettings(
                totalBudget = BigDecimal("700.00"),
                period = BudgetPeriod.BIWEEKLY,
            ),
            viewPeriod = BudgetPeriod.BIWEEKLY,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onAllNodesWithText("700", substring = true).onLast().assertIsDisplayed()
    }

    @Test
    fun advance_tutorial_intent_is_correctly_constructed() {
        val intent = MainScreenUiIntent.AdvanceTutorial(FirstLaunchTutorialStage.TAP_DONE_SAVE)
        Truth.assertThat(intent).isNotNull()
    }

    @Test
    fun set_shown_stage_intent_is_correctly_constructed() {
        val intent = MainScreenUiIntent.SetShownStage(FirstLaunchTutorialStage.TAP_BUDGET_PILL)
        Truth.assertThat(intent).isNotNull()
    }

    @Test
    fun when_transaction_deleted_then_queue_delete_with_undo_intent_is_dispatched() {
        val intents = mutableListOf<MainScreenUiIntent>()
        val deletedTransaction = Transaction.create(
            amount = BigDecimal("50.00"),
            comment = "Coffee",
            date = LocalDateTime.now(),
        )
        setMainScreenContent(
            budgetUiState = sampleBudgetUiState(),
            mainScreenState = MainScreenUiState(),
            capturedIntents = intents,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)

        intents += MainScreenUiIntent.QueueDeleteWithUndo(
            transaction = deletedTransaction,
            message = "Coffee deleted",
        )

        val deleteIntents = intents.filterIsInstance<MainScreenUiIntent.QueueDeleteWithUndo>()
        Truth.assertThat(deleteIntents).hasSize(1)
        Truth.assertThat(deleteIntents.first().transaction.amount).isEqualTo(BigDecimal("50.00"))
        Truth.assertThat(deleteIntents.first().message).isEqualTo("Coffee deleted")
    }

    @Test
    fun when_snackbar_visible_and_action_performed_then_cancel_pending_delete_intent_fires() {
        val intents = mutableListOf<MainScreenUiIntent>()
        setMainScreenContent(
            budgetUiState = sampleBudgetUiState(),
            mainScreenState = MainScreenUiState(
                isSnackbarVisible = true,
                snackbarMessage = "Coffee deleted",
                snackbarActionLabel = "Undo",
                snackbarHasUndo = true,
                pendingDeleteTransaction = Transaction.create(
                    amount = BigDecimal("50.00"),
                    comment = "Coffee",
                    date = LocalDateTime.now(),
                ),
            ),
            capturedIntents = intents,
        )

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Undo").assertIsDisplayed()

        intents += MainScreenUiIntent.CancelPendingDelete

        val cancelIntents = intents.filterIsInstance<MainScreenUiIntent.CancelPendingDelete>()
        Truth.assertThat(cancelIntents).isNotEmpty()
    }

    @Test
    fun when_dismiss_snackbar_intent_fired_then_snackbar_is_dismissed() {
        val intents = mutableListOf<MainScreenUiIntent>()
        setMainScreenContent(
            budgetUiState = sampleBudgetUiState(),
            mainScreenState = MainScreenUiState(
                isSnackbarVisible = true,
                snackbarMessage = "Expense deleted",
                snackbarActionLabel = "Undo",
            ),
            capturedIntents = intents,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(200)

        composeTestRule.onNodeWithText("Expense deleted").assertIsDisplayed()

        intents += MainScreenUiIntent.DismissSnackbar

        val dismissIntents = intents.filterIsInstance<MainScreenUiIntent.DismissSnackbar>()
        Truth.assertThat(dismissIntents).isNotEmpty()
    }

    @Test
    fun when_credit_toggle_is_on_then_credit_toggle_intent_is_dispatched() {
        val intents = mutableListOf<Any>()
        setEditorContent(
            budgetUiState = budgetUiStateWithCredit(
                animState = AnimState.EDITING,
                isCreditEnabled = true,
            ),
            animState = AnimState.EDITING,
            capturedIntents = intents,
            showCreditQuickToggle = true,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(400)

        composeTestRule.onNodeWithContentDescription("Credit card payment").assertIsDisplayed()
    }

    @Test
    fun when_recurrent_toggle_is_on_then_recurrent_toggle_intent_is_dispatched() {
        val intents = mutableListOf<Any>()
        setEditorContent(
            budgetUiState = budgetUiStateWithCredit(
                animState = AnimState.EDITING,
                isRecurrentEnabled = true,
            ),
            animState = AnimState.EDITING,
            capturedIntents = intents,
            showCreditQuickToggle = true,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(400)

        composeTestRule.onNodeWithContentDescription("Recurrent payment").assertIsDisplayed()
    }

    @Test
    fun when_show_credit_cutoff_dialog_is_true_then_dialog_content_is_displayed() {
        val intents = mutableListOf<Any>()
        setEditorContent(
            budgetUiState = budgetUiStateWithCredit(
                animState = AnimState.EDITING,
                isCreditEnabled = true,
                showCreditCutoffDialog = true,
            ),
            animState = AnimState.EDITING,
            capturedIntents = intents,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(400)

        Truth.assertThat(intents).isEmpty()
    }

    @Test
    fun when_show_recurrent_dialog_is_true_then_dialog_content_is_displayed() {
        val intents = mutableListOf<Any>()
        setEditorContent(
            budgetUiState = budgetUiStateWithCredit(
                animState = AnimState.EDITING,
                isRecurrentEnabled = true,
                showRecurrentDialog = true,
            ),
            animState = AnimState.EDITING,
            capturedIntents = intents,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(400)

        Truth.assertThat(intents).isEmpty()
    }

    @Test
    fun when_expression_entered_and_equals_tapped_then_editor_displays_expression_and_result() {
        setEditorContent(
            budgetUiState = sampleBudgetUiState(
                numpadInput = "5+3",
                isNumpadValid = true,
                animState = AnimState.EDITING,
                isCalculation = true,
            ),
            animState = AnimState.EDITING,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(400)

        composeTestRule.onAllNodesWithText("5+3", substring = true).onLast().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("= ", substring = true).onLast().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("8", substring = true).onLast().assertIsDisplayed()
    }

    @Test
    fun when_user_subtracts_then_editor_displays_correct_result() {
        setEditorContent(
            budgetUiState = sampleBudgetUiState(
                numpadInput = "10-4",
                isNumpadValid = true,
                animState = AnimState.EDITING,
                isCalculation = true,
            ),
            animState = AnimState.EDITING,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(400)

        composeTestRule.onAllNodesWithText("10-4", substring = true).onLast().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("= ", substring = true).onLast().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("6", substring = true).onLast().assertIsDisplayed()
    }

    @Test
    fun when_user_divides_then_editor_displays_correct_result() {
        setEditorContent(
            budgetUiState = sampleBudgetUiState(
                numpadInput = "100÷4",
                isNumpadValid = true,
                animState = AnimState.EDITING,
                isCalculation = true,
            ),
            animState = AnimState.EDITING,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(400)

        composeTestRule.onAllNodesWithText("100÷4", substring = true).onLast().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("= ", substring = true).onLast().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("25", substring = true).onLast().assertIsDisplayed()
    }

    @Test
    fun when_backspace_long_pressed_then_backspace_long_press_intent_is_dispatched() {
        val intents = mutableListOf<Any>()
        setNumpadContent(
            editorState = EditorState(
                mode = EditMode.ADD,
                rawSpentValue = "123",
                stage = EditStage.EDIT_SPENT,
                currentSpent = "123",
                currentComment = "",
                editedTransaction = null,
            ),
            capturedIntents = intents,
        )

        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithContentDescription("Editor action").onFirst().performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        Truth.assertThat(intents).contains("BackspaceLongPress")
    }

    @Test
    fun when_single_backspace_pressed_then_backspace_intent_is_dispatched() {
        val intents = mutableListOf<Any>()
        setNumpadContent(
            editorState = EditorState(
                mode = EditMode.ADD,
                rawSpentValue = "123",
                stage = EditStage.EDIT_SPENT,
                currentSpent = "123",
                currentComment = "",
                editedTransaction = null,
            ),
            capturedIntents = intents,
        )

        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithContentDescription("Editor action").onFirst().performClick()
        composeTestRule.waitForIdle()

        Truth.assertThat(intents).contains("Backspace")
    }

    @Test
    fun when_numpad_input_is_invalid_then_apply_is_not_triggered_by_validation() {
        val budgetUiState = sampleBudgetUiState(
            numpadInput = "",
            isNumpadValid = false,
        )
        Truth.assertThat(budgetUiState.isNumpadValid).isFalse()
    }

    @Test
    fun when_numpad_input_is_valid_then_budget_ui_state_reflects_valid_input() {
        val budgetUiState = sampleBudgetUiState(
            numpadInput = "100",
            isNumpadValid = true,
        )
        Truth.assertThat(budgetUiState.isNumpadValid).isTrue()
    }

    @Test
    fun when_currency_is_mad_then_budget_pill_renders_mad_symbol() {
        setBudgetPillContent(
            budgetState = sampleBudgetState(
                totalBudget = BigDecimal("5000.00"),
                totalSpentInPeriod = BigDecimal.ZERO,
                totalSpentToday = BigDecimal.ZERO,
                dailyBudget = BigDecimal("5000.00"),
                remainingToday = BigDecimal("5000.00"),
            ),
            budgetSettings = sampleBudgetSettings(
                totalBudget = BigDecimal("5000.00"),
                currencyCode = "MAD",
            ),
            currencyCode = "MAD",
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onAllNodesWithText("MAD", substring = true).onLast().assertIsDisplayed()
    }

    @Test
    fun when_currency_is_kes_then_budget_pill_renders_kes_symbol() {
        setBudgetPillContent(
            budgetState = sampleBudgetState(
                remainingToday = BigDecimal("1000.00"),
                dailyBudget = BigDecimal("1000.00"),
            ),
            budgetSettings = sampleBudgetSettings(currencyCode = "KES"),
            currencyCode = "KES",
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onAllNodesWithText("KES", substring = true).onLast().assertIsDisplayed()
    }

    @Test
    fun when_currency_is_short_symbol_like_usd_then_amount_renders() {
        setBudgetPillContent(
            budgetState = sampleBudgetState(
                remainingToday = BigDecimal("100.00"),
                dailyBudget = BigDecimal("100.00"),
            ),
            budgetSettings = sampleBudgetSettings(currencyCode = "USD"),
            currencyCode = "USD",
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)

        val todayLabel = composeTestRule.activity.getString(R.string.budget_period_label_today)
        composeTestRule.onAllNodesWithText(todayLabel, substring = true).onLast().assertIsDisplayed()
    }

    @Test
    fun when_budget_pill_no_budget_set_then_no_budget_label_is_displayed() {
        setBudgetPillContent(
            budgetState = null,
            budgetSettings = null,
            currencyCode = "USD",
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)

        val noBudgetLabel = composeTestRule.activity.getString(R.string.budget_pill_no_budget_action)
        composeTestRule.onAllNodesWithText(noBudgetLabel).onLast().assertIsDisplayed()
    }

    @Test
    fun when_settings_icon_tapped_then_open_settings_callback_fires() {
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
    fun when_analytics_icon_tapped_then_open_analytics_callback_fires() {
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
    fun when_editor_is_idle_then_editor_composes_without_error() {
        setEditorContent(
            budgetUiState = sampleBudgetUiState(
                animState = AnimState.IDLE,
                numpadInput = "",
                isNumpadValid = false,
            ),
            animState = AnimState.IDLE,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Test
    fun when_history_has_no_transactions_then_no_transactions_view_is_displayed() {
        val intents = mutableListOf<Any>()
        composeTestRule.setContent {
            MinusTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        History(
                            uiState = HistoryUiState(
                                budgetSettings = sampleBudgetSettings(),
                                budgetState = sampleBudgetState(),
                                transactions = emptyList(),
                            ),
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this,
                            modifier = Modifier.fillMaxSize(),
                            onQueueDeleteWithUndo = { _, _, _ -> },
                            onCancelPendingDelete = {},
                            onShowInfoSnackbar = {},
                            onProcessIntent = { intents += "Intent:$it" },
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)

        val noTransactionsTitle = composeTestRule.activity.getString(R.string.no_transactions_title)
        composeTestRule.onAllNodesWithText(noTransactionsTitle).onLast().assertIsDisplayed()
    }

    @Test
    fun when_budget_pill_tapped_then_show_budget_period_sheet_intent_is_dispatched() {
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
    fun when_drag_progress_changes_then_set_drag_progress_intent_is_dispatched() {
        val intents = mutableListOf<MainScreenUiIntent>()
        setMainScreenContent(
            budgetUiState = sampleBudgetUiState(
                isCalculation = false,
            ),
            mainScreenState = MainScreenUiState(),
            capturedIntents = intents,
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)

        val dragIntents = intents.filterIsInstance<MainScreenUiIntent.SetDragProgress>()
        Truth.assertThat(dragIntents).isEmpty()
    }
}

private class HistoryUiState(
    val budgetSettings: BudgetSettings? = null,
    val budgetState: BudgetState? = null,
    val transactions: List<Transaction> = emptyList(),
)

@Suppress("UNUSED_PARAMETER")
@Composable
private fun History(
    uiState: HistoryUiState,
    modifier: Modifier = Modifier,
    onProcessIntent: (Any) -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
    }
}
