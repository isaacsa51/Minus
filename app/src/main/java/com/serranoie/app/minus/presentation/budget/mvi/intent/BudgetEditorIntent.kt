package com.serranoie.app.minus.presentation.budget.mvi.intent

import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.presentation.budget.mvi.BudgetUiIntent
import com.serranoie.app.minus.presentation.editor.AnimState
import com.serranoie.app.minus.presentation.editor.EditMode
import java.time.LocalDate

sealed interface BudgetEditorIntent : BudgetUiIntent {
    data class DateSelected(val date: LocalDate) : BudgetEditorIntent
    data class UpdateSettings(val settings: BudgetSettings) : BudgetEditorIntent
    data class SetEditMode(val mode: EditMode) : BudgetEditorIntent
    data class SetAnimState(val state: AnimState) : BudgetEditorIntent
    data class CommentUpdated(val comment: String) : BudgetEditorIntent
    data class DeleteTag(val tag: String) : BudgetEditorIntent
    data class SetRecurrentEnabled(val enabled: Boolean) : BudgetEditorIntent
    data object DismissRecurrentDialog : BudgetEditorIntent
    data class RecurrentExpenseApplied(
        val frequency: RecurrentFrequency,
        val endDate: LocalDate,
        val subscriptionDay: Int? = null,
    ) : BudgetEditorIntent
    data object FinishBudgetEarly : BudgetEditorIntent
}
