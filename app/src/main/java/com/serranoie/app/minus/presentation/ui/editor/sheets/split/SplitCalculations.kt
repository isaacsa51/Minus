package com.serranoie.app.minus.presentation.ui.editor.sheets.split

import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import java.math.BigDecimal
import java.math.RoundingMode

fun BudgetPeriod.toDays(): Int = when (this) {
    BudgetPeriod.DAILY -> 1
    BudgetPeriod.WEEKLY -> 7
    BudgetPeriod.BIWEEKLY -> 14
    BudgetPeriod.MONTHLY -> 30
}

fun availablePeriodsFor(totalDays: Int): List<BudgetPeriod> = buildList {
    add(BudgetPeriod.DAILY)
    if (totalDays >= 7) add(BudgetPeriod.WEEKLY)
    if (totalDays >= 14) add(BudgetPeriod.BIWEEKLY)
    if (totalDays >= 30) add(BudgetPeriod.MONTHLY)
}

private fun budgetForPeriod(
    totalBudget: BigDecimal,
    totalDays: Int,
    period: BudgetPeriod,
): BigDecimal {
    if (totalBudget == BigDecimal.ZERO || totalDays <= 0) return BigDecimal.ZERO

    val periodDays = period.toDays()
    // coerceAtLeast(1) so a period longer than the range (e.g. a 10-day
    // budget with a MONTHLY view) returns the full budget for the single
    // "partial" period instead of throwing ArithmeticException. Mirrors the
    // guard in blocksRemaining.
    val numPeriods = (totalDays / periodDays).coerceAtLeast(1)

    return totalBudget.divide(BigDecimal(numPeriods), 2, RoundingMode.HALF_UP)
}

fun splitBudget(
	totalBudget: BigDecimal,
	totalSpent: BigDecimal,
	totalDays: Int,
	daysRemaining: Int,
	period: BudgetPeriod,
	mode: BudgetSplitMode,
): BigDecimal {
	if (totalBudget == BigDecimal.ZERO || totalDays <= 0) return BigDecimal.ZERO
	return when (mode) {
		BudgetSplitMode.STATIC ->
			budgetForPeriod(totalBudget, totalDays, period)

		BudgetSplitMode.DYNAMIC -> {
			if (daysRemaining <= 0) return BigDecimal.ZERO
			val remaining = totalBudget.subtract(totalSpent)
			if (remaining <= BigDecimal.ZERO) return BigDecimal.ZERO
			val daily = remaining.divide(BigDecimal(daysRemaining), 2, RoundingMode.HALF_UP)
			daily.multiply(BigDecimal(period.toDays()))
		}
	}
}

data class DynamicAllocations(
	val dailyAllocation: BigDecimal,
	val weeklyAllocation: BigDecimal,
	val biweeklyAllocation: BigDecimal,
	val monthlyAllocation: BigDecimal,
	val isTodayOverDailyAllocation: Boolean,
) {
	fun forPeriod(period: BudgetPeriod): BigDecimal = when (period) {
		BudgetPeriod.DAILY -> dailyAllocation
		BudgetPeriod.WEEKLY -> weeklyAllocation
		BudgetPeriod.BIWEEKLY -> biweeklyAllocation
		BudgetPeriod.MONTHLY -> monthlyAllocation
	}
}

fun computeDynamicAllocations(
	totalBudget: BigDecimal,
	totalSpentInPeriod: BigDecimal,
	totalSpentToday: BigDecimal,
	daysRemaining: Int,
): DynamicAllocations {
	if (totalBudget <= BigDecimal.ZERO || daysRemaining <= 0) {
		return DynamicAllocations(
			dailyAllocation = BigDecimal.ZERO,
			weeklyAllocation = BigDecimal.ZERO,
			biweeklyAllocation = BigDecimal.ZERO,
			monthlyAllocation = BigDecimal.ZERO,
			isTodayOverDailyAllocation = totalSpentToday > BigDecimal.ZERO,
		)
	}
	val remaining = totalBudget.subtract(totalSpentInPeriod)
	if (remaining <= BigDecimal.ZERO) {
		return DynamicAllocations(
			dailyAllocation = BigDecimal.ZERO,
			weeklyAllocation = BigDecimal.ZERO,
			biweeklyAllocation = BigDecimal.ZERO,
			monthlyAllocation = BigDecimal.ZERO,
			isTodayOverDailyAllocation = true,
		)
	}

	val daily = remaining.divide(BigDecimal(daysRemaining), 2, RoundingMode.HALF_UP)
	val weekly = remaining.divide(
		BigDecimal(blocksRemaining(daysRemaining, 7)),
		2,
		RoundingMode.HALF_UP,
	)
	val biweekly = remaining.divide(
		BigDecimal(blocksRemaining(daysRemaining, 14)),
		2,
		RoundingMode.HALF_UP,
	)
	val monthly = remaining.divide(
		BigDecimal(blocksRemaining(daysRemaining, 30)),
		2,
		RoundingMode.HALF_UP,
	)

	return DynamicAllocations(
		dailyAllocation = daily,
		weeklyAllocation = weekly,
		biweeklyAllocation = biweekly,
		monthlyAllocation = monthly,
		isTodayOverDailyAllocation = totalSpentToday > daily,
	)
}

internal fun blocksRemaining(daysRemaining: Int, blockDays: Int): Int {
    val blocks = (daysRemaining + blockDays - 1) / blockDays
    return blocks.coerceAtLeast(1)
}
