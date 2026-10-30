package com.serranoie.app.minus.domain.calculator

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.domain.model.CategoryLimit
import com.serranoie.app.minus.domain.model.CategoryLimitUnit
import com.serranoie.app.minus.domain.model.Transaction
import org.junit.Test
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class CategoryLimitsTest {

    private val periodStart = LocalDate.of(2026, 11, 1)
    private val periodEnd = LocalDate.of(2026, 11, 30)
    private val today = LocalDate.of(2026, 11, 11)

    private fun category(name: String, amount: String?, unit: CategoryLimitUnit = CategoryLimitUnit.PERIOD) =
        Category(name = name, limit = amount?.let { CategoryLimit(BigDecimal(it), unit) })

    private fun spend(
        name: String,
        amount: String,
        date: LocalDate,
        isDeleted: Boolean = false,
        isAdjustment: Boolean = false,
        isRecurrent: Boolean = false,
    ) = Transaction(
        amount = BigDecimal(amount),
        comment = name,
        date = LocalDateTime.of(date, java.time.LocalTime.NOON),
        isDeleted = isDeleted,
        isAdjustment = isAdjustment,
        isRecurrent = isRecurrent,
    )

    private fun statuses(categories: List<Category>, transactions: List<Transaction>) =
        categoryLimitStatuses(categories, transactions, periodStart, periodEnd, today, DayOfWeek.MONDAY)

    @Test
    fun categories_without_a_limit_are_left_out() {
        val result = statuses(listOf(category("Food", null)), listOf(spend("Food", "10", today)))

        assertThat(result).isEmpty()
    }

    @Test
    fun period_limit_counts_spend_across_the_whole_period() {
        val result = statuses(
            listOf(category("Food", "300")),
            listOf(spend("Food", "100", periodStart), spend("Food", "20", today), spend("Food", "50", periodEnd.plusDays(1))),
        )

        assertThat(result.getValue("Food").spent).isEqualTo(BigDecimal("120"))
    }

    @Test
    fun weekly_limit_counts_only_the_calendar_week_containing_today() {
        val result = statuses(
            listOf(category("Coffee", "50", CategoryLimitUnit.WEEKLY)),
            listOf(
                spend("Coffee", "5", LocalDate.of(2026, 11, 8)),
                spend("Coffee", "7", LocalDate.of(2026, 11, 9)),
                spend("Coffee", "3", today),
                spend("Coffee", "9", LocalDate.of(2026, 11, 16)),
            ),
        )

        val coffee = result.getValue("Coffee")
        assertThat(coffee.spent).isEqualTo(BigDecimal("10"))
        assertThat(coffee.windowStart).isEqualTo(LocalDate.of(2026, 11, 9))
        assertThat(coffee.windowEnd).isEqualTo(LocalDate.of(2026, 11, 15))
    }

    @Test
    fun week_start_follows_the_given_first_day() {
        val result = categoryLimitStatuses(
            listOf(category("Coffee", "50", CategoryLimitUnit.WEEKLY)),
            emptyList(),
            periodStart,
            periodEnd,
            today,
            DayOfWeek.SUNDAY,
        )

        assertThat(result.getValue("Coffee").windowStart).isEqualTo(LocalDate.of(2026, 11, 8))
    }

    @Test
    fun deleted_adjustment_recurring_and_other_category_rows_are_ignored() {
        val result = statuses(
            listOf(category("Food", "300")),
            listOf(
                spend("Food", "40", today, isDeleted = true),
                spend("Food", "40", today, isAdjustment = true),
                spend("Food", "40", today, isRecurrent = true),
                spend("Rent", "40", today),
                spend("Food", "15", today),
            ),
        )

        assertThat(result.getValue("Food").spent).isEqualTo(BigDecimal("15"))
    }

    @Test
    fun spending_above_the_limit_is_over() {
        val result = statuses(listOf(category("Coffee", "50")), listOf(spend("Coffee", "62", today)))

        assertThat(result.getValue("Coffee").pace).isEqualTo(CategoryLimitPace.OVER)
    }

    @Test
    fun spending_well_ahead_of_elapsed_time_is_ahead() {
        val result = statuses(listOf(category("Food", "300")), listOf(spend("Food", "240", today)))

        assertThat(result.getValue("Food").pace).isEqualTo(CategoryLimitPace.AHEAD)
    }

    @Test
    fun spending_in_line_with_elapsed_time_is_on_track() {
        val result = statuses(listOf(category("Transport", "120")), listOf(spend("Transport", "30", today)))

        assertThat(result.getValue("Transport").pace).isEqualTo(CategoryLimitPace.ON_TRACK)
    }

    @Test
    fun over_by_reports_how_far_a_pending_amount_goes_past_the_limit() {
        val coffee = statuses(listOf(category("Coffee", "50")), listOf(spend("Coffee", "48", today))).getValue("Coffee")

        assertThat(coffee.overBy(BigDecimal("12"))).isEqualTo(BigDecimal("10"))
        assertThat(coffee.overBy(BigDecimal("2"))).isNull()
        assertThat(coffee.overBy(null)).isNull()
    }

    @Test
    fun projected_fraction_grows_with_the_typed_amount_and_caps_at_one() {
        val transport = statuses(listOf(category("Transport", "120")), listOf(spend("Transport", "30", today)))
            .getValue("Transport")

        assertThat(transport.projectedFraction(null)).isWithin(0.001f).of(0.25f)
        assertThat(transport.projectedFraction(BigDecimal("30"))).isWithin(0.001f).of(0.5f)
        assertThat(transport.projectedFraction(BigDecimal("500"))).isEqualTo(1f)
        assertThat(transport.projectedFraction(BigDecimal("-5"))).isWithin(0.001f).of(0.25f)
    }
}
