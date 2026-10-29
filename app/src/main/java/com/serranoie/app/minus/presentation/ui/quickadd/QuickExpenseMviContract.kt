package com.serranoie.app.minus.presentation.ui.quickadd

import com.serranoie.app.minus.domain.model.SupportedCurrency

data class QuickExpenseUiState(
    val isLoading: Boolean = true,
    val needsBudget: Boolean = false,
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val amount: String = "",
    val currency: SupportedCurrency? = null,
    val isSaving: Boolean = false,
    val error: Error? = null,
) {
    val canSave: Boolean get() = !isSaving && amount.toBigDecimalOrNull()?.signum() == 1

    enum class Error { SaveFailed }

    companion object {
        val INITIAL = QuickExpenseUiState()
    }
}

sealed interface QuickExpenseUiIntent {
    data class AmountChanged(val raw: String) : QuickExpenseUiIntent
    data class CategoryToggled(val name: String) : QuickExpenseUiIntent
    data object Save : QuickExpenseUiIntent
}

sealed interface QuickExpenseUiEffect {
    data class Saved(val queuedForNextPeriod: Boolean) : QuickExpenseUiEffect
}
