package com.serranoie.app.minus.presentation.ui.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.domain.shortcut.normalizeQuickAmount
import com.serranoie.app.minus.domain.shortcut.quickAddCategories
import com.serranoie.app.minus.domain.usecase.GetCurrentPeriodIdUseCase
import com.serranoie.app.minus.presentation.ui.budget.ApplyTransactionResult
import com.serranoie.app.minus.presentation.ui.budget.BudgetTransactionHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.asLog
import logcat.logcat
import javax.inject.Inject

@HiltViewModel
class QuickExpenseViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionHandler: BudgetTransactionHandler,
    private val getCurrentPeriodId: GetCurrentPeriodIdUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickExpenseUiState.INITIAL)
    val uiState: StateFlow<QuickExpenseUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<QuickExpenseUiEffect>()
    val effects: SharedFlow<QuickExpenseUiEffect> = _effects.asSharedFlow()

    private var budgetSettings: BudgetSettings? = null

    init {
        viewModelScope.launch {
            val settings = runCatching { budgetRepository.getBudgetSettingsSync() }
                .onFailure { logcat { "Quick add could not read budget settings: ${it.asLog()}" } }
                .getOrNull()
            budgetSettings = settings
            _uiState.update {
                it.copy(
                    isLoading = false,
                    needsBudget = settings == null,
                    currency = settings?.let { s -> SupportedCurrency.findByCode(s.currencyCode) },
                )
            }
            budgetRepository.getActiveCategories()
                .catch { logcat { "Quick add categories failed: ${it.asLog()}" } }
                .collect { categories ->
                    _uiState.update { it.copy(categories = quickAddCategories(categories)) }
                }
        }
    }

    fun processIntent(intent: QuickExpenseUiIntent) {
        when (intent) {
            is QuickExpenseUiIntent.AmountChanged -> _uiState.update {
                it.copy(amount = normalizeQuickAmount(intent.raw), error = null)
            }

            is QuickExpenseUiIntent.CategoryToggled -> _uiState.update {
                it.copy(selectedCategory = intent.name.takeUnless { name -> name == it.selectedCategory })
            }

            QuickExpenseUiIntent.Save -> save()
        }
    }

    private fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val result = withContext(NonCancellable) {
                transactionHandler.applyTransaction(
                    input = state.amount,
                    isCalculation = false,
                    isRecurrentEnabled = false,
                    isCreditEnabled = false,
                    comment = state.selectedCategory.orEmpty(),
                    note = "",
                    budgetSettings = budgetSettings,
                    resolveActivePeriodId = { getCurrentPeriodId() },
                )
            }
            when (result) {
                is ApplyTransactionResult.Added -> _effects.emit(QuickExpenseUiEffect.Saved(queuedForNextPeriod = false))
                is ApplyTransactionResult.QueuedForNextPeriod ->
                    _effects.emit(QuickExpenseUiEffect.Saved(queuedForNextPeriod = true))

                else -> {
                    logcat { "Quick add save rejected: $result" }
                    _uiState.update { it.copy(isSaving = false, error = QuickExpenseUiState.Error.SaveFailed) }
                }
            }
        }
    }
}
