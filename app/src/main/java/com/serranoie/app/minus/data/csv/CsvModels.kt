package com.serranoie.app.minus.data.csv

import com.serranoie.app.minus.domain.model.ArchivedBudget
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class CsvTransactionRow(
    val id: Long,
    val date: LocalDateTime,
    val amount: BigDecimal,
    val comment: String,
    val isRecurrent: Boolean,
    val frequency: RecurrentFrequency?,
    val endDate: LocalDate?,
    val subscriptionDay: Int?,
    val isCredit: Boolean,
    val isCreditPaid: Boolean,
    val periodId: Long
)

data class CsvBackupMetadata(
    val budgetSettings: BudgetSettings,
    val currentPeriodStartedAtMillis: Long,
    val currentPeriodId: Long,
)

data class CsvImportPayload(
    val rows: List<CsvTransactionRow>,
    val metadata: CsvBackupMetadata?,
    val archivedBudgets: List<ArchivedBudget>,
    val errors: List<String>,
)

data class CsvImportResult(
    val imported: Int,
    val discarded: Int,
    val errors: List<String>
)

fun CsvTransactionRow.toDomainTransaction(): Transaction {
    return Transaction(
        id = id,
        amount = amount,
        comment = comment,
        date = date,
        periodId = periodId,
        isDeleted = false,
        isRecurrent = isRecurrent,
        recurrentFrequency = frequency,
        recurrentEndDate = endDate?.atStartOfDay(),
        subscriptionDay = subscriptionDay,
        isCredit = isCredit,
        isCreditPaid = isCreditPaid
    )
}

