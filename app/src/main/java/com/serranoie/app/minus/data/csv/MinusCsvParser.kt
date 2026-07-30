package com.serranoie.app.minus.data.csv

import com.serranoie.app.minus.domain.model.ArchivedBudget
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import org.apache.commons.csv.CSVFormat
import java.io.InputStream
import java.io.InputStreamReader
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MinusCsvParser {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun parse(inputStream: InputStream): CsvImportPayload {
        val errors = mutableListOf<String>()
        val rows = mutableListOf<CsvTransactionRow>()
        val archivedBudgets = mutableListOf<ArchivedBudget>()
        var metadata: CsvBackupMetadata? = null

        val format = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .get()

        InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
            val parser = format.parse(reader)
            parser.forEachIndexed { index, record ->
                val lineNo = index + 2
                val raw = record.toMap()

                val dateRaw = raw.valueOf(MinusCsvContract.COL_DATE)
                if (dateRaw == MinusCsvContract.MARKER_META) {
                    runCatching { parseMetadata(raw) }
                        .onSuccess { parsedMeta -> metadata = parsedMeta }
                        .onFailure { throwable -> errors.add("Line $lineNo metadata discarded: ${throwable.message}") }
                    return@forEachIndexed
                }

                if (dateRaw == MinusCsvContract.MARKER_ARCHIVED) {
                    runCatching { parseArchivedBudget(raw) }
                        .onSuccess { archivedBudgets.add(it) }
                        .onFailure { throwable -> errors.add("Line $lineNo archived budget discarded: ${throwable.message}") }
                    return@forEachIndexed
                }

                runCatching {
                    val row = parseRecord(raw)
                    if (row.amount <= BigDecimal.ZERO) {
                        errors.add("Line $lineNo discarded: amount must be > 0")
                        null
                    } else {
                        row
                    }
                }.onSuccess { parsed ->
                    if (parsed != null) rows.add(parsed)
                }.onFailure { throwable ->
                    errors.add("Line $lineNo discarded: ${throwable.message}")
                }
            }
        }

        return CsvImportPayload(
            rows = rows,
            metadata = metadata,
            archivedBudgets = archivedBudgets,
            errors = errors
        )
    }

    private fun parseRecord(raw: Map<String, String>): CsvTransactionRow {
        val date = LocalDateTime.parse(raw.valueOf(MinusCsvContract.COL_DATE), dateTimeFormatter)
        val amount = raw.valueOf(MinusCsvContract.COL_AMOUNT).toBigDecimal()
        val comment = raw.valueOf(MinusCsvContract.COL_COMMENT)

        val isRecurrent = raw.valueOf(MinusCsvContract.COL_IS_RECURRENT).trim() == "1"
        val frequency = raw.valueOf(MinusCsvContract.COL_FREQUENCY)
            .takeIf { it.isNotBlank() }
            ?.let { RecurrentFrequency.valueOf(it) }

        val endDate = raw.valueOf(MinusCsvContract.COL_END_DATE)
            .takeIf { it.isNotBlank() }
            ?.let { LocalDate.parse(it, dateFormatter) }

        val subDay = raw.valueOf(MinusCsvContract.COL_SUB_DAY)
            .takeIf { it.isNotBlank() }
            ?.toInt()
            ?.takeIf { it in 1..31 }

        val id = raw.valueOf(MinusCsvContract.COL_ID)
            .toLongOrNull()
            ?.coerceAtLeast(0L)
            ?: 0L

        val isCredit = raw.valueOf(MinusCsvContract.COL_IS_CREDIT).trim() == "1"
        val isCreditPaid = raw.valueOf(MinusCsvContract.COL_IS_CREDIT_PAID).trim() == "1"

        val periodId = raw.valueOf(MinusCsvContract.COL_PERIOD_ID)
            .toLongOrNull()
            ?.coerceAtLeast(0L)
            ?: 0L

        return CsvTransactionRow(
            id = id,
            date = date,
            amount = amount,
            comment = comment,
            isRecurrent = isRecurrent,
            frequency = frequency,
            endDate = endDate,
            subscriptionDay = subDay,
            isCredit = isCredit,
            isCreditPaid = isCreditPaid,
            periodId = periodId,
        )
    }

    private fun parseArchivedBudget(raw: Map<String, String>): ArchivedBudget {
        val spentAmount = raw.valueOf(MinusCsvContract.COL_AMOUNT).toBigDecimal()
        val periodId = raw.valueOf(MinusCsvContract.COL_PERIOD_ID).toLong()
        val createdAt = raw.valueOf(MinusCsvContract.COL_CREATED_AT)
            .toLongOrNull() ?: System.currentTimeMillis()
        val totalBudget = raw.valueOf(MinusCsvContract.COL_BUDGET_TOTAL).toBigDecimal()
        val periodType = BudgetPeriod.valueOf(raw.valueOf(MinusCsvContract.COL_BUDGET_PERIOD))
        val startDate = LocalDate.parse(raw.valueOf(MinusCsvContract.COL_BUDGET_START_DATE), dateFormatter)
        val endDate = LocalDate.parse(raw.valueOf(MinusCsvContract.COL_BUDGET_END_DATE), dateFormatter)
        val currencyCode = raw.valueOf(MinusCsvContract.COL_CURRENCY_CODE).ifBlank { "USD" }

        return ArchivedBudget(
            periodId = periodId,
            totalBudget = totalBudget,
            spentAmount = spentAmount,
            startDate = startDate,
            endDate = endDate,
            currencyCode = currencyCode,
            periodType = periodType,
            createdAt = createdAt
        )
    }

    private fun parseMetadata(raw: Map<String, String>): CsvBackupMetadata {
        val totalBudget = raw.valueOf(MinusCsvContract.COL_BUDGET_TOTAL).toBigDecimal()
        val period = BudgetPeriod.valueOf(raw.valueOf(MinusCsvContract.COL_BUDGET_PERIOD))
        val startDate = LocalDate.parse(raw.valueOf(MinusCsvContract.COL_BUDGET_START_DATE), dateFormatter)
        val endDate = raw.valueOf(MinusCsvContract.COL_BUDGET_END_DATE)
            .takeIf { it.isNotBlank() }
            ?.let { LocalDate.parse(it, dateFormatter) }
        val currencyCode = raw.valueOf(MinusCsvContract.COL_CURRENCY_CODE).ifBlank { "USD" }
        val daysInPeriod = raw.valueOf(MinusCsvContract.COL_DAYS_IN_PERIOD).toIntOrNull()?.coerceAtLeast(1) ?: 1
        val rollOverEnabled = raw.valueOf(MinusCsvContract.COL_ROLLOVER_ENABLED) == "1"
        val rollOverCarryForward = raw.valueOf(MinusCsvContract.COL_ROLLOVER_CARRY_FORWARD) == "1"
        val strategy = raw.valueOf(MinusCsvContract.COL_REMAINING_BUDGET_STRATEGY)
            .takeIf { it.isNotBlank() }
            ?.let {
                try {
                    RemainingBudgetStrategy.valueOf(it)
                } catch (_: Exception) {
                    RemainingBudgetStrategy.ASK_ALWAYS
                }
            } ?: RemainingBudgetStrategy.ASK_ALWAYS

        val currentPeriodStartedAtMillis = raw.valueOf(MinusCsvContract.COL_CURRENT_PERIOD_STARTED_AT)
            .toLongOrNull()
            ?.coerceAtLeast(0L)
            ?: 0L
        val currentPeriodId = raw.valueOf(MinusCsvContract.COL_CURRENT_PERIOD_ID)
            .toLongOrNull()
            ?.coerceAtLeast(0L)
            ?: 0L

        val splitMode = raw.valueOf(MinusCsvContract.COL_SPLIT_MODE)
            .takeIf { it.isNotBlank() }
            ?.let {
                try {
                    BudgetSplitMode.valueOf(it)
                } catch (_: Exception) {
                    BudgetSplitMode.STATIC
                }
            } ?: BudgetSplitMode.STATIC

        return CsvBackupMetadata(
            budgetSettings = BudgetSettings(
                totalBudget = totalBudget,
                period = period,
                startDate = startDate,
                endDate = endDate,
                currencyCode = currencyCode,
                daysInPeriod = daysInPeriod,
                rollOverEnabled = rollOverEnabled,
                rollOverCarryForward = rollOverCarryForward,
                remainingBudgetStrategy = strategy,
                creditCardCutoffDay = raw.valueOf(MinusCsvContract.COL_CREDIT_CARD_CUTOFF_DAY)
                    .toIntOrNull()
                    ?.takeIf { it in 1..31 },
                splitMode = splitMode,
            ),
            currentPeriodStartedAtMillis = currentPeriodStartedAtMillis,
            currentPeriodId = currentPeriodId,
        )
    }

    private fun Map<String, String>.valueOf(key: String): String {
        return this[key]?.trim().orEmpty()
    }
}
