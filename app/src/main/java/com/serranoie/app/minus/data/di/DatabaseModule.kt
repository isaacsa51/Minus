package com.serranoie.app.minus.data.di

import android.content.Context
import androidx.room.Room
import com.serranoie.app.minus.data.local.AppDatabase
import com.serranoie.app.minus.data.local.dao.BudgetSettingsDao
import com.serranoie.app.minus.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database dependencies.
 */
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
}
