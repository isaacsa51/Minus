package com.serranoie.app.minus.data.csv

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
    val subscriptionDay: Int?
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
        isDeleted = false,
        isRecurrent = isRecurrent,
        recurrentFrequency = frequency,
        recurrentEndDate = endDate?.atStartOfDay(),
        subscriptionDay = subscriptionDay
    )
}
