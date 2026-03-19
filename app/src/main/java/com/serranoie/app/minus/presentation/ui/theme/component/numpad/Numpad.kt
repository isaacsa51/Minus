package com.serranoie.app.minus.presentation.ui.theme.component.numpad

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.util.getFloatDivider
import com.serranoie.app.minus.presentation.util.tryConvertStringToNumber
import com.serranoie.app.minus.presentation.util.join
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.domain.model.Transaction
import java.util.Date

val BUTTON_GAP = 6.dp

enum class KeyboardAction { PUT_NUMBER, SET_DOT, REMOVE_LAST }

/**
 * Edit mode for the numpad/editor
 */
enum class EditMode { ADD, EDIT }

/**
 * Edit stage for tracking the current editor state
 */
enum class EditStage { IDLE, EDIT_SPENT }

/**
 * Simple Transaction data class for the numpad
 */
data class Transaction(
    val id: Long = 0,
    val amount: String,
    val comment: String = "",
    val date: Date = Date()
)

/**
 * Data class to hold editor state needed by the numpad
 */
data class EditorState(
    val mode: EditMode = EditMode.ADD,
    val rawSpentValue: String = "",
    val stage: EditStage = EditStage.IDLE,
    val currentSpent: String = "",
    val currentComment: String = "",
    val editedTransaction: Transaction? = null
)

/**
 * Refactored Numpad composable that uses lambda callbacks instead of ViewModels.
 * Fully previewable without Hilt dependency injection.
 */
@Composable
fun Numpad(
    modifier: Modifier = Modifier,
    editorState: EditorState,
    onNumberInput: (Int) -> Unit = {},
    onDotInput: () -> Unit = {},
    onBackspace: () -> Unit = {},
    onBackspaceLongPress: () -> Unit = {},
    onDelete: () -> Unit = {},
    onApply: () -> Unit = {},
    onToggleDebug: (() -> Unit)? = null,
    onShowSnackbar: ((String) -> Unit)? = null,
    onActivateTutorial: (() -> Unit)? = null,
    onTestNotifications: (() -> Unit)? = null,
    numberHintAnchorModifier: Modifier = Modifier,
    applyHintAnchorModifier: Modifier = Modifier,
    onNumberPressedForTutorial: (() -> Unit)? = null,
    onApplyPressedForTutorial: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    var debugProgress by remember { mutableStateOf(0) }

    Column(
        modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .weight(1F)
        ) {
            for (i in 7..9) {
                NumpadButton(
                    modifier = Modifier
                        .weight(1F)
                        .padding(BUTTON_GAP),
                    type = NumpadButtonType.DEFAULT,
                    text = i.toString(),
                    onClick = {
                        onNumberInput(i)
                        debugProgress = 0
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )
            }
            NumpadButton(
                modifier = Modifier
                    .weight(1F)
                    .padding(BUTTON_GAP),
                type = NumpadButtonType.SECONDARY,
                icon = Icons.AutoMirrored.Rounded.Backspace,
                onClick = {
                    onBackspace()
                    debugProgress = 0
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                onLongClick = {
                    debugProgress = 0
                    onBackspaceLongPress()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
            )
        }
        Row(
            Modifier
                .fillMaxSize()
                .weight(3F)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .weight(3F)
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .weight(1F)
                ) {
                    for (i in 4..6) {
                        NumpadButton(
                            modifier = Modifier
                                .weight(1F)
                                .padding(BUTTON_GAP)
                                .then(if (i == 5) numberHintAnchorModifier else Modifier),
                            type = NumpadButtonType.DEFAULT,
                            text = i.toString(),
                            onClick = {
                                onNumberInput(i)
                                onNumberPressedForTutorial?.invoke()
                                debugProgress = 0
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                    }
                }
                Row(
                    Modifier
                        .fillMaxSize()
                        .weight(1F)
                ) {
                    for (i in 1..3) {
                        NumpadButton(
                            modifier = Modifier
                                .weight(1F)
                                .padding(BUTTON_GAP),
                            type = NumpadButtonType.DEFAULT,
                            text = i.toString(),
                            onClick = {
                                onNumberInput(i)
                                onNumberPressedForTutorial?.invoke()
                                debugProgress = 0
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                    }
                }
                Row(
                    Modifier
                        .fillMaxSize()
                        .weight(1F)
                ) {
                    NumpadButton(
                        modifier = Modifier
                            .weight(2F)
                            .padding(BUTTON_GAP),
                        type = NumpadButtonType.DEFAULT,
                        text = "0",
                        onClick = {
                            onNumberInput(0)
                            onNumberPressedForTutorial?.invoke()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                    )
                    NumpadButton(
                        modifier = Modifier
                            .weight(1F)
                            .padding(BUTTON_GAP),
                        type = NumpadButtonType.DEFAULT,
                        text = getFloatDivider(),
                        onClick = {
                            onDotInput()
                            debugProgress = if (debugProgress == 7) -1 else (debugProgress + 1)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .weight(1F)
            ) {
                val fixedSpent = tryConvertStringToNumber(editorState.rawSpentValue).join(third = false)

                AnimatedContent(
                    label = "Delete or Apply",
                    targetState = (fixedSpent == "0" || fixedSpent == "0." || fixedSpent == "0.0") && editorState.mode === EditMode.EDIT,
                    transitionSpec = {
                        if (targetState && !initialState) {
                            fadeIn(
                                tween(durationMillis = 250)
                            ) togetherWith fadeOut(
                                tween(durationMillis = 250)
                            )
                        } else {
                            fadeIn(
                                tween(durationMillis = 250)
                            ) togetherWith fadeOut(
                                tween(durationMillis = 250)
                            )
                        }.using(
                            SizeTransform(clip = false)
                        )
                    }
                ) { targetIsDelete ->
                    if (targetIsDelete) {
                        NumpadButton(
                            modifier = Modifier
                                .weight(1F)
                                .padding(BUTTON_GAP),
                            type = NumpadButtonType.DELETE,
                            icon = Icons.Default.Delete,
                            onClick = {
                                onDelete()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    } else {
                        NumpadButton(
                            modifier = Modifier
                                .weight(1F)
                                .padding(BUTTON_GAP)
                                .then(applyHintAnchorModifier),
                            type = NumpadButtonType.PRIMARY,
                            icon = Icons.Default.Check,
                            onClick = {
                                if (debugProgress == -1) {
                                    // Hidden debug feature: press 0 eight times then apply to test notifications
                                    onTestNotifications?.invoke()
                                    onShowSnackbar?.invoke("Test notifications triggered!")
                                    debugProgress = 0
                                    return@NumpadButton
                                }

                                debugProgress = 0
                                onApplyPressedForTutorial?.invoke()
                                onApply()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun NumpadPreview() {
	MinusTheme {
	    Numpad(
	        editorState = EditorState(
	            mode = EditMode.ADD,
	            rawSpentValue = "123",
	            stage = EditStage.EDIT_SPENT
	        ),
	        onNumberInput = { Log.d("NumpadPreview", "Number: $it") },
	        onDotInput = { Log.d("NumpadPreview", "Dot pressed") },
	        onBackspace = { Log.d("NumpadPreview", "Backspace") },
	        onBackspaceLongPress = { Log.d("NumpadPreview", "Backspace long press") },
	        onDelete = { Log.d("NumpadPreview", "Delete") },
	        onApply = { Log.d("NumpadPreview", "Apply") },
	    )
	}
}

@Preview
@Composable
fun NumpadPreviewEmpty() {
    MinusTheme {
        Numpad(
            editorState = EditorState(
                mode = EditMode.ADD,
                rawSpentValue = "",
                stage = EditStage.IDLE
            ),
            onNumberInput = { },
            onDotInput = { },
            onBackspace = { },
            onBackspaceLongPress = { },
            onDelete = { },
            onApply = { },
        )
    }
}

@Preview
@Composable
fun NumpadPreviewEditMode() {
    MinusTheme {
        Numpad(
            editorState = EditorState(
                mode = EditMode.EDIT,
                rawSpentValue = "0",
                stage = EditStage.EDIT_SPENT
            ),
            onNumberInput = { },
            onDotInput = { },
            onBackspace = { },
            onBackspaceLongPress = { },
            onDelete = { },
            onApply = { },
        )
    }
}
