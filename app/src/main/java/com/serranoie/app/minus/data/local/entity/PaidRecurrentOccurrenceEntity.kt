package com.serranoie.app.minus.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "paid_recurrent_occurrences",
    primaryKeys = ["transactionId", "occurrenceDateEpochDay"],
)
data class PaidRecurrentOccurrenceEntity(
    val transactionId: Long,
    val occurrenceDateEpochDay: Long,
    val paidAt: Long = System.currentTimeMillis(),
)
