package com.serranoie.app.minus.data.csv

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.presentation.CURRENT_PERIOD_ID_KEY
import com.serranoie.app.minus.presentation.CURRENT_PERIOD_STARTED_AT_KEY
import com.serranoie.app.minus.presentation.ONBOARDING_COMPLETED_KEY
import com.serranoie.app.minus.presentation.settingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.io.OutputStream
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MinusCsvService @Inject constructor(
    private val repository: BudgetRepository,
    @param:ApplicationContext private val context: Context,
) {

    private val parser = MinusCsvParser()
    private val exporter = MinusCsvExporter()
    private val fileDateFormatter = DateTimeFormatter.ofPattern("ddMMM", java.util.Locale.ENGLISH)

    suspend fun getExportFileName(): String {
        val settings = repository.getBudgetSettingsSync() ?: return MinusCsvContract.FILE_NAME
        val periodCount = repository.getPeriodCount()

        // If current period has no transactions yet, count might be one lower than expected
        // for the "current" label, but user asked for "sequence number of the period".
        // If they are in their 5th period, we use BP5.
        val bpLabel = "BP${periodCount + 1}"

        val startDate = settings.startDate.format(fileDateFormatter).lowercase()
        val endDate = settings.getPeriodEndDate().format(fileDateFormatter).lowercase()

        return "minus_backup-${bpLabel}_$startDate-$endDate.csv"
    }

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
