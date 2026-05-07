package com.serranoie.app.minus.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.ZoneOffset

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["clientGeneratedId"], unique = true),
        Index(value = ["categoryId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: String,
    val comment: String,
    val date: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val clientGeneratedId: String? = null,
    val periodId: Long = 0L,
    val isRecurrent: Boolean = false,
    val recurrentFrequency: String? = null,
    val recurrentEndDate: Long? = null,
    val subscriptionDay: Int? = null,
    val categoryId: Long? = null,
    val isCredit: Boolean = false
) {
    companion object {
        fun fromDomain(
            amount: String,
            comment: String,
            date: LocalDateTime,
            isRecurrent: Boolean = false,
            recurrentFrequency: String? = null,
            recurrentEndDate: LocalDateTime? = null,
            subscriptionDay: Int? = null,
            categoryId: Long? = null,
            isCredit: Boolean = false
        ): TransactionEntity = TransactionEntity(
            id = 0,
            amount = amount,
            comment = comment,
            date = date.toEpochSecond(ZoneOffset.UTC) * 1000,
            isRecurrent = isRecurrent,
            recurrentFrequency = recurrentFrequency,
            recurrentEndDate = recurrentEndDate?.toEpochSecond(ZoneOffset.UTC)?.times(1000),
            subscriptionDay = subscriptionDay,
            categoryId = categoryId,
            isCredit = isCredit
        )
    }
}