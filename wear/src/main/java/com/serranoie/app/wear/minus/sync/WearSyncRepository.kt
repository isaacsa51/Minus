package com.serranoie.app.wear.minus.sync

import android.content.Context
import android.util.Log
import com.serranoie.app.wear.minus.data.PendingExpenseStore

class WearSyncRepository(
    private val context: Context
) : SyncRepository {

    companion object {
        private const val TAG = "WearSyncRepo"
    }

    override suspend fun syncPendingExpenses(): Boolean {
        val store = PendingExpenseStore(context)
        val syncManager = WearSyncManager(context)
        val retryable = store.getRetryable()
        Log.d(TAG, "syncPendingExpenses: retryable=${retryable.size}")

        if (retryable.isEmpty()) {
            val snapshotRequested = syncManager.requestSnapshot()
            Log.d(TAG, "syncPendingExpenses: no retryable expenses, snapshotRequested=$snapshotRequested")
            return true
        }

        var anyFailure = false
        retryable.forEach { expense ->
            val sent = syncManager.sendExpense(expense)
            if (sent) {
                Log.d(TAG, "syncPendingExpenses: sent expense id=${expense.clientGeneratedId}")
                store.markSentWaitingAck(expense.clientGeneratedId)
            } else {
                anyFailure = true
                Log.w(TAG, "syncPendingExpenses: failed send id=${expense.clientGeneratedId}")
                store.markFailedRetryable(expense.clientGeneratedId)
            }
        }

        val snapshotRequested = syncManager.requestSnapshot()
        Log.d(TAG, "syncPendingExpenses: snapshotRequested=$snapshotRequested, anyFailure=$anyFailure")
        return !anyFailure
    }
}
