package com.serranoie.app.minus.domain.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class ExpenseNotificationParserTest {

    private val deny = listOf("refund", "credited", "deposit", "balance", "cashback", "otp")

    private fun parse(text: String, currency: String = "USD") =
        ExpenseNotificationParser.parse(text, currency, deny)

    @Test
    fun parses_symbol_before_amount() {
        assertEquals(BigDecimal("42.90"), parse("Purchase approved $42.90 at STARBUCKS"))
    }

    @Test
    fun parses_iso_code_after_amount() {
        assertEquals(BigDecimal("12.00"), parse("You spent 12.00 USD at the shop"))
    }

    @Test
    fun parses_comma_decimal_and_dot_grouping() {
        assertEquals(BigDecimal("1234.56"), parse("Compra de R$ 1.234,56 no cartao", "BRL"))
    }

    @Test
    fun parses_dot_decimal_and_comma_grouping() {
        assertEquals(BigDecimal("1234.56"), parse("Charged $1,234.56 today"))
    }

    @Test
    fun treats_three_trailing_digits_as_grouping() {
        assertEquals(BigDecimal("1234"), parse("Pago de 1.234 € en tienda", "EUR"))
    }

    @Test
    fun ignores_income_sign() {
        assertNull(parse("You received +$1,200.00 from Ana"))
    }

    @Test
    fun ignores_denied_words() {
        assertNull(parse("Refund of $42.90 processed"))
        assertNull(parse("Your balance is $1,500.00"))
    }

    @Test
    fun ignores_percentages() {
        assertNull(parse("Get 50% off on every $ purchase"))
    }

    @Test
    fun ignores_numbers_without_currency_evidence() {
        assertNull(parse("Your order 123456 has shipped"))
    }

    @Test
    fun ignores_numbers_far_from_currency_evidence() {
        assertNull(parse("Order 998877 confirmed, see your $ summary later in the week"))
    }

    @Test
    fun ignores_zero_amounts() {
        assertNull(parse("Authorization for $0.00 at MERCHANT"))
    }

    @Test
    fun keeps_negative_sign_as_spend() {
        assertEquals(BigDecimal("8.50"), parse("Card debit -$8.50 METRO"))
    }
}
