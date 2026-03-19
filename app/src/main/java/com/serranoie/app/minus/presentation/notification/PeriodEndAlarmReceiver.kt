package com.serranoie.app.minus.presentation.notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.serranoie.app.minus.data.repository.BudgetRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
class PeriodEndAlarmReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PeriodEndAlarmReceiverEntryPoint {
        fun budgetRepository(): BudgetRepository
        fun notificationHelper(): NotificationHelper
    }
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationScheduler.ACTION_SHOW_PERIOD_END_NOTIFICATION) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    PeriodEndAlarmReceiverEntryPoint::class.java
                )
                val budgetRepository = entryPoint.budgetRepository()
                val notificationHelper = entryPoint.notificationHelper()
	            val settings = budgetRepository.getBudgetSettingsSync() ?: return@launch
	            val periodEnd = settings.getPeriodEndDate()
                val today = LocalDate.now()
                if (!today.isAfter(periodEnd)) {
                    return@launch
                }
                val transactions = budgetRepository.getTransactions().first()
                val periodTransactions = transactions.filter { transaction ->
                    val txDate = transaction.date?.toLocalDate()
                    txDate != null && !txDate.isBefore(settings.startDate) && !txDate.isAfter(periodEnd)
                }
                val totalSpent = periodTransactions
                    .filter { !it.isDeleted }
                    .sumOf { it.amount }
                val remaining = settings.totalBudget.subtract(totalSpent)
                notificationHelper.showPeriodEndNotification(
                    remainingBudget = remaining.toPlainString(),
                    currency = settings.currencyCode
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error handling period end alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
    companion object {
        private const val TAG = "PeriodEndAlarmReceiver"
    }
}