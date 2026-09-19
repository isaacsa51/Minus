package com.serranoie.app.minus.presentation.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.RecurrentOccurrenceStatus
import com.serranoie.app.minus.domain.usecase.GetCurrentPeriodIdUseCase
import com.serranoie.app.minus.presentation.ui.budget.BudgetTransactionHandler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import logcat.asLog
import logcat.logcat
import java.time.LocalDate
import java.time.LocalTime

class RecurrentNotificationActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_MARK_PAID = "com.serranoie.app.minus.action.RECURRENT_MARK_PAID"
        const val ACTION_SKIP = "com.serranoie.app.minus.action.RECURRENT_SKIP"
        const val EXTRA_TRANSACTION_ID = "transaction_id"
        const val EXTRA_OCCURRENCE_EPOCH_DAY = "occurrence_epoch_day"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RecurrentNotificationActionEntryPoint {
        fun budgetRepository(): BudgetRepository
        fun budgetTransactionHandler(): BudgetTransactionHandler
        fun notificationScheduler(): NotificationScheduler
        fun getCurrentPeriodIdUseCase(): GetCurrentPeriodIdUseCase
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != ACTION_MARK_PAID && action != ACTION_SKIP) return
        val transactionId = intent.getLongExtra(EXTRA_TRANSACTION_ID, 0L)
        val epochDay = intent.getLongExtra(EXTRA_OCCURRENCE_EPOCH_DAY, Long.MIN_VALUE)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, NotificationHelper.NOTIFICATION_ID_RECURRENT)
        if (transactionId <= 0L || epochDay == Long.MIN_VALUE) return
        val occurrenceDate = LocalDate.ofEpochDay(epochDay)

        NotificationManagerCompat.from(context).cancel(notificationId)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    RecurrentNotificationActionEntryPoint::class.java,
                )
                val budgetRepository = entryPoint.budgetRepository()
                val template = budgetRepository.getTransactionById(transactionId)
                if (template == null || template.isDeleted || !template.isRecurrent) {
                    logcat { "Recurrent notification action ignored; transaction missing or not recurrent: id=$transactionId" }
                    return@launch
                }
                when (action) {
                    ACTION_MARK_PAID -> {
                        val activePeriodId = entryPoint.getCurrentPeriodIdUseCase().invoke()
                        val occurrence = template.copy(
                            date = occurrenceDate.atTime(template.date?.toLocalTime() ?: LocalTime.NOON),
                        )
                        entryPoint.budgetTransactionHandler()
                            .markRecurrentOccurrencePaid(occurrence, activePeriodId)
                            .getOrThrow()
                        logcat { "Recurrent occurrence marked paid from notification: id=$transactionId date=$occurrenceDate" }
                    }

                    ACTION_SKIP -> {
                        budgetRepository.markRecurrentOccurrencePaid(
                            transactionId,
                            occurrenceDate,
                            RecurrentOccurrenceStatus.SKIPPED,
                        )
                        entryPoint.notificationScheduler().scheduleRecurrentExpenseNotification(template)
                        logcat { "Recurrent occurrence skipped from notification: id=$transactionId date=$occurrenceDate" }
                    }
                }
            } catch (e: Exception) {
                logcat { "Error handling recurrent notification action $action\n${e.asLog()}" }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
