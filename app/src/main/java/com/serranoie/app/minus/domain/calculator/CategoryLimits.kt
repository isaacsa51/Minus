package com.serranoie.app.minus.domain.calculator

import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.domain.model.CategoryLimit
import com.serranoie.app.minus.domain.model.CategoryLimitUnit
import com.serranoie.app.minus.domain.model.Transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

private const val DAYS_IN_WEEK = 7L
private val NEAR_LIMIT_RATIO = BigDecimal("0.9")
private val PACE_TOLERANCE = BigDecimal("0.1")

enum class CategoryLimitPace { ON_TRACK, AHEAD, OVER }

data class CategoryLimitStatus(
    val name: String,
    val limit: CategoryLimit,
    val spent: BigDecimal,
    val windowStart: LocalDate,
    val windowEnd: LocalDate,
    val pace: CategoryLimitPace,
) {
    val remaining: BigDecimal get() = limit.amount.subtract(spent)

    fun projectedFraction(pending: BigDecimal?): Float {
        if (limit.amount.signum() <= 0) return 1f
        val total = spent.add(pending?.takeIf { it.signum() > 0 } ?: BigDecimal.ZERO)
        return total.divide(limit.amount, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
    }

    fun overBy(pending: BigDecimal?): BigDecimal? {
        if (pending == null || pending.signum() <= 0) return null
        return spent.add(pending).subtract(limit.amount).takeIf { it.signum() > 0 }
    }
}

internal fun categoryLimitStatuses(
    categories: List<Category>,
    transactions: List<Transaction>,
    periodStart: LocalDate,
    periodEnd: LocalDate,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
): Map<String, CategoryLimitStatus> {
    val limited = categories.mapNotNull { category -> category.limit?.let { category.name to it } }
    if (limited.isEmpty()) return emptyMap()
    val weekStart = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    val spends = transactions.filter {
        it.date != null && !it.isDeleted && !it.isAdjustment && !it.isRecurrent && it.amount.signum() > 0
    }
    return limited.associate { (name, limit) ->
        val (start, end) = when (limit.unit) {
            CategoryLimitUnit.WEEKLY -> weekStart to weekStart.plusDays(DAYS_IN_WEEK - 1)
            CategoryLimitUnit.PERIOD -> periodStart to periodEnd
        }
        val spent = spends
            .filter { it.comment == name && it.date!!.toLocalDate() in start..end }
            .fold(BigDecimal.ZERO) { sum, tx -> sum.add(tx.amount) }
        name to CategoryLimitStatus(name, limit, spent, start, end, pace(spent, limit.amount, start, end, today))
    }
}

private fun pace(spent: BigDecimal, limit: BigDecimal, start: LocalDate, end: LocalDate, today: LocalDate): CategoryLimitPace {
    if (spent > limit) return CategoryLimitPace.OVER
    if (limit.signum() <= 0) return CategoryLimitPace.ON_TRACK
    val totalDays = ChronoUnit.DAYS.between(start, end) + 1
    val elapsedDays = (ChronoUnit.DAYS.between(start, today) + 1).coerceIn(1, totalDays)
    val elapsed = BigDecimal(elapsedDays).divide(BigDecimal(totalDays), 4, RoundingMode.HALF_UP)
    val used = spent.divide(limit, 4, RoundingMode.HALF_UP)
    return if (used >= NEAR_LIMIT_RATIO || used > elapsed.add(PACE_TOLERANCE)) {
        CategoryLimitPace.AHEAD
    } else {
        CategoryLimitPace.ON_TRACK
    }
}
