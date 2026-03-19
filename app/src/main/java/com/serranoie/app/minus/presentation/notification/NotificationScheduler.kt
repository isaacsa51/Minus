package com.serranoie.app.minus.presentation.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.serranoie.app.minus.DEFAULT_NOTIFICATION_HOUR
import com.serranoie.app.minus.DEFAULT_NOTIFICATION_MINUTE
import com.serranoie.app.minus.NOTIFICATION_HOUR_KEY
import com.serranoie.app.minus.NOTIFICATION_MINUTE_KEY
import com.serranoie.app.minus.settingsDataStore
import kotlinx.coroutines.flow.first
import com.serranoie.app.minus.data.repository.BudgetRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for budget-related notifications using WorkManager.
 * Schedules:
 * 1. Period end notifications - scheduled with AlarmManager for the day after the budget period ends
 * 2. Recurrent expense notifications - runs daily to check for due expenses
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val budgetRepository: BudgetRepository
) {
    companion object {
        private const val TAG = "NotificationScheduler"
        const val ACTION_SHOW_PERIOD_END_NOTIFICATION =
            "com.serranoie.app.minus.action.SHOW_PERIOD_END_NOTIFICATION"
        private const val PERIOD_END_ALARM_REQUEST_CODE = 5001
    }

    private val workManager by lazy { WorkManager.getInstance(context) }
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Initialize all notification workers.
     * Call this when the app starts or when budget settings change.
     */
    fun initializeNotifications() {
        scheduleRecurrentExpenseCheck()
        checkAndReschedulePeriodEndNotification()
    }

    /**
     * Check if period end notification needs to be rescheduled.
     * This is called on app startup to ensure notifications are properly scheduled.
     */
    private fun checkAndReschedulePeriodEndNotification() {
        scope.launch {
            try {
                val settings = budgetRepository.getBudgetSettingsSync()
                if (settings != null) {
                    schedulePeriodEndNotification(settings.getPeriodEndDate())
                } else {
                    cancelPeriodEndNotification()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking period end notification", e)
            }
        }
    }

    /**
     * Schedule an AlarmManager alarm for the day after the period ends.
     * Call this when budget settings are saved.
     */
    fun schedulePeriodEndNotification(periodEndDate: LocalDate, currentDate: LocalDate = LocalDate.now()) {
        scope.launch {
            val (hour, minute) = getPeriodEndNotificationTime()
            schedulePeriodEndNotification(periodEndDate, hour, minute, currentDate)
        }
    }
    
    private fun schedulePeriodEndNotification(
        periodEndDate: LocalDate,
        hour: Int,
        minute: Int,
        currentDate: LocalDate
    ) {
        val notificationDate = periodEndDate.plusDays(1)
        val now = LocalDateTime.now()
        val triggerDateTime = LocalDateTime.of(notificationDate, LocalTime.of(hour, minute))
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, PeriodEndAlarmReceiver::class.java).apply {
            action = ACTION_SHOW_PERIOD_END_NOTIFICATION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            PERIOD_END_ALARM_REQUEST_CODE,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)

        when {
            currentDate.isAfter(notificationDate) || !triggerDateTime.isAfter(now) -> {
                context.sendBroadcast(alarmIntent)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    pendingIntent
                )
            }
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    pendingIntent
                )
            }
            else -> {
                Log.w(TAG, "Exact alarms not allowed, falling back to inexact alarm")
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    pendingIntent
                )
            }
        }
    }

    /**
     * Schedule daily check for recurrent expenses.
     * Runs once per day at 8 AM to check if any recurrent expenses are due.
     */
    private fun scheduleRecurrentExpenseCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        // Calculate initial delay to 8 AM
        val now = LocalDateTime.now()
        val targetTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 0))
        val initialDelay = if (targetTime.isAfter(now)) {
            java.time.Duration.between(now, targetTime).toMillis()
        } else {
            java.time.Duration.between(now, targetTime.plusDays(1)).toMillis()
        }

        val workRequest = PeriodicWorkRequestBuilder<RecurrentExpenseNotificationWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            RecurrentExpenseNotificationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Cancel all scheduled notifications.
     * Call this when user wants to disable notifications.
     */
    fun cancelAllNotifications() {
        Log.d(TAG, "Cancelling all notification work")
        cancelPeriodEndNotification()
        workManager.cancelUniqueWork(RecurrentExpenseNotificationWorker.WORK_NAME)
    }

    /**
     * Reschedule notifications when budget settings change.
     */
    fun rescheduleNotifications(periodEndDate: LocalDate?) {
        Log.d(TAG, "Rescheduling notifications for period end: $periodEndDate")
        if (periodEndDate != null) {
            schedulePeriodEndNotification(periodEndDate)
        } else {
            cancelPeriodEndNotification()
        }
    }
    
    fun cancelPeriodEndNotification() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            PERIOD_END_ALARM_REQUEST_CODE,
            Intent(context, PeriodEndAlarmReceiver::class.java).apply {
                action = ACTION_SHOW_PERIOD_END_NOTIFICATION
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
    
    private suspend fun getPeriodEndNotificationTime(): Pair<Int, Int> {
        val preferences = context.settingsDataStore.data.first()
        val hour = preferences[NOTIFICATION_HOUR_KEY] ?: DEFAULT_NOTIFICATION_HOUR
        val minute = preferences[NOTIFICATION_MINUTE_KEY] ?: DEFAULT_NOTIFICATION_MINUTE
        return hour to minute
    }
}