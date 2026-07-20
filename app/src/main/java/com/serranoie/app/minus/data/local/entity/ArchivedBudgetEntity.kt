package com.serranoie.app.minus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "archived_budgets")
data class ArchivedBudgetEntity(
    @PrimaryKey
    val periodId: Long,
    val totalBudget: String,
    val spentAmount: String,
    val startDate: Long,
    val endDate: Long,
    val currencyCode: String,
    val periodType: String,
    val createdAt: Long = System.currentTimeMillis()
)
