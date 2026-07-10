package com.serranoie.app.minus.presentation

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.serranoie.app.minus.data.repository.BUDGET_SPLIT_VIEW_PERIOD_KEY_NAME
import com.serranoie.app.minus.data.repository.CATEGORY_GRID_MODE_KEY_NAME
import com.serranoie.app.minus.data.repository.CATEGORY_PICKER_DIRECT_POPUP_KEY_NAME
import com.serranoie.app.minus.data.repository.CREDIT_QUICK_TOGGLE_FEATURE_KEY_NAME
import com.serranoie.app.minus.data.repository.SAVINGS_GOAL_AMOUNT_KEY_NAME
import com.serranoie.app.minus.data.repository.SAVINGS_GOAL_MONTHS_KEY_NAME
import com.serranoie.app.minus.data.repository.SAVINGS_NEEDS_PCT_KEY_NAME
import com.serranoie.app.minus.data.repository.SAVINGS_PRESET_KEY_NAME
import com.serranoie.app.minus.data.repository.SAVINGS_SAVINGS_PCT_KEY_NAME
import com.serranoie.app.minus.data.repository.SAVINGS_WANTS_PCT_KEY_NAME
import com.serranoie.app.minus.data.repository.CURRENT_PERIOD_ID_KEY_NAME
import com.serranoie.app.minus.data.repository.CURRENT_PERIOD_STARTED_AT_KEY_NAME
import com.serranoie.app.minus.data.repository.DYNAMIC_COLOR_KEY_NAME
import com.serranoie.app.minus.data.repository.EARLY_FINISH_ACTIVE_KEY_NAME
import com.serranoie.app.minus.data.repository.EARLY_FINISH_ACTUAL_DATE_KEY_NAME
import com.serranoie.app.minus.data.repository.EARLY_FINISH_ORIGINAL_END_DATE_KEY_NAME
import com.serranoie.app.minus.data.repository.NOTIFICATION_HOUR_KEY_NAME
import com.serranoie.app.minus.data.repository.NOTIFICATION_MINUTE_KEY_NAME
import com.serranoie.app.minus.data.repository.ONBOARDING_COMPLETED_KEY_NAME
import com.serranoie.app.minus.data.repository.RECURRENT_NOTIFICATION_HOUR_KEY_NAME
import com.serranoie.app.minus.data.repository.RECURRENT_NOTIFICATION_MINUTE_KEY_NAME
import com.serranoie.app.minus.data.repository.RECURRENT_PAYMENTS_VIEW_MODE_KEY_NAME
import com.serranoie.app.minus.data.repository.SETTINGS_DATASTORE_NAME
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.data.repository.THEME_MODE_KEY_NAME
import com.serranoie.app.minus.data.repository.TYPOGRAPHY_MODE_KEY_NAME
import com.serranoie.app.minus.data.wearable.WearableService
import com.serranoie.app.minus.domain.time.MidnightTransitionManager
import com.serranoie.app.minus.navigation.AppNavGraph
import com.serranoie.app.minus.navigation.Screen
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import com.serranoie.app.minus.presentation.permission.PermissionHandler
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.ThemeManager
import com.serranoie.app.minus.presentation.ui.theme.ThemeMode
import com.serranoie.app.minus.presentation.ui.theme.TypographyMode
import com.serranoie.app.minus.presentation.ui.theme.component.RolloverDialog
import com.serranoie.app.minus.presentation.util.CensorManager
import com.serranoie.app.minus.presentation.util.LocalCensorMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import logcat.asLog
import logcat.logcat
import javax.inject.Inject

val Context.settingsDataStore by preferencesDataStore(SETTINGS_DATASTORE_NAME)
var Context.appTheme by mutableStateOf(ThemeMode.SYSTEM)
var Context.appTypography by mutableStateOf(TypographyMode.EXPRESSIVE)
var Context.dynamicColorEnabled by mutableStateOf(false)

val LocalWindowSize = compositionLocalOf { WindowWidthSizeClass.Compact }
val LocalWindowInsets = compositionLocalOf { PaddingValues(0.dp) }

val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey(ONBOARDING_COMPLETED_KEY_NAME)
val BUDGET_END_DATE_KEY = longPreferencesKey("budget_end_date_millis")
val NOTIFICATION_HOUR_KEY = intPreferencesKey(NOTIFICATION_HOUR_KEY_NAME)
val NOTIFICATION_MINUTE_KEY = intPreferencesKey(NOTIFICATION_MINUTE_KEY_NAME)
val RECURRENT_NOTIFICATION_HOUR_KEY = intPreferencesKey(RECURRENT_NOTIFICATION_HOUR_KEY_NAME)
val RECURRENT_NOTIFICATION_MINUTE_KEY = intPreferencesKey(RECURRENT_NOTIFICATION_MINUTE_KEY_NAME)
val THEME_MODE_KEY = stringPreferencesKey(THEME_MODE_KEY_NAME)
val TYPOGRAPHY_MODE_KEY = stringPreferencesKey(TYPOGRAPHY_MODE_KEY_NAME)
val DYNAMIC_COLOR_KEY = booleanPreferencesKey(DYNAMIC_COLOR_KEY_NAME)
val CREDIT_QUICK_TOGGLE_FEATURE_KEY = booleanPreferencesKey(CREDIT_QUICK_TOGGLE_FEATURE_KEY_NAME)
val CATEGORY_PICKER_DIRECT_POPUP_KEY = booleanPreferencesKey(CATEGORY_PICKER_DIRECT_POPUP_KEY_NAME)
val CATEGORY_GRID_MODE_KEY = booleanPreferencesKey(CATEGORY_GRID_MODE_KEY_NAME)
val RECURRENT_PAYMENTS_VIEW_MODE_KEY = stringPreferencesKey(RECURRENT_PAYMENTS_VIEW_MODE_KEY_NAME)
val EARLY_FINISH_ACTIVE_KEY = booleanPreferencesKey(EARLY_FINISH_ACTIVE_KEY_NAME)
val EARLY_FINISH_ACTUAL_DATE_KEY = longPreferencesKey(EARLY_FINISH_ACTUAL_DATE_KEY_NAME)
val EARLY_FINISH_ORIGINAL_END_DATE_KEY = longPreferencesKey(EARLY_FINISH_ORIGINAL_END_DATE_KEY_NAME)
val CURRENT_PERIOD_STARTED_AT_KEY = longPreferencesKey(CURRENT_PERIOD_STARTED_AT_KEY_NAME)
val CURRENT_PERIOD_ID_KEY = longPreferencesKey(CURRENT_PERIOD_ID_KEY_NAME)
val BUDGET_SPLIT_VIEW_PERIOD_KEY = stringPreferencesKey(BUDGET_SPLIT_VIEW_PERIOD_KEY_NAME)
val SAVINGS_PRESET_KEY = stringPreferencesKey(SAVINGS_PRESET_KEY_NAME)
val SAVINGS_NEEDS_PCT_KEY = intPreferencesKey(SAVINGS_NEEDS_PCT_KEY_NAME)
val SAVINGS_WANTS_PCT_KEY = intPreferencesKey(SAVINGS_WANTS_PCT_KEY_NAME)
val SAVINGS_SAVINGS_PCT_KEY = intPreferencesKey(SAVINGS_SAVINGS_PCT_KEY_NAME)
val SAVINGS_GOAL_AMOUNT_KEY = stringPreferencesKey(SAVINGS_GOAL_AMOUNT_KEY_NAME)
val SAVINGS_GOAL_MONTHS_KEY = intPreferencesKey(SAVINGS_GOAL_MONTHS_KEY_NAME)
const val DEFAULT_NOTIFICATION_HOUR = 9
const val DEFAULT_NOTIFICATION_MINUTE = 0
const val DEFAULT_RECURRENT_NOTIFICATION_HOUR = 8
const val DEFAULT_RECURRENT_NOTIFICATION_MINUTE = 0

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val isDone: MutableState<Boolean> = mutableStateOf(false)
    private val isReady: MutableState<Boolean> = mutableStateOf(false)
    private val dataStoreLoaded: MutableState<Boolean> = mutableStateOf(false)
    private val onboardingComplete: MutableState<Boolean> = mutableStateOf(false)
    private val earlyFinishPending: MutableState<Boolean> = mutableStateOf(false)

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var permissionHandler: PermissionHandler

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var censorManager: CensorManager

    @Inject
    lateinit var wearableService: WearableService

    @Inject
    lateinit var midnightTransitionManager: MidnightTransitionManager

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            permissionHandler.onNotificationPermissionResult(isGranted, notificationScheduler)
        }

    private fun checkAndRequestNotificationPermission() {
        permissionHandler.requestNotificationPermissionIfNeeded(
            activity = this,
            launcher = requestNotificationPermissionLauncher,
        )
    }

    override fun onResume() {
        super.onResume()
        censorManager.start()
    }

    override fun onPause() {
        super.onPause()
        censorManager.stop()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val context = this.applicationContext

        WindowCompat.setDecorFitsSystemWindows(window, false)

        installSplashScreen().setKeepOnScreenCondition {
            val keepOn = !dataStoreLoaded.value || !isDone.value
            keepOn
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            try {
                runCatching {
                    val nodeIds = wearableService.getReachableSenderNodeIds()
                    logcat {
                        "wear capability minus_wear_sender reachableNodes=${nodeIds.size} ids=${nodeIds.joinToString()}"
                    }
                }.onFailure {
                    logcat { it.asLog() }
                }

                val userSettings = settingsRepository.getSettings()
                onboardingComplete.value = userSettings.onboardingCompleted
                earlyFinishPending.value = userSettings.earlyFinishActive
                themeManager.applyUserSettings(context, userSettings)

                logcat("ISAAC:Main") {
                    "Initial settings load -> onboarding_completed=${userSettings.onboardingCompleted}, earlyFinishActive=${userSettings.earlyFinishActive}"
                }

                dataStoreLoaded.value = true
                isDone.value = true
            } catch (_: Exception) {
                logcat("ISAAC:Main") { "Initial settings load failed" }
                dataStoreLoaded.value = true
                isDone.value = true
            }

            notificationScheduler.initializeNotifications()
        }

        settingsRepository
            .observeSettings()
            .onEach { settings ->
                val previous = onboardingComplete.value
                logcat("ISAAC:Main") {
                    "Settings observer -> onboarding_completed=${settings.onboardingCompleted} (was $previous), earlyFinishActive=${settings.earlyFinishActive}"
                }
                onboardingComplete.value = settings.onboardingCompleted
                earlyFinishPending.value = settings.earlyFinishActive
                themeManager.applyUserSettings(applicationContext, settings)
            }.launchIn(lifecycleScope)

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    lifecycleScope.launch {
                        midnightTransitionManager.handleAppStart()
                    }
                }
            },
        )

        setContent {
            val activityResultRegistryOwner = LocalActivityResultRegistryOwner.current

            LaunchedEffect(Unit) {
                isReady.value = true
            }

            val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass

            // INFO: Seems like this is not needed anymore since we support tablet layouts.
// 			if (widthSizeClass == WindowWidthSizeClass.Compact) {
// 				lockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
// 			}

            val windowInsets = WindowInsets.systemBars.asPaddingValues()

            if (isReady.value && dataStoreLoaded.value) {
                val dynamicColor = context.dynamicColorEnabled
                val isCensored by censorManager.isCensored.collectAsStateWithLifecycle()

                val startDestination =
                    when {
                        earlyFinishPending.value -> Screen.Analytics.route
                        !onboardingComplete.value -> Screen.Onboarding.route
                        else -> Screen.Main.route
                    }
                logcat("ISAAC:Main") {
                    "Resolved startDestination=$startDestination (earlyFinishPending=${earlyFinishPending.value}, onboardingComplete=${onboardingComplete.value})"
                }

                MinusTheme(dynamicColor = dynamicColor) {
                    CompositionLocalProvider(
                        LocalWindowSize provides widthSizeClass,
                        LocalWindowInsets provides windowInsets,
                        LocalCensorMode provides isCensored,
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                        ) {
                            val navController = rememberNavController()

                            AppNavGraph(
                                activityResultRegistryOwner = activityResultRegistryOwner,
                                startDestination = startDestination,
                                navController = navController,
                                onOnboardingComplete = {
                                    logcat("ISAAC:Main") {
                                        "AppNavGraph.onOnboardingComplete fired -> writing onboarding_completed=true + dismissing midnight transition manager (user has no budget yet)"
                                    }
                                    lifecycleScope.launch {
                                        logcat {
                                            "onOnboardingComplete -> writing onboarding_completed=true"
                                        }
                                        settingsRepository.setOnboardingCompleted(true)
                                        midnightTransitionManager.onBudgetSetupHandled()
                                    }
                                },
                                onRequestNotificationPermission = {
                                    logcat("ISAAC:Main") { "AppNavGraph.onRequestNotificationPermission fired" }
                                    checkAndRequestNotificationPermission()
                                },
                            )

                            val shouldShowMidnightDialog by midnightTransitionManager.shouldShowTransitionDialog
                                .collectAsStateWithLifecycle()
                            val midnightTransitionData by midnightTransitionManager.midnightTransitionData.collectAsStateWithLifecycle()

                            if (shouldShowMidnightDialog && midnightTransitionData != null) {
                                val data = midnightTransitionData!!
                                if (data.shouldNavigateToAnalyticsOnly) {
                                    LaunchedEffect(
                                        data.periodEndDate,
                                        data.remainingAmount,
                                        data.totalBudget,
                                        data.totalSpent,
                                    ) {
                                        midnightTransitionManager.onTransitionDialogConfirmed()
                                        navController.navigate(Screen.Analytics.route) {
                                            popUpTo(Screen.Main.route) { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                } else {
                                    val periodLabel = "${data.periodStartDate.dayOfMonth} ${
                                        data.periodStartDate.month.name.lowercase().take(3)
                                    } - ${data.periodEndDate.dayOfMonth} ${
                                        data.periodEndDate.month.name.lowercase().take(3)
                                    }"
                                    RolloverDialog(
                                        remainingAmount = data.remainingAmount,
                                        currencyCode = data.currencyCode,
                                        periodLabel = periodLabel,
                                        spentAmount = data.totalSpent,
                                        onSplitEqually = {
                                            lifecycleScope.launch {
                                                midnightTransitionManager.rollRemainingSplitEqually()
                                                navController.navigate(Screen.Analytics.route) {
                                                    popUpTo(Screen.Main.route) { inclusive = false }
                                                    launchSingleTop = true
                                                }
                                            }
                                        },
                                        onCarryToNextDay = {
                                            lifecycleScope.launch {
                                                midnightTransitionManager.rollRemainingToFirstDay()
                                                navController.navigate(Screen.Analytics.route) {
                                                    popUpTo(Screen.Main.route) { inclusive = false }
                                                    launchSingleTop = true
                                                }
                                            }
                                        },
                                        onViewAnalytics = {
                                            midnightTransitionManager.onTransitionDialogConfirmed()
                                            navController.navigate(Screen.Analytics.route) {
                                                popUpTo(Screen.Main.route) { inclusive = false }
                                            }
                                        },
                                        onDismiss = {
                                            midnightTransitionManager.onTransitionDialogDismissed()
                                        },
                                    )
                                }
                            }

                            val needsBudgetSetup by midnightTransitionManager.needsBudgetSetup.collectAsStateWithLifecycle()
                            LaunchedEffect(needsBudgetSetup, onboardingComplete.value) {
                                if (needsBudgetSetup && onboardingComplete.value) {
                                    logcat(
                                        "ISAAC:Main",
                                    ) { "needsBudgetSetup detected AND onboardingComplete=true -> navigating to wallet setup" }
                                    midnightTransitionManager.onBudgetSetupHandled()

                                    val prefs = context.settingsDataStore.data.first()
                                    val hasBudget = prefs[BUDGET_END_DATE_KEY] != null
                                    navController.navigate(
                                        Screen.Main.createRoute(
                                            openWallet = true,
                                            forceWalletSetup = !hasBudget,
                                        ),
                                    ) {
                                        popUpTo(Screen.Main.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } else if (needsBudgetSetup && !onboardingComplete.value) {
                                    logcat("ISAAC:Main") {
                                        "needsBudgetSetup detected but onboarding NOT complete -> suppressing wallet setup navigation until onboarding finishes"
                                    }
                                }
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
