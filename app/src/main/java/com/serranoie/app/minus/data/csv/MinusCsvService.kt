package com.serranoie.app.minus.data.csv

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.serranoie.app.minus.CURRENT_PERIOD_ID_KEY
import com.serranoie.app.minus.CURRENT_PERIOD_STARTED_AT_KEY
import com.serranoie.app.minus.ONBOARDING_COMPLETED_KEY
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.settingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MinusCsvService @Inject constructor(
    private val repository: BudgetRepository,
    @ApplicationContext private val context: Context,
) {

    private val parser = MinusCsvParser()
    private val exporter = MinusCsvExporter()

    suspend fun exportAllTransactions(outputStream: OutputStream) {
        val transactions = repository.getTransactions().first()
        val settings = repository.getBudgetSettingsSync()
        val prefs = context.settingsDataStore.data.first()
        val metadata = settings?.let {
            CsvBackupMetadata(
                budgetSettings = it,
                currentPeriodStartedAtMillis = prefs[CURRENT_PERIOD_STARTED_AT_KEY] ?: 0L,
                currentPeriodId = prefs[CURRENT_PERIOD_ID_KEY] ?: 0L,
            )
        }
        exporter.export(transactions, metadata, outputStream)
    }

    suspend fun importTransactions(inputStream: InputStream): CsvImportResult {
        val payload = parser.parse(inputStream)
        val rows = payload.rows

        val reusable = rows.filter { it.id > 0L }.map { it.toDomainTransaction() }
        val fresh = rows.filter { it.id == 0L }.map { it.toDomainTransaction() }

        if (reusable.isNotEmpty()) {
            repository.upsertTransactions(reusable)
        }

        fresh.forEach { repository.addTransaction(it.copy(id = 0L)) }

        payload.metadata?.let { metadata ->
            repository.saveBudgetSettings(metadata.budgetSettings)
            context.settingsDataStore.edit { prefs ->
                prefs[CURRENT_PERIOD_STARTED_AT_KEY] = metadata.currentPeriodStartedAtMillis
                prefs[CURRENT_PERIOD_ID_KEY] = metadata.currentPeriodId
                prefs[ONBOARDING_COMPLETED_KEY] = true
            }
        }

        return CsvImportResult(
            imported = rows.size,
            discarded = payload.errors.size,
            errors = payload.errors,
        )
    }
}
