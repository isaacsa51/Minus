package com.serranoie.app.wear.minus.data

import kotlinx.serialization.Serializable

@Serializable
enum class SyncState {
    PENDING,
    SENT_WAITING_ACK,
    SYNCED,
    FAILED_RETRYABLE
}

@Serializable
data class PendingExpense(
    val clientGeneratedId: String,
    val amount: String,
    val comment: String,
    val eventTime: Long,
    val syncState: SyncState = SyncState.PENDING,
    val retryCount: Int = 0,
    val lastAttemptAt: Long? = null
)
