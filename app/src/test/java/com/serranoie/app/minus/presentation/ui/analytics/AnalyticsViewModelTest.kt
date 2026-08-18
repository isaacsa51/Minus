package com.serranoie.app.minus.presentation.ui.analytics

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.ArchivedBudget
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.model.UserSettings
import com.serranoie.app.minus.domain.usecase.ClearEarlyFinishStateUseCase
import com.serranoie.app.minus.domain.usecase.ObserveCurrentPeriodBoundaryUseCase
import com.serranoie.app.minus.domain.usecase.PersistBudgetSettingsUseCase
import com.serranoie.app.minus.presentation.ui.budget.BudgetStateCalculator
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

    private val settingsFlow = MutableStateFlow<BudgetSettings?>(null)
    private val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())
    private val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    private val allCategoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    private val archivedFlow = MutableStateFlow<List<ArchivedBudget>>(emptyList())
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
        persistBudgetSettingsUseCase
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
