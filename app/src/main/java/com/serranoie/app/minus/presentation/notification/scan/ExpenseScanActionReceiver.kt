package com.serranoie.app.minus.presentation.notification.scan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.usecase.GetCurrentPeriodIdUseCase
import com.serranoie.app.minus.presentation.notification.NotificationHelper
import com.serranoie.app.minus.presentation.ui.budget.ApplyTransactionResult
import com.serranoie.app.minus.presentation.ui.budget.BudgetTransactionHandler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.asLog
import logcat.logcat
import java.time.LocalDate

class ExpenseScanActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_QUICK_SAVE = "com.serranoie.app.minus.action.SCAN_QUICK_SAVE"
        const val ACTION_UNDO = "com.serranoie.app.minus.action.SCAN_UNDO"
        const val ACTION_QUICK_ADD = "com.serranoie.app.minus.action.SCAN_QUICK_ADD"

        const val EXTRA_AMOUNT = "scan_amount"
        const val EXTRA_SOURCE_LABEL = "scan_source_label"
        const val EXTRA_TRANSACTION_ID = "scan_transaction_id"
        const val EXTRA_NOTIFICATION_ID = "scan_notification_id"

        private const val RECENT_LOOKUP_LIMIT = 10
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ExpenseScanActionEntryPoint {
        fun budgetRepository(): BudgetRepository
        fun budgetTransactionHandler(): BudgetTransactionHandler
        fun getCurrentPeriodIdUseCase(): GetCurrentPeriodIdUseCase
        fun notificationHelper(): NotificationHelper
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != ACTION_QUICK_SAVE && action != ACTION_UNDO) return

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(NonCancellable) {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        ExpenseScanActionEntryPoint::class.java,
                    )
                    when (action) {
                        ACTION_QUICK_SAVE -> quickSave(entryPoint, intent, notificationId)
                        ACTION_UNDO -> undo(entryPoint, context, intent, notificationId)
                    }
                }
            } catch (e: Exception) {
                logcat { "Error handling scan notification action $action\n${e.asLog()}" }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun quickSave(
        entryPoint: ExpenseScanActionEntryPoint,
        intent: Intent,
        notificationId: Int,
    ) {
        val amount = intent.getStringExtra(EXTRA_AMOUNT)?.takeIf { it.isNotBlank() } ?: return
        val sourceLabel = intent.getStringExtra(EXTRA_SOURCE_LABEL).orEmpty()
        val budgetRepository = entryPoint.budgetRepository()
        val budgetSettings = budgetRepository.getBudgetSettingsSync()

        val result = entryPoint.budgetTransactionHandler().applyTransaction(
            input = amount,
            isCalculation = false,
            isRecurrentEnabled = false,
            isCreditEnabled = false,
            comment = "",
            note = sourceLabel,
            budgetSettings = budgetSettings,
            resolveActivePeriodId = { entryPoint.getCurrentPeriodIdUseCase().invoke() },
        )

        val notificationHelper = entryPoint.notificationHelper()
        when (result) {
            is ApplyTransactionResult.Added,
            is ApplyTransactionResult.QueuedForNextPeriod -> {
                val savedId = if (result is ApplyTransactionResult.Added) {
                    findSavedTransactionId(budgetRepository, amount)
                } else {
                    0L
                }
                notificationHelper.showSpendSavedNotification(
                    notificationId = notificationId,
                    amount = amount,
                    currency = budgetSettings?.currencyCode ?: "USD",
                    transactionId = savedId,
                    queuedForNextPeriod = result is ApplyTransactionResult.QueuedForNextPeriod,
                )
            }

            else -> {
                logcat { "Scan quick save rejected: $result" }
                notificationHelper.cancelSpendNotification(notificationId)
            }
        }
    }

    private suspend fun findSavedTransactionId(
        budgetRepository: BudgetRepository,
        amount: String,
    ): Long {
        val savedAmount = amount.toBigDecimalOrNull() ?: return 0L
        return budgetRepository.getRecentTransactions(RECENT_LOOKUP_LIMIT)
            .firstOrNull {
                !it.isRecurrent &&
                    !it.isDeleted &&
                    it.amount.compareTo(savedAmount) == 0 &&
                    it.date?.toLocalDate() == LocalDate.now()
            }
            ?.id
            ?: 0L
    }

    private suspend fun undo(
        entryPoint: ExpenseScanActionEntryPoint,
        context: Context,
        intent: Intent,
        notificationId: Int,
    ) {
        NotificationManagerCompat.from(context).cancel(notificationId)
        val transactionId = intent.getLongExtra(EXTRA_TRANSACTION_ID, 0L)
        if (transactionId <= 0L) return
        val transaction = entryPoint.budgetRepository().getTransactionById(transactionId) ?: return
        entryPoint.budgetTransactionHandler().deleteTransaction(transaction)
    }
}
