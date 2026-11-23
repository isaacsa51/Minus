package com.serranoie.app.minus.presentation.ui.budget

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.time.CalendarSystem
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class WidgetHeatmapMonthsTest {

    private fun spend(date: LocalDate) = Transaction(
        id = date.toEpochDay(),
        amount = BigDecimal("10.00"),
        comment = "",
        date = date.atTime(12, 0),
    )

    @Test
    fun `gregorian heatmap covers the current and three previous gregorian months`() {
        val months = buildHeatmapMonths(
            transactions = listOf(spend(LocalDate.of(2026, 9, 30))),
            now = LocalDate.of(2026, 10, 13),
            budget = BigDecimal.ONE,
            calendar = CalendarSystem.GREGORIAN,
        )

        assertThat(months.map { it.monthStart }).containsExactly(
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 10, 1),
        ).inOrder()
        assertThat(months.last().days).hasSize(31)
        assertThat(months[2].days.single { it.transactionCount > 0 }.dayOfMonth).isEqualTo(30)
    }

    @Test
    fun `persian heatmap follows persian month boundaries and lengths`() {
        val months = buildHeatmapMonths(
            transactions = listOf(spend(LocalDate.of(2026, 10, 22)), spend(LocalDate.of(2026, 10, 23))),
            now = LocalDate.of(2026, 10, 23),
            budget = BigDecimal.ONE,
            calendar = CalendarSystem.PERSIAN,
        )

        assertThat(months.map { it.monthStart }).containsExactly(
            LocalDate.of(2026, 7, 23),
            LocalDate.of(2026, 8, 23),
            LocalDate.of(2026, 9, 23),
            LocalDate.of(2026, 10, 23),
        ).inOrder()
        assertThat(months[2].days).hasSize(30)
        assertThat(months[2].days.single { it.transactionCount > 0 }.dayOfMonth).isEqualTo(30)
        assertThat(months.last().days.single { it.transactionCount > 0 }.dayOfMonth).isEqualTo(1)
    }

    @Test
    fun `heatmap counts recurring charges on their charge days and skips paid ones`() {
        val paidEarly = Transaction(
            id = 1283L,
            amount = BigDecimal("80"),
            comment = "medical",
            date = LocalDate.of(2026, 11, 19).atTime(20, 13),
        )
        val plannedOneOff = Transaction(
            id = 1279L,
            amount = BigDecimal("80"),
            comment = "medical",
            date = LocalDate.of(2026, 11, 20).atTime(10, 43),
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.ONCE,
            recurrentEndDate = LocalDate.of(2026, 11, 20).atStartOfDay(),
        )
        val monthly = Transaction(
            id = 7L,
            amount = BigDecimal("15"),
            comment = "phone",
            date = LocalDate.of(2026, 9, 5).atTime(9, 0),
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
            subscriptionDay = 5,
        )

        val months = buildHeatmapMonths(
            transactions = listOf(paidEarly, plannedOneOff, monthly),
            now = LocalDate.of(2026, 11, 22),
            budget = BigDecimal.ONE,
            calendar = CalendarSystem.GREGORIAN,
            paidOccurrences = setOf(PaidRecurrentOccurrence(1279L, LocalDate.of(2026, 11, 20))),
        )

        val november = months.last().days
        assertThat(november.single { it.dayOfMonth == 20 }.transactionCount).isEqualTo(0)
        assertThat(november.single { it.dayOfMonth == 19 }.transactionCount).isEqualTo(1)
        assertThat(november.single { it.dayOfMonth == 5 }.transactionCount).isEqualTo(1)
        assertThat(months[2].days.single { it.dayOfMonth == 5 }.transactionCount).isEqualTo(1)
        assertThat(months[1].days.single { it.dayOfMonth == 5 }.transactionCount).isEqualTo(1)
    }
}
