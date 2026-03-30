package com.serranoie.app.minus.presentation.budget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditorState
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditMode
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditStage
import com.serranoie.app.minus.presentation.budget.mvi.BudgetUiIntent
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.Numpad

/**
 * Wrapper composable that connects BudgetViewModel to Numpad.
 * Use this in production code.
 */
@Composable
fun NumpadWithViewModel(
    viewModel: BudgetViewModel = hiltViewModel(),
    numberHintAnchorModifier: Modifier = Modifier,
    applyHintAnchorModifier: Modifier = Modifier,
    onAnyNumberTapped: (() -> Unit)? = null,
    onApplyTapped: (() -> Unit)? = null
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    Numpad(
        editorState = EditorState(
            mode = EditMode.ADD,
            rawSpentValue = uiState.value.numpadInput,
            stage = if (uiState.value.numpadInput.isNotEmpty()) EditStage.EDIT_SPENT else EditStage.IDLE,
            currentSpent = uiState.value.numpadInput
        ),
        onNumberInput = { digit ->
            viewModel.processIntent(BudgetUiIntent.NumberTapped(digit.toString()))
        },
        onDotInput = { 
            viewModel.processIntent(BudgetUiIntent.DotTapped)
        },
        onBackspace = {
            viewModel.processIntent(BudgetUiIntent.BackspaceTapped)
        },
        onBackspaceLongPress = {
            viewModel.processIntent(BudgetUiIntent.ResetInputTapped)
        },
        onApply = {
            viewModel.processIntent(BudgetUiIntent.ApplyTapped)
        },
        onDelete = { },
        onToggleDebug = null,
        onShowSnackbar = null,
        onActivateTutorial = null,
        onTestNotifications = {
            viewModel.processIntent(BudgetUiIntent.TriggerTestNotifications)
        },
        numberHintAnchorModifier = numberHintAnchorModifier,
        applyHintAnchorModifier = applyHintAnchorModifier,
        onNumberPressedForTutorial = onAnyNumberTapped,
        onApplyPressedForTutorial = onApplyTapped
    )
}