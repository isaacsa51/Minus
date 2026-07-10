package com.serranoie.app.minus.presentation.ui.tutorial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import logcat.logcat
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme

/**
 * Wraps [content] with a first-launch coachmark overlay.
 *
 * When [showTutorial] is `true` and [state] has at least one registered target, draws a
 * dim scrim with a focus cutout around the current target and a tooltip card
 * (rendered by [tutorialTarget]) positioned next to the target. Tapping anywhere on the
 * overlay advances to the next target; after the last target, [onTutorialCompleted] is
 * invoked exactly once.
 *
 * Implementation note: the overlay is a sibling of [content] in a [Box] and uses
 * [onGloballyPositioned] to learn its own size, NOT [androidx.compose.foundation.layout.BoxWithConstraints].
 * `BoxWithConstraints` uses subcomposition and was causing layout interference with the
 * content (which is also a subcomposing layout via its own `BoxWithConstraints`).
 *
 * @param showTutorial toggles the overlay on/off; pass a Boolean derived from a
 *   remembered `MutableState` so re-compositions don't re-create the state.
 * @param onTutorialCompleted invoked once when the user taps through the last target.
 *   The caller typically flips a "do not show again" flag here.
 * @param state the [TutorialBoxState] shared with [markForTutorial] call sites.
 * @param tutorialTarget composable that renders the tooltip body for the currently
 *   highlighted target. Receives the target's index so it can pick the right text.
 * @param content the actual app UI to coachmark over.
 */
@Composable
fun TutorialBox(
    showTutorial: Boolean,
    onTutorialCompleted: () -> Unit,
    state: TutorialBoxState,
    tutorialTarget: @Composable (index: Int) -> Unit,
    content: @Composable () -> Unit,
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                val w = coords.size.width.toFloat()
                val h = coords.size.height.toFloat()
                if (w > 0f && h > 0f) canvasSize = Size(w, h)
            },
    ) {
        content()

        val activeBounds = state.currentBounds
        val shouldShow = showTutorial && !state.isCompleted && activeBounds != null && canvasSize.isSpecified
        logcat(TUTORIAL_LOG_TAG) {
            "TutorialBox render: showTutorial=$showTutorial " +
                "isCompleted=${state.isCompleted} " +
                "activeBounds=$activeBounds " +
                "canvasSize=$canvasSize " +
                "currentIndex=${state.currentIndexState.value} " +
                "registrationOrder=${state.registrationOrder.toList()} " +
                "shouldShow=$shouldShow"
        }
        if (shouldShow) {
            TutorialOverlay(
                bounds = activeBounds,
                canvasSize = canvasSize,
                index = state.currentIndexState.value,
                tutorialTarget = tutorialTarget,
                onTap = { state.advance() },
            )
        }
    }

    LaunchedEffect(state.isCompleted) {
        logcat(TUTORIAL_LOG_TAG) { "TutorialBox isCompleted changed → ${state.isCompleted}, firing onTutorialCompleted=${state.isCompleted}" }
        if (state.isCompleted) onTutorialCompleted()
    }
}

@Composable
private fun TutorialOverlay(
    bounds: Rect,
    canvasSize: Size,
    index: Int,
    tutorialTarget: @Composable (Int) -> Unit,
    onTap: () -> Unit,
) {
    if (bounds.isEmpty) return

    val scrimColor = Color.Black.copy(alpha = 0.55f)
    val highlightStrokeColor = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val paddingPx = with(density) { 6.dp.toPx() }
    val cornerRadiusPx = with(density) { 12.dp.toPx() }
    val tooltipGapPx = with(density) { 16.dp.toPx() }
    val tooltipMaxWidthPx = with(density) { 260.dp.toPx() }
    val tooltipMaxWidth = with(density) { tooltipMaxWidthPx.toDp() }
    val tooltipMinHeightEstimate = 96f

    val cutout = Rect(
        left = bounds.left - paddingPx,
        top = bounds.top - paddingPx,
        right = bounds.right + paddingPx,
        bottom = bounds.bottom + paddingPx,
    )

    val (tooltipX, tooltipY) = computeTooltipPosition(
        targetBounds = bounds,
        canvasSize = canvasSize,
        gapPx = tooltipGapPx,
        tooltipWidthPx = tooltipMaxWidthPx,
        tooltipHeightPx = tooltipMinHeightEstimate,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTap,
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outer = Path().apply {
                addRect(Rect(offset = Offset.Zero, size = this@Canvas.size))
            }
            val hole = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = cutout,
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    ),
                )
            }
            val scrimPath = Path().apply {
                op(outer, hole, PathOperation.Difference)
            }
            drawPath(path = scrimPath, color = scrimColor)
            drawRoundRect(
                color = highlightStrokeColor,
                topLeft = Offset(cutout.left, cutout.top),
                size = Size(cutout.width, cutout.height),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                style = Stroke(width = 3f),
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .offset { IntOffset(tooltipX, tooltipY) }
                .widthIn(max = tooltipMaxWidth)
                .padding(horizontal = 16.dp),
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                tutorialTarget(index)
            }
        }
    }
}

/**
 * Pick the side of the target with the most free space and anchor the tooltip just
 * outside the cutout. Clamps the result so the tooltip never falls off-screen even on
 * small phones.
 */
private fun computeTooltipPosition(
    targetBounds: Rect,
    canvasSize: Size,
    gapPx: Float,
    tooltipWidthPx: Float,
    tooltipHeightPx: Float,
): Pair<Int, Int> {
    val spaceAbove = targetBounds.top
    val spaceBelow = canvasSize.height - targetBounds.bottom
    val spaceLeft = targetBounds.left
    val spaceRight = canvasSize.width - targetBounds.right

    val placement = when {
        spaceBelow >= tooltipHeightPx + gapPx -> TooltipPlacement.Below
        spaceAbove >= tooltipHeightPx + gapPx -> TooltipPlacement.Above
        spaceRight >= tooltipWidthPx + gapPx -> TooltipPlacement.Right
        spaceLeft >= tooltipWidthPx + gapPx -> TooltipPlacement.Left
        else -> TooltipPlacement.Below
    }

    val tooltipWidth = tooltipWidthPx.coerceAtMost(canvasSize.width - 32f)
    val centerX = (targetBounds.left + targetBounds.right) / 2f
    val centerY = (targetBounds.top + targetBounds.bottom) / 2f

    val (rawX, rawY) = when (placement) {
        TooltipPlacement.Below -> {
            val anchoredX = (centerX - tooltipWidth / 2f).coerceIn(16f, canvasSize.width - tooltipWidth - 16f)
            anchoredX to (targetBounds.bottom + gapPx)
        }
        TooltipPlacement.Above -> {
            val anchoredX = (centerX - tooltipWidth / 2f).coerceIn(16f, canvasSize.width - tooltipWidth - 16f)
            anchoredX to (targetBounds.top - gapPx - tooltipHeightPx)
        }
        TooltipPlacement.Right -> {
            (targetBounds.right + gapPx) to (centerY - tooltipHeightPx / 2f)
        }
        TooltipPlacement.Left -> {
            (targetBounds.left - gapPx - tooltipWidth) to (centerY - tooltipHeightPx / 2f)
        }
    }

    val safeX = rawX.coerceIn(0f, (canvasSize.width - tooltipWidth).coerceAtLeast(0f))
    val safeY = rawY.coerceIn(0f, (canvasSize.height - tooltipHeightPx).coerceAtLeast(0f))
    return safeX.toInt() to safeY.toInt()
}

private val Size.isSpecified: Boolean
    get() = width > 0f && height > 0f

private enum class TooltipPlacement { Above, Below, Left, Right }

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun TutorialBoxPreview() {
    MinusTheme {
        val state = rememberTutorialBoxState()
        TutorialBox(
            showTutorial = true,
            onTutorialCompleted = {},
            state = state,
            tutorialTarget = { index ->
                Text(
                    text = when (index) {
                        0 -> "This is the first element you should look at."
                        1 -> "And this is the second one, further down."
                        else -> "Tutorial step $index"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Surface(
                    tonalElevation = 2.dp,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.markForTutorial(state, 0)
                ) {
                    Text(
                        text = "Highlight Me 1",
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(64.dp))

                Surface(
                    tonalElevation = 2.dp,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.markForTutorial(state, 1)
                ) {
                    Text(
                        text = "Highlight Me 2",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Renders the tooltip body shown inside the [TutorialBox] coachmark overlay for a
 * single step. Title in `titleMedium` (bold) on top, description in `bodyMedium`
 * (regular) below with an 8dp gap — kept inside the [Surface] that [TutorialBox] draws
 * around `tutorialTarget`, so this composable just paints text, no chrome.
 */
@Composable
fun TutorialTooltip(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (description.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
