package com.serranoie.app.minus.navigation

import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.preferredFrameRate
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.serranoie.app.minus.presentation.ui.analytics.AnalyticsScreen
import com.serranoie.app.minus.presentation.ui.changelog.ChangelogHistoryScreen
import com.serranoie.app.minus.presentation.ui.changelog.ChangelogHistoryViewModel
import com.serranoie.app.minus.presentation.ui.home.MainScreen
import com.serranoie.app.minus.presentation.ui.onboarding.OnboardingScreen
import com.serranoie.app.minus.presentation.ui.settings.SettingsScreen
import com.serranoie.app.minus.presentation.ui.settings.SettingsViewModel
import com.serranoie.app.minus.presentation.ui.settings.appearance.AppearanceOptionsScreen
import com.serranoie.app.minus.presentation.ui.settings.bugreport.BugReportScreen
import com.serranoie.app.minus.presentation.ui.settings.features.CsvSyncGuideScreen
import com.serranoie.app.minus.presentation.ui.settings.features.FeatureLabScreen
import com.serranoie.app.minus.presentation.ui.settings.features.NotificationScanScreen
import com.serranoie.app.minus.presentation.ui.settings.features.NotificationScanViewModel
import com.serranoie.app.minus.presentation.ui.subscriptions.SubscriptionsScreen
import logcat.logcat

private const val TAG = "ISAAC:AppNavGraph"

@OptIn(ExperimentalComposeUiApi::class)
private fun NavGraphBuilder.screen(
    route: String,
    popDirection: PopDirection,
    animated: Boolean,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) = composable(route = route, arguments = arguments) { entry ->
    val scope = this
    val reveal = transition.animateFloat(
        transitionSpec = { tween(BackMotionTokens.TransitionDurationMillis) },
        label = "reveal",
    ) { state -> if (state == EnterExitState.Visible) 1f else 0f }

    val pageModifier = if (animated) {
        Modifier
            .predictiveBackPage(
                gesture = rememberBackGesture(),
                transition = transition,
                dimAlpha = { if (popDirection.isPop) revealDim(reveal.value) else 0f },
            )
            .then(
                if (transition.currentState != transition.targetState) {
                    Modifier.preferredFrameRate(FrameRateCategory.High)
                } else {
                    Modifier
                },
            )
    } else {
        Modifier
    }

    Box(modifier = Modifier.fillMaxSize().then(pageModifier)) {
        with(scope) { content(entry) }
    }
}

@Composable
fun AppNavGraph(
    activityResultRegistryOwner: ActivityResultRegistryOwner?,
    startDestination: String,
    onOnboardingComplete: () -> Unit,
    onRequestNotificationPermission: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    val tag = TAG
    logcat(tag) { "Created with startDestination: $startDestination" }

    val enteringOffsetPx = with(LocalDensity.current) {
        BackMotionTokens.EnteringStartOffset.roundToPx()
    }
    val animatorScale = rememberAnimatorDurationScale()
    val animated = animatorScale > 0f
    val transitionDurationMillis = (BackMotionTokens.TransitionDurationMillis * (if (animatorScale <= 0f) 1f else animatorScale)).toInt()
    val popDirection = remember { PopDirection() }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            popDirection.record(pop = false)
            if (animated) screenEnter(transitionDurationMillis) else EnterTransition.None
        },
        exitTransition = { if (animated) screenExit(transitionDurationMillis) else ExitTransition.None },
        popEnterTransition = {
            popDirection.record(pop = true)
            if (animated) screenPopEnter(enteringOffsetPx, transitionDurationMillis) else EnterTransition.None
        },
        popExitTransition = { if (animated) screenPopExit(transitionDurationMillis) else ExitTransition.None },
    ) {
        screen(Screen.Onboarding.route, popDirection, animated) {
            OnboardingScreen(
                onOnboardingCompleted = {
                    onOnboardingComplete()
                    navController.navigate(
                        Screen.Main.createRoute(openWallet = true, forceWalletSetup = true),
                    ) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
                },
            )
        }

        screen(Screen.Analytics.route, popDirection, animated) {
            AnalyticsScreen(
                activityResultRegistryOwner = activityResultRegistryOwner,
                isRootDestination = startDestination == Screen.Analytics.route,
                onNavigateToMainWithWallet = {
                    navController.navigate(
                        Screen.Main.createRoute(openWallet = true, forceWalletSetup = true),
                    ) { popUpTo(0) { inclusive = true } }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSubscriptions = {
                    navController.navigate(Screen.Subscriptions.route)
                },
            )
        }

        screen(Screen.Subscriptions.route, popDirection, animated) {
            SubscriptionsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        screen(
            popDirection = popDirection,
            animated = animated,
            route = Screen.Main.route,
            arguments = listOf(
                navArgument(Screen.Main.ARG_OPEN_WALLET) {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument(Screen.Main.ARG_FORCE_WALLET_SETUP) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { backStackEntry ->
            logcat(tag) { "Navigating to Main" }

            val openWallet =
                backStackEntry.arguments?.getBoolean(Screen.Main.ARG_OPEN_WALLET) ?: false
            val forceWalletSetup =
                backStackEntry.arguments?.getBoolean(Screen.Main.ARG_FORCE_WALLET_SETUP) ?: false

            LaunchedEffect(backStackEntry.id) {
                if (openWallet || forceWalletSetup) {
                    backStackEntry.arguments?.putBoolean(Screen.Main.ARG_OPEN_WALLET, false)
                    backStackEntry.arguments?.putBoolean(Screen.Main.ARG_FORCE_WALLET_SETUP, false)
                }
            }

            MainScreen(
                openWalletOnStart = openWallet,
                onNavigateToAnalytics = {
                    navController.navigate(Screen.Analytics.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onRequestNotificationPermission = onRequestNotificationPermission,
            )
        }

        screen(Screen.Settings.route, popDirection, animated) {
            logcat(tag) { "Navigating to Settings" }

            SettingsScreen(
                onNavigateToBugReport = {
                    navController.navigate(Screen.BugReport.route)
                },
                onNavigateToChangelog = {
                    navController.navigate(Screen.Changelog.route)
                },
                onNavigateToAppearance = {
                    navController.navigate(Screen.Appearance.route)
                },
                onNavigateToFeatureLab = {
                    navController.navigate(Screen.FeatureLab.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        screen(Screen.FeatureLab.route, popDirection, animated) {
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            FeatureLabScreen(
                state = uiState,
                onCreditQuickToggle = viewModel::onCreditQuickToggleFeatureToggle,
                onShowPastTransactionsToggle = viewModel::onShowPastTransactionsToggle,
                onCategoryPickerDirectPopupToggle = viewModel::onCategoryPickerDirectPopupFeatureToggle,
                onCategoryGridModeToggle = viewModel::onCategoryGridModeToggle,
                onExtraNoteToggle = viewModel::onExtraNoteToggle,
                onReserveUpcomingChargesToggle = viewModel::onReserveUpcomingChargesToggle,
                onNewCategoryTagToggle = viewModel::onNewCategoryTagToggle,
                onNavigateToCsvSyncGuide = {
                    navController.navigate(Screen.CsvSyncGuide.route)
                },
                onNavigateToNotificationScan = {
                    navController.navigate(Screen.NotificationScan.route)
                },
                onBack = { navController.popBackStack() },
            )
        }

        screen(Screen.NotificationScan.route, popDirection, animated) {
            val viewModel: NotificationScanViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val lifecycleOwner = LocalLifecycleOwner.current

            LaunchedEffect(lifecycleOwner) {
                lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    viewModel.refreshAccess()
                }
            }

            NotificationScanScreen(
                state = uiState,
                onEnabledToggle = viewModel::onEnabledToggle,
                onAppToggle = viewModel::onAppToggle,
                onQueryChange = viewModel::onQueryChange,
                onOpenAccessSettings = viewModel::openAccessSettings,
                onBack = { navController.popBackStack() },
            )
        }

        screen(Screen.CsvSyncGuide.route, popDirection, animated) {
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            CsvSyncGuideScreen(
                syncFolderName = uiState.syncFolderName,
                syncStatus = uiState.syncStatus,
                onSyncFolderResult = viewModel::onSyncFolderResult,
                onBack = { navController.popBackStack() },
            )
        }

        screen(Screen.Appearance.route, popDirection, animated) {
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            AppearanceOptionsScreen(
                state = uiState,
                onThemeChange = viewModel::onThemeChange,
                onTypographyChange = viewModel::onTypographyChange,
                onContrastChange = viewModel::onContrastChange,
                onColorSchemeChange = viewModel::onColorSchemeChange,
                onLanguageChange = viewModel::onLanguageChange,
                onMaterialYouToggle = viewModel::onMaterialYouToggle,
                onRoundedFontToggle = viewModel::onRoundedFontToggle,
                onAmoledToggle = viewModel::onAmoledToggle,
                onBack = { navController.popBackStack() }
            )
        }

        screen(Screen.Changelog.route, popDirection, animated) {
            logcat(tag) { "Navigating to Changelog" }

            val viewModel = hiltViewModel<ChangelogHistoryViewModel>()
            val releases by viewModel.releases.collectAsStateWithLifecycle()

            ChangelogHistoryScreen(
                releases = releases,
                onBack = { navController.popBackStack() },
            )
        }

        screen(Screen.BugReport.route, popDirection, animated) {
            BugReportScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
    }
}
