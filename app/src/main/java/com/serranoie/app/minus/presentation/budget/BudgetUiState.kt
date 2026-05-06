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
	val isFirstLaunch: Boolean = true,
	val isRecurrentEnabled: Boolean = false,
	val isCreditEnabled: Boolean = false,
	val showRecurrentDialog: Boolean = false,
	val showCreditCutoffDialog: Boolean = false,
	val pendingRecurrentAmount: BigDecimal? = null,
	val pendingRecurrentComment: String = "",
	val currentPeriodStartedAtMillis: Long = 0L,
	val currentPeriodId: Long = 0L,
	val isCalculation: Boolean = false,
	val dragProgress: Float = 0f,
	val lockSwipeable: Boolean = false,
	val lockDraggable: Boolean = false,
	val pendingExpensesForNextPeriod: List<Transaction> = emptyList()
) {
	companion object {
		val INITIAL = BudgetUiState()
	}
}
