package com.serranoie.app.minus.wearsync

import android.util.Log
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.sync.contract.ExpensePayload
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearExpenseIngestor @Inject constructor(
    private val repository: BudgetRepository
) {

    companion object {
        private const val TAG = "WearExpenseIngestor"
    }

    private val ingestMutex = Mutex()

    sealed interface IngestResult {
        data object Ok : IngestResult
        data class Error(val reason: String) : IngestResult
    }

    suspend fun ingest(payload: ExpensePayload): IngestResult = ingestMutex.withLock {
        if (payload.amount.isBlank()) {
            return@withLock IngestResult.Error("Amount empty")
        }

        val amount = runCatching { BigDecimal(payload.amount) }.getOrNull()
            ?: return@withLock IngestResult.Error("Invalid amount")

        val exists = repository.existsTransactionByClientGeneratedId(payload.clientGeneratedId)
        if (exists) {
            Log.d(TAG, "ingest: duplicate pre-check id=${payload.clientGeneratedId}")
            return@withLock IngestResult.Ok
        }

        val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(payload.eventTime), ZoneId.systemDefault())
        val tx = Transaction.create(
            amount = amount,
            comment = payload.comment,
            date = date,
            periodId = payload.periodId ?: 0L,
            clientGeneratedId = payload.clientGeneratedId
        )
        val inserted = repository.addTransactionIfAbsent(tx)
        if (!inserted) {
            Log.d(TAG, "ingest: duplicate on insert-ignore id=${payload.clientGeneratedId}")
        } else {
            Log.d(TAG, "ingest: inserted id=${payload.clientGeneratedId}, amount=${payload.amount}")
        }

        IngestResult.Ok
    }
}
