package com.serranoie.app.minus.data.di

import android.content.Context
import androidx.room.Room
import com.serranoie.app.minus.data.local.AppDatabase
import com.serranoie.app.minus.data.local.AppDatabaseMigrations
import com.serranoie.app.minus.data.local.dao.ArchivedBudgetDao
import com.serranoie.app.minus.data.local.dao.BudgetSettingsDao
import com.serranoie.app.minus.data.local.dao.CategoryDao
import com.serranoie.app.minus.data.local.dao.QueuedTransactionDao
import com.serranoie.app.minus.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(AppDatabaseMigrations.MIGRATION_6_7)
            .addMigrations(AppDatabaseMigrations.MIGRATION_8_9)
            .addMigrations(AppDatabaseMigrations.MIGRATION_9_10)
            .addMigrations(AppDatabaseMigrations.MIGRATION_10_11)
            .addMigrations(AppDatabaseMigrations.MIGRATION_11_12)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideBudgetSettingsDao(database: AppDatabase): BudgetSettingsDao {
        return database.budgetSettingsDao()
    }

    @Provides
    fun provideArchivedBudgetDao(database: AppDatabase): ArchivedBudgetDao {
        return database.archivedBudgetDao()
    }

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideQueuedTransactionDao(database: AppDatabase): QueuedTransactionDao {
        return database.queuedTransactionDao()
    }
}
