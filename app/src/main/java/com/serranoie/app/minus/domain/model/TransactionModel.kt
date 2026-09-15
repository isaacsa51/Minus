package com.serranoie.app.minus.domain.model

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class Transaction(
    val id: Long = 0,
    val amount: BigDecimal,
    val comment: String = "",
    val note: String = "",
    val date: LocalDateTime?,
    val createdAt: Long = System.currentTimeMillis(),
    val clientGeneratedId: String? = null,
    val periodId: Long = 0L,
    val isDeleted: Boolean = false,
    val isRecurrent: Boolean = false,
    val recurrentFrequency: RecurrentFrequency? = null,
    val recurrentEndDate: LocalDateTime? = null,
    val subscriptionDay: Int? = null,
    val categoryId: Long? = null,
    val isCredit: Boolean = false,
    val isCreditPaid: Boolean = false,
    val isAdjustment: Boolean = false,
    val sourceTransactionId: Long? = null
) {
    companion object {
        fun create(
            amount: BigDecimal,
            comment: String = "",
            note: String = "",
            date: LocalDateTime?,
            periodId: Long = 0L,
            clientGeneratedId: String? = null,
            isRecurrent: Boolean = false,
            recurrentFrequency: RecurrentFrequency? = null,
            recurrentEndDate: LocalDateTime? = null,
            subscriptionDay: Int? = null,
            categoryId: Long? = null,
            isCredit: Boolean = false,
            isCreditPaid: Boolean = false,
            isAdjustment: Boolean = false
        ): Transaction = Transaction(
            id = 0,
            amount = amount,
            comment = comment,
            note = note,
            date = date,
            periodId = periodId,
            clientGeneratedId = clientGeneratedId,
            isDeleted = false,
            isRecurrent = isRecurrent,
            recurrentFrequency = recurrentFrequency,
            recurrentEndDate = recurrentEndDate,
            subscriptionDay = subscriptionDay,
            categoryId = categoryId,
            isCredit = isCredit,
            isCreditPaid = isCreditPaid,
            isAdjustment = isAdjustment
        )
    }
}


enum class RecurrentFrequency {
    WEEKLY,
    BIWEEKLY,
    MONTHLY
}

enum class RecurrentOccurrenceStatus {
    PAID,
    SKIPPED,
}

/**
 * Equality/hashCode intentionally ignore [status] and key only on
 * ([transactionId], [occurrenceDate]) — every existing `paidOccurrences.contains(...)` /
 * `!paidOccurrences.contains(...)` check across the app (RecurringExpenseCalculator,
 * HistoryCalculations, etc.) means "has this occurrence been resolved at all", regardless of
 * paid vs. skipped, and those checks construct a bare two-arg instance to test membership.
 * Making status part of equality would silently break every one of those call sites.
 */
data class PaidRecurrentOccurrence(
    val transactionId: Long,
    val occurrenceDate: LocalDate,
    val status: RecurrentOccurrenceStatus = RecurrentOccurrenceStatus.PAID,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaidRecurrentOccurrence) return false
        return transactionId == other.transactionId && occurrenceDate == other.occurrenceDate
    }

    override fun hashCode(): Int = 31 * transactionId.hashCode() + occurrenceDate.hashCode()
}
