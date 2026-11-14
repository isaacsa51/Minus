package com.serranoie.app.minus.domain.calculator

import com.serranoie.app.minus.domain.model.SavingsPocket
import com.serranoie.app.minus.domain.model.SavingsPreferences
import com.serranoie.app.minus.domain.model.Transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class SavingsPocketStatus(val pocket: SavingsPocket, val balance: BigDecimal)

internal fun pocketBalances(transactions: List<Transaction>): Map<Long, BigDecimal> =
    transactions
        .filter { it.pocketId != null && !it.isDeleted }
        .groupBy { it.pocketId!! }
        .mapValues { (_, rows) -> rows.sumOf { it.amount } }

internal fun pocketStatuses(pockets: List<SavingsPocket>, transactions: List<Transaction>): List<SavingsPocketStatus> {
    val balances = pocketBalances(transactions)
    return pockets.map { SavingsPocketStatus(it, balances[it.id] ?: BigDecimal.ZERO) }
}

internal fun pocketEntry(
    pocket: SavingsPocket,
    amount: BigDecimal,
    withdraw: Boolean,
    periodId: Long,
    date: LocalDateTime,
    label: String = pocket.name,
): Transaction = Transaction(
    amount = if (withdraw) amount.abs().negate() else amount.abs(),
    comment = label,
    date = date,
    periodId = periodId,
    isAdjustment = true,
    pocketId = pocket.id,
)

internal fun goalPocketUpdate(
    existing: SavingsPocket?,
    previous: SavingsPreferences?,
    updated: SavingsPreferences,
    today: LocalDate,
): SavingsPocket? {
    val target = updated.savingsGoalAmount?.takeIf { it.signum() > 0 }
    val deadline = target?.let { updated.savingsGoalMonths?.takeIf { it > 0 } }?.let { today.plusMonths(it.toLong()) }
    val goalChanged = previous != null && (
        previous.savingsGoalAmount?.stripTrailingZeros() != updated.savingsGoalAmount?.stripTrailingZeros() ||
            previous.savingsGoalMonths != updated.savingsGoalMonths
        )
    return when {
        existing == null -> target?.let { SavingsPocket(name = "", target = it, targetDate = deadline) }
        goalChanged -> existing.copy(target = target, targetDate = deadline).takeIf { it != existing }
        else -> null
    }
}

data class PocketPeriodFlow(val added: BigDecimal, val withdrawn: BigDecimal)

internal fun periodPocketFlow(transactions: List<Transaction>): PocketPeriodFlow? {
    val rows = transactions.filter { it.pocketId != null && !it.isDeleted }
    if (rows.isEmpty()) return null
    return PocketPeriodFlow(
        added = rows.filter { it.amount.signum() > 0 }.sumOf { it.amount },
        withdrawn = rows.filter { it.amount.signum() < 0 }.sumOf { it.amount.abs() },
    )
}
