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
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.presentation.MainActivity
import com.serranoie.app.minus.presentation.notification.scan.ExpenseScanActionReceiver
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import logcat.logcat
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_PERIOD_END = "budget_period_end"
        const val CHANNEL_RECURRENT = "recurrent_expenses"
        const val CHANNEL_CREDIT = "credit_expenses"
        const val CHANNEL_SPEND_DETECTED = "spend_detected"

        const val NOTIFICATION_ID_PERIOD_END = 1001
        const val NOTIFICATION_ID_RECURRENT = 1002
        const val NOTIFICATION_ID_CREDIT = 1003

        private const val SPEND_SAVED_TIMEOUT_MS = 15_000L
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
        val spendDetectedChannel = NotificationChannel(
            CHANNEL_SPEND_DETECTED,
            context.getString(R.string.notification_channel_spend_detected_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_spend_detected_description)
            enableVibration(true)
        }

        notificationManager.createNotificationChannel(creditChannel)
        notificationManager.createNotificationChannel(spendDetectedChannel)
        logcat { "Notification channels created" }
    }

    /**
     * Whether this app may post notifications. Callers that detect something worth notifying about
     * should check this first, so nothing is consumed on the way to a notification that Android
     * would silently drop.
     */
    fun canPostNotifications(): Boolean {
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
        val hasPermission = canPostNotifications()
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

        val formattedAmount = formatAmount(remainingBudget, currency)
        val message = buildPeriodEndMessage(remainingBudget, formattedAmount)

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

    private fun buildPeriodEndMessage(remainingBudget: String, formattedAmount: String): String {
        val amount = remainingBudget.toDoubleOrNull() ?: 0.0
        return if (amount > 0) {
            context.getString(R.string.notification_period_end_message_positive, formattedAmount)
        } else if (amount < 0) {
            context.getString(
                R.string.notification_period_end_message_negative,
                formattedAmount
            )
        } else {
            context.getString(R.string.notification_period_end_message_neutral)
        }
    }

    fun recurrentNotificationId(transactionId: Long): Int = "recurrent_$transactionId".hashCode()

    private fun addRecurrentActions(
        builder: NotificationCompat.Builder,
        transactionId: Long,
        occurrenceDate: LocalDate,
        notificationId: Int,
    ) {
        builder
            .addAction(
                0,
                context.getString(R.string.mark_as_paid),
                recurrentActionIntent(
                    RecurrentNotificationActionReceiver.ACTION_MARK_PAID, transactionId, occurrenceDate, notificationId,
                ),
            )
            .addAction(
                0,
                context.getString(R.string.subscriptions_item_skip_short),
                recurrentActionIntent(
                    RecurrentNotificationActionReceiver.ACTION_SKIP, transactionId, occurrenceDate, notificationId,
                ),
            )
    }

    private fun recurrentActionIntent(
        action: String,
        transactionId: Long,
        occurrenceDate: LocalDate,
        notificationId: Int,
    ): PendingIntent {
        val intent = Intent(context, RecurrentNotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(RecurrentNotificationActionReceiver.EXTRA_TRANSACTION_ID, transactionId)
            putExtra(RecurrentNotificationActionReceiver.EXTRA_OCCURRENCE_EPOCH_DAY, occurrenceDate.toEpochDay())
            putExtra(RecurrentNotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getBroadcast(
            context,
            "$action$transactionId${occurrenceDate.toEpochDay()}$notificationId".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun formatAmount(amount: String, currency: String): String {
        val decimalValue = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
        return symbolOnlyCurrencyFormat(currency).format(decimalValue)
    }

    fun showRecurrentExpenseNotification(
        amount: String,
        comment: String,
        currency: String,
        frequency: RecurrentFrequency,
        transactionId: Long? = null,
        occurrenceDate: LocalDate? = null,
    ) {
        val hasPermission = canPostNotifications()
        if (!hasPermission) {
            logcat { "Cannot show notification - permission not granted" }
            return
        }
        val notificationId = transactionId?.let { recurrentNotificationId(it) } ?: NOTIFICATION_ID_RECURRENT

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = formatAmount(amount, currency)
        val name = comment.ifBlank { context.getString(R.string.upcoming_recurrent_unnamed_expense) }
        val title = context.getString(R.string.notification_recurrent_due_today_title, name)
        val message = context.getString(
            when (frequency) {
                RecurrentFrequency.WEEKLY -> R.string.notification_recurrent_charged_weekly
                RecurrentFrequency.BIWEEKLY -> R.string.notification_recurrent_charged_biweekly
                RecurrentFrequency.MONTHLY -> R.string.notification_recurrent_charged_monthly
            },
            formattedAmount,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_RECURRENT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        if (transactionId != null && occurrenceDate != null) {
            addRecurrentActions(builder, transactionId, occurrenceDate, notificationId)
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    fun showUpcomingSubscriptionNotification(
        amount: String,
        comment: String,
        daysUntil: Long,
        currency: String,
        transactionId: Long? = null,
        occurrenceDate: LocalDate? = null,
    ) {
        val hasPermission = canPostNotifications()
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

        val formattedAmount = formatAmount(amount, currency)
        val title = context.getString(R.string.notification_upcoming_subscription_title)
        val message = if (comment.isNotBlank()) {
            context.getString(
                R.string.notification_upcoming_subscription_message_with_comment,
                comment,
                formattedAmount,
                daysText
            )
        } else {
            context.getString(
                R.string.notification_upcoming_subscription_message_without_comment,
                formattedAmount,
                daysText
            )
        }

        val notificationId = transactionId?.let { "upcoming_$it".hashCode() }
            ?: (NOTIFICATION_ID_RECURRENT + daysUntil.toInt())
        val builder = NotificationCompat.Builder(context, CHANNEL_RECURRENT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        if (transactionId != null && occurrenceDate != null) {
            addRecurrentActions(builder, transactionId, occurrenceDate, notificationId)
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        logcat { "Upcoming subscription notification shown: $message" }
    }

    fun showCreditCutoffNotification(
        totalAmount: String,
        dueDateText: String,
        currency: String
    ) {
        val hasPermission = canPostNotifications()
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

        val formattedAmount = formatAmount(totalAmount, currency)
        val message = context.getString(
            R.string.notification_credit_cutoff_message,
            dueDateText,
            formattedAmount
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

    fun spendDetectedNotificationId(sourceLabel: String, amount: String): Int =
        "spend_${sourceLabel}_$amount".hashCode()

    fun showSpendDetectedNotification(
        amount: BigDecimal,
        currency: String,
        sourceLabel: String,
    ) {
        if (!canPostNotifications()) return

        val plainAmount = amount.toPlainString()
        val notificationId = spendDetectedNotificationId(sourceLabel, plainAmount)
        val formattedAmount = formatAmount(plainAmount, currency)
        val message = context.getString(R.string.notification_scan_detected_message, formattedAmount)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = ExpenseScanActionReceiver.ACTION_QUICK_ADD
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(ExpenseScanActionReceiver.EXTRA_AMOUNT, plainAmount)
            putExtra(ExpenseScanActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val quickSaveIntent = Intent(context, ExpenseScanActionReceiver::class.java).apply {
            action = ExpenseScanActionReceiver.ACTION_QUICK_SAVE
            putExtra(ExpenseScanActionReceiver.EXTRA_AMOUNT, plainAmount)
            putExtra(ExpenseScanActionReceiver.EXTRA_SOURCE_LABEL, sourceLabel)
            putExtra(ExpenseScanActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val quickSavePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            quickSaveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SPEND_DETECTED)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(sourceLabel)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, context.getString(R.string.notification_scan_action_minus_it), openPendingIntent)
            .addAction(0, context.getString(R.string.notification_scan_action_quick_save), quickSavePendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
        logcat { "Spend detected notification shown for $sourceLabel" }
    }

    fun showSpendSavedNotification(
        notificationId: Int,
        amount: String,
        currency: String,
        transactionId: Long,
        queuedForNextPeriod: Boolean,
    ) {
        if (!canPostNotifications()) {
            cancelSpendNotification(notificationId)
            return
        }

        val formattedAmount = formatAmount(amount, currency)
        val message = if (queuedForNextPeriod) {
            context.getString(R.string.notification_scan_queued_message, formattedAmount)
        } else {
            context.getString(R.string.notification_scan_saved_message, formattedAmount)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_SPEND_DETECTED)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_scan_saved_title))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(SPEND_SAVED_TIMEOUT_MS)
            .setCategory(NotificationCompat.CATEGORY_STATUS)

        if (transactionId > 0L) {
            val undoIntent = Intent(context, ExpenseScanActionReceiver::class.java).apply {
                action = ExpenseScanActionReceiver.ACTION_UNDO
                putExtra(ExpenseScanActionReceiver.EXTRA_TRANSACTION_ID, transactionId)
                putExtra(ExpenseScanActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            builder.addAction(
                0,
                context.getString(R.string.notification_scan_action_undo),
                PendingIntent.getBroadcast(
                    context,
                    notificationId + 2,
                    undoIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    fun cancelSpendNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
