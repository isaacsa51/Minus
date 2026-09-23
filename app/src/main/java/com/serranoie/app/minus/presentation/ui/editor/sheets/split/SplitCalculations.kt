package com.serranoie.app.minus.presentation.ui.editor.sheets.split

import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
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

internal data class BlockWindow(
    val daysFromStart: Int,
    val daysInBlock: Int,
    val daysAfter: Int,
)

internal fun blockWindow(totalDays: Int, daysRemaining: Int, blockDays: Int): BlockWindow {
    val daysElapsed = (totalDays - daysRemaining).coerceAtLeast(0)
    val offsetInBlock = daysElapsed % blockDays
    val daysFromStart = daysRemaining + offsetInBlock
    val daysInBlock = minOf(blockDays, daysFromStart)
    return BlockWindow(daysFromStart, daysInBlock, daysFromStart - daysInBlock)
}

private fun share(pool: BigDecimal, days: Int, over: Int): BigDecimal =
    if (over <= 0 || pool.signum() <= 0) BigDecimal.ZERO
    else pool.multiply(BigDecimal(days)).divide(BigDecimal(over), 2, RoundingMode.HALF_UP)

fun earnedAllowance(splitBudget: BigDecimal, throughDay: Int, totalDays: Int): BigDecimal =
    share(splitBudget, minOf(throughDay, totalDays).coerceAtLeast(0), totalDays)

internal fun BudgetState.spentIn(period: BudgetPeriod): BigDecimal = when (period) {
    BudgetPeriod.DAILY -> totalSpentToday
    BudgetPeriod.WEEKLY -> totalSpentThisWeek
    BudgetPeriod.BIWEEKLY -> totalSpentThisBiweek
    BudgetPeriod.MONTHLY -> totalSpentThisMonth
}

private fun BudgetState.carryOverThrough(day: Int, draft: BigDecimal): BigDecimal =
    remainingToday.subtract(draft)
        .add(earnedAllowance(splitBudget, day, periodTotalDays))
        .subtract(earnedAllowance(splitBudget, periodTotalDays - daysRemaining + 1, periodTotalDays))

fun BudgetState.carryOverRemaining(period: BudgetPeriod, draft: BigDecimal = BigDecimal.ZERO): BigDecimal {
    val window = blockWindow(periodTotalDays, daysRemaining, period.toDays())
    return carryOverThrough(periodTotalDays - window.daysAfter, draft)
}

fun BudgetState.carryOverNext(period: BudgetPeriod, draft: BigDecimal = BigDecimal.ZERO): BigDecimal {
    val window = blockWindow(periodTotalDays, daysRemaining, period.toDays())
    if (window.daysAfter <= 0) return BigDecimal.ZERO
    val nextBlockEnd = periodTotalDays - window.daysAfter + minOf(period.toDays(), window.daysAfter)
    return carryOverThrough(nextBlockEnd, draft)
}

fun staticBlockBudget(
    totalBudget: BigDecimal,
    totalDays: Int,
    daysRemaining: Int,
    period: BudgetPeriod,
): BigDecimal {
    if (totalDays <= 0) return BigDecimal.ZERO
    val window = blockWindow(totalDays, daysRemaining, period.toDays())
    return share(totalBudget, window.daysInBlock, totalDays)
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
    totalDays: Int = daysRemaining,
    totalSpentThisWeek: BigDecimal = BigDecimal.ZERO,
    totalSpentThisBiweek: BigDecimal = BigDecimal.ZERO,
    totalSpentThisMonth: BigDecimal = BigDecimal.ZERO,
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

    fun block(period: BudgetPeriod, spentInBlock: BigDecimal): BigDecimal {
        val window = blockWindow(totalDays, daysRemaining, period.toDays())
        return share(remaining.add(spentInBlock), window.daysInBlock, window.daysFromStart)
    }

    val daily = block(BudgetPeriod.DAILY, totalSpentToday)
    return DynamicAllocations(
        dailyAllocation = daily,
        weeklyAllocation = block(BudgetPeriod.WEEKLY, totalSpentThisWeek),
        biweeklyAllocation = block(BudgetPeriod.BIWEEKLY, totalSpentThisBiweek),
        monthlyAllocation = block(BudgetPeriod.MONTHLY, totalSpentThisMonth),
        isTodayOverDailyAllocation = totalSpentToday > daily,
    )
}

data class NextBlockAllocations(
    val dailyAllocation: BigDecimal,
    val weeklyAllocation: BigDecimal,
    val biweeklyAllocation: BigDecimal,
    val monthlyAllocation: BigDecimal,
) {
    fun forPeriod(period: BudgetPeriod): BigDecimal = when (period) {
        BudgetPeriod.DAILY -> dailyAllocation
        BudgetPeriod.WEEKLY -> weeklyAllocation
        BudgetPeriod.BIWEEKLY -> biweeklyAllocation
        BudgetPeriod.MONTHLY -> monthlyAllocation
    }

    companion object {
        val ZERO = NextBlockAllocations(
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
        )
    }
}

fun computeNextBlockAllocations(
    totalBudget: BigDecimal,
    totalSpentInPeriod: BigDecimal,
    totalDays: Int,
    daysRemaining: Int,
): NextBlockAllocations {
    val remaining = totalBudget.subtract(totalSpentInPeriod)
    if (remaining <= BigDecimal.ZERO || totalDays <= 0 || daysRemaining <= 0) {
        return NextBlockAllocations.ZERO
    }

    fun nextBlock(period: BudgetPeriod): BigDecimal {
        val window = blockWindow(totalDays, daysRemaining, period.toDays())
        return share(remaining, minOf(period.toDays(), window.daysAfter), window.daysAfter)
    }

    return NextBlockAllocations(
        dailyAllocation = nextBlock(BudgetPeriod.DAILY),
        weeklyAllocation = nextBlock(BudgetPeriod.WEEKLY),
        biweeklyAllocation = nextBlock(BudgetPeriod.BIWEEKLY),
        monthlyAllocation = nextBlock(BudgetPeriod.MONTHLY),
    )
}

fun BudgetState.dynamicAllocations(draft: BigDecimal = BigDecimal.ZERO): DynamicAllocations =
    computeDynamicAllocations(
        totalBudget = totalBudget,
        totalSpentInPeriod = totalSpentInPeriod.add(draft),
        totalSpentToday = totalSpentToday.add(draft),
        daysRemaining = daysRemaining,
        totalDays = periodTotalDays,
        totalSpentThisWeek = totalSpentThisWeek.add(draft),
        totalSpentThisBiweek = totalSpentThisBiweek.add(draft),
        totalSpentThisMonth = totalSpentThisMonth.add(draft),
    )

fun BudgetState.allocationFor(
    period: BudgetPeriod,
    splitMode: BudgetSplitMode,
    draft: BigDecimal = BigDecimal.ZERO,
): BigDecimal = when (splitMode) {
    BudgetSplitMode.STATIC -> staticBlockBudget(totalBudget, periodTotalDays, daysRemaining, period)

    BudgetSplitMode.DYNAMIC -> dynamicAllocations(draft).forPeriod(period)

    BudgetSplitMode.CARRY_OVER -> carryOverRemaining(period).add(spentIn(period))
}

fun BudgetState.nextAllocationFor(period: BudgetPeriod, draft: BigDecimal = BigDecimal.ZERO): BigDecimal =
    computeNextBlockAllocations(
        totalBudget = totalBudget,
        totalSpentInPeriod = totalSpentInPeriod.add(draft),
        totalDays = periodTotalDays,
        daysRemaining = daysRemaining,
    ).forPeriod(period)
