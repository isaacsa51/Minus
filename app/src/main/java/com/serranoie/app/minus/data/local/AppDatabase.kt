package com.serranoie.app.minus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.serranoie.app.minus.data.local.dao.BudgetSettingsDao
import com.serranoie.app.minus.data.local.dao.TransactionDao
import com.serranoie.app.minus.data.local.entity.BudgetSettingsEntity
import com.serranoie.app.minus.data.local.entity.TransactionEntity

/**
 * Room database for the Minus budget app.
 * Contains transactions and budget settings.
 */
@Database(
    entities = [
        TransactionEntity::class,
        BudgetSettingsEntity::class
    ],
    version = 6, // Added periodId field to TransactionEntity
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    abstract fun budgetSettingsDao(): BudgetSettingsDao

    companion object {
        const val DATABASE_NAME = "minus_budget.db"
    }
}
