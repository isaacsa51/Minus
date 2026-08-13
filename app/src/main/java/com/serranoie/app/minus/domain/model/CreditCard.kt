package com.serranoie.app.minus.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

data class CreditCard(
    val cutoffDay: Int,
    val gracePeriodDays: Long = 20
)

fun calculatePaymentDueDate(card: CreditCard, targetDate: LocalDate = LocalDate.now()): LocalDate {
    val currentMonthCutoff = try {
        targetDate.withDayOfMonth(card.cutoffDay)
    } catch (e: Exception) {
        targetDate.withDayOfMonth(targetDate.lengthOfMonth())
    }

    val actualCutoffDate = if (targetDate.isAfter(currentMonthCutoff)) {
        currentMonthCutoff
    } else {
        currentMonthCutoff.minusMonths(1)
    }

    val rawDueDate = actualCutoffDate.plusDays(card.gracePeriodDays)

    return when (rawDueDate.dayOfWeek) {
        DayOfWeek.SATURDAY -> rawDueDate.plusDays(2)
        DayOfWeek.SUNDAY -> rawDueDate.plusDays(1)
        else -> rawDueDate
    }
}
