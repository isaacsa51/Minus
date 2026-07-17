package com.serranoie.app.minus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.serranoie.app.minus.data.local.dao.BudgetSettingsDao
import com.serranoie.app.minus.data.local.dao.CategoryDao
import com.serranoie.app.minus.data.local.dao.QueuedTransactionDao
import com.serranoie.app.minus.data.local.dao.TransactionDao
import com.serranoie.app.minus.data.local.entity.BudgetSettingsEntity
import com.serranoie.app.minus.data.local.entity.CategoryEntity
import com.serranoie.app.minus.data.local.entity.QueuedTransactionEntity
import com.serranoie.app.minus.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        BudgetSettingsEntity::class,
        CategoryEntity::class,
        QueuedTransactionEntity::class
    ],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    abstract fun budgetSettingsDao(): BudgetSettingsDao

    abstract fun categoryDao(): CategoryDao

    abstract fun queuedTransactionDao(): QueuedTransactionDao

    companion object {
        const val DATABASE_NAME = "minus_budget.db"
    }
}
