package com.serranoie.app.minus.data.csv

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.ArchivedBudget
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.model.UserSettings
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class MinusCsvServiceTest {

    private val repository: BudgetRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private lateinit var service: MinusCsvService

    private val upsertedTransactions = slot<List<Transaction>>()
    private val addedTransactions = mutableListOf<Transaction>()
    private val upsertedArchives = slot<List<ArchivedBudget>>()

    @Before
    fun setUp() {
        service = MinusCsvService(repository, settingsRepository)

        coEvery { repository.findOrCreateCategory(any()) } answers {
            val name = firstArg<String>()
            Category(id = CATEGORY_IDS[name] ?: 99L, name = name)
        }
        coEvery { repository.upsertTransactions(capture(upsertedTransactions)) } just Runs
        coEvery { repository.addTransaction(capture(addedTransactions)) } just Runs
        coEvery { repository.upsertArchivedBudgets(capture(upsertedArchives)) } just Runs
    }

    private fun csvOf(
        transactions: List<Transaction> = emptyList(),
        archived: List<ArchivedBudget> = emptyList(),
        metadata: CsvBackupMetadata? = null,
    ): ByteArrayInputStream {
        val out = ByteArrayOutputStream()
        MinusCsvExporter().export(transactions, archived, metadata, out)
        return ByteArrayInputStream(out.toByteArray())
    }

    private fun tx(
        id: Long,
        amount: String = "10.00",
        comment: String = "Coffee",
        date: LocalDateTime = LocalDateTime.of(2026, 3, 10, 9, 30),
        periodId: Long = 7L,
    ) = Transaction(
        id = id,
        amount = BigDecimal(amount),
        comment = comment,
        date = date,
        periodId = periodId,
    )

    private fun settings(
        start: LocalDate = LocalDate.of(2026, 1, 1),
        end: LocalDate = LocalDate.of(2026, 1, 31),
    ) = BudgetSettings(
        totalBudget = BigDecimal("1000.00"),
        period = BudgetPeriod.MONTHLY,
        startDate = start,
        endDate = end,
    )

    @Test
    fun `rows with a real id are upserted with that id preserved and never re-inserted`() = runTest {
        service.importTransactions(csvOf(listOf(tx(id = 1L, comment = "Coffee"), tx(id = 2L, comment = "Rent"))))

        assertThat(upsertedTransactions.isCaptured).isTrue()
        assertThat(upsertedTransactions.captured.map { it.id }).containsExactly(1L, 2L)
        assertThat(addedTransactions).isEmpty()
    }

    @Test
    fun `rows with id zero are inserted fresh, never upserted`() = runTest {
        service.importTransactions(csvOf(listOf(tx(id = 0L, comment = "Coffee"))))

        assertThat(upsertedTransactions.isCaptured).isFalse()
        assertThat(addedTransactions).hasSize(1)
        assertThat(addedTransactions.single().id).isEqualTo(0L)
    }

    @Test
    fun `a backup mixing kept and fresh rows routes each to the correct path`() = runTest {
        service.importTransactions(
            csvOf(listOf(tx(id = 5L, comment = "Coffee"), tx(id = 0L, comment = "Rent")))
        )

        assertThat(upsertedTransactions.captured.map { it.id }).containsExactly(5L)
        assertThat(addedTransactions.map { it.comment }).containsExactly("Rent")
    }

    @Test
    fun `each distinct non-blank comment resolves a category exactly once`() = runTest {
        service.importTransactions(
            csvOf(
                listOf(
                    tx(id = 1L, comment = "Coffee"),
                    tx(id = 2L, comment = "Coffee"),
                    tx(id = 3L, comment = "Rent"),
                )
            )
        )

        coVerify(exactly = 1) { repository.findOrCreateCategory("Coffee") }
        coVerify(exactly = 1) { repository.findOrCreateCategory("Rent") }
    }

    @Test
    fun `the resolved category id is attached to each imported transaction`() = runTest {
        service.importTransactions(
            csvOf(listOf(tx(id = 1L, comment = "Coffee"), tx(id = 2L, comment = "Rent")))
        )

        val byComment = upsertedTransactions.captured.associate { it.comment to it.categoryId }
        assertThat(byComment["Coffee"]).isEqualTo(CATEGORY_IDS["Coffee"])
        assertThat(byComment["Rent"]).isEqualTo(CATEGORY_IDS["Rent"])
    }

    @Test
    fun `blank comments do not trigger category resolution and leave the category unset`() = runTest {
        service.importTransactions(csvOf(listOf(tx(id = 1L, comment = "   "))))

        coVerify(exactly = 0) { repository.findOrCreateCategory(any()) }
        assertThat(upsertedTransactions.captured.single().categoryId).isNull()
    }

    @Test
    fun `archived budgets in the backup are upserted`() = runTest {
        val archive = ArchivedBudget(
            periodId = 50L,
            totalBudget = BigDecimal("800.00"),
            spentAmount = BigDecimal("600.00"),
            startDate = LocalDate.of(2025, 12, 1),
            endDate = LocalDate.of(2025, 12, 31),
            currencyCode = "USD",
            periodType = BudgetPeriod.MONTHLY,
            createdAt = 1_700_000_000_000L,
        )

        service.importTransactions(csvOf(archived = listOf(archive)))

        assertThat(upsertedArchives.isCaptured).isTrue()
        assertThat(upsertedArchives.captured).containsExactly(archive)
    }

    @Test
    fun `a backup with no archives does not call upsertArchivedBudgets`() = runTest {
        service.importTransactions(csvOf(listOf(tx(id = 1L))))

        assertThat(upsertedArchives.isCaptured).isFalse()
    }

    @Test
    fun `metadata applies the budget settings, current period and marks onboarding complete`() = runTest {
        val meta = CsvBackupMetadata(
            budgetSettings = settings(),
            currentPeriodStartedAtMillis = 1_709_000_000_000L,
            currentPeriodId = 202L,
        )

        service.importTransactions(csvOf(metadata = meta))

        coVerify { repository.saveBudgetSettings(any()) }
        coVerify { settingsRepository.setCurrentPeriod(202L, 1_709_000_000_000L) }
        coVerify { settingsRepository.setOnboardingCompleted(true) }
    }

    @Test
    fun `a backup without metadata leaves settings and onboarding untouched`() = runTest {
        service.importTransactions(csvOf(listOf(tx(id = 1L))))

        coVerify(exactly = 0) { repository.saveBudgetSettings(any()) }
        coVerify(exactly = 0) { settingsRepository.setCurrentPeriod(any(), any()) }
        coVerify(exactly = 0) { settingsRepository.setOnboardingCompleted(any()) }
    }

    @Test
    fun `the result reports valid rows as imported and rejected rows as discarded`() = runTest {
        val result = service.importTransactions(
            csvOf(
                listOf(
                    tx(id = 1L, amount = "10.00"),
                    tx(id = 2L, amount = "20.00"),
                    tx(id = 3L, amount = "0.00"),
                )
            )
        )

        assertThat(result.imported).isEqualTo(2)
        assertThat(result.discarded).isEqualTo(1)
        assertThat(result.errors).hasSize(1)
    }

    @Test
    fun `getExportFileName builds the name from the period sequence and its dates`() = runTest {
        coEvery { repository.getBudgetSettingsSync() } returns settings(
            start = LocalDate.of(2026, 1, 1),
            end = LocalDate.of(2026, 1, 31),
        )
        coEvery { repository.getPeriodCount() } returns 4

        assertThat(service.getExportFileName()).isEqualTo("minus_backup-BP5_01jan-31jan.csv")
    }

    @Test
    fun `getExportFileName falls back to the default when there is no budget`() = runTest {
        coEvery { repository.getBudgetSettingsSync() } returns null

        assertThat(service.getExportFileName()).isEqualTo(MinusCsvContract.FILE_NAME)
    }

    @Test
    fun `exportAllTransactions writes a metadata row and every transaction`() = runTest {
        coEvery { repository.getTransactions() } returns flowOf(listOf(tx(id = 1L, comment = "Groceries")))
        coEvery { repository.getArchivedBudgets() } returns flowOf(emptyList())
        coEvery { repository.getBudgetSettingsSync() } returns settings()
        coEvery { settingsRepository.getSettings() } returns UserSettings.DEFAULT

        val out = ByteArrayOutputStream()
        service.exportAllTransactions(out)
        val csv = out.toString(Charsets.UTF_8.name())

        assertThat(csv).contains(MinusCsvContract.MARKER_META)
        assertThat(csv).contains("Groceries")
    }

    private companion object {
        val CATEGORY_IDS = mapOf("Coffee" to 11L, "Rent" to 22L)
    }
}
