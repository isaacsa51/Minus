package com.serranoie.app.minus.presentation.ui.history

import app.cash.paparazzi.Paparazzi
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.Transaction
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Locale

class HistorySearchTest {
    @get:Rule
    val paparazzi = Paparazzi()

    private val spanish = Locale.forLanguageTag("es-MX")
    private val english = Locale.US

    private fun txn(
        comment: String,
        note: String = "",
        date: LocalDateTime? = LocalDateTime.of(2026, 10, 12, 13, 0),
        amount: String = "1",
        categoryId: Long? = null,
    ) = Transaction(amount = BigDecimal(amount), comment = comment, note = note, date = date, categoryId = categoryId)

    private fun on(month: Int, day: Int, year: Int = 2026) = txn("Comida", date = LocalDateTime.of(year, month, day, 13, 0))

    private fun matches(
        query: String,
        locale: Locale,
        transaction: Transaction,
        categories: Map<Long, String> = emptyMap(),
    ) = historySearchMatcher(query, locale, categories)(transaction)

    @Test
    fun `numeric dates follow the locale day and month order`() {
        val t = txn("Comida")
        assertThat(matches("12/10/2026", spanish, t)).isTrue()
        assertThat(matches("12/10", spanish, t)).isTrue()
        assertThat(matches("12-10-26", spanish, t)).isTrue()
        assertThat(matches("10/12/2026", spanish, t)).isFalse()
        assertThat(matches("10/12/2026", english, t)).isTrue()
        assertThat(matches("12/10/2025", spanish, t)).isFalse()
    }

    @Test
    fun `month names are read in the current language`() {
        val t = txn("Comida")
        assertThat(matches("12 octubre", spanish, t)).isTrue()
        assertThat(matches("12 de octubre", spanish, t)).isTrue()
        assertThat(matches("oct", spanish, t)).isTrue()
        assertThat(matches("12 october", english, t)).isTrue()
        assertThat(matches("october 12", english, t)).isTrue()
        assertThat(matches("12 noviembre", spanish, t)).isFalse()
        assertThat(matches("13 octubre", spanish, t)).isFalse()
    }

    @Test
    fun `text and date combine and connector words are ignored`() {
        val food = txn("Food")
        assertThat(matches("Food on 12 october", english, food)).isTrue()
        assertThat(matches("food on 12 november", english, food)).isFalse()
        assertThat(matches("rent on 12 october", english, food)).isFalse()
        assertThat(matches("comida el 12 de octubre", spanish, txn("Comida"))).isTrue()
    }

    @Test
    fun `text search ignores case and accents and still reads the note`() {
        assertThat(matches("cafe", spanish, txn("Café"))).isTrue()
        assertThat(matches("COFFEE", english, txn("Lunch", note = "coffee with Ana"))).isTrue()
        assertThat(matches("coffee beans", english, txn("Beans", note = "for coffee"))).isTrue()
    }

    @Test
    fun `a transaction without a date only matches on text`() {
        val undated = txn("Gym", date = null)
        assertThat(matches("gym", english, undated)).isTrue()
        assertThat(matches("12 october", english, undated)).isFalse()
    }

    @Test
    fun `persian locale matches jalali day and month`() {
        val persian = Locale.forLanguageTag("fa-IR")
        val t = txn("خرید")
        assertThat(matches("۲۰ مهر", persian, t)).isTrue()
        assertThat(matches("20 مهر", persian, t)).isTrue()
        assertThat(matches("۲۱ مهر", persian, t)).isFalse()
    }

    @Test
    fun `a currency symbol marks an exact amount`() {
        val t = txn("Rent", amount = "250.00")
        assertThat(matches("$250", english, t)).isTrue()
        assertThat(matches("250$", spanish, t)).isTrue()
        assertThat(matches("€250", english, t)).isTrue()
        assertThat(matches("MX$250", spanish, t)).isTrue()
        assertThat(matches("$ 250", english, t)).isTrue()
        assertThat(matches("$25", english, t)).isFalse()
        assertThat(matches("$250", english, txn("Refund", amount = "-250.00"))).isTrue()
    }

    @Test
    fun `amounts accept decimal commas and thousands separators`() {
        assertThat(matches("250,50€", spanish, txn("Cena", amount = "250.50"))).isTrue()
        assertThat(matches("$250.5", english, txn("Dinner", amount = "250.50"))).isTrue()
        assertThat(matches("$1,200", english, txn("Laptop", amount = "1200"))).isTrue()
        assertThat(matches("$1.200", spanish, txn("Laptop", amount = "1200"))).isTrue()
        assertThat(matches("$1,234.50", english, txn("TV", amount = "1234.50"))).isTrue()
    }

    @Test
    fun `comparison operators filter by amount`() {
        val big = txn("Rent", amount = "600")
        val small = txn("Coffee", amount = "4.50")
        assertThat(matches(">500", english, big)).isTrue()
        assertThat(matches(">500", english, small)).isFalse()
        assertThat(matches("< 10", english, small)).isTrue()
        assertThat(matches("<10", english, big)).isFalse()
        assertThat(matches(">=600", english, big)).isTrue()
        assertThat(matches("<=4.5", english, small)).isTrue()
        assertThat(matches(">$500", english, big)).isTrue()
    }

    @Test
    fun `amount combines with text and dates`() {
        val uber = txn("Uber", amount = "150")
        assertThat(matches("uber >100", english, uber)).isTrue()
        assertThat(matches("uber >200", english, uber)).isFalse()
        assertThat(matches("uber $150 12 october", english, uber)).isTrue()
        assertThat(matches("Food, 12 october", english, txn("Food"))).isTrue()
    }

    @Test
    fun `a plain number still means a day, not an amount`() {
        val t = txn("Rent", amount = "250", date = LocalDateTime.of(2026, 10, 12, 13, 0))
        assertThat(matches("250", english, t)).isFalse()
        assertThat(matches("12", english, t)).isTrue()
    }

    @Test
    fun `a date range matches every day between both ends`() {
        assertThat(matches("1 oct to 16 oct", english, on(10, 1))).isTrue()
        assertThat(matches("1 oct to 16 oct", english, on(10, 12))).isTrue()
        assertThat(matches("1 oct to 16 oct", english, on(10, 16))).isTrue()
        assertThat(matches("1 oct to 16 oct", english, on(10, 17))).isFalse()
        assertThat(matches("1 oct to 16 oct", english, on(9, 30))).isFalse()
        assertThat(matches("10/1 to 10/16", english, on(10, 12))).isTrue()
        assertThat(matches("1/10..16/10", spanish, on(10, 12))).isTrue()
        assertThat(matches("1/10..16/10", spanish, on(10, 17))).isFalse()
        assertThat(matches("1 de octubre a 16 de octubre", spanish, on(10, 12))).isTrue()
        assertThat(matches("del 1 al 16 de octubre", spanish, on(10, 1))).isTrue()
        assertThat(matches("del 1 al 16 de octubre", spanish, on(10, 17))).isFalse()
    }

    @Test
    fun `a month range covers whole months, wraps the year and respects explicit years`() {
        assertThat(matches("oct to nov", english, on(11, 30))).isTrue()
        assertThat(matches("oct to nov", english, on(12, 1))).isFalse()
        assertThat(matches("nov 4 to nov 30", english, on(11, 4))).isTrue()
        assertThat(matches("nov 4 to nov 30", english, on(11, 3))).isFalse()
        assertThat(matches("dec to jan", english, on(12, 20))).isTrue()
        assertThat(matches("dec to jan", english, on(1, 5, 2027))).isTrue()
        assertThat(matches("dec to jan", english, on(10, 12))).isFalse()
        assertThat(matches("1/10/2026 a 16/10/2026", spanish, on(10, 12))).isTrue()
        assertThat(matches("1/10/2026 a 16/10/2026", spanish, on(10, 12, 2025))).isFalse()
        assertThat(matches("dec 2026 to jan 2027", english, on(1, 5, 2027))).isTrue()
        assertThat(matches("dec 2026 to jan 2027", english, on(1, 5, 2026))).isFalse()
    }

    @Test
    fun `a range combines with text and amounts, and to between words is still text`() {
        val tacos = txn("Tacos", amount = "150", date = LocalDateTime.of(2026, 10, 12, 13, 0))
        assertThat(matches("tacos 1 oct to 16 oct >100", english, tacos)).isTrue()
        assertThat(matches("tacos 1 oct to 16 oct >200", english, tacos)).isFalse()
        assertThat(matches("uber to airport", english, txn("Uber to airport"))).isTrue()
        assertThat(matches("uber to airport", english, txn("Uber"))).isFalse()
        assertThat(matches("1 oct to 16 oct", english, txn("Undated", date = null))).isFalse()
    }

    @Test
    fun `comparison operators also work on dates`() {
        assertThat(matches(">1/10", spanish, on(10, 12))).isTrue()
        assertThat(matches(">1/10", spanish, on(10, 1))).isFalse()
        assertThat(matches(">=1/10", spanish, on(10, 1))).isTrue()
        assertThat(matches("<=12/10", spanish, on(10, 12))).isTrue()
        assertThat(matches("<12/10", spanish, on(10, 12))).isFalse()
        assertThat(matches(">oct", english, on(11, 1))).isTrue()
        assertThat(matches(">oct", english, on(10, 31))).isFalse()
        assertThat(matches(">= 1/10 <= 16/10", spanish, on(10, 12))).isTrue()
        assertThat(matches(">= 1/10 <= 16/10", spanish, on(10, 17))).isFalse()
    }

    @Test
    fun `persian range uses jalali days`() {
        val persian = Locale.forLanguageTag("fa-IR")
        val t = on(10, 12)
        assertThat(matches("۱ مهر تا ۲۰ مهر", persian, t)).isTrue()
        assertThat(matches("۱ مهر تا ۱۹ مهر", persian, t)).isFalse()
    }

    @Test
    fun `quoted words only match text`() {
        val tacos = txn("Tacos")
        assertThat(matches("\"tacos\" 12 oct", english, tacos)).isTrue()
        assertThat(matches("“tacos” 12 oct", english, tacos)).isTrue()
        assertThat(matches("\"oct\"", english, txn("Comida"))).isFalse()
        assertThat(matches("\"coffee beans\"", english, txn("Coffee beans"))).isTrue()
        assertThat(matches("\"coffee beans\"", english, txn("Beans and coffee"))).isFalse()
    }

    @Test
    fun `category name matches even when the description differs`() {
        val categories = mapOf(5L to "Food")
        val tacos = txn("Tacos", categoryId = 5L)
        assertThat(matches("food", english, tacos, categories)).isTrue()
        assertThat(matches("food tacos", english, tacos, categories)).isTrue()
        assertThat(matches("rent", english, tacos, categories)).isFalse()
        assertThat(matches("food", english, txn("Tacos", categoryId = 6L), categories)).isFalse()
    }

    @Test
    fun `a full dotted date after an operator compares dates, not amounts`() {
        val spanish = Locale.forLanguageTag("es-MX")
        assertThat(matches(">12.10.2026", spanish, on(10, 20))).isTrue()
        assertThat(matches(">12.10.2026", spanish, on(10, 1))).isFalse()
        assertThat(matches(">1.234.567", english, txn("Car", amount = "2000000"))).isTrue()
    }

    @Test
    fun `the example label in front of a query is ignored`() {
        val uber = txn("Uber", amount = "150")
        assertThat(matches("Range: 1 oct to 20 oct", english, on(10, 12))).isTrue()
        assertThat(matches("Amount: Uber $150", english, uber)).isTrue()
        assertThat(matches("More than: Uber >100", english, uber)).isTrue()
        assertThat(matches("Text only: \"Uber\"", english, uber)).isTrue()
        assertThat(matches("Amount: Taxi", english, uber)).isFalse()
    }

    @Test
    fun `highlight boxes every text match ignoring case and accents`() {
        val highlight = historySearchHighlight("eats UBER", english)
        assertThat(highlight.ranges("Uber Eats uber")).containsExactly(0..3, 5..8, 10..13).inOrder()
        assertThat(historySearchHighlight("cafe", spanish).ranges("Mi Café")).containsExactly(3..6)
        assertThat(historySearchHighlight("\"uber eats\"", english).ranges("Uber Eats")).containsExactly(0..8)
        assertThat(highlight.amount).isFalse()
    }

    @Test
    fun `highlight ignores date and amount filters but flags the amount`() {
        val highlight = historySearchHighlight("Uber >100 12/10 1 oct to 20 oct", spanish)
        assertThat(highlight.ranges("Uber 100 12/10 oct")).containsExactly(0..3)
        assertThat(highlight.amount).isTrue()
        assertThat(historySearchHighlight("$150", english).amount).isTrue()
        assertThat(historySearchHighlight("", english).ranges("Uber")).isEmpty()
    }

    private val octoberFilter = HistorySearchFilter(
        categoryIds = setOf(9L),
        kind = HistoryTransactionKind.EXPENSE,
        minAmount = BigDecimal("50"),
        maxAmount = BigDecimal("200.50"),
        startDate = java.time.LocalDate.of(2026, 10, 1),
        endDate = java.time.LocalDate.of(2026, 10, 15),
    )

    @Test
    fun `a default filter writes no query text`() {
        assertThat(HistorySearchFilter().toQueryText(emptyMap(), english))
            .isEqualTo("" to HistorySearchFilter())
    }

    @Test
    fun `amounts, dates and one category move into the query and the rest stays in the filter`() {
        val (text, rest) = octoberFilter.toQueryText(mapOf(9L to "Food"), english)
        assertThat(text).isEqualTo("Food >=50 <=200.5 10/1/2026 .. 10/15/2026")
        assertThat(rest).isEqualTo(HistorySearchFilter(kind = HistoryTransactionKind.EXPENSE))
        assertThat(octoberFilter.toQueryText(mapOf(9L to "Food"), spanish).first)
            .isEqualTo("Food >=50 <=200.5 1/10/2026 .. 15/10/2026")
    }

    @Test
    fun `a single date bound writes a comparison`() {
        val from = HistorySearchFilter(startDate = java.time.LocalDate.of(2026, 10, 1))
        val until = HistorySearchFilter(endDate = java.time.LocalDate.of(2026, 10, 15))
        assertThat(from.toQueryText(emptyMap(), english).first).isEqualTo(">=10/1/2026")
        assertThat(until.toQueryText(emptyMap(), english).first).isEqualTo("<=10/15/2026")
    }

    @Test
    fun `categories stay in the filter when several are picked or category names are not searched`() {
        val several = HistorySearchFilter(categoryIds = setOf(1L, 2L))
        assertThat(several.toQueryText(mapOf(1L to "Food", 2L to "Uber"), english)).isEqualTo("" to several)
        val scoped = HistorySearchFilter(categoryIds = setOf(1L), searchCategoryNames = false)
        assertThat(scoped.toQueryText(mapOf(1L to "Food"), english)).isEqualTo("" to scoped)
    }

    @Test
    fun `a category name with spaces is quoted`() {
        val filter = HistorySearchFilter(categoryIds = setOf(1L))
        assertThat(filter.toQueryText(mapOf(1L to "Dining out"), english).first).isEqualTo("\"Dining out\"")
    }

    @Test
    fun `the written query finds the same expenses the filter did`() {
        val categories = mapOf(9L to "Food")
        val rows = listOf(
            txn("Tacos", amount = "50", categoryId = 9L, date = LocalDateTime.of(2026, 10, 1, 9, 0)),
            txn("Tacos", amount = "200.50", categoryId = 9L, date = LocalDateTime.of(2026, 10, 15, 21, 0)),
            txn("Tacos", amount = "49.99", categoryId = 9L, date = LocalDateTime.of(2026, 10, 5, 9, 0)),
            txn("Tacos", amount = "201", categoryId = 9L, date = LocalDateTime.of(2026, 10, 5, 9, 0)),
            txn("Tacos", amount = "80", categoryId = 9L, date = LocalDateTime.of(2026, 9, 30, 9, 0)),
            txn("Tacos", amount = "80", categoryId = 9L, date = LocalDateTime.of(2026, 10, 16, 9, 0)),
            txn("Tacos", amount = "80", categoryId = 3L, date = LocalDateTime.of(2026, 10, 5, 9, 0)),
        )
        listOf(english, spanish).forEach { locale ->
            val (text, rest) = octoberFilter.toQueryText(categories, locale)
            val matcher = historySearchMatcher(text, locale, categories)
            assertThat(rows.map { matcher(it) && rest.matches(it) }).isEqualTo(rows.map { octoberFilter.matches(it) })
        }
        assertThat(rows.map { octoberFilter.matches(it) }).containsExactly(true, true, false, false, false, false, false).inOrder()
    }
}
