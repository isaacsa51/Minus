package com.serranoie.app.minus.presentation.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.logcat
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private const val TAG = "ISAAC:Onboarding"

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val notificationScheduler: NotificationScheduler,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<OnboardingUiEffect>()
    val effects: SharedFlow<OnboardingUiEffect> = _effects.asSharedFlow()

    fun processIntent(intent: OnboardingUiIntent) {
        logcat(TAG) { "processIntent: $intent (state before: currentStep=${_uiState.value.currentStep}, isCompleted=${_uiState.value.isCompleted})" }
        when (intent) {
            is OnboardingUiIntent.OnBudgetAmountChanged -> handleBudgetAmountChanged(intent.amount)
            is OnboardingUiIntent.OnDaysSelected -> handleDaysSelected(intent.days)
            is OnboardingUiIntent.OnNextStep -> handleNextStep()
            is OnboardingUiIntent.OnPreviousStep -> handlePreviousStep()
            is OnboardingUiIntent.OnCompleteOnboarding -> handleCompleteOnboarding()
            is OnboardingUiIntent.OnWelcomeDismissed -> handleWelcomeDismissed()
            is OnboardingUiIntent.OnDateRangeSelected -> handleDateRangeSelected(
                intent.startDate,
                intent.endDate,
                intent.budgetDisplayDays,
            )
        }
    }

    private fun handleBudgetAmountChanged(amount: String) {
        if (amount.contains(".") && amount.count { it == '.' } > 1) return
        if (amount.length > 10) return

        _uiState.update { it.copy(budgetInput = amount) }
    }

    private fun handleDaysSelected(days: Int) {
        val period = daysToBudgetPeriod(days)
        _uiState.update { it.copy(selectedDays = days, selectedPeriod = period) }
    }

    private fun handleDateRangeSelected(
        startDate: LocalDate,
        endDate: LocalDate,
        budgetDisplayDays: Int,
    ) {
        val days = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        val period = budgetDisplayDaysToBudgetPeriod(budgetDisplayDays)

        _uiState.update {
            it.copy(
                startDate = startDate,
                endDate = endDate,
                selectedDays = days,
                daysInPeriod = budgetDisplayDays,
                selectedPeriod = period,
            )
        }

        handleCompleteOnboarding()
    }

    private fun handleNextStep() {
        val currentStep = _uiState.value.currentStep
        if (currentStep < OnboardingStep.entries.size - 1) {
            _uiState.update { it.copy(currentStep = currentStep + 1) }
        }
    }

    private fun handlePreviousStep() {
        val currentStep = _uiState.value.currentStep
        if (currentStep > 0) {
            _uiState.update { it.copy(currentStep = currentStep - 1) }
        }
    }

    private fun handleCompleteOnboarding() {
        val state = _uiState.value

        val budgetAmount = try {
            BigDecimal(state.budgetInput)
        } catch (e: NumberFormatException) {
            return
        }

        if (budgetAmount <= BigDecimal.ZERO) return

        viewModelScope.launch {
            try {
                val startDate = state.startDate ?: LocalDate.now()
                val endDate = state.endDate ?: startDate.plusDays(state.selectedDays.toLong() - 1)

                budgetRepository.saveBudgetSettings(
                    BudgetSettings(
                        totalBudget = budgetAmount,
                        period = state.selectedPeriod,
                        startDate = startDate,
                        endDate = endDate,
                        currencyCode = "USD",
                        daysInPeriod = state.selectedDays,
                        rollOverEnabled = true,
                        rollOverCarryForward = false,
                    )
                )

                notificationScheduler.schedulePeriodEndNotification(endDate)
                notificationScheduler.initializeNotifications()

                settingsRepository.setOnboardingCompleted(true)

                _uiState.update { it.copy(isCompleted = true) }
                _effects.emit(OnboardingUiEffect.OnboardingCompleted)
            } catch (e: Exception) {
                _effects.emit(OnboardingUiEffect.OnboardingFailed(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Mark onboarding as completed from the welcome step. The welcome
     * step is informational only — the user has not entered a budget
     * yet, so we skip the budget save and notification scheduling.
     * The actual budget setup happens in the wallet screen that the
     * nav graph opens next.
     */
    private fun handleWelcomeDismissed() {
        logcat(TAG) { "handleWelcomeDismissed: setting onboarding_completed=true" }
        viewModelScope.launch {
            try {
                settingsRepository.setOnboardingCompleted(true)
                _uiState.update { it.copy(isCompleted = true) }
                logcat(TAG) { "handleWelcomeDismissed: emitted OnboardingCompleted" }
                _effects.emit(OnboardingUiEffect.OnboardingCompleted)
            } catch (e: Exception) {
                logcat(TAG) { "handleWelcomeDismissed failed: ${e.message}" }
                _effects.emit(OnboardingUiEffect.OnboardingFailed(e.message ?: "Unknown error"))
            }
        }
    }

    private fun daysToBudgetPeriod(days: Int): BudgetPeriod = when (days) {
        1 -> BudgetPeriod.DAILY
        7 -> BudgetPeriod.WEEKLY
        15 -> BudgetPeriod.BIWEEKLY
        else -> BudgetPeriod.MONTHLY
    }

    private fun budgetDisplayDaysToBudgetPeriod(days: Int): BudgetPeriod = when (days) {
        1 -> BudgetPeriod.DAILY
        7 -> BudgetPeriod.WEEKLY
        14 -> BudgetPeriod.BIWEEKLY
        else -> BudgetPeriod.MONTHLY
    }
}
