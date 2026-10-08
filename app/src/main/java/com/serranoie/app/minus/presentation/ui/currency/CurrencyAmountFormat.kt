package com.serranoie.app.minus.presentation.ui.currency

import com.serranoie.app.minus.domain.model.SupportedCurrency
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

internal fun formatConversionAmount(amount: BigDecimal, currency: String, locale: Locale): String {
    val digits = (SupportedCurrency.findByCode(currency)?.defaultFractionDigits ?: 2).coerceAtLeast(0)
    val rounded = amount.setScale(digits, RoundingMode.HALF_EVEN)
    val formatter = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = digits
        maximumFractionDigits = digits
        roundingMode = RoundingMode.HALF_EVEN
    }
    return "$currency ${formatter.format(rounded)}"
}
