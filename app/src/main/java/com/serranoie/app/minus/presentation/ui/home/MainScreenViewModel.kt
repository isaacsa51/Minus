package com.serranoie.app.minus.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.FirstLaunchTutorialStage
import com.serranoie.app.minus.domain.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _localState = MutableStateFlow(MainScreenLocalState())

    private val _effects = MutableSharedFlow<MainScreenUiEffect>()
    val effects: SharedFlow<MainScreenUiEffect> = _effects.asSharedFlow()

    private var autoDismissJob: Job? = null

    val uiState: StateFlow<MainScreenUiState> = combine(
        settingsRepository.observeSettings(),
        _localState
    ) { settings, local ->
        MainScreenUiState(
            onboardingCompleted = settings.onboardingCompleted,
            tutorialBoxCompleted = settings.tutorialBoxCompleted,
            showCreditQuickToggleFeature = settings.isCreditQuickToggleEnabled,
            directCategoryPopupEnabled = settings.categoryPickerDirectPopupEnabled,
            categoryGridModeEnabled = settings.categoryGridModeEnabled,
            tutorialStage = settings.firstLaunchTutorialStage,
            selectedViewPeriod = settings.budgetSplitViewPeriod ?: local.selectedViewPeriod,
            pendingDeleteTransaction = local.pendingDeleteTransaction,
            isSnackbarVisible = local.isSnackbarVisible,
            snackbarMessage = local.snackbarMessage,
            snackbarActionLabel = local.snackbarActionLabel,
            snackbarHasUndo = local.snackbarHasUndo,
            shownStage = local.shownStage,
            showBudgetPeriodSheet = local.showBudgetPeriodSheet,
            forceBudgetPeriodSheetSetup = local.forceBudgetPeriodSheetSetup,
            walletSheetOpened = local.walletSheetOpened,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = MainScreenUiState()
    )

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
            is MainScreenUiIntent.SetTutorialBoxCompleted -> setTutorialBoxCompleted(intent.completed)

            is MainScreenUiIntent.ProcessBudgetTransactionIntent -> { /* caller */ }
            is MainScreenUiIntent.ProcessBudgetEditorIntent -> { /* caller */ }
            is MainScreenUiIntent.ProcessBudgetNumpadIntent -> { /* caller */ }
        }
    }

    fun onTransactionDeleteQueued(transaction: Transaction, message: String) {
        autoDismissJob?.cancel()

        _localState.update {
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

        _localState.update {
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
        val transaction = _localState.value.pendingDeleteTransaction
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

        _localState.update {
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
        viewModelScope.launch {
            settingsRepository.setFirstLaunchTutorialStage(currentStage.next())
        }
    }

    private fun setShownStage(stage: FirstLaunchTutorialStage?) {
        _localState.update { it.copy(shownStage = stage) }
    }

    private fun setDragProgress(progress: Float) {
        viewModelScope.launch {
            _effects.emit(MainScreenUiEffect.UpdateDragProgress(progress))
        }
    }

    private fun showBudgetPeriodSheet(forceSetup: Boolean) {
        _localState.update {
            it.copy(
                showBudgetPeriodSheet = true,
                forceBudgetPeriodSheetSetup = forceSetup,
            )
        }
    }

    private fun hideBudgetPeriodSheet() {
        _localState.update {
            it.copy(
                showBudgetPeriodSheet = false,
                forceBudgetPeriodSheetSetup = false,
            )
        }
    }

    private fun setSelectedPeriod(period: BudgetPeriod) {
        _localState.update { it.copy(selectedViewPeriod = period) }
        viewModelScope.launch {
            settingsRepository.setBudgetSplitViewPeriod(period)
        }
    }

    private fun markWalletSheetOpened() {
        _localState.update { it.copy(walletSheetOpened = true) }
    }

    private fun setTutorialBoxCompleted(completed: Boolean) {
        viewModelScope.launch {
            settingsRepository.setTutorialBoxCompleted(completed)
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoDismissJob?.cancel()
    }
}

private data class MainScreenLocalState(
    val selectedViewPeriod: BudgetPeriod = BudgetPeriod.DAILY,
    val pendingDeleteTransaction: Transaction? = null,
    val isSnackbarVisible: Boolean = false,
    val snackbarMessage: String = "",
    val snackbarActionLabel: String = "",
    val snackbarHasUndo: Boolean = false,
    val shownStage: FirstLaunchTutorialStage? = null,
    val showBudgetPeriodSheet: Boolean = false,
    val forceBudgetPeriodSheetSetup: Boolean = false,
    val walletSheetOpened: Boolean = false,
)
