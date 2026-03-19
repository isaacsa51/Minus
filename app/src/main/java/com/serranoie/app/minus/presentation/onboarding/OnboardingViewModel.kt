package com.serranoie.app.minus.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * ViewModel for the onboarding flow.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /**
     * Handle onboarding events.
     */
    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.OnBudgetAmountChanged -> handleBudgetAmountChanged(event.amount)
            is OnboardingEvent.OnDaysSelected -> handleDaysSelected(event.days)
            is OnboardingEvent.OnNextStep -> handleNextStep()
            is OnboardingEvent.OnPreviousStep -> handlePreviousStep()
            is OnboardingEvent.OnCompleteOnboarding -> handleCompleteOnboarding()
            is OnboardingEvent.OnDateRangeSelected -> handleDateRangeSelected(
                event.startDate, 
                event.endDate, 
                event.budgetDisplayDays
            )
        }
    }

    private fun handleBudgetAmountChanged(amount: String) {
        // Prevent multiple decimals
        if (amount.contains(".") && amount.count { it == '.' } > 1) return
        // Prevent input longer than 10 characters
        if (amount.length > 10) return

        _uiState.update { it.copy(budgetInput = amount) }
    }

    private fun handleDaysSelected(days: Int) {
        val period = when (days) {
            1 -> BudgetPeriod.DAILY
            7 -> BudgetPeriod.WEEKLY
            15 -> BudgetPeriod.BIWEEKLY
            else -> BudgetPeriod.MONTHLY
        }
        _uiState.update { it.copy(selectedDays = days, selectedPeriod = period) }
    }

    private fun handleDateRangeSelected(startDate: LocalDate, endDate: LocalDate, budgetDisplayDays: Int) {
        val days = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        
        // Determine the period based on the user's preference for budget display
        val period = when (budgetDisplayDays) {
            1 -> BudgetPeriod.DAILY
            7 -> BudgetPeriod.WEEKLY
            14 -> BudgetPeriod.BIWEEKLY
            else -> BudgetPeriod.MONTHLY
        }
        
        _uiState.update { 
            it.copy(
                startDate = startDate,
                endDate = endDate,
                selectedDays = days,
                daysInPeriod = budgetDisplayDays,
                selectedPeriod = period
            ) 
        }
        
        // Auto-complete onboarding after selecting dates and budget display
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

        // Validate budget amount
        val budgetAmount = try {
            BigDecimal(state.budgetInput)
        } catch (e: NumberFormatException) {
            return
        }

        if (budgetAmount <= BigDecimal.ZERO) return

        viewModelScope.launch {
            // Use the selected dates or default to today
            val startDate = state.startDate ?: LocalDate.now()
            val endDate = state.endDate ?: startDate.plusDays(state.selectedDays.toLong() - 1)

            // Save the budget settings with the custom days in period
            budgetRepository.saveBudgetSettings(
                BudgetSettings(
                    totalBudget = budgetAmount,
                    period = state.selectedPeriod,
                    startDate = startDate,
                    endDate = endDate,
                    currencyCode = "USD",
                    daysInPeriod = state.selectedDays,
                    rollOverEnabled = true,
                    rollOverCarryForward = false
                )
            )

            // Schedule notifications for period end and recurrent expenses
            notificationScheduler.schedulePeriodEndNotification(endDate)
            notificationScheduler.initializeNotifications()

            // Mark onboarding as completed
            _uiState.update { it.copy(isCompleted = true) }
        }
    }
}

/**
 * Onboarding UI State.
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
    val error: String? = null
)

/**
 * Onboarding steps.
 */
enum class OnboardingStep {
    WELCOME,       // Welcome message
    BUDGET_AMOUNT, // Enter budget amount
    BUDGET_PERIOD  // Select period (days)
}

/**
 * Onboarding events.
 */
sealed class OnboardingEvent {
    data class OnBudgetAmountChanged(val amount: String) : OnboardingEvent()
    data class OnDaysSelected(val days: Int) : OnboardingEvent()
    data class OnDateRangeSelected(val startDate: LocalDate, val endDate: LocalDate, val budgetDisplayDays: Int) : OnboardingEvent()
    data object OnNextStep : OnboardingEvent()
    data object OnPreviousStep : OnboardingEvent()
    data object OnCompleteOnboarding : OnboardingEvent()
}