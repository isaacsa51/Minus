package com.serranoie.app.wear.minus.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import com.serranoie.app.minus.sync.contract.WearJson

private val Context.pendingExpenseDataStore by preferencesDataStore(name = "wear_pending_expenses")

class PendingExpenseStore(private val context: Context) {

    private val key = stringPreferencesKey("pending_expenses_json")

    companion object {
        private const val TAG = "WearPendingStore"
        private const val ACK_TIMEOUT_MS = 20_000L
    }

    val allExpenses: Flow<List<PendingExpense>> = context.pendingExpenseDataStore.data.map { prefs ->
        val raw = prefs[key]
        if (raw.isNullOrBlank()) emptyList()
        else runCatching {
            WearJson.json.decodeFromString<List<PendingExpense>>(raw)
        }.getOrElse { emptyList() }
    }

    suspend fun enqueue(expense: PendingExpense) {
        val current = getAllOnce()
        val next = current + expense
        writeAll(next)
        Log.d(TAG, "enqueue: id=${expense.clientGeneratedId}, amount=${expense.amount}, total=${next.size}")
    }

    suspend fun markSentWaitingAck(clientGeneratedId: String) {
        mutate(clientGeneratedId) {
            it.copy(syncState = SyncState.SENT_WAITING_ACK, lastAttemptAt = System.currentTimeMillis())
        }
    }

    suspend fun markSynced(clientGeneratedId: String) {
        mutate(clientGeneratedId) {
            it.copy(syncState = SyncState.SYNCED, lastAttemptAt = System.currentTimeMillis())
        }
    }

    suspend fun markFailedRetryable(clientGeneratedId: String) {
        mutate(clientGeneratedId) {
            it.copy(
                syncState = SyncState.FAILED_RETRYABLE,
                retryCount = it.retryCount + 1,
                lastAttemptAt = System.currentTimeMillis()
            )
        }
    }

    suspend fun getRetryable(limit: Int = 30): List<PendingExpense> {
        val now = System.currentTimeMillis()
        val list = getAllOnce()
            .filter {
                it.syncState == SyncState.PENDING ||
                    it.syncState == SyncState.FAILED_RETRYABLE ||
                    (it.syncState == SyncState.SENT_WAITING_ACK && (it.lastAttemptAt == null || now - it.lastAttemptAt >= ACK_TIMEOUT_MS))
            }
            .sortedBy { it.eventTime }
            .take(limit)

        Log.d(TAG, "getRetryable: total=${getAllOnce().size}, retryable=${list.size}")
        return list
    }

    private suspend fun mutate(clientGeneratedId: String, transform: (PendingExpense) -> PendingExpense) {
        val current = getAllOnce()
        val next = current.map { if (it.clientGeneratedId == clientGeneratedId) transform(it) else it }
        writeAll(next)
    }

    private suspend fun getAllOnce(): List<PendingExpense> {
        return allExpenses.first()
    }

    private suspend fun writeAll(list: List<PendingExpense>) {
        context.pendingExpenseDataStore.edit { prefs ->
            prefs[key] = WearJson.json.encodeToString(list)
        }
    }
}
