package com.serranoie.app.minus.presentation.ui.budget

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.usecase.AddTransactionUseCase
import com.serranoie.app.minus.domain.usecase.DeleteTransactionUseCase
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import com.serranoie.app.minus.presentation.util.ErrorLogRecorder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
}
