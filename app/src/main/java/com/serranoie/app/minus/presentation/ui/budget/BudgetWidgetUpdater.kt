package com.serranoie.app.minus.presentation.ui.budget

import android.content.Context
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.time.CalendarSystem
import com.serranoie.app.minus.presentation.island.IslandBudgetSnapshot
import com.serranoie.app.minus.presentation.island.IslandBudgetStore
import com.serranoie.app.minus.presentation.ui.theme.component.budget.pill.calculateBudgetMetrics
import com.serranoie.app.minus.presentation.widget.DailySpending
import com.serranoie.app.minus.presentation.widget.MonthHeatmapData
import com.serranoie.app.minus.presentation.widget.updateAverageSpendWidget
import com.serranoie.app.minus.presentation.widget.updateBudgetOverviewWidget
import com.serranoie.app.minus.presentation.widget.updateBudgetPillWidget
import com.serranoie.app.minus.presentation.widget.updateCompleteBudgetWidget
import com.serranoie.app.minus.presentation.widget.updateDaysCountdownWidget
import com.serranoie.app.minus.presentation.widget.updateExpenseWidget
import com.serranoie.app.minus.presentation.widget.updateHeatmapWidget
import com.serranoie.app.minus.presentation.widget.updateMinMaxSpentWidget
import com.serranoie.app.minus.presentation.widget.updateMonthHeatmapWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

class BudgetWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun update(baseState: BudgetUiState) {
        val currency = baseState.budgetSettings?.currencyCode ?: "USD"
        // Pushed first: unlike the others, the pill has a "set up your budget" face to show.
        updateBudgetPill(baseState, currency)

        if (baseState !== BudgetUiState.INITIAL) updateIsland(baseState, currency)

        val budget = baseState.budgetState ?: return
        val totalSpent = budget.totalSpentInPeriod.toInt()
        val totalBudget = baseState.budgetSettings?.totalBudget?.toInt() ?: 1
        val daysLeft = budget.daysRemaining
        val budgetAmount = budget.totalBudget.toInt()
        val (startDate, endDate) = resolveBudgetPeriodDates(baseState)
        val heatmapData = buildHeatmapData(baseState)
        val currentPeriodTransactions = filterCurrentPeriodTransactions(baseState)

        updateExpenseWidget(context, totalSpent, totalBudget, currency)
        updateBudgetOverviewWidget(context, budgetAmount, currency, startDate, endDate, daysLeft)
        updateCompleteBudgetWidget(context, totalSpent, budgetAmount, currency, startDate, endDate, daysLeft)
        updateDaysCountdownWidget(context, daysLeft, budget.totalBudget.toInt())
        updateHeatmapWidget(context, heatmapData.monthHeatmapData)
        updateMonthHeatmapWidget(context, heatmapData.currentMonthHeatmap, heatmapData.currentMonthTotalSpent, currency)
        updateMinMaxSpentWidget(context, currentPeriodTransactions, currency)
        updateAverageSpendWidget(context, currentPeriodTransactions, currency, startDate, endDate)
    }

    /**
     * Feeds the slim budget-pill widget the same numbers the in-app pill shows: the metrics for the
     * view mode the user selected (daily / weekly / biweekly / monthly), computed by the pill's own
     * [calculateBudgetMetrics] so the two can never drift apart.
     */
    private suspend fun updateBudgetPill(baseState: BudgetUiState, currency: String) {
        val settings = baseState.budgetSettings
        val budgetState = baseState.budgetState
        val viewPeriod = baseState.selectedViewPeriod ?: settings?.period ?: BudgetPeriod.DAILY

        if (settings == null || budgetState == null) {
            updateBudgetPillWidget(
                context = context,
                hasBudget = false,
                viewPeriod = viewPeriod,
                remaining = BigDecimal.ZERO,
                currency = currency,
                progress = 0f,
                isOverBudget = false,
                isOverPeriodAllocation = false,
                nextAllocation = null,
            )
            return
        }

        val draftAmount = baseState.numpadDraftAmount ?: BigDecimal.ZERO
        val metrics = calculateBudgetMetrics(
            state = budgetState,
            period = viewPeriod,
            splitMode = settings.splitMode,
            draftSpend = draftAmount,
        )
        updateBudgetPillWidget(
            context = context,
            hasBudget = true,
            viewPeriod = viewPeriod,
            remaining = metrics.periodRemaining,
            currency = currency,
            progress = metrics.spendProgress,
            isOverBudget = metrics.isCurrentPeriodOverBudget,
            isOverPeriodAllocation = metrics.isOverCurrentSubPeriod,
            nextAllocation = metrics.nextPeriodAllocation,
        )
    }

    /*
    * [!INFO]
    * This syncs budget period inside the island
    * from the saameraw/essentials app.
    */
    private fun updateIsland(baseState: BudgetUiState, currency: String) {
        val settings = baseState.budgetSettings
        val budgetState = baseState.budgetState
        if (settings == null || budgetState == null) {
            IslandBudgetStore.update(context, null)
            return
        }
        val viewPeriod = baseState.selectedViewPeriod ?: settings.period
        val metrics = calculateBudgetMetrics(state = budgetState, period = viewPeriod, splitMode = settings.splitMode)
        IslandBudgetStore.update(
            context,
            IslandBudgetSnapshot(
                date = LocalDate.now(),
                currency = currency,
                viewPeriod = viewPeriod,
                splitRemaining = metrics.periodRemaining,
                isOverBudget = metrics.isCurrentPeriodOverBudget,
                isOverSplit = metrics.isOverCurrentSubPeriod,
                periodLeft = budgetState.totalBudget - budgetState.totalSpentInPeriod,
                daysLeft = budgetState.daysRemaining,
                totalBudget = budgetState.totalBudget,
                settingsTotalBudget = settings.totalBudget,
                rollOverLimit = settings.rollOverLimit,
                creditOwed = baseState.creditOwed,
                startDate = settings.startDate,
                finishDate = settings.getPeriodEndDate(),
            ),
        )
    }

    private fun filterCurrentPeriodTransactions(baseState: BudgetUiState): List<Transaction> {
        val settings = baseState.budgetSettings ?: return emptyList()
        val periodEnd = settings.getPeriodEndDate()
        val currentPeriodId = baseState.currentPeriodId
        val currentPeriodStartedAtMillis = baseState.currentPeriodStartedAtMillis

        return baseState.transactions.filter { transaction ->
            if (currentPeriodId > 0L && transaction.periodId > 0L) {
                return@filter transaction.periodId == currentPeriodId
            }

            val txDate = transaction.date?.toLocalDate() ?: return@filter false
            if (txDate.isBefore(settings.startDate) || txDate.isAfter(periodEnd)) {
                return@filter false
            }

            if (txDate.isEqual(settings.startDate) && currentPeriodStartedAtMillis > 0L) {
                return@filter transaction.createdAt >= currentPeriodStartedAtMillis
            }

            true
        }
    }

    private fun resolveBudgetPeriodDates(baseState: BudgetUiState): Pair<Date, Date> {
        val startDate = baseState.budgetSettings?.startDate?.let {
            Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant())
        } ?: Date()
        val endDate = baseState.budgetSettings?.getPeriodEndDate()?.let {
            Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant())
        } ?: Date()
        return startDate to endDate
    }

    private data class WidgetHeatmapData(
        val monthHeatmapData: List<MonthHeatmapData>,
        val currentMonthHeatmap: MonthHeatmapData,
        val currentMonthTotalSpent: Int,
    )

    private fun buildHeatmapData(baseState: BudgetUiState): WidgetHeatmapData {
        val monthHeatmapData = buildHeatmapMonths(
            transactions = baseState.transactions,
            now = LocalDate.now(),
            budget = baseState.budgetSettings?.totalBudget ?: BigDecimal.ONE,
            calendar = CalendarSystem.current(),
        )
        val currentMonthHeatmap = monthHeatmapData.last()
        return WidgetHeatmapData(
            monthHeatmapData = monthHeatmapData,
            currentMonthHeatmap = currentMonthHeatmap,
            currentMonthTotalSpent = currentMonthHeatmap.days.sumOf { it.spending }.toInt(),
        )
    }
}

internal fun buildHeatmapMonths(
    transactions: List<Transaction>,
    now: LocalDate,
    budget: BigDecimal,
    calendar: CalendarSystem,
): List<MonthHeatmapData> {
    val groupedByDate = transactions
        .filter { !it.isDeleted && it.date != null }
        .groupBy { it.date!!.toLocalDate() }
    val currentMonthStart = calendar.firstDayOfMonth(now)
    return (3L downTo 0L).map { monthsBack ->
        val monthStart = calendar.plusMonths(currentMonthStart, -monthsBack)
        val days = (1..calendar.lengthOfMonth(monthStart)).map { dayOfMonth ->
            val txs = groupedByDate[monthStart.plusDays(dayOfMonth - 1L)].orEmpty()
            DailySpending(
                dayOfMonth = dayOfMonth,
                spending = txs.sumOf { it.amount },
                budget = budget,
                transactionCount = txs.size,
            )
        }
        MonthHeatmapData(monthStart = monthStart, days = days)
    }
}
