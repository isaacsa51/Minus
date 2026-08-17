package com.serranoie.app.minus.presentation.ui.history

import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import java.time.LocalDate

internal fun buildDisplayTransactions(
    transactions: List<Transaction>,
    pendingRemovedTransactions: Map<Long, Transaction>,
): List<Transaction> = transactions + pendingRemovedTransactions.values.filterNot { pending ->
    transactions.any { it.id == pending.id }
}

internal fun splitPeriodTransactions(
    transactions: List<Transaction>,
    budgetStartDate: LocalDate,
    budgetEndDate: LocalDate,
    currentPeriodStartedAtMillis: Long,
    currentPeriodId: Long,
    previousPeriodId: Long,
): Pair<List<Transaction>, List<Transaction>> {
    val sorted = transactions.sortedByDescending { it.date }
    val current = sorted.filter { transaction ->
        if (currentPeriodId > 0L && transaction.periodId > 0L) {
            return@filter transaction.periodId == currentPeriodId
        }
        val txDate = transaction.date?.toLocalDate() ?: return@filter false
        if (txDate.isBefore(budgetStartDate) || txDate.isAfter(budgetEndDate)) {
            return@filter false
        }
        if (txDate.isEqual(budgetStartDate) && currentPeriodStartedAtMillis > 0L) {
            return@filter transaction.createdAt >= currentPeriodStartedAtMillis
        }
        true
    }
    val past = sorted.filter { transaction ->
        if (currentPeriodId > 0L && transaction.periodId > 0L) {
            return@filter transaction.periodId == previousPeriodId
        }
        val txDate = transaction.date?.toLocalDate() ?: return@filter false
        txDate.isBefore(budgetStartDate) || (txDate.isEqual(budgetStartDate) && currentPeriodStartedAtMillis > 0L && transaction.createdAt < currentPeriodStartedAtMillis)
    }
    return current to past
}

internal fun buildUpcomingRecurrentItems(
    transactions: List<Transaction>,
    budgetStartDate: LocalDate,
    budgetEndDate: LocalDate,
    today: LocalDate,
    paidOccurrences: Set<PaidRecurrentOccurrence> = emptySet(),
): Pair<List<UpcomingRecurrentItem>, List<UpcomingRecurrentItem>> {
    val recurrentTransactions = transactions.filter { it.isRecurrent }

    val upcomingInPeriod = recurrentTransactions.mapNotNull { transaction ->
        val savedDate = transaction.date?.toLocalDate()
        val nextDate = calculateNextChargeDate(transaction, today) ?: savedDate?.takeIf { date ->
            date.isAfter(today)
        }
        nextDate?.let { date ->
            if (!date.isBefore(budgetStartDate) && !date.isAfter(budgetEndDate) &&
                !paidOccurrences.contains(PaidRecurrentOccurrence(transaction.id, date))
            ) {
                UpcomingRecurrentItem(
                    transaction = transaction,
                    nextChargeDate = date,
                    isInCurrentPeriod = true,
                )
            } else {
                null
            }
        }
    }.distinctBy { it.transaction.id }.sortedBy { it.nextChargeDate }

    val futureOutOfPeriod = recurrentTransactions.mapNotNull { transaction ->
        calculateNextChargeDate(transaction, today)?.let { nextDate ->
            if (nextDate.isAfter(budgetEndDate) &&
                !paidOccurrences.contains(PaidRecurrentOccurrence(transaction.id, nextDate))
            ) {
                UpcomingRecurrentItem(
                    transaction = transaction,
                    nextChargeDate = nextDate,
                    isInCurrentPeriod = false,
                )
            } else {
                null
            }
        }
    }.filter { item -> !upcomingInPeriod.any { it.transaction.id == item.transaction.id } }
        .sortedBy { it.nextChargeDate }

    return upcomingInPeriod to futureOutOfPeriod
}

internal fun buildGroupedCurrentTransactions(
    currentPeriodTransactions: List<Transaction>,
    displayTransactions: List<Transaction>,
    budgetStartDate: LocalDate,
    budgetEndDate: LocalDate,
    today: LocalDate,
    paidOccurrences: Set<PaidRecurrentOccurrence> = emptySet(),
): Map<LocalDate?, List<Transaction>> {
    val regularTransactions = currentPeriodTransactions.filterNot { it.isRecurrent }
    val recurrentCharges = displayTransactions.filter { it.isRecurrent && !it.isDeleted }
        .flatMap { transaction ->
            getRecurringChargesInPeriod(transaction, budgetStartDate, budgetEndDate, today, paidOccurrences)
        }

    return groupTransactionsByDate(regularTransactions + recurrentCharges)
}

internal fun groupTransactionsByDate(
    transactions: List<Transaction>,
): Map<LocalDate?, List<Transaction>> = transactions.groupBy { it.date?.toLocalDate() }
    .toSortedMap(compareByDescending { it })

internal fun calculateNextChargeDate(transaction: Transaction, today: LocalDate): LocalDate? {
    if (!transaction.isRecurrent) {
        return null
    }

    val frequency = transaction.recurrentFrequency ?: return null
    val startDate = transaction.date?.toLocalDate() ?: return null
    val endDate = transaction.recurrentEndDate?.toLocalDate()

    if (endDate != null && today.isAfter(endDate)) {
        return null
    }

    return when (frequency) {
        RecurrentFrequency.WEEKLY -> {
            var nextDate = startDate
            while (!nextDate.isAfter(today)) {
                nextDate = nextDate.plusWeeks(1)
            }
            if (endDate == null || !nextDate.isAfter(endDate)) nextDate else null
        }

        RecurrentFrequency.BIWEEKLY -> {
            var nextDate = startDate
            while (!nextDate.isAfter(today)) {
                nextDate = nextDate.plusWeeks(2)
            }
            if (endDate == null || !nextDate.isAfter(endDate)) nextDate else null
        }

        RecurrentFrequency.MONTHLY -> {
            val billingDay = transaction.subscriptionDay ?: startDate.dayOfMonth
            var nextDate = today.withDayOfMonth(billingDay.coerceAtMost(today.lengthOfMonth()))

            if (today.dayOfMonth >= billingDay) {
                nextDate = nextDate.plusMonths(1)
                val maxDay = nextDate.lengthOfMonth()
                if (billingDay > maxDay) {
                    nextDate = nextDate.withDayOfMonth(maxDay)
                }
            }

            if (endDate != null && nextDate.isAfter(endDate)) null else nextDate
        }
    }
}

internal fun getRecurringChargesInPeriod(
    transaction: Transaction,
    periodStart: LocalDate,
    periodEnd: LocalDate,
    today: LocalDate,
    paidOccurrences: Set<PaidRecurrentOccurrence> = emptySet(),
): List<Transaction> {
    val frequency = transaction.recurrentFrequency ?: return emptyList()
    val originalDateTime = transaction.date ?: return emptyList()
    val startDate = originalDateTime.toLocalDate()
    val originalTime = originalDateTime.toLocalTime()
    val subscriptionEnd = transaction.recurrentEndDate?.toLocalDate() ?: periodEnd.plusMonths(1)

    val virtualTransactions = mutableListOf<Transaction>()
    var chargeDate = startDate

    while (!chargeDate.isAfter(subscriptionEnd)) {
        if (!chargeDate.isBefore(periodStart) && !chargeDate.isAfter(periodEnd) &&
            !chargeDate.isAfter(today) &&
            !paidOccurrences.contains(PaidRecurrentOccurrence(transaction.id, chargeDate))
        ) {
            virtualTransactions.add(
                transaction.copy(
                    date = chargeDate.atTime(originalTime),
                    id = transaction.id * 1000000 + chargeDate.toEpochDay(),
                    sourceTransactionId = transaction.sourceTransactionId ?: transaction.id,
                )
            )
        }

        chargeDate = when (frequency) {
            RecurrentFrequency.WEEKLY -> chargeDate.plusWeeks(1)
            RecurrentFrequency.BIWEEKLY -> chargeDate.plusWeeks(2)
            RecurrentFrequency.MONTHLY -> {
                val billingDay = transaction.subscriptionDay ?: startDate.dayOfMonth
                val nextMonth = chargeDate.plusMonths(1)
                val maxDay = nextMonth.lengthOfMonth()
                nextMonth.withDayOfMonth(billingDay.coerceAtMost(maxDay))
            }
        }
    }

    return virtualTransactions
}
