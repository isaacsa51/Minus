package com.serranoie.app.minus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_settings")
data class BudgetSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val totalBudget: String,
    val period: String,
    val startDate: Long,
    val endDate: Long? = null,
    val currencyCode: String,
    val daysInPeriod: Int = 1,
    val rollOverEnabled: Boolean = false,
    val rollOverCarryForward: Boolean = false,
    val remainingBudgetStrategy: String = "ASK_ALWAYS",
    val creditCardCutoffDay: Int? = null,
    val splitMode: String = "STATIC"
)
