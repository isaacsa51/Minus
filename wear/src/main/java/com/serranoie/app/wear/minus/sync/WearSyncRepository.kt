package com.serranoie.app.wear.minus.sync

import android.content.Context
import logcat.logcat
import com.serranoie.app.wear.minus.data.PendingExpenseStore

class WearSyncRepository(
    private val context: Context
) : SyncRepository {



    override suspend fun syncPendingExpenses(): Boolean {
        val store = PendingExpenseStore(context)
        val syncManager = WearSyncManager(context)
        val retryable = store.getRetryable()
        logcat { "syncPendingExpenses: retryable=${retryable.size}" }

        if (retryable.isEmpty()) {
            val snapshotRequested = syncManager.requestSnapshot()
            logcat { "syncPendingExpenses: no retryable expenses, snapshotRequested=$snapshotRequested" }
            return true
        }

        var anyFailure = false
        retryable.forEach { expense ->
            val sent = syncManager.sendExpense(expense)
            if (sent) {
                logcat { "syncPendingExpenses: sent expense id=${expense.clientGeneratedId}" }
                store.markSentWaitingAck(expense.clientGeneratedId)
            } else {
                anyFailure = true
                logcat { "syncPendingExpenses: failed send id=${expense.clientGeneratedId}" }
                store.markFailedRetryable(expense.clientGeneratedId)
            }
        }

        val snapshotRequested = syncManager.requestSnapshot()
        logcat { "syncPendingExpenses: snapshotRequested=$snapshotRequested, anyFailure=$anyFailure" }
        return !anyFailure
    }
}
