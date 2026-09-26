package com.serranoie.app.minus.presentation.ui.budget

import android.content.Context
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.Transaction
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
        val now = LocalDate.now()
        val groupedByDate = groupTransactionsForHeatmap(baseState.transactions, now)
        val defaultBudget = baseState.budgetSettings?.totalBudget ?: BigDecimal.ONE
        val monthHeatmapData = buildMultiMonthHeatmapData(now, groupedByDate, defaultBudget)
        val (currentMonthHeatmap, currentMonthTotalSpent) =
            buildCurrentMonthHeatmap(now, groupedByDate, defaultBudget)

        return WidgetHeatmapData(
            monthHeatmapData = monthHeatmapData,
            currentMonthHeatmap = currentMonthHeatmap,
            currentMonthTotalSpent = currentMonthTotalSpent,
        )
    }

    private fun groupTransactionsForHeatmap(
        transactions: List<Transaction>,
        now: LocalDate,
    ): Map<LocalDate, List<Transaction>> {
        val startMonth = now.minusMonths(3).withDayOfMonth(1)
        val endMonth = now.withDayOfMonth(now.lengthOfMonth())
        return transactions
            .mapNotNull { tx ->
                val date = tx.date?.toLocalDate() ?: return@mapNotNull null
                if (tx.isDeleted || date.isBefore(startMonth) || date.isAfter(endMonth)) {
                    return@mapNotNull null
                }
                date to tx
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second },
            )
    }

    private fun buildMultiMonthHeatmapData(
        now: LocalDate,
        groupedByDate: Map<LocalDate, List<Transaction>>,
        defaultBudget: BigDecimal,
    ): List<MonthHeatmapData> {
        return (0L..3L).map { monthOffset ->
            val month = now.minusMonths(3 - monthOffset)
            val days = (1..month.lengthOfMonth()).map { dayOfMonth ->
                val day = month.withDayOfMonth(dayOfMonth)
                val txs = groupedByDate[day].orEmpty()
                DailySpending(
                    dayOfMonth = dayOfMonth,
                    spending = txs.sumOf { it.amount },
                    budget = defaultBudget,
                    transactionCount = txs.size,
                )
            }
            MonthHeatmapData(
                year = month.year,
                month = month.monthValue,
                days = days,
            )
        }
    }

    private fun buildCurrentMonthHeatmap(
        now: LocalDate,
        groupedByDate: Map<LocalDate, List<Transaction>>,
        defaultBudget: BigDecimal,
    ): Pair<MonthHeatmapData, Int> {
        val currentMonthDays = (1..now.lengthOfMonth()).map { dayOfMonth ->
            val day = now.withDayOfMonth(dayOfMonth)
            val txs = groupedByDate[day].orEmpty()
            DailySpending(
                dayOfMonth = dayOfMonth,
                spending = txs.sumOf { it.amount },
                budget = defaultBudget,
                transactionCount = txs.size,
            )
        }

        val currentMonthTotalSpent = currentMonthDays.sumOf { it.spending }.toInt()
        val currentMonthHeatmap = MonthHeatmapData(
            year = now.year,
            month = now.monthValue,
            days = currentMonthDays,
        )
        return currentMonthHeatmap to currentMonthTotalSpent
    }
}
