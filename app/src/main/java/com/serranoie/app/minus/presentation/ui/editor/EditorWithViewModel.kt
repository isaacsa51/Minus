package com.serranoie.app.minus.presentation.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.emptyPreferences
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.presentation.CREDIT_QUICK_TOGGLE_FEATURE_KEY
import com.serranoie.app.minus.presentation.settingsDataStore
import com.serranoie.app.minus.presentation.ui.budget.BudgetViewModel
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetEditorIntent
import logcat.logcat

@Composable
fun EditorWithViewModel(
	viewModel: BudgetViewModel = hiltViewModel(),
	onOpenHistory: () -> Unit,
	onOpenSettings: () -> Unit = {},
	onOpenAnalytics: () -> Unit = {},
	onOpenWallet: () -> Unit = {},
	openWalletOnStart: Boolean = false,
	forceWalletSetup: Boolean = false,
	onBudgetPillClickForTutorial: () -> Unit = {},
	onAnalyticsClickForTutorial: () -> Unit = {},
	showAnalyticsButton: Boolean = true,
	showSettingsButton: Boolean = true,
	budgetPillHintAnchorModifier: Modifier = Modifier,
	analyticsHintAnchorModifier: Modifier = Modifier,
	modifier: Modifier = Modifier
) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val context = LocalContext.current
	val preferences by context.settingsDataStore.data.collectAsStateWithLifecycle(initialValue = emptyPreferences())
	val showCreditQuickToggleFeature = preferences[CREDIT_QUICK_TOGGLE_FEATURE_KEY] ?: false

	var showBottomSheet by remember { mutableStateOf(false) }
	var selectedViewPeriod by remember { mutableStateOf(uiState.budgetSettings?.period ?: BudgetPeriod.DAILY) }

	if (openWalletOnStart) {
		showBottomSheet = true
	}

	Editor(
		uiState = uiState,
		animState = uiState.animState,
		onInputChange = { _ ->
			// Users can type using the numpad buttons
		},
		onFocus = {
			if (uiState.numpadInput.isNotEmpty() && uiState.animState != AnimState.EDITING) {
				viewModel.processIntent(BudgetEditorIntent.SetAnimState(AnimState.EDITING))
			}
		},
		onOpenHistory = onOpenHistory,
		onOpenSettings = onOpenSettings,
		onOpenAnalytics = onOpenAnalytics,
		onOpenWallet = onOpenWallet,
		openWalletOnStart = false,
		showBudgetPeriodSheet = showBottomSheet,
		forceBudgetPeriodSheetSetup = forceWalletSetup,
		selectedViewPeriod = selectedViewPeriod,
		onShowBudgetPeriodSheet = { showBottomSheet = true },
		onHideBudgetPeriodSheet = { showBottomSheet = false },
		onPeriodSelected = { newPeriod ->
			selectedViewPeriod = newPeriod
			uiState.budgetSettings?.let { settings ->
				val updated = settings.copy(period = newPeriod)
				logcat("EditorWithViewModel") { "onPeriodSelected -> dispatch UpdateSettings with $updated" }
				viewModel.processIntent(BudgetEditorIntent.UpdateSettings(updated))
			}
		},
		onCommentClick = { /* TODO: Open comment dialog */ },
		onBudgetPillClickForTutorial = onBudgetPillClickForTutorial,
		onAnalyticsClickForTutorial = onAnalyticsClickForTutorial,
		showAnalyticsButton = showAnalyticsButton,
		showSettingsButton = showSettingsButton,
		onSaveBudget = { newSettings ->
			viewModel.saveBudgetSettings(
				newSettings,
				forceNewPeriodBoundary = forceWalletSetup
			)
		},
		onFinishBudgetEarly = {
			viewModel.processIntent(BudgetEditorIntent.FinishBudgetEarly)
		},
		onCommentUpdate = { comment ->
			viewModel.processIntent(BudgetEditorIntent.CommentUpdated(comment))
		},
		onDeleteTag = { tag ->
			viewModel.processIntent(BudgetEditorIntent.DeleteTag(tag))
		},
		onRecurrentToggle = { enabled ->
			viewModel.processIntent(BudgetEditorIntent.SetRecurrentEnabled(enabled))
		},
		onCreditToggle = { enabled ->
			viewModel.processIntent(BudgetEditorIntent.SetCreditEnabled(enabled))
		},
		showCreditQuickToggleFeature = showCreditQuickToggleFeature,
		onDismissRecurrentDialog = {
			viewModel.processIntent(BudgetEditorIntent.DismissRecurrentDialog)
		},
		onDismissCreditCutoffDialog = {
			viewModel.processIntent(BudgetEditorIntent.DismissCreditCutoffDialog)
		},
		onCreditCutoffConfirm = { cutoffDay ->
			viewModel.processIntent(BudgetEditorIntent.CreditCutoffDayConfirmed(cutoffDay))
		},
		onRecurrentExpenseConfirm = { frequency, endDate, subscriptionDay ->
			viewModel.processIntent(
				BudgetEditorIntent.RecurrentExpenseApplied(
					frequency, endDate, subscriptionDay
				)
			)
		},
		budgetPillHintAnchorModifier = budgetPillHintAnchorModifier,
		analyticsHintAnchorModifier = analyticsHintAnchorModifier,
		modifier = modifier,
	)
}
