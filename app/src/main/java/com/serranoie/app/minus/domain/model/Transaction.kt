package com.serranoie.app.minus.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Represents a single transaction (expense) in the budget tracker.
 */
data class Transaction(
	val id: Long = 0,
	val amount: BigDecimal,
	val comment: String = "",
	val date: LocalDateTime?,
	val createdAt: Long = System.currentTimeMillis(),
	val clientGeneratedId: String? = null,
	val periodId: Long = 0L,
	val isDeleted: Boolean = false,
	val isRecurrent: Boolean = false,
	val recurrentFrequency: RecurrentFrequency? = null,
	val recurrentEndDate: LocalDateTime? = null,
	val subscriptionDay: Int? = null // Day of month (1-31) for monthly subscriptions
) {
    companion object {
        fun create(
            amount: BigDecimal,
            comment: String = "",
            date: LocalDateTime?,
            periodId: Long = 0L,
            clientGeneratedId: String? = null,
            isRecurrent: Boolean = false,
            recurrentFrequency: RecurrentFrequency? = null,
            recurrentEndDate: LocalDateTime? = null,
            subscriptionDay: Int? = null
        ): Transaction = Transaction(
            id = 0,
            amount = amount,
            comment = comment,
            date = date,
            periodId = periodId,
            clientGeneratedId = clientGeneratedId,
            isDeleted = false,
            isRecurrent = isRecurrent,
            recurrentFrequency = recurrentFrequency,
            recurrentEndDate = recurrentEndDate,
            subscriptionDay = subscriptionDay
        )
    }
}

/**
 * Enum representing the frequency of a recurrent expense.
 */
enum class RecurrentFrequency {
    WEEKLY,      // Every 7 days from start date
    BIWEEKLY,    // Every 14 days from start date
    MONTHLY      // Specific day of month (use subscriptionDay)
}
