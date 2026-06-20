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
import javax.inject.Inject
import javax.inject.Singleton

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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        when {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED -> Unit

            activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            else -> launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun onNotificationPermissionResult(
        isGranted: Boolean,
        notificationScheduler: NotificationScheduler,
    ) {
        if (isGranted) {
            notificationScheduler.initializeNotifications()
        }
    }
}
