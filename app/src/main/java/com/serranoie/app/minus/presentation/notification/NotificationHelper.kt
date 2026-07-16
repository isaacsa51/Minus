package com.serranoie.app.minus.presentation.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import logcat.logcat
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
    @param:ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_PERIOD_END = "budget_period_end"
        const val CHANNEL_RECURRENT = "recurrent_expenses"
        const val CHANNEL_CREDIT = "credit_expenses"

        const val NOTIFICATION_ID_PERIOD_END = 1001
        const val NOTIFICATION_ID_RECURRENT = 1002
        const val NOTIFICATION_ID_CREDIT = 1003
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val periodEndChannel = NotificationChannel(
            CHANNEL_PERIOD_END,
            context.getString(R.string.notification_channel_period_end_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_period_end_description)
            enableVibration(true)
        }

        val recurrentChannel = NotificationChannel(
            CHANNEL_RECURRENT,
            context.getString(R.string.notification_channel_recurrent_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_recurrent_description)
            enableVibration(true)
        }

        val creditChannel = NotificationChannel(
            CHANNEL_CREDIT,
            context.getString(R.string.notification_channel_credit_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_credit_description)
            enableVibration(true)
        }

        notificationManager.createNotificationChannel(periodEndChannel)
        notificationManager.createNotificationChannel(recurrentChannel)
        notificationManager.createNotificationChannel(creditChannel)
        logcat { "Notification channels created" }
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            logcat { "Notification permission (Android 13+): $granted" }
            granted
        } else {
            logcat { "Notification permission: granted (pre-Android 13)" }
            true
        }
    }

    fun showPeriodEndNotification(remainingBudget: String, currency: String) {
        val hasPermission = checkNotificationPermission()
        if (!hasPermission) {
            logcat { "Cannot show notification - permission not granted" }
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

        val message = buildPeriodEndMessage(remainingBudget)

        val notification = NotificationCompat.Builder(context, CHANNEL_PERIOD_END)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_period_end_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_PERIOD_END, notification)
        logcat { "Period end notification shown successfully" }
    }

    private fun buildPeriodEndMessage(remainingBudget: String): String {
        val amount = remainingBudget.toDoubleOrNull() ?: 0.0
        return if (amount > 0) {
            context.getString(R.string.notification_period_end_message_positive, remainingBudget)
        } else if (amount < 0) {
            context.getString(
                R.string.notification_period_end_message_negative,
                kotlin.math.abs(amount).toString()
            )
        } else {
            context.getString(R.string.notification_period_end_message_neutral)
        }
    }

    fun showRecurrentExpenseNotification(amount: String, comment: String) {
        val hasPermission = checkNotificationPermission()
        if (!hasPermission) {
            logcat { "Cannot show notification - permission not granted" }
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

        val title = context.getString(R.string.notification_recurrent_expense_title)
        val message = if (comment.isNotBlank()) {
            context.getString(
                R.string.notification_recurrent_expense_message_with_comment,
                comment,
                amount
            )
        } else {
            context.getString(
                R.string.notification_recurrent_expense_message_without_comment,
                amount
            )
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

    fun showUpcomingSubscriptionNotification(
        amount: String,
        comment: String,
        daysUntil: Long,
        currency: String
    ) {
        val hasPermission = checkNotificationPermission()
        if (!hasPermission) {
            logcat { "Cannot show notification - permission not granted" }
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
            1L -> context.getString(R.string.notification_tomorrow)
            else -> context.getString(R.string.notification_in_days, daysUntil)
        }

        val title = context.getString(R.string.notification_upcoming_subscription_title)
        val message = if (comment.isNotBlank()) {
            context.getString(
                R.string.notification_upcoming_subscription_message_with_comment,
                comment,
                amount,
                daysText
            )
        } else {
            context.getString(
                R.string.notification_upcoming_subscription_message_without_comment,
                amount,
                daysText
            )
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
        logcat { "Upcoming subscription notification shown: $message" }
    }

    fun showCreditCutoffNotification(
        totalAmount: String,
        cutoffDateText: String,
        currency: String
    ) {
        val hasPermission = checkNotificationPermission()
        if (!hasPermission) {
            logcat { "Cannot show credit notification - permission not granted" }
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            3,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = context.getString(
            R.string.notification_credit_cutoff_message,
            cutoffDateText,
            totalAmount
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CREDIT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_credit_cutoff_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_CREDIT, notification)
    }

    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}