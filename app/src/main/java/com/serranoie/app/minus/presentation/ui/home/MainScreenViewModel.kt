package com.serranoie.app.minus.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.tutorial.FirstLaunchTutorialStage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<MainScreenUiEffect>()
    val effects: SharedFlow<MainScreenUiEffect> = _effects.asSharedFlow()

    private var autoDismissJob: Job? = null

    init {
        _uiState.update { it.copy(selectedViewPeriod = BudgetPeriod.DAILY) }
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.update {
                    it.copy(
                        selectedViewPeriod = settings.budgetSplitViewPeriod ?: BudgetPeriod.DAILY,
                    )
                }
            }
        }
    }

    fun processIntent(intent: MainScreenUiIntent, currentTutorialStage: FirstLaunchTutorialStage) {
        when (intent) {
            is MainScreenUiIntent.QueueDeleteWithUndo -> queueDeleteWithUndo(intent)
            is MainScreenUiIntent.CancelPendingDelete -> cancelPendingDelete()
            is MainScreenUiIntent.DismissSnackbar -> dismissSnackbar()
            is MainScreenUiIntent.AdvanceTutorial -> advanceTutorial(intent, currentTutorialStage)
            is MainScreenUiIntent.SetShownStage -> setShownStage(intent.stage)
            is MainScreenUiIntent.SetDragProgress -> setDragProgress(intent.progress)

            is MainScreenUiIntent.ShowBudgetPeriodSheet -> showBudgetPeriodSheet(intent.forceSetup)
            is MainScreenUiIntent.HideBudgetPeriodSheet -> hideBudgetPeriodSheet()
            is MainScreenUiIntent.SetSelectedPeriod -> setSelectedPeriod(intent.period)
            is MainScreenUiIntent.MarkWalletSheetOpened -> markWalletSheetOpened()

            is MainScreenUiIntent.ProcessBudgetTransactionIntent -> { /* caller */ }
            is MainScreenUiIntent.ProcessBudgetEditorIntent -> { /* caller */ }
            is MainScreenUiIntent.ProcessBudgetNumpadIntent -> { /* caller */ }
        }
    }

    fun onTransactionDeleteQueued(transaction: Transaction, message: String) {
        autoDismissJob?.cancel()

        _uiState.update {
            it.copy(
                pendingDeleteTransaction = transaction,
                isSnackbarVisible = true,
                snackbarMessage = message,
                snackbarActionLabel = "UNDO",
                snackbarHasUndo = true,
            )
        }

        viewModelScope.launch {
            _effects.emit(
                MainScreenUiEffect.ShowUndoSnackbar(
                    message = message,
                    actionLabel = "UNDO",
                )
            )
        }

        autoDismissJob = viewModelScope.launch {
            delay(3_500L.milliseconds)
            dismissSnackbar()
        }
    }

    private fun queueDeleteWithUndo(intent: MainScreenUiIntent.QueueDeleteWithUndo) {
        onTransactionDeleteQueued(intent.transaction, intent.message)
    }

    fun onPendingDeleteCanceled() {
        autoDismissJob?.cancel()
        autoDismissJob = null

        _uiState.update {
            it.copy(
                pendingDeleteTransaction = null,
                isSnackbarVisible = false,
                snackbarMessage = "",
                snackbarActionLabel = "",
                snackbarHasUndo = false,
            )
        }
    }

    private fun cancelPendingDelete() {
        val transaction = _uiState.value.pendingDeleteTransaction
        onPendingDeleteCanceled()
        if (transaction != null) {
            viewModelScope.launch {
                _effects.emit(MainScreenUiEffect.RequestUndo(transaction))
            }
        }
    }

    private fun dismissSnackbar() {
        autoDismissJob?.cancel()
        autoDismissJob = null

        _uiState.update {
            it.copy(
                pendingDeleteTransaction = null,
                isSnackbarVisible = false,
                snackbarMessage = "",
                snackbarActionLabel = "",
                snackbarHasUndo = false,
            )
        }
    }

    private fun advanceTutorial(intent: MainScreenUiIntent.AdvanceTutorial, currentStage: FirstLaunchTutorialStage) {
        if (currentStage != intent.expected) return
        // Actual DataStore write is handled by the composable (needs Context)
    }

    private fun setShownStage(stage: FirstLaunchTutorialStage?) {
        _uiState.update { it.copy(shownStage = stage) }
    }

    private fun setDragProgress(progress: Float) {
        viewModelScope.launch {
            _effects.emit(MainScreenUiEffect.UpdateDragProgress(progress))
        }
    }

    private fun showBudgetPeriodSheet(forceSetup: Boolean) {
        _uiState.update {
            it.copy(
                showBudgetPeriodSheet = true,
                forceBudgetPeriodSheetSetup = forceSetup,
            )
        }
    }

    private fun hideBudgetPeriodSheet() {
        _uiState.update {
            it.copy(
                showBudgetPeriodSheet = false,
                forceBudgetPeriodSheetSetup = false,
            )
        }
    }

    private fun setSelectedPeriod(period: BudgetPeriod) {
        _uiState.update { it.copy(selectedViewPeriod = period) }
        viewModelScope.launch {
            settingsRepository.setBudgetSplitViewPeriod(period)
        }
    }

    private fun markWalletSheetOpened() {
        _uiState.update { it.copy(walletSheetOpened = true) }
    }

    override fun onCleared() {
        super.onCleared()
        autoDismissJob?.cancel()
    }
}
