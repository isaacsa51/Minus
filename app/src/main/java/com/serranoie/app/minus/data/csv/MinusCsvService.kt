package com.serranoie.app.minus.data.csv

import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.io.OutputStream
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MinusCsvService @Inject constructor(
    private val repository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
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
        val archivedBudgets = repository.getArchivedBudgets().first()
        val paidOccurrences = repository.getPaidRecurrentOccurrences().first()
        val pockets = repository.getSavingsPockets().first()
        val settings = repository.getBudgetSettingsSync()
        val userSettings = settingsRepository.getSettings()
        val metadata = settings?.let {
            CsvBackupMetadata(
                budgetSettings = it,
                currentPeriodStartedAtMillis = userSettings.currentPeriodStartedAt,
                currentPeriodId = userSettings.currentPeriodId,
            )
        }
        exporter.export(transactions, archivedBudgets, metadata, paidOccurrences, outputStream, pockets)
    }

    suspend fun hasNoLocalData(): Boolean {
        return repository.getBudgetSettingsSync() == null &&
                repository.getRecentTransactions(1).isEmpty()
    }

    suspend fun importTransactions(
        inputStream: InputStream,
        allowIncompleteFullRestore: Boolean = false,
    ): CsvImportResult {
        val payload = parser.parse(inputStream)
        val rows = payload.rows

        // Rows only carry free-text comments, so resolve/create the matching category per
        // distinct comment the same way manual entry does (BudgetTransactionHandler), otherwise
        // imported transactions would all fall back to "Uncategorized".
        val categoryIdsByComment = rows.mapNotNull { it.comment.trim().ifBlank { null } }
            .distinct()
            .associateWith { repository.findOrCreateCategory(it).id }

        val reusable = rows.filter { it.id > 0L }
            .map { it.toDomainTransaction(categoryId = categoryIdsByComment[it.comment.trim()]) }
        val fresh = rows.filter { it.id == 0L }
            .map { it.toDomainTransaction(categoryId = categoryIdsByComment[it.comment.trim()]) }

        if (payload.metadata != null) {
            if (payload.errors.isNotEmpty() && !allowIncompleteFullRestore) {
                return CsvImportResult(
                    imported = 0,
                    discarded = payload.errors.size,
                    errors = payload.errors,
                )
            }
            repository.replaceAllTransactions(
                transactions = reusable + fresh.map { it.copy(id = 0L) },
                paidOccurrences = payload.paidOccurrences,
                pockets = payload.pockets,
            )
            repository.replaceAllArchivedBudgets(payload.archivedBudgets)
        } else {
            payload.pockets.forEach { repository.saveSavingsPocket(it) }
            if (reusable.isNotEmpty()) {
                repository.upsertTransactions(reusable)
            }
            fresh.forEach { repository.addTransaction(it.copy(id = 0L)) }
            if (payload.archivedBudgets.isNotEmpty()) {
                repository.upsertArchivedBudgets(payload.archivedBudgets)
            }
            payload.paidOccurrences.forEach { occurrence ->
                repository.markRecurrentOccurrencePaid(
                    occurrence.transactionId,
                    occurrence.occurrenceDate,
                    occurrence.status,
                )
            }
        }

        payload.metadata?.let { metadata ->
            repository.saveBudgetSettings(metadata.budgetSettings)
            settingsRepository.setCurrentPeriod(
                metadata.currentPeriodId,
                metadata.currentPeriodStartedAtMillis
            )
            settingsRepository.setOnboardingCompleted(true)
        }

        return CsvImportResult(
            imported = rows.size,
            discarded = payload.errors.size,
            errors = payload.errors,
        )
    }
}
