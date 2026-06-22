package com.serranoie.app.minus.presentation.ui.budget.controller

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.presentation.ui.budget.controller.PeriodActionsController.PeriodAction
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Unit tests for [PeriodActionsController]. The controller owns the
 * in-memory [isFirstLaunch] flag and emits [PeriodAction]s that the
 * parent ViewModel applies to its own UI state and routes to the
 * appropriate use case. Backticked English test names (when_X_then_Y)
 * following the BDD Given/When/Then convention.
 */
class PeriodActionsControllerTest {

    private fun newController() = PeriodActionsController(object : PeriodActions {
        override suspend fun noop() = Unit
    })

    private fun sampleSettings() = BudgetSettings(
        totalBudget = BigDecimal("1000.00"),
        period = BudgetPeriod.MONTHLY,
        startDate = LocalDate.of(2026, 1, 1),
        endDate = LocalDate.of(2026, 1, 30),
        currencyCode = "USD",
        daysInPeriod = 30,
    )

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `when_controller_is_created_then_is_first_launch_is_true`() {
        val controller = newController()

        assertThat(controller.isFirstLaunch.value).isTrue()
    }

    // -------------------------------------------------------------------------
    // markFirstLaunchComplete
    // -------------------------------------------------------------------------

    @Test
    fun `when_mark_first_launch_complete_is_called_then_is_first_launch_becomes_false_and_action_is_emitted`() {
        val controller = newController()

        val actions = controller.markFirstLaunchComplete()

        assertThat(controller.isFirstLaunch.value).isFalse()
        assertThat(actions).containsExactly(PeriodAction.MarkOnboardingCompleted)
    }

    @Test
    fun `when_mark_first_launch_complete_is_called_twice_then_action_is_emitted_twice_but_flag_stays_false`() {
        val controller = newController()

        controller.markFirstLaunchComplete()
        val actions = controller.markFirstLaunchComplete()

        assertThat(controller.isFirstLaunch.value).isFalse()
        assertThat(actions).containsExactly(PeriodAction.MarkOnboardingCompleted)
    }

    // -------------------------------------------------------------------------
    // saveBudgetSettings
    // -------------------------------------------------------------------------

    @Test
    fun `when_save_budget_settings_is_called_then_persist_action_is_emitted_and_is_first_launch_becomes_false`() {
        val controller = newController()
        val settings = sampleSettings()

        val actions = controller.saveBudgetSettings(settings)

        assertThat(controller.isFirstLaunch.value).isFalse()
        assertThat(actions).containsExactly(PeriodAction.PersistBudgetSettings(settings))
    }

    @Test
    fun `when_save_budget_settings_is_called_twice_then_persist_action_is_emitted_twice_with_each_settings`() {
        val controller = newController()
        val settings1 = sampleSettings()
        val settings2 = sampleSettings().copy(totalBudget = BigDecimal("2000.00"))

        val firstActions = controller.saveBudgetSettings(settings1)
        val secondActions = controller.saveBudgetSettings(settings2)

        assertThat(firstActions).containsExactly(PeriodAction.PersistBudgetSettings(settings1))
        assertThat(secondActions).containsExactly(PeriodAction.PersistBudgetSettings(settings2))
    }

    // -------------------------------------------------------------------------
    // updatePeriodEndNotificationTime
    // -------------------------------------------------------------------------

    @Test
    fun `when_update_period_end_notification_time_is_called_then_action_with_hour_and_minute_is_emitted`() {
        val controller = newController()

        val actions = controller.updatePeriodEndNotificationTime(hour = 9, minute = 30)

        assertThat(actions).containsExactly(PeriodAction.UpdatePeriodEndNotification(9, 30))
    }

    // -------------------------------------------------------------------------
    // updateRecurrentNotificationTime
    // -------------------------------------------------------------------------

    @Test
    fun `when_update_recurrent_notification_time_is_called_then_action_with_hour_and_minute_is_emitted`() {
        val controller = newController()

        val actions = controller.updateRecurrentNotificationTime(hour = 8, minute = 0)

        assertThat(actions).containsExactly(PeriodAction.UpdateRecurrentNotification(8, 0))
    }

    // -------------------------------------------------------------------------
    // finishBudgetEarly / clearEarlyFinishState
    // -------------------------------------------------------------------------

    @Test
    fun `when_finish_budget_early_is_called_then_finish_action_is_emitted`() {
        val controller = newController()

        val actions = controller.finishBudgetEarly()

        assertThat(actions).containsExactly(PeriodAction.FinishBudgetEarly)
    }

    @Test
    fun `when_clear_early_finish_state_is_called_then_clear_action_is_emitted`() {
        val controller = newController()

        val actions = controller.clearEarlyFinishState()

        assertThat(actions).containsExactly(PeriodAction.ClearEarlyFinish)
    }

    // -------------------------------------------------------------------------
    // triggerTestNotifications
    // -------------------------------------------------------------------------

    @Test
    fun `when_trigger_test_notifications_is_called_then_action_is_emitted`() {
        val controller = newController()

        val actions = controller.triggerTestNotifications()

        assertThat(actions).containsExactly(PeriodAction.TriggerTestNotifications)
    }

    @Test
    fun `when_trigger_test_notifications_is_called_twice_then_two_actions_are_emitted`() {
        val controller = newController()

        controller.triggerTestNotifications()
        val second = controller.triggerTestNotifications()

        assertThat(second).containsExactly(PeriodAction.TriggerTestNotifications)
    }
}
