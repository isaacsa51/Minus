package com.serranoie.app.minus.presentation.ui.editor.sheets.split

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

class DynamicAllocationsTest {

    private val totalBudget = BigDecimal("20326.00")
    private val totalSpent = BigDecimal("14839.27")

    @Test
    fun `spec example - 6 days remaining gives 914_46 daily`() {
        // 5486.73 / 6 = 914.455 exactly. With HALF_UP rounding to 2 dp the
        // 3rd-decimal 5 rounds up to 914.46. (The spec's "914.45" is an
        // approximate example; we follow the codebase's HALF_UP convention
        // used by the existing SplitBudgetTest for consistency.)
        val alloc = computeDynamicAllocations(
            totalBudget = totalBudget,
            totalSpentInPeriod = totalSpent,
            totalSpentToday = BigDecimal.ZERO,
            daysRemaining = 6,
        )

        assertThat(alloc.dailyAllocation).isEqualTo(BigDecimal("914.46"))
    }

    @Test
    fun `spec example - 6 days remaining gives 5486_73 for weekly final block`() {
        // 6 < 7 -> blocks = 1 -> allocation = 5486.73 / 1 = 5486.73
        val alloc = computeDynamicAllocations(
            totalBudget = totalBudget,
            totalSpentInPeriod = totalSpent,
            totalSpentToday = BigDecimal.ZERO,
            daysRemaining = 6,
        )

        assertThat(alloc.weeklyAllocation).isEqualTo(BigDecimal("5486.73"))
    }

    @Test
    fun `spec example - 6 days remaining gives 5486_73 for monthly final block`() {
        // 6 < 30 -> blocks = 1 -> allocation = 5486.73 / 1 = 5486.73
        val alloc = computeDynamicAllocations(
            totalBudget = totalBudget,
            totalSpentInPeriod = totalSpent,
            totalSpentToday = BigDecimal.ZERO,
            daysRemaining = 6,
        )

        assertThat(alloc.monthlyAllocation).isEqualTo(BigDecimal("5486.73"))
    }

    @Test
    fun `14 days remaining - weekly is half the remaining balance`() {
        // 5486.73 / ceil(14/7)=2 = 2743.365 -> 2743.37
        val alloc = computeDynamicAllocations(
            totalBudget = totalBudget,
            totalSpentInPeriod = totalSpent,
            totalSpentToday = BigDecimal.ZERO,
            daysRemaining = 14,
        )
        assertThat(alloc.weeklyAllocation).isEqualTo(BigDecimal("2743.37"))
    }

    @Test
    fun `14 days remaining - biweekly is the full remaining balance`() {
        // 14 / 14 = 1 -> blocks = 1 -> 5486.73 / 1 = 5486.73
        val alloc = computeDynamicAllocations(
            totalBudget = totalBudget,
            totalSpentInPeriod = totalSpent,
            totalSpentToday = BigDecimal.ZERO,
            daysRemaining = 14,
        )
        assertThat(alloc.biweeklyAllocation).isEqualTo(BigDecimal("5486.73"))
    }

    @Test
    fun `15 days remaining - weekly uses 3 blocks (ceiling division)`() {
        // 15 / 7 = 2.14 -> ceil = 3 -> 5486.73 / 3 = 1828.91
        val alloc = computeDynamicAllocations(
            totalBudget = totalBudget,
            totalSpentInPeriod = totalSpent,
            totalSpentToday = BigDecimal.ZERO,
            daysRemaining = 15,
        )
        assertThat(alloc.weeklyAllocation).isEqualTo(BigDecimal("1828.91"))
    }

    @Test
    fun `7 days remaining - weekly block is the full balance`() {
        // 7 / 7 = 1 -> blocks = 1 -> 5486.73
        val alloc = computeDynamicAllocations(
            totalBudget = totalBudget,
            totalSpentInPeriod = totalSpent,
            totalSpentToday = BigDecimal.ZERO,
            daysRemaining = 7,
        )
        assertThat(alloc.weeklyAllocation).isEqualTo(BigDecimal("5486.73"))
    }

    @Test
    fun `daily allocation decreases as days remaining decreases`() {
        // The whole point of dynamic mode: more time = smaller daily cap.
        val a = computeDynamicAllocations(
            totalBudget = totalBudget,
            totalSpentInPeriod = totalSpent,
            totalSpentToday = BigDecimal.ZERO,
            daysRemaining = 30,
        ).dailyAllocation
        val b = computeDynamicAllocations(
            totalBudget = totalBudget,
            totalSpentInPeriod = totalSpent,
            totalSpentToday = BigDecimal.ZERO,
            daysRemaining = 6,
        ).dailyAllocation
        assertThat(b).isGreaterThan(a)
    }

    @Test
    fun `period fully spent - all allocations are zero and over flag is true`() {
        val alloc = computeDynamicAllocations(
            totalBudget = totalBudget,
            totalSpentInPeriod = totalBudget, // exactly at limit
            totalSpentToday = BigDecimal.ZERO,
            daysRemaining = 6,
        )
        assertThat(alloc.dailyAllocation).isEqualTo(BigDecimal.ZERO)
        assertThat(alloc.weeklyAllocation).isEqualTo(BigDecimal.ZERO)
        assertThat(alloc.biweeklyAllocation).isEqualTo(BigDecimal.ZERO)
        assertThat(alloc.monthlyAllocation).isEqualTo(BigDecimal.ZERO)
        assertThat(alloc.isTodayOverDailyAllocation).isTrue()
    }

    @Test
    fun `overspent - all allocations are zero and over flag is true`() {
        val alloc = computeDynamicAllocations(
            totalBudget = BigDecimal("1000"),
            totalSpentInPeriod = BigDecimal("1500"),
            totalSpentToday = BigDecimal.ZERO,
            daysRemaining = 6,
        )
        assertThat(alloc.dailyAllocation).isEqualTo(BigDecimal.ZERO)
        assertThat(alloc.isTodayOverDailyAllocation).isTrue()
    }

    @Test
    fun `zero total budget - everything is zero, over flag follows today spend`() {
        val alloc = computeDynamicAllocations(
            totalBudget = BigDecimal.ZERO,
            totalSpentInPeriod = BigDecimal.ZERO,
            totalSpentToday = BigDecimal("50"),
            daysRemaining = 6,
        )
        assertThat(alloc.dailyAllocation).isEqualTo(BigDecimal.ZERO)
        assertThat(alloc.isTodayOverDailyAllocation).isTrue()
    }

    @Test
    fun `zero days remaining - everything is zero, over flag follows today spend`() {
        val alloc = computeDynamicAllocations(
            totalBudget = totalBudget,
            totalSpentInPeriod = totalSpent,
            totalSpentToday = BigDecimal("1.00"),
            daysRemaining = 0,
        )
        assertThat(alloc.dailyAllocation).isEqualTo(BigDecimal.ZERO)
        assertThat(alloc.isTodayOverDailyAllocation).isTrue()
    }

    @Test
    fun `today's spend exceeds daily allocation - over flag is true`() {
        // 1000 budget, 500 spent (today), 10 days remaining
        //   remaining = 500, daily = 500/10 = 50.00
        //   totalSpentToday = 500 > 50.00 -> over
        val alloc = computeDynamicAllocations(
            totalBudget = BigDecimal("1000"),
            totalSpentInPeriod = BigDecimal("500"),
            totalSpentToday = BigDecimal("500"),
            daysRemaining = 10,
        )
        assertThat(alloc.dailyAllocation).isEqualTo(BigDecimal("50.00"))
        assertThat(alloc.isTodayOverDailyAllocation).isTrue()
    }

    @Test
    fun `today's spend is within daily allocation - over flag is false`() {
        // 1000 budget, 50 spent (today), 10 days remaining
        //   remaining = 950, daily = 95.00
        //   totalSpentToday = 50 < 95.00 -> not over
        val alloc = computeDynamicAllocations(
            totalBudget = BigDecimal("1000"),
            totalSpentInPeriod = BigDecimal("50"),
            totalSpentToday = BigDecimal("50"),
            daysRemaining = 10,
        )
        assertThat(alloc.dailyAllocation).isEqualTo(BigDecimal("95.00"))
        assertThat(alloc.isTodayOverDailyAllocation).isFalse()
    }

    @Test
    fun `no spend today - over flag is false`() {
        val alloc = computeDynamicAllocations(
            totalBudget = totalBudget,
            totalSpentInPeriod = totalSpent,
            totalSpentToday = BigDecimal.ZERO,
            daysRemaining = 6,
        )
        assertThat(alloc.isTodayOverDailyAllocation).isFalse()
    }

    @Test
    fun `forPeriod returns the matching allocation`() {
        val alloc = computeDynamicAllocations(
            totalBudget = totalBudget,
            totalSpentInPeriod = totalSpent,
            totalSpentToday = BigDecimal.ZERO,
            daysRemaining = 6,
        )
        assertThat(alloc.forPeriod(com.serranoie.app.minus.domain.model.BudgetPeriod.DAILY))
            .isEqualTo(alloc.dailyAllocation)
        assertThat(alloc.forPeriod(com.serranoie.app.minus.domain.model.BudgetPeriod.WEEKLY))
            .isEqualTo(alloc.weeklyAllocation)
        assertThat(alloc.forPeriod(com.serranoie.app.minus.domain.model.BudgetPeriod.BIWEEKLY))
            .isEqualTo(alloc.biweeklyAllocation)
        assertThat(alloc.forPeriod(com.serranoie.app.minus.domain.model.BudgetPeriod.MONTHLY))
            .isEqualTo(alloc.monthlyAllocation)
    }
}
