package com.serranoie.app.minus.presentation.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.presentation.budget.BudgetViewModel
import com.serranoie.app.minus.presentation.budget.mvi.BudgetUiIntent
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
	budgetPillHintAnchorModifier: Modifier = Modifier,
	analyticsHintAnchorModifier: Modifier = Modifier,
	modifier: Modifier = Modifier
) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()

	Editor(
		uiState = uiState,
		animState = uiState.animState,
		onInputChange = { _ ->
			// Users can type using the numpad buttons
		},
		onFocus = {
			if (uiState.numpadInput.isNotEmpty() && uiState.animState != AnimState.EDITING) {
				viewModel.processIntent(BudgetUiIntent.SetAnimState(AnimState.EDITING))
			}
		},
		onOpenHistory = onOpenHistory,
		onOpenSettings = onOpenSettings,
		onOpenAnalytics = onOpenAnalytics,
		onOpenWallet = onOpenWallet,
		openWalletOnStart = openWalletOnStart,
		forceWalletSetup = forceWalletSetup,
		onCommentClick = { /* TODO: Open comment dialog */ },
		onBudgetPillClickForTutorial = onBudgetPillClickForTutorial,
		onAnalyticsClickForTutorial = onAnalyticsClickForTutorial,
		onChangePeriod = { newPeriod ->
			uiState.budgetSettings?.let { settings ->
				val updated = settings.copy(period = newPeriod)
				logcat("EditorWithViewModel") { "onChangePeriod -> dispatch UpdateSettings with $updated" }
				viewModel.processIntent(BudgetUiIntent.UpdateSettings(updated))
			}
		},
		onSaveBudget = { newSettings ->
			viewModel.saveBudgetSettings(newSettings)
		},
		onFinishBudgetEarly = {
			viewModel.processIntent(BudgetUiIntent.FinishBudgetEarly)
		},
		onCommentUpdate = { comment ->
			viewModel.processIntent(BudgetUiIntent.CommentUpdated(comment))
		},
		onDeleteTag = { tag ->
			viewModel.processIntent(BudgetUiIntent.DeleteTag(tag))
		},
		onRecurrentToggle = { enabled ->
			viewModel.processIntent(BudgetUiIntent.SetRecurrentEnabled(enabled))
		},
		onDismissRecurrentDialog = {
			viewModel.processIntent(BudgetUiIntent.DismissRecurrentDialog)
		},
		onRecurrentExpenseConfirm = { frequency, endDate, subscriptionDay ->
			viewModel.processIntent(
				BudgetUiIntent.RecurrentExpenseApplied(
					frequency, endDate, subscriptionDay
				)
			)
		},
		budgetPillHintAnchorModifier = budgetPillHintAnchorModifier,
		analyticsHintAnchorModifier = analyticsHintAnchorModifier,
		modifier = modifier,
	)
}
