package com.serranoie.app.minus.presentation.ui.history

import android.content.Context
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.model.UserSettings
import com.serranoie.app.minus.domain.usecase.GetCurrentPeriodIdUseCase
import com.serranoie.app.minus.domain.usecase.ObserveCurrentPeriodBoundaryUseCase
import com.serranoie.app.minus.domain.usecase.PersistBudgetSettingsUseCase
import com.serranoie.app.minus.presentation.ui.budget.BudgetStateCalculator
import com.serranoie.app.minus.presentation.ui.budget.BudgetTransactionHandler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val budgetTransactionHandler: BudgetTransactionHandler = mockk()
    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val budgetStateCalculator: BudgetStateCalculator = mockk(relaxed = true)
    private val observeCurrentPeriodBoundaryUseCase: ObserveCurrentPeriodBoundaryUseCase = mockk()
    private val persistBudgetSettingsUseCase: PersistBudgetSettingsUseCase = mockk(relaxed = true)
    private val getCurrentPeriodIdUseCase: GetCurrentPeriodIdUseCase = mockk()
    private val context: Context = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { budgetTransactionHandler.budgetRepository } returns budgetRepository
        every { budgetRepository.getTransactions() } returns flowOf(emptyList())
        every { budgetRepository.getBudgetSettings() } returns flowOf(null)
        every { budgetRepository.getActiveCategories() } returns flowOf(emptyList())
        every { budgetRepository.getPaidRecurrentOccurrences() } returns flowOf(emptySet())
        every { observeCurrentPeriodBoundaryUseCase.invoke() } returns flowOf(0L to 0L)
        every { settingsRepository.observeSettings() } returns flowOf(UserSettings.DEFAULT)
        every { context.getString(any()) } returns "error"
        coEvery { getCurrentPeriodIdUseCase.invoke() } returns 1L
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = HistoryViewModel(
        budgetTransactionHandler = budgetTransactionHandler,
        settingsRepository = settingsRepository,
        budgetStateCalculator = budgetStateCalculator,
        observeCurrentPeriodBoundaryUseCase = observeCurrentPeriodBoundaryUseCase,
        persistBudgetSettingsUseCase = persistBudgetSettingsUseCase,
        getCurrentPeriodIdUseCase = getCurrentPeriodIdUseCase,
        context = context,
    )

    private fun txn(id: Long = 1L) = Transaction(
        id = id,
        amount = BigDecimal("10.00"),
        comment = "Coffee",
        date = LocalDateTime.of(2026, 1, 10, 9, 0),
    )

    private fun budgetSettings() = BudgetSettings(
        totalBudget = BigDecimal("1000.00"),
        period = BudgetPeriod.MONTHLY,
        startDate = LocalDate.of(2026, 1, 1),
    )

    private suspend fun <T> ReceiveTurbine<T>.awaitCondition(predicate: (T) -> Boolean): T {
        while (true) {
            val item = awaitItem()
            if (predicate(item)) return item
        }
    }

    @Test
    fun `setting a recurrent transaction to delete opens the confirmation dialog`() = runTest {
        val vm = newViewModel()
        val t = txn()
        vm.uiState.test {
            vm.processIntent(HistoryUiIntent.SetRecurrentToDelete(t))
            val state = awaitCondition { it.recurrentToDelete != null && it.showDeleteRecurrentDialog }
            assertThat(state.recurrentToDelete).isEqualTo(t)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismissing the delete-recurrent dialog clears its state`() = runTest {
        val vm = newViewModel()
        vm.uiState.test {
            vm.processIntent(HistoryUiIntent.SetRecurrentToDelete(txn()))
            awaitCondition { it.showDeleteRecurrentDialog }

            vm.processIntent(HistoryUiIntent.DismissDeleteRecurrentDialog)
            val state = awaitCondition { !it.showDeleteRecurrentDialog }
            assertThat(state.recurrentToDelete).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggling the past period is reflected in the state`() = runTest {
        val vm = newViewModel()
        vm.uiState.test {
            vm.processIntent(HistoryUiIntent.TogglePastPeriod(visible = true))
            assertThat(awaitCondition { it.showPastPeriod }.showPastPeriod).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `locking swipeables is reflected in the state`() = runTest {
        val vm = newViewModel()
        vm.uiState.test {
            vm.processIntent(HistoryUiIntent.SetLockSwipeable(locked = false))
            assertThat(awaitCondition { !it.lockSwipeable }.lockSwipeable).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `expanding a transaction toggles it on and off`() = runTest {
        val vm = newViewModel()
        vm.uiState.test {
            vm.processIntent(HistoryUiIntent.ToggleExpandedTransaction(5L))
            assertThat(awaitCondition { it.expandedTransactionId == 5L }.expandedTransactionId)
                .isEqualTo(5L)

            vm.processIntent(HistoryUiIntent.ToggleExpandedTransaction(5L))
            assertThat(awaitCondition { it.expandedTransactionId == null }.expandedTransactionId)
                .isNull()

            vm.processIntent(HistoryUiIntent.ToggleExpandedTransaction(6L))
            assertThat(awaitCondition { it.expandedTransactionId == 6L }.expandedTransactionId)
                .isEqualTo(6L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setting an editing transaction is reflected in the state`() = runTest {
        val vm = newViewModel()
        val t = txn()
        vm.uiState.test {
            vm.processIntent(HistoryUiIntent.SetEditingTransaction(t))
            assertThat(awaitCondition { it.editingTransaction != null }.editingTransaction)
                .isEqualTo(t)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleting a transaction stages it for removal immediately then clears it on success`() = runTest {
        val t = txn(1L)
        coEvery { budgetTransactionHandler.deleteTransaction(t) } returns Result.success(Unit)
        val vm = newViewModel()

        vm.uiState.test {
            vm.processIntent(HistoryUiIntent.DeleteTransaction(t))
            assertThat(awaitCondition { it.pendingRemovedTransactions.containsKey(1L) }).isNotNull()

            advanceTimeBy(700.milliseconds)

            awaitCondition { !it.pendingRemovedTransactions.containsKey(1L) }
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { budgetTransactionHandler.deleteTransaction(t) }
    }

    @Test
    fun `a failed delete surfaces a snackbar`() = runTest {
        val t = txn(2L)
        coEvery { budgetTransactionHandler.deleteTransaction(t) } returns
            Result.failure(RuntimeException("boom"))
        val vm = newViewModel()

        vm.effects.test {
            vm.processIntent(HistoryUiIntent.DeleteTransaction(t))
            advanceTimeBy(700.milliseconds)

            assertThat(awaitItem()).isEqualTo(HistoryUiEffect.ShowSnackbar("error"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a successful edit clears the editing transaction`() = runTest {
        val t = txn(3L)
        coEvery { budgetTransactionHandler.editTransaction(t) } returns true
        val vm = newViewModel()

        vm.uiState.test {
            vm.processIntent(HistoryUiIntent.SetEditingTransaction(t))
            awaitCondition { it.editingTransaction == t }

            vm.processIntent(HistoryUiIntent.SaveEditedTransaction(t))
            awaitCondition { it.editingTransaction == null }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failed edit surfaces a snackbar and keeps the editing transaction`() = runTest {
        val t = txn(4L)
        coEvery { budgetTransactionHandler.editTransaction(t) } returns false
        val vm = newViewModel()

        vm.effects.test {
            vm.processIntent(HistoryUiIntent.SaveEditedTransaction(t))
            assertThat(awaitItem()).isEqualTo(HistoryUiEffect.ShowSnackbar("error"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirming a recurrent delete closes the dialog and deletes the transaction`() = runTest {
        val t = txn(5L)
        coEvery { budgetTransactionHandler.deleteTransaction(t) } returns Result.success(Unit)
        val vm = newViewModel()

        vm.uiState.test {
            vm.processIntent(HistoryUiIntent.SetRecurrentToDelete(t))
            awaitCondition { it.showDeleteRecurrentDialog }

            vm.processIntent(HistoryUiIntent.ConfirmDeleteRecurrent(t))
            awaitCondition { !it.showDeleteRecurrentDialog && it.recurrentToDelete == null }
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { budgetTransactionHandler.deleteTransaction(t) }
    }

    @Test
    fun `a failed mark-as-paid surfaces a snackbar`() = runTest {
        val t = txn(6L)
        coEvery { budgetTransactionHandler.markRecurrentOccurrencePaid(t, any()) } returns
            Result.failure(RuntimeException("nope"))
        val vm = newViewModel()

        vm.effects.test {
            vm.processIntent(HistoryUiIntent.MarkTransactionAsPaid(t))
            assertThat(awaitItem()).isEqualTo(HistoryUiEffect.ShowSnackbar("error"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updating the credit cutoff day writes it onto the current budget settings`() = runTest {
        every { budgetRepository.getBudgetSettings() } returns flowOf(budgetSettings())
        every { budgetStateCalculator.filterPeriodTransactions(any(), any(), any(), any()) } returns emptyList()
        every { budgetStateCalculator.calculateBudgetState(any(), any(), any(), any()) } returns BudgetState.EMPTY
        val vm = newViewModel()

        vm.uiState.test {
            awaitCondition { it.budgetSettings != null }

            vm.processIntent(HistoryUiIntent.UpdateCreditCutoffDay(9))
            advanceUntilIdle()

            coVerify { persistBudgetSettingsUseCase.invoke(match { it.creditCardCutoffDay == 9 }, false) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an out-of-range credit cutoff day is ignored`() = runTest {
        every { budgetRepository.getBudgetSettings() } returns flowOf(budgetSettings())
        every { budgetStateCalculator.filterPeriodTransactions(any(), any(), any(), any()) } returns emptyList()
        every { budgetStateCalculator.calculateBudgetState(any(), any(), any(), any()) } returns BudgetState.EMPTY
        val vm = newViewModel()

        vm.uiState.test {
            awaitCondition { it.budgetSettings != null }

            vm.processIntent(HistoryUiIntent.UpdateCreditCutoffDay(0))
            advanceUntilIdle()

            coVerify(exactly = 0) { persistBudgetSettingsUseCase.invoke(any(), any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
