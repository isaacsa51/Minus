package com.serranoie.app.minus.presentation.ui.budget

import android.content.Context
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.usecase.ClearEarlyFinishStateUseCase
import com.serranoie.app.minus.domain.usecase.FinishBudgetEarlyUseCase
import com.serranoie.app.minus.domain.usecase.GetCurrentPeriodIdUseCase
import com.serranoie.app.minus.domain.usecase.MarkOnboardingCompletedUseCase
import com.serranoie.app.minus.domain.usecase.ObserveCurrentPeriodBoundaryUseCase
import com.serranoie.app.minus.domain.usecase.ObserveCurrentPeriodRolloverUseCase
import com.serranoie.app.minus.domain.usecase.PersistBudgetSettingsUseCase
import com.serranoie.app.minus.domain.usecase.UpdatePeriodEndNotificationTimeUseCase
import com.serranoie.app.minus.presentation.notification.NotificationHelper
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import com.serranoie.app.minus.presentation.ui.budget.mvi.BudgetUiEffect
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetEditorIntent
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetNumpadIntent
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetSystemIntent
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetTransactionIntent
import com.serranoie.app.minus.presentation.ui.editor.AnimState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModelTest {

    private val context: Context = mockk(relaxed = true)
    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val notificationHelper: NotificationHelper = mockk(relaxed = true)
    private val notificationScheduler: NotificationScheduler = mockk(relaxed = true)
    private val transactionHandler: BudgetTransactionHandler = mockk(relaxed = true)
    private val budgetStateCalculator: BudgetStateCalculator = mockk(relaxed = true)
    private val budgetWidgetUpdater: BudgetWidgetUpdater = mockk(relaxed = true)
    private val budgetExpressionEvaluator: BudgetExpressionEvaluator = BudgetExpressionEvaluator()
    private val observeCurrentPeriodBoundaryUseCase: ObserveCurrentPeriodBoundaryUseCase =
        mockk(relaxed = true)
    private val observeCurrentPeriodRolloverUseCase: ObserveCurrentPeriodRolloverUseCase =
        mockk(relaxed = true)
    private val getCurrentPeriodIdUseCase: GetCurrentPeriodIdUseCase = mockk(relaxed = true)
    private val persistBudgetSettingsUseCase: PersistBudgetSettingsUseCase = mockk(relaxed = true)
    private val updatePeriodEndNotificationTimeUseCase: UpdatePeriodEndNotificationTimeUseCase =
        mockk(relaxed = true)
    private val finishBudgetEarlyUseCase: FinishBudgetEarlyUseCase = mockk(relaxed = true)
    private val clearEarlyFinishStateUseCase: ClearEarlyFinishStateUseCase = mockk(relaxed = true)
    private val markOnboardingCompletedUseCase: MarkOnboardingCompletedUseCase =
        mockk(relaxed = true)

    private val settingsFlow = MutableStateFlow<BudgetSettings?>(null)
    private val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())
    private val boundaryFlow = MutableStateFlow(0L to 0L)
    private val rolloverFlow = MutableStateFlow(BigDecimal.ZERO to false)
    private val categoriesFlow = MutableStateFlow<List<com.serranoie.app.minus.domain.model.Category>>(emptyList())
    private val queuedTransactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { budgetRepository.getBudgetSettings() } returns settingsFlow
        every { budgetRepository.getTransactions() } returns transactionsFlow
        every { budgetRepository.getQueuedTransactions() } returns queuedTransactionsFlow
        every { budgetRepository.getActiveCategories() } returns categoriesFlow
        every { observeCurrentPeriodBoundaryUseCase() } returns boundaryFlow
        every { observeCurrentPeriodRolloverUseCase() } returns rolloverFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = BudgetViewModel(
        context = context,
        budgetRepository = budgetRepository,
        notificationHelper = notificationHelper,
        notificationScheduler = notificationScheduler,
        transactionHandler = transactionHandler,
        budgetStateCalculator = budgetStateCalculator,
        budgetWidgetUpdater = budgetWidgetUpdater,
        budgetExpressionEvaluator = budgetExpressionEvaluator,
        observeCurrentPeriodBoundaryUseCase = observeCurrentPeriodBoundaryUseCase,
        observeCurrentPeriodRolloverUseCase = observeCurrentPeriodRolloverUseCase,
        getCurrentPeriodIdUseCase = getCurrentPeriodIdUseCase,
        persistBudgetSettingsUseCase = persistBudgetSettingsUseCase,
        updatePeriodEndNotificationTimeUseCase = updatePeriodEndNotificationTimeUseCase,
        finishBudgetEarlyUseCase = finishBudgetEarlyUseCase,
        clearEarlyFinishStateUseCase = clearEarlyFinishStateUseCase,
        markOnboardingCompletedUseCase = markOnboardingCompletedUseCase,
    )

    private fun sampleSettings() = BudgetSettings(
        totalBudget = BigDecimal("1000.00"),
        period = BudgetPeriod.MONTHLY,
        startDate = LocalDate.of(2026, 1, 1),
        endDate = LocalDate.of(2026, 1, 30),
        currencyCode = "USD",
        daysInPeriod = 30,
    )

    private fun sampleTransaction() = Transaction(
        id = 1L,
        amount = BigDecimal("12.34"),
        comment = "Coffee",
        date = LocalDateTime.of(2026, 1, 1, 9, 0),
    )

    private suspend fun <T> ReceiveTurbine<T>.awaitCondition(predicate: (T) -> Boolean): T {
        while (true) {
            val item = awaitItem()
            if (predicate(item)) return item
        }
    }

    @Test
    fun `when_viewmodel_is_created_then_ui_state_has_default_values`() = runTest {
        val viewModel = newViewModel()

        viewModel.uiState.test {
            val state = awaitCondition { it.numpadInput == "" }
            assertThat(state.isCalculation).isFalse()
            assertThat(state.isFirstLaunch).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun when_mark_first_launch_complete_is_called_then_is_first_launch_becomes_false_and_use_case_is_invoked() =
        runTest {
            coEvery { markOnboardingCompletedUseCase() } returns Unit
            val viewModel = newViewModel()
            
            viewModel.uiState.test {
                awaitCondition { it.isFirstLaunch }
                viewModel.markFirstLaunchComplete()
                coVerify { markOnboardingCompletedUseCase() }
                
                settingsFlow.value = sampleSettings()
                awaitCondition { !it.isFirstLaunch }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun when_number_tapped_intent_is_processed_then_numpad_input_is_updated() = runTest {
        val viewModel = newViewModel()

        viewModel.uiState.test {
            awaitCondition { it.numpadInput == "" }
            viewModel.processIntent(BudgetNumpadIntent.NumberTapped("5"))
            val state = awaitCondition { it.numpadInput == "5" }
            assertThat(state.animState).isEqualTo(AnimState.EDITING)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun when_operator_is_tapped_then_input_gains_operator_and_calculation_mode_is_set() = runTest {
        val viewModel = newViewModel()
        viewModel.uiState.test {
            awaitCondition { it.numpadInput == "" }
            viewModel.processIntent(BudgetNumpadIntent.NumberTapped("5"))
            awaitCondition { it.numpadInput == "5" }

            viewModel.processIntent(BudgetNumpadIntent.OperatorTapped('+'))
            val state = awaitCondition { it.isCalculation && it.numpadInput == "5+" }
            assertThat(state.isCalculation).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun when_equals_is_tapped_on_valid_expression_then_input_becomes_evaluation_result() = runTest {
        val viewModel = newViewModel()
        viewModel.uiState.test {
            awaitCondition { it.numpadInput == "" }
            viewModel.processIntent(BudgetNumpadIntent.NumberTapped("2"))
            viewModel.processIntent(BudgetNumpadIntent.OperatorTapped('+'))
            viewModel.processIntent(BudgetNumpadIntent.NumberTapped("3"))
            awaitCondition { it.numpadInput == "2+3" }

            viewModel.processIntent(BudgetNumpadIntent.EqualsTapped)
            awaitCondition { it.numpadInput == "5" }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun when_reset_input_is_tapped_then_input_is_cleared_and_calculation_is_reset() = runTest {
        val viewModel = newViewModel()
        viewModel.uiState.test {
            awaitCondition { it.numpadInput == "" }
            viewModel.processIntent(BudgetNumpadIntent.NumberTapped("1"))
            viewModel.processIntent(BudgetNumpadIntent.OperatorTapped('+'))
            awaitCondition { it.isCalculation }

            viewModel.processIntent(BudgetNumpadIntent.ResetInputTapped)
            awaitCondition { it.numpadInput == "" && !it.isCalculation }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun when_dismiss_recurrent_dialog_intent_is_processed_then_dialog_is_hidden() = runTest {
        coEvery {
            transactionHandler.applyTransaction(any(), any(), any(), any(), any(), any(), any())
        } returns ApplyTransactionResult.ShowRecurrentDialog(
            normalizedInput = "12.34",
            amount = BigDecimal("12.34"),
        )
        val viewModel = newViewModel()
        viewModel.uiState.test {
            awaitCondition { !it.showRecurrentDialog }
            
            viewModel.processIntent(BudgetNumpadIntent.NumberTapped("1"))
            viewModel.processIntent(BudgetEditorIntent.SetRecurrentEnabled(true))
            viewModel.processIntent(BudgetNumpadIntent.ApplyTapped)
            awaitCondition { it.showRecurrentDialog }

            viewModel.processIntent(BudgetEditorIntent.DismissRecurrentDialog)
            awaitCondition { !it.showRecurrentDialog }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun when_dismiss_credit_cutoff_dialog_intent_is_processed_then_dialog_and_credit_are_cleared() =
        runTest {
            val viewModel = newViewModel()
            viewModel.uiState.test {
                awaitCondition { !it.showCreditCutoffDialog }
                viewModel.processIntent(BudgetEditorIntent.SetCreditEnabled(true))
                awaitCondition { it.showCreditCutoffDialog }

                viewModel.processIntent(BudgetEditorIntent.DismissCreditCutoffDialog)
                awaitCondition { !it.showCreditCutoffDialog && !it.isCreditEnabled }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun when_set_lock_swipeable_system_intent_is_processed_then_lock_swipeable_is_set() = runTest {
        val viewModel = newViewModel()
        viewModel.uiState.test {
            awaitCondition { !it.lockSwipeable }
            viewModel.processIntent(BudgetSystemIntent.SetLockSwipeable(true))
            awaitCondition { it.lockSwipeable }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun when_set_lock_draggable_system_intent_is_processed_then_lock_draggable_is_set() = runTest {
        val viewModel = newViewModel()
        viewModel.uiState.test {
            awaitCondition { !it.lockDraggable }
            viewModel.processIntent(BudgetSystemIntent.SetLockDraggable(true))
            awaitCondition { it.lockDraggable }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun when_apply_intent_results_in_added_then_numpad_input_and_comment_are_cleared() = runTest {
        coEvery {
            transactionHandler.applyTransaction(any(), any(), any(), any(), any(), any(), any())
        } returns ApplyTransactionResult.Added(normalizedInput = "12.34")
        
        val viewModel = newViewModel()
        viewModel.uiState.test {
            awaitCondition { it.numpadInput == "" }
            viewModel.processIntent(BudgetNumpadIntent.NumberTapped("1"))
            viewModel.processIntent(BudgetEditorIntent.CommentUpdated("Lunch"))
            awaitCondition { it.numpadInput == "1" && it.currentComment == "Lunch" }

            viewModel.processIntent(BudgetNumpadIntent.ApplyTapped)
            awaitCondition { it.numpadInput == "" && it.currentComment == "" }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun when_apply_intent_results_in_show_recurrent_dialog_then_dialog_and_pending_values_are_set() =
        runTest {
            coEvery {
                transactionHandler.applyTransaction(any(), any(), any(), any(), any(), any(), any())
            } returns ApplyTransactionResult.ShowRecurrentDialog(
                normalizedInput = "12.34",
                amount = BigDecimal("12.34"),
            )
            val viewModel = newViewModel()
            viewModel.uiState.test {
                awaitCondition { !it.showRecurrentDialog }
                viewModel.processIntent(BudgetNumpadIntent.NumberTapped("1"))
                viewModel.processIntent(BudgetEditorIntent.SetRecurrentEnabled(true))
                viewModel.processIntent(BudgetNumpadIntent.ApplyTapped)
                awaitCondition { it.showRecurrentDialog && it.pendingRecurrentAmount == BigDecimal("12.34") }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun when_apply_intent_results_in_queued_for_next_period_then_show_message_effect_is_emitted() =
        runTest {
            coEvery {
                transactionHandler.applyTransaction(any(), any(), any(), any(), any(), any(), any())
            } returns ApplyTransactionResult.QueuedForNextPeriod(normalizedInput = "12.34")
            val viewModel = newViewModel()

            viewModel.effects.test {
                viewModel.processIntent(BudgetNumpadIntent.NumberTapped("1"))
                viewModel.processIntent(BudgetNumpadIntent.ApplyTapped)
                val effect = awaitItem()
                assertThat(effect).isInstanceOf(BudgetUiEffect.ShowMessage::class.java)
                effect as BudgetUiEffect.ShowMessage
                assertThat(effect.message).isEqualTo("Gasto en cola para el proximo periodo")
            }
        }

    @Test
    fun when_delete_transaction_handler_returns_failure_then_show_message_effect_is_emitted() =
        runTest {
            coEvery { transactionHandler.deleteTransaction(any()) } returns Result.failure(
                RuntimeException("boom")
            )
            val viewModel = newViewModel()
            val transaction = sampleTransaction()

            viewModel.effects.test {
                viewModel.processIntent(BudgetTransactionIntent.DeleteTransactionTapped(transaction))
                val effect = awaitItem()
                assertThat(effect).isInstanceOf(BudgetUiEffect.ShowMessage::class.java)
                effect as BudgetUiEffect.ShowMessage
                assertThat(effect.message).isEqualTo("Could not delete transaction")
            }
        }

    @Test
    fun when_restore_transaction_handler_returns_failure_then_show_message_effect_is_emitted() =
        runTest {
            coEvery { transactionHandler.restoreTransaction(any()) } returns Result.failure(
                RuntimeException("boom")
            )
            val viewModel = newViewModel()
            val transaction = sampleTransaction()

            viewModel.effects.test {
                viewModel.processIntent(BudgetTransactionIntent.RestoreTransactionTapped(transaction))
                val effect = awaitItem()
                assertThat(effect).isInstanceOf(BudgetUiEffect.ShowMessage::class.java)
                effect as BudgetUiEffect.ShowMessage
                assertThat(effect.message).isEqualTo("Could not restore transaction")
            }
        }

    @Test
    fun when_delete_transaction_tapped_intent_is_processed_then_handler_is_called_with_transaction() =
        runTest {
            coEvery { transactionHandler.deleteTransaction(any()) } returns Result.success(Unit)
            val viewModel = newViewModel()
            val transaction = sampleTransaction()
            viewModel.processIntent(BudgetTransactionIntent.DeleteTransactionTapped(transaction))
            advanceUntilIdle()
            coVerify { transactionHandler.deleteTransaction(transaction) }
        }

    @Test
    fun when_restore_transaction_tapped_intent_is_processed_then_handler_is_called_with_transaction() =
        runTest {
            coEvery { transactionHandler.restoreTransaction(any()) } returns Result.success(Unit)
            val viewModel = newViewModel()
            val transaction = sampleTransaction()
            viewModel.processIntent(BudgetTransactionIntent.RestoreTransactionTapped(transaction))
            advanceUntilIdle()
            coVerify { transactionHandler.restoreTransaction(transaction) }
        }

    @Test
    fun when_edit_transaction_tapped_intent_is_processed_then_handler_is_called_with_updated_transaction() =
        runTest {
            coEvery { transactionHandler.editTransaction(any()) } returns true
            val viewModel = newViewModel()
            val transaction = sampleTransaction()
            viewModel.processIntent(BudgetTransactionIntent.EditTransactionTapped(transaction))
            advanceUntilIdle()
            coVerify { transactionHandler.editTransaction(transaction) }
        }
}
