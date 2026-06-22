package com.serranoie.app.minus.presentation.ui.onboarding

import com.serranoie.app.minus.domain.model.BudgetPeriod
import java.time.LocalDate

/**
 * MVI contract for the Onboarding screen.
 *
 * Follows the same pattern as [com.serranoie.app.minus.presentation.ui.budget.mvi.BudgetMviContract]
 * and [com.serranoie.app.minus.presentation.ui.home.MainScreenMviContract]:
 *
 *  - [OnboardingUiState]   — the single source of truth for the screen, exposed
 *                           as `StateFlow` by the ViewModel
 *  - [OnboardingUiIntent]  — the set of user actions the screen can dispatch
 *  - [OnboardingUiEffect]  — one-shot events the screen should react to
 *                           (navigation, snackbar, …). Emitted via
 *                           `SharedFlow` by the ViewModel.
 *
 * The ViewModel is responsible for translating intents into state changes
 * and effect emissions; the UI is responsible for collecting state and
 * effects and rendering the corresponding UI.
 */
data class OnboardingUiState(
    val currentStep: Int = 0,
    val budgetInput: String = "",
    val selectedDays: Int = 1,
    val daysInPeriod: Int = 1,
    val selectedPeriod: BudgetPeriod = BudgetPeriod.DAILY,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val isLoading: Boolean = false,
    val isCompleted: Boolean = false,
    val error: String? = null,
)

/**
 * The set of user actions the Onboarding screen can dispatch.
 *
 * Naming aligns with the rest of the project ([BudgetUiIntent],
 * [MainScreenUiIntent]) — these are no longer called "events" to make
 * the MVI intent / state / effect split explicit.
 */
sealed interface OnboardingUiIntent {
    data class OnBudgetAmountChanged(val amount: String) : OnboardingUiIntent
    data class OnDaysSelected(val days: Int) : OnboardingUiIntent
    data class OnDateRangeSelected(
        val startDate: LocalDate,
        val endDate: LocalDate,
        val budgetDisplayDays: Int,
    ) : OnboardingUiIntent
    data object OnNextStep : OnboardingUiIntent
    data object OnPreviousStep : OnboardingUiIntent
    data object OnCompleteOnboarding : OnboardingUiIntent
}

/**
 * One-shot events the screen should react to. Emitted via
 * `SharedFlow<OnboardingUiEffect>` by the ViewModel.
 */
sealed interface OnboardingUiEffect {
    /**
     * Emitted after the user has successfully completed onboarding
     * (budget saved, notifications scheduled, onboarding flag persisted).
     * The screen should navigate away to the main flow.
     */
    data object OnboardingCompleted : OnboardingUiEffect

    /**
     * Emitted when onboarding fails for a reason the user should see
     * (e.g. the budget repository throws). The screen should surface
     * this as a snackbar / error state.
     */
    data class OnboardingFailed(val message: String) : OnboardingUiEffect
}

/**
 * The three steps of the onboarding flow. Used by [OnboardingUiState.currentStep].
 */
enum class OnboardingStep {
    WELCOME,
    BUDGET_AMOUNT,
    BUDGET_PERIOD,
}
