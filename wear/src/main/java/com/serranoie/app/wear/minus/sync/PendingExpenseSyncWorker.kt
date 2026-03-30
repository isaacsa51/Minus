package com.serranoie.app.wear.minus.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class PendingExpenseSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val syncRepository: SyncRepository = WearSyncRepository(applicationContext)
        val success = syncRepository.syncPendingExpenses()
        return if (success) Result.success() else Result.retry()
    }
}
