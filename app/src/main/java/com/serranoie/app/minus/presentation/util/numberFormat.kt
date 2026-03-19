package com.serranoie.app.minus.presentation.util

import android.content.Context
import com.serranoie.app.minus.domain.model.SupportedCurrency
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

fun getFloatDivider(): String {
	val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

	numberFormat.maximumFractionDigits = 1
	numberFormat.minimumFractionDigits = 1

	val formattedValue = numberFormat.format(1.0)

	return formattedValue.substring(1, 2)
}

/**
 * Formats a currency value using only the symbol (e.g., "$1,234.56" instead of "USD $1,234.56").
 * Uses the [SupportedCurrency] list for symbol lookup, falling back to the system locale formatter.
 */
fun formatCurrencySymbolOnly(
	value: BigDecimal,
	currencyCode: String,
	maximumFractionDigits: Int = 2,
	minimumFractionDigits: Int = 0,
): String {
	val symbol = SupportedCurrency.findByCode(currencyCode)?.symbol
	if (symbol != null) {
		val numberFormatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
			this.maximumFractionDigits = maximumFractionDigits
			this.minimumFractionDigits = minimumFractionDigits
		}
		return "$symbol${numberFormatter.format(value)}"
	}
	// Fallback to standard currency formatter
	val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
		currency = java.util.Currency.getInstance(currencyCode)
		this.maximumFractionDigits = maximumFractionDigits
		this.minimumFractionDigits = minimumFractionDigits
	}
	return formatter.format(value)
}

/**
 * Creates a NumberFormat that renders currency with symbol only (no currency code).
 */
fun symbolOnlyCurrencyFormat(
	currencyCode: String,
	maximumFractionDigits: Int = 2,
	minimumFractionDigits: Int = 0,
): NumberFormat {
	val symbol = SupportedCurrency.findByCode(currencyCode)?.symbol ?: "$"
	return (NumberFormat.getNumberInstance(Locale.getDefault()) as DecimalFormat).apply {
		this.maximumFractionDigits = maximumFractionDigits
		this.minimumFractionDigits = minimumFractionDigits
		positivePrefix = "$symbol"
		negativePrefix = "-$symbol"
	}
}

fun numberFormat(
	context: Context,
	value: BigDecimal,
	currency: String,
	trimDecimalPlaces: Boolean = false,
	forceShowAfterDot: Boolean = false,
	maximumFractionDigits: Int = if (forceShowAfterDot) 5 else 2,
	minimumFractionDigits: Int = if (forceShowAfterDot) 1 else 0,
): String {
	return formatCurrencySymbolOnly(
		value = value,
		currencyCode = currency,
		maximumFractionDigits = maximumFractionDigits,
		minimumFractionDigits = minimumFractionDigits,
	)
}

// Extension function for BigDecimal to check if zero
fun BigDecimal.isZero(): Boolean = this.signum() == 0

fun String.join(third: Boolean = false): String {
	return this
}
