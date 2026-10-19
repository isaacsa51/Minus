package com.serranoie.app.minus.presentation.island

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.serranoie.app.minus.domain.model.BudgetPeriod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal
import java.time.LocalDate

data class IslandBudgetSnapshot(
    val date: LocalDate,
    val currency: String,
    val viewPeriod: BudgetPeriod,
    val splitRemaining: BigDecimal,
    val isOverBudget: Boolean,
    val isOverSplit: Boolean,
    val periodLeft: BigDecimal,
    val daysLeft: Int,
    val totalBudget: BigDecimal,
    val settingsTotalBudget: BigDecimal,
    val rollOverLimit: BigDecimal?,
    val creditOwed: BigDecimal,
    val startDate: LocalDate,
    val finishDate: LocalDate,
) {
    val isAlert: Boolean get() = isOverBudget || isOverSplit
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
        if (state.value == snapshot) return
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
            putString("totalBudget", snapshot.totalBudget.toPlainString())
            putString("settingsTotalBudget", snapshot.settingsTotalBudget.toPlainString())
            putString("rollOverLimit", snapshot.rollOverLimit?.toPlainString())
            putString("creditOwed", snapshot.creditOwed.toPlainString())
            putString("startDate", snapshot.startDate.toString())
            putString("finishDate", snapshot.finishDate.toString())
        }
    }

    private fun read(context: Context): IslandBudgetSnapshot? = runCatching {
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
            totalBudget = prefs.decimal("totalBudget"),
            settingsTotalBudget = prefs.decimal("settingsTotalBudget"),
            rollOverLimit = prefs.getString("rollOverLimit", null)?.let(::BigDecimal),
            creditOwed = prefs.decimal("creditOwed"),
            startDate = LocalDate.parse(prefs.require("startDate")),
            finishDate = LocalDate.parse(prefs.require("finishDate")),
        )
    }.getOrNull()

    private fun SharedPreferences.require(key: String) = requireNotNull(getString(key, null))

    private fun SharedPreferences.decimal(key: String) = BigDecimal(require(key))
}
