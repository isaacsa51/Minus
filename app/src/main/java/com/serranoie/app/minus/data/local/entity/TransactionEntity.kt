package com.serranoie.app.minus.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Room entity for storing transactions.
 * Amount is stored as String to preserve BigDecimal precision.
 */
@Entity(
    tableName = "transactions",
    indices = [Index(value = ["clientGeneratedId"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: String, // BigDecimal as String to preserve precision
    val comment: String,
    val date: Long, // Epoch millis
    val createdAt: Long = System.currentTimeMillis(),
    val clientGeneratedId: String? = null,
    val periodId: Long = 0L,
    val isRecurrent: Boolean = false,
    val recurrentFrequency: String? = null, // Stored as string name of enum
    val recurrentEndDate: Long? = null, // Epoch millis for end date
    val subscriptionDay: Int? = null // Day of month (1-31) for monthly subscriptions
) {
    companion object {
        fun fromDomain(
            amount: String,
            comment: String,
            date: LocalDateTime,
            isRecurrent: Boolean = false,
            recurrentFrequency: String? = null,
            recurrentEndDate: LocalDateTime? = null,
            subscriptionDay: Int? = null
        ): TransactionEntity = TransactionEntity(
            id = 0,
            amount = amount,
            comment = comment,
            date = date.toEpochSecond(ZoneOffset.UTC) * 1000,
            isRecurrent = isRecurrent,
            recurrentFrequency = recurrentFrequency,
            recurrentEndDate = recurrentEndDate?.toEpochSecond(ZoneOffset.UTC)?.times(1000),
            subscriptionDay = subscriptionDay
        )
    }
}
