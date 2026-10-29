package com.serranoie.app.minus.domain.calculator

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
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
