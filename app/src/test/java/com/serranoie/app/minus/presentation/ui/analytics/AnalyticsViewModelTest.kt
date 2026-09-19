package com.serranoie.app.minus.presentation.ui.analytics

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.ArchivedBudget
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.RecurrentOccurrenceStatus
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.model.UserSettings
import com.serranoie.app.minus.domain.usecase.ClearEarlyFinishStateUseCase
import com.serranoie.app.minus.domain.usecase.ObserveCurrentPeriodBoundaryUseCase
import com.serranoie.app.minus.domain.usecase.PersistBudgetSettingsUseCase
import com.serranoie.app.minus.presentation.ui.budget.BudgetStateCalculator
import com.serranoie.app.minus.presentation.util.ErrorLogRecorder
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val budgetStateCalculator: BudgetStateCalculator = mockk(relaxed = true)
    private val observeCurrentPeriodBoundaryUseCase: ObserveCurrentPeriodBoundaryUseCase = mockk()
    private val clearEarlyFinishStateUseCase: ClearEarlyFinishStateUseCase = mockk(relaxed = true)
    private val persistBudgetSettingsUseCase: PersistBudgetSettingsUseCase = mockk(relaxed = true)
    private val errorLogRecorder: ErrorLogRecorder = mockk(relaxed = true)

    private val settingsFlow = MutableStateFlow<BudgetSettings?>(null)
    private val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())
    private val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    private val allCategoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    private val archivedFlow = MutableStateFlow<List<ArchivedBudget>>(emptyList())
    private val paidOccurrencesFlow = MutableStateFlow<Set<PaidRecurrentOccurrence>>(emptySet())
    private val boundaryFlow = flowOf(0L to 1L)
    private val userSettingsFlow = flowOf(UserSettings())
    private val rolloverFlow = flowOf(BigDecimal.ZERO to false)

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        every { budgetRepository.getBudgetSettings() } returns settingsFlow
        every { budgetRepository.getTransactions() } returns transactionsFlow
        every { budgetRepository.getActiveCategories() } returns categoriesFlow
        every { budgetRepository.getAllCategories() } returns allCategoriesFlow
        every { budgetRepository.getArchivedBudgets() } returns archivedFlow
        every { budgetRepository.getPaidRecurrentOccurrences() } returns paidOccurrencesFlow
        every { observeCurrentPeriodBoundaryUseCase() } returns boundaryFlow
        every { settingsRepository.observeSettings() } returns userSettingsFlow
        every { settingsRepository.observeCurrentPeriodRollover() } returns rolloverFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = AnalyticsViewModel(
        budgetRepository,
        settingsRepository,
        budgetStateCalculator,
        observeCurrentPeriodBoundaryUseCase,
        clearEarlyFinishStateUseCase,
        persistBudgetSettingsUseCase,
        errorLogRecorder
    )

    @Test
    fun `initial state is loading then success when data emits`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            assertThat(awaitItem().isLoading).isTrue()
            assertThat(awaitItem().isLoading).isFalse()
        }
    }

    @Test
    fun `selecting historical period updates display state`() = runTest {
        val archive = ArchivedBudget(
            periodId = 10L,
            totalBudget = BigDecimal("1000.00"),
            spentAmount = BigDecimal("500.00"),
            startDate = LocalDate.now().minusMonths(1),
            endDate = LocalDate.now().minusDays(1),
            currencyCode = "USD",
            periodType = BudgetPeriod.MONTHLY
        )
        archivedFlow.value = listOf(archive)
        
        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(2)

            viewModel.onPeriodSelected(10L)
            
            val state = awaitItem()
            assertThat(state.selectedPeriodId).isEqualTo(10L)
            assertThat(state.displayState.isHistoricalView).isTrue()
            assertThat(state.displayState.wholeBudget).isEqualTo(BigDecimal("1000.00"))
        }
    }

    @Test
    fun `closing historical view returns to current`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(2)

            viewModel.onPeriodSelected(10L)
            awaitItem()

            viewModel.onClose()
            val state = awaitItem()
            assertThat(state.selectedPeriodId).isNull()
            assertThat(state.displayState.isHistoricalView).isFalse()
        }
    }

    @Test
    fun `onClose without selected period triggers NavigateToMain effect`() = runTest {
        val viewModel = createViewModel()
        
        viewModel.effects.test {
            assertThat(awaitItem()).isNull()
            
            viewModel.onClose()
            assertThat(awaitItem()).isEqualTo(AnalyticsUiEffect.NavigateToMain)
        }
    }

    @Test
    fun `displayState categories includes hidden categories, unlike active-only list`() = runTest {
        val hiddenGroceries = Category(id = 5L, name = "Groceries", isHidden = true)
        categoriesFlow.value = emptyList()
        allCategoriesFlow.value = listOf(hiddenGroceries)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(1)

            val state = awaitItem()
            assertThat(state.categories).isEmpty()
            assertThat(state.displayState.categories).containsExactly(hiddenGroceries)
        }
    }

    @Test
    fun `paid recurring occurrence is not double-counted in historical period spend total`() = runTest {
        val periodStart = LocalDate.now().minusMonths(2)
        val periodEnd = LocalDate.now().minusMonths(1).minusDays(1)
        val recurrentDate = periodStart.plusDays(5)

        val archive = ArchivedBudget(
            periodId = 10L,
            totalBudget = BigDecimal("1000.00"),
            spentAmount = BigDecimal("100.00"),
            startDate = periodStart,
            endDate = periodEnd,
            currencyCode = "USD",
            periodType = BudgetPeriod.MONTHLY
        )
        archivedFlow.value = listOf(archive)

        val recurringParent = Transaction(
            id = 1L,
            amount = BigDecimal("100.00"),
            comment = "Netflix",
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
            date = recurrentDate.atTime(10, 0),
            periodId = 10L,
        )
        val paidOccurrenceTransaction = Transaction(
            id = 2L,
            amount = BigDecimal("100.00"),
            comment = "Netflix",
            isRecurrent = false,
            date = recurrentDate.atTime(10, 0),
            periodId = 10L,
        )

        transactionsFlow.value = listOf(recurringParent, paidOccurrenceTransaction)
        paidOccurrencesFlow.value = setOf(PaidRecurrentOccurrence(1L, recurrentDate))

        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(2)

            viewModel.onPeriodSelected(10L)

            val state = awaitItem()
            assertThat(state.displayState.spends.sumOf { it.amount }).isEqualTo(BigDecimal("100.00"))
        }
    }

    @Test
    fun `reconstructed virtual period total matches what opening it recomputes`() = runTest {
        val baseAnchor = LocalDate.now().withDayOfMonth(10)
        val netflixAnchor = baseAnchor.minusMonths(2)
        val monthBDate = baseAnchor.minusMonths(1)

        settingsFlow.value = BudgetSettings(
            totalBudget = BigDecimal("1000"),
            period = BudgetPeriod.MONTHLY,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(20),
            currencyCode = "USD",
        )

        val netflix = Transaction(
            id = 1L,
            amount = BigDecimal("100.00"),
            comment = "Netflix",
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
            date = netflixAnchor.atTime(10, 0),
        )
        val coffee = Transaction(
            id = 2L,
            amount = BigDecimal("20.00"),
            comment = "Coffee",
            date = monthBDate.atTime(9, 0),
        )

        transactionsFlow.value = listOf(netflix, coffee)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(1)
            val state = awaitItem()

            val monthBPeriodId = -(monthBDate.year.toLong() * 100 + monthBDate.monthValue.toLong())
            val monthBArchive = state.archivedBudgets.first { it.periodId == monthBPeriodId }
            assertThat(monthBArchive.spentAmount).isEqualTo(BigDecimal("120.00"))

            viewModel.onPeriodSelected(monthBPeriodId)
            val openedState = awaitItem()
            assertThat(openedState.displayState.spends.sumOf { it.amount }).isEqualTo(BigDecimal("120.00"))
        }
    }

    @Test
    fun `archived period total in the list is recomputed from live rows, not the stored snapshot`() = runTest {
        val periodStart = LocalDate.now().minusMonths(2)
        val periodEnd = LocalDate.now().minusMonths(1).minusDays(1)
        archivedFlow.value = listOf(
            ArchivedBudget(
                periodId = 10L,
                totalBudget = BigDecimal("1000.00"),
                spentAmount = BigDecimal("850.00"),
                startDate = periodStart,
                endDate = periodEnd,
                currencyCode = "USD",
                periodType = BudgetPeriod.MONTHLY
            )
        )
        transactionsFlow.value = listOf(
            Transaction(id = 1L, amount = BigDecimal("20.00"), date = periodStart.plusDays(1).atTime(9, 0), periodId = 10L),
            Transaction(id = 2L, amount = BigDecimal("30.00"), date = periodStart.plusDays(2).atTime(9, 0), periodId = 10L),
            Transaction(id = 3L, amount = BigDecimal("999.00"), date = periodStart.plusDays(3).atTime(9, 0), periodId = 11L),
        )

        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(1)
            val listed = awaitItem().archivedBudgets.single { it.periodId == 10L }
            assertThat(listed.spentAmount).isEqualTo(BigDecimal("50.00"))

            transactionsFlow.value = transactionsFlow.value.filter { it.id != 2L }
            assertThat(awaitItem().archivedBudgets.single { it.periodId == 10L }.spentAmount)
                .isEqualTo(BigDecimal("20.00"))
        }
    }

    @Test
    fun `deleting a projected recurring charge skips that occurrence instead of deleting the subscription`() = runTest {
        val occurrenceDate = LocalDate.now().minusMonths(1)
        val projectedCharge = Transaction(
            id = 7L * 1_000_000L + occurrenceDate.toEpochDay(),
            amount = BigDecimal("100.00"),
            comment = "Netflix",
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
            date = occurrenceDate.atTime(10, 0),
            sourceTransactionId = 7L,
        )

        val viewModel = createViewModel()
        viewModel.deleteTransaction(projectedCharge)
        runCurrent()

        coVerify { budgetRepository.markRecurrentOccurrencePaid(7L, occurrenceDate, RecurrentOccurrenceStatus.SKIPPED) }
        coVerify(exactly = 0) { budgetRepository.deleteTransaction(any()) }
    }

    @Test
    fun `deleting a one-time expense in a past period removes the row`() = runTest {
        val expense = Transaction(id = 3L, amount = BigDecimal("20.00"), date = LocalDate.now().minusMonths(1).atTime(9, 0), periodId = 10L)

        val viewModel = createViewModel()
        viewModel.deleteTransaction(expense)
        runCurrent()

        coVerify { budgetRepository.deleteTransaction(expense) }
        coVerify(exactly = 0) { budgetRepository.markRecurrentOccurrencePaid(any(), any(), any()) }
    }

    @Test
    fun `onCreateNewPeriod clears early finish and navigates`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            assertThat(awaitItem()).isNull()

            viewModel.onCreateNewPeriod()
            runCurrent()
            
            assertThat(awaitItem()).isEqualTo(AnalyticsUiEffect.NavigateToMainWithWallet)
        }
    }
}
