package com.serranoie.app.minus.presentation.util

import android.content.Context
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.domain.model.SymbolPosition
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
 * [maximumFractionDigits] defaults to the currency's natural decimal count (-1 = use currency default).
 */
fun formatCurrencySymbolOnly(
	value: BigDecimal,
	currencyCode: String,
	maximumFractionDigits: Int = -1, // -1 means use currency's default
	minimumFractionDigits: Int = 0,
): String {
	val supported = SupportedCurrency.findByCode(currencyCode)
	val resolvedMaxDigits = if (maximumFractionDigits < 0) {
		supported?.defaultFractionDigits ?: 2
	} else {
		maximumFractionDigits
	}
	val symbol = supported?.symbol
	if (symbol != null) {
		val numberFormatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
			this.maximumFractionDigits = resolvedMaxDigits
			this.minimumFractionDigits = minimumFractionDigits
		}
		val formatted = numberFormatter.format(value)
		return if (supported.symbolPosition == SymbolPosition.END) {
			"$formatted$symbol"
		} else {
			"$symbol$formatted"
		}
	}
	return try {
		val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
			currency = java.util.Currency.getInstance(currencyCode)
			this.maximumFractionDigits = resolvedMaxDigits
			this.minimumFractionDigits = minimumFractionDigits
		}
		formatter.format(value)
	} catch (e: Exception) {
		val numberFormatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
			this.maximumFractionDigits = resolvedMaxDigits
			this.minimumFractionDigits = minimumFractionDigits
		}
		"$currencyCode${numberFormatter.format(value)}"
	}
}

/**
 * Creates a NumberFormat that renders currency with symbol only (no currency code).
 * [maximumFractionDigits] defaults to the currency's natural decimal count (-1 = use currency default).
 */
fun symbolOnlyCurrencyFormat(
	currencyCode: String,
	maximumFractionDigits: Int = -1,  // -1 means use currency's default
	minimumFractionDigits: Int = 0,
): NumberFormat {
	val supported = SupportedCurrency.findByCode(currencyCode)
	val resolvedMaxDigits = if (maximumFractionDigits < 0) {
		supported?.defaultFractionDigits ?: 2
	} else {
		maximumFractionDigits
	}
	val currencySymbol = supported?.symbol ?: "$"
	val symbolAtEnd = supported?.symbolPosition == SymbolPosition.END
	return (NumberFormat.getNumberInstance(Locale.getDefault()) as DecimalFormat).apply {
		this.maximumFractionDigits = resolvedMaxDigits
		this.minimumFractionDigits = minimumFractionDigits
		if (symbolAtEnd) {
			positiveSuffix = currencySymbol
			negativeSuffix = "-$currencySymbol"
		} else {
			positivePrefix = currencySymbol
			negativePrefix = "-$currencySymbol"
		}
		try {
			currency = java.util.Currency.getInstance(currencyCode)
		} catch (e: Exception) {
			// Keep the prefix/suffix set above
		}
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

fun BigDecimal.isZero(): Boolean = this.signum() == 0

fun String.join(third: Boolean = false): String {
	return this
}
