package com.serranoie.app.minus.navigation

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.net.toUri
import dagger.hilt.android.EntryPointAccessors
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.serranoie.app.minus.DEFAULT_NOTIFICATION_HOUR
import com.serranoie.app.minus.DEFAULT_NOTIFICATION_MINUTE
import com.serranoie.app.minus.EARLY_FINISH_ACTIVE_KEY
import com.serranoie.app.minus.EARLY_FINISH_ACTUAL_DATE_KEY
import com.serranoie.app.minus.EARLY_FINISH_ORIGINAL_END_DATE_KEY
import com.serranoie.app.minus.domain.model.PeriodMappingMode
import com.serranoie.app.minus.NOTIFICATION_HOUR_KEY
import com.serranoie.app.minus.NOTIFICATION_MINUTE_KEY
import com.serranoie.app.minus.appTheme
import com.serranoie.app.minus.dynamicColorEnabled
import com.serranoie.app.minus.presentation.analytics.Analytics
import com.serranoie.app.minus.presentation.analytics.AnalyticsActions
import com.serranoie.app.minus.presentation.analytics.AnalyticsState
import com.serranoie.app.minus.presentation.budget.BudgetViewModel
import com.serranoie.app.minus.presentation.home.MainScreen
import com.serranoie.app.minus.presentation.onboarding.OnboardingScreen
import com.serranoie.app.minus.presentation.settings.CsvTransferEntryPoint
import com.serranoie.app.minus.presentation.settings.Settings
import com.serranoie.app.minus.presentation.tutorial.FIRST_LAUNCH_TUTORIAL_STAGE_KEY
import com.serranoie.app.minus.presentation.tutorial.FirstLaunchTutorialStage
import com.serranoie.app.minus.presentation.tutorial.PERIOD_MAPPING_MODE_KEY
import com.serranoie.app.minus.presentation.tutorial.periodMappingModeFlow
import com.serranoie.app.minus.presentation.wallet.Wallet
import com.serranoie.app.minus.presentation.ui.theme.component.BottomSheetScrollState
import com.serranoie.app.minus.presentation.ui.theme.component.LocalBottomSheetScrollState
import com.serranoie.app.minus.settingsDataStore
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date

/**
 * Returns direction-aware screen transition animations based on navigation direction.
 *
 * Forward navigation (e.g., Onboard→Sorter):
 * - Enter: Slide in from right (+30) + fade in
 * - Exit: Slide out to left (-30) + fade out
 *
 * Backward navigation (e.g., Settings→Review):
 * - Enter: Slide in from left (-30) + fade in
 * - Exit: Slide out to right (+30) + fade out
 */
fun getScreenTransitions(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry
): Pair<EnterTransition, ExitTransition> {
    // Determine navigation direction based on back stack index
    val initialIndex = initialState.destination.id
    val targetIndex = targetState.destination.id
    val isForwardNavigation = targetIndex > initialIndex

    return if (isForwardNavigation) {
        // Forward: New screen slides in from right, old slides out to left
        val enter = slideInHorizontally(
            initialOffsetX = { 30 },
            animationSpec = tween(300)
        ) + fadeIn(
            animationSpec = tween(250, delayMillis = 50)
        )
        val exit = slideOutHorizontally(
            targetOffsetX = { -30 },
            animationSpec = tween(300)
        ) + fadeOut(
            animationSpec = tween(200)
        )
        Pair(enter, exit)
    } else {
        // Backward: New screen slides in from left, old slides out to right
        val enter = slideInHorizontally(
            initialOffsetX = { -30 },
            animationSpec = tween(300)
        ) + fadeIn(
            animationSpec = tween(250, delayMillis = 50)
        )
        val exit = slideOutHorizontally(
            targetOffsetX = { 30 },
            animationSpec = tween(300)
        ) + fadeOut(
            animationSpec = tween(200)
        )
        Pair(enter, exit)
    }
}

/**
 * Root navigation graph for the app.
 *
 * Flow:
 *  - First launch:  Onboarding → Wallet (setup) → Main
 *  - Regular launch: Main (wallet editing is a bottom sheet within Main)
 *  - Period ended:  Analytics → Wallet (new setup) → Main
 *
 * @param navController       Optional external controller; a new one is created by default.
 * @param startDestination    Route to start on – either [Screen.Onboarding], [Screen.Main], or [Screen.Analytics].
 * @param activityResultRegistryOwner Forwarded to screens that need it (Wallet, MainScreen).
 * @param onOnboardingComplete  Called when the user finishes onboarding so the caller can
 *                              persist the "completed" flag.
 */
@Composable
fun AppNavGraph(
    activityResultRegistryOwner: ActivityResultRegistryOwner?,
    startDestination: String,
    onOnboardingComplete: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val tag = "AppNavGraph - ISAAC"
    Log.d(tag, "AppNavGraph created with startDestination: $startDestination")

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            val (enter, _) = getScreenTransitions(initialState, targetState)
            enter
        },
        exitTransition = {
            val (_, exit) = getScreenTransitions(initialState, targetState)
            exit
        },
        popEnterTransition = {
            val (enter, _) = getScreenTransitions(initialState, targetState)
            enter
        },
        popExitTransition = {
            val (_, exit) = getScreenTransitions(initialState, targetState)
            exit
        }
    ) {

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onSetBudget = {
                    Log.d(tag, "onSetBudget triggered - navigating to Wallet (forceChange=true)")
                    navController.navigate(Screen.Wallet.createRoute(forceChange = true))
                },
                onOnboardingComplete = {
                    Log.d(tag, "onOnboardingComplete triggered from OnboardingScreen")
                    onOnboardingComplete()
                },
                onClose = {
                    Log.d(tag, "onClose triggered from OnboardingScreen - navigating to Main")
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Wallet.route,
            arguments = listOf(
                navArgument(Screen.Wallet.ARG_FORCE_CHANGE) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val forceChange = backStackEntry.arguments?.getBoolean(Screen.Wallet.ARG_FORCE_CHANGE) ?: false

            CompositionLocalProvider(
                LocalBottomSheetScrollState provides BottomSheetScrollState(topPadding = 0.dp)
            ) {
                Wallet(
                    forceChange = forceChange,
                    activityResultRegistryOwner = activityResultRegistryOwner,
                    onClose = {
                        Log.d(tag, "Wallet onClose - navigating to Main")
                        navController.navigate(Screen.Main.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onOnboardingComplete = {
                        Log.d(tag, "Wallet onOnboardingComplete - calling parent onOnboardingComplete")
                        onOnboardingComplete()
                    }
                )
            }
        }

        composable(Screen.Analytics.route) {
            val viewModel: BudgetViewModel = hiltViewModel()
            val uiState = viewModel.uiState.collectAsState().value
            val context = androidx.compose.ui.platform.LocalContext.current
            val preferences = context.settingsDataStore.data.collectAsState(initial = emptyPreferences()).value

            val budgetSettings = uiState.budgetSettings
            val transactions = uiState.transactions
            val budgetState = uiState.budgetState

            val startDate = budgetSettings?.startDate?.atStartOfDay()?.let {
                Date.from(it.atZone(ZoneId.systemDefault()).toInstant())
            } ?: Date()

            val plannedFinishDate = budgetSettings?.getPeriodEndDate()?.atStartOfDay()?.let {
                Date.from(it.atZone(ZoneId.systemDefault()).toInstant())
            }

            val earlyFinishActive = preferences[EARLY_FINISH_ACTIVE_KEY] ?: false
            val earlyFinishActualDate = preferences[EARLY_FINISH_ACTUAL_DATE_KEY]?.let {
                Date(it)
            }
            val earlyFinishOriginalEndDate = preferences[EARLY_FINISH_ORIGINAL_END_DATE_KEY]?.let {
                Date(it)
            }

            // Check if period is finished naturally or marked as finished early
            val today = java.time.LocalDate.now()
            val periodFinishedNaturally = budgetSettings?.getPeriodEndDate()?.isBefore(today) ?: false
            val periodFinished = periodFinishedNaturally || earlyFinishActive

            val wholeBudget = budgetSettings?.totalBudget ?: budgetState?.totalBudget ?: BigDecimal.ZERO
            val totalSpentInPeriod = transactions.filter { !it.isDeleted }.sumOf { it.amount }
            val remainingBudget = wholeBudget.subtract(totalSpentInPeriod)
            val plannedPeriodDays = budgetSettings?.getPeriodEndDate()?.let { periodEnd ->
                ChronoUnit.DAYS.between(budgetSettings.startDate, periodEnd).toInt() + 1
            } ?: 0
            val dailyBudget = if (wholeBudget > BigDecimal.ZERO && plannedPeriodDays > 0) {
                wholeBudget.divide(BigDecimal(plannedPeriodDays), 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }
            val extraAffordableDaysFromRemaining = if (
                earlyFinishActive &&
                remainingBudget > BigDecimal.ZERO &&
                dailyBudget > BigDecimal.ZERO
            ) {
                remainingBudget.divide(dailyBudget, 0, RoundingMode.DOWN).toInt().coerceAtLeast(0)
            } else {
                0
            }

            val analyticsState = AnalyticsState(
                periodFinished = periodFinished,
                transactions = transactions,
                spends = transactions,
                wholeBudget = wholeBudget,
                currencyCode = budgetSettings?.currencyCode ?: "USD",
                finishPeriodActualDate = if (earlyFinishActive) earlyFinishActualDate else null,
                startPeriodDate = startDate,
                finishPeriodDate = if (earlyFinishActive) earlyFinishOriginalEndDate else plannedFinishDate,
                extraAffordableDaysFromRemaining = extraAffordableDaysFromRemaining
            )

            Analytics(
                state = analyticsState,
                actions = AnalyticsActions(
                    onCreateNewPeriod = {
                        viewModel.clearEarlyFinishState()
                        navController.navigate(Screen.Wallet.createRoute(forceChange = true))
                    },
                    onClose = {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ),
                activityResultRegistryOwner = activityResultRegistryOwner
            )
        }

        composable(Screen.Main.route) {
            Log.d(tag, "Navigating to Main")
            MainScreen(
                onNavigateToAnalytics = {
                    navController.navigate(Screen.Analytics.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToWallet = {
                    navController.navigate(Screen.Wallet.createRoute(false))
                }
            )
        }

        composable(Screen.Settings.route) {
            Log.d(tag, "Navigating to Settings")
            val context = androidx.compose.ui.platform.LocalContext.current
            val viewModel: BudgetViewModel = hiltViewModel()
            val csvTransferManager = EntryPointAccessors
                .fromApplication(context.applicationContext, CsvTransferEntryPoint::class.java)
                .csvTransferManager()
            val importLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
                onResult = { uri ->
                    uri?.let {
                        runCatching {
                            context.contentResolver.takePersistableUriPermission(
                                it,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }
                        csvTransferManager.enqueueImport(it.toString())
                    }
                }
            )

            val preferences = context.settingsDataStore.data.collectAsState(initial = emptyPreferences()).value
            val currentThemeMode = context.appTheme
            val notificationHour = preferences[NOTIFICATION_HOUR_KEY] ?: DEFAULT_NOTIFICATION_HOUR
            val notificationMinute = preferences[NOTIFICATION_MINUTE_KEY] ?: DEFAULT_NOTIFICATION_MINUTE
            val exactAlarmEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            val periodMappingMode = context.periodMappingModeFlow()
                .collectAsState(initial = PeriodMappingMode.ACTIVE_BUDGET).value

            val currentThemeString = when (currentThemeMode) {
                com.serranoie.app.minus.presentation.ui.theme.ThemeMode.LIGHT -> "Light"
                com.serranoie.app.minus.presentation.ui.theme.ThemeMode.NIGHT -> "Dark"
                else -> "System"
            }
            Settings(
                currentTheme = currentThemeString,
                isMaterialYouEnabled = context.dynamicColorEnabled,
                notificationHour = notificationHour,
                notificationMinute = notificationMinute,
                exactAlarmEnabled = exactAlarmEnabled,
                onThemeChange = { themeMode ->
                    Log.d(tag, "Theme changed to: $themeMode")
                    val newThemeMode = when (themeMode) {
                        "Light" -> com.serranoie.app.minus.presentation.ui.theme.ThemeMode.LIGHT
                        "Dark" -> com.serranoie.app.minus.presentation.ui.theme.ThemeMode.NIGHT
                        else -> com.serranoie.app.minus.presentation.ui.theme.ThemeMode.SYSTEM
                    }
                    context.appTheme = newThemeMode
                    kotlinx.coroutines.runBlocking {
                        context.settingsDataStore.edit { prefs ->
                            prefs[com.serranoie.app.minus.THEME_MODE_KEY] = newThemeMode.toString()
                        }
                    }
                },
                onMaterialYouToggle = {
                    Log.d(tag, "Material You toggled")
                    val newValue = !context.dynamicColorEnabled
                    context.dynamicColorEnabled = newValue
                    kotlinx.coroutines.runBlocking {
                        context.settingsDataStore.edit { prefs ->
                            prefs[com.serranoie.app.minus.DYNAMIC_COLOR_KEY] = newValue
                        }
                    }
                },
                onNotificationTimeChange = { hour, minute ->
                    viewModel.updatePeriodEndNotificationTime(hour, minute)
                },
                onOpenExactAlarmSettings = {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = "package:${context.packageName}".toUri()
                        }
                    } else {
                        Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = "package:${context.packageName}".toUri()
                        }
                    }
                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
                periodMappingMode = periodMappingMode,
                onPeriodMappingModeChange = { mode ->
                    kotlinx.coroutines.runBlocking {
                        context.settingsDataStore.edit { prefs ->
                            prefs[PERIOD_MAPPING_MODE_KEY] = mode.name
                        }
                    }
                },
                onExportCsv = {
                    kotlinx.coroutines.runBlocking {
                        csvTransferManager.exportAndShareCsv()
                    }
                },
                onImportCsv = {
                    importLauncher.launch(arrayOf("text/*", "text/csv", "application/csv"))
                },
                onResetTutorial = {
                    kotlinx.coroutines.runBlocking {
                        context.settingsDataStore.edit { prefs ->
                            prefs[FIRST_LAUNCH_TUTORIAL_STAGE_KEY] = FirstLaunchTutorialStage.TAP_ANY_NUMBER.name
                        }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
