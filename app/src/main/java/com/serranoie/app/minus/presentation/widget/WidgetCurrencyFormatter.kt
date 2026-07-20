package com.serranoie.app.minus.presentation.widget

import com.serranoie.app.minus.presentation.util.formatCurrencySymbolOnly
import java.math.BigDecimal

fun formatWidgetCurrency(currency: String, amount: Int): String {
    return formatCurrencySymbolOnly(
        value = BigDecimal(amount),
        currencyCode = currency,
        minimumFractionDigits = 0,
    )
}