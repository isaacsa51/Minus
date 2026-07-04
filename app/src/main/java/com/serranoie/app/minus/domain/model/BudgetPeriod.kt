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

data class SupportedCurrency(
    val code: String,
    val symbol: String,
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
            SupportedCurrency("VND", "₫"),
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
        )
    }
}