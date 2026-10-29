package com.serranoie.app.minus.presentation.island

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.history.buildUpcomingRecurrentItems
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal
import java.time.LocalDate

data class IslandBudgetSnapshot(
    val date: LocalDate,
    val currency: String,
    val viewPeriod: BudgetPeriod,
    val splitRemaining: BigDecimal,
    val splitSpent: BigDecimal = BigDecimal.ZERO,
    val splitBudget: BigDecimal = BigDecimal.ZERO,
    val spendProgress: Float = 0f,
    val isOverBudget: Boolean,
    val isOverSplit: Boolean,
    val periodLeft: BigDecimal,
    val daysLeft: Int,
    val creditOwed: BigDecimal,
    val nextCharge: IslandCharge? = null,
    val startDate: LocalDate,
    val finishDate: LocalDate,
) {
    val isAlert: Boolean get() = isOverBudget || isOverSplit
}

data class IslandCharge(
    val name: String,
    val amount: BigDecimal,
    val date: LocalDate,
)

internal fun IslandBudgetSnapshot.overspend(): BigDecimal? =
    if (isOverSplit) (splitSpent - splitBudget).takeIf { it.signum() == 1 } else null

internal fun nextIslandCharge(
    transactions: List<Transaction>,
    paidOccurrences: Set<PaidRecurrentOccurrence>,
    today: LocalDate,
): IslandCharge? {
    val (inPeriod, later) = buildUpcomingRecurrentItems(
        transactions = transactions.filterNot { it.isDeleted },
        budgetStartDate = LocalDate.MIN,
        budgetEndDate = LocalDate.MAX,
        today = today.minusDays(1),
        paidOccurrences = paidOccurrences,
    )
    return (inPeriod + later)
        .filter { !it.nextChargeDate.isBefore(today) && !it.nextChargeDate.isAfter(today.plusDays(1)) }
        .minByOrNull { it.nextChargeDate }
        ?.let { IslandCharge(it.transaction.comment, it.transaction.amount, it.nextChargeDate) }
}

object IslandBudgetStore {
    private const val PREFS = "island_budget"
    private val state = MutableStateFlow<IslandBudgetSnapshot?>(null)
    private var loaded = false

    fun observe(context: Context): StateFlow<IslandBudgetSnapshot?> {
        if (!loaded) {
            loaded = true
            state.compareAndSet(null, read(context))
        }
        return state
    }

    fun update(context: Context, snapshot: IslandBudgetSnapshot?) {
        loaded = true
        if (snapshot != null && state.value == snapshot) return
        state.value = snapshot
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            clear()
            if (snapshot == null) return@edit
            putString("date", snapshot.date.toString())
            putString("currency", snapshot.currency)
            putString("viewPeriod", snapshot.viewPeriod.name)
            putString("splitRemaining", snapshot.splitRemaining.toPlainString())
            putBoolean("isOverBudget", snapshot.isOverBudget)
            putBoolean("isOverSplit", snapshot.isOverSplit)
            putString("periodLeft", snapshot.periodLeft.toPlainString())
            putInt("daysLeft", snapshot.daysLeft)
            putString("splitSpent", snapshot.splitSpent.toPlainString())
            putString("splitBudget", snapshot.splitBudget.toPlainString())
            putFloat("spendProgress", snapshot.spendProgress)
            putString("creditOwed", snapshot.creditOwed.toPlainString())
            snapshot.nextCharge?.let { charge ->
                putString("chargeName", charge.name)
                putString("chargeAmount", charge.amount.toPlainString())
                putString("chargeDate", charge.date.toString())
            }
            putString("startDate", snapshot.startDate.toString())
            putString("finishDate", snapshot.finishDate.toString())
        }
    }

    internal fun read(context: Context): IslandBudgetSnapshot? = runCatching {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val date = prefs.getString("date", null) ?: return null
        IslandBudgetSnapshot(
            date = LocalDate.parse(date),
            currency = prefs.getString("currency", null) ?: "USD",
            viewPeriod = BudgetPeriod.valueOf(prefs.require("viewPeriod")),
            splitRemaining = prefs.decimal("splitRemaining"),
            isOverBudget = prefs.getBoolean("isOverBudget", false),
            isOverSplit = prefs.getBoolean("isOverSplit", false),
            periodLeft = prefs.decimal("periodLeft"),
            daysLeft = prefs.getInt("daysLeft", 0),
            splitSpent = prefs.optionalDecimal("splitSpent"),
            splitBudget = prefs.optionalDecimal("splitBudget"),
            spendProgress = prefs.getFloat("spendProgress", 0f),
            creditOwed = prefs.decimal("creditOwed"),
            nextCharge = prefs.getString("chargeDate", null)?.let { date ->
                IslandCharge(
                    name = prefs.getString("chargeName", null).orEmpty(),
                    amount = prefs.decimal("chargeAmount"),
                    date = LocalDate.parse(date),
                )
            },
            startDate = LocalDate.parse(prefs.require("startDate")),
            finishDate = LocalDate.parse(prefs.require("finishDate")),
        )
    }.getOrNull()

    private fun SharedPreferences.require(key: String) = requireNotNull(getString(key, null))

    private fun SharedPreferences.decimal(key: String) = BigDecimal(require(key))

    private fun SharedPreferences.optionalDecimal(key: String) =
        getString(key, null)?.let(::BigDecimal) ?: BigDecimal.ZERO
}
