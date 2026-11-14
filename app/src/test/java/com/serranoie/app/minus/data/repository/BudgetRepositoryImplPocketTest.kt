package com.serranoie.app.minus.data.repository

import androidx.room.withTransaction
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.local.AppDatabase
import com.serranoie.app.minus.data.local.dao.TransactionDao
import com.serranoie.app.minus.data.local.entity.TransactionEntity
import com.serranoie.app.minus.domain.model.Transaction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class BudgetRepositoryImplPocketTest {
    private val database = mockk<AppDatabase>()
    private val transactionDao = mockk<TransactionDao>(relaxed = true)
    private val repository = BudgetRepositoryImpl(
        appDatabase = database,
        transactionDao = transactionDao,
        settingsDao = mockk(),
        archivedBudgetDao = mockk(),
        categoryDao = mockk(),
        queuedTransactionDao = mockk(),
        paidRecurrentOccurrenceDao = mockk(),
        savingsPocketDao = mockk(),
    )

    @Before
    fun setUp() {
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { database.withTransaction(any<suspend () -> Boolean>()) } coAnswers {
            secondArg<suspend () -> Boolean>().invoke()
        }
        coEvery { transactionDao.getByPocketId(3L) } returns listOf(row("500"), row("-100"))
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    @Test
    fun `a withdrawal within the balance is inserted inside one transaction`() = runTest {
        val inserted = repository.addPocketWithdrawal(withdrawal("400"))

        assertThat(inserted).isTrue()
        coVerify { database.withTransaction(any<suspend () -> Boolean>()) }
        coVerify { transactionDao.insert(match { it.pocketId == 3L && BigDecimal(it.amount).compareTo(BigDecimal("-400")) == 0 }) }
    }

    @Test
    fun `a withdrawal above the balance is rejected without inserting`() = runTest {
        val inserted = repository.addPocketWithdrawal(withdrawal("400.01"))

        assertThat(inserted).isFalse()
        coVerify(exactly = 0) { transactionDao.insert(any()) }
    }

    private fun withdrawal(amount: String) = Transaction(
        amount = BigDecimal(amount).negate(),
        date = LocalDateTime.of(2026, 11, 14, 12, 0),
        isAdjustment = true,
        pocketId = 3L,
    )

    private fun row(amount: String) = TransactionEntity(
        amount = amount,
        comment = "",
        date = 0L,
        isAdjustment = true,
        pocketId = 3L,
    )
}
