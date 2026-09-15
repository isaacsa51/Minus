package com.serranoie.app.minus.presentation.ui.subscriptions

import android.content.Context
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.calculator.RecurringExpenseCalculator
import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.RecurrentOccurrenceStatus
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.usecase.GetCurrentPeriodIdUseCase
import com.serranoie.app.minus.presentation.ui.budget.BudgetTransactionHandler
import com.serranoie.app.minus.presentation.util.ErrorLogRecorder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionsViewModelTest {

    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val budgetTransactionHandler: BudgetTransactionHandler = mockk()
    private val getCurrentPeriodIdUseCase: GetCurrentPeriodIdUseCase = mockk()
    // Real instance: it's pure frequency math, already covered by RecurringExpenseCalculatorTest,
    // and using it here (rather than stubbing every method) lets the due-today/window logic
    // exercise genuine calculator behavior instead of drifting from it.
    private val recurringExpenseCalculator = RecurringExpenseCalculator()
    private val errorLogRecorder: ErrorLogRecorder = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { budgetRepository.getTransactions() } returns flowOf(emptyList())
        every { budgetRepository.getPaidRecurrentOccurrences() } returns flowOf(emptySet())
        every { budgetRepository.getBudgetSettings() } returns flowOf(null)
        every { context.getString(any()) } returns "error"
        coEvery { getCurrentPeriodIdUseCase.invoke() } returns 1L
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = SubscriptionsViewModel(
        budgetRepository = budgetRepository,
        budgetTransactionHandler = budgetTransactionHandler,
        getCurrentPeriodIdUseCase = getCurrentPeriodIdUseCase,
        recurringExpenseCalculator = recurringExpenseCalculator,
        errorLogRecorder = errorLogRecorder,
        context = context,
    )

    private fun recurrentTransaction(
        id: Long = 1L,
        startDate: LocalDate,
        frequency: RecurrentFrequency,
        recurrentEndDate: LocalDate? = null,
        subscriptionDay: Int? = null,
        amount: BigDecimal = BigDecimal("10.00"),
    ) = Transaction(
        id = id,
        amount = amount,
        comment = "Subscription $id",
        date = startDate.atStartOfDay(),
        isRecurrent = true,
        recurrentFrequency = frequency,
        recurrentEndDate = recurrentEndDate?.atStartOfDay(),
        subscriptionDay = subscriptionDay,
    )

    private suspend fun <T> ReceiveTurbine<T>.awaitCondition(predicate: (T) -> Boolean): T {
        while (true) {
            val item = awaitItem()
            if (predicate(item)) return item
        }
    }

    @Test
    fun `no recurring transactions produces an empty state`() = runTest {
        val vm = newViewModel()

        vm.uiState.test {
            val state = awaitCondition { !it.isLoading }
            assertThat(state.dueSoon).isEmpty()
            assertThat(state.upcoming).isEmpty()
            assertThat(state.activeCount).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a next charge within the due-soon window appears in dueSoon`() = runTest {
        val today = LocalDate.now()
        // Weekly, started 4 days ago: next charge lands 3 days from now.
        val t = recurrentTransaction(startDate = today.minusDays(4), frequency = RecurrentFrequency.WEEKLY)
        every { budgetRepository.getTransactions() } returns flowOf(listOf(t))

        val vm = newViewModel()
        vm.uiState.test {
            val state = awaitCondition { !it.isLoading }
            assertThat(state.dueSoon.map { it.transaction.id }).containsExactly(t.id)
            assertThat(state.upcoming).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a next charge beyond the due-soon window appears in upcoming`() = runTest {
        val today = LocalDate.now()
        // Biweekly, started 4 days ago: next charge lands 10 days from now.
        val t = recurrentTransaction(startDate = today.minusDays(4), frequency = RecurrentFrequency.BIWEEKLY)
        every { budgetRepository.getTransactions() } returns flowOf(listOf(t))

        val vm = newViewModel()
        vm.uiState.test {
            val state = awaitCondition { !it.isLoading }
            assertThat(state.upcoming.map { it.transaction.id }).containsExactly(t.id)
            assertThat(state.dueSoon).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a subscription due exactly today is surfaced in dueSoon with a zero-day next charge`() = runTest {
        val today = LocalDate.now()
        val t = recurrentTransaction(
            startDate = today.minusMonths(3),
            frequency = RecurrentFrequency.MONTHLY,
            subscriptionDay = today.dayOfMonth,
        )
        every { budgetRepository.getTransactions() } returns flowOf(listOf(t))

        val vm = newViewModel()
        vm.uiState.test {
            val state = awaitCondition { !it.isLoading }
            assertThat(state.dueSoon.map { it.transaction.id }).containsExactly(t.id)
            assertThat(state.dueSoon.single().nextChargeDate).isEqualTo(today)
            assertThat(state.daysUntilNextCharge).isEqualTo(0L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a subscription already marked paid for today is not surfaced again`() = runTest {
        val today = LocalDate.now()
        val t = recurrentTransaction(
            id = 9L,
            startDate = today.minusMonths(3),
            frequency = RecurrentFrequency.MONTHLY,
            subscriptionDay = today.dayOfMonth,
        )
        every { budgetRepository.getTransactions() } returns flowOf(listOf(t))
        every { budgetRepository.getPaidRecurrentOccurrences() } returns
            flowOf(setOf(PaidRecurrentOccurrence(9L, today)))

        val vm = newViewModel()
        vm.uiState.test {
            val state = awaitCondition { !it.isLoading }
            assertThat(state.dueSoon).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a subscription past its end date is excluded from the active count and total`() = runTest {
        val today = LocalDate.now()
        val active = recurrentTransaction(id = 1L, startDate = today.minusDays(4), frequency = RecurrentFrequency.WEEKLY)
        val expired = recurrentTransaction(
            id = 2L,
            startDate = today.minusMonths(2),
            frequency = RecurrentFrequency.WEEKLY,
            recurrentEndDate = today.minusDays(1),
        )
        every { budgetRepository.getTransactions() } returns flowOf(listOf(active, expired))

        val vm = newViewModel()
        vm.uiState.test {
            val state = awaitCondition { !it.isLoading }
            assertThat(state.activeCount).isEqualTo(1)
            // Weekly $10.00 normalized to a monthly equivalent (*4.33); the expired
            // transaction must not contribute to this total.
            assertThat(state.monthlyTotal).isEqualTo(BigDecimal("43.30"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirming paid delegates to the budget transaction handler with the occurrence date`() = runTest {
        val t = recurrentTransaction(startDate = LocalDate.now().minusMonths(1), frequency = RecurrentFrequency.MONTHLY)
        val occurrenceDate = LocalDate.now().plusDays(3)
        coEvery { budgetTransactionHandler.markRecurrentOccurrencePaid(any(), any()) } returns Result.success(Unit)

        val vm = newViewModel()
        vm.onConfirmPaid(t, occurrenceDate)

        coVerify {
            budgetTransactionHandler.markRecurrentOccurrencePaid(
                match { it.date?.toLocalDate() == occurrenceDate && it.sourceTransactionId == t.id },
                1L,
            )
        }
    }

    @Test
    fun `a failed confirm-paid call surfaces a snackbar effect`() = runTest {
        val t = recurrentTransaction(startDate = LocalDate.now().minusMonths(1), frequency = RecurrentFrequency.MONTHLY)
        coEvery { budgetTransactionHandler.markRecurrentOccurrencePaid(any(), any()) } returns
            Result.failure(RuntimeException("nope"))

        val vm = newViewModel()
        vm.effects.test {
            assertThat(awaitItem()).isNull()
            vm.onConfirmPaid(t, LocalDate.now().plusDays(3))
            assertThat(awaitItem()).isEqualTo(SubscriptionsUiEffect.ShowSnackbar("error"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `skipping a cycle calls the repository directly without going through the transaction handler`() = runTest {
        val t = recurrentTransaction(startDate = LocalDate.now().minusMonths(1), frequency = RecurrentFrequency.MONTHLY)
        val occurrenceDate = LocalDate.now().plusDays(3)

        val vm = newViewModel()
        vm.onSkip(t, occurrenceDate)

        coVerify { budgetRepository.markRecurrentOccurrencePaid(t.id, occurrenceDate, RecurrentOccurrenceStatus.SKIPPED) }
        coVerify(exactly = 0) { budgetTransactionHandler.markRecurrentOccurrencePaid(any(), any()) }
    }

    @Test
    fun `a failed skip call surfaces a snackbar effect`() = runTest {
        val t = recurrentTransaction(startDate = LocalDate.now().minusMonths(1), frequency = RecurrentFrequency.MONTHLY)
        coEvery { budgetRepository.markRecurrentOccurrencePaid(any(), any(), any()) } throws RuntimeException("nope")

        val vm = newViewModel()
        vm.effects.test {
            assertThat(awaitItem()).isNull()
            vm.onSkip(t, LocalDate.now().plusDays(3))
            assertThat(awaitItem()).isEqualTo(SubscriptionsUiEffect.ShowSnackbar("error"))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
