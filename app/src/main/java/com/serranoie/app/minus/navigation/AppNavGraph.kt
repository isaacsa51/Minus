package com.serranoie.app.minus.navigation

import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
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
import logcat.logcat

private const val TAG = "ISAAC:AppNavGraph"

private fun getScreenTransitions(
    initialState: NavBackStackEntry,
    targetState: NavBackStackEntry,
): Pair<EnterTransition, ExitTransition> {
    val isForward = targetState.destination.id > initialState.destination.id

    val enter = slideInHorizontally(
        initialOffsetX = { if (isForward) 30 else -30 },
        animationSpec = tween(300),
    ) + fadeIn(animationSpec = tween(250, delayMillis = 50))

    val exit = slideOutHorizontally(
        targetOffsetX = { if (isForward) -30 else 30 },
        animationSpec = tween(300),
    ) + fadeOut(animationSpec = tween(200))

    return enter to exit
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

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { getScreenTransitions(initialState, targetState).first },
        exitTransition = { getScreenTransitions(initialState, targetState).second },
        popEnterTransition = { getScreenTransitions(initialState, targetState).first },
        popExitTransition = { getScreenTransitions(initialState, targetState).second },
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingCompleted = {
                    onOnboardingComplete()
                    navController.navigate(
                        Screen.Main.createRoute(openWallet = true, forceWalletSetup = true),
                    ) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
                },
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                activityResultRegistryOwner = activityResultRegistryOwner,
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
            )
        }

        composable(
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

        composable(Screen.Settings.route) {
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
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable(Screen.Appearance.route) {
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
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Changelog.route) {
            logcat(tag) { "Navigating to Changelog" }

            val viewModel = hiltViewModel<ChangelogHistoryViewModel>()
            val releases by viewModel.releases.collectAsStateWithLifecycle()

            ChangelogHistoryScreen(
                releases = releases,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.BugReport.route) {
            BugReportScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
    }
}
