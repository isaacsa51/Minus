package com.serranoie.app.minus.presentation.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.CreditCard
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.model.calculatePaymentDueDate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import logcat.asLog
import logcat.logcat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for budget-related notifications using WorkManager.
 * Schedules:
 * 1. Period end notifications - scheduled with AlarmManager for the configured period end time
 * 2. Recurrent expense notifications - scheduled per transaction occurrence at the configured time
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
) {
    companion object {
        const val ACTION_SHOW_PERIOD_END_NOTIFICATION =
            "com.serranoie.app.minus.action.SHOW_PERIOD_END_NOTIFICATION"
        private const val PERIOD_END_ALARM_REQUEST_CODE = 5001
        private const val MIDNIGHT_ALARM_REQUEST_CODE = 5002
        const val ACTION_MIDNIGHT_PERIOD_CHECK =
            "com.serranoie.app.minus.action.MIDNIGHT_PERIOD_CHECK"
        private const val MAX_OCCURRENCE_LOOKUP_ITERATIONS = 500
        const val CREDIT_CUTOFF_REMINDER_DAYS = 3L
        const val UPCOMING_REMINDER_LEAD_DAYS = 3L
        private const val CREDIT_CUTOFF_WORK_NAME = "credit_cutoff_reminder"
        private const val MAX_CREDIT_CUTOFF_LOOKAHEAD_MONTHS = 24L
    }

    private val workManager by lazy { WorkManager.getInstance(context) }
    private val scope = CoroutineScope(Dispatchers.IO)

    fun initializeNotifications() {
        scheduleAllRecurrentExpenseNotifications()
        scheduleCreditCutoffReminder()
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
        val triggerTime =
            midnight.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        logcat {
            "Scheduling midnight period check: triggerDateTime=$midnight triggerMillis=$triggerTime"
        }

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

    fun schedulePeriodEndNotification(
        periodEndDate: LocalDate,
        currentDate: LocalDate = LocalDate.now()
    ) {
        scope.launch {
            val (hour, minute) = getPeriodEndNotificationTime()
            schedulePeriodEndNotification(periodEndDate.plusDays(1), hour, minute, currentDate)
        }
    }

    private fun schedulePeriodEndNotification(
        periodEndDate: LocalDate,
        hour: Int,
        minute: Int,
        currentDate: LocalDate
    ) {
        val now = LocalDateTime.now()
        val triggerDateTime = LocalDateTime.of(periodEndDate, LocalTime.of(hour, minute))
        val triggerMillis =
            triggerDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
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
            currentDate.isAfter(periodEndDate) || !triggerDateTime.isAfter(now) -> {
                logcat {
                    "Scheduling period end notification: triggerDateTime=$triggerDateTime triggerMillis=$triggerMillis mode=immediate_broadcast periodEndDate=$periodEndDate currentDate=$currentDate"
                }
                context.sendBroadcast(alarmIntent)
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() -> {
                logcat {
                    "Scheduling period end notification: triggerDateTime=$triggerDateTime triggerMillis=$triggerMillis mode=exact_allow_while_idle periodEndDate=$periodEndDate currentDate=$currentDate"
                }
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }

            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                logcat {
                    "Scheduling period end notification: triggerDateTime=$triggerDateTime triggerMillis=$triggerMillis mode=exact_allow_while_idle_pre_s periodEndDate=$periodEndDate currentDate=$currentDate"
                }
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }

            else -> {
                logcat {
                    "Scheduling period end notification: triggerDateTime=$triggerDateTime triggerMillis=$triggerMillis mode=inexact_allow_while_idle periodEndDate=$periodEndDate currentDate=$currentDate exactAlarmsAllowed=false"
                }
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        }
    }

    private fun scheduleAllRecurrentExpenseNotifications() {
        scope.launch {
            val transactions = budgetRepository.getTransactions().first()
            val recurrentTransactions = transactions.filter {
                it.isRecurrent && !it.isDeleted && it.recurrentFrequency != null && it.date != null
            }
            logcat { "Scheduling recurrent notifications for ${recurrentTransactions.size} recurrent transactions" }
            recurrentTransactions.forEach { scheduleRecurrentExpenseNotification(it) }
        }
    }

    fun rescheduleRecurrentExpenseNotifications() {
        scheduleAllRecurrentExpenseNotifications()
    }

    fun scheduleRecurrentExpenseNotification(transaction: Transaction) {
        scope.launch {
            scheduleRecurrentExpenseNotification(transaction, getRecurrentNotificationTime())
        }
    }

    fun cancelRecurrentExpenseNotification(transaction: Transaction) {
        val workName = recurrentWorkName(transaction)
        workManager.cancelUniqueWork(workName)
        workManager.cancelUniqueWork(upcomingWorkName(transaction))
        logcat { "Cancelled recurrent notification work: workName=$workName transactionId=${transaction.id}" }
    }

    private suspend fun scheduleRecurrentExpenseNotification(
        transaction: Transaction,
        notificationTime: Pair<Int, Int>,
    ) {
        if (!transaction.isRecurrent || transaction.isDeleted || transaction.recurrentFrequency == null || transaction.date == null) {
            cancelRecurrentExpenseNotification(transaction)
            return
        }

        val stableId = transaction.sourceTransactionId ?: transaction.id
        val paidDates = budgetRepository.getPaidOccurrenceDatesFor(stableId)

        val nextRunDateTime =
            nextOccurrenceDateTime(transaction, LocalDateTime.now(), notificationTime, paidDates) ?: run {
                cancelRecurrentExpenseNotification(transaction)
                logcat { "No future recurrent notification to schedule for transactionId=${transaction.id}" }
                return
            }
        val initialDelay =
            Duration.between(LocalDateTime.now(), nextRunDateTime).toMillis().coerceAtLeast(0L)
        val nextRunMillis =
            nextRunDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val workName = recurrentWorkName(transaction)

        logcat {
            "Scheduling recurrent expense notification: transactionId=${transaction.id} nextRunDateTime=$nextRunDateTime nextRunMillis=$nextRunMillis configuredTime=%02d:%02d initialDelayMs=$initialDelay workName=$workName policy=REPLACE".format(
                notificationTime.first,
                notificationTime.second
            )
        }

        val workRequest = OneTimeWorkRequestBuilder<RecurrentExpenseNotificationWorker>()
            .setInputData(
                workDataOf(RecurrentExpenseNotificationWorker.KEY_TRANSACTION_ID to transaction.id)
            )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .addTag(RecurrentExpenseNotificationWorker.TAG_RECURRENT_NOTIFICATION)
            .build()

        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        scheduleUpcomingReminder(transaction, notificationTime, paidDates)
    }

    private fun scheduleUpcomingReminder(
        transaction: Transaction,
        notificationTime: Pair<Int, Int>,
        paidDates: Set<LocalDate>,
    ) {
        val now = LocalDateTime.now()
        val workName = upcomingWorkName(transaction)
        val reminderDateTime = nextOccurrenceDateTime(
            transaction, now, notificationTime, paidDates, leadDays = UPCOMING_REMINDER_LEAD_DAYS,
        ) ?: run {
            workManager.cancelUniqueWork(workName)
            logcat { "No future upcoming reminder to schedule for transactionId=${transaction.id}" }
            return
        }
        val occurrenceDate = reminderDateTime.toLocalDate().plusDays(UPCOMING_REMINDER_LEAD_DAYS)
        val initialDelay = Duration.between(now, reminderDateTime).toMillis().coerceAtLeast(0L)
        logcat {
            "Scheduling upcoming recurrent reminder: transactionId=${transaction.id} occurrenceDate=$occurrenceDate reminderDateTime=$reminderDateTime initialDelayMs=$initialDelay workName=$workName policy=REPLACE"
        }
        val workRequest = OneTimeWorkRequestBuilder<RecurrentExpenseNotificationWorker>()
            .setInputData(
                workDataOf(
                    RecurrentExpenseNotificationWorker.KEY_TRANSACTION_ID to transaction.id,
                    RecurrentExpenseNotificationWorker.KEY_UPCOMING_OCCURRENCE_EPOCH_DAY to occurrenceDate.toEpochDay(),
                )
            )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(RecurrentExpenseNotificationWorker.TAG_RECURRENT_NOTIFICATION)
            .build()
        workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, workRequest)
    }

    private fun recurrentWorkName(transaction: Transaction): String {
        val stableId = transaction.sourceTransactionId ?: transaction.id
        return "${RecurrentExpenseNotificationWorker.WORK_NAME}_$stableId"
    }

    private fun upcomingWorkName(transaction: Transaction): String {
        val stableId = transaction.sourceTransactionId ?: transaction.id
        return "${RecurrentExpenseNotificationWorker.WORK_NAME}_upcoming_$stableId"
    }

    internal fun nextOccurrenceDateTime(
        transaction: Transaction,
        now: LocalDateTime,
        notificationTime: Pair<Int, Int>,
        paidDates: Set<LocalDate> = emptySet(),
        leadDays: Long = 0L,
    ): LocalDateTime? {
        val startDate = transaction.date?.toLocalDate() ?: return null
        val frequency = transaction.recurrentFrequency ?: return null
        val endDate = transaction.recurrentEndDate?.toLocalDate()
        val billingDay = transaction.subscriptionDay ?: startDate.dayOfMonth

        fun advance(from: LocalDate): LocalDate = when (frequency) {
            RecurrentFrequency.WEEKLY -> from.plusWeeks(1)
            RecurrentFrequency.BIWEEKLY -> from.plusWeeks(2)
            RecurrentFrequency.MONTHLY -> nextMonthlyOccurrence(
                startDate = startDate,
                today = from.plusDays(1),
                subscriptionDay = billingDay,
            )
        }

        var occurrenceDate = when (frequency) {
            RecurrentFrequency.WEEKLY -> nextSteppedOccurrence(startDate, now.toLocalDate(), 7)
            RecurrentFrequency.BIWEEKLY -> nextSteppedOccurrence(startDate, now.toLocalDate(), 14)
            RecurrentFrequency.MONTHLY -> nextMonthlyOccurrence(
                startDate = startDate,
                today = now.toLocalDate(),
                subscriptionDay = billingDay,
            )
        }
        var triggerDateTime = LocalDateTime.of(
            occurrenceDate.minusDays(leadDays),
            LocalTime.of(notificationTime.first, notificationTime.second)
        )

        var iterations = 0
        while ((!triggerDateTime.isAfter(now) || paidDates.contains(occurrenceDate)) &&
            iterations < MAX_OCCURRENCE_LOOKUP_ITERATIONS
        ) {
            occurrenceDate = advance(occurrenceDate)
            triggerDateTime = LocalDateTime.of(
                occurrenceDate.minusDays(leadDays),
                LocalTime.of(notificationTime.first, notificationTime.second)
            )
            iterations++
        }

        if (endDate != null && occurrenceDate.isAfter(endDate)) return null
        if (paidDates.contains(occurrenceDate)) return null
        return triggerDateTime
    }

    internal fun nextSteppedOccurrence(
        startDate: LocalDate,
        today: LocalDate,
        stepDays: Long
    ): LocalDate {
        if (!today.isAfter(startDate)) return startDate
        val daysBetween = ChronoUnit.DAYS.between(startDate, today)
        val steps = (daysBetween + stepDays - 1) / stepDays
        return startDate.plusDays(steps * stepDays)
    }

    internal fun nextMonthlyOccurrence(
        startDate: LocalDate,
        today: LocalDate,
        subscriptionDay: Int,
    ): LocalDate {
        var candidate = today.withDayOfMonth(subscriptionDay.coerceIn(1, today.lengthOfMonth()))
        if (candidate.isBefore(today)) {
            val nextMonth = today.plusMonths(1)
            candidate =
                nextMonth.withDayOfMonth(subscriptionDay.coerceIn(1, nextMonth.lengthOfMonth()))
        }
        return if (candidate.isBefore(startDate)) startDate else candidate
    }

    fun scheduleCreditCutoffReminder() {
        scope.launch {
            try {
                val cutoffDay = budgetRepository.getBudgetSettingsSync()?.creditCardCutoffDay
                if (cutoffDay == null) {
                    workManager.cancelUniqueWork(CREDIT_CUTOFF_WORK_NAME)
                    logcat { "No credit card cutoff day configured, credit cutoff reminder cancelled" }
                    return@launch
                }
                val now = LocalDateTime.now()
                val triggerDateTime =
                    nextCreditCutoffReminderDateTime(cutoffDay, now, getRecurrentNotificationTime())
                val initialDelay = Duration.between(now, triggerDateTime).toMillis().coerceAtLeast(0L)
                logcat {
                    "Scheduling credit cutoff reminder: cutoffDay=$cutoffDay triggerDateTime=$triggerDateTime initialDelayMs=$initialDelay workName=$CREDIT_CUTOFF_WORK_NAME policy=REPLACE"
                }
                val workRequest = OneTimeWorkRequestBuilder<RecurrentExpenseNotificationWorker>()
                    .setInputData(
                        workDataOf(RecurrentExpenseNotificationWorker.KEY_CREDIT_CUTOFF_REMINDER to true)
                    )
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .addTag(RecurrentExpenseNotificationWorker.TAG_RECURRENT_NOTIFICATION)
                    .build()
                workManager.enqueueUniqueWork(CREDIT_CUTOFF_WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest)
            } catch (e: Exception) {
                logcat { "Error scheduling credit cutoff reminder\n${e.asLog()}" }
            }
        }
    }

    internal fun nextCreditCutoffReminderDateTime(
        cutoffDay: Int,
        now: LocalDateTime,
        notificationTime: Pair<Int, Int>,
    ): LocalDateTime {
        val card = CreditCard(cutoffDay = cutoffDay)
        val time = LocalTime.of(notificationTime.first, notificationTime.second)
        return (0L..MAX_CREDIT_CUTOFF_LOOKAHEAD_MONTHS).asSequence()
            .map { monthsAhead ->
                val dueDate = calculatePaymentDueDate(card, now.toLocalDate().plusMonths(monthsAhead))
                LocalDateTime.of(dueDate.minusDays(CREDIT_CUTOFF_REMINDER_DAYS), time)
            }
            .first { it.isAfter(now) }
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
        workManager.cancelAllWorkByTag(RecurrentExpenseNotificationWorker.TAG_RECURRENT_NOTIFICATION)
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
        val settings = settingsRepository.getSettings()
        return settings.notificationHour to settings.notificationMinute
    }

    private suspend fun getRecurrentNotificationTime(): Pair<Int, Int> {
        val settings = settingsRepository.getSettings()
        return settings.recurrentNotificationHour to settings.recurrentNotificationMinute
    }
}
