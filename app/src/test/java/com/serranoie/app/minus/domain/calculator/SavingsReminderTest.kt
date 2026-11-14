package com.serranoie.app.minus.domain.calculator

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.SavingsPocket
import com.serranoie.app.minus.domain.model.SavingsPreferences
import com.serranoie.app.minus.domain.model.SavingsSplitPreset
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class SavingsReminderTest {

    private fun budget(
        total: String = "1000",
        period: BudgetPeriod = BudgetPeriod.MONTHLY,
        start: LocalDate = LocalDate.of(2026, 11, 1),
        end: LocalDate? = null,
    ) = BudgetSettings(totalBudget = BigDecimal(total), period = period, startDate = start, endDate = end)

    @Test
    fun without_goal_the_amount_is_the_savings_percentage_of_the_budget() {
        val amount = savingsReminderAmount(SavingsPreferences.DEFAULT, budget(total = "1000"))

        assertThat(amount).isEqualTo(BigDecimal("200.00"))
    }

    @Test
    fun conservative_preset_uses_its_ten_percent() {
        val prefs = SavingsPreferences.fromPreset(SavingsSplitPreset.CONSERVATIVE)

        assertThat(savingsReminderAmount(prefs, budget(total = "1500"))).isEqualTo(BigDecimal("150.00"))
    }

    @Test
    fun monthly_period_with_goal_asks_for_goal_divided_by_months() {
        val prefs = SavingsPreferences.DEFAULT.copy(savingsGoalAmount = BigDecimal("1200"), savingsGoalMonths = 6)

        assertThat(savingsReminderAmount(prefs, budget())).isEqualTo(BigDecimal("200.00"))
    }

    private val goalPrefs = SavingsPreferences.DEFAULT.copy(savingsGoalAmount = BigDecimal("15000"), savingsGoalMonths = 6)
    private val today = LocalDate.of(2026, 11, 13)

    private fun goalPocket(saved: String, deadline: LocalDate?) = SavingsPocketStatus(
        SavingsPocket(id = 1, name = "", target = BigDecimal("15000"), targetDate = deadline),
        BigDecimal(saved),
    )

    @Test
    fun with_a_goal_pocket_the_amount_is_what_is_left_spread_over_the_months_left() {
        val amount = savingsReminderAmount(goalPrefs, budget(), goalPocket("3000", LocalDate.of(2027, 5, 13)), today)

        assertThat(amount).isEqualTo(BigDecimal("2000.00"))
    }

    @Test
    fun a_partial_month_before_the_deadline_counts_as_a_month() {
        val amount = savingsReminderAmount(goalPrefs, budget(), goalPocket("3000", LocalDate.of(2027, 5, 1)), today)

        assertThat(amount).isEqualTo(BigDecimal("2000.00"))
    }

    @Test
    fun a_reached_goal_falls_back_to_the_savings_percentage() {
        val amount = savingsReminderAmount(goalPrefs, budget(total = "9000"), goalPocket("15000", LocalDate.of(2027, 5, 13)), today)

        assertThat(amount).isEqualTo(BigDecimal("1800.00"))
    }

    @Test
    fun a_passed_deadline_asks_for_the_whole_remainder() {
        val amount = savingsReminderAmount(goalPrefs, budget(), goalPocket("14000", LocalDate.of(2026, 11, 1)), today)

        assertThat(amount).isEqualTo(BigDecimal("1000.00"))
    }

    @Test
    fun a_goal_pocket_without_deadline_falls_back_to_the_settings_goal() {
        val amount = savingsReminderAmount(goalPrefs, budget(), goalPocket("3000", null), today)

        assertThat(amount).isEqualTo(BigDecimal("2500.00"))
    }

    @Test
    fun the_goal_line_reports_what_is_left_and_the_deadline() {
        val goal = savingsReminderGoal(goalPocket("3000", LocalDate.of(2027, 5, 13)))

        assertThat(goal).isEqualTo(SavingsReminderGoal(BigDecimal("12000"), LocalDate.of(2027, 5, 13)))
    }

    @Test
    fun no_goal_line_without_deadline_or_once_reached() {
        assertThat(savingsReminderGoal(goalPocket("3000", null))).isNull()
        assertThat(savingsReminderGoal(goalPocket("15000", LocalDate.of(2027, 5, 13)))).isNull()
        assertThat(savingsReminderGoal(null)).isNull()
    }

    @Test
    fun pending_set_aside_round_trips_pocket_and_amount() {
        val raw = encodePendingSetAside(7L, BigDecimal("2000.00"))

        assertThat(raw).isEqualTo("7|2000")
        assertThat(decodePendingSetAside(raw)).isEqualTo(7L to "2000")
        assertThat(decodePendingSetAside("")).isNull()
        assertThat(decodePendingSetAside("x|2000")).isNull()
    }

    @Test
    fun weekly_period_with_goal_scales_the_monthly_amount_to_seven_days() {
        val prefs = SavingsPreferences.DEFAULT.copy(savingsGoalAmount = BigDecimal("1200"), savingsGoalMonths = 6)
        val weekly = budget(period = BudgetPeriod.WEEKLY, end = LocalDate.of(2026, 11, 7))

        assertThat(savingsReminderAmount(prefs, weekly)).isEqualTo(BigDecimal("45.99"))
    }

    @Test
    fun zero_savings_percentage_without_goal_gives_no_reminder() {
        val prefs = SavingsPreferences(preset = SavingsSplitPreset.CUSTOM, needsPct = 70, wantsPct = 30, savingsPct = 0)

        assertThat(savingsReminderAmount(prefs, budget())).isNull()
    }

    @Test
    fun empty_budget_without_goal_gives_no_reminder() {
        assertThat(savingsReminderAmount(SavingsPreferences.DEFAULT, budget(total = "0"))).isNull()
    }

    private val nine = LocalTime.of(9, 0)
    private val start = LocalDate.of(2026, 11, 1)

    @Test
    fun before_the_start_day_the_reminder_fires_on_the_start_day_at_the_alert_time() {
        val trigger = savingsReminderTriggerAt(start, nine, LocalDateTime.of(2026, 10, 29, 22, 0))

        assertThat(trigger).isEqualTo(LocalDateTime.of(2026, 11, 1, 9, 0))
    }

    @Test
    fun on_the_start_day_before_the_alert_time_it_waits_for_the_alert_time() {
        val trigger = savingsReminderTriggerAt(start, nine, LocalDateTime.of(2026, 11, 1, 7, 30))

        assertThat(trigger).isEqualTo(LocalDateTime.of(2026, 11, 1, 9, 0))
    }

    @Test
    fun on_the_start_day_after_the_alert_time_it_fires_right_away() {
        val now = LocalDateTime.of(2026, 11, 1, 15, 0)

        assertThat(savingsReminderTriggerAt(start, nine, now)).isEqualTo(now)
    }

    @Test
    fun after_the_start_day_there_is_no_reminder() {
        assertThat(savingsReminderTriggerAt(start, nine, LocalDateTime.of(2026, 11, 2, 8, 0))).isNull()
    }
}
