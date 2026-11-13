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
import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.domain.model.RecurrentFrequency
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
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
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
        every { budgetRepository.getAllCategories() } returns flowOf(emptyList())
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
    fun `deleting several transactions in quick succession deletes all of them, not just the last`() = runTest {
        val t1 = txn(1L)
        val t2 = txn(2L)
        val t3 = txn(3L)
        coEvery { budgetTransactionHandler.deleteTransaction(any()) } returns Result.success(Unit)
        val vm = newViewModel()

        vm.uiState.test {
            vm.processIntent(HistoryUiIntent.DeleteTransaction(t1))
            advanceTimeBy(200.milliseconds)
            vm.processIntent(HistoryUiIntent.DeleteTransaction(t2))
            advanceTimeBy(200.milliseconds)
            vm.processIntent(HistoryUiIntent.DeleteTransaction(t3))

            advanceTimeBy(700.milliseconds)
            awaitCondition { it.pendingRemovedTransactions.isEmpty() }
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { budgetTransactionHandler.deleteTransaction(t1) }
        coVerify { budgetTransactionHandler.deleteTransaction(t2) }
        coVerify { budgetTransactionHandler.deleteTransaction(t3) }
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
        coEvery { budgetTransactionHandler.editTransaction(t) } returns Result.success(Unit)
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
        coEvery { budgetTransactionHandler.editTransaction(t) } returns Result.failure(RuntimeException("boom"))
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

    private fun searchFixture() = listOf(
        Transaction(id = 1L, amount = BigDecimal("4.00"), comment = "Coffee", date = LocalDateTime.of(2026, 1, 10, 9, 0)),
        Transaction(
            id = 2L,
            amount = BigDecimal("12.00"),
            comment = "Lunch",
            note = "coffee with Ana",
            date = LocalDateTime.of(2026, 1, 12, 13, 0),
        ),
        Transaction(id = 3L, amount = BigDecimal("40.00"), comment = "Groceries", date = LocalDateTime.of(2026, 1, 12, 18, 0)),
        Transaction(id = 4L, amount = BigDecimal("9.00"), comment = "Coffee beans", date = LocalDateTime.of(2025, 12, 20, 10, 0)),
    )

    @Test
    fun `search matches description and note ignoring case across current and past periods`() = runTest {
        every { budgetRepository.getBudgetSettings() } returns flowOf(budgetSettings())
        every { settingsRepository.observeSettings() } returns flowOf(UserSettings.DEFAULT.copy(showPastTransactions = true))
        every { budgetRepository.getTransactions() } returns flowOf(searchFixture())
        val vm = newViewModel()

        vm.uiState.test {
            vm.processIntent(HistoryUiIntent.SetSearchQuery("COFFEE"))
            val state = awaitCondition { it.searchQuery == "COFFEE" && it.budgetSettings != null }

            assertThat(state.groupedCurrentTransactions.values.flatten().map { it.id })
                .containsExactly(1L, 2L, 4L)
            assertThat(state.groupedPastTransactions).isEmpty()
            assertThat(state.expandedDates).containsExactly(
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 12),
                LocalDate.of(2025, 12, 20),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search leaves out past periods when they are hidden`() = runTest {
        every { budgetRepository.getBudgetSettings() } returns flowOf(budgetSettings())
        every { settingsRepository.observeSettings() } returns flowOf(UserSettings.DEFAULT.copy(showPastTransactions = false))
        every { budgetRepository.getTransactions() } returns flowOf(searchFixture())
        val vm = newViewModel()

        vm.uiState.test {
            vm.processIntent(HistoryUiIntent.SetSearchQuery("coffee"))
            val state = awaitCondition { it.searchQuery == "coffee" && it.budgetSettings != null }

            assertThat(state.groupedCurrentTransactions.values.flatten().map { it.id }).containsExactly(1L, 2L)
            assertThat(state.searchResultCount).isEqualTo(2)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search matches the category name of an expense, even a hidden category`() = runTest {
        every { budgetRepository.getBudgetSettings() } returns flowOf(budgetSettings())
        every { budgetRepository.getAllCategories() } returns flowOf(listOf(Category(id = 9L, name = "Dining")))
        every { budgetRepository.getTransactions() } returns flowOf(
            searchFixture().map { if (it.id == 2L) it.copy(categoryId = 9L) else it }
        )
        val vm = newViewModel()

        vm.uiState.test {
            vm.processIntent(HistoryUiIntent.SetSearchQuery("dining"))
            val state = awaitCondition { it.searchQuery == "dining" && it.budgetSettings != null }

            assertThat(state.groupedCurrentTransactions.values.flatten().map { it.id }).containsExactly(2L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a search with no matches leaves the result list empty`() = runTest {
        every { budgetRepository.getBudgetSettings() } returns flowOf(budgetSettings())
        every { budgetRepository.getTransactions() } returns flowOf(searchFixture())
        val vm = newViewModel()

        vm.uiState.test {
            vm.processIntent(HistoryUiIntent.SetSearchQuery("rent"))
            val state = awaitCondition { it.searchQuery == "rent" && it.budgetSettings != null }

            assertThat(state.groupedCurrentTransactions).isEmpty()
            assertThat(state.groupedPastTransactions).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearing the search restores the current and past groups`() = runTest {
        every { budgetRepository.getBudgetSettings() } returns flowOf(budgetSettings())
        every { settingsRepository.observeSettings() } returns flowOf(UserSettings.DEFAULT.copy(showPastTransactions = true))
        every { budgetRepository.getTransactions() } returns flowOf(searchFixture())
        val vm = newViewModel()

        vm.uiState.test {
            vm.processIntent(HistoryUiIntent.SetSearchQuery("rent"))
            awaitCondition { it.searchQuery == "rent" && it.budgetSettings != null }

            vm.processIntent(HistoryUiIntent.SetSearchQuery(""))
            val state = awaitCondition { it.searchQuery.isEmpty() && it.groupedCurrentTransactions.isNotEmpty() }

            assertThat(state.groupedCurrentTransactions.values.flatten().map { it.id })
                .containsExactly(1L, 2L, 3L)
            assertThat(state.groupedPastTransactions.values.flatten().map { it.id })
                .containsExactly(4L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searching hides the upcoming recurring sections`() = runTest {
        val subscription = Transaction(
            id = 9L,
            amount = BigDecimal("15.00"),
            comment = "Coffee club",
            date = LocalDateTime.of(2026, 1, 5, 0, 0),
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
            subscriptionDay = 5,
        )
        every { budgetRepository.getBudgetSettings() } returns flowOf(budgetSettings())
        every { budgetRepository.getTransactions() } returns flowOf(listOf(subscription))
        val vm = newViewModel()

        vm.uiState.test {
            awaitCondition { it.upcomingRecurrentInPeriod.isNotEmpty() || it.futureRecurrentOutOfPeriod.isNotEmpty() }

            vm.processIntent(HistoryUiIntent.SetSearchQuery("coffee"))
            val state = awaitCondition { it.searchQuery == "coffee" }

            assertThat(state.upcomingRecurrentInPeriod).isEmpty()
            assertThat(state.futureRecurrentOutOfPeriod).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggling a date while searching does not change the expanded dates kept for the normal list`() = runTest {
        every { budgetRepository.getBudgetSettings() } returns flowOf(budgetSettings())
        every { budgetRepository.getTransactions() } returns flowOf(searchFixture())
        val vm = newViewModel()

        vm.uiState.test {
            val before = awaitCondition { it.groupedCurrentTransactions.isNotEmpty() }.expandedDates

            vm.processIntent(HistoryUiIntent.SetSearchQuery("coffee"))
            awaitCondition { it.searchQuery == "coffee" }
            vm.processIntent(HistoryUiIntent.ToggleExpandedDate(LocalDate.of(2026, 1, 10)))
            vm.processIntent(HistoryUiIntent.SetSearchQuery(""))
            val state = awaitCondition { it.searchQuery.isEmpty() && it.groupedCurrentTransactions.isNotEmpty() }

            assertThat(state.expandedDates).isEqualTo(before)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search by date finds every expense on that day`() = runTest {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            every { budgetRepository.getBudgetSettings() } returns flowOf(budgetSettings())
            every { budgetRepository.getTransactions() } returns flowOf(searchFixture())
            val vm = newViewModel()

            vm.uiState.test {
                vm.processIntent(HistoryUiIntent.SetSearchQuery("January 12"))
                val state = awaitCondition { it.searchQuery == "January 12" && it.budgetSettings != null }

                assertThat(state.groupedCurrentTransactions.values.flatten().map { it.id })
                    .containsExactly(2L, 3L)
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `search waits for typing to pause 225 ms before filtering`() = runTest {
        every { settingsRepository.observeSettings() } returns flowOf(UserSettings.DEFAULT.copy(showPastTransactions = true))
        every { budgetRepository.getBudgetSettings() } returns flowOf(budgetSettings())
        every { budgetRepository.getTransactions() } returns flowOf(searchFixture())
        val vm = newViewModel()

        vm.uiState.test {
            awaitCondition { it.groupedCurrentTransactions.isNotEmpty() }

            vm.processIntent(HistoryUiIntent.SetSearchQuery("cof"))
            advanceTimeBy(200.milliseconds)
            vm.processIntent(HistoryUiIntent.SetSearchQuery("coffee"))
            advanceTimeBy(224.milliseconds)
            assertThat(vm.uiState.value.searchQuery).isEmpty()
            assertThat(vm.uiState.value.groupedCurrentTransactions.values.flatten().map { it.id })
                .containsExactly(1L, 2L, 3L)

            advanceTimeBy(2.milliseconds)
            assertThat(vm.uiState.value.searchQuery).isEqualTo("coffee")
            assertThat(vm.uiState.value.groupedCurrentTransactions.values.flatten().map { it.id })
                .containsExactly(1L, 2L, 4L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearing the search applies immediately without waiting`() = runTest {
        every { budgetRepository.getBudgetSettings() } returns flowOf(budgetSettings())
        every { budgetRepository.getTransactions() } returns flowOf(searchFixture())
        val vm = newViewModel()

        vm.uiState.test {
            vm.processIntent(HistoryUiIntent.SetSearchQuery("coffee"))
            awaitCondition { it.searchQuery == "coffee" && it.budgetSettings != null }

            vm.processIntent(HistoryUiIntent.SetSearchQuery(""))
            runCurrent()
            assertThat(vm.uiState.value.searchQuery).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search reports how many expenses matched and their total`() = runTest {
        every { settingsRepository.observeSettings() } returns flowOf(UserSettings.DEFAULT.copy(showPastTransactions = true))
        every { budgetRepository.getBudgetSettings() } returns flowOf(budgetSettings())
        every { budgetRepository.getTransactions() } returns flowOf(searchFixture())
        val vm = newViewModel()

        vm.uiState.test {
            vm.processIntent(HistoryUiIntent.SetSearchQuery("coffee"))
            val state = awaitCondition { it.searchQuery == "coffee" && it.budgetSettings != null }

            assertThat(state.searchResultCount).isEqualTo(3)
            assertThat(state.searchResultTotal).isEqualToIgnoringScale(BigDecimal("25.00"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search total leaves out income adjustments like the day total`() = runTest {
        every { settingsRepository.observeSettings() } returns flowOf(UserSettings.DEFAULT.copy(showPastTransactions = true))
        val income = Transaction(
            id = 7L,
            amount = BigDecimal("-100.00"),
            comment = "Coffee refund",
            date = LocalDateTime.of(2026, 1, 11, 9, 0),
            isAdjustment = true,
        )
        every { budgetRepository.getBudgetSettings() } returns flowOf(budgetSettings())
        every { budgetRepository.getTransactions() } returns flowOf(searchFixture() + income)
        val vm = newViewModel()

        vm.uiState.test {
            vm.processIntent(HistoryUiIntent.SetSearchQuery("coffee"))
            val state = awaitCondition { it.searchQuery == "coffee" && it.budgetSettings != null }

            assertThat(state.searchResultCount).isEqualTo(4)
            assertThat(state.searchResultTotal).isEqualToIgnoringScale(BigDecimal("25.00"))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
