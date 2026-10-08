package com.serranoie.app.minus.presentation.ui.currency

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class CurrencyAmountFormatTest {
    @Test fun englishUsesDecimalPoint() = assertEquals("USD 12.50", formatConversionAmount(BigDecimal("12.50"), "USD", Locale.US))
    @Test fun frenchUsesDecimalComma() = assertEquals("BRL 12,50", formatConversionAmount(BigDecimal("12.50"), "BRL", Locale.FRANCE))
    @Test fun spanishUsesDecimalComma() = assertEquals("EUR 12,50", formatConversionAmount(BigDecimal("12.50"), "EUR", Locale.forLanguageTag("es-ES")))
    @Test fun brazilianPortugueseUsesDecimalComma() = assertEquals("BRL 12,50", formatConversionAmount(BigDecimal("12.50"), "BRL", Locale.forLanguageTag("pt-BR")))
    @Test fun roundsHalfEvenWithoutDoubleConversion() = assertEquals("USD 12.34", formatConversionAmount(BigDecimal("12.345"), "USD", Locale.US))
    @Test fun zeroFractionCurrencyStaysWhole() = assertEquals("JPY 13", formatConversionAmount(BigDecimal("12.6"), "JPY", Locale.US))
    @Test fun customUnitRetainsCodeAndDecimals() = assertEquals("IRT 12,50", formatConversionAmount(BigDecimal("12.50"), "IRT", Locale.FRANCE))
    @Test fun largeAmountsKeepDecimalPrecision() = assertEquals("USD 123,456,789,012,345,678.12", formatConversionAmount(BigDecimal("123456789012345678.125"), "USD", Locale.US))
}
