package com.serranoie.app.minus.navigation

sealed class Screen(val route: String) {

    data object Onboarding : Screen("onboarding")

    data object Main : Screen("main?openWallet={openWallet}&forceWalletSetup={forceWalletSetup}") {
        fun createRoute(openWallet: Boolean = false, forceWalletSetup: Boolean = false) =
            "main?openWallet=$openWallet&forceWalletSetup=$forceWalletSetup"

        const val ARG_OPEN_WALLET = "openWallet"
        const val ARG_FORCE_WALLET_SETUP = "forceWalletSetup"
    }

    data object Analytics : Screen("analytics")

    data object Settings : Screen("settings")

    data object BugReport : Screen("bug_report")

    data object Changelog : Screen("changelog")

    data object Appearance : Screen("appearance")
}
