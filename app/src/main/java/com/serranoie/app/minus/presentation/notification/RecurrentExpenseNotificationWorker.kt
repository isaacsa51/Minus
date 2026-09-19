package com.serranoie.app.minus.presentation.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.CreditCard
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.model.calculatePaymentDueDate
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import logcat.asLog
import logcat.logcat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Worker that checks for recurrent expenses that should trigger a new occurrence.
 * This worker runs periodically to check if any recurrent expenses are due today
 * and sends a notification to remind the user.
 * 
 * For monthly subscriptions, uses the specific subscriptionDay (e.g., 15th of each month)
 * rather than calculating from the start date.
 */
class RecurrentExpenseNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "recurrent_expense_notification"
        const val KEY_TRANSACTION_ID = "transaction_id"
        const val TAG_RECURRENT_NOTIFICATION = "recurrent_expense_notification_tag"
        const val KEY_CREDIT_CUTOFF_REMINDER = "credit_cutoff_reminder"
        const val KEY_UPCOMING_OCCURRENCE_EPOCH_DAY = "upcoming_occurrence_epoch_day"
        private const val LAST_RECURRENT_NOTIFICATION_PREFIX = "last_recurrent_notification_"
        private const val LAST_UPCOMING_NOTIFICATION_PREFIX = "last_upcoming_notification_"
        private const val LAST_CREDIT_CUTOFF_REMINDER_KEY = "last_credit_cutoff_reminder"
    }
    
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RecurrentExpenseWorkerEntryPoint {
        fun budgetRepository(): BudgetRepository
        fun settingsRepository(): SettingsRepository
        fun notificationHelper(): NotificationHelper
        fun notificationScheduler(): NotificationScheduler
    }

    override suspend fun doWork(): Result {
        logcat { "RecurrentExpenseNotificationWorker starting..." }
        
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            RecurrentExpenseWorkerEntryPoint::class.java
        )
        val budgetRepository = entryPoint.budgetRepository()
        val settingsRepository = entryPoint.settingsRepository()
        val notificationHelper = entryPoint.notificationHelper()
        val notificationScheduler = entryPoint.notificationScheduler()

        return try {
            val settings = budgetRepository.getBudgetSettingsSync()

            if (settings == null) {
                logcat { "No budget settings found, skipping notification" }
                return Result.success()
            }

            val today = LocalDate.now()
            val transactionId = inputData.getLong(KEY_TRANSACTION_ID, 0L)
            if (transactionId > 0L) {
                val transaction = budgetRepository.getTransactionById(transactionId)
                if (transaction == null || transaction.isDeleted || !transaction.isRecurrent) {
                    logcat { "Recurrent notification skipped; transaction missing, deleted, or not recurrent: transactionId=$transactionId" }
                    return Result.success()
                }

                val upcomingEpochDay = inputData.getLong(KEY_UPCOMING_OCCURRENCE_EPOCH_DAY, Long.MIN_VALUE)
                if (upcomingEpochDay != Long.MIN_VALUE) {
                    notifyUpcomingOccurrence(
                        transaction = transaction,
                        occurrenceDate = LocalDate.ofEpochDay(upcomingEpochDay),
                        settings = settings,
                        today = today,
                        notificationHelper = notificationHelper,
                        settingsRepository = settingsRepository,
                        budgetRepository = budgetRepository,
                    )
                } else {
                    notifyRecurrentTransactionIfDue(
                        transaction = transaction,
                        settings = settings,
                        today = today,
                        notificationHelper = notificationHelper,
                        settingsRepository = settingsRepository,
                        budgetRepository = budgetRepository,
                    )
                }
                notificationScheduler.scheduleRecurrentExpenseNotification(transaction)
                return Result.success()
            }

            val transactions = budgetRepository.getTransactions().first()
            maybeSendCreditCutoffReminder(
                settings = settings,
                transactions = transactions,
                today = today,
                notificationHelper = notificationHelper,
                settingsRepository = settingsRepository,
            )
            if (inputData.getBoolean(KEY_CREDIT_CUTOFF_REMINDER, false)) {
                notificationScheduler.scheduleCreditCutoffReminder()
            } else {
                logcat { "Legacy recurrent scan completed without sending app-open notifications" }
            }
            Result.success()

        } catch (e: Exception) {
            logcat { "Error in RecurrentExpenseNotificationWorker\n${e.asLog()}" }
            Result.failure()
        }
    }
    private suspend fun maybeSendCreditCutoffReminder(
        settings: BudgetSettings,
        transactions: List<Transaction>,
        today: LocalDate,
        notificationHelper: NotificationHelper,
        settingsRepository: SettingsRepository,
    ) {
        val cutoffDay = settings.creditCardCutoffDay ?: return
        val card = CreditCard(cutoffDay = cutoffDay)
        val dueDate = calculatePaymentDueDate(card, today)

        val daysUntilDueDate = ChronoUnit.DAYS.between(today, dueDate)
        if (daysUntilDueDate !in 0..NotificationScheduler.CREDIT_CUTOFF_REMINDER_DAYS) {
            logcat { "Credit cutoff reminder skipped; dueDate=$dueDate is $daysUntilDueDate days away" }
            return
        }
        if (settingsRepository.getString(LAST_CREDIT_CUTOFF_REMINDER_KEY) == dueDate.toString()) {
            logcat { "Credit cutoff reminder already sent for dueDate=$dueDate" }
            return
        }

        val currentMonthCutoff = runCatching { today.withDayOfMonth(cutoffDay) }.getOrElse {
            today.withDayOfMonth(today.lengthOfMonth())
        }
        val actualCutoffDate = if (today.isAfter(currentMonthCutoff)) {
            currentMonthCutoff
        } else {
            currentMonthCutoff.minusMonths(1)
        }

        val creditTotal = transactions
            .filter { tx ->
                tx.isCredit &&
                    !tx.isDeleted &&
                    !tx.isCreditPaid &&
                    tx.date != null &&
                    !tx.date.toLocalDate().isAfter(actualCutoffDate)
            }
            .sumOf { it.amount }

        if (creditTotal <= java.math.BigDecimal.ZERO) return

        val formatter = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())
        notificationHelper.showCreditCutoffNotification(
            totalAmount = creditTotal.toPlainString(),
            dueDateText = dueDate.format(formatter),
            currency = settings.currencyCode
        )
        settingsRepository.setString(LAST_CREDIT_CUTOFF_REMINDER_KEY, dueDate.toString())
        logcat { "Credit cutoff reminder shown for dueDate=$dueDate total=$creditTotal" }
    }

    private suspend fun notifyRecurrentTransactionIfDue(
        transaction: Transaction,
        settings: BudgetSettings,
        today: LocalDate,
        notificationHelper: NotificationHelper,
        settingsRepository: SettingsRepository,
        budgetRepository: BudgetRepository,
    ) {
        val frequency = transaction.recurrentFrequency ?: return
        if (!isDueToday(transaction, today, frequency)) {
            logcat { "Recurrent notification worker fired but transaction is not due today: transactionId=${transaction.id} today=$today" }
            return
        }

        val stableId = transaction.sourceTransactionId ?: transaction.id
        val paidDates = budgetRepository.getPaidOccurrenceDatesFor(stableId)
        if (paidDates.contains(today)) {
            logcat { "Recurrent notification worker fired but today's occurrence is already marked paid: transactionId=${transaction.id} today=$today" }
            return
        }

        val dedupeKey = recurrentNotificationDedupeKey(transaction, today)
        val lastNotified = settingsRepository.getString(dedupeKey)
        if (lastNotified == today.toString()) {
            logcat { "Skipping duplicate recurrent notification: transactionId=${transaction.id} date=$today" }
            return
        }

        notificationHelper.showRecurrentExpenseNotification(
            amount = transaction.amount.toPlainString(),
            comment = transaction.comment,
            currency = settings.currencyCode,
            frequency = frequency,
            transactionId = stableId,
            occurrenceDate = today,
        )
        settingsRepository.setString(dedupeKey, today.toString())
        logcat { "Recurrent expense notification shown for transactionId=${transaction.id} date=$today" }
    }

    private fun recurrentNotificationDedupeKey(transaction: Transaction, date: LocalDate) =
        "$LAST_RECURRENT_NOTIFICATION_PREFIX${transaction.id}_${date}"

    private suspend fun notifyUpcomingOccurrence(
        transaction: Transaction,
        occurrenceDate: LocalDate,
        settings: BudgetSettings,
        today: LocalDate,
        notificationHelper: NotificationHelper,
        settingsRepository: SettingsRepository,
        budgetRepository: BudgetRepository,
    ) {
        val daysUntil = ChronoUnit.DAYS.between(today, occurrenceDate)
        if (daysUntil <= 0L) {
            logcat { "Upcoming reminder skipped; occurrence is not ahead anymore: transactionId=${transaction.id} occurrenceDate=$occurrenceDate today=$today" }
            return
        }

        val stableId = transaction.sourceTransactionId ?: transaction.id
        if (budgetRepository.getPaidOccurrenceDatesFor(stableId).contains(occurrenceDate)) {
            logcat { "Upcoming reminder skipped; occurrence already resolved: transactionId=${transaction.id} occurrenceDate=$occurrenceDate" }
            return
        }

        val dedupeKey = "$LAST_UPCOMING_NOTIFICATION_PREFIX${transaction.id}_$occurrenceDate"
        if (settingsRepository.getString(dedupeKey) != null) {
            logcat { "Skipping duplicate upcoming reminder: transactionId=${transaction.id} occurrenceDate=$occurrenceDate" }
            return
        }

        notificationHelper.showUpcomingSubscriptionNotification(
            amount = transaction.amount.toPlainString(),
            comment = transaction.comment,
            daysUntil = daysUntil,
            currency = settings.currencyCode,
            transactionId = stableId,
            occurrenceDate = occurrenceDate,
        )
        settingsRepository.setString(dedupeKey, today.toString())
        logcat { "Upcoming recurrent reminder shown for transactionId=${transaction.id} occurrenceDate=$occurrenceDate daysUntil=$daysUntil" }
    }

    private fun isDueToday(transaction: Transaction, today: LocalDate, frequency: RecurrentFrequency): Boolean {
        val startDate = transaction.date?.toLocalDate() ?: return false
        
        val endDate = transaction.recurrentEndDate?.toLocalDate()
        if (endDate != null && today.isAfter(endDate)) {
            return false
        }
        
        if (today.isBefore(startDate)) {
            return false
        }

        return when (frequency) {
            RecurrentFrequency.WEEKLY -> {
                val daysBetween = ChronoUnit.DAYS.between(startDate, today).toInt()
                daysBetween >= 0 && daysBetween % 7 == 0
            }
            RecurrentFrequency.BIWEEKLY -> {
                val daysBetween = ChronoUnit.DAYS.between(startDate, today).toInt()
                daysBetween >= 0 && daysBetween % 14 == 0
            }
            RecurrentFrequency.MONTHLY -> {
                val billingDay = transaction.subscriptionDay ?: startDate.dayOfMonth
                val todayDay = today.dayOfMonth
                
                if (todayDay != billingDay) {
                    return false
                }
                
                // For monthly subscriptions, we should notify on the billing day 
                // as long as we're within the subscription period (startDate to endDate)
                // This handles both the first billing and subsequent billings
                true
            }
        }
    }

}
