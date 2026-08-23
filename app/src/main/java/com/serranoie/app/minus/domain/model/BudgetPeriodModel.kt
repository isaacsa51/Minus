package com.serranoie.app.minus.domain.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Currency
import java.util.Locale

enum class BudgetPeriod {
    DAILY, WEEKLY, BIWEEKLY, MONTHLY
}

enum class RemainingBudgetStrategy {
    ASK_ALWAYS, SPLIT_EQUALLY, ADD_TO_FIRST_DAY,
}

/**
 * How the period's total budget is divided for the per-period pill shown in the UI.
 *
 * - [STATIC]: totalBudget / (totalDays / period.toDays()), unchanged regardless of spend.
 * - [DYNAMIC]: (totalBudget - totalSpent) / daysRemaining, so the pill updates as you spend.
 */
enum class BudgetSplitMode {
    STATIC,
    DYNAMIC,
}

enum class SymbolPosition {
    START,
    END,
}

data class SupportedCurrency(
    val code: String,
    val symbol: String,
    val symbolPosition: SymbolPosition = SymbolPosition.START,
) {
    fun displayName(locale: Locale = Locale.getDefault()): String =
        runCatching { Currency.getInstance(code).getDisplayName(locale) }
            .getOrDefault(code)

    @Composable
    @ReadOnlyComposable
    fun displayName(): String {
        val locale = LocalConfiguration.current.locales[0]
        return displayName(locale)
    }

    val defaultFractionDigits: Int
        get() = SupportedCurrencyData.BY_CODE[code]?.defaultFractionDigits ?: 2

    val hasDecimals: Boolean get() = SupportedCurrencyData.BY_CODE[code]?.hasDecimals ?: true

    companion object {
        val ALL = listOf(
            SupportedCurrency("USD", "$"),
            SupportedCurrency("MXN", "$"),
            SupportedCurrency("EUR", "€"),
            SupportedCurrency("GBP", "£"),
            SupportedCurrency("HKD", "HK$"),
            SupportedCurrency("SGD", "S$"),
            SupportedCurrency("JPY", "¥"),
            SupportedCurrency("CNY", "¥"),
            SupportedCurrency("KRW", "₩"),
            SupportedCurrency("INR", "₹"),
            SupportedCurrency("PKR", "₨"),
            SupportedCurrency("BDT", "৳"),
            SupportedCurrency("MYR", "RM"),
            SupportedCurrency("IDR", "Rp"),
            SupportedCurrency("VND", "₫", SymbolPosition.END),
            SupportedCurrency("BRL", "R$"),
            SupportedCurrency("ARS", "$"),
            SupportedCurrency("COP", "$"),
            SupportedCurrency("CLP", "$"),
            SupportedCurrency("PEN", "S/"),
            SupportedCurrency("CAD", "CA$"),
            SupportedCurrency("AUD", "A$"),
            SupportedCurrency("NZD", "NZ$"),
            SupportedCurrency("CHF", "CHF"),
            SupportedCurrency("SEK", "kr"),
            SupportedCurrency("NOK", "kr"),
            SupportedCurrency("DKK", "kr"),
            SupportedCurrency("PLN", "zł"),
            SupportedCurrency("TRY", "₺"),
            SupportedCurrency("RUB", "₽"),
            SupportedCurrency("THB", "฿"),
            SupportedCurrency("PHP", "₱"),
            SupportedCurrency("TWD", "NT$"),
            SupportedCurrency("ILS", "₪"),
            SupportedCurrency("ZAR", "R"),
            SupportedCurrency("NGN", "₦"),
            SupportedCurrency("EGP", "E£"),
            SupportedCurrency("MAD", "MAD"),
            SupportedCurrency("TND", "TND"),
            SupportedCurrency("LYD", "LYD"),
            SupportedCurrency("DZD", "DZD"),
            SupportedCurrency("SDG", "SDG"),
            SupportedCurrency("KES", "KES"),
            SupportedCurrency("GHS", "₵"),
            SupportedCurrency("XOF", "XOF"),
            SupportedCurrency("GMD", "GMD"),
            SupportedCurrency("SLE", "SLE"),
            SupportedCurrency("LRD", "LRD"),
            SupportedCurrency("GNF", "GNF"),
            SupportedCurrency("MRU", "MRU"),
            SupportedCurrency("CVE", "CVE"),
            SupportedCurrency("STN", "STN"),
            SupportedCurrency("TZS", "TZS"),
            SupportedCurrency("UGX", "UGX"),
            SupportedCurrency("ETB", "ETB"),
            SupportedCurrency("RWF", "RWF"),
            SupportedCurrency("BIF", "BIF"),
            SupportedCurrency("DJF", "DJF"),
            SupportedCurrency("SOS", "SOS"),
            SupportedCurrency("ERN", "ERN"),
            SupportedCurrency("XAF", "XAF"),
            SupportedCurrency("CDF", "CDF"),
            SupportedCurrency("ZMW", "ZMW"),
            SupportedCurrency("MWK", "MWK"),
            SupportedCurrency("MZN", "MZN"),
            SupportedCurrency("AOA", "AOA"),
            SupportedCurrency("BWP", "BWP"),
            SupportedCurrency("LSL", "LSL"),
            SupportedCurrency("SZL", "SZL"),
            SupportedCurrency("NAD", "NAD"),
            SupportedCurrency("MUR", "MUR"),
            SupportedCurrency("SCR", "SCR"),
            SupportedCurrency("MGA", "MGA"),
            SupportedCurrency("ZWL", "ZWL"),
            SupportedCurrency("SAR", "SAR"),
            SupportedCurrency("SYP", "SYP"),
            SupportedCurrency("HUF", "Ft", SymbolPosition.END),
            SupportedCurrency("UAH", "₴", SymbolPosition.END),
            SupportedCurrency("GEL", "₾"),
            SupportedCurrency("QAR", "QAR"),
            SupportedCurrency("AED", "AED"),
        )

        fun findByCode(code: String): SupportedCurrency? =
            ALL.find { it.code.equals(code, ignoreCase = true) }
    }
}

data class BudgetSettings(
    val totalBudget: BigDecimal,
    val period: BudgetPeriod,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val currencyCode: String = "USD",
    val daysInPeriod: Int = 1,
    val rollOverEnabled: Boolean = false,
    val rollOverLimit: BigDecimal? = null,
    val rollOverCarryForward: Boolean = false,
    val remainingBudgetStrategy: RemainingBudgetStrategy = RemainingBudgetStrategy.ASK_ALWAYS,
    val creditCardCutoffDay: Int? = null,
    val splitMode: BudgetSplitMode = BudgetSplitMode.STATIC,
) {
    fun getDaysForPeriod(): Int {
        return when (period) {
            BudgetPeriod.DAILY -> 1
            BudgetPeriod.WEEKLY -> 7
            BudgetPeriod.BIWEEKLY -> 14
            BudgetPeriod.MONTHLY -> 30
        }.coerceAtLeast(daysInPeriod)
    }

    fun calculateDailyBudget(): BigDecimal {
        val days = getDaysForPeriod()
        return if (days > 0) {
            totalBudget.divide(BigDecimal(days), 2, java.math.RoundingMode.HALF_UP)
        } else {
            totalBudget
        }
    }

    fun getPeriodEndDate(): LocalDate {
        return endDate ?: startDate.plusDays(getDaysForPeriod().toLong() - 1)
    }

    companion object {
        val DEFAULT = BudgetSettings(
            totalBudget = BigDecimal.ZERO,
            period = BudgetPeriod.DAILY,
            startDate = LocalDate.now(),
            endDate = null,
            currencyCode = "USD",
            daysInPeriod = 1,
            rollOverEnabled = false,
            rollOverLimit = null,
            rollOverCarryForward = false,
            remainingBudgetStrategy = RemainingBudgetStrategy.ASK_ALWAYS,
            creditCardCutoffDay = null,
            splitMode = BudgetSplitMode.STATIC,
        )
    }
}

data class SupportedCurrencyData(
    val code: String,
    val symbol: String,
    val symbolPosition: SymbolPosition = SymbolPosition.START,
    val defaultFractionDigits: Int,
) {
    val hasDecimals: Boolean get() = defaultFractionDigits > 0

    companion object {
        private val SUPPORTED_CODES: List<Pair<String, SymbolPosition>> = listOf(
            "USD" to SymbolPosition.START,
            "MXN" to SymbolPosition.START,
            "EUR" to SymbolPosition.START,
            "GBP" to SymbolPosition.START,
            "HKD" to SymbolPosition.START,
            "SGD" to SymbolPosition.START,
            "JPY" to SymbolPosition.START,
            "CNY" to SymbolPosition.START,
            "KRW" to SymbolPosition.START,
            "INR" to SymbolPosition.START,
            "PKR" to SymbolPosition.START,
            "BDT" to SymbolPosition.START,
            "MYR" to SymbolPosition.START,
            "IDR" to SymbolPosition.START,
            "VND" to SymbolPosition.END,
            "BRL" to SymbolPosition.START,
            "ARS" to SymbolPosition.START,
            "COP" to SymbolPosition.START,
            "CLP" to SymbolPosition.START,
            "PEN" to SymbolPosition.START,
            "CAD" to SymbolPosition.START,
            "AUD" to SymbolPosition.START,
            "NZD" to SymbolPosition.START,
            "CHF" to SymbolPosition.START,
            "SEK" to SymbolPosition.START,
            "NOK" to SymbolPosition.START,
            "DKK" to SymbolPosition.START,
            "PLN" to SymbolPosition.END,
            "TRY" to SymbolPosition.START,
            "RUB" to SymbolPosition.START,
            "THB" to SymbolPosition.START,
            "PHP" to SymbolPosition.START,
            "TWD" to SymbolPosition.START,
            "ILS" to SymbolPosition.START,
            "ZAR" to SymbolPosition.START,
            "NGN" to SymbolPosition.START,
            "EGP" to SymbolPosition.START,
            "MAD" to SymbolPosition.START,
            "TND" to SymbolPosition.START,
            "LYD" to SymbolPosition.START,
            "DZD" to SymbolPosition.START,
            "SDG" to SymbolPosition.START,
            "KES" to SymbolPosition.START,
            "GHS" to SymbolPosition.START,
            "XOF" to SymbolPosition.START,
            "GMD" to SymbolPosition.START,
            "SLE" to SymbolPosition.START,
            "LRD" to SymbolPosition.START,
            "GNF" to SymbolPosition.START,
            "MRU" to SymbolPosition.START,
            "CVE" to SymbolPosition.START,
            "STN" to SymbolPosition.START,
            "TZS" to SymbolPosition.START,
            "UGX" to SymbolPosition.START,
            "ETB" to SymbolPosition.START,
            "RWF" to SymbolPosition.START,
            "BIF" to SymbolPosition.START,
            "DJF" to SymbolPosition.START,
            "SOS" to SymbolPosition.START,
            "ERN" to SymbolPosition.START,
            "XAF" to SymbolPosition.START,
            "CDF" to SymbolPosition.START,
            "ZMW" to SymbolPosition.START,
            "MWK" to SymbolPosition.START,
            "MZN" to SymbolPosition.START,
            "AOA" to SymbolPosition.START,
            "BWP" to SymbolPosition.START,
            "LSL" to SymbolPosition.START,
            "SZL" to SymbolPosition.START,
            "NAD" to SymbolPosition.START,
            "MUR" to SymbolPosition.START,
            "SCR" to SymbolPosition.START,
            "MGA" to SymbolPosition.START,
            "ZWL" to SymbolPosition.START,
            "HUF" to SymbolPosition.END,
            "UAH" to SymbolPosition.END,
            "QAR" to SymbolPosition.START,
            "AED" to SymbolPosition.START
        )

        private fun fractionDigits(code: String): Int =
            runCatching { Currency.getInstance(code).defaultFractionDigits }
                .getOrElse { 2 }

        val BY_CODE: Map<String, SupportedCurrencyData> =
            SUPPORTED_CODES.associate { (code, position) ->
                val supported = SupportedCurrency.ALL.find { it.code == code }
                code to SupportedCurrencyData(
                    code = code,
                    symbol = supported?.symbol ?: code,
                    symbolPosition = position,
                    defaultFractionDigits = fractionDigits(code),
                )
            }

        fun findByCode(code: String): SupportedCurrencyData? = BY_CODE[code.uppercase()]
    }
}
