package com.serranoie.app.minus.domain.currency

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class CurrencyConversionTest {
    private fun rate(base: String = "USD", quote: String = "BRL", value: String = "5", manual: Boolean = false) = ExchangeRate(base, quote, BigDecimal(value), "2026-10-08", manual)
    @Test fun direct() { assertEquals(BigDecimal("50"), ConversionPreferences("BRL", referenceRates = listOf(rate())).convert(BigDecimal.TEN, "USD")) }
    @Test fun inverse() { assertEquals(0, BigDecimal("10").compareTo(ConversionPreferences("USD", referenceRates = listOf(rate())).convert(BigDecimal("50"), "BRL"))) }
    @Test fun manualWins() { assertEquals(BigDecimal("60"), ConversionPreferences("BRL", listOf(rate(value = "6", manual = true)), listOf(rate())).convert(BigDecimal.TEN, "USD")) }
    @Test fun inverseManualWinsOverDirectReference() { assertEquals(0, BigDecimal("50").compareTo(ConversionPreferences("BRL", listOf(rate("BRL", "USD", "0.2", true)), listOf(rate(value = "6"))).convert(BigDecimal.TEN, "USD"))) }
    @Test fun missingIsNotZero() { assertNull(ConversionPreferences("BRL").convert(BigDecimal.TEN, "USD")) }
    @Test fun disabled() { assertNull(ConversionPreferences().convert(BigDecimal.TEN, "USD")) }
    @Test fun identity() { assertEquals(BigDecimal.TEN, ConversionPreferences("USD").convert(BigDecimal.TEN, "USD")) }
    @Test fun negativeAmount() { assertEquals(BigDecimal("-50"), ConversionPreferences("BRL", referenceRates = listOf(rate())).convert(BigDecimal("-10"), "USD")) }
    @Test fun preservesPrecision() { assertEquals(BigDecimal("0.1234567890123456789"), ConversionPreferences("BRL", referenceRates = listOf(rate(value = "0.1234567890123456789"))).convert(BigDecimal.ONE, "USD")) }
    @Test(expected = IllegalArgumentException::class) fun rejectsZero() { rate(value = "0") }
    @Test(expected = IllegalArgumentException::class) fun rejectsNegative() { rate(value = "-1") }
    @Test(expected = IllegalArgumentException::class) fun rejectsBadCode() { rate(base = "USD?x") }
    @Test(expected = IllegalArgumentException::class) fun rejectsSamePair() { rate(quote = "USD") }
    @Test fun originalUntouched() { val amount = BigDecimal("10.55"); ConversionPreferences("BRL", referenceRates = listOf(rate())).convert(amount, "USD"); assertEquals(BigDecimal("10.55"), amount) }
    @Test fun manualToman() { assertEquals(0, BigDecimal("100").compareTo(ConversionPreferences("IRT", listOf(rate("IRR", "IRT", "0.1", true))).convert(BigDecimal("1000"), "IRR"))) }
    @Test fun inverseNonTerminating() { assertNotNull(ConversionPreferences("USD", referenceRates = listOf(rate(value = "3000000000000000000"))).convert(BigDecimal.ONE, "BRL")) }
}
