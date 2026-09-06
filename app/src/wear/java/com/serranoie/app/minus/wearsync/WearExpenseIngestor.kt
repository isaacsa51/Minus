package com.serranoie.app.minus.wearsync

import logcat.logcat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
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
    private val repository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
) {

    companion object {
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
            logcat { "ingest: duplicate pre-check id=${payload.clientGeneratedId}" }
            return@withLock IngestResult.Ok
        }

        val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(payload.eventTime), ZoneId.systemDefault())

        val categoryId: Long? = if (payload.comment.isNotBlank()) {
            repository.findOrCreateCategory(payload.comment.trim()).id
        } else null

        // The watch has no notion of the phone's budget periods and never sends a
        // periodId, so resolve the phone's active period here. Without this the
        // row is stored with periodId = 0L: it shows in the current period via the
        // date-window fallback, but once the period rolls over
        // filterPeriodTransactions() / splitPeriodTransactions() drop it from
        // every period view (it was never assigned to a period, and its date is
        // now before the new period start).
        val periodId = payload.periodId ?: settingsRepository.getCurrentPeriodId()

        val tx = Transaction.create(
            amount = amount,
            comment = payload.comment,
            date = date,
            periodId = periodId,
            clientGeneratedId = payload.clientGeneratedId,
            categoryId = categoryId
        )
        val inserted = repository.addTransactionIfAbsent(tx)
        if (!inserted) {
            logcat { "ingest: duplicate on insert-ignore id=${payload.clientGeneratedId}" }
        } else {
            logcat { "ingest: inserted id=${payload.clientGeneratedId}, amount=${payload.amount}" }
        }

        IngestResult.Ok
    }
}
