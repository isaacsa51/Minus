package com.serranoie.app.minus.dev

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.serranoie.app.minus.data.local.AppDatabase
import com.serranoie.app.minus.data.local.AppDatabaseMigrations
import com.serranoie.app.minus.data.repository.BudgetRepositoryImpl
import com.serranoie.app.minus.data.repository.SETTINGS_DATASTORE_NAME
import com.serranoie.app.minus.data.repository.SettingsRepositoryImpl
import com.serranoie.app.minus.domain.calculator.BudgetCalculator
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.Transaction
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class SeedFinishEarlyScenarioTest {

    @Test
    fun seedActivePeriodWithSpendingTwentyDaysIn() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val zone = ZoneId.systemDefault()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(
                AppDatabaseMigrations.MIGRATION_6_7,
                AppDatabaseMigrations.MIGRATION_8_9,
                AppDatabaseMigrations.MIGRATION_9_10,
                AppDatabaseMigrations.MIGRATION_10_11,
                AppDatabaseMigrations.MIGRATION_11_12,
                AppDatabaseMigrations.MIGRATION_12_13,
                AppDatabaseMigrations.MIGRATION_13_14,
            )
            .build()

        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(SETTINGS_DATASTORE_NAME) }
        )

        val budgetRepository = BudgetRepositoryImpl(
            appDatabase = database,
            transactionDao = database.transactionDao(),
            settingsDao = database.budgetSettingsDao(),
            archivedBudgetDao = database.archivedBudgetDao(),
            categoryDao = database.categoryDao(),
            queuedTransactionDao = database.queuedTransactionDao(),
            budgetCalculator = BudgetCalculator(),
        )
        val settingsRepository = SettingsRepositoryImpl(dataStore)

        val today = LocalDate.now()
        val startDate = today.minusDays(20)
        val endDate = today.plusDays(10)
        val periodId = startDate.atStartOfDay(zone).toInstant().toEpochMilli()

        val settings = BudgetSettings(
            totalBudget = BigDecimal("1000.00"),
            period = BudgetPeriod.MONTHLY,
            startDate = startDate,
            endDate = endDate,
            currencyCode = "USD",
            daysInPeriod = 30,
            remainingBudgetStrategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
        )
        budgetRepository.saveBudgetSettings(settings)

        val spends = listOf(
            "Groceries" to BigDecimal("50.00") to 18L,
            "Gas" to BigDecimal("80.00") to 15L,
            "Coffee" to BigDecimal("40.00") to 12L,
            "Dinner" to BigDecimal("70.00") to 8L,
            "Movies" to BigDecimal("60.00") to 3L,
        )
        spends.forEach { (commentAndAmount, daysAgo) ->
            val (comment, amount) = commentAndAmount
            budgetRepository.addTransaction(
                Transaction.create(
                    amount = amount,
                    comment = comment,
                    date = today.minusDays(daysAgo).atTime(12, 0),
                    periodId = periodId,
                )
            )
        }

        settingsRepository.setOnboardingCompleted(true)
        settingsRepository.setCurrentPeriod(periodId = periodId, startedAt = periodId)
        settingsRepository.setBudgetEndDate(endDate.atStartOfDay(zone).toInstant().toEpochMilli())

        database.close()
    }
}
