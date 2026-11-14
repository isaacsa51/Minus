package com.serranoie.app.minus.presentation.ui.e2e.pockets

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.serranoie.app.minus.data.local.AppDatabase
import com.serranoie.app.minus.data.repository.BudgetRepositoryImpl
import com.serranoie.app.minus.data.repository.SettingsRepositoryImpl
import com.serranoie.app.minus.domain.calculator.pocketBalances
import com.serranoie.app.minus.domain.calculator.pocketEntry
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.SavingsPocket
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.time.MidnightPeriodChecker
import com.serranoie.app.minus.domain.time.MidnightTransitionManager
import com.serranoie.app.minus.domain.time.TimeProvider
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import com.serranoie.app.minus.presentation.ui.budget.BudgetPeriodManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class PocketE2EHarness {

    val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    val database: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val budgetRepository = BudgetRepositoryImpl(
        appDatabase = database,
        transactionDao = database.transactionDao(),
        settingsDao = database.budgetSettingsDao(),
        archivedBudgetDao = database.archivedBudgetDao(),
        categoryDao = database.categoryDao(),
        queuedTransactionDao = database.queuedTransactionDao(),
        paidRecurrentOccurrenceDao = database.paidRecurrentOccurrenceDao(),
        savingsPocketDao = database.savingsPocketDao(),
    )
    private val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dataStoreFile = File(context.cacheDir, "pockets_${System.nanoTime()}.preferences_pb")
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = dataStoreScope,
        produceFile = { dataStoreFile },
    )
    val settingsRepository = SettingsRepositoryImpl(dataStore)
    val notificationScheduler: NotificationScheduler = mockk(relaxed = true)
    private val timeProvider: TimeProvider = mockk(relaxed = true)
    val checker = MidnightPeriodChecker(budgetRepository, settingsRepository)
    val transitionManager = MidnightTransitionManager(checker)
    val periodManager = BudgetPeriodManager(
        budgetRepository = budgetRepository,
        settingsRepository = settingsRepository,
        timeProvider = timeProvider,
        notificationScheduler = notificationScheduler,
        midnightPeriodChecker = checker,
    )

    val today: LocalDate = LocalDate.now()
    private var clock = System.currentTimeMillis()

    init {
        every { timeProvider.nowEpochMillis() } answers { clock }
    }

    fun close() {
        database.close()
        dataStoreScope.cancel()
        dataStoreFile.delete()
    }

    fun settings(
        start: LocalDate,
        end: LocalDate,
        totalBudget: BigDecimal,
        strategy: RemainingBudgetStrategy = RemainingBudgetStrategy.ASK_ALWAYS,
    ) = BudgetSettings(
        totalBudget = totalBudget,
        period = BudgetPeriod.MONTHLY,
        startDate = start,
        endDate = end,
        currencyCode = "USD",
        daysInPeriod = (java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1).toInt(),
        remainingBudgetStrategy = strategy,
        splitMode = BudgetSplitMode.STATIC,
    )

    fun startPeriod(
        start: LocalDate,
        end: LocalDate,
        totalBudget: BigDecimal,
        strategy: RemainingBudgetStrategy = RemainingBudgetStrategy.ASK_ALWAYS,
    ): BudgetSettings = runBlocking {
        clock += 60_000
        periodManager.persistBudgetSettings(settings(start, end, totalBudget, strategy), forceNewPeriodBoundary = false)
        budgetRepository.getBudgetSettingsSync()!!
    }

    fun currentPeriodId(): Long = runBlocking { settingsRepository.getCurrentPeriodId() }

    fun spend(amount: String, at: LocalDateTime, comment: String = "Groceries") = runBlocking {
        clock += 60_000
        budgetRepository.addTransaction(
            Transaction(
                amount = BigDecimal(amount),
                comment = comment,
                date = at,
                createdAt = clock,
                periodId = settingsRepository.getCurrentPeriodId(),
            ),
        )
    }

    fun createPocket(name: String, target: BigDecimal? = null): SavingsPocket = runBlocking {
        clock += 60_000
        val id = budgetRepository.saveSavingsPocket(SavingsPocket(name = name, target = target, createdAt = clock))
        budgetRepository.getSavingsPockets().first().single { it.id == id }
    }

    fun moveMoney(pocket: SavingsPocket, amount: String, at: LocalDateTime, withdraw: Boolean = false): Boolean =
        runBlocking {
            val entry = pocketEntry(
                pocket = pocket,
                amount = BigDecimal(amount),
                withdraw = withdraw,
                periodId = settingsRepository.getCurrentPeriodId(),
                date = at,
            )
            if (withdraw) budgetRepository.addPocketWithdrawal(entry) else budgetRepository.addPocketDeposit(entry)
        }

    fun transactions(): List<Transaction> = runBlocking { budgetRepository.getTransactions().first() }

    fun pockets(): List<SavingsPocket> = runBlocking { budgetRepository.getSavingsPockets().first() }

    fun balanceOf(pocket: SavingsPocket): BigDecimal = pocketBalances(transactions())[pocket.id] ?: BigDecimal.ZERO

    fun pendingRollover(): Pair<BigDecimal, RemainingBudgetStrategy?> =
        runBlocking { settingsRepository.getPendingRollover() }

    fun appStart() = runBlocking { transitionManager.handleAppStart() }

    fun finishEarly() = runBlocking {
        clock += 60_000
        periodManager.finishBudgetEarly()
    }
}
