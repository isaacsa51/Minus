package com.serranoie.app.minus.presentation.ui.theme.component.budget

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.analytics.GraphGranularity
import com.serranoie.app.minus.presentation.ui.theme.component.budget.graphs.calculateCumulativePoints
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class BudgetGraphTest {

    private fun localDateToDate(localDate: LocalDate): Date {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
    }

    @Test
    fun `calculateCumulativePoints correctly sums daily transactions`() {
        val start = LocalDate.of(2026, 8, 1)
        val end = LocalDate.of(2026, 8, 5)
        
        val transactions = listOf(
            Transaction(amount = BigDecimal("10"), date = start.atTime(10, 0)),
            Transaction(amount = BigDecimal("20"), date = start.plusDays(2).atTime(10, 0)),
            Transaction(amount = BigDecimal("30"), date = start.plusDays(4).atTime(10, 0))
        )

        val result = calculateCumulativePoints(
            transactions = transactions,
            startDate = localDateToDate(start),
            endDate = localDateToDate(end),
            granularity = GraphGranularity.DAYS
        )

        assertThat(result).hasSize(6)
        assertThat(result[0]).isEqualTo(BigDecimal.ZERO)
        assertThat(result[1]).isEqualTo(BigDecimal("10"))
        assertThat(result[3]).isEqualTo(BigDecimal("30"))
        assertThat(result[5]).isEqualTo(BigDecimal("60"))
    }

    @Test
    fun `calculateCumulativePoints handles weekly granularity`() {
        val start = LocalDate.of(2026, 8, 1)
        val end = LocalDate.of(2026, 8, 21) // 3 weeks total
        
        val transactions = listOf(
            Transaction(amount = BigDecimal("100"), date = start.atTime(10, 0)), // Week 1
            Transaction(amount = BigDecimal("200"), date = start.plusDays(8).atTime(10, 0)), // Week 2
            Transaction(amount = BigDecimal("300"), date = start.plusDays(15).atTime(10, 0)) // Week 3
        )

        val result = calculateCumulativePoints(
            transactions = transactions,
            startDate = localDateToDate(start),
            endDate = localDateToDate(end),
            granularity = GraphGranularity.WEEK
        )

        assertThat(result).hasSize(4)
        assertThat(result[0]).isEqualTo(BigDecimal.ZERO)
        assertThat(result[1]).isEqualTo(BigDecimal("100"))
        assertThat(result[2]).isEqualTo(BigDecimal("300"))
        assertThat(result[3]).isEqualTo(BigDecimal("600"))
    }
}
