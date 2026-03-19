package com.serranoie.app.minus.domain.calculator

import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.PeriodKey
import com.serranoie.app.minus.domain.model.PeriodMappingMode
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object TransactionPeriodMapper {

    fun mapTransaction(
        transaction: Transaction,
        budgetSettings: BudgetSettings,
        mappingMode: PeriodMappingMode,
        today: LocalDate = LocalDate.now()
    ): List<PeriodKey> {
        val dates = resolveOccurrenceDates(transaction, budgetSettings, today)
        return dates.map { date -> mapDate(date, budgetSettings, mappingMode) }
    }

    fun mapDate(
        date: LocalDate,
        budgetSettings: BudgetSettings,
        mappingMode: PeriodMappingMode
    ): PeriodKey {
        return when (mappingMode) {
            PeriodMappingMode.ACTIVE_BUDGET -> mapByActiveBudget(date, budgetSettings)
            PeriodMappingMode.CALENDAR_BUCKET -> mapByCalendar(date, budgetSettings.period)
        }
    }

    private fun mapByActiveBudget(date: LocalDate, settings: BudgetSettings): PeriodKey {
        val baseStart = settings.startDate
        val period = settings.period

        val start = when (period) {
            BudgetPeriod.DAILY -> date
            BudgetPeriod.WEEKLY -> {
                val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(baseStart, date)
                val offsetWeeks = kotlin.math.floor(daysDiff / 7.0).toLong()
                baseStart.plusWeeks(offsetWeeks)
            }
            BudgetPeriod.BIWEEKLY -> {
                val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(baseStart, date)
                val offsetBiWeeks = kotlin.math.floor(daysDiff / 14.0).toLong()
                baseStart.plusWeeks(offsetBiWeeks * 2)
            }
            BudgetPeriod.MONTHLY -> {
                val monthsDiff = java.time.temporal.ChronoUnit.MONTHS.between(
                    baseStart.withDayOfMonth(1),
                    date.withDayOfMonth(1)
                )
                baseStart.plusMonths(monthsDiff)
            }
        }

        val end = when (period) {
            BudgetPeriod.DAILY -> start
            BudgetPeriod.WEEKLY -> start.plusDays(6)
            BudgetPeriod.BIWEEKLY -> start.plusDays(13)
            BudgetPeriod.MONTHLY -> start.plusMonths(1).minusDays(1)
        }

        return PeriodKey(startDate = start, endDate = end, period = period)
    }

    private fun mapByCalendar(date: LocalDate, budgetPeriod: BudgetPeriod): PeriodKey {
        val period = budgetPeriod
        val start = when (period) {
            BudgetPeriod.DAILY -> date
            BudgetPeriod.WEEKLY -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            BudgetPeriod.BIWEEKLY -> {
                val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val weekOfYear = java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear().let { date.get(it) }
                if (weekOfYear % 2 == 0) weekStart.minusWeeks(1) else weekStart
            }
            BudgetPeriod.MONTHLY -> date.withDayOfMonth(1)
        }

        val end = when (period) {
            BudgetPeriod.DAILY -> start
            BudgetPeriod.WEEKLY -> start.plusDays(6)
            BudgetPeriod.BIWEEKLY -> start.plusDays(13)
            BudgetPeriod.MONTHLY -> start.plusMonths(1).minusDays(1)
        }

        return PeriodKey(startDate = start, endDate = end, period = period)
    }

    private fun resolveOccurrenceDates(
        transaction: Transaction,
        budgetSettings: BudgetSettings,
        today: LocalDate
    ): List<LocalDate> {
        val txDate = transaction.date?.toLocalDate() ?: return emptyList()
        if (!transaction.isRecurrent || transaction.recurrentFrequency == null) {
            return listOf(txDate)
        }

        val periodStart = budgetSettings.startDate
        val periodEnd = budgetSettings.getPeriodEndDate()
        val effectiveEnd = listOfNotNull(transaction.recurrentEndDate?.toLocalDate(), periodEnd, today).minOrNull() ?: periodEnd

        var current = txDate
        val occurrences = mutableListOf<LocalDate>()

        while (!current.isAfter(effectiveEnd)) {
            if (!current.isBefore(periodStart) && !current.isAfter(periodEnd)) {
                occurrences.add(current)
            }
            current = nextOccurrence(current, transaction.recurrentFrequency, transaction.subscriptionDay)
        }

        return if (occurrences.isEmpty()) listOf(txDate) else occurrences
    }

    private fun nextOccurrence(
        date: LocalDate,
        frequency: RecurrentFrequency?,
        subscriptionDay: Int?
    ): LocalDate {
        return when (frequency) {
            RecurrentFrequency.WEEKLY -> date.plusWeeks(1)
            RecurrentFrequency.BIWEEKLY -> date.plusWeeks(2)
            RecurrentFrequency.MONTHLY -> {
                val next = date.plusMonths(1)
                val targetDay = (subscriptionDay ?: date.dayOfMonth).coerceAtMost(next.lengthOfMonth())
                next.withDayOfMonth(targetDay)
            }
            null -> date
        }
    }
}
