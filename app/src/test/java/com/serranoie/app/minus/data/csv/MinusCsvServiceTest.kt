package com.serranoie.app.minus.data.csv

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.ArchivedBudget
import com.serranoie.app.minus.domain.model.SavingsPocket
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.RecurrentOccurrenceStatus
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.model.UserSettings
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
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
        coEvery { repository.addTransaction(capture(addedTransactions)) } returns 1L
        coEvery { repository.upsertArchivedBudgets(capture(upsertedArchives)) } just Runs
        every { repository.getSavingsPockets() } returns flowOf(emptyList())
    }

    private fun csvOf(
        transactions: List<Transaction> = emptyList(),
        archived: List<ArchivedBudget> = emptyList(),
        metadata: CsvBackupMetadata? = null,
        paidOccurrences: Set<PaidRecurrentOccurrence> = emptySet(),
        pockets: List<SavingsPocket> = emptyList(),
    ): ByteArrayInputStream {
        val out = ByteArrayOutputStream()
        MinusCsvExporter().export(transactions, archived, metadata, paidOccurrences, out, pockets)
        return ByteArrayInputStream(out.toByteArray())
    }

    private fun tx(
        id: Long,
        amount: String = "10.00",
        comment: String = "Coffee",
        note: String = "",
        date: LocalDateTime = LocalDateTime.of(2026, 3, 10, 9, 30),
        periodId: Long = 7L,
    ) = Transaction(
        id = id,
        amount = BigDecimal(amount),
        comment = comment,
        note = note,
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
    fun `the extra note is carried onto each imported transaction`() = runTest {
        service.importTransactions(
            csvOf(listOf(tx(id = 5L, comment = "Taxi", note = "Split with Sam"), tx(id = 0L, comment = "Rent")))
        )

        assertThat(upsertedTransactions.captured.single().note).isEqualTo("Split with Sam")
        assertThat(addedTransactions.single().note).isEmpty()
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
    fun `a backup exported before the header rename still restores category and extra note`() = runTest {
        val legacy = """
            date,amount,comment,note,is_recurrent,frequency,end_date,sub_day,id,is_credit,is_credit_paid,period_id
            2026-03-10 09:30,10.50,Coffee,Split with Sam,0,,,,5,0,0,7
        """.trimIndent()

        service.importTransactions(ByteArrayInputStream(legacy.toByteArray()))

        val imported = upsertedTransactions.captured.single()
        assertThat(imported.comment).isEqualTo("Coffee")
        assertThat(imported.categoryId).isEqualTo(CATEGORY_IDS["Coffee"])
        assertThat(imported.note).isEqualTo("Split with Sam")
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
    fun `a full backup replaces every local transaction instead of merging`() = runTest {
        val meta = CsvBackupMetadata(
            budgetSettings = settings(),
            currentPeriodStartedAtMillis = 1_709_000_000_000L,
            currentPeriodId = 202L,
        )
        val replaced = slot<List<Transaction>>()
        coEvery { repository.replaceAllTransactions(capture(replaced), any(), any()) } just Runs

        service.importTransactions(
            csvOf(listOf(tx(id = 5L, comment = "Coffee"), tx(id = 0L, comment = "Rent")), metadata = meta)
        )

        assertThat(replaced.captured.map { it.comment }).containsExactly("Coffee", "Rent")
        assertThat(replaced.captured.map { it.id }).containsExactly(5L, 0L)
        assertThat(upsertedTransactions.isCaptured).isFalse()
        assertThat(addedTransactions).isEmpty()
    }

    @Test
    fun `a partial import without metadata merges and never wipes`() = runTest {
        service.importTransactions(csvOf(listOf(tx(id = 1L))))

        coVerify(exactly = 0) { repository.replaceAllTransactions(any(), any(), any()) }
    }

    @Test
    fun `a partial import merges paid and skipped occurrences without replacing local data`() = runTest {
        val date = LocalDate.of(2026, 3, 10)
        val paid = PaidRecurrentOccurrence(5L, date)
        val skipped = PaidRecurrentOccurrence(5L, date.plusMonths(1), RecurrentOccurrenceStatus.SKIPPED)

        service.importTransactions(
            csvOf(transactions = listOf(tx(id = 5L)), paidOccurrences = setOf(paid, skipped))
        )

        coVerify { repository.upsertTransactions(match { it.single().id == 5L }) }
        coVerify(exactly = 1) { repository.markRecurrentOccurrencePaid(5L, date, RecurrentOccurrenceStatus.PAID) }
        coVerify(exactly = 1) {
            repository.markRecurrentOccurrencePaid(5L, date.plusMonths(1), RecurrentOccurrenceStatus.SKIPPED)
        }
        coVerify(exactly = 0) { repository.replaceAllTransactions(any(), any(), any()) }
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
        coEvery { repository.getPaidRecurrentOccurrences() } returns flowOf(emptySet())
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

    @Test
    fun `a full backup restores its pockets and keeps pocket rows linked`() = runTest {
        val meta = CsvBackupMetadata(
            budgetSettings = settings(),
            currentPeriodStartedAtMillis = 1_709_000_000_000L,
            currentPeriodId = 202L,
        )
        val pocket = SavingsPocket(id = 3L, name = "Trip", createdAt = 1L)
        val replaced = slot<List<Transaction>>()
        val restoredPockets = slot<List<SavingsPocket>>()
        coEvery { repository.replaceAllTransactions(capture(replaced), any(), capture(restoredPockets)) } just Runs

        service.importTransactions(
            csvOf(
                listOf(tx(id = 5L, amount = "75.00", comment = "Trip").copy(isAdjustment = true, pocketId = 3L)),
                metadata = meta,
                pockets = listOf(pocket),
            ),
        )

        assertThat(restoredPockets.captured).containsExactly(pocket)
        assertThat(replaced.captured.single().pocketId).isEqualTo(3L)
        assertThat(replaced.captured.single().isAdjustment).isTrue()
    }

    @Test
    fun `a partial import saves the pockets it carries`() = runTest {
        val pocket = SavingsPocket(id = 3L, name = "Trip", createdAt = 1L)

        service.importTransactions(csvOf(listOf(tx(id = 5L)), pockets = listOf(pocket)))

        coVerify { repository.saveSavingsPocket(pocket) }
    }

    @Test
    fun `exportAllTransactions writes the savings pockets`() = runTest {
        every { repository.getTransactions() } returns flowOf(emptyList())
        every { repository.getArchivedBudgets() } returns flowOf(emptyList())
        every { repository.getPaidRecurrentOccurrences() } returns flowOf(emptySet())
        every { repository.getSavingsPockets() } returns flowOf(listOf(SavingsPocket(id = 3L, name = "Trip", createdAt = 1L)))
        coEvery { repository.getBudgetSettingsSync() } returns null
        val out = ByteArrayOutputStream()

        service.exportAllTransactions(out)

        val parsed = MinusCsvParser().parse(ByteArrayInputStream(out.toByteArray()))
        assertThat(parsed.pockets.single().name).isEqualTo("Trip")
    }
}
