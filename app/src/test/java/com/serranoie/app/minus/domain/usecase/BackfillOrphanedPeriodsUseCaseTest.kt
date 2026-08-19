package com.serranoie.app.minus.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.ArchivedBudget
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class BackfillOrphanedPeriodsUseCaseTest {

    @Test
    fun `windows walk backward in fixed-length steps aligned to the current period start`() {
        val currentStart = LocalDate.of(2026, 8, 14)
        val earliest = LocalDate.of(2026, 7, 20)

        val windows = computeBackfillWindows(
            currentPeriodStart = currentStart,
            windowDays = 7,
            earliestOrphanedDate = earliest,
        )

        assertThat(windows).containsExactly(
            BackfillWindow(LocalDate.of(2026, 8, 7), LocalDate.of(2026, 8, 13)),
            BackfillWindow(LocalDate.of(2026, 7, 31), LocalDate.of(2026, 8, 6)),
            BackfillWindow(LocalDate.of(2026, 7, 24), LocalDate.of(2026, 7, 30)),
            BackfillWindow(LocalDate.of(2026, 7, 17), LocalDate.of(2026, 7, 23)),
        ).inOrder()
    }

    @Test
    fun `windows stop as soon as the earliest orphaned date is covered, no extra trailing window`() {
        val windows = computeBackfillWindows(
            currentPeriodStart = LocalDate.of(2026, 8, 14),
            windowDays = 7,
            earliestOrphanedDate = LocalDate.of(2026, 8, 10),
        )

        assertThat(windows).containsExactly(
            BackfillWindow(LocalDate.of(2026, 8, 7), LocalDate.of(2026, 8, 13)),
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BackfillOrphanedPeriodsUseCaseOrchestrationTest {

    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)

    private val useCase = BackfillOrphanedPeriodsUseCase(budgetRepository, settingsRepository)

    private fun settings(startDate: LocalDate, period: BudgetPeriod = BudgetPeriod.WEEKLY) = BudgetSettings(
        totalBudget = BigDecimal("700.00"),
        period = period,
        startDate = startDate,
        currencyCode = "USD",
    )

    @Test
    fun `already backfilled is a no-op`() = runTest {
        coEvery { settingsRepository.getString(BackfillOrphanedPeriodsUseCase.BACKFILL_DONE_KEY) } returns "true"

        useCase()

        coVerify(exactly = 0) { budgetRepository.getTransactions() }
    }

    @Test
    fun `no settings yet defers without marking done`() = runTest {
        coEvery { settingsRepository.getString(any()) } returns null
        coEvery { budgetRepository.getBudgetSettingsSync() } returns null

        useCase()

        coVerify(exactly = 0) { settingsRepository.setString(BackfillOrphanedPeriodsUseCase.BACKFILL_DONE_KEY, "true") }
    }

    @Test
    fun `slices orphaned transactions into real weekly periods and reassigns their periodId`() = runTest {
        val currentStart = LocalDate.of(2026, 8, 14)
        coEvery { settingsRepository.getString(any()) } returns null
        coEvery { budgetRepository.getBudgetSettingsSync() } returns settings(currentStart)

        val coffee = Transaction.create(
            amount = BigDecimal("20.00"),
            comment = "Coffee",
            date = LocalDate.of(2026, 8, 10).atStartOfDay(),
        ).copy(id = 1L)
        val netflix = Transaction.create(
            amount = BigDecimal("100.00"),
            comment = "Netflix",
            date = LocalDate.of(2026, 7, 12).atTime(10, 0),
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
        ).copy(id = 2L)

        coEvery { budgetRepository.getTransactions() } returns flowOf(listOf(coffee, netflix))
        coEvery { budgetRepository.getArchivedBudgets() } returns flowOf(emptyList())
        coEvery { budgetRepository.getPaidRecurrentOccurrences() } returns
            flowOf(emptySet<PaidRecurrentOccurrence>())

        val archivedSlot = mutableListOf<List<ArchivedBudget>>()
        coEvery { budgetRepository.upsertArchivedBudgets(any()) } answers { archivedSlot.add(firstArg()) }
        val transactionsSlot = mutableListOf<List<Transaction>>()
        coEvery { budgetRepository.upsertTransactions(any()) } answers { transactionsSlot.add(firstArg()) }

        useCase()

        val weekArchive = archivedSlot.single().single {
            it.startDate == LocalDate.of(2026, 8, 7) && it.endDate == LocalDate.of(2026, 8, 13)
        }
        assertThat(weekArchive.spentAmount).isEqualTo(BigDecimal("120.00"))
        assertThat(weekArchive.periodId).isGreaterThan(0L)

        val updatedCoffee = transactionsSlot.flatten().single { it.id == 1L }
        assertThat(updatedCoffee.periodId).isEqualTo(weekArchive.periodId)

        coVerify { settingsRepository.setString(BackfillOrphanedPeriodsUseCase.BACKFILL_DONE_KEY, "true") }
    }

    @Test
    fun `never touches a window that overlaps an already-real archived period`() = runTest {
        val currentStart = LocalDate.of(2026, 8, 14)
        coEvery { settingsRepository.getString(any()) } returns null
        coEvery { budgetRepository.getBudgetSettingsSync() } returns settings(currentStart)

        val orphaned = Transaction.create(
            amount = BigDecimal("50.00"),
            comment = "Groceries",
            date = LocalDate.of(2026, 8, 10).atStartOfDay(),
        ).copy(id = 1L)

        coEvery { budgetRepository.getTransactions() } returns flowOf(listOf(orphaned))
        coEvery { budgetRepository.getArchivedBudgets() } returns flowOf(
            listOf(
                ArchivedBudget(
                    periodId = 999L,
                    totalBudget = BigDecimal("700.00"),
                    spentAmount = BigDecimal("300.00"),
                    startDate = LocalDate.of(2026, 8, 7),
                    endDate = LocalDate.of(2026, 8, 13),
                    currencyCode = "USD",
                    periodType = BudgetPeriod.WEEKLY,
                )
            )
        )
        coEvery { budgetRepository.getPaidRecurrentOccurrences() } returns
            flowOf(emptySet<PaidRecurrentOccurrence>())

        useCase()

        coVerify(exactly = 0) { budgetRepository.upsertArchivedBudgets(any()) }
        coVerify(exactly = 0) { budgetRepository.upsertTransactions(any()) }
        coVerify { settingsRepository.setString(BackfillOrphanedPeriodsUseCase.BACKFILL_DONE_KEY, "true") }
    }
}
