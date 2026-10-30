package com.serranoie.app.minus.presentation.ui.budget

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.time.CalendarSystem
import com.serranoie.app.minus.domain.usecase.AddTransactionUseCase
import com.serranoie.app.minus.domain.usecase.DeleteTransactionUseCase
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import com.serranoie.app.minus.presentation.util.ErrorLogRecorder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetTransactionHandlerTest {

    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val notificationScheduler: NotificationScheduler = mockk(relaxed = true)
    private val errorLogRecorder: ErrorLogRecorder = mockk(relaxed = true)

    private lateinit var handler: BudgetTransactionHandler

    private val template = Transaction(
        id = 42L,
        amount = BigDecimal("15.00"),
        comment = "Netflix",
        date = LocalDateTime.of(2026, 1, 15, 0, 0),
        isRecurrent = true,
        recurrentFrequency = RecurrentFrequency.MONTHLY,
        subscriptionDay = 15,
    )

    @Before
    fun setUp() {
        handler = BudgetTransactionHandler(
            budgetRepository = budgetRepository,
            addTransactionUseCase = AddTransactionUseCase(budgetRepository),
            deleteTransactionUseCase = DeleteTransactionUseCase(budgetRepository),
            budgetExpressionEvaluator = BudgetExpressionEvaluator(),
            notificationScheduler = notificationScheduler,
            errorLogRecorder = errorLogRecorder,
        )

        coEvery { budgetRepository.getTransactionById(template.id) } returns template
        coEvery { budgetRepository.findOrCreateCategory(any()) } returns Category(id = 1L, name = "Netflix")
    }

    @Test
    fun `marking a future occurrence paid records the scheduled date as handled`() = runTest {
        val futureOccurrence = template.copy(date = LocalDate.of(2026, 4, 15).atStartOfDay())

        handler.markRecurrentOccurrencePaid(futureOccurrence, activePeriodId = 7L)

        coVerify {
            budgetRepository.markRecurrentOccurrencePaid(template.id, LocalDate.of(2026, 4, 15))
        }
    }

    @Test
    fun `marking an occurrence paid creates a real transaction dated today, not the scheduled date`() = runTest {
        val futureOccurrence = template.copy(date = LocalDate.of(2026, 4, 15).atStartOfDay())
        val captured = slot<Transaction>()

        handler.markRecurrentOccurrencePaid(futureOccurrence, activePeriodId = 7L)

        coVerify { budgetRepository.addTransaction(capture(captured)) }
        val recorded = captured.captured
        assertThat(recorded.date?.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(recorded.date?.toLocalDate()).isNotEqualTo(LocalDate.of(2026, 4, 15))
        assertThat(recorded.amount).isEqualTo(template.amount)
        assertThat(recorded.comment).isEqualTo(template.comment)
        assertThat(recorded.periodId).isEqualTo(7L)
    }

    @Test
    fun `the materialized transaction is not itself recurring and has no link back to the template`() = runTest {
        val futureOccurrence = template.copy(date = LocalDate.of(2026, 4, 15).atStartOfDay())
        val captured = slot<Transaction>()

        handler.markRecurrentOccurrencePaid(futureOccurrence, activePeriodId = 7L)

        coVerify { budgetRepository.addTransaction(capture(captured)) }
        assertThat(captured.captured.sourceTransactionId).isNull()
        assertThat(captured.captured.isRecurrent).isFalse()
    }

    @Test
    fun `marking paid reschedules the series notification past the paid date`() = runTest {
        val futureOccurrence = template.copy(date = LocalDate.of(2026, 4, 15).atStartOfDay())

        handler.markRecurrentOccurrencePaid(futureOccurrence, activePeriodId = 7L)

        coVerify { notificationScheduler.scheduleRecurrentExpenseNotification(template) }
    }

    @Test
    fun `marking a virtual occurrence paid resolves back to the real template via sourceTransactionId`() = runTest {
        val virtualOccurrence = template.copy(
            id = template.id * 1000000 + LocalDate.of(2026, 2, 15).toEpochDay(),
            date = LocalDate.of(2026, 2, 15).atStartOfDay(),
            sourceTransactionId = template.id,
        )

        handler.markRecurrentOccurrencePaid(virtualOccurrence, activePeriodId = 7L)

        coVerify {
            budgetRepository.markRecurrentOccurrencePaid(template.id, LocalDate.of(2026, 2, 15))
        }
    }

    @Test
    fun `editing an income transaction with negative amount succeeds`() = runTest {
        val incomeTransaction = Transaction(
            id = 5L,
            amount = BigDecimal("-5.50"),
            comment = "Salary adjustment",
            date = LocalDateTime.now(),
        )

        val result = handler.editTransaction(incomeTransaction)

        assertThat(result.isSuccess).isTrue()
        coVerify { budgetRepository.updateTransaction(any()) }
    }

    @Test
    fun `editing a transaction with zero amount fails`() = runTest {
        val zeroTransaction = Transaction(
            id = 6L,
            amount = BigDecimal("0.00"),
            comment = "Zero test",
            date = LocalDateTime.now(),
        )

        val result = handler.editTransaction(zeroTransaction)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `persian monthly recurrence lands on the persian subscription day of the current month`() {
        val date = handler.resolveRecurrentTransactionDate(
            frequency = RecurrentFrequency.MONTHLY,
            subscriptionDay = 1,
            now = LocalDateTime.of(2026, 10, 13, 10, 30),
            calendar = CalendarSystem.PERSIAN,
        )

        assertThat(date).isEqualTo(LocalDateTime.of(2026, 10, 23, 10, 30))
    }

    @Test
    fun `monthly subscription day that already passed this month starts next month`() {
        val date = handler.resolveRecurrentTransactionDate(
            frequency = RecurrentFrequency.MONTHLY,
            subscriptionDay = 5,
            now = LocalDateTime.of(2026, 10, 18, 10, 30),
            calendar = CalendarSystem.GREGORIAN,
        )

        assertThat(date).isEqualTo(LocalDateTime.of(2026, 11, 5, 10, 30))
    }

    @Test
    fun `monthly subscription day of today stays today`() {
        val date = handler.resolveRecurrentTransactionDate(
            frequency = RecurrentFrequency.MONTHLY,
            subscriptionDay = 18,
            now = LocalDateTime.of(2026, 10, 18, 10, 30),
            calendar = CalendarSystem.GREGORIAN,
        )

        assertThat(date).isEqualTo(LocalDateTime.of(2026, 10, 18, 10, 30))
    }

    private suspend fun applySplit(
        input: String,
        comment: String,
        isRecurrentEnabled: Boolean = false,
        budgetSettings: BudgetSettings? = null,
    ): ApplyTransactionResult {
        every { budgetRepository.getAllCategories() } returns flowOf(listOf(Category(id = 9L, name = "Food+Drinks")))
        coEvery { budgetRepository.findOrCreateCategory(any()) } answers {
            val name = firstArg<String>()
            Category(id = name.length.toLong(), name = name)
        }
        return handler.applyTransaction(
            input = input,
            isCalculation = true,
            isRecurrentEnabled = isRecurrentEnabled,
            isCreditEnabled = true,
            comment = comment,
            note = "lunch run",
            budgetSettings = budgetSettings,
            resolveActivePeriodId = { 7L },
        )
    }

    @Test
    fun `split categories save one transaction per typed amount in a single write`() = runTest {
        val saved = slot<List<Transaction>>()

        val result = applySplit(input = "15+125+66", comment = "uber+food+coffee")

        assertThat(result).isInstanceOf(ApplyTransactionResult.Added::class.java)
        assertThat((result as ApplyTransactionResult.Added).splitCount).isEqualTo(3)
        coVerify(exactly = 1) { budgetRepository.addTransactions(capture(saved)) }
        coVerify(exactly = 0) { budgetRepository.addTransaction(any()) }
        val transactions = saved.captured
        assertThat(transactions.map { it.comment }).containsExactly("uber", "food", "coffee").inOrder()
        assertThat(transactions.map { it.amount }).containsExactly(BigDecimal("15"), BigDecimal("125"), BigDecimal("66")).inOrder()
        assertThat(transactions.map { it.categoryId }).containsExactly(4L, 4L, 6L).inOrder()
        assertThat(transactions.map { it.periodId }.toSet()).containsExactly(7L)
        assertThat(transactions.map { it.date }.toSet()).hasSize(1)
        assertThat(transactions.all { it.isCredit && it.note == "lunch run" }).isTrue()
    }

    @Test
    fun `split with mismatched counts saves nothing and reports both counts`() = runTest {
        val result = applySplit(input = "15+125+66", comment = "uber+food")

        assertThat(result).isEqualTo(ApplyTransactionResult.SplitMismatch(amounts = 3, categories = 2))
        coVerify(exactly = 0) { budgetRepository.addTransactions(any()) }
        coVerify(exactly = 0) { budgetRepository.addTransaction(any()) }
    }

    @Test
    fun `split is refused while recurring is enabled`() = runTest {
        val result = applySplit(input = "15+125", comment = "uber+food", isRecurrentEnabled = true)

        assertThat(result).isEqualTo(ApplyTransactionResult.SplitWithRecurrent)
        coVerify(exactly = 0) { budgetRepository.addTransactions(any()) }
    }

    @Test
    fun `an existing category whose name has a plus is saved whole`() = runTest {
        val saved = slot<Transaction>()

        applySplit(input = "15+125", comment = "Food+Drinks")

        coVerify { budgetRepository.addTransaction(capture(saved)) }
        assertThat(saved.captured.amount).isEqualTo(BigDecimal("140"))
        assertThat(saved.captured.comment).isEqualTo("Food+Drinks")
    }

    @Test
    fun `a single category still saves the total`() = runTest {
        val saved = slot<Transaction>()

        applySplit(input = "15+125+66", comment = "food")

        coVerify { budgetRepository.addTransaction(capture(saved)) }
        assertThat(saved.captured.amount).isEqualTo(BigDecimal("206"))
        coVerify(exactly = 0) { budgetRepository.addTransactions(any()) }
    }

    @Test
    fun `split after the period ended queues every part`() = runTest {
        val ended = BudgetSettings(
            totalBudget = BigDecimal("1000"),
            period = BudgetPeriod.MONTHLY,
            startDate = LocalDate.now().minusDays(30),
            endDate = LocalDate.now().minusDays(1),
        )
        val queued = mutableListOf<Transaction>()
        coEvery { budgetRepository.addQueuedTransaction(capture(queued)) } returns Unit

        val result = applySplit(input = "15+125", comment = "uber+food", budgetSettings = ended)

        assertThat(result).isInstanceOf(ApplyTransactionResult.QueuedForNextPeriod::class.java)
        assertThat(queued.map { it.comment to it.amount })
            .containsExactly("uber" to BigDecimal("15"), "food" to BigDecimal("125")).inOrder()
        coVerify(exactly = 0) { budgetRepository.addTransactions(any()) }
    }
}
