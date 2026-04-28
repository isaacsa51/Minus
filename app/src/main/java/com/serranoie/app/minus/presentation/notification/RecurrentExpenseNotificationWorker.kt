package com.serranoie.app.minus.presentation.notification

import android.content.Context
import logcat.asLog
import logcat.logcat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
    }
    
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RecurrentExpenseWorkerEntryPoint {
        fun budgetRepository(): BudgetRepository
        fun notificationHelper(): NotificationHelper
    }

    override suspend fun doWork(): Result {
        logcat { "RecurrentExpenseNotificationWorker starting..." }
        
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            RecurrentExpenseWorkerEntryPoint::class.java
        )
        val budgetRepository = entryPoint.budgetRepository()
        val notificationHelper = entryPoint.notificationHelper()

        return try {
            val settings = budgetRepository.getBudgetSettingsSync()

            if (settings == null) {
                logcat { "No budget settings found, skipping notification" }
                return Result.success()
            }

            val transactions = budgetRepository.getTransactions().first()
            val today = LocalDate.now()
            val budgetEndDate = settings.getPeriodEndDate()

            val recurrentTransactions = transactions.filter { transaction ->
                val frequency = transaction.recurrentFrequency
                val date = transaction.date
                transaction.isRecurrent &&
                frequency != null &&
                date != null &&
                isDueToday(transaction, today, frequency)
            }

            val upcomingSubscriptions = transactions.filter { transaction ->
                shouldWarnUpcoming(transaction, today, budgetEndDate)
            }

            logcat { "Found ${recurrentTransactions.size} recurrent expenses due today" }
            logcat { "Found ${upcomingSubscriptions.size} upcoming subscriptions this period" }

            for (transaction in recurrentTransactions) {
                val amount = transaction.amount.toPlainString()
                val comment = transaction.comment
                val frequency = transaction.recurrentFrequency?.name ?: "RECURRENT"

                notificationHelper.showRecurrentExpenseNotification(
                    amount = amount,
                    comment = comment,
                    frequency = frequency,
                    currency = settings.currencyCode
                )

                logcat { "Recurrent expense notification shown for: $amount $comment ($frequency)" }
            }

            // Send notifications for upcoming subscriptions (charging in 1-3 days)
            for (transaction in upcomingSubscriptions) {
                val daysUntilCharge = calculateDaysUntilCharge(transaction, today)
                if (daysUntilCharge != null && daysUntilCharge in 1..3) {
                    notificationHelper.showUpcomingSubscriptionNotification(
                        amount = transaction.amount.toPlainString(),
                        comment = transaction.comment,
                        daysUntil = daysUntilCharge,
                        currency = settings.currencyCode
                    )
                    logcat { "Upcoming subscription notification: ${transaction.comment} in $daysUntilCharge days" }
                }
            }

            Result.success()

        } catch (e: Exception) {
            logcat { "Error in RecurrentExpenseNotificationWorker\n${e.asLog()}" }
            Result.failure()
        }
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
                daysBetween > 0 && daysBetween % 7 == 0
            }
            RecurrentFrequency.BIWEEKLY -> {
                val daysBetween = ChronoUnit.DAYS.between(startDate, today).toInt()
                daysBetween > 0 && daysBetween % 14 == 0
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

    private fun shouldWarnUpcoming(transaction: Transaction, today: LocalDate, budgetEndDate: LocalDate): Boolean {
        if (!transaction.isRecurrent) return false
        if (transaction.recurrentFrequency != RecurrentFrequency.MONTHLY) return false
        
        val subscriptionDay = transaction.subscriptionDay ?: transaction.date?.toLocalDate()?.dayOfMonth ?: return false
        
        var nextChargeDate = today.withDayOfMonth(subscriptionDay)
        if (nextChargeDate.isBefore(today) || nextChargeDate.isEqual(today)) {
            nextChargeDate = nextChargeDate.plusMonths(1)
        }
        
        return !nextChargeDate.isAfter(budgetEndDate) &&
               ChronoUnit.DAYS.between(today, nextChargeDate) in 1..3
    }

    private fun calculateDaysUntilCharge(transaction: Transaction, today: LocalDate): Long? {
        if (transaction.recurrentFrequency != RecurrentFrequency.MONTHLY) return null
        
        val subscriptionDay = transaction.subscriptionDay ?: transaction.date?.toLocalDate()?.dayOfMonth ?: return null
        
        var nextChargeDate = today.withDayOfMonth(subscriptionDay)
        if (nextChargeDate.isBefore(today) || nextChargeDate.isEqual(today)) {
            nextChargeDate = nextChargeDate.plusMonths(1)
        }
        
        return ChronoUnit.DAYS.between(today, nextChargeDate)
    }
}
