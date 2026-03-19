package com.serranoie.app.minus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing budget settings.
 * Uses a single-row pattern with id = 1.
 */
@Entity(tableName = "budget_settings")
data class BudgetSettingsEntity(
    @PrimaryKey
    val id: Int = 1, // Single row pattern
    val totalBudget: String, // BigDecimal as String
    val period: String, // BudgetPeriod.name()
    val startDate: Long, // Epoch millis
    val endDate: Long? = null, // Optional end date
    val currencyCode: String,
    val daysInPeriod: Int = 1,
    val rollOverEnabled: Boolean = false,
    val rollOverCarryForward: Boolean = false,
    val remainingBudgetStrategy: String = "ASK_ALWAYS" // RemainingBudgetStrategy.name()
)
