package com.serranoie.app.minus.presentation.ui.budget

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.presentation.ui.budget.mvi.BudgetUiEffect
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetNumpadIntent
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditMode
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditStage
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditorState
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.Numpad

@Composable
fun NumpadWithViewModel(
    viewModel: BudgetViewModel = hiltViewModel(),
    numberHintAnchorModifier: Modifier = Modifier,
    applyHintAnchorModifier: Modifier = Modifier,
    onAnyNumberTapped: (() -> Unit)? = null,
    onApplyTapped: (() -> Unit)? = null,
    onShowSnackbar: ((String) -> Unit)? = null
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BudgetUiEffect.ShowMessage -> {
                    onShowSnackbar?.invoke(effect.message)
                }
                else -> { /* Ignore other effects */ }
            }
        }
    }

    Numpad(
        modifier = Modifier,
        editorState = EditorState(
            mode = EditMode.ADD,
            rawSpentValue = uiState.value.numpadInput,
            stage = if (uiState.value.numpadInput.isNotEmpty()) EditStage.EDIT_SPENT else EditStage.IDLE,
            currentSpent = uiState.value.numpadInput,
            currentComment = uiState.value.currentComment,
            editedTransaction = null
        ),
        isCalculation = uiState.value.isCalculation,
        onNumberInput = { digit ->
            viewModel.processIntent(BudgetNumpadIntent.NumberTapped(digit.toString()))
        },
        onDotInput = { 
            viewModel.processIntent(BudgetNumpadIntent.DotTapped)
        },
        onEqualsInput = {
            viewModel.processIntent(BudgetNumpadIntent.EqualsTapped)
        },
        onBackspace = {
            viewModel.processIntent(BudgetNumpadIntent.BackspaceTapped)
        },
        onBackspaceLongPress = {
            viewModel.processIntent(BudgetNumpadIntent.ResetInputTapped)
        },
        onApply = {
            Log.d("NumpadWithViewModel", "Apply button pressed, processing ApplyTapped intent")
            viewModel.processIntent(BudgetNumpadIntent.ApplyTapped)
        },
        onDelete = { },
        onCalculationModeChanged = { isEnabled ->
            viewModel.processIntent(BudgetNumpadIntent.SetCalculationMode(isEnabled))
        },
        onOperatorInput = { operator ->
            viewModel.processIntent(BudgetNumpadIntent.OperatorTapped(operator))
        },
        onDragProgressChanged = { progress ->
            viewModel.processIntent(BudgetNumpadIntent.SetDragProgress(progress))
        },
        onToggleDebug = null,
        onShowSnackbar = onShowSnackbar,
        onActivateTutorial = null,
        onTestNotifications = {
            viewModel.processIntent(BudgetNumpadIntent.TriggerTestNotifications)
        },
        numberHintAnchorModifier = numberHintAnchorModifier,
        applyHintAnchorModifier = applyHintAnchorModifier,
        onNumberPressedForTutorial = onAnyNumberTapped,
        onApplyPressedForTutorial = onApplyTapped
    )
}