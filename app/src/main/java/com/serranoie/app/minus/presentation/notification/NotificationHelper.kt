package com.serranoie.app.minus.presentation.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.serranoie.app.minus.MainActivity
import com.serranoie.app.minus.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class to manage notification channels and show notifications.
 * Creates two notification channels:
 * 1. Budget Period End - for notifications when budget period ends
 * 2. Recurrent Expenses - for notifications when recurrent expenses are due
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val TAG = "NotificationHelper"
        const val CHANNEL_PERIOD_END = "budget_period_end"
        const val CHANNEL_RECURRENT = "recurrent_expenses"

        const val NOTIFICATION_ID_PERIOD_END = 1001
        const val NOTIFICATION_ID_RECURRENT = 1002

        const val EXTRA_REMAINING_BUDGET = "remaining_budget"
        const val EXTRA_CURRENCY = "currency"
        const val EXTRA_EXPENSE_AMOUNT = "expense_amount"
        const val EXTRA_EXPENSE_COMMENT = "expense_comment"
        const val EXTRA_EXPENSE_FREQUENCY = "expense_frequency"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Budget Period End Channel
            val periodEndChannel = NotificationChannel(
                CHANNEL_PERIOD_END,
                "Budget Period End",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when your budget period ends with remaining balance"
                enableVibration(true)
            }

            // Recurrent Expenses Channel
            val recurrentChannel = NotificationChannel(
                CHANNEL_RECURRENT,
                "Recurrent Expenses",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when recurrent expenses are due"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(periodEndChannel)
            notificationManager.createNotificationChannel(recurrentChannel)
            Log.d(TAG, "Notification channels created")
        }
    }

    /**
     * Check and log notification permission status.
     */
    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "Notification permission (Android 13+): $granted")
            granted
        } else {
            Log.d(TAG, "Notification permission: granted (pre-Android 13)")
            true
        }
    }

    /**
     * Show notification when budget period ends.
     */
    fun showPeriodEndNotification(remainingBudget: String, currency: String) {
        val hasPermission = checkNotificationPermission()
        if (!hasPermission) {
            Log.w(TAG, "Cannot show notification - permission not granted")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = buildPeriodEndMessage(remainingBudget, currency)

        val notification = NotificationCompat.Builder(context, CHANNEL_PERIOD_END)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Periodo de ahorro finalizado")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_PERIOD_END, notification)
        Log.d(TAG, "Period end notification shown successfully")
    }

    /**
     * Build a user-friendly message for period end notification.
     */
    private fun buildPeriodEndMessage(remainingBudget: String, currency: String): String {
        val amount = remainingBudget.toDoubleOrNull() ?: 0.0
        return if (amount > 0) {
            "El periodo terminó con $$remainingBudget sobrante. Tap para ver detalles."
        } else if (amount < 0) {
            "El periodo se acabó. Gastaste $${kotlin.math.abs(amount)} de más. Ver más detalles."
        } else {
            "El periodo se acabó. No gastaste nada. Ver más detalles."
        }
    }

    /**
     * Show notification when a recurrent expense is due.
     */
    fun showRecurrentExpenseNotification(amount: String, comment: String, frequency: String, currency: String) {
        val hasPermission = checkNotificationPermission()
        if (!hasPermission) {
            Log.w(TAG, "Cannot show notification - permission not granted")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Gasto recurrente"
        val message = if (comment.isNotBlank()) {
            "Tu gasto recurrente de \"$comment\" por \"$$amount\" es hoy."
        } else {
            "Tu gasto recurrente con un total de \"$$amount\" es hoy."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_RECURRENT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_RECURRENT, notification)
    }

    /**
     * Show notification for an upcoming subscription charge.
     * Warns users about subscriptions charging in the next few days within their budget period.
     */
    fun showUpcomingSubscriptionNotification(amount: String, comment: String, daysUntil: Long, currency: String) {
        val hasPermission = checkNotificationPermission()
        if (!hasPermission) {
            Log.w(TAG, "Cannot show notification - permission not granted")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val daysText = when (daysUntil) {
            1L -> "mañana"
            else -> "en $daysUntil días"
        }

        val title = "Suscripción próxima"
        val message = if (comment.isNotBlank()) {
            "Tu suscripción \"$comment\" de \"$$amount\" se cobrará $daysText. Prepárate para el cargo."
        } else {
            "Tienes una suscripción de \"$$amount\" que se cobrará $daysText."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_RECURRENT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        val notificationId = NOTIFICATION_ID_RECURRENT + daysUntil.toInt()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
        Log.d(TAG, "Upcoming subscription notification shown: $message")
    }

    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}