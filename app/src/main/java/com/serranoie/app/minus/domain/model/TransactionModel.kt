package com.serranoie.app.minus.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

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
    val subscriptionDay: Int? = null,
    val categoryId: Long? = null,
    val isCredit: Boolean = false,
    val sourceTransactionId: Long? = null
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
            subscriptionDay: Int? = null,
            categoryId: Long? = null,
            isCredit: Boolean = false
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
            subscriptionDay = subscriptionDay,
            categoryId = categoryId,
            isCredit = isCredit
        )
    }
}


enum class RecurrentFrequency {
    WEEKLY,
    BIWEEKLY,
    MONTHLY
}
