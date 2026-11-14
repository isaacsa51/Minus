package com.serranoie.app.minus.presentation.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.calculator.pocketStatuses
import com.serranoie.app.minus.domain.calculator.savingsReminderAmount
import com.serranoie.app.minus.domain.calculator.savingsReminderGoal
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import logcat.asLog
import logcat.logcat
import java.time.LocalDate

@HiltWorker
class SavingsReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        remind()
        Result.success()
    } catch (e: Exception) {
        logcat { "Savings reminder failed\n${e.asLog()}" }
        Result.retry()
    }

    private suspend fun remind() {
        val settings = settingsRepository.getSettings()
        if (!settings.savingsReminderEnabled) return
        val budget = budgetRepository.getBudgetSettingsSync() ?: return
        val periodKey = budget.startDate.toString()
        if (settingsRepository.getString(LAST_SAVINGS_REMINDER_KEY) == periodKey) return
        if (LocalDate.now().isAfter(budget.getPeriodEndDate())) return
        if (!notificationHelper.canPostNotifications(NotificationHelper.CHANNEL_SAVINGS)) return
        val pockets = budgetRepository.getSavingsPockets().first()
        val goalPocket = pockets.firstOrNull { it.isGoalPocket }
            ?.let { pocketStatuses(listOf(it), budgetRepository.getTransactions().first()).first() }
        val amount = savingsReminderAmount(settings.savingsPreferences, budget, goalPocket) ?: return
        notificationHelper.showSavingsReminderNotification(
            amount = amount,
            currency = budget.currencyCode,
            goal = savingsReminderGoal(goalPocket),
            pocket = goalPocket?.pocket ?: pockets.singleOrNull(),
        )
        settingsRepository.setString(LAST_SAVINGS_REMINDER_KEY, periodKey)
    }

    companion object {
        private const val LAST_SAVINGS_REMINDER_KEY = "last_savings_reminder_period_start"
    }
}
