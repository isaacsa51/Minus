package com.serranoie.app.minus.data.csv

import com.serranoie.app.minus.data.repository.BudgetRepository
import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MinusCsvService @Inject constructor(
    private val repository: BudgetRepository
) {

    private val parser = MinusCsvParser()
    private val exporter = MinusCsvExporter()

    suspend fun exportAllTransactions(outputStream: OutputStream) {
        val transactions = repository.getTransactions().first()
        exporter.export(transactions, outputStream)
    }

    suspend fun importTransactions(inputStream: InputStream): CsvImportResult {
        val (rows, parseErrors) = parser.parse(inputStream)

        val reusable = rows.filter { it.id > 0L }.map { it.toDomainTransaction() }
        val fresh = rows.filter { it.id == 0L }.map { it.toDomainTransaction() }

        if (reusable.isNotEmpty()) {
            repository.upsertTransactions(reusable)
        }

        fresh.forEach { repository.addTransaction(it.copy(id = 0L)) }

        return CsvImportResult(
            imported = rows.size,
            discarded = parseErrors.size,
            errors = parseErrors
        )
    }
}
