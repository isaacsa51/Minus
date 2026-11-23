package com.serranoie.app.minus.data.csv

import com.serranoie.app.minus.domain.model.ArchivedBudget
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.SavingsPocket
import com.serranoie.app.minus.domain.model.Transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class CsvTransactionRow(
    val id: Long,
    val date: LocalDateTime,
    val amount: BigDecimal,
    val comment: String,
    val note: String,
    val isRecurrent: Boolean,
    val frequency: RecurrentFrequency?,
    val endDate: LocalDate?,
    val subscriptionDay: Int?,
    val isCredit: Boolean,
    val isCreditPaid: Boolean,
    val periodId: Long,
    val createdAt: Long? = null,
    val isAdjustment: Boolean = false,
    val pocketId: Long? = null,
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
    val paidOccurrences: List<PaidRecurrentOccurrence>,
    val errors: List<String>,
    val pockets: List<SavingsPocket> = emptyList(),
)

data class CsvImportResult(
    val imported: Int,
    val discarded: Int,
    val errors: List<String>
)

fun CsvTransactionRow.toDomainTransaction(categoryId: Long? = null): Transaction {
    return Transaction(
        id = id,
        amount = amount,
        comment = comment,
        note = note,
        date = date,
        periodId = periodId,
        isDeleted = false,
        isRecurrent = isRecurrent,
        recurrentFrequency = frequency,
        recurrentEndDate = endDate?.atStartOfDay(),
        subscriptionDay = subscriptionDay,
        categoryId = categoryId,
        isCredit = isCredit,
        isCreditPaid = isCreditPaid,
        createdAt = createdAt ?: System.currentTimeMillis(),
        isAdjustment = isAdjustment,
        pocketId = pocketId,
    )
}


data class CsvImportPreview(
    val currentPeriodTransactions: Int,
    val budgetTotal: BigDecimal?,
    val currencyCode: String,
    val periodStart: LocalDate?,
    val periodEnd: LocalDate?,
    val completedPeriods: Int,
    val recurringPayments: Int,
    val discarded: Int,
    val replacesAllData: Boolean,
) {
    val blocked: Boolean get() = replacesAllData && discarded > 0
}

fun CsvImportPayload.toPreview(): CsvImportPreview {
    val settings = metadata?.budgetSettings
    val oneOffs = rows.filterNot { it.isRecurrent }
    return CsvImportPreview(
        currentPeriodTransactions = metadata
            ?.let { meta -> oneOffs.count { it.periodId == meta.currentPeriodId } }
            ?: oneOffs.size,
        budgetTotal = settings?.totalBudget,
        currencyCode = settings?.currencyCode ?: "USD",
        periodStart = settings?.startDate,
        periodEnd = settings?.getPeriodEndDate(),
        completedPeriods = archivedBudgets.size,
        recurringPayments = rows.count { it.isRecurrent },
        discarded = errors.size,
        replacesAllData = metadata != null,
    )
}
