package com.serranoie.app.minus

import android.Manifest
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.serranoie.app.minus.navigation.AppNavGraph
import com.serranoie.app.minus.navigation.Screen
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.ThemeMode
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import com.serranoie.app.minus.presentation.ui.theme.syncTheme
import com.serranoie.app.minus.presentation.tutorial.FIRST_LAUNCH_TUTORIAL_STAGE_KEY
import com.serranoie.app.minus.presentation.tutorial.FirstLaunchTutorialStage
import com.serranoie.app.minus.presentation.ui.theme.component.tooltip.HintTipProvider
import com.serranoie.app.minus.presentation.util.lockScreenOrientation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

val Context.settingsDataStore by preferencesDataStore("settings")
var Context.appTheme by mutableStateOf(ThemeMode.SYSTEM)
var Context.dynamicColorEnabled by mutableStateOf(false)
var Context.systemLocale: Locale? by mutableStateOf(null)
var Context.errorForReport: String? by mutableStateOf(null)

val LocalWindowSize = compositionLocalOf { WindowWidthSizeClass.Compact }
val LocalWindowInsets = compositionLocalOf { PaddingValues(0.dp) }

val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
val BUDGET_END_DATE_KEY = longPreferencesKey("budget_end_date_millis")
val NOTIFICATION_HOUR_KEY = intPreferencesKey("notification_hour")
val NOTIFICATION_MINUTE_KEY = intPreferencesKey("notification_minute")
val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color_enabled")
val EARLY_FINISH_ACTIVE_KEY = booleanPreferencesKey("early_finish_active")
val EARLY_FINISH_ACTUAL_DATE_KEY = longPreferencesKey("early_finish_actual_date_millis")
val EARLY_FINISH_ORIGINAL_END_DATE_KEY = longPreferencesKey("early_finish_original_end_date_millis")
val CURRENT_PERIOD_STARTED_AT_KEY = longPreferencesKey("current_period_started_at_millis")
val CURRENT_PERIOD_ID_KEY = longPreferencesKey("current_period_id")
const val DEFAULT_NOTIFICATION_HOUR = 9
const val DEFAULT_NOTIFICATION_MINUTE = 0

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

	private val tag = "MainActivity - ISAAC"

	private val isDone: MutableState<Boolean> = mutableStateOf(false)
	private val isReady: MutableState<Boolean> = mutableStateOf(false)
	private val dataStoreLoaded: MutableState<Boolean> = mutableStateOf(false)
	private val onboardingComplete: MutableState<Boolean> = mutableStateOf(false)
	private val periodEnded: MutableState<Boolean> = mutableStateOf(false)
	private val earlyFinishPending: MutableState<Boolean> = mutableStateOf(false)

	@javax.inject.Inject
	lateinit var notificationScheduler: NotificationScheduler

	private val requestNotificationPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { isGranted ->
		if (isGranted) {
			notificationScheduler.initializeNotifications()
		}
	}

	private fun checkAndRequestNotificationPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			when {
				ContextCompat.checkSelfPermission(
					this, Manifest.permission.POST_NOTIFICATIONS
				) == PackageManager.PERMISSION_GRANTED -> {
				}

				shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
					requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
				}

				else -> {
					requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
				}
			}
		}
	}

	@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
	override fun onCreate(savedInstanceState: Bundle?) {
		val context = this.applicationContext

		WindowCompat.setDecorFitsSystemWindows(window, false)
		installSplashScreen().setKeepOnScreenCondition {
			val keepOn = !dataStoreLoaded.value || !isDone.value
			keepOn
		}

		lifecycleScope.launch {
			try {
				runCatching {
					val cap = Wearable.getCapabilityClient(this@MainActivity)
						.getCapability("minus_wear_sender", CapabilityClient.FILTER_REACHABLE)
						.await()
					Log.d(tag, "wear capability minus_wear_sender reachableNodes=${cap.nodes.size} ids=${cap.nodes.joinToString { it.id }}")
				}.onFailure {
					Log.e(tag, "wear capability check failed", it)
				}

				val prefs = context.settingsDataStore.data.first()
				onboardingComplete.value = prefs[ONBOARDING_COMPLETED_KEY] ?: false
				val endDateMillis = prefs[BUDGET_END_DATE_KEY]

				if (endDateMillis != null && endDateMillis > 0) {
					val endDate = Instant.ofEpochMilli(endDateMillis).atZone(ZoneId.systemDefault())
						.toLocalDate()
					val today = LocalDate.now()
					periodEnded.value = today.isAfter(endDate)
				}

				earlyFinishPending.value = prefs[EARLY_FINISH_ACTIVE_KEY] ?: false

				val themeModeString = prefs[THEME_MODE_KEY]
				if (themeModeString != null) {
					try {
						context.appTheme = ThemeMode.valueOf(themeModeString)
					} catch (e: IllegalArgumentException) {
						context.appTheme = ThemeMode.SYSTEM
					}
				} else {
					context.appTheme = ThemeMode.SYSTEM
				}

				val dynamicColor = prefs[DYNAMIC_COLOR_KEY] ?: false
				context.dynamicColorEnabled = dynamicColor

				dataStoreLoaded.value = true
				isDone.value = true
			} catch (e: Exception) {
				dataStoreLoaded.value = true
				isDone.value = true
			}

			notificationScheduler.initializeNotifications()
		}

		super.onCreate(savedInstanceState)
		enableEdgeToEdge()

		setContent {
			val localContext = LocalContext.current
			val activityResultRegistryOwner = LocalActivityResultRegistryOwner.current

			LaunchedEffect(Unit) {
				syncTheme(localContext)
				isReady.value = true
			}

			LaunchedEffect(isReady.value) {
				if (isReady.value) {
					checkAndRequestNotificationPermission()
				}
			}

			// Request notification permission after onboarding is complete (fallback)
			LaunchedEffect(onboardingComplete.value) {
				if (onboardingComplete.value) {
				}
			}

			val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass

			if (widthSizeClass == WindowWidthSizeClass.Compact) {
				lockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
			}

			val windowInsets = WindowInsets.systemBars.asPaddingValues()

			if (isReady.value && dataStoreLoaded.value) {
				val dynamicColor = context.dynamicColorEnabled

				MinusTheme(dynamicColor = dynamicColor) {
					CompositionLocalProvider(
						LocalWindowSize provides widthSizeClass,
						LocalWindowInsets provides windowInsets,
					) {
						Surface(
						color = MaterialTheme.colorScheme.background
					) {
							HintTipProvider {
								// Determine start destination based on app state
								val startDestination = when {
								// First: Check if budget period has ended or was finished early
								periodEnded.value || earlyFinishPending.value -> {
									Screen.Analytics.route
								}
								// Second: Check if onboarding is not complete
								!onboardingComplete.value -> {
									Screen.Onboarding.route
								}
								// Otherwise: Go to main screen
								else -> {
									Screen.Main.route
								}
							}

								AppNavGraph(
									activityResultRegistryOwner = activityResultRegistryOwner,
									startDestination = startDestination,
									onOnboardingComplete = {
										lifecycleScope.launch {
											context.settingsDataStore.edit { prefs ->
												prefs[ONBOARDING_COMPLETED_KEY] = true
												prefs[FIRST_LAUNCH_TUTORIAL_STAGE_KEY] = FirstLaunchTutorialStage.TAP_ANY_NUMBER.name
											}
										}
									}
								)
							}
						}
					}
				}

				LaunchedEffect(Unit) {
					isDone.value = true
				}
			}
		}
	}
}
