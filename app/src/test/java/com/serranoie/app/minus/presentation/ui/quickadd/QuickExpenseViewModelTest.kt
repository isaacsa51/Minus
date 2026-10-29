package com.serranoie.app.minus.presentation.ui.quickadd

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.domain.usecase.GetCurrentPeriodIdUseCase
import com.serranoie.app.minus.presentation.ui.budget.ApplyTransactionResult
import com.serranoie.app.minus.presentation.ui.budget.BudgetTransactionHandler
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
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class QuickExpenseViewModelTest {

    private val settings = BudgetSettings(
        totalBudget = BigDecimal("3000"),
        period = BudgetPeriod.MONTHLY,
        startDate = LocalDate.of(2099, 6, 1),
        currencyCode = "EUR",
    )
    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val transactionHandler: BudgetTransactionHandler = mockk()
    private val getCurrentPeriodId: GetCurrentPeriodIdUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { budgetRepository.getBudgetSettingsSync() } returns settings
        every { budgetRepository.getActiveCategories() } returns flowOf(
            listOf(
                Category(name = "Rent", usageCount = 9, lastUsedAt = 100),
                Category(name = "Coffee", usageCount = 2, lastUsedAt = 500),
            ),
        )
        coEvery { getCurrentPeriodId() } returns 7L
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = QuickExpenseViewModel(budgetRepository, transactionHandler, getCurrentPeriodId)

    private fun stubResult(result: ApplyTransactionResult) {
        coEvery {
            transactionHandler.applyTransaction(
                input = any(),
                isCalculation = any(),
                isRecurrentEnabled = any(),
                isCreditEnabled = any(),
                comment = any(),
                note = any(),
                budgetSettings = any(),
                resolveActivePeriodId = any(),
            )
        } returns result
    }

    @Test
    fun `loads the quick add categories and the budget currency`() = runTest {
        viewModel().uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.needsBudget).isFalse()
            assertThat(state.categories).containsExactly("Coffee", "Rent").inOrder()
            assertThat(state.currency?.code).isEqualTo("EUR")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `without a budget the sheet asks to open the app`() = runTest {
        coEvery { budgetRepository.getBudgetSettingsSync() } returns null

        viewModel().uiState.test {
            assertThat(expectMostRecentItem().needsBudget).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `amount is normalized and a zero amount cannot be saved`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            vm.processIntent(QuickExpenseUiIntent.AmountChanged("12,5"))
            assertThat(expectMostRecentItem().amount).isEqualTo("12.5")
            assertThat(vm.uiState.value.canSave).isTrue()

            vm.processIntent(QuickExpenseUiIntent.AmountChanged("0"))
            assertThat(expectMostRecentItem().canSave).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tapping the selected category again clears it`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            vm.processIntent(QuickExpenseUiIntent.CategoryToggled("Coffee"))
            assertThat(expectMostRecentItem().selectedCategory).isEqualTo("Coffee")

            vm.processIntent(QuickExpenseUiIntent.CategoryToggled("Coffee"))
            assertThat(expectMostRecentItem().selectedCategory).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `save writes the amount with the chosen category and reports Saved`() = runTest {
        stubResult(ApplyTransactionResult.Added(normalizedInput = "12.5", transactionId = 3L))
        val vm = viewModel()
        vm.uiState.test {
            vm.processIntent(QuickExpenseUiIntent.AmountChanged("12.5"))
            vm.processIntent(QuickExpenseUiIntent.CategoryToggled("Coffee"))
            expectMostRecentItem()

            vm.effects.test {
                vm.processIntent(QuickExpenseUiIntent.Save)
                assertThat(awaitItem()).isEqualTo(QuickExpenseUiEffect.Saved(queuedForNextPeriod = false))
            }
            cancelAndIgnoreRemainingEvents()
        }
        coVerify {
            transactionHandler.applyTransaction(
                input = "12.5",
                isCalculation = false,
                isRecurrentEnabled = false,
                isCreditEnabled = false,
                comment = "Coffee",
                note = "",
                budgetSettings = settings,
                resolveActivePeriodId = any(),
            )
        }
    }

    @Test
    fun `a save after the period ended reports it was queued`() = runTest {
        stubResult(ApplyTransactionResult.QueuedForNextPeriod(normalizedInput = "4"))
        val vm = viewModel()
        vm.uiState.test {
            vm.processIntent(QuickExpenseUiIntent.AmountChanged("4"))
            expectMostRecentItem()

            vm.effects.test {
                vm.processIntent(QuickExpenseUiIntent.Save)
                assertThat(awaitItem()).isEqualTo(QuickExpenseUiEffect.Saved(queuedForNextPeriod = true))
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failed save keeps the input and shows SaveFailed`() = runTest {
        stubResult(ApplyTransactionResult.Failed(IOException("disk")))
        val vm = viewModel()
        vm.uiState.test {
            vm.processIntent(QuickExpenseUiIntent.AmountChanged("9"))
            vm.processIntent(QuickExpenseUiIntent.CategoryToggled("Rent"))
            vm.processIntent(QuickExpenseUiIntent.Save)

            val state = expectMostRecentItem()
            assertThat(state.error).isEqualTo(QuickExpenseUiState.Error.SaveFailed)
            assertThat(state.isSaving).isFalse()
            assertThat(state.amount).isEqualTo("9")
            assertThat(state.selectedCategory).isEqualTo("Rent")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a categories stream failure still lets the user save without a category`() = runTest {
        every { budgetRepository.getActiveCategories() } returns kotlinx.coroutines.flow.flow {
            throw IOException("db")
        }

        viewModel().uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.categories).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
