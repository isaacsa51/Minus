package com.serranoie.app.minus.presentation.ui.theme.component.numpad

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.tutorial.TutorialBoxState
import com.serranoie.app.minus.presentation.ui.tutorial.markForTutorial
import com.serranoie.app.minus.presentation.util.Utils.abortFeedback
import com.serranoie.app.minus.presentation.util.font.format.getFloatDivider
import com.serranoie.app.minus.presentation.util.font.format.join
import com.serranoie.app.minus.presentation.util.font.format.tryConvertStringToNumber
import java.util.Date
import kotlin.math.abs

val BUTTON_GAP = 3.dp
private const val TEST_NOTIFICATION_TAP_COUNT = 5

enum class EditMode { ADD, EDIT }

enum class EditStage { IDLE, EDIT_SPENT }

data class Transaction(
    val id: Long, val amount: String, val comment: String, val date: Date
)

data class EditorState(
    val mode: EditMode,
    val rawSpentValue: String,
    val stage: EditStage,
    val currentSpent: String,
    val currentComment: String,
    val editedTransaction: Transaction?
)

@Composable
fun Numpad(
    modifier: Modifier = Modifier,
    editorState: EditorState,
    onNumberInput: (Int) -> Unit = {},
    onDotInput: () -> Unit = {},
    onEqualsInput: () -> Unit = {},
    onBackspace: () -> Unit = {},
    onBackspaceLongPress: () -> Unit = {},
    onDelete: () -> Unit = {},
    onApply: () -> Unit = {},
    isCalculation: Boolean = false,
    onCalculationModeChanged: (Boolean) -> Unit = {},
    onOperatorInput: (Char) -> Unit = {},
    onToggleDebug: (() -> Unit)? = null,
    onShowSnackbar: ((String) -> Unit)? = null,
    onActivateTutorial: (() -> Unit)? = null,
    onTestNotifications: (() -> Unit)? = null,
    numberHintAnchorModifier: Modifier = Modifier,
    applyHintAnchorModifier: Modifier = Modifier,
    onNumberPressedForTutorial: (() -> Unit)? = null,
    onApplyPressedForTutorial: (() -> Unit)? = null,
    onDragProgressChanged: (Float) -> Unit = {},
    dragProgress: Float = 0f,
    hasHardKeyboard: Boolean = false,
    enableCalculationMode: Boolean = true,
    enableCalcModeSwipe: Boolean = enableCalculationMode,
    leftContent: (@Composable ColumnScope.() -> Unit)? = null,
    tutorialBoxState: TutorialBoxState? = null,
) {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    var debugProgress by remember { mutableIntStateOf(0) }

    val effectiveDragProgress by animateFloatAsState(
        targetValue = if (dragProgress > 0f && dragProgress < 1f) dragProgress else if (isCalculation) 1f else 0f,
        animationSpec = if (dragProgress > 0f && dragProgress < 1f) tween(0) else tween(200),
        label = "NumpadDragProgress"
    )

    val hasOperators by remember(editorState.rawSpentValue) {
        derivedStateOf { editorState.rawSpentValue.any { it in "+-×÷" } }
    }

    val shouldTriggerTestNotifications: () -> Boolean = {
        if (debugProgress >= TEST_NOTIFICATION_TAP_COUNT) {
            onTestNotifications?.invoke()
            onShowSnackbar?.invoke("Test notifications triggered!")
            debugProgress = 0
            true
        } else false
    }

    Column(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp)
            .pointerInput(isCalculation, hasOperators, enableCalculationMode, enableCalcModeSwipe, hasHardKeyboard) {
                if (!enableCalculationMode || !enableCalcModeSwipe || hasHardKeyboard) return@pointerInput
                var accumulatedDrag = 0f
                var lastReportedProgress = 0f
                var lastTickProgress = 0f
                var hasTriggered = false

                detectVerticalDragGestures(
                    onDragStart = {
                        accumulatedDrag = 0f
                        lastTickProgress = 0f
                        hasTriggered = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDrag += dragAmount
                        val progress = when {
                            accumulatedDrag < 0f && !isCalculation -> {
                                (-accumulatedDrag / 110f).coerceIn(0f, 1f)
                            }
                            accumulatedDrag > 0f && isCalculation && !hasOperators -> {
                                (accumulatedDrag / 110f).coerceIn(0f, 1f)
                            }
                            else -> 0f
                        }

                        if (abs(progress - lastTickProgress) > 0.15f) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            lastTickProgress = progress
                        }

                        if (abs(progress - lastReportedProgress) >= 0.05f) {
                            onDragProgressChanged(progress)
                            lastReportedProgress = progress
                        }

                        if (progress >= 1f && !hasTriggered) {
                            onCalculationModeChanged(!isCalculation)
                            onDragProgressChanged(0f)
                            hasTriggered = true
                        }
                    },
                    onDragEnd = {
                        if (!hasTriggered && lastReportedProgress > 0.2f) {
                            view.abortFeedback()
                        }
                        onDragProgressChanged(0f)
                    },
                    onDragCancel = { onDragProgressChanged(0f) }
                )
            }
    ) {
        if (hasHardKeyboard) {
            CompactHardwareLayout(
                editorState = editorState,
                isCalculation = isCalculation,
                onOperatorInput = onOperatorInput,
                onDotInput = onDotInput,
                onEqualsInput = onEqualsInput,
                onBackspace = onBackspace,
                onBackspaceLongPress = onBackspaceLongPress,
                onDelete = onDelete,
                onApply = onApply
            )
        } else {
            if (effectiveDragProgress > 0.01f || isCalculation) {
                OperatorRow(
                    effectiveDragProgress = effectiveDragProgress,
                    tutorialBoxState = tutorialBoxState,
                    onOperatorInput = onOperatorInput
                )
            }

            Row(Modifier.fillMaxWidth().weight(4f)) {
                Box(Modifier.fillMaxHeight().weight(3f)) {
                    AnimatedContent(
                        targetState = leftContent != null,
                        label = "NumpadLeftContentSwap",
                        transitionSpec = {
                            (fadeIn(tween(250)) + scaleIn(initialScale = 0.96f, animationSpec = tween(250))) togetherWith
                            (fadeOut(tween(200)) + scaleOut(targetScale = 0.96f, animationSpec = tween(200))) using
                            SizeTransform(clip = false)
                        }
                    ) { hasLeftContent ->
                        if (hasLeftContent) {
                            Column(Modifier.fillMaxSize()) { leftContent?.invoke(this) }
                        } else {
                            NumberGrid(
                                effectiveDragProgress = effectiveDragProgress,
                                isCalculation = isCalculation,
                                onNumberInput = {
                                    onNumberInput(it)
                                    debugProgress = 0
                                },
                                onDotInput = {
                                    onDotInput()
                                    debugProgress = (debugProgress + 1).coerceAtMost(TEST_NOTIFICATION_TAP_COUNT)
                                },
                                onEqualsInput = onEqualsInput,
                                numberHintAnchorModifier = numberHintAnchorModifier,
                                onNumberPressedForTutorial = onNumberPressedForTutorial
                            )
                        }
                    }
                }

                ActionButtonsColumn(
                    editorState = editorState,
                    isCalculation = isCalculation,
                    onBackspace = {
                        onBackspace()
                        debugProgress = 0
                    },
                    onBackspaceLongPress = {
                        onBackspaceLongPress()
                        debugProgress = 0
                    },
                    onDelete = onDelete,
                    onApply = {
                        if (!shouldTriggerTestNotifications()) {
                            debugProgress = 0
                            onApplyPressedForTutorial?.invoke()
                            onApply()
                        }
                    },
                    applyHintAnchorModifier = applyHintAnchorModifier
                )
            }
        }
    }
}

@Composable
private fun CompactHardwareLayout(
    editorState: EditorState,
    isCalculation: Boolean,
    onOperatorInput: (Char) -> Unit,
    onDotInput: () -> Unit,
    onEqualsInput: () -> Unit,
    onBackspace: () -> Unit,
    onBackspaceLongPress: () -> Unit,
    onDelete: () -> Unit,
    onApply: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Column(Modifier.fillMaxWidth().fillMaxHeight()) {
        Row(Modifier.fillMaxWidth().weight(1f)) {
            listOf('÷', '×', '+', '-').forEach { operator ->
                NumpadButton(
                    modifier = Modifier.weight(1f).padding(BUTTON_GAP),
                    type = NumpadButtonType.OPERATOR,
                    text = operator.toString(),
                    onClick = {
                        onOperatorInput(operator)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )
            }
        }

        Row(Modifier.fillMaxWidth().weight(1f)) {
            NumpadButton(
                modifier = Modifier.weight(2f).padding(BUTTON_GAP),
                type = NumpadButtonType.OPERATOR,
                text = getFloatDivider(),
                onClick = {
                    onDotInput()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            )
            NumpadButton(
                modifier = Modifier.weight(2f).padding(BUTTON_GAP),
                type = NumpadButtonType.OPERATOR,
                text = "=",
                onClick = {
                    onEqualsInput()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            )
        }

        Row(Modifier.fillMaxWidth().weight(1f)) {
            NumpadButton(
                modifier = Modifier.weight(2f).padding(BUTTON_GAP),
                type = NumpadButtonType.TERTIARY,
                icon = Icons.AutoMirrored.Rounded.Backspace,
                onClick = {
                    onBackspace()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                onLongClick = {
                    onBackspaceLongPress()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )

            val targetIsDelete = remember(editorState.rawSpentValue, editorState.mode, isCalculation) {
                !isCalculation && editorState.mode == EditMode.EDIT &&
                        tryConvertStringToNumber(editorState.rawSpentValue).join(third = false).let {
                            it == "0" || it == "0." || it == "0.0"
                        }
            }

            NumpadButton(
                modifier = Modifier.weight(2f).padding(BUTTON_GAP),
                type = if (targetIsDelete) NumpadButtonType.DELETE else NumpadButtonType.PRIMARY,
                icon = if (targetIsDelete) Icons.Default.Delete else if (isCalculation) Icons.Default.Done else Icons.Default.Check,
                onClick = {
                    if (targetIsDelete) onDelete() else onApply()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
        }
    }
}

@Composable
private fun ColumnScope.OperatorRow(
    effectiveDragProgress: Float,
    tutorialBoxState: TutorialBoxState?,
    onOperatorInput: (Char) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        Modifier.fillMaxWidth().weight(effectiveDragProgress.coerceAtLeast(0.01f)).clipToBounds(),
        contentAlignment = androidx.compose.ui.Alignment.BottomCenter
    ) {
        Row(
            Modifier.fillMaxWidth().fillMaxHeight().graphicsLayer(alpha = effectiveDragProgress)
                .let { if (tutorialBoxState != null) it.markForTutorial(tutorialBoxState, 7) else it }
        ) {
            listOf('÷', '×', '+', '-').forEach { operator ->
                NumpadButton(
                    modifier = Modifier.weight(1f).padding(BUTTON_GAP),
                    type = NumpadButtonType.OPERATOR,
                    text = operator.toString(),
                    onClick = {
                        onOperatorInput(operator)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )
            }
        }
    }
}

@Composable
private fun NumberGrid(
    effectiveDragProgress: Float,
    isCalculation: Boolean,
    onNumberInput: (Int) -> Unit,
    onDotInput: () -> Unit,
    onEqualsInput: () -> Unit,
    numberHintAnchorModifier: Modifier,
    onNumberPressedForTutorial: (() -> Unit)?
) {
    val haptic = LocalHapticFeedback.current
    Column(Modifier.fillMaxHeight()) {
        val rows = listOf(7..9, 4..6, 1..3)
        for (row in rows) {
            Row(Modifier.fillMaxWidth().weight(1f)) {
                for (i in row) {
                    NumpadButton(
                        modifier = Modifier.weight(1f).padding(BUTTON_GAP)
                            .then(if (i == 5) numberHintAnchorModifier else Modifier),
                        type = NumpadButtonType.DEFAULT,
                        text = i.toString(),
                        onClick = {
                            onNumberInput(i)
                            if (i in 4..6) onNumberPressedForTutorial?.invoke()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth().weight(1f)) {
            if (effectiveDragProgress > 0.01f || isCalculation) {
                NumpadButton(
                    modifier = Modifier.weight(effectiveDragProgress.coerceAtLeast(0.01f)).padding(BUTTON_GAP).graphicsLayer(alpha = effectiveDragProgress),
                    type = NumpadButtonType.OPERATOR,
                    text = getFloatDivider(),
                    onClick = {
                        onDotInput()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )
            }
            NumpadButton(
                modifier = Modifier.weight(3f - (2f * effectiveDragProgress)).padding(BUTTON_GAP),
                type = NumpadButtonType.DEFAULT,
                text = "0",
                onClick = {
                    onNumberInput(0)
                    onNumberPressedForTutorial?.invoke()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            )
            Box(Modifier.weight(1.5f - (0.5f * effectiveDragProgress)).fillMaxHeight()) {
                val showEquals = isCalculation || effectiveDragProgress > 0.5f
                AnimatedContent(targetState = showEquals, label = "LastButtonSwap") { equals ->
                    NumpadButton(
                        modifier = Modifier.fillMaxSize().padding(BUTTON_GAP),
                        type = NumpadButtonType.OPERATOR,
                        text = if (equals) "=" else getFloatDivider(),
                        onClick = {
                            if (equals) onEqualsInput() else onDotInput()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.ActionButtonsColumn(
    editorState: EditorState,
    isCalculation: Boolean,
    onBackspace: () -> Unit,
    onBackspaceLongPress: () -> Unit,
    onDelete: () -> Unit,
    onApply: () -> Unit,
    applyHintAnchorModifier: Modifier
) {
    val haptic = LocalHapticFeedback.current
    Column(Modifier.fillMaxHeight().weight(1f)) {
        NumpadButton(
            modifier = Modifier.weight(1f).padding(BUTTON_GAP),
            type = NumpadButtonType.TERTIARY,
            icon = Icons.AutoMirrored.Rounded.Backspace,
            onClick = {
                onBackspace()
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            onLongClick = {
                onBackspaceLongPress()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        )

        val targetIsDelete = remember(editorState.rawSpentValue, editorState.mode, isCalculation) {
            !isCalculation && editorState.mode == EditMode.EDIT &&
                    tryConvertStringToNumber(editorState.rawSpentValue).join(third = false).let {
                        it == "0" || it == "0." || it == "0.0"
                    }
        }

        AnimatedContent(
            modifier = Modifier.weight(3f),
            targetState = targetIsDelete,
            label = "Delete or Apply"
        ) { isDelete ->
            NumpadButton(
                modifier = Modifier.fillMaxSize().padding(BUTTON_GAP).then(if (!isDelete) applyHintAnchorModifier else Modifier),
                type = if (isDelete) NumpadButtonType.DELETE else NumpadButtonType.PRIMARY,
                icon = if (isDelete) Icons.Default.Delete else (if (isCalculation) Icons.Default.Done else Icons.Default.Check),
                onClick = {
                    if (isDelete) onDelete() else onApply()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
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
                stage = EditStage.EDIT_SPENT,
                currentSpent = "123",
                currentComment = "",
                editedTransaction = null
            ),
            isCalculation = false
        )
    }
}

@Preview(name = "Numpad - Calculation ON")
@Composable
fun NumpadPreviewCalculationMode() {
    MinusTheme {
        Numpad(
            editorState = EditorState(
                mode = EditMode.ADD,
                rawSpentValue = "123",
                stage = EditStage.EDIT_SPENT,
                currentSpent = "123",
                currentComment = "",
                editedTransaction = null
            ),
            isCalculation = true
        )
    }
}
