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
import com.serranoie.app.minus.domain.time.MidnightPeriodChecker
import com.serranoie.app.minus.domain.time.SystemTimeProvider
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import com.serranoie.app.minus.presentation.ui.budget.BudgetPeriodManager
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class VerifyFinishEarlyRolloverTest {

    @Test
    fun finishEarlyThenStartNewPeriodCarriesSurplusAndArchivesOldPeriod() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val zone = ZoneId.systemDefault()

        val database =
            Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
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
        val notificationScheduler =
            NotificationScheduler(context, budgetRepository, settingsRepository)
        val midnightPeriodChecker = MidnightPeriodChecker(budgetRepository, settingsRepository)
        val periodManager = BudgetPeriodManager(
            budgetRepository = budgetRepository,
            settingsRepository = settingsRepository,
            timeProvider = SystemTimeProvider(),
            notificationScheduler = notificationScheduler,
            midnightPeriodChecker = midnightPeriodChecker,
        )

        val before = requireNotNull(budgetRepository.getBudgetSettingsSync()) {
            "Run SeedFinishEarlyScenarioTest first"
        }
        require(before.totalBudget.compareTo(BigDecimal("1000.00")) == 0) {
            "Unexpected seed state, totalBudget=${before.totalBudget}"
        }
        val oldPeriodId = settingsRepository.getSettings().currentPeriodId

        val nowBeforeFinish = LocalDate.now()
        periodManager.finishBudgetEarly()
        val settingsAfterFinish = settingsRepository.getSettings()
        android.util.Log.i(
            "ROLLOVER_VERIFY",
            "DEBUG nowBeforeFinish=$nowBeforeFinish zone=$zone " +
                    "earlyFinishActualDate=${settingsAfterFinish.earlyFinishActualDate} " +
                    "decoded=${
                        java.time.Instant.ofEpochMilli(settingsAfterFinish.earlyFinishActualDate)
                            .atZone(zone).toLocalDate()
                    } " +
                    "decodedUtc=${
                        java.time.Instant.ofEpochMilli(settingsAfterFinish.earlyFinishActualDate)
                            .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    }"
        )

        val (pendingAfterFinish, _) = settingsRepository.getPendingRollover()
        check(pendingAfterFinish.compareTo(BigDecimal("700.00")) == 0) {
            "Expected 700.00 pending rollover after finishing early, got $pendingAfterFinish"
        }

        val today = LocalDate.now()
        val newSettings = BudgetSettings(
            totalBudget = BigDecimal("1200.00"),
            period = BudgetPeriod.MONTHLY,
            startDate = today,
            endDate = today.plusDays(29),
            currencyCode = "USD",
            remainingBudgetStrategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
        )
        periodManager.persistBudgetSettings(newSettings, forceNewPeriodBoundary = false)

        val after = requireNotNull(budgetRepository.getBudgetSettingsSync())
        check(after.totalBudget.compareTo(BigDecimal("1900.00")) == 0) {
            "FAIL: expected new totalBudget = 1200.00 (new income) + 700.00 (surplus) = 1900.00, got ${after.totalBudget}"
        }

        val (pendingAfterNewPeriod, _) = settingsRepository.getPendingRollover()
        check(pendingAfterNewPeriod.compareTo(BigDecimal.ZERO) == 0) {
            "FAIL: pending rollover should be cleared after being applied, got $pendingAfterNewPeriod"
        }

        val archived = database.archivedBudgetDao().getArchivedBudgetById(oldPeriodId)
        checkNotNull(archived) { "FAIL: old period ($oldPeriodId) was never archived" }
        check(archived.spentAmount == "300.00") {
            "FAIL: archived spentAmount should be 300.00, got ${archived.spentAmount}"
        }
        val archivedEndDate = LocalDate.ofEpochDay(archived.endDate / 86400000)
        check(archivedEndDate == today) {
            "FAIL: archived endDate should be today ($today, the actual early-finish date), got $archivedEndDate"
        }

        android.util.Log.i(
            "ROLLOVER_VERIFY",
            "PASS: newTotalBudget=${after.totalBudget} (1200 income + 700 surplus), " +
                    "archived old period spentAmount=${archived.spentAmount} endDate=$archivedEndDate"
        )

        database.close()
    }
}
