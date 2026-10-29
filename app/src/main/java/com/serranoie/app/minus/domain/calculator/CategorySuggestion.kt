package com.serranoie.app.minus.domain.calculator

import com.serranoie.app.minus.domain.model.Transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.min

private const val HOURS_IN_DAY = 24
private const val HOUR_WINDOW = 2
private val CLOSE_AMOUNT_RATIO = BigDecimal("1.2")
private val NEAR_AMOUNT_RATIO = BigDecimal("1.5")

internal fun rankCategoriesForEntry(
    categories: List<String>,
    history: List<Transaction>,
    amount: BigDecimal?,
    now: LocalDateTime,
): List<String> {
    val known = categories.toSet()
    val scores = HashMap<String, Int>()
    for (transaction in history) {
        val date = transaction.date ?: continue
        if (transaction.isDeleted || transaction.isAdjustment || transaction.isRecurrent) continue
        if (transaction.comment !in known || transaction.amount.signum() <= 0) continue
        val score = hourScore(date.hour, now.hour) + amountScore(transaction.amount, amount)
        if (score > 0) scores.merge(transaction.comment, score, Int::plus)
    }
    if (scores.isEmpty()) return categories
    return categories.sortedByDescending { scores[it] ?: 0 }
}

private fun hourScore(a: Int, b: Int): Int {
    val diff = abs(a - b)
    return if (min(diff, HOURS_IN_DAY - diff) <= HOUR_WINDOW) 1 else 0
}

private fun amountScore(past: BigDecimal, typed: BigDecimal?): Int {
    if (typed == null || typed.signum() <= 0) return 0
    val ratio = past.max(typed).divide(past.min(typed), 2, RoundingMode.HALF_UP)
    return when {
        ratio <= CLOSE_AMOUNT_RATIO -> 2
        ratio <= NEAR_AMOUNT_RATIO -> 1
        else -> 0
    }
}
