package com.serranoie.app.minus.presentation.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.ArchivedBudget
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.model.UserSettings
import com.serranoie.app.minus.domain.usecase.ClearEarlyFinishStateUseCase
import com.serranoie.app.minus.domain.usecase.ObserveCurrentPeriodBoundaryUseCase
import com.serranoie.app.minus.domain.usecase.PersistBudgetSettingsUseCase
import com.serranoie.app.minus.presentation.ui.budget.BudgetStateCalculator
import com.serranoie.app.minus.presentation.ui.history.calculateNextChargeDate
import com.serranoie.app.minus.presentation.ui.history.getRecurringChargesInPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import logcat.logcat
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val budgetSettings: BudgetSettings? = null,
    val budgetState: BudgetState? = null,
    val allTransactions: List<Transaction> = emptyList(),
    val currentPeriodId: Long = 0L,
    val selectedPeriodId: Long? = null,
    val archivedBudgets: List<ArchivedBudget> = emptyList(),
    val displayState: AnalyticsState = AnalyticsState(),
    val userSettings: UserSettings = UserSettings.DEFAULT,
    val rolloverAmount: BigDecimal = BigDecimal.ZERO,
    val rolloverCarryForward: Boolean = false,
)

sealed interface AnalyticsUiEffect {
    data object NavigateToMainWithWallet : AnalyticsUiEffect
    data object NavigateToMain : AnalyticsUiEffect
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
    private val budgetStateCalculator: BudgetStateCalculator,
    private val observeCurrentPeriodBoundaryUseCase: ObserveCurrentPeriodBoundaryUseCase,
    private val clearEarlyFinishStateUseCase: ClearEarlyFinishStateUseCase,
    private val persistBudgetSettingsUseCase: PersistBudgetSettingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val _effects = MutableStateFlow<AnalyticsUiEffect?>(null)
    val effects: StateFlow<AnalyticsUiEffect?> = _effects.asStateFlow()

    init {
        observeBudgetData()
        viewModelScope.launch {
            val demoIds = listOf(20260501L, 20260601L, 20260708L, 20260615L)
            demoIds.forEach { budgetRepository.deleteArchivedBudget(it) }
        }
    }

    private fun observeBudgetData() {
        viewModelScope.launch {
            combine(
                budgetRepository.getBudgetSettings().distinctUntilChanged(),
                budgetRepository.getTransactions().distinctUntilChanged(),
                budgetRepository.getArchivedBudgets().distinctUntilChanged(),
                observeCurrentPeriodBoundaryUseCase().distinctUntilChanged(),
                settingsRepository.observeSettings().distinctUntilChanged(),
                settingsRepository.observeCurrentPeriodRollover().distinctUntilChanged(),
            ) { args: Array<Any?> ->
                val settings = args[0] as BudgetSettings?
                val transactions = args[1] as List<Transaction>
                val archives = args[2] as List<ArchivedBudget>
                val periodBoundary = args[3] as Pair<Long, Long>
                val userSettings = args[4] as UserSettings
                val rollover = args[5] as Pair<BigDecimal, Boolean>

                val currentPeriodId = periodBoundary.second
                val updatedState = _uiState.value.copy(
                    isLoading = false,
                    budgetSettings = settings,
                    allTransactions = transactions,
                    archivedBudgets = archives,
                    currentPeriodId = currentPeriodId,
                    userSettings = userSettings,
                    rolloverAmount = rollover.first,
                    rolloverCarryForward = rollover.second,
                    displayState = if (_uiState.value.selectedPeriodId != null && _uiState.value.selectedPeriodId != currentPeriodId) {
                        buildHistoricalDisplayState(
                            _uiState.value.selectedPeriodId!!, transactions, archives
                        )
                    } else {
                        buildDisplayState(
                            settings = settings,
                            allTransactions = transactions,
                            currentPeriodId = currentPeriodId,
                            userSettings = userSettings,
                            rolloverAmountFromPref = rollover.first,
                        )
                    },
                )
                _uiState.value = updatedState
            }.distinctUntilChanged().collect {}
        }
    }

    private fun buildHistoricalDisplayState(
        periodId: Long, allTransactions: List<Transaction>, archives: List<ArchivedBudget>
    ): AnalyticsState {
        val archive = archives.find { it.periodId == periodId } ?: return buildDisplayState(
            settings = _uiState.value.budgetSettings,
            allTransactions = allTransactions,
            currentPeriodId = _uiState.value.currentPeriodId,
            userSettings = _uiState.value.userSettings,
            rolloverAmountFromPref = _uiState.value.rolloverAmount
        )

        val transactions = allTransactions.filter { it.periodId == periodId && !it.isDeleted }
        val (paidRecurring, upcomingRecurring, oneTimeSpends) = splitRecurringAndOneTime(
            allTransactions = allTransactions,
            filteredTransactions = transactions,
            periodStart = archive.startDate,
            periodEnd = archive.endDate,
            today = archive.endDate, // For archives, today is end date
        )

        val actualSpends = (oneTimeSpends + paidRecurring).distinctBy { it.id }

        val budgetSettings = BudgetSettings(
            totalBudget = archive.totalBudget,
            period = archive.periodType,
            startDate = archive.startDate,
            endDate = archive.endDate,
            currencyCode = archive.currencyCode
        )

        val budgetState = budgetStateCalculator.calculateBudgetState(
            settings = budgetSettings,
            transactions = transactions,
            currentDate = archive.endDate
        )

        return AnalyticsState(
            periodFinished = true,
            transactions = actualSpends,
            spends = actualSpends,
            recurringInPeriod = (paidRecurring + upcomingRecurring).distinctBy { it.id },
            oneTimeSpends = oneTimeSpends,
            wholeBudget = archive.totalBudget,
            currencyCode = archive.currencyCode,
            startPeriodDate = Date.from(
                archive.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            ),
            finishPeriodDate = Date.from(
                archive.endDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            ),
            budgetSettingsForDisplay = budgetSettings,
            budgetStateForDisplay = budgetState,
            isHistoricalView = true
        )
    }

    private fun buildDisplayState(
        settings: BudgetSettings?,
        allTransactions: List<Transaction>,
        currentPeriodId: Long,
        userSettings: UserSettings,
        rolloverAmountFromPref: BigDecimal,
    ): AnalyticsState {
        if (settings == null) return AnalyticsState(isLoading = false)

        val today = LocalDate.now()
        val endDate = settings.getPeriodEndDate()

        val transactions = filterTransactions(
            allTransactions = allTransactions,
            currentPeriodId = currentPeriodId,
            settings = settings,
            shouldShowEndedSnapshot = false,
            endedPeriodStartDate = null,
            lastPeriodEnd = null,
        )

        val (paidRecurring, upcomingRecurring, oneTimeSpends) = splitRecurringAndOneTime(
            allTransactions = allTransactions,
            filteredTransactions = transactions,
            periodStart = settings.startDate,
            periodEnd = settings.getPeriodEndDate(),
            today = today,
        )

        val displayBudgetState = budgetStateCalculator.calculateBudgetState(
            settings = settings,
            transactions = transactions,
            currentDate = today
        )

        val startDate = settings.startDate.atStartOfDay().atZone(ZoneId.systemDefault())
            .let { Date.from(it.toInstant()) }

        val plannedFinishDate =
            settings.getPeriodEndDate().atStartOfDay().atZone(ZoneId.systemDefault())
                .let { Date.from(it.toInstant()) }

        val earlyFinishActive = userSettings.earlyFinishActive
        val earlyFinishActualDate =
            if (userSettings.earlyFinishActualDate > 0) Date(userSettings.earlyFinishActualDate) else null
        val earlyFinishOriginalEndDate =
            if (userSettings.earlyFinishOriginalEndDate > 0) Date(userSettings.earlyFinishOriginalEndDate) else null

        val savingsPreferences = userSettings.savingsPreferences

        val periodFinishedNaturally =
            settings.getPeriodEndDate().isBefore(today) || settings.getPeriodEndDate()
                .isEqual(today)
        val periodFinished = periodFinishedNaturally || earlyFinishActive

        val displayBudget = displayBudgetState.totalBudget

        val totalSpent = displayBudgetState.totalSpentInPeriod
        val remainingBudget = displayBudgetState.remainingToday

        val dailyBudget = displayBudgetState.dailyBudget

        val extraAffordableDays =
            if (earlyFinishActive && remainingBudget > BigDecimal.ZERO && dailyBudget > BigDecimal.ZERO) {
                remainingBudget.divide(dailyBudget, 0, RoundingMode.DOWN).toInt().coerceAtLeast(0)
            } else 0

        val shouldShowRolloverStyle = displayBudget > settings.totalBudget

        val creditOwed = allTransactions.filter { it.isCredit && !it.isDeleted && !it.isCreditPaid }
            .sumOf { it.amount }
        val creditTransactions =
            allTransactions.filter { it.isCredit && !it.isDeleted && !it.isCreditPaid }
                .sortedByDescending { it.date }
        val debtAdjustedBalance = remainingBudget.subtract(creditOwed)

        return AnalyticsState(
            periodFinished = periodFinished,
            transactions = transactions,
            spends = transactions,
            recurringInPeriod = (paidRecurring + upcomingRecurring).distinctBy { it.id },
            oneTimeSpends = oneTimeSpends,
            wholeBudget = displayBudget,
            currencyCode = settings.currencyCode,
            finishPeriodActualDate = if (earlyFinishActive) earlyFinishActualDate else null,
            startPeriodDate = startDate,
            finishPeriodDate = if (earlyFinishActive) earlyFinishOriginalEndDate else plannedFinishDate,
            extraAffordableDaysFromRemaining = extraAffordableDays,
            budgetSettingsForDisplay = settings,
            budgetStateForDisplay = displayBudgetState,
            showRolloverStyleInBudgetDisplay = shouldShowRolloverStyle,
            isLoading = false,
            savingsPreferences = savingsPreferences,
            creditOwed = creditOwed,
            debtAdjustedBalance = debtAdjustedBalance,
            creditTransactions = creditTransactions,
        )
    }

    private fun splitRecurringAndOneTime(
        allTransactions: List<Transaction>,
        filteredTransactions: List<Transaction>,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        today: LocalDate,
    ): Triple<List<Transaction>, List<Transaction>, List<Transaction>> {
        val oneTimeSpends =
            filteredTransactions.filterNot { it.isDeleted }.filterNot { it.isRecurrent }

        val paidRecurringInPeriod =
            filteredTransactions.filterNot { it.isDeleted }.filter { it.isRecurrent }

        val recurringParents = (paidRecurringInPeriod + allTransactions.filterNot { it.isDeleted }
            .filter { it.isRecurrent }).distinctBy { it.id }

        val paidCharges = recurringParents.flatMap { parent ->
            getRecurringChargesInPeriod(parent, periodStart, periodEnd, today)
        }

        val upcomingCharges = recurringParents.mapNotNull { parent ->
            val date = calculateNextChargeDate(parent, today) ?: parent.date?.toLocalDate()
                ?.takeIf { it.isAfter(today) } ?: return@mapNotNull null

            if (date.isBefore(periodStart) || date.isAfter(periodEnd)) {
                return@mapNotNull null
            }
            val chargeId = parent.id * 1_000_000L + date.toEpochDay()
            parent.copy(
                date = date.atTime(parent.date?.toLocalTime() ?: LocalTime.MIDNIGHT),
                id = chargeId,
            )
        }

        return Triple(paidCharges, upcomingCharges, oneTimeSpends)
    }

    private fun filterTransactions(
        allTransactions: List<Transaction>,
        currentPeriodId: Long,
        settings: BudgetSettings,
        shouldShowEndedSnapshot: Boolean,
        endedPeriodStartDate: LocalDate?,
        lastPeriodEnd: LocalDate?,
    ): List<Transaction> {
        return allTransactions.filter { tx ->
            val txDate = tx.date?.toLocalDate() ?: return@filter false
            if (shouldShowEndedSnapshot && endedPeriodStartDate != null && lastPeriodEnd != null) {
                !txDate.isBefore(endedPeriodStartDate) && !txDate.isAfter(lastPeriodEnd)
            } else {
                if (currentPeriodId > 0L && tx.periodId > 0L) {
                    return@filter tx.periodId == currentPeriodId
                }
                val start = settings.startDate
                val end = settings.getPeriodEndDate()
                !txDate.isBefore(start) && !txDate.isAfter(end)
            }
        }
    }

    fun onCreateNewPeriod() {
        viewModelScope.launch {
            clearEarlyFinishStateUseCase()
            _effects.value = AnalyticsUiEffect.NavigateToMainWithWallet
        }
    }

    fun onClose() {
        if (_uiState.value.selectedPeriodId != null) {
            _uiState.value = _uiState.value.copy(selectedPeriodId = null)
            observeBudgetData() // Refresh to current
        } else {
            _effects.value = AnalyticsUiEffect.NavigateToMain
        }
    }

    fun onPeriodSelected(periodId: Long) {
        _uiState.value = _uiState.value.copy(selectedPeriodId = periodId)
        observeBudgetData()
    }

    fun onMarkCreditPaid() {
        viewModelScope.launch {
            budgetRepository.markAllCreditTransactionsAsPaid()
        }
    }

    fun onPayTransactionClick(transactionId: Long) {
        viewModelScope.launch {
            budgetRepository.markTransactionAsPaid(transactionId)
        }
    }

    fun onCutoffDayChanged(day: Int) {
        logcat("AnalyticsViewModel") { "onCutoffDayChanged: day=$day" }
        val currentSettings = _uiState.value.budgetSettings
        if (currentSettings == null) {
            logcat("AnalyticsViewModel") { "onCutoffDayChanged: currentSettings is NULL, skipping persistence" }
            return
        }
        if (day !in 1..31) return

        viewModelScope.launch {
            logcat("AnalyticsViewModel") { "onCutoffDayChanged: launching persistence for day=$day" }
            persistBudgetSettingsUseCase(
                settings = currentSettings.copy(creditCardCutoffDay = day),
                forceNewPeriodBoundary = false,
            )
        }
    }

    fun consumeEffect() {
        _effects.value = null
    }

    companion object {
        private const val RECURRING_TAG = "Analytics/Recurring"
    }
}
