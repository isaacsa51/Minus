package com.serranoie.app.minus.presentation.util.font.format

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class CurrencyFormatTest {

    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `USD gets a leading dollar sign and grouped thousands`() {
        assertThat(formatCurrencySymbolOnly(BigDecimal("1234567"), "USD"))
            .isEqualTo("$1,234,567")
    }

    @Test
    fun `USD keeps two decimals when the value has them`() {
        assertThat(formatCurrencySymbolOnly(BigDecimal("12.34"), "USD"))
            .isEqualTo("$12.34")
    }

    @Test
    fun `minimumFractionDigits pads a whole value`() {
        assertThat(formatCurrencySymbolOnly(BigDecimal("12"), "USD", minimumFractionDigits = 2))
            .isEqualTo("$12.00")
    }

    @Test
    fun `maximumFractionDigits of zero drops the fractional part`() {
        assertThat(formatCurrencySymbolOnly(BigDecimal("12.34"), "USD", maximumFractionDigits = 0))
            .isEqualTo("$12")
    }

    @Test
    fun `an end-position currency puts the symbol after the number`() {
        assertThat(formatCurrencySymbolOnly(BigDecimal("1000"), "VND"))
            .isEqualTo("1,000₫")
    }

    @Test
    fun `currency code lookup is case-insensitive`() {
        assertThat(formatCurrencySymbolOnly(BigDecimal("5"), "usd"))
            .isEqualTo("$5")
    }

    @Test
    fun `an unknown currency code falls back to prefixing the raw code`() {
        assertThat(formatCurrencySymbolOnly(BigDecimal("100"), "XyZ"))
            .isEqualTo("XyZ100")
    }

    @Test
    fun `the simple formatter concatenates the symbol before a negative number`() {
        assertThat(formatCurrencySymbolOnly(BigDecimal("-50"), "USD"))
            .isEqualTo("$-50")
    }

    @Test
    fun `the factory formatter prefixes the symbol and groups thousands`() {
        assertThat(symbolOnlyCurrencyFormat("USD").format(BigDecimal("1234")))
            .isEqualTo("$1,234")
    }

    @Test
    fun `the factory formatter honours minimumFractionDigits`() {
        assertThat(symbolOnlyCurrencyFormat("USD", minimumFractionDigits = 2).format(BigDecimal("12.5")))
            .isEqualTo("$12.50")
    }

    @Test
    fun `the factory formatter puts a real minus in front of the symbol`() {
        assertThat(symbolOnlyCurrencyFormat("USD").format(BigDecimal("-50")))
            .isEqualTo("-$50")
    }

    @Test
    fun `the factory formatter places the symbol at the end for end-position currencies`() {
        assertThat(symbolOnlyCurrencyFormat("VND").format(BigDecimal("1000")))
            .isEqualTo("1,000₫")
    }
}
