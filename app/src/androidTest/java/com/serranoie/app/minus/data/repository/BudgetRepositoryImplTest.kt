package com.serranoie.app.minus.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.local.AppDatabase
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.SavingsPocket
import com.serranoie.app.minus.domain.model.Transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Regression coverage for the entity<->domain mapping in [BudgetRepositoryImpl].
 * Uses a real (in-memory) Room database rather than a mock, because the bug this
 * guards against -- [BudgetSettingsEntity][com.serranoie.app.minus.data.local.entity.BudgetSettingsEntity]
 * silently missing a column that [BudgetSettings] exposes -- is invisible to any
 * test that mocks [BudgetRepository] itself. `rollOverLimit` had exactly this gap:
 * it round-tripped to null on every save/reload, which is why the "crossed out
 * previous total" rollover styling in `BudgetDisplay` rendered correctly right
 * after applying a rollover (from in-memory state) but disappeared as soon as any
 * screen re-read budget settings from disk (e.g. the History screen).
 */
@RunWith(AndroidJUnit4::class)
class BudgetRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: BudgetRepositoryImpl

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BudgetRepositoryImpl(
            appDatabase = database,
            transactionDao = database.transactionDao(),
            settingsDao = database.budgetSettingsDao(),
            archivedBudgetDao = database.archivedBudgetDao(),
            categoryDao = database.categoryDao(),
            queuedTransactionDao = database.queuedTransactionDao(),
            paidRecurrentOccurrenceDao = database.paidRecurrentOccurrenceDao(),
            savingsPocketDao = database.savingsPocketDao(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun rollOverLimitSurvivesASaveAndReloadThroughRoom() = runBlocking {
        val today = LocalDate.now()
        val settings = BudgetSettings(
            totalBudget = BigDecimal("1900.00"),
            period = BudgetPeriod.MONTHLY,
            startDate = today,
            endDate = today.plusDays(29),
            currencyCode = "USD",
            remainingBudgetStrategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
            rollOverLimit = BigDecimal("700.00"),
        )

        repository.saveBudgetSettings(settings)
        val reloaded = repository.getBudgetSettingsSync()

        assertThat(reloaded?.rollOverLimit).isEqualTo(BigDecimal("700.00"))
    }

    @Test
    fun nullRollOverLimitStaysNullThroughRoom() = runBlocking {
        val today = LocalDate.now()
        val settings = BudgetSettings(
            totalBudget = BigDecimal("1000.00"),
            period = BudgetPeriod.MONTHLY,
            startDate = today,
            endDate = today.plusDays(29),
            currencyCode = "USD",
        )

        repository.saveBudgetSettings(settings)
        val reloaded = repository.getBudgetSettingsSync()

        assertThat(reloaded?.rollOverLimit).isNull()
    }

    @Test
    fun deletingAnArchivedPeriodKeepsItsPocketRows() = runBlocking {
        val day = LocalDate.now().minusMonths(1).atTime(12, 0)
        repository.addTransaction(Transaction(amount = BigDecimal("100"), date = day, periodId = 10L))
        repository.addTransaction(
            Transaction(amount = BigDecimal("500"), date = day, periodId = 10L, isAdjustment = true, pocketId = 3L),
        )

        repository.deleteArchivedBudget(10L)
        val remaining = repository.getTransactions().first()

        assertThat(remaining.map { it.pocketId }).containsExactly(3L)
    }

    @Test
    fun aWithdrawalAboveThePocketBalanceIsRejected() = runBlocking {
        val day = LocalDate.now().atTime(12, 0)
        repository.saveSavingsPocket(SavingsPocket(id = 3L, name = "Trip"))
        repository.addTransaction(
            Transaction(amount = BigDecimal("400"), date = day, periodId = 1L, isAdjustment = true, pocketId = 3L),
        )
        val withdrawal = Transaction(amount = BigDecimal("-400.01"), date = day, periodId = 1L, isAdjustment = true, pocketId = 3L)

        assertThat(repository.addPocketWithdrawal(withdrawal)).isFalse()
        assertThat(repository.addPocketWithdrawal(withdrawal.copy(amount = BigDecimal("-400")))).isTrue()
        assertThat(repository.getTransactions().first()).hasSize(2)
    }

    @Test
    fun aPocketThatReceivedMoneyIsNotDeletedAndADeletedPocketRejectsDeposits() = runBlocking {
        val day = LocalDate.now().atTime(12, 0)
        val pocketId = repository.saveSavingsPocket(SavingsPocket(name = "Trip"))
        val deposit = Transaction(amount = BigDecimal("75"), date = day, periodId = 1L, isAdjustment = true, pocketId = pocketId)

        assertThat(repository.addPocketDeposit(deposit)).isTrue()
        assertThat(repository.deleteSavingsPocket(pocketId)).isFalse()
        assertThat(repository.addPocketWithdrawal(deposit.copy(amount = BigDecimal("-75")))).isTrue()
        assertThat(repository.deleteSavingsPocket(pocketId)).isTrue()
        assertThat(repository.addPocketDeposit(deposit)).isFalse()
        assertThat(repository.getTransactions().first()).hasSize(2)
    }
}
