package com.serranoie.app.minus.presentation.util.font.format

import android.icu.text.SimpleDateFormat as IcuSimpleDateFormat
import android.icu.util.TimeZone as IcuTimeZone
import android.icu.util.ULocale
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.time.CalendarSystem
import logcat.logcat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

const val DAY = 24 * 60 * 60 * 1000

fun LocalDate.toDate(): Date =
    Date(this.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000)

fun LocalDateTime.toDate(): Date = Date(
    this.toEpochSecond(
        ZoneId.systemDefault().rules.getOffset(this)
    ) * 1000
)

fun LocalDate.formatLocalized(pattern: String, locale: Locale = Locale.getDefault()): String =
    atStartOfDay().formatLocalized(pattern, locale)

fun LocalDateTime.formatLocalized(pattern: String, locale: Locale = Locale.getDefault()): String =
    toDate().formatLocalized(pattern, locale)

private data class IcuFormatKey(val pattern: String, val locale: Locale, val timeZone: String)

private val icuFormats = ConcurrentHashMap<IcuFormatKey, IcuSimpleDateFormat>()
private val timeFormatters = ConcurrentHashMap<Pair<String, Locale>, DateTimeFormatter>()

fun Date.formatLocalized(pattern: String, locale: Locale = Locale.getDefault()): String {
    val key = IcuFormatKey(pattern, locale, TimeZone.getDefault().id)
    val format = icuFormats.getOrPut(key) {
        val calendar = if (CalendarSystem.of(locale) == CalendarSystem.PERSIAN) "persian" else "gregorian"
        val uLocale = ULocale.forLocale(locale).setKeywordValue("calendar", calendar)
        IcuSimpleDateFormat(pattern, uLocale).apply { timeZone = IcuTimeZone.getTimeZone(key.timeZone) }
    }
    return synchronized(format) { format.format(this) }
}

fun localizedFormatter(pattern: String, locale: Locale = Locale.getDefault()): (LocalDate) -> String =
    { it.formatLocalized(pattern, locale) }


fun Date.toLocalDate(): LocalDate = this.toInstant()
    .atZone(ZoneId.systemDefault())
    .toLocalDate()

fun Date.toLocalDateTime(): LocalDateTime = this.toInstant()
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime()

fun countDaysToToday(finishDate: Date): Int {
    val today = LocalDate.now()
    val end = finishDate.toLocalDate()
    if (!today.isBefore(end)) {
        return 0
    }
    val result = ChronoUnit.DAYS.between(today, end).toInt()
    logcat("DateFormatting") { "countDaysToToday: finishDate=$finishDate, today=$today, result=$result" }
    return result
}

fun countDays(toDate: Date, fromDate: Date): Int {
    val fromDateRound = roundToDay(fromDate)
    val toDateRound = roundToDay(toDate)

    val daysFrom = ceil(fromDateRound.time / DAY.toDouble()).toInt()
    val daysTo = ceil(toDateRound.time / DAY.toDouble()).toInt()

    val result = daysTo - daysFrom + 1
    logcat("DateFormatting - ISAAC") { "countDays: fromDate=$fromDate (rounded=$fromDateRound, daysFrom=$daysFrom), toDate=$toDate (rounded=$toDateRound, daysTo=$daysTo), result=$result" }
    return result
}

fun isToday(date: Date): Boolean {
    return isSameDay(date.time, Date().time)
}

fun isSameDay(timestampA: Long, timestampB: Long): Boolean {
    return isSameDay(Date(timestampA), Date(timestampB))
}

fun isSameDay(dateA: Date, dateB: Date): Boolean {
    return roundToDay(dateA) == roundToDay(dateB)
}

@Composable
fun getWeek(): Array<DayOfWeek> {
    val locale = LocalConfiguration.current.locales[0]

    return getWeek(locale)
}

fun getWeek(locale: Locale): Array<DayOfWeek> {
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    return Array(7) { firstDayOfWeek.plus(it.toLong()) }
}

@Composable
fun prettyDate(
    date: Date,
    pattern: String,
    simplifyIfToday: Boolean = true,
): String {
    val locale = LocalConfiguration.current.locales[0]

    var final = ""

    final += if (simplifyIfToday && date.toLocalDate() == LocalDate.now()) {
        stringResource(R.string.today)
    } else {
        date.formatLocalized(pattern, locale)
    }

    return final.trim()
}

@Composable
fun prettyDate(
    date: LocalDateTime?,
    showTime: Boolean = true,
    forceShowDate: Boolean = false,
    forceHideDate: Boolean = false,
    forceShowYear: Boolean = false,
    human: Boolean = false,
    shortMonth: Boolean = false,
): String {
    val locale = LocalConfiguration.current.locales[0]

    val dateWithMonthPattern = if (shortMonth) "dd MMM" else "dd MMMM"
    val timePattern = if (DateFormat.is24HourFormat(LocalContext.current)) "HH:mm" else "hh:mm a"
    val timeFormatter = timeFormatters.getOrPut(timePattern to locale) { DateTimeFormatter.ofPattern(timePattern, locale) }


    val currentLocalDate = LocalDate.now()
    val isToday = date?.toLocalDate() == currentLocalDate

    val datePart = if (human && isToday) {
        stringResource(R.string.today)
    } else if (!forceHideDate && (!isToday || !showTime || forceShowDate)) {
        val currentYear = currentLocalDate.formatLocalized("yyyy", locale)
        val convertedYear = date?.formatLocalized("yyyy", locale)
        var dp = date?.formatLocalized(dateWithMonthPattern, locale).orEmpty()
        if (convertedYear != currentYear || forceShowYear) {
            dp += " $convertedYear"
        }
        dp
    } else ""

    val timePart = if (showTime && date != null) " ${timeFormatter.format(date)}" else ""

    return "$datePart$timePart".trim()
}

@Composable
fun prettyMonth(monthStart: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    val sameYear = monthStart.formatLocalized("yyyy", locale) == LocalDate.now().formatLocalized("yyyy", locale)
    return monthStart.formatLocalized(if (sameYear) "LLLL" else "LLLL yyyy", locale)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

@Composable
fun prettyWeekDay(dayOfWeek: DayOfWeek): String {
    val locale = LocalConfiguration.current.locales[0]

    val dayOfWeekFormatter = if (locale.language == "ru") {
        DateTimeFormatter.ofPattern("ccc", locale)
    } else {
        DateTimeFormatter.ofPattern("ccccc", locale)
    }

    return dayOfWeekFormatter.format(dayOfWeek).uppercase(locale)
}

fun roundToDay(date: Date): Date {
    val calendar = Calendar.getInstance()
    calendar.time = date

    return Calendar
        .Builder()
        .setTimeZone(calendar.timeZone)
        .setDate(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        .build()
        .time
}

fun calculateDaysToCutoff(cutoffDay: Int, fromDate: LocalDate = LocalDate.now()): Int {
    val cutoffThisMonth = try {
        fromDate.withDayOfMonth(cutoffDay)
    } catch (e: Exception) {
        fromDate.withDayOfMonth(fromDate.lengthOfMonth())
    }

    return if (!fromDate.isAfter(cutoffThisMonth)) {
        ChronoUnit.DAYS.between(fromDate, cutoffThisMonth).toInt()
    } else {
        val nextMonth = fromDate.plusMonths(1)
        val cutoffNextMonth = try {
            nextMonth.withDayOfMonth(cutoffDay)
        } catch (e: Exception) {
            nextMonth.withDayOfMonth(nextMonth.lengthOfMonth())
        }
        ChronoUnit.DAYS.between(fromDate, cutoffNextMonth).toInt()
    }
}

