package com.serranoie.app.minus.presentation.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import logcat.asLog
import logcat.logcat
import androidx.datastore.preferences.core.edit
import com.serranoie.app.minus.MIDNIGHT_TRANSITION_OCCURRED_KEY
import com.serranoie.app.minus.LAST_PERIOD_END_KEY
import com.serranoie.app.minus.REMAINING_FROM_LAST_PERIOD_KEY
import com.serranoie.app.minus.settingsDataStore
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
import java.time.ZoneId

/**
 * BroadcastReceiver that handles midnight period transition detection.
 * When the clock hits midnight, this receiver checks if a budget period has ended
 * and triggers the appropriate notifications and state updates.
 */
class MidnightPeriodTransitionReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MidnightPeriodTransitionReceiverEntryPoint {
        fun budgetRepository(): BudgetRepository
        fun notificationHelper(): NotificationHelper
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationScheduler.ACTION_MIDNIGHT_PERIOD_CHECK) return

        logcat { "Midnight period check triggered" }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    MidnightPeriodTransitionReceiverEntryPoint::class.java
                )
                val budgetRepository = entryPoint.budgetRepository()
                val notificationHelper = entryPoint.notificationHelper()

                val settings = budgetRepository.getBudgetSettingsSync() ?: run {
                    logcat { "No budget settings found, skipping" }
                    return@launch
                }

                val periodEnd = settings.getPeriodEndDate()
                val today = LocalDate.now()

                // Check if the period ended only after the final day has fully passed
                if (!today.isAfter(periodEnd)) {
                    logcat { "Period has not ended yet (end=$periodEnd, today=$today), skipping" }
                    return@launch
                }

                logcat { "Period has ended! Period end: $periodEnd, Today: $today" }

                // Calculate period summary
                val transactions = budgetRepository.getTransactions().first()
                val periodTransactions = transactions.filter { transaction ->
                    val txDate = transaction.date?.toLocalDate()
                    txDate != null && !txDate.isBefore(settings.startDate) && !txDate.isAfter(periodEnd)
                }
                val totalSpent = periodTransactions
                    .filter { !it.isDeleted }
                    .sumOf { it.amount }
                val remaining = settings.totalBudget.subtract(totalSpent)

                // Show notification
                notificationHelper.showPeriodEndNotification(
                    remainingBudget = remaining.toPlainString(),
                    currency = settings.currencyCode
                )

                // Update DataStore to mark that midnight transition happened
                // This allows MainActivity to detect it when coming to foreground
                updateMidnightTransitionState(context, periodEnd, remaining)

                logcat { "Midnight transition completed successfully" }

            } catch (e: Exception) {
                logcat { "Error handling midnight period check\n${e.asLog()}" }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun updateMidnightTransitionState(
        context: Context,
        periodEnd: LocalDate,
        remaining: java.math.BigDecimal
    ) {
        // Store transition data for foreground detection
        context.settingsDataStore.edit { prefs ->
            prefs[LAST_PERIOD_END_KEY] = periodEnd.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            prefs[REMAINING_FROM_LAST_PERIOD_KEY] = remaining.toPlainString()
            prefs[MIDNIGHT_TRANSITION_OCCURRED_KEY] = true
        }
    }
}
