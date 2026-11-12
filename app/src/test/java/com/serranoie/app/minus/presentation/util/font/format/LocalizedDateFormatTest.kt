package com.serranoie.app.minus.presentation.util.font.format

import app.cash.paparazzi.Paparazzi
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.time.CalendarSystem
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import java.util.TimeZone

class LocalizedDateFormatTest {
    @get:Rule
    val paparazzi = Paparazzi()

    private val date = LocalDate.of(2026, 10, 13)

    @Test
    fun `gregorian locale keeps gregorian dates`() {
        assertThat(date.formatLocalized("dd MMMM yyyy", Locale.US)).isEqualTo("13 October 2026")
    }

    @Test
    fun `persian iran locale uses jalali dates`() {
        assertThat(date.formatLocalized("d MMMM yyyy", Locale.forLanguageTag("fa-IR")))
            .isEqualTo("۲۱ مهر ۱۴۰۵")
    }

    @Test
    fun `persian language without region uses jalali dates`() {
        assertThat(date.formatLocalized("d MMMM yyyy", Locale.forLanguageTag("fa")))
            .isEqualTo("۲۱ مهر ۱۴۰۵")
    }

    @Test
    fun `persian calendar extension applies to other languages`() {
        assertThat(date.formatLocalized("d MMMM yyyy", Locale.forLanguageTag("en-US-u-ca-persian")))
            .isEqualTo("21 Mehr 1405")
    }

    @Test
    fun `non persian locale preferring another calendar stays gregorian`() {
        assertThat(date.formatLocalized("d M yyyy", Locale.forLanguageTag("ar-SA-u-nu-latn")))
            .isEqualTo("13 10 2026")
    }

    @Test
    fun `persian year rolls over at nowruz not january`() {
        val locale = Locale.forLanguageTag("en-US-u-ca-persian")
        assertThat(LocalDate.of(2026, 3, 20).formatLocalized("d MMM yyyy", locale)).isEqualTo("29 Esfand 1404")
        assertThat(LocalDate.of(2026, 3, 21).formatLocalized("d MMM yyyy", locale)).isEqualTo("1 Farvardin 1405")
    }

    @Test
    fun `date time keeps its time of day`() {
        assertThat(LocalDateTime.of(2026, 10, 13, 23, 45).formatLocalized("dd MMM HH:mm", Locale.US))
            .isEqualTo("13 Oct 23:45")
    }

    @Test
    fun `interleaved patterns and locales never borrow each other's format`() {
        val persian = Locale.forLanguageTag("fa-IR")
        repeat(3) {
            assertThat(date.formatLocalized("dd MMMM yyyy", Locale.US)).isEqualTo("13 October 2026")
            assertThat(date.formatLocalized("d MMMM yyyy", persian)).isEqualTo("۲۱ مهر ۱۴۰۵")
            assertThat(date.formatLocalized("yyyy", Locale.US)).isEqualTo("2026")
            assertThat(date.formatLocalized("dd MMMM", Locale.forLanguageTag("es-MX"))).isEqualTo("13 octubre")
        }
    }

    @Test
    fun `a time zone change after formatting keeps the local time of day`() {
        val original = TimeZone.getDefault()
        try {
            val time = LocalDateTime.of(2026, 10, 13, 23, 45)
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            assertThat(time.formatLocalized("dd MMM HH:mm", Locale.US)).isEqualTo("13 Oct 23:45")
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
            assertThat(time.formatLocalized("dd MMM HH:mm", Locale.US)).isEqualTo("13 Oct 23:45")
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun `persian calendar math agrees with icu for every day across a century`() {
        val locale = Locale.forLanguageTag("en-US-u-ca-persian")
        val persian = CalendarSystem.PERSIAN
        var day = LocalDate.of(1990, 1, 1)
        while (!day.isAfter(LocalDate.of(2090, 12, 31))) {
            assertThat(persian.dayOfMonth(day).toString()).isEqualTo(day.formatLocalized("d", locale))
            assertThat(persian.lastDayOfMonth(day).plusDays(1).formatLocalized("d", locale)).isEqualTo("1")
            day = day.plusDays(1)
        }
    }
}
