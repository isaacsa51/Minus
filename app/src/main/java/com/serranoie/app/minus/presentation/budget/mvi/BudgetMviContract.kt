package com.serranoie.app.minus.presentation.budget.mvi

import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.editor.AnimState
import com.serranoie.app.minus.presentation.editor.EditMode
import java.math.BigDecimal
import java.time.LocalDate

sealed interface BudgetUiIntent {
    data class NumberTapped(val digit: String) : BudgetUiIntent
    data object DotTapped : BudgetUiIntent
    data object BackspaceTapped : BudgetUiIntent
    data object ApplyTapped : BudgetUiIntent
    data object ResetInputTapped : BudgetUiIntent

    data class DeleteTransactionTapped(val transaction: Transaction) : BudgetUiIntent
    data class EditTransactionTapped(val updatedTransaction: Transaction) : BudgetUiIntent

    data class DateSelected(val date: LocalDate) : BudgetUiIntent
    data class UpdateSettings(val settings: BudgetSettings) : BudgetUiIntent
    data class SetEditMode(val mode: EditMode) : BudgetUiIntent
    data class SetAnimState(val state: AnimState) : BudgetUiIntent
    data class CommentUpdated(val comment: String) : BudgetUiIntent

    data class RolloverSplitEqually(val remaining: BigDecimal) : BudgetUiIntent
    data class RolloverCarryToNextDay(val remaining: BigDecimal) : BudgetUiIntent
    data object DismissRolloverDialog : BudgetUiIntent

    data object MarkFirstLaunchComplete : BudgetUiIntent
    data class SetRecurrentEnabled(val enabled: Boolean) : BudgetUiIntent
    data object DismissRecurrentDialog : BudgetUiIntent
    data class RecurrentExpenseApplied(
        val frequency: RecurrentFrequency,
        val endDate: LocalDate,
        val subscriptionDay: Int? = null
    ) : BudgetUiIntent

    data object FinishBudgetEarly : BudgetUiIntent
    data object TriggerTestNotifications : BudgetUiIntent
}

sealed interface BudgetUiEffect {
    data class ShowMessage(val message: String) : BudgetUiEffect
}
