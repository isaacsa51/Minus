package com.serranoie.app.minus.presentation.ui.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.model.UserSettings
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetEditorIntent
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetNumpadIntent
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetTransactionIntent
import com.serranoie.app.minus.presentation.ui.editor.AnimState
import com.serranoie.app.minus.presentation.ui.tutorial.FirstLaunchTutorialStage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        every { settingsRepository.observeSettings() } returns flowOf(UserSettings())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = MainScreenViewModel(settingsRepository)

    private fun sampleTransaction(
        id: Long = 1L,
        amount: BigDecimal = BigDecimal("12.34"),
    ) = Transaction(
        id = id,
        amount = amount,
        comment = "Coffee",
        date = LocalDateTime.of(2026, 1, 1, 9, 0),
    )

    @Test
    fun when_viewmodel_is_created_then_initial_state_is_default() = runTest {
        // Given / When
        val viewModel = newViewModel()

        // Then
        val state = viewModel.uiState.value
        assertThat(state.pendingDeleteTransaction).isNull()
        assertThat(state.isSnackbarVisible).isFalse()
        assertThat(state.snackbarMessage).isEqualTo("")
        assertThat(state.snackbarActionLabel).isEqualTo("")
        assertThat(state.snackbarHasUndo).isFalse()
        assertThat(state.shownStage).isNull()
        assertThat(state.showBudgetPeriodSheet).isFalse()
        assertThat(state.forceBudgetPeriodSheetSetup).isFalse()
        assertThat(state.selectedViewPeriod).isEqualTo(BudgetPeriod.DAILY)
        assertThat(state.walletSheetOpened).isFalse()
    }

    @Test
    fun when_queue_delete_with_undo_is_processed_then_snackbar_state_and_effect_are_set() =
        runTest {
            // Given
            val viewModel = newViewModel()
            val transaction = sampleTransaction()

            // When
            viewModel.effects.test {
                viewModel.processIntent(
                    MainScreenUiIntent.QueueDeleteWithUndo(transaction, "Deleted"),
                    currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
                )

                // Then
                val state = viewModel.uiState.value
                assertThat(state.pendingDeleteTransaction).isEqualTo(transaction)
                assertThat(state.isSnackbarVisible).isTrue()
                assertThat(state.snackbarMessage).isEqualTo("Deleted")
                assertThat(state.snackbarActionLabel).isEqualTo("UNDO")
                assertThat(state.snackbarHasUndo).isTrue()

                val effect = awaitItem()
                assertThat(effect).isInstanceOf(MainScreenUiEffect.ShowUndoSnackbar::class.java)
                effect as MainScreenUiEffect.ShowUndoSnackbar
                assertThat(effect.message).isEqualTo("Deleted")
                assertThat(effect.actionLabel).isEqualTo("UNDO")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun when_cancel_pending_delete_is_processed_then_snackbar_state_is_cleared_and_request_undo_is_emitted() =
        runTest {
            // Given
            val viewModel = newViewModel()
            viewModel.effects.test {
                viewModel.processIntent(
                    MainScreenUiIntent.QueueDeleteWithUndo(sampleTransaction(), "Deleted"),
                    currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
                )
                runCurrent()
                awaitItem()

                // When
                viewModel.processIntent(
                    MainScreenUiIntent.CancelPendingDelete,
                    currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
                )
                runCurrent()

                // Then
                val state = viewModel.uiState.value
                assertThat(state.pendingDeleteTransaction).isNull()
                assertThat(state.isSnackbarVisible).isFalse()
                assertThat(state.snackbarMessage).isEqualTo("")
                assertThat(state.snackbarActionLabel).isEqualTo("")
                assertThat(state.snackbarHasUndo).isFalse()

                val effect = awaitItem()
                assertThat(effect).isInstanceOf(MainScreenUiEffect.RequestUndo::class.java)
                assertThat((effect as MainScreenUiEffect.RequestUndo).transaction.id).isEqualTo(1L)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun when_dismiss_snackbar_is_processed_then_snackbar_state_is_cleared_and_no_effect_is_emitted() =
        runTest {
            // Given
            val viewModel = newViewModel()
            viewModel.effects.test {
                viewModel.processIntent(
                    MainScreenUiIntent.QueueDeleteWithUndo(sampleTransaction(), "Deleted"),
                    currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
                )
                runCurrent()
                awaitItem()

                // When
                viewModel.processIntent(
                    MainScreenUiIntent.DismissSnackbar,
                    currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
                )
                runCurrent()

                // Then
                val state = viewModel.uiState.value
                assertThat(state.pendingDeleteTransaction).isNull()
                assertThat(state.isSnackbarVisible).isFalse()
                assertThat(state.snackbarMessage).isEqualTo("")
                assertThat(state.snackbarActionLabel).isEqualTo("")
                assertThat(state.snackbarHasUndo).isFalse()

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun when_show_budget_period_sheet_with_force_setup_is_processed_then_sheet_and_force_setup_flags_are_true() =
        runTest {
            // Given
            val viewModel = newViewModel()

            // When
            viewModel.processIntent(
                MainScreenUiIntent.ShowBudgetPeriodSheet(forceSetup = true),
                currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
            )

            // Then
            val state = viewModel.uiState.value
            assertThat(state.showBudgetPeriodSheet).isTrue()
            assertThat(state.forceBudgetPeriodSheetSetup).isTrue()
        }

    @Test
    fun when_show_budget_period_sheet_without_force_setup_is_processed_then_sheet_is_visible_but_force_setup_is_false() =
        runTest {
            // Given
            val viewModel = newViewModel()

            // When
            viewModel.processIntent(
                MainScreenUiIntent.ShowBudgetPeriodSheet(forceSetup = false),
                currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
            )

            // Then
            val state = viewModel.uiState.value
            assertThat(state.showBudgetPeriodSheet).isTrue()
            assertThat(state.forceBudgetPeriodSheetSetup).isFalse()
        }

    @Test
    fun when_hide_budget_period_sheet_is_processed_then_both_sheet_flags_are_false() = runTest {
        // Given
        val viewModel = newViewModel()
        viewModel.processIntent(
            MainScreenUiIntent.ShowBudgetPeriodSheet(forceSetup = true),
            currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
        )

        // When
        viewModel.processIntent(
            MainScreenUiIntent.HideBudgetPeriodSheet,
            currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
        )

        // Then
        val state = viewModel.uiState.value
        assertThat(state.showBudgetPeriodSheet).isFalse()
        assertThat(state.forceBudgetPeriodSheetSetup).isFalse()
    }

    @Test
    fun when_set_selected_period_is_processed_then_selected_view_period_is_updated() = runTest {
        // Given
        val viewModel = newViewModel()

        // When
        viewModel.processIntent(
            MainScreenUiIntent.SetSelectedPeriod(BudgetPeriod.WEEKLY),
            currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
        )

        // Then
        assertThat(viewModel.uiState.value.selectedViewPeriod).isEqualTo(BudgetPeriod.WEEKLY)
    }

    @Test
    fun when_set_selected_period_is_processed_twice_then_last_value_wins() = runTest {
        // Given
        val viewModel = newViewModel()

        // When
        viewModel.processIntent(
            MainScreenUiIntent.SetSelectedPeriod(BudgetPeriod.WEEKLY),
            currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
        )
        viewModel.processIntent(
            MainScreenUiIntent.SetSelectedPeriod(BudgetPeriod.MONTHLY),
            currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
        )

        // Then
        assertThat(viewModel.uiState.value.selectedViewPeriod).isEqualTo(BudgetPeriod.MONTHLY)
    }

    @Test
    fun when_mark_wallet_sheet_opened_is_processed_then_wallet_sheet_opened_is_true() = runTest {
        // Given
        val viewModel = newViewModel()

        // When
        viewModel.processIntent(
            MainScreenUiIntent.MarkWalletSheetOpened,
            currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
        )

        // Then
        assertThat(viewModel.uiState.value.walletSheetOpened).isTrue()
    }

    @Test
    fun when_mark_wallet_sheet_opened_is_processed_twice_then_flag_stays_true() = runTest {
        // Given
        val viewModel = newViewModel()

        // When
        viewModel.processIntent(
            MainScreenUiIntent.MarkWalletSheetOpened,
            currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
        )
        viewModel.processIntent(
            MainScreenUiIntent.MarkWalletSheetOpened,
            currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
        )

        // Then
        assertThat(viewModel.uiState.value.walletSheetOpened).isTrue()
    }

    @Test
    fun when_set_shown_stage_is_processed_then_shown_stage_is_updated() = runTest {
        // Given
        val viewModel = newViewModel()

        // When
        viewModel.processIntent(
            MainScreenUiIntent.SetShownStage(FirstLaunchTutorialStage.TAP_ANY_NUMBER),
            currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
        )

        // Then
        assertThat(viewModel.uiState.value.shownStage).isEqualTo(FirstLaunchTutorialStage.TAP_ANY_NUMBER)
    }

    @Test
    fun when_set_shown_stage_with_null_is_processed_then_shown_stage_is_null() = runTest {
        // Given
        val viewModel = newViewModel()
        viewModel.processIntent(
            MainScreenUiIntent.SetShownStage(FirstLaunchTutorialStage.TAP_DONE_SAVE),
            currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
        )

        // When
        viewModel.processIntent(
            MainScreenUiIntent.SetShownStage(null),
            currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
        )

        // Then
        assertThat(viewModel.uiState.value.shownStage).isNull()
    }

    @Test
    fun when_set_drag_progress_is_processed_then_update_drag_progress_effect_is_emitted_with_value() =
        runTest {
            // Given
            val viewModel = newViewModel()

            // When / Then
            viewModel.effects.test {
                viewModel.processIntent(
                    MainScreenUiIntent.SetDragProgress(0.5f),
                    currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
                )

                val effect = awaitItem()
                assertThat(effect).isEqualTo(MainScreenUiEffect.UpdateDragProgress(0.5f))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun when_set_drag_progress_is_processed_twice_then_effects_are_emitted_in_order() = runTest {
        // Given
        val viewModel = newViewModel()

        // When / Then
        viewModel.effects.test {
            viewModel.processIntent(
                MainScreenUiIntent.SetDragProgress(0.25f),
                currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
            )
            assertThat(awaitItem()).isEqualTo(MainScreenUiEffect.UpdateDragProgress(0.25f))

            viewModel.processIntent(
                MainScreenUiIntent.SetDragProgress(0.75f),
                currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
            )
            assertThat(awaitItem()).isEqualTo(MainScreenUiEffect.UpdateDragProgress(0.75f))

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun when_advance_tutorial_expected_mismatches_current_then_state_is_unchanged() = runTest {
        // Given
        val viewModel = newViewModel()
        val before = viewModel.uiState.value

        // When
        viewModel.processIntent(
            MainScreenUiIntent.AdvanceTutorial(FirstLaunchTutorialStage.TAP_ANY_NUMBER),
            currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
        )

        // Then
        assertThat(viewModel.uiState.value).isEqualTo(before)
    }

    @Test
    fun when_advance_tutorial_expected_matches_current_then_state_remains_unchanged() = runTest {
        // Given
        val viewModel = newViewModel()
        val before = viewModel.uiState.value

        // When
        viewModel.processIntent(
            MainScreenUiIntent.AdvanceTutorial(FirstLaunchTutorialStage.TAP_DONE_SAVE),
            currentTutorialStage = FirstLaunchTutorialStage.TAP_DONE_SAVE,
        )

        // Then
        assertThat(viewModel.uiState.value).isEqualTo(before)
    }

    @Test
    fun when_process_budget_intents_are_processed_then_state_is_unchanged_and_no_effect_is_emitted() =
        runTest {
            // Given
            val viewModel = newViewModel()
            val before = viewModel.uiState.value

            // When
            viewModel.effects.test {
                viewModel.processIntent(
                    MainScreenUiIntent.ProcessBudgetTransactionIntent(
                        BudgetTransactionIntent.RestoreTransactionTapped(sampleTransaction())
                    ),
                    currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
                )
                viewModel.processIntent(
                    MainScreenUiIntent.ProcessBudgetEditorIntent(
                        BudgetEditorIntent.SetAnimState(AnimState.IDLE)
                    ),
                    currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
                )
                viewModel.processIntent(
                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                        BudgetNumpadIntent.SetDragProgress(0.1f)
                    ),
                    currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
                )
                runCurrent()

                // Then
                assertThat(viewModel.uiState.value).isEqualTo(before)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun when_3500ms_pass_without_user_action_then_snackbar_is_auto_dismissed_and_no_effect_is_emitted() =
        runTest {
            // Given
            val viewModel = newViewModel()
            viewModel.effects.test {
                viewModel.processIntent(
                    MainScreenUiIntent.QueueDeleteWithUndo(sampleTransaction(), "Deleted"),
                    currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
                )
                advanceUntilIdle()
                awaitItem()

                // When
                advanceTimeBy(3_500L.milliseconds)
                advanceUntilIdle()

                // Then
                val state = viewModel.uiState.value
                assertThat(state.pendingDeleteTransaction).isNull()
                assertThat(state.isSnackbarVisible).isFalse()
                assertThat(state.snackbarMessage).isEqualTo("")
                assertThat(state.snackbarActionLabel).isEqualTo("")
                assertThat(state.snackbarHasUndo).isFalse()

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }
}
