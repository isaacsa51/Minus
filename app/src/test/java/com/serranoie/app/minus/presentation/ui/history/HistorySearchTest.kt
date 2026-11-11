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
    ) = Transaction(amount = BigDecimal(amount), comment = comment, note = note, date = date)

    private fun matches(query: String, locale: Locale, transaction: Transaction) =
        historySearchMatcher(query, locale)(transaction)

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
}
