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
private val FULL_DATE_COMPARE = Regex("^[<>]=?\\d{1,4}([/.-])\\d{1,4}\\1\\d{1,4}$")
private val EXAMPLE_LABEL = Regex("^[^:：\"“”„«»]{1,24}[:：]\\s+")
private val NUMERIC_DATE_SEPARATOR = Regex("[/.-]")
private val QUOTED = Regex("[\"“”„«»]([^\"“”„«»]*)[\"“”„«»]")
private val DATE_COMPARE = Regex("^([<>]=?)(.+)$")
private const val RANGE_DOTS = ".."
private const val MIN_MONTH_PREFIX = 3
private const val THOUSANDS_GROUP = 3
private const val MONTH_NAME_YEAR = 2000
private const val MONTH_SAMPLES = 26
private const val MONTH_SAMPLE_STEP_DAYS = 15L
private const val YEAR_DIGITS = 4
private const val MAX_DAY = 31
private const val MAX_MONTH = 12
private const val TWO_DIGIT_YEAR = 100
private const val NO_DAY_LOW = 0
private const val NO_DAY_HIGH = 99
private const val MONTH_KEY = 100
private const val YEAR_KEY = 10_000

private val searchDates = ConcurrentHashMap<Pair<Locale, LocalDate>, SearchDate>()
private val gregorianMonthNames = ConcurrentHashMap<Pair<Locale, Int>, List<String>>()
private val monthLookups = ConcurrentHashMap<Locale, List<Pair<Int, List<String>>>>()

private val CONNECTOR_WORDS = setOf(
    "on", "at", "in", "of", "the", "from",
    "de", "del", "el", "la", "en", "a", "do", "da", "di", "le", "du", "au", "em", "no", "na", "il",
    "am", "im", "vom", "von", "der", "den",
    "в", "от", "на", "στις", "στο", "της",
)

private val RANGE_WORDS = setOf(
    RANGE_DOTS, "to", "until", "till", "a", "al", "hasta", "ate", "bis", "fino", "au", "по", "до", "εως", "تا",
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

private class PartialDate(val year: Int?, val month: Int?, val day: Int?)

private class DateBound(val year: Int?, val month: Int, val day: Int?)

private class DateFilter(
    val low: DateBound?,
    val lowStrict: Boolean,
    val high: DateBound?,
    val highStrict: Boolean,
) {
    private val yearly = low?.year != null || high?.year != null

    fun matches(date: SearchDate): Boolean {
        val key = (if (yearly) date.year * YEAR_KEY else 0) + date.month * MONTH_KEY + date.day
        val aboveLow = low?.let { if (lowStrict) key > it.key(NO_DAY_HIGH) else key >= it.key(NO_DAY_LOW) } ?: true
        val belowHigh = high?.let { if (highStrict) key < it.key(NO_DAY_LOW) else key <= it.key(NO_DAY_HIGH) } ?: true
        val wraps = !yearly && low != null && high != null && low.key(NO_DAY_LOW) > high.key(NO_DAY_HIGH)
        return if (wraps) aboveLow || belowHigh else aboveLow && belowHigh
    }

    private fun DateBound.key(missingDay: Int) =
        (if (yearly) (year ?: 0) * YEAR_KEY else 0) + month * MONTH_KEY + (day ?: missingDay)
}

private class DateContext(
    val locale: Locale,
    val order: List<Char>,
    val months: List<Pair<Int, List<String>>>,
)

internal fun historySearchMatcher(
    query: String,
    locale: Locale,
    categoryNames: Map<Long, String> = emptyMap(),
): (Transaction) -> Boolean {
    val normalized = query.searchNormalized(locale).replace(EXAMPLE_LABEL, "")
    val phrases = QUOTED.findAll(normalized).map { it.groupValues[1].trim() }.filter { it.isNotEmpty() }.toList()
    val allTokens = normalized.replace(QUOTED, " ")
        .replace(RANGE_DOTS, " $RANGE_DOTS ")
        .replace(AMOUNT_GLUE, "$1")
        .split(TOKEN_SEPARATOR)
        .map { it.trimEnd(',') }
        .filter { it.isNotEmpty() }
    val context = DateContext(locale, numericDateOrder(locale), monthLookup(locale))
    val (ranges, remaining) = extractDateRanges(allTokens, context)
    val tokens = remaining.filterNot { it in CONNECTOR_WORDS || it == RANGE_DOTS }
        .ifEmpty { if (ranges.isEmpty() && phrases.isEmpty()) remaining else emptyList() }
    val fullDateCompares = tokens.filter { FULL_DATE_COMPARE.matches(it) }
        .mapNotNull { token -> token.toDateCompare(context)?.let { token to it } }
        .toMap()
    val amountFilters = tokens.filterNot { it in fullDateCompares }
        .mapNotNull { token -> token.toAmountFilter()?.let { token to it } }
        .toMap()
    val dateCompares = fullDateCompares + tokens.filterNot { it in amountFilters || it in fullDateCompares }
        .mapNotNull { token -> token.toDateCompare(context)?.let { token to it } }
        .toMap()
    val categories = categoryNames.mapValues { it.value.searchNormalized(locale) }

    return { transaction ->
        val category = transaction.categoryId?.let(categories::get).orEmpty()
        val text = "${transaction.comment} ${transaction.note}".searchNormalized(locale) + " " + category
        val date by lazy { transaction.date?.toLocalDate()?.let { searchDate(it, locale) } }
        phrases.all { text.contains(it) } &&
            ranges.all { range -> date?.let(range::matches) == true } &&
            tokens.all { token ->
                val amountFilter = amountFilters[token]
                val dateCompare = dateCompares[token]
                when {
                    amountFilter != null -> amountFilter.matches(transaction.amount)
                    dateCompare != null -> date?.let(dateCompare::matches) == true
                    NUMERIC_DATE.matches(token) -> date?.matchesNumeric(token, context.order) == true
                    token.all { it.isDigit() } -> text.contains(token) ||
                        date?.let { d -> token.toIntOrNull().let { it == d.day || it == d.year } } == true
                    else -> text.contains(token) ||
                        token.length >= MIN_MONTH_PREFIX && date?.monthNames?.any { it.startsWith(token) } == true
                }
            }
    }
}

private fun extractDateRanges(tokens: List<String>, context: DateContext): Pair<List<DateFilter>, List<String>> {
    val used = mutableSetOf<Int>()
    val ranges = mutableListOf<DateFilter>()
    for (index in tokens.indices) {
        if (index in used || tokens[index] !in RANGE_WORDS) continue
        val left = scanDate(tokens, (index - 1 downTo 0).filterNot { it in used }, context) ?: continue
        val right = scanDate(tokens, (index + 1 until tokens.size).filterNot { it in used }, context) ?: continue
        val range = rangeOf(left.first, right.first) ?: continue
        ranges += range
        used += index
        used += left.second
        used += right.second
    }
    return ranges to tokens.filterIndexed { index, _ -> index !in used }
}

private fun scanDate(tokens: List<String>, indices: List<Int>, context: DateContext): Pair<PartialDate, List<Int>>? {
    var year: Int? = null
    var month: Int? = null
    var day: Int? = null
    val consumed = mutableListOf<Int>()
    val pending = mutableListOf<Int>()
    for (index in indices) {
        val token = tokens[index]
        val number = token.toIntOrNull()
        val monthNumber = if (number == null) context.monthOf(token) else null
        when {
            token in CONNECTOR_WORDS || token in RANGE_WORDS -> {
                pending += index
                continue
            }
            consumed.isEmpty() && NUMERIC_DATE.matches(token) ->
                return token.toNumericDate(context)?.let { it to listOf(index) }
            number != null && token.length == YEAR_DIGITS && year == null -> year = number
            number != null && number in 1..MAX_DAY && day == null -> day = number
            monthNumber != null && month == null -> month = monthNumber
            else -> break
        }
        consumed += pending
        consumed += index
        pending.clear()
    }
    return if (consumed.isEmpty()) null else PartialDate(year, month, day) to consumed
}

private fun rangeOf(start: PartialDate, end: PartialDate): DateFilter? {
    val startMonth = start.month ?: end.month ?: return null
    val endMonth = end.month ?: startMonth
    val wraps = startMonth * MONTH_KEY + (start.day ?: NO_DAY_LOW) > endMonth * MONTH_KEY + (end.day ?: NO_DAY_HIGH)
    val startYear = start.year ?: end.year?.let { if (wraps) it - 1 else it }
    val endYear = end.year ?: start.year?.let { if (wraps) it + 1 else it }
    return DateFilter(
        low = DateBound(startYear, startMonth, start.day),
        lowStrict = false,
        high = DateBound(endYear, endMonth, end.day),
        highStrict = false,
    )
}

private fun String.toDateCompare(context: DateContext): DateFilter? {
    val match = DATE_COMPARE.matchEntire(this) ?: return null
    val operator = match.groupValues[1]
    val value = match.groupValues[2]
    val numeric = if (NUMERIC_DATE.matches(value)) value.toNumericDate(context) else null
    val bound = numeric?.month?.let { DateBound(numeric.year, it, numeric.day) }
        ?: context.monthOf(value)?.let { DateBound(null, it, null) }
        ?: return null
    return if (operator.startsWith(">")) {
        DateFilter(low = bound, lowStrict = operator == ">", high = null, highStrict = false)
    } else {
        DateFilter(low = null, lowStrict = false, high = bound, highStrict = operator == "<")
    }
}

private fun String.toNumericDate(context: DateContext): PartialDate? {
    val parts = split(NUMERIC_DATE_SEPARATOR).map { it.toInt() }
    val order = if (parts.size == 2) context.order - 'y' else context.order
    val fields = parts.zip(order).associate { (value, field) -> field to value }
    val day = fields['d']?.takeIf { it in 1..MAX_DAY } ?: return null
    val month = fields['M']?.takeIf { it in 1..MAX_MONTH } ?: return null
    val year = fields['y']?.let { if (it < TWO_DIGIT_YEAR) it + context.century() else it }
    return PartialDate(year, month, day)
}

private fun DateContext.century(): Int =
    searchDate(LocalDate.now(), locale).year / TWO_DIGIT_YEAR * TWO_DIGIT_YEAR

private fun DateContext.monthOf(token: String): Int? =
    if (token.length < MIN_MONTH_PREFIX) {
        null
    } else {
        months.firstOrNull { (_, names) -> names.any { it.startsWith(token) } }?.first
    }

private fun monthLookup(locale: Locale): List<Pair<Int, List<String>>> = monthLookups.getOrPut(locale) {
    (0 until MONTH_SAMPLES)
        .map { searchDate(LocalDate.of(MONTH_NAME_YEAR, 1, 1).plusDays(it * MONTH_SAMPLE_STEP_DAYS), locale) }
        .distinctBy { it.month }
        .map { it.month to it.monthNames }
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
