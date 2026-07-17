package com.serranoie.app.minus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "queued_transactions")
data class QueuedTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: String,
    val comment: String,
    val date: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val categoryId: Long? = null,
    val isCredit: Boolean = false,
    val isCreditPaid: Boolean = false
)
