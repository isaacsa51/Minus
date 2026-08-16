package com.serranoie.app.minus.dev

import android.util.Log
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.serranoie.app.minus.data.local.AppDatabase
import com.serranoie.app.minus.data.local.AppDatabaseMigrations
import com.serranoie.app.minus.data.repository.SETTINGS_DATASTORE_NAME
import com.serranoie.app.minus.data.repository.SettingsRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InspectDbStateTest {

    @Test
    fun dumpCurrentState() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

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
        val settingsRepository = SettingsRepositoryImpl(dataStore)

        val settings = database.budgetSettingsDao().getSettingsSync()
        Log.i("DBDUMP", "budgetSettings row = $settings")

        val allTx = database.transactionDao().getAllTransactions().first()
        Log.i("DBDUMP", "transaction count = ${allTx.size}")
        allTx.forEach { Log.i("DBDUMP", "tx: id=${it.id} amount=${it.amount} date=${it.date} periodId=${it.periodId} comment=${it.comment}") }

        val queued = database.queuedTransactionDao().getAllQueuedTransactions().first()
        Log.i("DBDUMP", "queued transaction count = ${queued.size}")

        val userSettings = settingsRepository.getSettings()
        Log.i("DBDUMP", "userSettings currentPeriodId=${userSettings.currentPeriodId} currentPeriodStartedAt=${userSettings.currentPeriodStartedAt} onboardingCompleted=${userSettings.onboardingCompleted} earlyFinishActive=${userSettings.earlyFinishActive}")

        database.close()
    }
}
