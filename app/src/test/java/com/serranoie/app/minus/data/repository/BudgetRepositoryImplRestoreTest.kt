package com.serranoie.app.minus.data.repository

import androidx.room.withTransaction
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.local.AppDatabase
import com.serranoie.app.minus.data.local.dao.PaidRecurrentOccurrenceDao
import com.serranoie.app.minus.data.local.dao.SavingsPocketDao
import com.serranoie.app.minus.data.local.dao.TransactionDao
import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.RecurrentOccurrenceStatus
import com.serranoie.app.minus.domain.model.SavingsPocket
import com.serranoie.app.minus.domain.model.Transaction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class BudgetRepositoryImplRestoreTest {
    private val database = mockk<AppDatabase>()
    private val transactionDao = mockk<TransactionDao>(relaxed = true)
    private val occurrenceDao = mockk<PaidRecurrentOccurrenceDao>(relaxed = true)
    private val pocketDao = mockk<SavingsPocketDao>(relaxed = true)
    private val repository = BudgetRepositoryImpl(
        appDatabase = database,
        transactionDao = transactionDao,
        settingsDao = mockk(),
        archivedBudgetDao = mockk(),
        categoryDao = mockk(),
        queuedTransactionDao = mockk(),
        paidRecurrentOccurrenceDao = occurrenceDao,
        savingsPocketDao = pocketDao,
    )
    private val date = LocalDate.of(2026, 3, 10)

    @Before
    fun setUp() {
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { database.withTransaction(any<suspend () -> Unit>()) } coAnswers {
            secondArg<suspend () -> Unit>().invoke()
        }
        coEvery { transactionDao.insertOrReplace(any()) } returns 42L
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    @Test
    fun `restore maps the fresh transaction and preserves existing ids and statuses`() = runTest {
        repository.replaceAllTransactions(
            listOf(transaction(0L), transaction(7L)),
            listOf(
                PaidRecurrentOccurrence(0L, date, RecurrentOccurrenceStatus.SKIPPED),
                PaidRecurrentOccurrence(7L, date),
            ),
        )

        coVerify {
            occurrenceDao.insertAll(match { entities ->
                entities.map { it.transactionId } == listOf(42L, 7L) &&
                    entities.map { it.status } == listOf("SKIPPED", "PAID") &&
                    entities.all { it.occurrenceDateEpochDay == date.toEpochDay() }
            })
        }
    }

    @Test
    fun `restore rejects occurrences with no transaction`() = runTest {
        val result = runCatching {
            repository.replaceAllTransactions(listOf(transaction(7L)), listOf(PaidRecurrentOccurrence(99L, date)))
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        coVerify(exactly = 0) { occurrenceDao.insertAll(any()) }
    }

    @Test
    fun `restore rejects id zero when multiple fresh transactions make it ambiguous`() = runTest {
        val result = runCatching {
            repository.replaceAllTransactions(
                listOf(transaction(0L), transaction(0L)),
                listOf(PaidRecurrentOccurrence(0L, date)),
            )
        }

        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        coVerify(exactly = 0) { occurrenceDao.insertAll(any()) }
    }

    private fun transaction(id: Long) = Transaction(
        id = id,
        amount = BigDecimal.TEN,
        comment = "Subscription",
        date = LocalDateTime.of(2026, 3, 10, 9, 30),
        isRecurrent = true,
    )

    @Test
    fun `restore replaces the savings pockets with the backup ones keeping their ids`() = runTest {
        repository.replaceAllTransactions(
            listOf(transaction(7L)),
            emptyList(),
            listOf(SavingsPocket(id = 3L, name = "Trip", createdAt = 1L)),
        )

        coVerifyOrder {
            pocketDao.deleteAll()
            pocketDao.upsert(match { it.id == 3L && it.name == "Trip" })
        }
    }
}
