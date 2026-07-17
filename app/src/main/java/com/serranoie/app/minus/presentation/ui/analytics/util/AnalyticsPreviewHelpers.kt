package com.serranoie.app.minus.presentation.ui.analytics.util

import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.analytics.AnalyticsState
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Date

val previewStartDate: Date
    get() =
        Calendar
            .getInstance()
            .apply {
                add(Calendar.DAY_OF_MONTH, -34)
            }.time

val previewFinishDate: Date
    get() =
        Calendar
            .getInstance()
            .apply {
                add(Calendar.DAY_OF_MONTH, 7)
            }.time

fun previewAnalyticsTransactions(): List<Transaction> {
    val categories =
        listOf(
            "Comida",
        )

    return categories.flatMapIndexed { index, category ->
        listOf(
            Transaction(
                amount = BigDecimal(45 + index * 18),
                date = LocalDateTime.now().minusDays((index * 3L) + 1),
                comment = category,
            ),
        )
    }
}

fun previewAnalyticsState(periodFinished: Boolean): AnalyticsState {
    val txs = previewAnalyticsTransactions()
    val creditTxs = listOf(
        Transaction(
            id = 100L,
            amount = BigDecimal("121.00"),
            date = LocalDateTime.now(),
            comment = "Credit Card Payment",
            isCredit = true
        )
    )
    return AnalyticsState(
        periodFinished = periodFinished,
        transactions = txs + creditTxs,
        spends = txs + creditTxs,
        wholeBudget = BigDecimal(2400),
        startPeriodDate = previewStartDate,
        finishPeriodDate = if (periodFinished) Date() else previewFinishDate,
        finishPeriodActualDate = if (periodFinished) Date() else null,
        creditOwed = BigDecimal("121.00"),
        creditTransactions = creditTxs
    )
}
