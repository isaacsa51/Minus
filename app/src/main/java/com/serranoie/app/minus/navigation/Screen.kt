package com.serranoie.app.minus.navigation

/**
 * All navigable destinations in the app.
 * Each Screen has a unique [route] string used by the NavHost.
 */
sealed class Screen(val route: String) {

    /** First-launch welcome & explanation screen. */
    data object Onboarding : Screen("onboarding")

    /**
     * Budget setup screen.
     * [forceChange] = true means the user is doing initial setup (no back navigation available).
     */
    data object Wallet : Screen("wallet/{forceChange}") {
        fun createRoute(forceChange: Boolean = false) = "wallet/$forceChange"
        const val ARG_FORCE_CHANGE = "forceChange"
    }

    /** Main app screen (editor + history). */
    data object Main : Screen("main")

    /** Period summary/analytics screen shown when budget period ends. */
    data object Analytics : Screen("analytics")

    /** Settings screen for app preferences. */
    data object Settings : Screen("settings")
}
