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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.tutorial.TutorialBoxState
import com.serranoie.app.minus.presentation.ui.tutorial.markForTutorial
import com.serranoie.app.minus.presentation.util.Utils.abortFeedback
import com.serranoie.app.minus.presentation.util.haptic.HapticUtil
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
    onThousandsInput: () -> Unit = {},
    showThousandsShortcut: Boolean = false,
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
                if (!enableCalculationMode || !enableCalcModeSwipe) return@pointerInput
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
                            HapticUtil.performSliderHaptic(view)
                            lastTickProgress = progress
                        }

                        if (progress != lastReportedProgress) {
                            onDragProgressChanged(progress)
                            lastReportedProgress = progress
                        }

                        if (progress >= 1f && !hasTriggered) {
                            HapticUtil.performHeavyHaptic(view)
                            onCalculationModeChanged(!isCalculation)
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
        if (hasHardKeyboard && !isCalculation) {
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
                                showThousandsShortcut = showThousandsShortcut,
                                onNumberInput = {
                                    onNumberInput(it)
                                    debugProgress = 0
                                },
                                onDotInput = {
                                    onDotInput()
                                    debugProgress = (debugProgress + 1).coerceAtMost(TEST_NOTIFICATION_TAP_COUNT)
                                },
                                onThousandsInput = {
                                    onThousandsInput()
                                    debugProgress = 0
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
                    effectiveDragProgress = effectiveDragProgress,
                    showThousandsShortcut = showThousandsShortcut,
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
                    onEqualsInput = onEqualsInput,
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
        ExpandableRow(
            items = listOf('÷', '×', '+', '-'),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { _, operator, interactionSource ->
            NumpadButton(
                modifier = Modifier.padding(BUTTON_GAP),
                type = NumpadButtonType.OPERATOR,
                text = operator.toString(),
                interactionSource = interactionSource,
                onClick = {
                    onOperatorInput(operator)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            )
        }

        ExpandableRow(
            itemCount = 2,
            baseWeights = listOf(2f, 2f),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { index, interactionSource ->
            if (index == 0) {
                NumpadButton(
                    modifier = Modifier.padding(BUTTON_GAP),
                    type = NumpadButtonType.OPERATOR,
                    text = getFloatDivider(),
                    interactionSource = interactionSource,
                    onClick = {
                        onDotInput()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )
            } else {
                NumpadButton(
                    modifier = Modifier.padding(BUTTON_GAP),
                    type = NumpadButtonType.OPERATOR,
                    text = "=",
                    interactionSource = interactionSource,
                    onClick = {
                        onEqualsInput()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )
            }
        }

        val targetIsDelete = remember(editorState.rawSpentValue, editorState.mode, isCalculation) {
            !isCalculation && editorState.mode == EditMode.EDIT &&
                    tryConvertStringToNumber(editorState.rawSpentValue).join(third = false).let {
                        it == "0" || it == "0." || it == "0.0"
                    }
        }

        ExpandableRow(
            itemCount = 2,
            baseWeights = listOf(2f, 2f),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { index, interactionSource ->
            if (index == 0) {
                NumpadButton(
                    modifier = Modifier.padding(BUTTON_GAP),
                    type = NumpadButtonType.TERTIARY,
                    icon = Icons.AutoMirrored.Rounded.Backspace,
                    interactionSource = interactionSource,
                    onClick = {
                        onBackspace()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onLongClick = {
                        onBackspaceLongPress()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
            } else {
                NumpadButton(
                    modifier = Modifier.padding(BUTTON_GAP),
                    type = if (targetIsDelete) NumpadButtonType.DELETE else NumpadButtonType.PRIMARY,
                    icon = if (targetIsDelete) Icons.Default.Delete else if (isCalculation) Icons.Default.Done else Icons.Default.Check,
                    interactionSource = interactionSource,
                    onClick = {
                        if (targetIsDelete) onDelete() else onApply()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
            }
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
    val operators = remember { listOf('÷', '×', '+', '-') }
    Box(
        Modifier.fillMaxWidth().weight(effectiveDragProgress.coerceAtLeast(0.01f)).clipToBounds(),
        contentAlignment = Alignment.BottomCenter
    ) {
        ExpandableRow(
            items = operators,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .graphicsLayer(alpha = effectiveDragProgress)
                .let { if (tutorialBoxState != null) it.markForTutorial(tutorialBoxState, 7) else it }
        ) { _, operator, interactionSource ->
            NumpadButton(
                modifier = Modifier.padding(BUTTON_GAP),
                type = NumpadButtonType.OPERATOR,
                text = operator.toString(),
                interactionSource = interactionSource,
                onClick = {
                    onOperatorInput(operator)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            )
        }
    }
}

private val ThousandsOffsetFirstY = (-5).dp
private val ThousandsOffsetThirdY = 5.dp

@Composable
private fun ThousandsButtonLabel(color: Color, style: TextStyle) {
    val scaledStyle = style.copy(fontSize = style.fontSize * 0.75f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = "0",
            color = color,
            style = scaledStyle,
            maxLines = 1,
            modifier = Modifier.offset(y = ThousandsOffsetFirstY)
        )
        Text(
            text = "0",
            color = color,
            style = scaledStyle,
            maxLines = 1
        )
        Text(
            text = "0",
            color = color,
            style = scaledStyle,
            maxLines = 1,
            modifier = Modifier.offset(y = ThousandsOffsetThirdY)
        )
    }
}

@Composable
private fun NumberGrid(
    effectiveDragProgress: Float,
    isCalculation: Boolean,
    showThousandsShortcut: Boolean,
    onNumberInput: (Int) -> Unit,
    onDotInput: () -> Unit,
    onThousandsInput: () -> Unit,
    onEqualsInput: () -> Unit,
    numberHintAnchorModifier: Modifier,
    onNumberPressedForTutorial: (() -> Unit)?
) {
    val haptic = LocalHapticFeedback.current
    Column(Modifier.fillMaxHeight()) {
        val rows = listOf(7..9, 4..6, 1..3)
        for (row in rows) {
            ExpandableRow(
                items = row.toList(),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) { _, i, interactionSource ->
                NumpadButton(
                    modifier = Modifier
                        .padding(BUTTON_GAP)
                        .then(if (i == 5) numberHintAnchorModifier else Modifier),
                    type = NumpadButtonType.DEFAULT,
                    text = i.toString(),
                    interactionSource = interactionSource,
                    onClick = {
                        onNumberInput(i)
                        if (i in 4..6) onNumberPressedForTutorial?.invoke()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )
            }
        }

        val showDot = effectiveDragProgress > 0.01f || isCalculation
        val thousandsButton: @Composable RowScope.(MutableInteractionSource) -> Unit = { interactionSource ->
            NumpadButton(
                modifier = Modifier.padding(BUTTON_GAP),
                type = NumpadButtonType.DEFAULT,
                interactionSource = interactionSource,
                animateTextSize = false,
                onClick = {
                    onThousandsInput()
                    onNumberPressedForTutorial?.invoke()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            ) { color, style -> ThousandsButtonLabel(color, style) }
        }
        val equalsOrDotButton: @Composable RowScope.(MutableInteractionSource) -> Unit = { interactionSource ->
            val showEquals = isCalculation || effectiveDragProgress > 0.5f
            AnimatedContent(targetState = showEquals, label = "LastButtonSwap") { equals ->
                NumpadButton(
                    modifier = Modifier.fillMaxSize().padding(BUTTON_GAP),
                    type = NumpadButtonType.OPERATOR,
                    text = if (equals) "=" else getFloatDivider(),
                    interactionSource = interactionSource,
                    onClick = {
                        if (equals) onEqualsInput() else onDotInput()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )
            }
        }
        val zeroButton: @Composable RowScope.(MutableInteractionSource) -> Unit = { interactionSource ->
            NumpadButton(
                modifier = Modifier.padding(BUTTON_GAP),
                type = NumpadButtonType.DEFAULT,
                text = "0",
                interactionSource = interactionSource,
                onClick = {
                    onNumberInput(0)
                    onNumberPressedForTutorial?.invoke()
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            )
        }
        if (showDot) {
            if (showThousandsShortcut) {
                ExpandableRow(
                    itemCount = 3,
                    baseWeights = listOf(
                        effectiveDragProgress.coerceAtLeast(0.01f),
                        1f,
                        1f
                    ),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) { index, interactionSource ->
                    when (index) {
                        0 -> NumpadButton(
                            modifier = Modifier.padding(BUTTON_GAP).graphicsLayer(alpha = effectiveDragProgress),
                            type = NumpadButtonType.OPERATOR,
                            text = getFloatDivider(),
                            interactionSource = interactionSource,
                            onClick = {
                                onDotInput()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                        1 -> zeroButton(interactionSource)
                        2 -> thousandsButton(interactionSource)
                    }
                }
            } else {
                ExpandableRow(
                    itemCount = 3,
                    baseWeights = listOf(
                        effectiveDragProgress.coerceAtLeast(0.01f),
                        3f - (2f * effectiveDragProgress),
                        1.5f - (0.5f * effectiveDragProgress)
                    ),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) { index, interactionSource ->
                    when (index) {
                        0 -> NumpadButton(
                            modifier = Modifier.padding(BUTTON_GAP).graphicsLayer(alpha = effectiveDragProgress),
                            type = NumpadButtonType.OPERATOR,
                            text = getFloatDivider(),
                            interactionSource = interactionSource,
                            onClick = {
                                onDotInput()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                        1 -> zeroButton(interactionSource)
                        2 -> equalsOrDotButton(interactionSource)
                    }
                }
            }
        } else if (showThousandsShortcut) {
            ExpandableRow(
                itemCount = 3,
                baseWeights = listOf(1.5f, 1.5f, 1.5f),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) { index, interactionSource ->
                when (index) {
                    0 -> equalsOrDotButton(interactionSource)
                    1 -> zeroButton(interactionSource)
                    2 -> thousandsButton(interactionSource)
                }
            }
        } else {
            ExpandableRow(
                itemCount = 2,
                baseWeights = listOf(3f, 1.5f),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) { index, interactionSource ->
                when (index) {
                    0 -> zeroButton(interactionSource)
                    1 -> equalsOrDotButton(interactionSource)
                }
            }
        }
    }
}

@Composable
private fun RowScope.ActionButtonsColumn(
    editorState: EditorState,
    isCalculation: Boolean,
    effectiveDragProgress: Float = 0f,
    showThousandsShortcut: Boolean = false,
    onBackspace: () -> Unit,
    onBackspaceLongPress: () -> Unit,
    onDelete: () -> Unit,
    onApply: () -> Unit,
    onEqualsInput: () -> Unit = {},
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

        val showEqualsInColumn4 = showThousandsShortcut && (effectiveDragProgress > 0.01f || isCalculation)
        val checkWeight = if (showEqualsInColumn4) 3f - (1f * effectiveDragProgress) else 3f

        AnimatedContent(
            modifier = Modifier.weight(checkWeight),
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

        if (showEqualsInColumn4) {
            val equalsWeight = 1f * effectiveDragProgress
            if (equalsWeight > 0.01f) {
                NumpadButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(equalsWeight)
                        .padding(BUTTON_GAP)
                        .graphicsLayer(alpha = effectiveDragProgress),
                    type = NumpadButtonType.OPERATOR,
                    text = "=",
                    onClick = {
                        onEqualsInput()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun NumpadPreview() {
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

@Preview(name = "Numpad - Thousands Shortcut")
@Composable
private fun NumpadPreviewThousandsShortcut() {
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
            isCalculation = false,
            showThousandsShortcut = true
        )
    }
}

@Preview(name = "Numpad - Calculation ON")
@Composable
private fun NumpadPreviewCalculationMode() {
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

@Preview(name = "Numpad - Thousands + Calculation ON")
@Composable
private fun NumpadPreviewThousandsCalculation() {
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
            isCalculation = true,
            showThousandsShortcut = true
        )
    }
}
