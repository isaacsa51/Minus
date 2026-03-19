package com.serranoie.app.minus.presentation.budget

import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.editor.AnimState
import com.serranoie.app.minus.presentation.editor.EditMode
import java.math.BigDecimal
import java.time.LocalDate

data class BudgetUiState(
    val isLoading: Boolean = false,
    val budgetSettings: BudgetSettings? = null,
    val budgetState: BudgetState? = null,
    val transactions: List<Transaction> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val error: String? = null,
    val numpadInput: String = "",
    val isNumpadValid: Boolean = false,
    val editMode: EditMode = EditMode.ADD,
    val animState: AnimState = AnimState.IDLE,
    val currentComment: String = "",
    val tags: List<String> = emptyList(),
    val showRolloverDialog: Boolean = false,
    val remainingFromPreviousPeriod: BigDecimal = BigDecimal.ZERO,
    val isFirstLaunch: Boolean = true,
    val isRecurrentEnabled: Boolean = false,
    val showRecurrentDialog: Boolean = false,
    val pendingRecurrentAmount: BigDecimal? = null,
    val pendingRecurrentComment: String = "",
    val currentPeriodStartedAtMillis: Long = 0L,
    val currentPeriodId: Long = 0L
) {
    companion object {
        val INITIAL = BudgetUiState()
    }
}

sealed class BudgetUiEvent {
    data class OnNumberInput(val digit: String) : BudgetUiEvent()
    data object OnDotInput : BudgetUiEvent()
    data object OnBackspace : BudgetUiEvent()
    data object OnApply : BudgetUiEvent()
    data class OnDeleteTransaction(val transaction: Transaction) : BudgetUiEvent()
    data class OnEditTransaction(val updatedTransaction: Transaction) : BudgetUiEvent()
    data class OnDateSelected(val date: LocalDate) : BudgetUiEvent()
    data class OnUpdateSettings(val settings: BudgetSettings) : BudgetUiEvent()
    data object OnResetInput : BudgetUiEvent()
    data class OnSetEditMode(val mode: EditMode) : BudgetUiEvent()
    data class OnSetAnimState(val state: AnimState) : BudgetUiEvent()
    data class OnCommentUpdate(val comment: String) : BudgetUiEvent()
    data class OnRolloverSplitEqually(val remaining: BigDecimal) : BudgetUiEvent()
    data class OnRolloverCarryToNextDay(val remaining: BigDecimal) : BudgetUiEvent()
    data object OnDismissRolloverDialog : BudgetUiEvent()
    data object OnMarkFirstLaunchComplete : BudgetUiEvent()
    data class OnSetRecurrentEnabled(val enabled: Boolean) : BudgetUiEvent()
    data object OnDismissRecurrentDialog : BudgetUiEvent()
    data class OnRecurrentExpenseApply(
        val frequency: com.serranoie.app.minus.domain.model.RecurrentFrequency, 
        val endDate: LocalDate,
        val subscriptionDay: Int? = null
    ) : BudgetUiEvent()
    data object OnFinishBudgetEarly : BudgetUiEvent()
}
