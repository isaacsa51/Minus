package com.serranoie.app.minus.presentation.ui.e2e.budget

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import com.serranoie.app.minus.data.local.AppDatabase
import com.serranoie.app.minus.data.repository.BudgetRepositoryImpl
import com.serranoie.app.minus.data.repository.SettingsRepositoryImpl
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.time.MidnightPeriodChecker
import com.serranoie.app.minus.domain.time.TimeProvider
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import com.serranoie.app.minus.presentation.ui.budget.BudgetPeriodManager
import com.serranoie.app.minus.presentation.ui.budget.BudgetStateCalculator
import com.serranoie.app.minus.presentation.ui.history.splitPeriodTransactions
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Money spent in period A must never be counted against period B, and must drop off the current
 * half of History the moment B starts - including when B starts on the same calendar day A was
 * closed, which is the case every date-range fallback gets wrong.
 */
@RunWith(AndroidJUnit4::class)
class PeriodBoundaryLeakE2ETest {

    private lateinit var database: AppDatabase
    private lateinit var budgetRepository: BudgetRepositoryImpl
    private lateinit var settingsRepository: SettingsRepositoryImpl
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStoreFile: File
    private lateinit var periodManager: BudgetPeriodManager
    private lateinit var checker: MidnightPeriodChecker

    private val calculator = BudgetStateCalculator()
    private val notificationScheduler: NotificationScheduler = mockk(relaxed = true)
    private val timeProvider: TimeProvider = mockk(relaxed = true)

    private val today: LocalDate = LocalDate.now()
    private var clock = System.currentTimeMillis()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        budgetRepository = BudgetRepositoryImpl(
            appDatabase = database,
            transactionDao = database.transactionDao(),
            settingsDao = database.budgetSettingsDao(),
            archivedBudgetDao = database.archivedBudgetDao(),
            categoryDao = database.categoryDao(),
            queuedTransactionDao = database.queuedTransactionDao(),
            paidRecurrentOccurrenceDao = database.paidRecurrentOccurrenceDao(),
        )
        dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dataStoreFile = File(context.cacheDir, "leak_${System.nanoTime()}.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        settingsRepository = SettingsRepositoryImpl(dataStore)
        checker = MidnightPeriodChecker(budgetRepository, settingsRepository)
        periodManager = BudgetPeriodManager(
            budgetRepository = budgetRepository,
            settingsRepository = settingsRepository,
            timeProvider = timeProvider,
            notificationScheduler = notificationScheduler,
            midnightPeriodChecker = checker,
        )
        every { timeProvider.nowEpochMillis() } answers { clock }
    }

    @After
    fun tearDown() {
        database.close()
        dataStoreScope.cancel()
        dataStoreFile.delete()
    }

    private fun settings(
        start: LocalDate,
        end: LocalDate,
        totalBudget: BigDecimal = BigDecimal("1500.00"),
    ) = BudgetSettings(
        totalBudget = totalBudget,
        period = BudgetPeriod.MONTHLY,
        startDate = start,
        endDate = end,
        currencyCode = "USD",
        daysInPeriod = 30,
        remainingBudgetStrategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
        splitMode = BudgetSplitMode.STATIC,
    )

    private fun applyPeriod(start: LocalDate, end: LocalDate, totalBudget: BigDecimal) {
        clock += 60_000
        runBlocking {
            periodManager.persistBudgetSettings(
                settings = settings(start, end, totalBudget),
                forceNewPeriodBoundary = false,
            )
        }
    }

    private fun spend(amount: BigDecimal, at: LocalDateTime, comment: String) {
        clock += 60_000
        runBlocking {
            budgetRepository.addTransaction(
                Transaction(
                    amount = amount,
                    comment = comment,
                    date = at,
                    createdAt = clock,
                    periodId = settingsRepository.getCurrentPeriodId(),
                ),
            )
        }
    }

    private fun finishEarly() {
        clock += 60_000
        runBlocking { periodManager.finishBudgetEarly() }
    }

    private class Screen(
        val currentPeriod: List<Transaction>,
        val pastPeriod: List<Transaction>,
        val pillTransactions: List<Transaction>,
        val spentInPeriod: BigDecimal,
        val diagnostic: String,
    )

    private fun readScreen(): Screen = runBlocking {
        val stored = budgetRepository.getBudgetSettingsSync()!!
        val transactions = budgetRepository.getTransactions().first()
        val user = settingsRepository.getSettings()

        val split = splitPeriodTransactions(
            transactions = transactions,
            budgetStartDate = stored.startDate,
            budgetEndDate = stored.getPeriodEndDate(),
            currentPeriodStartedAtMillis = user.currentPeriodStartedAt,
            currentPeriodId = user.currentPeriodId,
        )
        val pill = calculator.filterPeriodTransactions(
            transactions = transactions,
            settings = stored,
            currentPeriodId = user.currentPeriodId,
            currentPeriodStartedAtMillis = user.currentPeriodStartedAt,
        )
        val state = calculator.calculateBudgetState(
            settings = stored,
            transactions = pill,
            currentDate = LocalDate.now(),
            allTransactions = transactions,
        )
        val diagnostic = buildString {
            append("window=${stored.startDate}..${stored.getPeriodEndDate()} ")
            append("currentPeriodId=${user.currentPeriodId} ")
            append("currentPeriodStartedAt=${user.currentPeriodStartedAt} ")
            append("earlyFinishActive=${user.earlyFinishActive} ")
            append("endHandled=${user.periodEndAlreadyHandled} ")
            append("stored=")
            append(transactions.joinToString { "${it.comment}(periodId=${it.periodId},createdAt=${it.createdAt})" })
        }
        Screen(split.first, split.second, pill, state.totalSpentInPeriod, diagnostic)
    }

    private fun assertHidden(screen: Screen, comment: String) {
        assertWithMessage("History still lists it. %s", screen.diagnostic)
            .that(screen.currentPeriod.map { it.comment }).doesNotContain(comment)
        assertWithMessage("The pill still counts it. %s", screen.diagnostic)
            .that(screen.pillTransactions.map { it.comment }).doesNotContain(comment)
        assertWithMessage("Spent did not reset. %s", screen.diagnostic)
            .that(screen.spentInPeriod).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun spend_of_a_period_finished_early_is_hidden_when_the_next_period_keeps_the_same_start_day() {
        applyPeriod(today, today.plusDays(7), BigDecimal("1500.00"))
        spend(BigDecimal("55.50"), today.atTime(10, 0), "Period A spend")
        finishEarly()

        applyPeriod(today, today.plusDays(4), BigDecimal("1500.00"))

        assertHidden(readScreen(), "Period A spend")
    }

    @Test
    fun spend_of_a_period_finished_early_is_hidden_when_the_next_period_keeps_the_start_day_and_runs_longer() {
        applyPeriod(today, today.plusDays(4), BigDecimal("550.00"))
        spend(BigDecimal("100.00"), today.atTime(10, 0), "Period A spend")
        finishEarly()

        applyPeriod(today, today.plusDays(15), BigDecimal("550.00"))

        assertHidden(readScreen(), "Period A spend")
    }

    @Test
    fun spend_of_a_period_finished_early_is_hidden_when_the_next_period_starts_the_same_day() {
        applyPeriod(today.minusDays(10), today.plusDays(7), BigDecimal("1500.00"))
        spend(BigDecimal("55.50"), today.atTime(10, 0), "Period A spend")
        finishEarly()

        applyPeriod(today, today.plusDays(4), BigDecimal("1500.00"))

        assertHidden(readScreen(), "Period A spend")
    }

    @Test
    fun spend_of_a_lapsed_period_is_hidden_when_the_next_period_is_backdated_onto_its_last_day() {
        applyPeriod(today.minusDays(10), today.minusDays(1), BigDecimal("1500.00"))
        spend(BigDecimal("55.50"), today.minusDays(1).atTime(10, 0), "Period A spend")

        applyPeriod(today.minusDays(1), today.plusDays(4), BigDecimal("1500.00"))

        assertHidden(readScreen(), "Period A spend")
    }

    @Test
    fun spend_of_a_period_finished_early_moves_to_the_past_half_of_history() {
        applyPeriod(today, today.plusDays(7), BigDecimal("1500.00"))
        spend(BigDecimal("55.50"), today.atTime(10, 0), "Period A spend")
        finishEarly()

        applyPeriod(today, today.plusDays(4), BigDecimal("1500.00"))

        val screen = readScreen()
        assertWithMessage("Not filed under the past period. %s", screen.diagnostic)
            .that(screen.pastPeriod.map { it.comment }).contains("Period A spend")
    }

    @Test
    fun spend_made_after_the_new_period_starts_is_still_counted() {
        applyPeriod(today, today.plusDays(7), BigDecimal("1500.00"))
        spend(BigDecimal("55.50"), today.atTime(10, 0), "Period A spend")
        finishEarly()
        applyPeriod(today, today.plusDays(4), BigDecimal("1500.00"))

        spend(BigDecimal("20.00"), today.atTime(18, 0), "Period B spend")

        val screen = readScreen()
        assertWithMessage("The new period's own spend went missing. %s", screen.diagnostic)
            .that(screen.currentPeriod.map { it.comment }).contains("Period B spend")
        assertWithMessage("History still lists it. %s", screen.diagnostic)
            .that(screen.currentPeriod.map { it.comment }).doesNotContain("Period A spend")
        assertWithMessage("Spent is wrong. %s", screen.diagnostic)
            .that(screen.spentInPeriod).isEqualTo(BigDecimal("20.00"))
    }
}
