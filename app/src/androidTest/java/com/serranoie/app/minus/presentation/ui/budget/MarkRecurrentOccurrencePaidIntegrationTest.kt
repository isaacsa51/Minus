package com.serranoie.app.minus.presentation.ui.budget

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.local.AppDatabase
import com.serranoie.app.minus.data.repository.BudgetRepositoryImpl
import com.serranoie.app.minus.domain.calculator.BudgetCalculator
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.usecase.AddTransactionUseCase
import com.serranoie.app.minus.domain.usecase.DeleteTransactionUseCase
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MarkRecurrentOccurrencePaidIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: BudgetRepositoryImpl
    private lateinit var handler: BudgetTransactionHandler

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
            budgetCalculator = BudgetCalculator(),
        )
        handler = BudgetTransactionHandler(
            budgetRepository = repository,
            addTransactionUseCase = AddTransactionUseCase(repository),
            deleteTransactionUseCase = DeleteTransactionUseCase(repository),
            budgetExpressionEvaluator = BudgetExpressionEvaluator(),
            notificationScheduler = mockk(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun markingAFutureOccurrencePaidRecordsItAsHandledAndMaterializesARealTransactionDatedToday() = runBlocking {
        val fixedStartDate = LocalDate.of(2026, 1, 15)
        val scheduledDate = LocalDate.of(2026, 6, 15)

        val template = Transaction.create(
            amount = BigDecimal("15.00"),
            comment = "Netflix",
            date = fixedStartDate.atStartOfDay(),
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
            subscriptionDay = 15,
        )
        repository.addTransaction(template)
        val savedTemplate = repository.getTransactions().first().single { it.comment == "Netflix" }

        val tappedOccurrence = savedTemplate.copy(date = scheduledDate.atStartOfDay())
        val result = handler.markRecurrentOccurrencePaid(tappedOccurrence, activePeriodId = 99L)
        assertThat(result.isSuccess).isTrue()

        val paidDates = repository.getPaidOccurrenceDatesFor(savedTemplate.id)
        assertThat(paidDates).containsExactly(scheduledDate)

        val allTransactions = repository.getTransactions().first()
        val materialized = allTransactions.single { it.id != savedTemplate.id }
        val today = LocalDate.now()

        assertThat(materialized.comment).isEqualTo("Netflix")
        assertThat(materialized.date?.toLocalDate()).isEqualTo(today)
        assertThat(materialized.date?.toLocalDate()).isNotEqualTo(scheduledDate)
        assertThat(materialized.amount).isEqualTo(BigDecimal("15.00"))
        assertThat(materialized.isRecurrent).isFalse()
        assertThat(materialized.sourceTransactionId).isNull()
        assertThat(materialized.periodId).isEqualTo(99L)

        val templateAfter = repository.getTransactionById(savedTemplate.id)
        assertThat(templateAfter?.isRecurrent).isTrue()
        assertThat(templateAfter?.date?.toLocalDate()).isEqualTo(fixedStartDate)
    }

    @Test
    fun markingOneSeriesOccurrencePaidDoesNotTouchAnUnrelatedRecurringSeries() = runBlocking {
        val fixedStartDate = LocalDate.of(2026, 1, 15)
        val scheduledDate = LocalDate.of(2026, 6, 15)

        val netflix = Transaction.create(
            amount = BigDecimal("15.00"),
            comment = "Netflix",
            date = fixedStartDate.atStartOfDay(),
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
            subscriptionDay = 15,
        )
        val spotify = Transaction.create(
            amount = BigDecimal("9.99"),
            comment = "Spotify",
            date = fixedStartDate.atStartOfDay(),
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
            subscriptionDay = 20,
        )
        repository.addTransaction(netflix)
        repository.addTransaction(spotify)
        val savedNetflix = repository.getTransactions().first().single { it.comment == "Netflix" }
        val savedSpotify = repository.getTransactions().first().single { it.comment == "Spotify" }

        val tappedOccurrence = savedNetflix.copy(date = scheduledDate.atStartOfDay())
        handler.markRecurrentOccurrencePaid(tappedOccurrence, activePeriodId = 1L)

        assertThat(repository.getPaidOccurrenceDatesFor(savedNetflix.id)).containsExactly(scheduledDate)
        assertThat(repository.getPaidOccurrenceDatesFor(savedSpotify.id)).isEmpty()

        val allTransactions = repository.getTransactions().first()
        assertThat(allTransactions.none { it.comment == "Spotify" && it.id != savedSpotify.id }).isTrue()

        val spotifyAfter = repository.getTransactionById(savedSpotify.id)
        assertThat(spotifyAfter?.isRecurrent).isTrue()
    }
}
