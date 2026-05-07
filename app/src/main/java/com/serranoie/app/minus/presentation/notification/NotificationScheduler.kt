package com.serranoie.app.minus.presentation.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import logcat.asLog
import logcat.logcat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
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
        const val ACTION_SHOW_PERIOD_END_NOTIFICATION =
            "com.serranoie.app.minus.action.SHOW_PERIOD_END_NOTIFICATION"
        private const val PERIOD_END_ALARM_REQUEST_CODE = 5001
        private const val MIDNIGHT_ALARM_REQUEST_CODE = 5002
        const val ACTION_MIDNIGHT_PERIOD_CHECK =
            "com.serranoie.app.minus.action.MIDNIGHT_PERIOD_CHECK"
    }

    private val workManager by lazy { WorkManager.getInstance(context) }
    private val scope = CoroutineScope(Dispatchers.IO)

    fun initializeNotifications() {
        scheduleRecurrentExpenseCheck()
        checkAndReschedulePeriodEndNotification()
        scheduleMidnightPeriodCheck()
    }

    fun scheduleMidnightPeriodCheck() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, MidnightPeriodTransitionReceiver::class.java).apply {
            action = ACTION_MIDNIGHT_PERIOD_CHECK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MIDNIGHT_ALARM_REQUEST_CODE,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)

        val tomorrow = LocalDate.now().plusDays(1)
        val midnight = LocalDateTime.of(tomorrow, LocalTime.MIDNIGHT)
        val triggerTime = midnight.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                logcat { "Midnight period check scheduled for $midnight" }
            }
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                logcat { "Midnight period check scheduled for $midnight" }
            }
            else -> {
                logcat { "Exact alarms not allowed, falling back to inexact alarm for midnight" }
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        }
    }

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
                logcat { "Error checking period end notification\n${e.asLog()}" }
            }
        }
    }

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
                logcat { "Exact alarms not allowed, falling back to inexact alarm" }
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    pendingIntent
                )
            }
        }
    }

    private fun scheduleRecurrentExpenseCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

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

    fun runRecurrentExpenseCheckNow() {
        val immediateWork = OneTimeWorkRequestBuilder<RecurrentExpenseNotificationWorker>().build()
        workManager.enqueue(immediateWork)
        logcat { "Queued immediate recurrent expense check" }
    }

    fun cancelAllNotifications() {
        logcat { "Cancelling all notification work" }
        cancelPeriodEndNotification()
        cancelMidnightPeriodCheck()
        workManager.cancelUniqueWork(RecurrentExpenseNotificationWorker.WORK_NAME)
    }

    fun cancelMidnightPeriodCheck() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MIDNIGHT_ALARM_REQUEST_CODE,
            Intent(context, MidnightPeriodTransitionReceiver::class.java).apply {
                action = ACTION_MIDNIGHT_PERIOD_CHECK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        logcat { "Midnight period check alarm cancelled" }
    }

    fun rescheduleNotifications(periodEndDate: LocalDate?) {
        logcat { "Rescheduling notifications for period end: $periodEndDate" }
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