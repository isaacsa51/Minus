package com.serranoie.app.minus.presentation.ui.analytics

import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.time.LAST_PERIOD_END_KEY
import com.serranoie.app.minus.domain.time.REMAINING_FROM_LAST_PERIOD_KEY
import com.serranoie.app.minus.domain.usecase.ClearEarlyFinishStateUseCase
import com.serranoie.app.minus.domain.usecase.ObserveCurrentPeriodBoundaryUseCase
import com.serranoie.app.minus.presentation.EARLY_FINISH_ACTIVE_KEY
import com.serranoie.app.minus.presentation.EARLY_FINISH_ACTUAL_DATE_KEY
import com.serranoie.app.minus.presentation.EARLY_FINISH_ORIGINAL_END_DATE_KEY
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
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
    }

    private fun observeBudgetData() {
        viewModelScope.launch {
            combine(
                budgetRepository.getBudgetSettings(),
                budgetRepository.getTransactions(),
                observeCurrentPeriodBoundaryUseCase(),
            ) { settings, transactions, periodBoundary ->
                Triple(settings, transactions, periodBoundary)
            }.collect { (settings, transactions, periodBoundary) ->
                val currentPeriodId = periodBoundary.second
                _uiState.value = _uiState.value.copy(
                    budgetSettings = settings,
                    allTransactions = transactions,
                    currentPeriodId = currentPeriodId,
                    displayState = buildDisplayState(settings, transactions, currentPeriodId),
                )
            }
        }
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
        val remainingFromLastPeriod = prefs?.get(REMAINING_FROM_LAST_PERIOD_KEY)?.toBigDecimalOrNull()

        val shouldShowEndedSnapshot = lastPeriodEnd != null &&
            remainingFromLastPeriod != null &&
            !lastPeriodEnd.isBefore(settings.startDate)

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

        val startDate = if (shouldShowEndedSnapshot && endedPeriodStartDate != null) {
            Date.from(endedPeriodStartDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        } else {
            settings.startDate.atStartOfDay().atZone(ZoneId.systemDefault()).let { Date.from(it.toInstant()) }
        }

        val plannedFinishDate = if (shouldShowEndedSnapshot && lastPeriodEnd != null) {
            Date.from(lastPeriodEnd.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        } else {
            settings.getPeriodEndDate().atStartOfDay().atZone(ZoneId.systemDefault()).let { Date.from(it.toInstant()) }
        }

        val earlyFinishActive = prefs?.get(EARLY_FINISH_ACTIVE_KEY) ?: false
        val earlyFinishActualDate = prefs?.get(EARLY_FINISH_ACTUAL_DATE_KEY)?.let { Date(it) }
        val earlyFinishOriginalEndDate = prefs?.get(EARLY_FINISH_ORIGINAL_END_DATE_KEY)?.let { Date(it) }

        val periodFinishedNaturally = settings.getPeriodEndDate().isBefore(today) ||
            settings.getPeriodEndDate().isEqual(today)
        val periodFinished = periodFinishedNaturally || earlyFinishActive

        val wholeBudget = if (shouldShowEndedSnapshot && remainingFromLastPeriod != null) {
            val spent = transactions.filter { !it.isDeleted }.sumOf { it.amount }
            spent.add(remainingFromLastPeriod)
        } else {
            settings.totalBudget
        }

        val totalSpent = transactions.filter { !it.isDeleted }.sumOf { it.amount }
        val remainingBudget = wholeBudget.subtract(totalSpent)

        val plannedPeriodDays = if (shouldShowEndedSnapshot && endedPeriodStartDate != null && lastPeriodEnd != null) {
            ChronoUnit.DAYS.between(endedPeriodStartDate, lastPeriodEnd).toInt() + 1
        } else {
            ChronoUnit.DAYS.between(settings.startDate, settings.getPeriodEndDate()).toInt() + 1
        }

        val dailyBudget = if (wholeBudget > BigDecimal.ZERO && plannedPeriodDays > 0) {
            wholeBudget.divide(BigDecimal(plannedPeriodDays), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val extraAffordableDays = if (earlyFinishActive && remainingBudget > BigDecimal.ZERO && dailyBudget > BigDecimal.ZERO) {
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
            totalSpent.divide(wholeBudget, 4, RoundingMode.HALF_UP)
                .toFloat()
        } else 0f
        val isOverBudget = totalSpent > wholeBudget
        val totalSpentToday = transactions
            .filter { tx -> !tx.isDeleted && tx.date?.toLocalDate() == today }
            .fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
        val remainingToday = totalSpentToday

        val displayBudgetState = BudgetState(
            remainingToday = remainingBudget.coerceAtLeast(BigDecimal.ZERO),
            totalSpentToday = totalSpentToday,
            dailyBudget = dailyBudget,
            daysRemaining = daysRemaining,
            progress = progress,
            isOverBudget = isOverBudget,
            totalBudget = wholeBudget,
            totalSpentInPeriod = totalSpent,
        )

        val shouldShowRolloverStyle = !shouldShowEndedSnapshot &&
            displaySettings.rollOverLimit?.let { it > BigDecimal.ZERO } == true

        return AnalyticsState(
            periodFinished = periodFinished,
            transactions = transactions,
            spends = transactions,
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
        )
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
        _effects.value = AnalyticsUiEffect.NavigateToMain
    }

    fun consumeEffect() {
        _effects.value = null
    }
}
