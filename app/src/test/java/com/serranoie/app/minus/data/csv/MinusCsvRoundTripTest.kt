package com.serranoie.app.minus.data.csv

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.ArchivedBudget
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.Transaction
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class MinusCsvRoundTripTest {

    private val exporter = MinusCsvExporter()
    private val parser = MinusCsvParser()

    private fun roundTrip(
        transactions: List<Transaction> = emptyList(),
        archived: List<ArchivedBudget> = emptyList(),
        metadata: CsvBackupMetadata? = null,
    ): CsvImportPayload {
        val out = ByteArrayOutputStream()
        exporter.export(transactions, archived, metadata, out)
        return parser.parse(ByteArrayInputStream(out.toByteArray()))
    }

    private fun tx(
        id: Long = 1L,
        amount: String = "10.00",
        comment: String = "Coffee",
        date: LocalDateTime = LocalDateTime.of(2026, 3, 10, 9, 30),
        isRecurrent: Boolean = false,
        frequency: RecurrentFrequency? = null,
        recurrentEndDate: LocalDateTime? = null,
        subscriptionDay: Int? = null,
        isCredit: Boolean = false,
        isCreditPaid: Boolean = false,
        periodId: Long = 7L,
    ) = Transaction(
        id = id,
        amount = BigDecimal(amount),
        comment = comment,
        date = date,
        periodId = periodId,
        isRecurrent = isRecurrent,
        recurrentFrequency = frequency,
        recurrentEndDate = recurrentEndDate,
        subscriptionDay = subscriptionDay,
        isCredit = isCredit,
        isCreditPaid = isCreditPaid,
    )

    @Test
    fun `a plain transaction survives the round trip field for field`() {
        val original = tx(id = 42L, amount = "12.50", comment = "Lunch", periodId = 9L)

        val row = roundTrip(transactions = listOf(original)).rows.single()

        assertThat(row.id).isEqualTo(42L)
        assertThat(row.amount).isEqualTo(BigDecimal("12.50"))
        assertThat(row.comment).isEqualTo("Lunch")
        assertThat(row.date).isEqualTo(LocalDateTime.of(2026, 3, 10, 9, 30))
        assertThat(row.periodId).isEqualTo(9L)
        assertThat(row.isRecurrent).isFalse()
        assertThat(row.isCredit).isFalse()
        assertThat(row.isCreditPaid).isFalse()
    }

    @Test
    fun `a recurrent transaction keeps its frequency, subscription day and end date`() {
        val original = tx(
            id = 2L,
            comment = "Netflix",
            isRecurrent = true,
            frequency = RecurrentFrequency.MONTHLY,
            recurrentEndDate = LocalDateTime.of(2026, 12, 31, 0, 0),
            subscriptionDay = 15,
        )

        val row = roundTrip(transactions = listOf(original)).rows.single()

        assertThat(row.isRecurrent).isTrue()
        assertThat(row.frequency).isEqualTo(RecurrentFrequency.MONTHLY)
        assertThat(row.subscriptionDay).isEqualTo(15)
        assertThat(row.endDate).isEqualTo(LocalDate.of(2026, 12, 31))
    }

    @Test
    fun `credit flags survive the round trip`() {
        val original = tx(id = 3L, isCredit = true, isCreditPaid = true)

        val row = roundTrip(transactions = listOf(original)).rows.single()

        assertThat(row.isCredit).isTrue()
        assertThat(row.isCreditPaid).isTrue()
    }

    @Test
    fun `amount scale is preserved exactly`() {
        val rows = roundTrip(
            transactions = listOf(
                tx(id = 1L, amount = "0.01"),
                tx(id = 2L, amount = "12.5"),
                tx(id = 3L, amount = "1000.00"),
            )
        ).rows

        assertThat(rows.map { it.amount }).containsExactly(
            BigDecimal("0.01"), BigDecimal("12.5"), BigDecimal("1000.00"),
        ).inOrder()
    }

    @Test
    fun `comments with commas and quotes are escaped and read back verbatim`() {
        val nasty = """Lunch, with team "the A-team" & co"""
        val original = tx(id = 5L, comment = nasty)

        val row = roundTrip(transactions = listOf(original)).rows.single()

        assertThat(row.comment).isEqualTo(nasty)
    }

    @Test
    fun `transactions keep their original order`() {
        val rows = roundTrip(
            transactions = listOf(
                tx(id = 1L, comment = "first"),
                tx(id = 2L, comment = "second"),
                tx(id = 3L, comment = "third"),
            )
        ).rows

        assertThat(rows.map { it.comment }).containsExactly("first", "second", "third").inOrder()
    }

    @Test
    fun `seconds on a transaction date are truncated to the minute`() {
        val original = tx(id = 1L, date = LocalDateTime.of(2026, 3, 10, 9, 30, 45))

        val row = roundTrip(transactions = listOf(original)).rows.single()

        assertThat(row.date).isEqualTo(LocalDateTime.of(2026, 3, 10, 9, 30, 0))
    }

    @Test
    fun `a transaction with no date is dropped on export`() {
        val original = tx(id = 1L).copy(date = null)

        val payload = roundTrip(transactions = listOf(original))

        assertThat(payload.rows).isEmpty()
    }

    @Test
    fun `a non-positive amount is rejected on import`() {
        val payload = roundTrip(transactions = listOf(tx(id = 1L, amount = "0.00")))

        assertThat(payload.rows).isEmpty()
        assertThat(payload.errors).isNotEmpty()
    }

    @Test
    fun `an archived budget survives the round trip`() {
        val archive = ArchivedBudget(
            periodId = 101L,
            totalBudget = BigDecimal("1500.00"),
            spentAmount = BigDecimal("1234.56"),
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 1, 31),
            currencyCode = "EUR",
            periodType = BudgetPeriod.MONTHLY,
            createdAt = 1_710_000_000_000L,
        )

        val parsed = roundTrip(archived = listOf(archive)).archivedBudgets.single()

        assertThat(parsed).isEqualTo(archive)
    }

    @Test
    fun `backup metadata survives the round trip`() {
        val settings = BudgetSettings(
            totalBudget = BigDecimal("2000.00"),
            period = BudgetPeriod.MONTHLY,
            startDate = LocalDate.of(2026, 2, 1),
            endDate = LocalDate.of(2026, 2, 28),
            currencyCode = "GBP",
            daysInPeriod = 28,
            rollOverEnabled = true,
            rollOverCarryForward = true,
            remainingBudgetStrategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
            creditCardCutoffDay = 5,
            splitMode = BudgetSplitMode.DYNAMIC,
        )
        val metadata = CsvBackupMetadata(
            budgetSettings = settings,
            currentPeriodStartedAtMillis = 1_709_000_000_000L,
            currentPeriodId = 202L,
        )

        val parsed = roundTrip(metadata = metadata).metadata!!
        val s = parsed.budgetSettings

        assertThat(s.totalBudget).isEqualTo(BigDecimal("2000.00"))
        assertThat(s.period).isEqualTo(BudgetPeriod.MONTHLY)
        assertThat(s.startDate).isEqualTo(LocalDate.of(2026, 2, 1))
        assertThat(s.endDate).isEqualTo(LocalDate.of(2026, 2, 28))
        assertThat(s.currencyCode).isEqualTo("GBP")
        assertThat(s.daysInPeriod).isEqualTo(28)
        assertThat(s.rollOverEnabled).isTrue()
        assertThat(s.rollOverCarryForward).isTrue()
        assertThat(s.remainingBudgetStrategy).isEqualTo(RemainingBudgetStrategy.SPLIT_EQUALLY)
        assertThat(s.creditCardCutoffDay).isEqualTo(5)
        assertThat(s.splitMode).isEqualTo(BudgetSplitMode.DYNAMIC)
        assertThat(parsed.currentPeriodStartedAtMillis).isEqualTo(1_709_000_000_000L)
        assertThat(parsed.currentPeriodId).isEqualTo(202L)
    }

    @Test
    fun `metadata with no end date comes back with the derived period end date`() {
        val settings = BudgetSettings(
            totalBudget = BigDecimal("900.00"),
            period = BudgetPeriod.WEEKLY,
            startDate = LocalDate.of(2026, 3, 2),
            endDate = null,
            daysInPeriod = 7,
        )
        val metadata = CsvBackupMetadata(settings, 0L, 0L)

        val parsed = roundTrip(metadata = metadata).metadata!!

        assertThat(parsed.budgetSettings.endDate).isEqualTo(settings.getPeriodEndDate())
    }

    @Test
    fun `a full backup with metadata, an archive and transactions all come back together`() {
        val settings = BudgetSettings(
            totalBudget = BigDecimal("1000.00"),
            period = BudgetPeriod.MONTHLY,
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 1, 31),
        )
        val payload = roundTrip(
            transactions = listOf(tx(id = 1L, comment = "a"), tx(id = 2L, comment = "b")),
            archived = listOf(
                ArchivedBudget(
                    periodId = 50L,
                    totalBudget = BigDecimal("800.00"),
                    spentAmount = BigDecimal("600.00"),
                    startDate = LocalDate.of(2025, 12, 1),
                    endDate = LocalDate.of(2025, 12, 31),
                    currencyCode = "USD",
                    periodType = BudgetPeriod.MONTHLY,
                    createdAt = 1_700_000_000_000L,
                )
            ),
            metadata = CsvBackupMetadata(settings, 111L, 222L),
        )

        assertThat(payload.rows).hasSize(2)
        assertThat(payload.archivedBudgets).hasSize(1)
        assertThat(payload.metadata).isNotNull()
        assertThat(payload.errors).isEmpty()
    }
}
