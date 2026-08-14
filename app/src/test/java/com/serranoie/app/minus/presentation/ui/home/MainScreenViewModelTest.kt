package com.serranoie.app.minus.presentation.ui.home

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.FirstLaunchTutorialStage
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.model.UserSettings
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
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
    private val settingsFlow = MutableStateFlow(UserSettings())

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { settingsRepository.observeSettings() } returns settingsFlow
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

    private suspend fun <T> ReceiveTurbine<T>.awaitCondition(predicate: (T) -> Boolean): T {
        while (true) {
            val item = awaitItem()
            if (predicate(item)) return item
        }
    }

    @Test
    fun when_viewmodel_is_created_then_initial_state_is_default() = runTest {
        val viewModel = newViewModel()
        viewModel.uiState.test {
            val state = awaitCondition { it.selectedViewPeriod == BudgetPeriod.DAILY }
            assertThat(state.pendingDeleteTransaction).isNull()
            assertThat(state.isSnackbarVisible).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun when_queue_delete_with_undo_is_processed_then_snackbar_state_and_effect_are_set() =
        runTest {
            val viewModel = newViewModel()
            val transaction = sampleTransaction()

            viewModel.uiState.test {
                val uiTurbine = this
                uiTurbine.awaitCondition { !it.isSnackbarVisible }
                
                viewModel.effects.test {
                    val effectTurbine = this
                    viewModel.processIntent(
                        MainScreenUiIntent.QueueDeleteWithUndo(transaction, "Deleted"),
                        currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
                    )

                    val state = uiTurbine.awaitCondition { it.isSnackbarVisible && it.pendingDeleteTransaction == transaction }
                    assertThat(state.snackbarMessage).isEqualTo("Deleted")

                    val effect = effectTurbine.awaitItem()
                    assertThat(effect).isInstanceOf(MainScreenUiEffect.ShowUndoSnackbar::class.java)
                    effectTurbine.cancelAndIgnoreRemainingEvents()
                }
                uiTurbine.cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun when_cancel_pending_delete_is_processed_then_snackbar_state_is_cleared_and_request_undo_is_emitted() =
        runTest {
            val viewModel = newViewModel()
            val transaction = sampleTransaction()
            
            viewModel.uiState.test {
                val uiTurbine = this
                uiTurbine.awaitCondition { !it.isSnackbarVisible }
                
                viewModel.effects.test {
                    val effectTurbine = this
                    viewModel.processIntent(
                        MainScreenUiIntent.QueueDeleteWithUndo(transaction, "Deleted"),
                        currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
                    )
                    uiTurbine.awaitCondition { it.isSnackbarVisible }
                    effectTurbine.awaitItem() // Consume ShowUndoSnackbar

                    viewModel.processIntent(
                        MainScreenUiIntent.CancelPendingDelete,
                        currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
                    )

                    uiTurbine.awaitCondition { !it.isSnackbarVisible }
                    val effect = effectTurbine.awaitItem()
                    assertThat(effect).isInstanceOf(MainScreenUiEffect.RequestUndo::class.java)
                    effectTurbine.cancelAndIgnoreRemainingEvents()
                }
                uiTurbine.cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun when_dismiss_snackbar_is_processed_then_snackbar_state_is_cleared_and_no_effect_is_emitted() =
        runTest {
            val viewModel = newViewModel()
            viewModel.uiState.test {
                val uiTurbine = this
                uiTurbine.awaitCondition { !it.isSnackbarVisible }
                
                viewModel.effects.test {
                    val effectTurbine = this
                    viewModel.processIntent(
                        MainScreenUiIntent.QueueDeleteWithUndo(sampleTransaction(), "Deleted"),
                        currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
                    )
                    uiTurbine.awaitCondition { it.isSnackbarVisible }
                    effectTurbine.awaitItem()

                    viewModel.processIntent(
                        MainScreenUiIntent.DismissSnackbar,
                        currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
                    )

                    uiTurbine.awaitCondition { !it.isSnackbarVisible }
                    effectTurbine.expectNoEvents()
                    effectTurbine.cancelAndIgnoreRemainingEvents()
                }
                uiTurbine.cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun when_show_budget_period_sheet_with_force_setup_is_processed_then_sheet_and_force_setup_flags_are_true() =
        runTest {
            val viewModel = newViewModel()
            viewModel.uiState.test {
                val uiTurbine = this
                uiTurbine.awaitCondition { !it.showBudgetPeriodSheet }
                
                viewModel.processIntent(
                    MainScreenUiIntent.ShowBudgetPeriodSheet(forceSetup = true),
                    currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
                )

                uiTurbine.awaitCondition { it.showBudgetPeriodSheet && it.forceBudgetPeriodSheetSetup }
                uiTurbine.cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun when_show_budget_period_sheet_without_force_setup_is_processed_then_sheet_is_visible_but_force_setup_is_false() =
        runTest {
            val viewModel = newViewModel()
            viewModel.uiState.test {
                val uiTurbine = this
                uiTurbine.awaitCondition { !it.showBudgetPeriodSheet }

                viewModel.processIntent(
                    MainScreenUiIntent.ShowBudgetPeriodSheet(forceSetup = false),
                    currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
                )

                uiTurbine.awaitCondition { it.showBudgetPeriodSheet && !it.forceBudgetPeriodSheetSetup }
                uiTurbine.cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun when_hide_budget_period_sheet_is_processed_then_both_sheet_flags_are_false() = runTest {
        val viewModel = newViewModel()
        viewModel.uiState.test {
            val uiTurbine = this
            uiTurbine.awaitCondition { !it.showBudgetPeriodSheet }
            
            viewModel.processIntent(
                MainScreenUiIntent.ShowBudgetPeriodSheet(forceSetup = true),
                currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
            )
            uiTurbine.awaitCondition { it.showBudgetPeriodSheet }

            viewModel.processIntent(
                MainScreenUiIntent.HideBudgetPeriodSheet,
                currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
            )

            uiTurbine.awaitCondition { !it.showBudgetPeriodSheet }
            uiTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun when_set_selected_period_is_processed_then_selected_view_period_is_updated() = runTest {
        val viewModel = newViewModel()
        viewModel.uiState.test {
            val uiTurbine = this
            uiTurbine.awaitCondition { it.selectedViewPeriod == BudgetPeriod.DAILY }

            viewModel.processIntent(
                MainScreenUiIntent.SetSelectedPeriod(BudgetPeriod.WEEKLY),
                currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
            )

            uiTurbine.awaitCondition { it.selectedViewPeriod == BudgetPeriod.WEEKLY }
            uiTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun when_mark_wallet_sheet_opened_is_processed_then_wallet_sheet_opened_is_true() = runTest {
        val viewModel = newViewModel()
        viewModel.uiState.test {
            val uiTurbine = this
            uiTurbine.awaitCondition { !it.walletSheetOpened }

            viewModel.processIntent(
                MainScreenUiIntent.MarkWalletSheetOpened,
                currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
            )

            uiTurbine.awaitCondition { it.walletSheetOpened }
            uiTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun when_set_shown_stage_is_processed_then_shown_stage_is_updated() = runTest {
        val viewModel = newViewModel()
        viewModel.uiState.test {
            val uiTurbine = this
            uiTurbine.awaitCondition { it.shownStage == null }

            viewModel.processIntent(
                MainScreenUiIntent.SetShownStage(FirstLaunchTutorialStage.TAP_ANY_NUMBER),
                currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
            )

            uiTurbine.awaitCondition { it.shownStage == FirstLaunchTutorialStage.TAP_ANY_NUMBER }
            uiTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun when_set_drag_progress_is_processed_then_update_drag_progress_effect_is_emitted_with_value() =
        runTest {
            val viewModel = newViewModel()
            viewModel.effects.test {
                val effectTurbine = this
                viewModel.processIntent(
                    MainScreenUiIntent.SetDragProgress(0.5f),
                    currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
                )

                assertThat(effectTurbine.awaitItem()).isEqualTo(MainScreenUiEffect.UpdateDragProgress(0.5f))
                effectTurbine.cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun when_3500ms_pass_without_user_action_then_snackbar_is_auto_dismissed_and_no_effect_is_emitted() =
        runTest {
            val viewModel = newViewModel()
            viewModel.uiState.test {
                val uiTurbine = this
                uiTurbine.awaitCondition { !it.isSnackbarVisible }
                
                viewModel.processIntent(
                    MainScreenUiIntent.QueueDeleteWithUndo(sampleTransaction(), "Deleted"),
                    currentTutorialStage = FirstLaunchTutorialStage.COMPLETED,
                )
                uiTurbine.awaitCondition { it.isSnackbarVisible }

                advanceTimeBy(3_500L.milliseconds)

                uiTurbine.awaitCondition { !it.isSnackbarVisible }
                uiTurbine.cancelAndIgnoreRemainingEvents()
            }
        }
}
