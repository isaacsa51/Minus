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

const val PENDING_SET_ASIDE_KEY_NAME = "pending_set_aside"

data class SavingsReminderGoal(val remaining: BigDecimal, val deadline: LocalDate)

internal fun savingsReminderGoal(goalPocket: SavingsPocketStatus?): SavingsReminderGoal? {
    val target = goalPocket?.pocket?.target?.takeIf { it.signum() > 0 } ?: return null
    val deadline = goalPocket.pocket.targetDate ?: return null
    val remaining = target.subtract(goalPocket.balance).takeIf { it.signum() > 0 } ?: return null
    return SavingsReminderGoal(remaining, deadline)
}

fun encodePendingSetAside(pocketId: Long, amount: BigDecimal): String =
    "$pocketId|${amount.stripTrailingZeros().toPlainString()}"

fun decodePendingSetAside(raw: String): Pair<Long, String>? {
    val pocketId = raw.substringBefore('|').toLongOrNull() ?: return null
    val amount = raw.substringAfter('|', "").takeIf { it.toBigDecimalOrNull() != null } ?: return null
    return pocketId to amount
}

private val AVERAGE_DAYS_IN_MONTH = BigDecimal("30.44")
private val HUNDRED = BigDecimal(100)

internal fun savingsReminderAmount(
    preferences: SavingsPreferences,
    settings: BudgetSettings,
    goalPocket: SavingsPocketStatus? = null,
    today: LocalDate = LocalDate.now(),
): BigDecimal? {
    val goal = savingsReminderGoal(goalPocket)
    val goalReached = goal == null && goalPocket?.pocket?.targetDate != null &&
        goalPocket.pocket.target?.signum() == 1
    val perMonth = when {
        goal != null -> goal.remaining.divide(BigDecimal(monthsLeft(today, goal.deadline)), 2, RoundingMode.HALF_UP)
        goalReached -> null
        else -> preferences.projectedPerPeriod()
    }
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

private fun monthsLeft(today: LocalDate, deadline: LocalDate): Long {
    val whole = ChronoUnit.MONTHS.between(today, deadline)
    val months = if (today.plusMonths(whole).isBefore(deadline)) whole + 1 else whole
    return months.coerceAtLeast(1)
}

internal fun savingsReminderTriggerAt(periodStart: LocalDate, time: LocalTime, now: LocalDateTime): LocalDateTime? {
    val today = now.toLocalDate()
    return when {
        today.isAfter(periodStart) -> null
        today.isBefore(periodStart) -> LocalDateTime.of(periodStart, time)
        else -> maxOf(LocalDateTime.of(periodStart, time), now)
    }
}
