package com.serranoie.app.minus.presentation.notification.scan

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.serranoie.app.minus.R
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.notification.ExpenseNotificationParser
import com.serranoie.app.minus.presentation.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import logcat.asLog
import logcat.logcat
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class ExpenseNotificationListener : NotificationListenerService() {

    companion object {
        private const val DEDUP_WINDOW_MS = 60_000L
        private const val RATE_WINDOW_MS = 60 * 60_000L
        private const val MAX_PER_RATE_WINDOW = 10

        fun requestRebindIfGranted(context: Context) {
            val component = ComponentName(context, ExpenseNotificationListener::class.java)
            runCatching { requestRebind(component) }
                .onFailure { logcat { "Could not request listener rebind: ${it.asLog()}" } }
        }
    }

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var budgetRepository: BudgetRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val seen = ArrayDeque<Pair<String, Long>>()

    @Volatile
    private var connectedAt = Long.MAX_VALUE

    @Volatile
    private var enabled = false

    @Volatile
    private var watchedPackages: Set<String> = emptySet()

    private var settingsJob: Job? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        connectedAt = System.currentTimeMillis()
        settingsJob?.cancel()
        settingsJob = scope.launch {
            settingsRepository.observeSettings().collect { settings ->
                enabled = settings.notificationScanEnabled
                watchedPackages = settings.notificationScanPackages
                if (!settings.notificationScanEnabled) requestUnbind()
            }
        }
        logcat { "Expense notification listener connected" }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        connectedAt = Long.MAX_VALUE
        settingsJob?.cancel()
        settingsJob = null
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        if (sbn.postTime < connectedAt) return
        if (!enabled || sbn.packageName !in watchedPackages) return

        val flags = sbn.notification?.flags ?: return
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) return

        val text = readText(sbn)
        if (text.isBlank()) return

        scope.launch {
            try {
                val currencyCode = budgetRepository.getBudgetSettingsSync()?.currencyCode ?: "USD"
                val amount = ExpenseNotificationParser.parse(
                    text = text,
                    currencyCode = currencyCode,
                    denyWords = getString(R.string.notification_scan_deny_words).split(","),
                ) ?: return@launch

                // Checked before admit() so a dropped notification does not burn this spend's
                // dedup/rate budget: nothing is counted that cannot be shown.
                if (!notificationHelper.canPostNotifications()) {
                    logcat { "Skipping scanned spend: cannot post notifications" }
                    return@launch
                }

                if (!admit(sbn.packageName, amount)) return@launch

                notificationHelper.showSpendDetectedNotification(
                    amount = amount,
                    currency = currencyCode,
                    sourceLabel = appLabel(sbn.packageName),
                )
            } catch (e: Exception) {
                logcat { "Error handling scanned notification\n${e.asLog()}" }
            }
        }
    }

    private fun readText(sbn: StatusBarNotification): String {
        val extras = sbn.notification?.extras ?: return ""
        return listOf(
            Notification.EXTRA_TITLE,
            Notification.EXTRA_TEXT,
            Notification.EXTRA_BIG_TEXT,
            Notification.EXTRA_SUB_TEXT,
        ).mapNotNull { key ->
            runCatching { extras.getCharSequence(key)?.toString() }.getOrNull()
        }.filter { it.isNotBlank() }.joinToString(separator = " ")
    }

    private fun admit(sourcePackage: String, amount: BigDecimal): Boolean = synchronized(seen) {
        val now = System.currentTimeMillis()
        while (seen.isNotEmpty() && now - seen.first().second > RATE_WINDOW_MS) {
            seen.removeFirst()
        }
        val key = "$sourcePackage|${amount.toPlainString()}"
        if (seen.any { it.first == key && now - it.second <= DEDUP_WINDOW_MS }) return false
        if (seen.size >= MAX_PER_RATE_WINDOW) return false
        seen.addLast(key to now)
        true
    }

    private fun appLabel(sourcePackage: String): String = runCatching {
        packageManager.getApplicationLabel(
            packageManager.getApplicationInfo(sourcePackage, 0),
        ).toString()
    }.getOrDefault(sourcePackage)
}
