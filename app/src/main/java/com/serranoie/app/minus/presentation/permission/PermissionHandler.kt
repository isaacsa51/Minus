package com.serranoie.app.minus.presentation.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionHandler @Inject constructor() {

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
