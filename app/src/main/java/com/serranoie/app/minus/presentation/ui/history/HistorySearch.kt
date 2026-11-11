package com.serranoie.app.minus.presentation.ui.history

import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.time.CalendarSystem
import com.serranoie.app.minus.presentation.util.font.format.formatLocalized
import java.math.BigDecimal
import java.text.Normalizer
import java.time.LocalDate
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private val TOKEN_SEPARATOR = Regex("\\s+")
private val AMOUNT_GLUE = Regex("([<>]=?|\\p{Sc})\\s+(?=[\\d\\p{Sc}])")
private val AMOUNT_COMPARE = Regex("^([<>]=?)\\p{L}{0,3}\\p{Sc}?([\\d.,]+)\\p{Sc}?$")
private val AMOUNT_EXACT = Regex("^(?:\\p{L}{0,3}\\p{Sc}([\\d.,]+)|([\\d.,]+)\\p{Sc})$")
private val COMBINING_MARKS = Regex("\\p{Mn}+")
private val NUMERIC_DATE = Regex("^\\d{1,4}([/.-])\\d{1,4}(\\1\\d{1,4})?$")
private val NUMERIC_DATE_SEPARATOR = Regex("[/.-]")
private const val MIN_MONTH_PREFIX = 3
private const val THOUSANDS_GROUP = 3
private const val MONTH_NAME_YEAR = 2000

private val searchDates = ConcurrentHashMap<Pair<Locale, LocalDate>, SearchDate>()
private val gregorianMonthNames = ConcurrentHashMap<Pair<Locale, Int>, List<String>>()

private val CONNECTOR_WORDS = setOf(
    "on", "at", "in", "of", "the", "from",
    "de", "del", "el", "la", "en", "a", "do", "da", "di", "le", "du", "au", "em", "no", "na", "il",
    "am", "im", "vom", "von", "der", "den",
    "в", "от", "на", "στις", "στο", "της",
)

private class AmountFilter(val operator: String, val value: BigDecimal) {
    fun matches(amount: BigDecimal): Boolean {
        val comparison = amount.abs().compareTo(value)
        return when (operator) {
            ">" -> comparison > 0
            ">=" -> comparison >= 0
            "<" -> comparison < 0
            "<=" -> comparison <= 0
            else -> comparison == 0
        }
    }
}

private class SearchDate(
    val day: Int,
    val month: Int,
    val year: Int,
    val monthNames: List<String>,
)

internal fun historySearchMatcher(query: String, locale: Locale): (Transaction) -> Boolean {
    val allTokens = query.searchNormalized(locale)
        .replace(AMOUNT_GLUE, "$1")
        .split(TOKEN_SEPARATOR)
        .map { it.trimEnd(',') }
        .filter { it.isNotEmpty() }
    val tokens = allTokens.filterNot { it in CONNECTOR_WORDS }.ifEmpty { allTokens }
    val amountFilters = tokens.mapNotNull { token -> token.toAmountFilter()?.let { token to it } }.toMap()
    val numericOrder = numericDateOrder(locale)

    return { transaction ->
        val text = "${transaction.comment} ${transaction.note}".searchNormalized(locale)
        val date by lazy { transaction.date?.toLocalDate()?.let { searchDate(it, locale) } }
        tokens.all { token ->
            val amountFilter = amountFilters[token]
            when {
                amountFilter != null -> amountFilter.matches(transaction.amount)
                NUMERIC_DATE.matches(token) -> date?.matchesNumeric(token, numericOrder) == true
                token.all { it.isDigit() } -> text.contains(token) ||
                    date?.let { d -> token.toIntOrNull().let { it == d.day || it == d.year } } == true
                else -> text.contains(token) ||
                    token.length >= MIN_MONTH_PREFIX && date?.monthNames?.any { it.startsWith(token) } == true
            }
        }
    }
}

private fun String.toAmountFilter(): AmountFilter? {
    AMOUNT_COMPARE.matchEntire(this)?.let { match ->
        return match.groupValues[2].toSearchAmount()?.let { AmountFilter(match.groupValues[1], it) }
    }
    val exact = AMOUNT_EXACT.matchEntire(this) ?: return null
    return exact.groupValues[1].ifEmpty { exact.groupValues[2] }.toSearchAmount()?.let { AmountFilter("=", it) }
}

private fun String.toSearchAmount(): BigDecimal? {
    val lastSeparator = indexOfLast { it == '.' || it == ',' }
    val decimals = if (lastSeparator < 0) 0 else length - lastSeparator - 1
    val digits = filter { it.isDigit() }
    return if (decimals == 0 || decimals == THOUSANDS_GROUP) {
        digits.toBigDecimalOrNull()
    } else {
        "${digits.dropLast(decimals)}.${digits.takeLast(decimals)}".toBigDecimalOrNull()
    }
}

private fun searchDate(date: LocalDate, locale: Locale): SearchDate = searchDates.getOrPut(locale to date) {
    if (CalendarSystem.of(locale) == CalendarSystem.GREGORIAN) {
        SearchDate(
            day = date.dayOfMonth,
            month = date.monthValue,
            year = date.year,
            monthNames = gregorianMonthNames.getOrPut(locale to date.monthValue) {
                monthNames(LocalDate.of(MONTH_NAME_YEAR, date.monthValue, 1), locale)
            },
        )
    } else {
        SearchDate(
            day = date.formatLocalized("d", locale).searchNormalized(locale).toInt(),
            month = date.formatLocalized("M", locale).searchNormalized(locale).toInt(),
            year = date.formatLocalized("y", locale).searchNormalized(locale).toInt(),
            monthNames = monthNames(date, locale),
        )
    }
}

private fun monthNames(date: LocalDate, locale: Locale): List<String> =
    listOf("MMMM", "MMM", "LLLL", "LLL")
        .map { date.formatLocalized(it, locale).searchNormalized(locale).trimEnd('.') }
        .distinct()

private fun SearchDate.matchesNumeric(token: String, order: List<Char>): Boolean {
    val parts = token.split(NUMERIC_DATE_SEPARATOR).map { it.toInt() }
    val fields = if (parts.size == 2) order - 'y' else order
    return parts.zip(fields).all { (value, field) ->
        when (field) {
            'd' -> value == day
            'M' -> value == month
            else -> value == year || (value < 100 && value == year % 100)
        }
    }
}

private fun numericDateOrder(locale: Locale): List<Char> {
    val pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
        FormatStyle.SHORT, null, IsoChronology.INSTANCE, locale,
    )
    return listOf('d', 'M', 'y').sortedBy { field -> pattern.indexOf(field).takeIf { it >= 0 } ?: Int.MAX_VALUE }
}

private fun String.searchNormalized(locale: Locale): String =
    Normalizer.normalize(lowercase(locale), Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .map { if (it.isDigit()) it.digitToInt().digitToChar() else it }
        .joinToString("")
