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
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.computeDynamicAllocations
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
import java.time.temporal.ChronoUnit
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
            budgetRepository.seedArchivedData()
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
        val totalSpent = actualSpends.sumOf { it.amount }

        val budgetSettings = BudgetSettings(
            totalBudget = archive.totalBudget,
            period = archive.periodType,
            startDate = archive.startDate,
            endDate = archive.endDate,
            currencyCode = archive.currencyCode
        )

        val budgetState = BudgetState(
            remainingToday = archive.totalBudget.subtract(totalSpent)
                .coerceAtLeast(BigDecimal.ZERO),
            totalSpentToday = BigDecimal.ZERO,
            dailyBudget = archive.totalBudget.divide(
                BigDecimal(
                    ChronoUnit.DAYS.between(
                        archive.startDate, archive.endDate
                    ).toInt() + 1
                ), 2, RoundingMode.HALF_UP
            ),
            daysRemaining = 0,
            progress = (totalSpent.divide(archive.totalBudget, 4, RoundingMode.HALF_UP)
                .toFloat()).coerceIn(0f, 1f),
            isOverBudget = totalSpent > archive.totalBudget,
            totalBudget = archive.totalBudget,
            totalSpentInPeriod = totalSpent
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
        if (settings == null) return AnalyticsState()

        val today = LocalDate.now()
        val endDate = settings.getPeriodEndDate()

        // We use rolloverAmountFromPref instead of manually reading from preferences
        val remainingFromLastPeriod = rolloverAmountFromPref
        val shouldShowEndedSnapshot = false // Logic for snapshot can be added if needed

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

        val actualSpends = (oneTimeSpends + paidRecurring).distinctBy { it.id }

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

        val wholeBudget = settings.totalBudget

        val totalSpent = actualSpends.sumOf { it.amount }
        val remainingBudget = wholeBudget.subtract(totalSpent)

        val plannedPeriodDays =
            ChronoUnit.DAYS.between(settings.startDate, settings.getPeriodEndDate()).toInt() + 1

        val dailyBudget = if (wholeBudget > BigDecimal.ZERO && plannedPeriodDays > 0) {
            wholeBudget.divide(BigDecimal(plannedPeriodDays), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val extraAffordableDays =
            if (earlyFinishActive && remainingBudget > BigDecimal.ZERO && dailyBudget > BigDecimal.ZERO) {
                remainingBudget.divide(dailyBudget, 0, RoundingMode.DOWN).toInt().coerceAtLeast(0)
            } else 0

        val daysRemaining = ChronoUnit.DAYS.between(today, endDate).coerceAtLeast(0).toInt()
        val progress = if (wholeBudget > BigDecimal.ZERO) {
            totalSpent.divide(wholeBudget, 4, RoundingMode.HALF_UP).toFloat()
        } else 0f
        val isOverBudget = totalSpent > wholeBudget
        val totalSpentToday = actualSpends.filter { tx -> tx.date?.toLocalDate() == today }
            .fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }

        val allocations = computeDynamicAllocations(
            totalBudget = wholeBudget,
            totalSpentInPeriod = totalSpent,
            totalSpentToday = totalSpentToday,
            daysRemaining = daysRemaining,
        )

        val displayBudgetState = BudgetState(
            remainingToday = remainingBudget.coerceAtLeast(BigDecimal.ZERO),
            totalSpentToday = totalSpentToday,
            dailyBudget = dailyBudget,
            daysRemaining = daysRemaining,
            progress = progress,
            isOverBudget = isOverBudget,
            totalBudget = wholeBudget,
            totalSpentInPeriod = totalSpent,
            dailyAllocation = allocations.dailyAllocation,
            weeklyAllocation = allocations.weeklyAllocation,
            biweeklyAllocation = allocations.biweeklyAllocation,
            monthlyAllocation = allocations.monthlyAllocation,
            isTodayOverDailyAllocation = allocations.isTodayOverDailyAllocation,
        )

        val shouldShowRolloverStyle = settings.rollOverLimit?.let { it > BigDecimal.ZERO } == true

        val creditOwed = allTransactions.filter { it.isCredit && !it.isDeleted && !it.isCreditPaid }
            .sumOf { it.amount }
        val creditTransactions =
            allTransactions.filter { it.isCredit && !it.isDeleted && !it.isCreditPaid }
                .sortedByDescending { it.date }
        val debtAdjustedBalance = remainingBudget.subtract(creditOwed)

        return AnalyticsState(
            periodFinished = periodFinished,
            transactions = actualSpends,
            spends = actualSpends,
            recurringInPeriod = (paidRecurring + upcomingRecurring).distinctBy { it.id },
            oneTimeSpends = oneTimeSpends,
            wholeBudget = wholeBudget,
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

    fun seedFakeData() {
        viewModelScope.launch {
            budgetRepository.seedArchivedData()
        }
    }

    fun onMarkCreditPaid() {
        val settings = _uiState.value.budgetSettings ?: return
        val cutoffDay = settings.creditCardCutoffDay ?: 15 // Fallback if not set
        val today = LocalDate.now()

        // Use the same cycle logic as the reminder
        val cutoffThisMonth = runCatching { today.withDayOfMonth(cutoffDay) }.getOrElse {
            today.withDayOfMonth(today.lengthOfMonth())
        }

        val cycle = if (today.isAfter(cutoffThisMonth)) {
            val cutoffNextMonth =
                runCatching { today.plusMonths(1).withDayOfMonth(cutoffDay) }.getOrElse {
                    today.plusMonths(1).withDayOfMonth(today.plusMonths(1).lengthOfMonth())
                }
            cutoffThisMonth to cutoffNextMonth
        } else {
            val cutoffLastMonth =
                runCatching { today.minusMonths(1).withDayOfMonth(cutoffDay) }.getOrElse {
                    today.minusMonths(1).withDayOfMonth(today.minusMonths(1).lengthOfMonth())
                }
            cutoffLastMonth to cutoffThisMonth
        }

        viewModelScope.launch {
            budgetRepository.markCreditTransactionsAsPaid(cycle.first, cycle.second)
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
