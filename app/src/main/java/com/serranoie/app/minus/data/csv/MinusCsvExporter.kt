package com.serranoie.app.minus.data.csv

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

    fun export(transactions: List<Transaction>, outputStream: OutputStream) {
        val format = CSVFormat.DEFAULT.builder()
            .setHeader(*MinusCsvContract.HEADERS)
            .build()

        OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
            CSVPrinter(writer, format).use { printer ->
                transactions.forEach { tx ->
                    val txDate = tx.date ?: return@forEach
                    val frequency = tx.recurrentFrequency?.name.orEmpty()
                    val endDate = tx.recurrentEndDate?.toLocalDate()?.format(dateFormatter).orEmpty()
                    val subDay = tx.subscriptionDay?.toString().orEmpty()

                    printer.printRecord(
                        txDate.format(dateTimeFormatter),
                        tx.amount.toPlainString(),
                        tx.comment,
                        if (tx.isRecurrent) "1" else "0",
                        frequency,
                        endDate,
                        subDay,
                        tx.id.toString()
                    )
                }
                printer.flush()
            }
        }
    }
}
