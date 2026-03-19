package com.serranoie.app.minus.data.csv

import com.serranoie.app.minus.domain.model.RecurrentFrequency
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

    fun parse(inputStream: InputStream): Pair<List<CsvTransactionRow>, List<String>> {
        val errors = mutableListOf<String>()
        val rows = mutableListOf<CsvTransactionRow>()

        val format = CSVFormat.DEFAULT.builder()
            .setHeader(*MinusCsvContract.HEADERS)
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .build()

        InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
            val parser = format.parse(reader)
            parser.forEachIndexed { index, record ->
                val lineNo = index + 2
                runCatching {
                    val row = parseRecord(record.toMap())
                    if (row.amount <= BigDecimal.ZERO) {
                        errors.add("Line $lineNo discarded: amount must be > 0")
                        null
                    } else row
                }.onSuccess { parsed ->
                    if (parsed != null) rows.add(parsed)
                }.onFailure { throwable ->
                    errors.add("Line $lineNo discarded: ${throwable.message}")
                }
            }
        }

        return rows to errors
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

        return CsvTransactionRow(
            id = id,
            date = date,
            amount = amount,
            comment = comment,
            isRecurrent = isRecurrent,
            frequency = frequency,
            endDate = endDate,
            subscriptionDay = subDay
        )
    }

    private fun Map<String, String>.valueOf(key: String): String {
        return this[key]?.trim().orEmpty()
    }
}
