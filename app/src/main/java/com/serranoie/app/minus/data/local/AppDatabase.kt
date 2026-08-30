package com.serranoie.app.minus.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.serranoie.app.minus.data.local.dao.ArchivedBudgetDao
import com.serranoie.app.minus.data.local.dao.BudgetSettingsDao
import com.serranoie.app.minus.data.local.dao.CategoryDao
import com.serranoie.app.minus.data.local.dao.PaidRecurrentOccurrenceDao
import com.serranoie.app.minus.data.local.dao.QueuedTransactionDao
import com.serranoie.app.minus.data.local.dao.TransactionDao
import com.serranoie.app.minus.data.local.entity.ArchivedBudgetEntity
import com.serranoie.app.minus.data.local.entity.BudgetSettingsEntity
import com.serranoie.app.minus.data.local.entity.CategoryEntity
import com.serranoie.app.minus.data.local.entity.PaidRecurrentOccurrenceEntity
import com.serranoie.app.minus.data.local.entity.QueuedTransactionEntity
import com.serranoie.app.minus.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        BudgetSettingsEntity::class,
        CategoryEntity::class,
        QueuedTransactionEntity::class,
        ArchivedBudgetEntity::class,
        PaidRecurrentOccurrenceEntity::class
    ],
    version = 17,
    autoMigrations = [
        AutoMigration(from = 16, to = 17)
    ],
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    abstract fun budgetSettingsDao(): BudgetSettingsDao

    abstract fun archivedBudgetDao(): ArchivedBudgetDao

    abstract fun categoryDao(): CategoryDao

    abstract fun queuedTransactionDao(): QueuedTransactionDao

    abstract fun paidRecurrentOccurrenceDao(): PaidRecurrentOccurrenceDao

    companion object {
        const val DATABASE_NAME = "minus_budget.db"
    }
}
