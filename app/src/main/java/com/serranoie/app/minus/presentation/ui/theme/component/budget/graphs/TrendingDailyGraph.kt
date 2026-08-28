package com.serranoie.app.minus.presentation.ui.theme.component.budget.graphs

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.time.LocalTime

private fun hourPointsMaxVal(points: List<BigDecimal>): Float {
    val rawMax = points.maxOfOrNull { it } ?: BigDecimal.ZERO
    return if (rawMax <= BigDecimal.ZERO) 100f else rawMax.toFloat()
}

private fun interpolateHourPoints(
    oldPoints: List<BigDecimal>,
    newPoints: List<BigDecimal>,
    factor: Float,
): List<BigDecimal> {
    if (factor >= 1f) return newPoints
    if (factor <= 0f) return oldPoints
    return List(newPoints.size) { i ->
        val oldVal = (oldPoints.getOrNull(i) ?: BigDecimal.ZERO).toFloat()
        val newVal = newPoints[i].toFloat()
        (oldVal + (newVal - oldVal) * factor).toBigDecimal()
    }
}

@Stable
private class GraphHourTransitionState(initialPoints: List<BigDecimal>) {
    var renderPoints by mutableStateOf(initialPoints)
        private set
    var oldPoints by mutableStateOf(initialPoints)
        private set

    val animProgress = Animatable(1f)

    val interpolatedPoints by derivedStateOf {
        interpolateHourPoints(oldPoints, renderPoints, animProgress.value)
    }

    val maxVal by derivedStateOf { hourPointsMaxVal(interpolatedPoints) }
    val oldMaxVal by derivedStateOf { hourPointsMaxVal(oldPoints) }
    val newMaxVal by derivedStateOf { hourPointsMaxVal(renderPoints) }

    suspend fun updateTo(newPoints: List<BigDecimal>, animDuration: Int = 600) {
        if (newPoints == renderPoints) return
        oldPoints = interpolatedPoints
        renderPoints = newPoints
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(animDuration))
    }
}

@Composable
private fun rememberGraphHourTransitionState(points: List<BigDecimal>): GraphHourTransitionState {
    val state = remember { GraphHourTransitionState(points) }
    LaunchedEffect(points) { state.updateTo(points) }
    return state
}

@Composable
internal fun GraphHourCanvas(
    points: List<BigDecimal>,
    modifier: Modifier = Modifier,
    currencyCode: String,
    isToday: Boolean = false,
) {
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    val currencyFormat = remember(currencyCode) { symbolOnlyCurrencyFormat(currencyCode) }
    val textMeasurer = rememberTextMeasurer()
    val tooltipStyle = MaterialTheme.typography.labelSmallEmphasized.copy(
        color = Color.White,
        fontWeight = FontWeight.Bold
    )
    val labelStyle = MaterialTheme.typography.labelSmallCondensed.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )

    var touchPosition by remember { mutableStateOf<Offset?>(null) }

    val transitionState = rememberGraphHourTransitionState(points)

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> touchPosition = offset },
                    onDrag = { change, _ -> touchPosition = change.position },
                    onDragEnd = { touchPosition = null },
                    onDragCancel = { touchPosition = null },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        touchPosition = offset
                        tryAwaitRelease()
                        touchPosition = null
                    },
                    onTap = { offset -> touchPosition = offset },
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val leftMargin = 42.dp.toPx()
        val bottomMargin = 20.dp.toPx()
        val topPadding = 10.dp.toPx()
        val drawableWidth = width - leftMargin
        val drawableHeight = height - bottomMargin - topPadding
        val interpolatedPoints = transitionState.interpolatedPoints
        val stepWidth = drawableWidth / (interpolatedPoints.size - 1).coerceAtLeast(1)
        val baseline = height - bottomMargin
        val progress = transitionState.animProgress.value

        val oldAlpha = (1f - progress * 2.5f).coerceIn(0f, 1f)
        val newAlpha = (progress * 2.5f - 1.5f).coerceIn(0f, 1f)
        val effectiveNewAlpha = if (progress >= 1f) 1f else newAlpha

        if (oldAlpha > 0f && progress < 1f) {
            drawHourGridlines(
                leftMargin = leftMargin,
                baseline = baseline,
                drawableWidth = drawableWidth,
                drawableHeight = drawableHeight,
                width = width,
                maxVal = transitionState.oldMaxVal,
                gridColor = gridColor,
                textMeasurer = textMeasurer,
                labelStyle = labelStyle,
                currencyFormat = currencyFormat,
                alpha = oldAlpha,
            )
        }
        if (effectiveNewAlpha > 0f) {
            drawHourGridlines(
                leftMargin = leftMargin,
                baseline = baseline,
                drawableWidth = drawableWidth,
                drawableHeight = drawableHeight,
                width = width,
                maxVal = transitionState.newMaxVal,
                gridColor = gridColor,
                textMeasurer = textMeasurer,
                labelStyle = labelStyle,
                currencyFormat = currencyFormat,
                alpha = effectiveNewAlpha,
            )
        }

        drawHourAxisLabels(
            leftMargin = leftMargin,
            baseline = baseline,
            stepWidth = stepWidth,
            textMeasurer = textMeasurer,
            labelStyle = labelStyle,
        )

        if (isToday) {
            val currentHourX = leftMargin + LocalTime.now().hour * stepWidth
            drawLine(
                color = tertiaryColor.copy(alpha = 0.4f),
                start = Offset(currentHourX, topPadding),
                end = Offset(currentHourX, baseline),
                strokeWidth = 2.dp.toPx(),
            )
        }

        if (interpolatedPoints.size > 1) {
            drawGraphArea(
                points = interpolatedPoints,
                maxVal = transitionState.maxVal,
                color = tertiaryColor,
                leftMargin = leftMargin,
                topPadding = topPadding,
                bottomMargin = bottomMargin,
            )
            drawGraphLine(
                points = interpolatedPoints,
                maxVal = transitionState.maxVal,
                color = tertiaryColor,
                width = 4.dp.toPx(),
                leftMargin = leftMargin,
                topPadding = topPadding,
                bottomMargin = bottomMargin,
            )
        }

        touchPosition?.let { pos ->
            if (pos.x >= leftMargin) {
                drawTooltipInteraction(
                    pos = pos,
                    currentPoints = interpolatedPoints,
                    maxVal = transitionState.maxVal,
                    color = tertiaryColor,
                    surfaceColor = surfaceColor,
                    currencyFormat = currencyFormat,
                    textMeasurer = textMeasurer,
                    tooltipStyle = tooltipStyle,
                    leftMargin = leftMargin,
                    topPadding = topPadding,
                    bottomMargin = bottomMargin,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Graph Hour Canvas")
@Preview(
    showBackground = true, name = "Graph Hour Canvas",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun PreviewGraphHourCanvas() {
    val points = buildList {
        var cumulative = BigDecimal.ZERO
        val hourlyAmounts = mapOf(8 to 15, 9 to 6, 13 to 42)
        for (hour in 0 until 24) {
            cumulative += hourlyAmounts[hour]?.toBigDecimal() ?: BigDecimal.ZERO
            add(cumulative)
        }
    }

    MinusTheme {
        Surface {
            Box(Modifier.padding(16.dp)) {
                GraphHourCanvas(
                    points = points,
                    currencyCode = "USD",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                )
            }
        }
    }
}
