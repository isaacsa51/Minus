package com.serranoie.app.minus.domain.calculator

import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.SavingsPreferences
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

private val AVERAGE_DAYS_IN_MONTH = BigDecimal("30.44")
private val HUNDRED = BigDecimal(100)

internal fun savingsReminderAmount(preferences: SavingsPreferences, settings: BudgetSettings): BigDecimal? {
    val perMonth = preferences.projectedPerPeriod()
    val amount = when {
        perMonth == null -> settings.totalBudget.multiply(BigDecimal(preferences.savingsPct))
            .divide(HUNDRED, 2, RoundingMode.HALF_UP)
        settings.period == BudgetPeriod.MONTHLY -> perMonth
        else -> {
            val days = ChronoUnit.DAYS.between(settings.startDate, settings.getPeriodEndDate()) + 1
            perMonth.multiply(BigDecimal(days)).divide(AVERAGE_DAYS_IN_MONTH, 2, RoundingMode.HALF_UP)
        }
    }
    return amount.takeIf { it.signum() > 0 }
}

internal fun savingsReminderTriggerAt(periodStart: LocalDate, time: LocalTime, now: LocalDateTime): LocalDateTime? {
    val today = now.toLocalDate()
    return when {
        today.isAfter(periodStart) -> null
        today.isBefore(periodStart) -> LocalDateTime.of(periodStart, time)
        else -> maxOf(LocalDateTime.of(periodStart, time), now)
    }
}
