package com.serranoie.app.minus.data.csv

import com.serranoie.app.minus.data.csv.MinusCsvContract as C
import com.serranoie.app.minus.domain.model.ArchivedBudget
import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.SavingsPocket
import com.serranoie.app.minus.domain.model.Transaction
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter

class MinusCsvExporter {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun export(
        transactions: List<Transaction>,
        archivedBudgets: List<ArchivedBudget>,
        metadata: CsvBackupMetadata?,
        paidOccurrences: Set<PaidRecurrentOccurrence>,
        outputStream: OutputStream,
        pockets: List<SavingsPocket> = emptyList(),
    ) {
        val format = CSVFormat.DEFAULT.builder()
            .setHeader(*C.HEADERS)
            .get()

        OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
            CSVPrinter(writer, format).use { printer ->
                metadata?.let { meta ->
                    val s = meta.budgetSettings
                    printer.row(
                        C.COL_DATE to C.MARKER_META,
                        C.COL_BUDGET_TOTAL to s.totalBudget.toPlainString(),
                        C.COL_BUDGET_PERIOD to s.period.name,
                        C.COL_BUDGET_START_DATE to s.startDate.format(dateFormatter),
                        C.COL_BUDGET_END_DATE to s.getPeriodEndDate().format(dateFormatter),
                        C.COL_CURRENCY_CODE to s.currencyCode,
                        C.COL_DAYS_IN_PERIOD to s.daysInPeriod.toString(),
                        C.COL_ROLLOVER_ENABLED to s.rollOverEnabled.flag(),
                        C.COL_ROLLOVER_CARRY_FORWARD to s.rollOverCarryForward.flag(),
                        C.COL_REMAINING_BUDGET_STRATEGY to s.remainingBudgetStrategy.name,
                        C.COL_CURRENT_PERIOD_STARTED_AT to meta.currentPeriodStartedAtMillis.toString(),
                        C.COL_CURRENT_PERIOD_ID to meta.currentPeriodId.toString(),
                        C.COL_CREDIT_CARD_CUTOFF_DAY to s.creditCardCutoffDay?.toString().orEmpty(),
                        C.COL_SPLIT_MODE to s.splitMode.name,
                    )
                }

                pockets.forEach { pocket ->
                    printer.row(
                        C.COL_DATE to C.MARKER_POCKET,
                        C.COL_ID to pocket.id.toString(),
                        C.COL_CATEGORY to pocket.name,
                        C.COL_AMOUNT to pocket.target?.toPlainString().orEmpty(),
                        C.COL_END_DATE to pocket.targetDate?.format(dateFormatter).orEmpty(),
                        C.COL_CREATED_AT to pocket.createdAt.toString(),
                    )
                }

                archivedBudgets.forEach { archive ->
                    printer.row(
                        C.COL_DATE to C.MARKER_ARCHIVED,
                        C.COL_AMOUNT to archive.spentAmount.toPlainString(),
                        C.COL_PERIOD_ID to archive.periodId.toString(),
                        C.COL_CREATED_AT to archive.createdAt.toString(),
                        C.COL_BUDGET_TOTAL to archive.totalBudget.toPlainString(),
                        C.COL_BUDGET_PERIOD to archive.periodType.name,
                        C.COL_BUDGET_START_DATE to archive.startDate.format(dateFormatter),
                        C.COL_BUDGET_END_DATE to archive.endDate.format(dateFormatter),
                        C.COL_CURRENCY_CODE to archive.currencyCode,
                    )
                }

                paidOccurrences.forEach { occurrence ->
                    printer.row(
                        C.COL_DATE to C.MARKER_PAID_OCCURRENCE,
                        C.COL_ID to occurrence.transactionId.toString(),
                        C.COL_BUDGET_PERIOD to occurrence.status.name,
                        C.COL_BUDGET_START_DATE to occurrence.occurrenceDate.format(dateFormatter),
                    )
                }

                transactions.forEach { tx ->
                    val txDate = tx.date ?: return@forEach
                    printer.row(
                        C.COL_DATE to txDate.format(dateTimeFormatter),
                        C.COL_AMOUNT to tx.amount.toPlainString(),
                        C.COL_CATEGORY to tx.comment,
                        C.COL_EXTRA_NOTE to tx.note,
                        C.COL_IS_RECURRENT to tx.isRecurrent.flag(),
                        C.COL_FREQUENCY to tx.recurrentFrequency?.name.orEmpty(),
                        C.COL_END_DATE to tx.recurrentEndDate?.toLocalDate()?.format(dateFormatter).orEmpty(),
                        C.COL_SUB_DAY to tx.subscriptionDay?.toString().orEmpty(),
                        C.COL_ID to tx.id.toString(),
                        C.COL_IS_CREDIT to tx.isCredit.flag(),
                        C.COL_IS_CREDIT_PAID to tx.isCreditPaid.flag(),
                        C.COL_PERIOD_ID to tx.periodId.toString(),
                        C.COL_CREATED_AT to tx.createdAt.toString(),
                        C.COL_IS_ADJUSTMENT to tx.isAdjustment.flag(),
                        C.COL_POCKET_ID to tx.pocketId?.toString().orEmpty(),
                    )
                }
                printer.flush()
            }
        }
    }

    private fun CSVPrinter.row(vararg values: Pair<String, String>) {
        val byColumn = values.toMap()
        printRecord(C.HEADERS.map { byColumn[it].orEmpty() })
    }

    private fun Boolean.flag() = if (this) "1" else "0"
}
