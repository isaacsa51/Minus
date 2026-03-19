package com.serranoie.app.minus.presentation.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.presentation.budget.BudgetUiEvent
import com.serranoie.app.minus.presentation.budget.BudgetViewModel
import java.time.LocalDate

@Composable
fun EditorWithViewModel(
	viewModel: BudgetViewModel = hiltViewModel(),
	onOpenHistory: () -> Unit,
	onOpenSettings: () -> Unit = {},
	onOpenAnalytics: () -> Unit = {},
	onOpenWallet: () -> Unit = {},
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
		onFocus = {
			if (uiState.numpadInput.isNotEmpty() && uiState.animState != AnimState.EDITING) {
				viewModel.onEvent(BudgetUiEvent.OnSetAnimState(AnimState.EDITING))
			}
		},
		onOpenHistory = onOpenHistory,
		onOpenSettings = onOpenSettings,
		onOpenAnalytics = onOpenAnalytics,
		onOpenWallet = onOpenWallet,
		onCommentClick = { /* TODO: Open comment dialog */ },
		onBudgetPillClickForTutorial = onBudgetPillClickForTutorial,
		onAnalyticsClickForTutorial = onAnalyticsClickForTutorial,
		onChangePeriod = { newPeriod ->
			uiState.budgetSettings?.let { settings ->
				viewModel.onEvent(
					BudgetUiEvent.OnUpdateSettings(settings.copy(period = newPeriod))
				)
			}
		},
		onSaveBudget = { newSettings ->
			viewModel.saveBudgetSettings(newSettings)
		},
		onFinishBudgetEarly = {
			viewModel.onEvent(BudgetUiEvent.OnFinishBudgetEarly)
		},
		onCommentUpdate = { comment ->
			viewModel.onEvent(BudgetUiEvent.OnCommentUpdate(comment))
		},
		onRecurrentToggle = { enabled ->
			viewModel.onEvent(BudgetUiEvent.OnSetRecurrentEnabled(enabled))
		},
		onDismissRecurrentDialog = {
			viewModel.onEvent(BudgetUiEvent.OnDismissRecurrentDialog)
		},
		onRecurrentExpenseConfirm = { frequency, endDate, subscriptionDay ->
			viewModel.onEvent(BudgetUiEvent.OnRecurrentExpenseApply(frequency, endDate, subscriptionDay))
		},
		budgetPillHintAnchorModifier = budgetPillHintAnchorModifier,
		analyticsHintAnchorModifier = analyticsHintAnchorModifier,
		modifier = modifier,
	)
}
