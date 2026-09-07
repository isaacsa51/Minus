package com.serranoie.app.minus.wearsync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.sync.contract.ExpensePayload
import com.serranoie.app.minus.sync.contract.WearJson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import logcat.logcat
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

private val Context.wearIngestLedgerStore by preferencesDataStore(name = "wear_ingest_ledger")

@Singleton
class WearExpenseIngestor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
) {

    private companion object {
        val LEDGER_KEY = stringPreferencesKey("ingested_client_ids")

        const val LEDGER_MAX = 2000
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

        if (isInLedger(payload.clientGeneratedId)) {
            logcat { "ingest: already processed (ledger), ignoring id=${payload.clientGeneratedId}" }
            return@withLock IngestResult.Ok
        }

        val exists = repository.existsTransactionByClientGeneratedId(payload.clientGeneratedId)
        if (exists) {
            logcat { "ingest: duplicate pre-check id=${payload.clientGeneratedId}" }
            recordInLedger(payload.clientGeneratedId)
            return@withLock IngestResult.Ok
        }

        val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(payload.eventTime), ZoneId.systemDefault())

        val categoryId: Long? = if (payload.comment.isNotBlank()) {
            repository.findOrCreateCategory(payload.comment.trim()).id
        } else null

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
        recordInLedger(payload.clientGeneratedId)

        IngestResult.Ok
    }

    private suspend fun isInLedger(id: String): Boolean = id in readLedger()

    private suspend fun readLedger(): List<String> {
        val raw = context.wearIngestLedgerStore.data.first()[LEDGER_KEY] ?: return emptyList()
        return runCatching {
            WearJson.json.decodeFromString(ListSerializer(String.serializer()), raw)
        }.getOrElse { emptyList() }
    }

    private suspend fun recordInLedger(id: String) {
        context.wearIngestLedgerStore.edit { prefs ->
            val current = runCatching {
                prefs[LEDGER_KEY]?.let {
                    WearJson.json.decodeFromString(ListSerializer(String.serializer()), it)
                }
            }.getOrNull() ?: emptyList()
            if (id in current) return@edit
            val next = (current + id).takeLast(LEDGER_MAX)
            prefs[LEDGER_KEY] = WearJson.json.encodeToString(ListSerializer(String.serializer()), next)
        }
    }
}
