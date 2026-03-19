package com.serranoie.app.minus.presentation.home

import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.presentation.budget.BudgetViewModel
import com.serranoie.app.minus.presentation.onboarding.FinishDateSelector
import com.serranoie.app.minus.presentation.onboarding.OnboardingScreen
import com.serranoie.app.minus.presentation.ui.theme.component.BottomSheetWrapper
import com.serranoie.app.minus.presentation.wallet.Wallet
import kotlinx.coroutines.launch

const val WALLET_SHEET = "wallet"
const val ON_BOARDING_SHEET = "onboarding"
const val FINISH_DATE_SHEET = "finishDate"
const val SETTINGS_SHEET = "settings"
const val RECALCULATE_BUDGET_SHEET = "recalculateBudget"

private const val TAG = "BottomSheets"

/**
 * Central registry for all bottom sheets in the app.
 * Each sheet is declared with a unique identifier using BottomSheetWrapper.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheets(
	activityResultRegistryOwner: ActivityResultRegistryOwner?,
	onOpenWallet: () -> Unit = {},
) {
	val coroutineScope = rememberCoroutineScope()

	// Expose onOpenWallet via remember to be used externally
	LaunchedEffect(onOpenWallet) {
		// This callback can be used to trigger the wallet sheet from outside
	}
	val budgetViewModel: BudgetViewModel = hiltViewModel()
	val uiState by budgetViewModel.uiState.collectAsStateWithLifecycle()

	var currentSheet by remember { mutableStateOf<String?>(null) }

	LaunchedEffect(uiState.isFirstLaunch) {
		if (uiState.isFirstLaunch && currentSheet == null) {
			currentSheet = ON_BOARDING_SHEET
		}
	}

	val onboardingState = rememberModalBottomSheetState(
		initialValue = ModalBottomSheetValue.Hidden, confirmValueChange = { true })


	LaunchedEffect(currentSheet) {
		if (currentSheet == ON_BOARDING_SHEET) onboardingState.show()
		else onboardingState.hide()
	}

	BottomSheetWrapper(
		name = ON_BOARDING_SHEET, cancelable = false, state = onboardingState
	) { state ->
		OnboardingScreen(onSetBudget = {
			currentSheet = WALLET_SHEET
		}, onClose = {
			coroutineScope.launch { state.hide() }
		}, onOnboardingComplete = {
			currentSheet = null
			budgetViewModel.markFirstLaunchComplete()
		})
	}

	val walletState = rememberModalBottomSheetState(
		initialValue = ModalBottomSheetValue.Hidden, confirmValueChange = { true })


	LaunchedEffect(currentSheet) {
		if (currentSheet == WALLET_SHEET) {
			walletState.show()
		} else {
			walletState.hide()
		}
	}

	BottomSheetWrapper(
		name = WALLET_SHEET, cancelable = false, state = walletState
	) { state ->
		Wallet(
			forceChange = false,
			activityResultRegistryOwner = activityResultRegistryOwner,
			budgetViewModel = budgetViewModel,
			onClose = {
				budgetViewModel.markFirstLaunchComplete()
				coroutineScope.launch {
					state.hide()
					currentSheet = null
				}
			})
	}

	BottomSheetWrapper(
		name = FINISH_DATE_SHEET,
	) { state ->
		val budgetSettings = uiState.budgetSettings
		FinishDateSelector(
			totalBudget = budgetSettings?.totalBudget ?: java.math.BigDecimal.ZERO,
			currencyCode = budgetSettings?.currencyCode ?: "USD",
			onBackPressed = {
				coroutineScope.launch { state.hide() }
			},
			onApply = { startDate, endDate, period ->
				val newSettings = budgetSettings?.copy(
					startDate = startDate,
					endDate = endDate,
					period = period,
				)
				if (newSettings != null) {
					budgetViewModel.saveBudgetSettings(newSettings)
				}
				coroutineScope.launch { state.hide() }
			})
	}
}
