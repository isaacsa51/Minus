package com.serranoie.app.minus.presentation.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.presentation.budget.BudgetViewModel
import com.serranoie.app.minus.presentation.budget.mvi.intent.BudgetEditorIntent
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
				viewModel.processIntent(BudgetEditorIntent.SetAnimState(AnimState.EDITING))
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
				viewModel.processIntent(BudgetEditorIntent.UpdateSettings(updated))
			}
		},
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
		onDismissRecurrentDialog = {
			viewModel.processIntent(BudgetEditorIntent.DismissRecurrentDialog)
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
