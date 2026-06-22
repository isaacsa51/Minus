package com.serranoie.app.minus.presentation.ui.budget.controller

import com.serranoie.app.minus.domain.model.BudgetSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PeriodActionsController(
    private val actions: PeriodActions,
) {

    private val _isFirstLaunch = MutableStateFlow(true)
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    sealed interface PeriodAction {
        data object MarkOnboardingCompleted : PeriodAction
        data class PersistBudgetSettings(val settings: BudgetSettings) : PeriodAction
        data class UpdatePeriodEndNotification(val hour: Int, val minute: Int) : PeriodAction
        data class UpdateRecurrentNotification(val hour: Int, val minute: Int) : PeriodAction
        data object FinishBudgetEarly : PeriodAction
        data object ClearEarlyFinish : PeriodAction
        data object TriggerTestNotifications : PeriodAction
    }

    fun markFirstLaunchComplete(): List<PeriodAction> {
        _isFirstLaunch.value = false
        return listOf(PeriodAction.MarkOnboardingCompleted)
    }

    fun saveBudgetSettings(settings: BudgetSettings): List<PeriodAction> {
        if (_isFirstLaunch.value) {
            _isFirstLaunch.value = false
        }
        return listOf(PeriodAction.PersistBudgetSettings(settings))
    }

    fun updatePeriodEndNotificationTime(hour: Int, minute: Int): List<PeriodAction> =
        listOf(PeriodAction.UpdatePeriodEndNotification(hour, minute))

    fun updateRecurrentNotificationTime(hour: Int, minute: Int): List<PeriodAction> =
        listOf(PeriodAction.UpdateRecurrentNotification(hour, minute))

    fun finishBudgetEarly(): List<PeriodAction> = listOf(PeriodAction.FinishBudgetEarly)

    fun clearEarlyFinishState(): List<PeriodAction> = listOf(PeriodAction.ClearEarlyFinish)

    fun triggerTestNotifications(): List<PeriodAction> =
        listOf(PeriodAction.TriggerTestNotifications)
}

interface PeriodActions {
    suspend fun noop()
}
