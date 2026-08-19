package com.serranoie.app.minus.presentation.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.ArchivedBudget
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.model.UserSettings
import com.serranoie.app.minus.domain.usecase.ClearEarlyFinishStateUseCase
import com.serranoie.app.minus.domain.usecase.ObserveCurrentPeriodBoundaryUseCase
import com.serranoie.app.minus.domain.usecase.PersistBudgetSettingsUseCase
import com.serranoie.app.minus.presentation.ui.budget.BudgetStateCalculator
import com.serranoie.app.minus.presentation.ui.history.splitRecurringAndOneTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import logcat.logcat
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val budgetSettings: BudgetSettings? = null,
    val budgetState: BudgetState? = null,
    val allTransactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val currentPeriodId: Long = 0L,
    val selectedPeriodId: Long? = null,
    val archivedBudgets: List<ArchivedBudget> = emptyList(),
    val displayState: AnalyticsState = AnalyticsState(),
    val userSettings: UserSettings = UserSettings.DEFAULT,
    val rolloverAmount: BigDecimal = BigDecimal.ZERO,
    val rolloverCarryForward: Boolean = false,
    val graphGranularity: GraphGranularity = GraphGranularity.DAYS,
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

    private val _selectedPeriodId = MutableStateFlow<Long?>(null)
    private val _granularity = MutableStateFlow(GraphGranularity.DAYS)

    val uiState: StateFlow<AnalyticsUiState> = combine(
        budgetRepository.getBudgetSettings().distinctUntilChanged(),
        budgetRepository.getTransactions().distinctUntilChanged(),
        budgetRepository.getActiveCategories().distinctUntilChanged(),
        budgetRepository.getArchivedBudgets().distinctUntilChanged(),
        observeCurrentPeriodBoundaryUseCase().distinctUntilChanged(),
        settingsRepository.observeSettings().distinctUntilChanged(),
        settingsRepository.observeCurrentPeriodRollover().distinctUntilChanged(),
        _granularity,
        _selectedPeriodId,
        budgetRepository.getAllCategories().distinctUntilChanged(),
        budgetRepository.getPaidRecurrentOccurrences().distinctUntilChanged(),
    ) { args: Array<Any?> ->
        val settings = args[0] as BudgetSettings?
        val transactions = args[1] as List<Transaction>
        val categories = args[2] as List<Category>
        val archives = args[3] as List<ArchivedBudget>
        val periodBoundary = args[4] as Pair<Long, Long>
        val userSettings = args[5] as UserSettings
        val rollover = args[6] as Pair<BigDecimal, Boolean>
        val granularity = args[7] as GraphGranularity
        val selectedPeriodId = args[8] as Long?
        val allCategories = args[9] as List<Category>
        val paidOccurrences = args[10] as Set<PaidRecurrentOccurrence>

        val currentPeriodId = periodBoundary.second
        val reconstructedArchives = reconstructHistory(transactions, archives, settings, paidOccurrences)
        val allArchives = (archives + reconstructedArchives).distinctBy { it.periodId }
            .sortedByDescending { it.startDate }

        val displayState = if (selectedPeriodId != null && selectedPeriodId != currentPeriodId) {
            buildHistoricalDisplayState(
                periodId = selectedPeriodId,
                allTransactions = transactions,
                archives = allArchives,
                granularity = granularity,
                categories = allCategories,
                currentSettings = settings,
                currentPeriodId = currentPeriodId,
                userSettings = userSettings,
                paidOccurrences = paidOccurrences,
            )
        } else {
            buildDisplayState(
                settings = settings,
                allTransactions = transactions,
                currentPeriodId = currentPeriodId,
                userSettings = userSettings,
                granularity = granularity,
                archives = allArchives,
                categories = allCategories,
                paidOccurrences = paidOccurrences,
            )
        }

        AnalyticsUiState(
            isLoading = false,
            budgetSettings = settings,
            allTransactions = transactions,
            categories = categories,
            archivedBudgets = allArchives,
            currentPeriodId = currentPeriodId,
            selectedPeriodId = selectedPeriodId,
            userSettings = userSettings,
            rolloverAmount = rollover.first,
            rolloverCarryForward = rollover.second,
            graphGranularity = granularity,
            displayState = displayState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AnalyticsUiState()
    )

    private val _effects = MutableStateFlow<AnalyticsUiEffect?>(null)
    val effects: StateFlow<AnalyticsUiEffect?> = _effects.asStateFlow()

    init {
        viewModelScope.launch {
            val demoIds = listOf(20260501L, 20260601L, 20260708L, 20260615L)
            demoIds.forEach { budgetRepository.deleteArchivedBudget(it) }
        }
    }

    private fun reconstructHistory(
        allTransactions: List<Transaction>,
        existingArchives: List<ArchivedBudget>,
        currentSettings: BudgetSettings?,
        paidOccurrences: Set<PaidRecurrentOccurrence>,
    ): List<ArchivedBudget> {
        if (currentSettings == null) return emptyList()

        val orphaned = allTransactions.filter { it.periodId <= 0L && !it.isDeleted }
        if (orphaned.isEmpty()) return emptyList()

        val currentPeriodStart = currentSettings.startDate
        val currentPeriodEnd = currentSettings.getPeriodEndDate()
        val dailyBudget = currentSettings.calculateDailyBudget()

        return orphaned
            .filter { tx ->
                val date = tx.date?.toLocalDate() ?: return@filter false
                date.isBefore(currentPeriodStart) || date.isAfter(currentPeriodEnd)
            }
            .groupBy { tx ->
                val date = tx.date?.toLocalDate() ?: return@groupBy "0-0"
                "${date.year}-${date.monthValue}"
            }
            .mapNotNull { (key, txs) ->
                val parts = key.split("-")
                if (parts.size < 2) return@mapNotNull null
                val year = parts[0].toInt()
                val month = parts[1].toInt()

                val actualStartDate = txs.mapNotNull { it.date?.toLocalDate() }.minOrNull()
                    ?: LocalDate.of(year, month, 1)
                val actualEndDate = txs.mapNotNull { it.date?.toLocalDate() }.maxOrNull()
                    ?: actualStartDate.plusMonths(1).minusDays(1)

                val hasOverlap = existingArchives.any { existing ->
                    val eStart = existing.startDate
                    val eEnd = existing.endDate
                    actualStartDate <= eEnd && actualEndDate >= eStart
                }

                if (hasOverlap) return@mapNotNull null

                val virtualId = -(year.toLong() * 100 + month.toLong())
                val daysInMonth = java.time.temporal.ChronoUnit.DAYS.between(
                    actualStartDate,
                    actualEndDate.plusDays(1)
                )

                val periodTransactions = allTransactions.filter { tx ->
                    val date = tx.date?.toLocalDate() ?: return@filter false
                    !date.isBefore(actualStartDate) && !date.isAfter(actualEndDate) && !tx.isDeleted
                }
                val (paidRecurring, _, oneTimeSpends) = splitRecurringAndOneTime(
                    allTransactions = allTransactions,
                    filteredTransactions = periodTransactions,
                    periodStart = actualStartDate,
                    periodEnd = actualEndDate,
                    today = actualEndDate,
                    paidOccurrences = paidOccurrences,
                )
                val spentAmount = (oneTimeSpends + paidRecurring).distinctBy { it.id }
                    .sumOf { it.amount }

                ArchivedBudget(
                    periodId = virtualId,
                    totalBudget = dailyBudget.multiply(BigDecimal(daysInMonth)),
                    spentAmount = spentAmount,
                    startDate = actualStartDate,
                    endDate = actualEndDate,
                    currencyCode = currentSettings.currencyCode,
                    periodType = com.serranoie.app.minus.domain.model.BudgetPeriod.MONTHLY,
                    createdAt = System.currentTimeMillis()
                )
            }
    }

    private fun buildHistoricalDisplayState(
        periodId: Long,
        allTransactions: List<Transaction>,
        archives: List<ArchivedBudget>,
        granularity: GraphGranularity,
        categories: List<Category>,
        currentSettings: BudgetSettings?,
        currentPeriodId: Long,
        userSettings: UserSettings,
        paidOccurrences: Set<PaidRecurrentOccurrence>,
    ): AnalyticsState {
        val archive = archives.find { it.periodId == periodId } ?: return buildDisplayState(
            settings = currentSettings,
            allTransactions = allTransactions,
            currentPeriodId = currentPeriodId,
            userSettings = userSettings,
            granularity = granularity,
            archives = archives,
            categories = categories,
            paidOccurrences = paidOccurrences,
        )

        val transactions = findTransactionsForArchive(archive, allTransactions, periodId)
        val (paidRecurring, upcomingRecurring, oneTimeSpends) = splitRecurringAndOneTime(
            allTransactions = allTransactions,
            filteredTransactions = transactions,
            periodStart = archive.startDate,
            periodEnd = archive.endDate,
            today = archive.endDate,
            paidOccurrences = paidOccurrences,
        )

        val actualSpends = (oneTimeSpends + paidRecurring).distinctBy { it.id }
        val budgetSettings = archive.toBudgetSettings()

        val budgetState = budgetStateCalculator.calculateBudgetState(
            settings = budgetSettings,
            transactions = transactions,
            currentDate = archive.endDate
        )

        val previousTransactions = findPreviousPeriodTransactions(
            allTransactions = allTransactions,
            archives = archives,
            currentStartDate = archive.startDate
        )

        return AnalyticsState(
            periodFinished = true,
            transactions = actualSpends,
            allTransactions = allTransactions,
            spends = actualSpends,
            recurringInPeriod = (paidRecurring + upcomingRecurring).distinctBy { it.id },
            oneTimeSpends = oneTimeSpends,
            wholeBudget = archive.totalBudget,
            currencyCode = archive.currencyCode,
            startPeriodDate = archive.startDate.toDate(),
            finishPeriodDate = archive.endDate.toDate(),
            budgetSettingsForDisplay = budgetSettings,
            budgetStateForDisplay = budgetState,
            isHistoricalView = true,
            previousPeriodTransactions = previousTransactions,
            categories = categories,
            graphGranularity = granularity
        )
    }

    private fun findTransactionsForArchive(
        archive: ArchivedBudget,
        allTransactions: List<Transaction>,
        periodId: Long
    ): List<Transaction> {
        return if (periodId < 0L) {
            allTransactions.filter { tx ->
                val date = tx.date?.toLocalDate() ?: return@filter false
                !date.isBefore(archive.startDate) && !date.isAfter(archive.endDate) && !tx.isDeleted
            }
        } else {
            allTransactions.filter { it.periodId == periodId && !it.isDeleted }
        }
    }

    private fun ArchivedBudget.toBudgetSettings(): BudgetSettings {
        return BudgetSettings(
            totalBudget = totalBudget,
            period = periodType,
            startDate = startDate,
            endDate = endDate,
            currencyCode = currencyCode
        )
    }

    private fun buildDisplayState(
        settings: BudgetSettings?,
        allTransactions: List<Transaction>,
        currentPeriodId: Long,
        userSettings: UserSettings,
        granularity: GraphGranularity,
        archives: List<ArchivedBudget>,
        categories: List<Category>,
        paidOccurrences: Set<PaidRecurrentOccurrence>,
    ): AnalyticsState {
        if (settings == null) return AnalyticsState(isLoading = false, graphGranularity = granularity, categories = categories)

        val today = LocalDate.now()

        val transactions = filterTransactions(
            allTransactions = allTransactions,
            currentPeriodId = currentPeriodId,
            settings = settings,
            shouldShowEndedSnapshot = false,
            endedPeriodStartDate = null,
            lastPeriodEnd = null,
        )

        val previousTransactions = findPreviousPeriodTransactions(
            allTransactions = allTransactions,
            archives = archives,
            currentStartDate = settings.startDate
        )

        val (paidRecurring, upcomingRecurring, oneTimeSpends) = splitRecurringAndOneTime(
            allTransactions = allTransactions,
            filteredTransactions = transactions,
            periodStart = settings.startDate,
            periodEnd = settings.getPeriodEndDate(),
            today = today,
            paidOccurrences = paidOccurrences,
        )

        val displayBudgetState = budgetStateCalculator.calculateBudgetState(
            settings = settings,
            transactions = transactions,
            currentDate = today
        )

        val earlyFinishActive = userSettings.earlyFinishActive
        val earlyFinishInfo = calculateEarlyFinishStats(
            userSettings = userSettings,
            remainingBudget = displayBudgetState.remainingToday,
            dailyBudget = displayBudgetState.dailyBudget
        )

        val creditInfo = calculateCreditAnalytics(allTransactions, displayBudgetState.remainingToday)

        return AnalyticsState(
            periodFinished = settings.getPeriodEndDate().isBefore(today) || earlyFinishActive,
            transactions = transactions,
            allTransactions = allTransactions,
            spends = transactions,
            recurringInPeriod = (paidRecurring + upcomingRecurring).distinctBy { it.id },
            oneTimeSpends = oneTimeSpends,
            wholeBudget = displayBudgetState.totalBudget,
            currencyCode = settings.currencyCode,
            finishPeriodActualDate = earlyFinishInfo.actualDate,
            startPeriodDate = settings.startDate.toDate(),
            finishPeriodDate = if (earlyFinishActive) earlyFinishInfo.originalEndDate else settings.getPeriodEndDate().toDate(),
            extraAffordableDaysFromRemaining = earlyFinishInfo.extraAffordableDays,
            budgetSettingsForDisplay = settings,
            budgetStateForDisplay = displayBudgetState,
            showRolloverStyleInBudgetDisplay = displayBudgetState.totalBudget > settings.totalBudget,
            isLoading = false,
            savingsPreferences = userSettings.savingsPreferences,
            creditOwed = creditInfo.owed,
            debtAdjustedBalance = creditInfo.debtAdjustedBalance,
            creditTransactions = creditInfo.transactions,
            userSettings = userSettings,
            previousPeriodTransactions = previousTransactions,
            categories = categories,
            graphGranularity = granularity,
        )
    }

    private fun findPreviousPeriodTransactions(
        allTransactions: List<Transaction>,
        archives: List<ArchivedBudget>,
        currentStartDate: LocalDate
    ): List<Transaction> {
        val previousArchive = archives.firstOrNull { it.startDate.isBefore(currentStartDate) }
        return if (previousArchive != null) {
            allTransactions.filter { tx ->
                val date = tx.date?.toLocalDate() ?: return@filter false
                !date.isBefore(previousArchive.startDate) && !date.isAfter(previousArchive.endDate) && !tx.isDeleted
            }
        } else emptyList()
    }

    private data class EarlyFinishStats(
        val actualDate: Date?,
        val originalEndDate: Date?,
        val extraAffordableDays: Int
    )

    private fun calculateEarlyFinishStats(
        userSettings: UserSettings,
        remainingBudget: BigDecimal,
        dailyBudget: BigDecimal
    ): EarlyFinishStats {
        val earlyFinishActive = userSettings.earlyFinishActive
        val extraAffordableDays = if (earlyFinishActive && remainingBudget > BigDecimal.ZERO && dailyBudget > BigDecimal.ZERO) {
            remainingBudget.divide(dailyBudget, 0, RoundingMode.DOWN).toInt().coerceAtLeast(0)
        } else 0

        return EarlyFinishStats(
            actualDate = if (userSettings.earlyFinishActualDate > 0) Date(userSettings.earlyFinishActualDate) else null,
            originalEndDate = if (userSettings.earlyFinishOriginalEndDate > 0) Date(userSettings.earlyFinishOriginalEndDate) else null,
            extraAffordableDays = extraAffordableDays
        )
    }

    private data class CreditAnalytics(
        val owed: BigDecimal,
        val transactions: List<Transaction>,
        val debtAdjustedBalance: BigDecimal
    )

    private fun calculateCreditAnalytics(
        allTransactions: List<Transaction>,
        remainingBudget: BigDecimal
    ): CreditAnalytics {
        val unpaidCredit = allTransactions.filter { it.isCredit && !it.isDeleted && !it.isCreditPaid }
        val creditOwed = unpaidCredit.sumOf { it.amount }
        val creditTransactions = unpaidCredit.sortedByDescending { it.date }
        val debtAdjustedBalance = remainingBudget.subtract(creditOwed)

        return CreditAnalytics(
            owed = creditOwed,
            transactions = creditTransactions,
            debtAdjustedBalance = debtAdjustedBalance
        )
    }

    private fun LocalDate.toDate(): Date = Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())

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
        if (_selectedPeriodId.value != null) {
            _selectedPeriodId.value = null
        } else {
            _effects.value = AnalyticsUiEffect.NavigateToMain
        }
    }

    fun onPeriodSelected(periodId: Long) {
        _selectedPeriodId.value = periodId
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
        val currentSettings = uiState.value.budgetSettings
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

    fun onTutorialCompleted(hasSpends: Boolean) {
        viewModelScope.launch {
            if (hasSpends) {
                settingsRepository.setAnalyticsSpendsTutorialCompleted(true)
            } else {
                settingsRepository.setAnalyticsTutorialCompleted(true)
            }
        }
    }

    fun onGranularityChanged(granularity: GraphGranularity) {
        _granularity.value = granularity
    }

    fun consumeEffect() {
        _effects.value = null
    }
}
