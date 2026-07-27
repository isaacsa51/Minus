package com.serranoie.app.minus.presentation.ui.budget

import android.content.Context
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
import com.serranoie.app.minus.presentation.ui.editor.EditMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        every { observeCurrentPeriodBoundaryUseCase() } returns kotlinx.coroutines.flow.flowOf(0L to 0L)
        every { observeCurrentPeriodRolloverUseCase() } returns kotlinx.coroutines.flow.flowOf(
            BigDecimal.ZERO to false
        )
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

    @Test
    fun `when_viewmodel_is_created_then_ui_state_has_default_values`() = runTest {
        val viewModel = newViewModel()

        val state = viewModel.uiState.value
        assertThat(state.numpadInput).isEqualTo("")
        assertThat(state.isCalculation).isFalse()
        assertThat(state.dragProgress).isEqualTo(0f)
        assertThat(state.isFirstLaunch).isTrue()
        assertThat(state.transactions).isEmpty()
    }

    @Test
    fun when_save_budget_settings_is_called_then_persist_use_case_is_invoked_with_same_settings() =
        runTest {
            coEvery {
                persistBudgetSettingsUseCase(any(), any())
            } returns PeriodBoundaryResult(
                periodStartMillis = 0L,
                periodId = 1L,
            )
            val viewModel = newViewModel()
            val settings = sampleSettings()

            viewModel.saveBudgetSettings(settings, forceNewPeriodBoundary = true)

            advanceUntilIdle()
            coVerify { persistBudgetSettingsUseCase(settings, forceNewPeriodBoundary = true) }
        }

    @Test
    fun when_update_period_end_notification_time_is_called_then_use_case_is_invoked_with_hour_and_minute() =
        runTest {
            coEvery { updatePeriodEndNotificationTimeUseCase(any(), any()) } returns Unit
            val viewModel = newViewModel()

            viewModel.updatePeriodEndNotificationTime(hour = 9, minute = 30)

            advanceUntilIdle()
            coVerify { updatePeriodEndNotificationTimeUseCase(9, 30) }
        }

    @Test
    fun when_update_recurrent_notification_time_is_called_then_use_case_is_invoked_with_hour_and_minute() =
        runTest {
            coEvery {
                updatePeriodEndNotificationTimeUseCase.updateRecurrentNotificationTime(
                    any(),
                    any()
                )
            } returns Unit
            val viewModel = newViewModel()

            viewModel.updateRecurrentNotificationTime(hour = 8, minute = 0)

            advanceUntilIdle()
            coVerify {
                updatePeriodEndNotificationTimeUseCase.updateRecurrentNotificationTime(
                    8,
                    0
                )
            }
        }

    @Test
    fun when_finish_budget_early_is_called_then_use_case_is_invoked() = runTest {
        coEvery { finishBudgetEarlyUseCase() } returns Unit
        val viewModel = newViewModel()

        viewModel.finishBudgetEarly()

        advanceUntilIdle()
        coVerify { finishBudgetEarlyUseCase() }
    }

    @Test
    fun when_clear_early_finish_state_is_called_then_use_case_is_invoked() = runTest {
        coEvery { clearEarlyFinishStateUseCase() } returns Unit
        val viewModel = newViewModel()

        viewModel.clearEarlyFinishState()

        advanceUntilIdle()
        coVerify { clearEarlyFinishStateUseCase() }
    }

    @Test
    fun when_mark_first_launch_complete_is_called_then_is_first_launch_becomes_false_and_use_case_is_invoked() =
        runTest {
            coEvery { markOnboardingCompletedUseCase() } returns Unit
            val viewModel = newViewModel()
            assertThat(viewModel.uiState.value.isFirstLaunch).isTrue()

            viewModel.markFirstLaunchComplete()

            advanceUntilIdle()
            assertThat(viewModel.uiState.value.isFirstLaunch).isFalse()
            coVerify { markOnboardingCompletedUseCase() }
        }

    @Test
    fun when_number_tapped_intent_is_processed_then_numpad_input_is_updated() = runTest {
        val viewModel = newViewModel()

        viewModel.processIntent(BudgetNumpadIntent.NumberTapped("5"))

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.numpadInput).isEqualTo("5")
        assertThat(viewModel.uiState.value.animState).isEqualTo(AnimState.EDITING)
    }

    @Test
    fun when_multiple_digits_are_tapped_then_numpad_input_concatenates_them() = runTest {
        val viewModel = newViewModel()

        viewModel.processIntent(BudgetNumpadIntent.NumberTapped("1"))
        viewModel.processIntent(BudgetNumpadIntent.NumberTapped("2"))
        viewModel.processIntent(BudgetNumpadIntent.NumberTapped("3"))

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.numpadInput).isEqualTo("123")
    }

    @Test
    fun when_backspace_is_tapped_then_last_digit_is_removed() = runTest {
        val viewModel = newViewModel()
        viewModel.processIntent(BudgetNumpadIntent.NumberTapped("1"))
        viewModel.processIntent(BudgetNumpadIntent.NumberTapped("2"))

        viewModel.processIntent(BudgetNumpadIntent.BackspaceTapped)

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.numpadInput).isEqualTo("1")
    }

    @Test
    fun when_dot_is_tapped_then_input_gains_a_dot() = runTest {
        val viewModel = newViewModel()
        viewModel.processIntent(BudgetNumpadIntent.NumberTapped("5"))

        viewModel.processIntent(BudgetNumpadIntent.DotTapped)

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.numpadInput).isEqualTo("5.")
    }

    @Test
    fun when_operator_is_tapped_then_input_gains_operator_and_calculation_mode_is_set() = runTest {
        val viewModel = newViewModel()
        viewModel.processIntent(BudgetNumpadIntent.NumberTapped("5"))

        viewModel.processIntent(BudgetNumpadIntent.OperatorTapped('+'))

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.numpadInput).isEqualTo("5+")
        assertThat(viewModel.uiState.value.isCalculation).isTrue()
    }

    @Test
    fun when_equals_is_tapped_on_valid_expression_then_input_becomes_evaluation_result() = runTest {
        val viewModel = newViewModel()
        viewModel.processIntent(BudgetNumpadIntent.NumberTapped("2"))
        viewModel.processIntent(BudgetNumpadIntent.OperatorTapped('+'))
        viewModel.processIntent(BudgetNumpadIntent.NumberTapped("3"))

        viewModel.processIntent(BudgetNumpadIntent.EqualsTapped)

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.numpadInput).isEqualTo("5")
    }

    @Test
    fun when_reset_input_is_tapped_then_input_is_cleared_and_calculation_is_reset() = runTest {
        val viewModel = newViewModel()
        viewModel.processIntent(BudgetNumpadIntent.NumberTapped("1"))
        viewModel.processIntent(BudgetNumpadIntent.OperatorTapped('+'))

        viewModel.processIntent(BudgetNumpadIntent.ResetInputTapped)

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.numpadInput).isEqualTo("")
        assertThat(viewModel.uiState.value.isCalculation).isFalse()
    }

    @Test
    fun when_set_calculation_mode_is_processed_then_calculation_flag_and_drag_progress_are_set() =
        runTest {
            val viewModel = newViewModel()

            viewModel.processIntent(BudgetNumpadIntent.SetCalculationMode(true))

            advanceUntilIdle()
            assertThat(viewModel.uiState.value.isCalculation).isTrue()
            assertThat(viewModel.uiState.value.dragProgress).isEqualTo(0f)
        }

    @Test
    fun when_set_drag_progress_is_processed_then_drag_progress_is_set() = runTest {
        val viewModel = newViewModel()

        viewModel.processIntent(BudgetNumpadIntent.SetDragProgress(0.5f))

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.dragProgress).isEqualTo(0.5f)
    }

    @Test
    fun when_set_edit_mode_intent_is_processed_then_edit_mode_is_updated() = runTest {
        val viewModel = newViewModel()

        viewModel.processIntent(BudgetEditorIntent.SetEditMode(EditMode.EDIT))

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.editMode).isEqualTo(EditMode.EDIT)
    }

    @Test
    fun when_set_anim_state_intent_is_processed_with_non_empty_numpad_input_then_anim_state_is_updated() =
        runTest {
            val viewModel = newViewModel()
            viewModel.processIntent(BudgetNumpadIntent.NumberTapped("5"))

            viewModel.processIntent(BudgetEditorIntent.SetAnimState(AnimState.EDITING))

            advanceUntilIdle()
            assertThat(viewModel.uiState.value.animState).isEqualTo(AnimState.EDITING)
        }

    @Test
    fun when_comment_updated_intent_is_processed_then_current_comment_is_updated() = runTest {
        val viewModel = newViewModel()

        viewModel.processIntent(BudgetEditorIntent.CommentUpdated("Lunch"))

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.currentComment).isEqualTo("Lunch")
    }

    @Test
    fun when_set_recurrent_enabled_intent_is_processed_then_flag_is_set() = runTest {
        val viewModel = newViewModel()

        viewModel.processIntent(BudgetEditorIntent.SetRecurrentEnabled(true))

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.isRecurrentEnabled).isTrue()
    }

    @Test
    fun when_set_credit_enabled_with_no_cutoff_day_intent_is_processed_then_credit_flag_and_dialog_are_set() =
        runTest {
            val viewModel = newViewModel()
            // budgetSettings is null in uiState, so creditCardCutoffDay is null

            viewModel.processIntent(BudgetEditorIntent.SetCreditEnabled(true))

            advanceUntilIdle()
            assertThat(viewModel.uiState.value.isCreditEnabled).isTrue()
            assertThat(viewModel.uiState.value.showCreditCutoffDialog).isTrue()
        }

    @Test
    fun when_date_selected_intent_is_processed_then_selected_date_is_updated() = runTest {
        val viewModel = newViewModel()
        val date = LocalDate.of(2026, 6, 21)

        viewModel.processIntent(BudgetEditorIntent.DateSelected(date))

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedDate).isEqualTo(date)
    }

    @Test
    fun when_dismiss_recurrent_dialog_intent_is_processed_then_dialog_is_hidden() = runTest {
        val viewModel = newViewModel()
        // Force the dialog open first
        viewModel.processIntent(BudgetEditorIntent.SetRecurrentEnabled(true))

        viewModel.processIntent(BudgetEditorIntent.DismissRecurrentDialog)

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.showRecurrentDialog).isFalse()
    }

    @Test
    fun when_dismiss_credit_cutoff_dialog_intent_is_processed_then_dialog_and_credit_are_cleared() =
        runTest {
            val viewModel = newViewModel()
            viewModel.processIntent(BudgetEditorIntent.SetCreditEnabled(true))

            viewModel.processIntent(BudgetEditorIntent.DismissCreditCutoffDialog)

            advanceUntilIdle()
            assertThat(viewModel.uiState.value.showCreditCutoffDialog).isFalse()
            assertThat(viewModel.uiState.value.isCreditEnabled).isFalse()
        }

    @Test
    fun when_mark_first_launch_complete_system_intent_is_processed_then_is_first_launch_becomes_false() =
        runTest {
            coEvery { markOnboardingCompletedUseCase() } returns Unit
            val viewModel = newViewModel()

            viewModel.processIntent(BudgetSystemIntent.MarkFirstLaunchComplete)

            advanceUntilIdle()
            assertThat(viewModel.uiState.value.isFirstLaunch).isFalse()
        }

    @Test
    fun when_set_lock_swipeable_system_intent_is_processed_then_lock_swipeable_is_set() = runTest {
        val viewModel = newViewModel()

        viewModel.processIntent(BudgetSystemIntent.SetLockSwipeable(true))

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.lockSwipeable).isTrue()
    }

    @Test
    fun when_set_lock_draggable_system_intent_is_processed_then_lock_draggable_is_set() = runTest {
        val viewModel = newViewModel()

        viewModel.processIntent(BudgetSystemIntent.SetLockDraggable(true))

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.lockDraggable).isTrue()
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

    @Test
    fun when_apply_intent_results_in_added_then_numpad_input_and_comment_are_cleared() = runTest {
        coEvery {
            transactionHandler.applyTransaction(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns
                ApplyTransactionResult.Added(normalizedInput = "12.34")
        val viewModel = newViewModel()
        viewModel.processIntent(BudgetNumpadIntent.NumberTapped("1"))
        viewModel.processIntent(BudgetNumpadIntent.NumberTapped("2"))
        viewModel.processIntent(BudgetEditorIntent.CommentUpdated("Lunch"))

        viewModel.processIntent(BudgetNumpadIntent.ApplyTapped)

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.numpadInput).isEqualTo("")
        assertThat(viewModel.uiState.value.currentComment).isEqualTo("")
    }

    @Test
    fun when_apply_intent_results_in_show_recurrent_dialog_then_dialog_and_pending_values_are_set() =
        runTest {
            coEvery {
                transactionHandler.applyTransaction(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns
                    ApplyTransactionResult.ShowRecurrentDialog(
                        normalizedInput = "12.34",
                        amount = BigDecimal("12.34"),
                    )
            val viewModel = newViewModel()
            viewModel.processIntent(BudgetNumpadIntent.NumberTapped("1"))
            viewModel.processIntent(BudgetNumpadIntent.NumberTapped("2"))
            viewModel.processIntent(BudgetEditorIntent.SetRecurrentEnabled(true))

            viewModel.processIntent(BudgetNumpadIntent.ApplyTapped)

            advanceUntilIdle()
            assertThat(viewModel.uiState.value.showRecurrentDialog).isTrue()
            assertThat(viewModel.uiState.value.pendingRecurrentAmount).isEqualTo(BigDecimal("12.34"))
        }

    @Test
    fun when_apply_intent_results_in_queued_for_next_period_then_show_message_effect_is_emitted() =
        runTest {
            coEvery {
                transactionHandler.applyTransaction(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns
                    ApplyTransactionResult.QueuedForNextPeriod(normalizedInput = "12.34")
            val viewModel = newViewModel()
            viewModel.processIntent(BudgetNumpadIntent.NumberTapped("1"))
            viewModel.processIntent(BudgetNumpadIntent.NumberTapped("2"))

            viewModel.effects.test {
                viewModel.processIntent(BudgetNumpadIntent.ApplyTapped)
                advanceUntilIdle()

                val effect = awaitItem()
                assertThat(effect).isInstanceOf(BudgetUiEffect.ShowMessage::class.java)
                effect as BudgetUiEffect.ShowMessage
                assertThat(effect.message).isEqualTo("Gasto en cola para el proximo periodo")
                cancelAndIgnoreRemainingEvents()
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
                advanceUntilIdle()

                val effect = awaitItem()
                assertThat(effect).isInstanceOf(BudgetUiEffect.ShowMessage::class.java)
                effect as BudgetUiEffect.ShowMessage
                assertThat(effect.message).isEqualTo("Could not delete transaction")
                cancelAndIgnoreRemainingEvents()
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
                advanceUntilIdle()

                val effect = awaitItem()
                assertThat(effect).isInstanceOf(BudgetUiEffect.ShowMessage::class.java)
                effect as BudgetUiEffect.ShowMessage
                assertThat(effect.message).isEqualTo("Could not restore transaction")
                cancelAndIgnoreRemainingEvents()
            }
        }
}
