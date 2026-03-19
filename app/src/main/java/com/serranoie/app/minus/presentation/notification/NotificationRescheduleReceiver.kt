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
import kotlinx.coroutines.launch
class NotificationRescheduleReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationRescheduleReceiverEntryPoint {
        fun budgetRepository(): BudgetRepository
    }
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Rescheduling notifications after system event: ${intent.action}")
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    NotificationRescheduleReceiverEntryPoint::class.java
                )
                val scheduler = NotificationScheduler(
                    context.applicationContext,
                    entryPoint.budgetRepository()
                )
                scheduler.initializeNotifications()
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling notifications", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
    companion object {
        private const val TAG = "NotificationRescheduleReceiver"
    }
}