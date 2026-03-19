package com.serranoie.app.minus.domain.model

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Enum representing the budget calculation period.
 */
enum class BudgetPeriod {
    DAILY,
    WEEKLY,
    BIWEEKLY,
    MONTHLY
}

/**
 * Strategy for handling remaining budget at the end of a period.
 */
enum class RemainingBudgetStrategy {
    /** Always ask the user what to do */
    ASK_ALWAYS,
    /** Split remaining equally across all days of the new period */
    SPLIT_EQUALLY,
    /** Add all remaining to the first day of the new period */
    ADD_TO_FIRST_DAY,
}

/**
 * Supported currencies with their symbols.
 */
data class SupportedCurrency(
    val code: String,
    val symbol: String,
    val name: String,
) {
    companion object {
        val ALL = listOf(
            SupportedCurrency("USD", "$", "Dólar estadounidense"),
            SupportedCurrency("MXN", "$", "Peso mexicano"),
            SupportedCurrency("EUR", "€", "Euro"),
            SupportedCurrency("GBP", "£", "Libra esterlina"),
            SupportedCurrency("JPY", "¥", "Yen japonés"),
            SupportedCurrency("CNY", "¥", "Yuan chino"),
            SupportedCurrency("KRW", "₩", "Won surcoreano"),
            SupportedCurrency("INR", "₹", "Rupia india"),
            SupportedCurrency("BRL", "R$", "Real brasileño"),
            SupportedCurrency("ARS", "$", "Peso argentino"),
            SupportedCurrency("COP", "$", "Peso colombiano"),
            SupportedCurrency("CLP", "$", "Peso chileno"),
            SupportedCurrency("PEN", "S/", "Sol peruano"),
            SupportedCurrency("CAD", "CA$", "Dólar canadiense"),
            SupportedCurrency("AUD", "A$", "Dólar australiano"),
            SupportedCurrency("CHF", "CHF", "Franco suizo"),
            SupportedCurrency("SEK", "kr", "Corona sueca"),
            SupportedCurrency("NOK", "kr", "Corona noruega"),
            SupportedCurrency("DKK", "kr", "Corona danesa"),
            SupportedCurrency("PLN", "zł", "Zloty polaco"),
            SupportedCurrency("TRY", "₺", "Lira turca"),
            SupportedCurrency("RUB", "₽", "Rublo ruso"),
            SupportedCurrency("THB", "฿", "Baht tailandés"),
            SupportedCurrency("PHP", "₱", "Peso filipino"),
            SupportedCurrency("TWD", "NT$", "Dólar taiwanés"),
            SupportedCurrency("ILS", "₪", "Shekel israelí"),
            SupportedCurrency("ZAR", "R", "Rand sudafricano"),
            SupportedCurrency("NGN", "₦", "Naira nigeriana"),
            SupportedCurrency("EGP", "E£", "Libra egipcia"),
        )

        fun findByCode(code: String): SupportedCurrency? =
            ALL.find { it.code.equals(code, ignoreCase = true) }
    }
}

/**
 * User settings for budget calculation.
 */
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
) {
    /**
     * Get the number of days for the period based on the period type or custom days.
     */
    fun getDaysForPeriod(): Int {
        return when (period) {
            BudgetPeriod.DAILY -> 1
            BudgetPeriod.WEEKLY -> 7
            BudgetPeriod.BIWEEKLY -> 14
            BudgetPeriod.MONTHLY -> 30
        }.coerceAtLeast(daysInPeriod)
    }

    /**
     * Calculate the daily budget based on total budget and days in period.
     */
    fun calculateDailyBudget(): BigDecimal {
        val days = getDaysForPeriod()
        return if (days > 0) {
            totalBudget.divide(BigDecimal(days), 2, java.math.RoundingMode.HALF_UP)
        } else {
            totalBudget
        }
    }

    /**
     * Get the end date of the current period.
     * Uses the actual endDate if set, otherwise calculates from period type.
     */
    fun getPeriodEndDate(): LocalDate {
        return endDate ?: startDate.plusDays(getDaysForPeriod().toLong())
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
        )
    }
}
