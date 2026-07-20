package com.serranoie.app.minus.presentation.ui.analytics

import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.SavingsPreferences
import com.serranoie.app.minus.domain.model.SavingsSplitPreset
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.model.ArchivedBudget
import com.serranoie.app.minus.domain.time.LAST_PERIOD_END_KEY
import com.serranoie.app.minus.domain.time.REMAINING_FROM_LAST_PERIOD_KEY
import com.serranoie.app.minus.domain.usecase.ClearEarlyFinishStateUseCase
import com.serranoie.app.minus.domain.usecase.ObserveCurrentPeriodBoundaryUseCase
import com.serranoie.app.minus.presentation.EARLY_FINISH_ACTIVE_KEY
import com.serranoie.app.minus.presentation.EARLY_FINISH_ACTUAL_DATE_KEY
import com.serranoie.app.minus.presentation.EARLY_FINISH_ORIGINAL_END_DATE_KEY
import com.serranoie.app.minus.presentation.SAVINGS_GOAL_AMOUNT_KEY
import com.serranoie.app.minus.presentation.SAVINGS_GOAL_MONTHS_KEY
import com.serranoie.app.minus.presentation.SAVINGS_NEEDS_PCT_KEY
import com.serranoie.app.minus.presentation.SAVINGS_PRESET_KEY
import com.serranoie.app.minus.presentation.SAVINGS_SAVINGS_PCT_KEY
import com.serranoie.app.minus.presentation.SAVINGS_WANTS_PCT_KEY
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.computeDynamicAllocations
import com.serranoie.app.minus.presentation.ui.history.calculateNextChargeDate
import com.serranoie.app.minus.presentation.ui.history.getRecurringChargesInPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.inject.Inject

data class AnalyticsUiState(
    val isLoading: Boolean = false,
    val budgetSettings: BudgetSettings? = null,
    val budgetState: BudgetState? = null,
    val allTransactions: List<Transaction> = emptyList(),
    val currentPeriodId: Long = 0L,
    val selectedPeriodId: Long? = null,
    val archivedBudgets: List<ArchivedBudget> = emptyList(),
    val displayState: AnalyticsState = AnalyticsState(),
)

sealed interface AnalyticsUiEffect {
    data object NavigateToMainWithWallet : AnalyticsUiEffect
    data object NavigateToMain : AnalyticsUiEffect
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val observeCurrentPeriodBoundaryUseCase: ObserveCurrentPeriodBoundaryUseCase,
    private val clearEarlyFinishStateUseCase: ClearEarlyFinishStateUseCase,
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
                budgetRepository.getBudgetSettings(),
                budgetRepository.getTransactions(),
                budgetRepository.getArchivedBudgets(),
                observeCurrentPeriodBoundaryUseCase(),
            ) { settings, transactions, archives, periodBoundary ->
                val currentPeriodId = periodBoundary.second
                _uiState.value = _uiState.value.copy(
                    budgetSettings = settings,
                    allTransactions = transactions,
                    archivedBudgets = archives,
                    currentPeriodId = currentPeriodId,
                    displayState = if (_uiState.value.selectedPeriodId != null && _uiState.value.selectedPeriodId != currentPeriodId) {
                        buildHistoricalDisplayState(_uiState.value.selectedPeriodId!!, transactions, archives)
                    } else {
                        buildDisplayState(settings, transactions, currentPeriodId)
                    },
                )
            }.collect {}
        }
    }

    private fun buildHistoricalDisplayState(
        periodId: Long,
        allTransactions: List<Transaction>,
        archives: List<ArchivedBudget>
    ): AnalyticsState {
        val archive = archives.find { it.periodId == periodId } ?: return buildDisplayState(_uiState.value.budgetSettings, allTransactions, _uiState.value.currentPeriodId)

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
            remainingToday = archive.totalBudget.subtract(totalSpent).coerceAtLeast(BigDecimal.ZERO),
            totalSpentToday = BigDecimal.ZERO,
            dailyBudget = archive.totalBudget.divide(BigDecimal(ChronoUnit.DAYS.between(archive.startDate, archive.endDate).toInt() + 1), 2, RoundingMode.HALF_UP),
            daysRemaining = 0,
            progress = (totalSpent.divide(archive.totalBudget, 4, RoundingMode.HALF_UP).toFloat()).coerceIn(0f, 1f),
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
            startPeriodDate = Date.from(archive.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
            finishPeriodDate = Date.from(archive.endDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
            budgetSettingsForDisplay = budgetSettings,
            budgetStateForDisplay = budgetState,
            isHistoricalView = true
        )
    }

    private fun buildDisplayState(
        settings: BudgetSettings?,
        allTransactions: List<Transaction>,
        currentPeriodId: Long,
    ): AnalyticsState {
        if (settings == null) return AnalyticsState()

        val prefs = _prefsSnapshot
        val today = LocalDate.now()
        val endDate = settings.getPeriodEndDate()

        val lastPeriodEnd = prefs?.get(LAST_PERIOD_END_KEY)?.let {
            Date(it).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        }
        val remainingFromLastPeriod =
            prefs?.get(REMAINING_FROM_LAST_PERIOD_KEY)?.toBigDecimalOrNull()

        val shouldShowEndedSnapshot =
            lastPeriodEnd != null && remainingFromLastPeriod != null && !lastPeriodEnd.isBefore(
                settings.startDate
            )

        val endedPeriodStartDate = if (shouldShowEndedSnapshot) {
            val currentEnd = settings.getPeriodEndDate()
            val currentDays = ChronoUnit.DAYS.between(settings.startDate, currentEnd).toInt() + 1
            lastPeriodEnd.minusDays((currentDays - 1).toLong())
        } else null

        val transactions = filterTransactions(
            allTransactions = allTransactions,
            currentPeriodId = currentPeriodId,
            settings = settings,
            shouldShowEndedSnapshot = shouldShowEndedSnapshot,
            endedPeriodStartDate = endedPeriodStartDate,
            lastPeriodEnd = lastPeriodEnd,
        )

        val (paidRecurring, upcomingRecurring, oneTimeSpends) = splitRecurringAndOneTime(
            allTransactions = allTransactions,
            filteredTransactions = transactions,
            periodStart = settings.startDate,
            periodEnd = settings.getPeriodEndDate(),
            today = today,
        )

        val actualSpends = (oneTimeSpends + paidRecurring).distinctBy { it.id }

        val startDate = if (shouldShowEndedSnapshot && endedPeriodStartDate != null) {
            Date.from(
                endedPeriodStartDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()
            )
        } else {
            settings.startDate.atStartOfDay().atZone(ZoneId.systemDefault())
                .let { Date.from(it.toInstant()) }
        }

        val plannedFinishDate = if (shouldShowEndedSnapshot && lastPeriodEnd != null) {
            Date.from(lastPeriodEnd.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        } else {
            settings.getPeriodEndDate().atStartOfDay().atZone(ZoneId.systemDefault())
                .let { Date.from(it.toInstant()) }
        }

        val earlyFinishActive = prefs?.get(EARLY_FINISH_ACTIVE_KEY) ?: false
        val earlyFinishActualDate = prefs?.get(EARLY_FINISH_ACTUAL_DATE_KEY)?.let { Date(it) }
        val earlyFinishOriginalEndDate =
            prefs?.get(EARLY_FINISH_ORIGINAL_END_DATE_KEY)?.let { Date(it) }

        val savingsNeedsPct = prefs?.get(SAVINGS_NEEDS_PCT_KEY)
            ?: SavingsPreferences.DEFAULT_NEEDS_PCT
        val savingsWantsPct = prefs?.get(SAVINGS_WANTS_PCT_KEY)
            ?: SavingsPreferences.DEFAULT_WANTS_PCT
        val savingsSavingsPct = prefs?.get(SAVINGS_SAVINGS_PCT_KEY)
            ?: SavingsPreferences.DEFAULT_SAVINGS_PCT
        val savingsPreset = prefs?.get(SAVINGS_PRESET_KEY)?.let { name ->
            runCatching { SavingsSplitPreset.valueOf(name) }
                .getOrElse { SavingsSplitPreset.fromValues(savingsNeedsPct, savingsWantsPct, savingsSavingsPct) }
        } ?: SavingsSplitPreset.fromValues(savingsNeedsPct, savingsWantsPct, savingsSavingsPct)
        val savingsPreferences = SavingsPreferences(
            preset = savingsPreset,
            needsPct = savingsNeedsPct,
            wantsPct = savingsWantsPct,
            savingsPct = savingsSavingsPct,
            savingsGoalAmount = prefs?.get(SAVINGS_GOAL_AMOUNT_KEY)?.toBigDecimalOrNull(),
            savingsGoalMonths = prefs?.get(SAVINGS_GOAL_MONTHS_KEY),
        )

        val periodFinishedNaturally =
            settings.getPeriodEndDate().isBefore(today) || settings.getPeriodEndDate()
                .isEqual(today)
        val periodFinished = periodFinishedNaturally || earlyFinishActive

        val wholeBudget = if (shouldShowEndedSnapshot && remainingFromLastPeriod != null) {
            val spent = actualSpends.sumOf { it.amount }
            spent.add(remainingFromLastPeriod)
        } else {
            settings.totalBudget
        }

        val totalSpent = actualSpends.sumOf { it.amount }
        val remainingBudget = wholeBudget.subtract(totalSpent)

        val plannedPeriodDays =
            if (shouldShowEndedSnapshot && endedPeriodStartDate != null && lastPeriodEnd != null) {
                ChronoUnit.DAYS.between(endedPeriodStartDate, lastPeriodEnd).toInt() + 1
            } else {
                ChronoUnit.DAYS.between(settings.startDate, settings.getPeriodEndDate()).toInt() + 1
            }

        val dailyBudget = if (wholeBudget > BigDecimal.ZERO && plannedPeriodDays > 0) {
            wholeBudget.divide(BigDecimal(plannedPeriodDays), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val extraAffordableDays =
            if (earlyFinishActive && remainingBudget > BigDecimal.ZERO && dailyBudget > BigDecimal.ZERO) {
                remainingBudget.divide(dailyBudget, 0, RoundingMode.DOWN).toInt().coerceAtLeast(0)
            } else 0

        val displaySettings = settings.copy(
            totalBudget = if (shouldShowEndedSnapshot && remainingFromLastPeriod != null) {
                wholeBudget.subtract(remainingFromLastPeriod)
            } else settings.totalBudget,
            rollOverLimit = if (shouldShowEndedSnapshot) remainingFromLastPeriod else settings.rollOverLimit,
        )

        val daysRemaining = ChronoUnit.DAYS.between(today, endDate).coerceAtLeast(0).toInt()
        val progress = if (wholeBudget > BigDecimal.ZERO) {
            totalSpent.divide(wholeBudget, 4, RoundingMode.HALF_UP).toFloat()
        } else 0f
        val isOverBudget = totalSpent > wholeBudget
        val totalSpentToday =
            actualSpends.filter { tx -> tx.date?.toLocalDate() == today }
                .fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val remainingToday = totalSpentToday

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

        val shouldShowRolloverStyle =
            !shouldShowEndedSnapshot && displaySettings.rollOverLimit?.let { it > BigDecimal.ZERO } == true

        val creditOwed = allTransactions
            .filter { it.isCredit && !it.isDeleted && !it.isCreditPaid }
            .sumOf { it.amount }
        val creditTransactions = allTransactions
            .filter { it.isCredit && !it.isDeleted && !it.isCreditPaid }
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
            budgetSettingsForDisplay = displaySettings,
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

    private var _prefsSnapshot: Preferences? = null

    fun updatePrefsSnapshot(prefs: Preferences) {
        _prefsSnapshot = prefs
        val current = _uiState.value
        _uiState.value = current.copy(
            displayState = buildDisplayState(
                current.budgetSettings,
                current.allTransactions,
                current.currentPeriodId,
            )
        )
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
            val cutoffNextMonth = runCatching { today.plusMonths(1).withDayOfMonth(cutoffDay) }.getOrElse {
                today.plusMonths(1).withDayOfMonth(today.plusMonths(1).lengthOfMonth())
            }
            cutoffThisMonth to cutoffNextMonth
        } else {
            val cutoffLastMonth = runCatching { today.minusMonths(1).withDayOfMonth(cutoffDay) }.getOrElse {
                today.minusMonths(1).withDayOfMonth(today.minusMonths(1).lengthOfMonth())
            }
            cutoffLastMonth to cutoffThisMonth
        }

        viewModelScope.launch {
            budgetRepository.markCreditTransactionsAsPaid(cycle.first, cycle.second)
        }
    }

    fun onCutoffDayChanged(day: Int) {
        val currentSettings = _uiState.value.budgetSettings ?: return
        if (day !in 1..31) return

        viewModelScope.launch {
            budgetRepository.saveBudgetSettings(
                currentSettings.copy(creditCardCutoffDay = day)
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
