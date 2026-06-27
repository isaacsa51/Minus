package com.serranoie.app.minus.presentation.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import logcat.logcat
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ISAAC:Permissions"

@Singleton
class PermissionHandler @Inject constructor() {

    fun requestAttachmentPickerPermissionIfNeeded(
        launcher: ActivityResultLauncher<Array<String>>,
    ) {
        launcher.launch(arrayOf("image/*", "video/*"))
    }

    fun onAttachmentPickerResult(
        context: Context,
        uris: List<Uri>,
        onAttachmentsSelected: (List<Uri>) -> Unit,
    ) {
        if (uris.isEmpty()) return

        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
        onAttachmentsSelected(uris)
    }

    fun requestNotificationPermissionIfNeeded(
        activity: ComponentActivity,
        launcher: ActivityResultLauncher<String>,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            logcat(TAG) { "requestNotificationPermissionIfNeeded: pre-Tiramisu, no-op" }
            return
        }

        val alreadyGranted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        when {
            alreadyGranted -> logcat(TAG) { "requestNotificationPermissionIfNeeded: already granted, no-op" }

            activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                logcat(TAG) { "requestNotificationPermissionIfNeeded: rationale path -> launching" }
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            else -> {
                logcat(TAG) { "requestNotificationPermissionIfNeeded: first-time path -> launching" }
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun onNotificationPermissionResult(
        isGranted: Boolean,
        notificationScheduler: NotificationScheduler,
    ) {
        logcat(TAG) { "onNotificationPermissionResult: isGranted=$isGranted" }
        if (isGranted) {
            notificationScheduler.initializeNotifications()
        }
    }
}
