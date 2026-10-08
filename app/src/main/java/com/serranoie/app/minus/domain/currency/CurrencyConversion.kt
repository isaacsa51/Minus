package com.serranoie.app.minus.domain.currency

import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate

/** Rates are display preferences, never transaction updates. */
data class ExchangeRate(
    val base: String,
    val quote: String,
    val rate: BigDecimal,
    val date: String,
    val manual: Boolean,
) {
    init {
        require(base.matches(Regex("[A-Z]{3}")) && quote.matches(Regex("[A-Z]{3}")))
        require(base != quote && rate.signum() > 0 && rate.precision() <= 40)
        require(rate.scale() in -40..100)
        LocalDate.parse(date)
    }
    val key: String get() = "$base/$quote"
}

data class ConversionPreferences(
    val displayCurrency: String? = null,
    val manualRates: List<ExchangeRate> = emptyList(),
    val referenceRates: List<ExchangeRate> = emptyList(),
) {
    fun rateFor(base: String, quote: String): ExchangeRate? {
        fun find(rates: List<ExchangeRate>): ExchangeRate? {
            rates.firstOrNull { it.base == base && it.quote == quote }?.let { return it }
            return rates.firstOrNull { it.base == quote && it.quote == base }?.let {
                it.copy(base = base, quote = quote, rate = BigDecimal.ONE.divide(it.rate, MathContext.DECIMAL128))
            }
        }
        // A manual rate in either direction always wins over a reference rate.
        return find(manualRates) ?: find(referenceRates)
    }
    fun convert(amount: BigDecimal, base: String): BigDecimal? {
        val quote = displayCurrency ?: return null
        if (base == quote) return amount
        return rateFor(base, quote)?.let { amount.multiply(it.rate, MathContext.DECIMAL128) }
    }
}
