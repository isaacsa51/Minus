package com.serranoie.app.wear.minus.sync

interface SyncRepository {
    suspend fun syncPendingExpenses(): Boolean
}
