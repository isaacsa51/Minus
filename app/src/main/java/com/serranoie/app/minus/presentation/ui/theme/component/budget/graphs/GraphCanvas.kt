package com.serranoie.app.minus.presentation.ui.theme.component.budget.graphs

import android.content.res.Configuration
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.text.Format
import java.time.format.DateTimeFormatter
import java.util.Date

@Composable
internal fun GraphCanvas(
    currentPoints: List<BigDecimal>,
    previousPoints: List<BigDecimal>,
    maxVal: Float,
    oldMaxVal: Float,
    newMaxVal: Float,
    modifier: Modifier = Modifier,
    currencyCode: String,
    forcedTouchPosition: Offset? = null,
    startDate: Date,
    currentWindowIndex: Int,
    oldWindowIndex: Int,
    scrollStep: Int,
    oldScrollStep: Int,
    renderDataSize: Int,
    oldDataSize: Int,
    animProgress: Float,
    dateFormatter: DateTimeFormatter,
) {
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    val currencyFormat = remember(currencyCode) {
        symbolOnlyCurrencyFormat(currencyCode)
    }

    val textMeasurer = rememberTextMeasurer()
    val tooltipStyle = MaterialTheme.typography.labelSmallEmphasized.copy(
        color = Color.White,
        fontWeight = FontWeight.Bold
    )

    val labelStyle = MaterialTheme.typography.labelSmallCondensed.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )

    var touchPosition by remember(forcedTouchPosition) { mutableStateOf(forcedTouchPosition) }

    val thousandsUnit = stringResource(R.string.unit_thousands)
    val millionsUnit = stringResource(R.string.unit_millions)

    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }
    val drawParams =
        remember(tertiaryColor, secondaryColor, surfaceColor, gridColor, dashEffect, labelStyle) {
            GraphDrawParams(
                graphColor = tertiaryColor,
                secondaryColor = secondaryColor,
                surfaceColor = surfaceColor,
                gridColor = gridColor,
                dashEffect = dashEffect,
                labelStyle = labelStyle,
                tertiaryColor = tertiaryColor
            )
        }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        touchPosition = offset
                    },
                    onDrag = { change, _ ->
                        touchPosition = change.position
                    },
                    onDragEnd = {
                        touchPosition = null
                    },
                    onDragCancel = {
                        touchPosition = null
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        touchPosition = offset
                        tryAwaitRelease()
                        touchPosition = null
                    },
                    onTap = { offset ->
                        touchPosition = offset
                    }
                )
            }
    ) {
        val oldAlpha = (1f - animProgress * 2.5f).coerceIn(0f, 1f)
        val newAlpha = (animProgress * 2.5f - 1.5f).coerceIn(0f, 1f)

        if (oldAlpha > 0f && animProgress < 1f) {
            drawCoordinateSystem(
                params = drawParams,
                textMeasurer = textMeasurer,
                currencyFormat = currencyFormat,
                dateFormatter = dateFormatter,
                startDate = startDate,
                windowIndex = oldWindowIndex,
                scrollStep = oldScrollStep,
                dataSize = oldDataSize,
                maxVal = oldMaxVal,
                alpha = oldAlpha,
                thousandsUnit = thousandsUnit,
                millionsUnit = millionsUnit
            )
        }

        val effectiveNewAlpha = if (animProgress >= 1f) 1f else newAlpha

        if (effectiveNewAlpha > 0f) {
            drawCoordinateSystem(
                params = drawParams,
                textMeasurer = textMeasurer,
                currencyFormat = currencyFormat,
                dateFormatter = dateFormatter,
                startDate = startDate,
                windowIndex = currentWindowIndex,
                scrollStep = scrollStep,
                dataSize = renderDataSize,
                maxVal = newMaxVal,
                alpha = effectiveNewAlpha,
                thousandsUnit = thousandsUnit,
                millionsUnit = millionsUnit,
                isTodayHighlighted = true
            )
        }

        drawGraphLinesContent(
            currentPoints = currentPoints,
            previousPoints = previousPoints,
            maxVal = maxVal,
            params = drawParams,
            touchPosition = touchPosition,
            currencyFormat = currencyFormat,
            textMeasurer = textMeasurer,
            tooltipStyle = tooltipStyle,
            thousandsUnit = thousandsUnit,
            millionsUnit = millionsUnit
        )
    }
}

private fun DrawScope.drawGraphLinesContent(
    currentPoints: List<BigDecimal>,
    previousPoints: List<BigDecimal>,
    maxVal: Float,
    params: GraphDrawParams,
    touchPosition: Offset?,
    currencyFormat: Format,
    textMeasurer: TextMeasurer,
    tooltipStyle: TextStyle,
    thousandsUnit: String,
    millionsUnit: String
) {
    val leftMargin = 42.dp.toPx()
    val bottomMargin = 20.dp.toPx()
    val topPadding = 10.dp.toPx()

    if (previousPoints.size > 1) {
        drawGraphLine(
            points = previousPoints,
            maxVal = maxVal,
            color = params.secondaryColor.copy(alpha = 0.3f),
            width = 3.dp.toPx(),
            leftMargin = leftMargin,
            topPadding = topPadding,
            bottomMargin = bottomMargin
        )
    }

    if (currentPoints.size > 1) {
        drawGraphArea(
            points = currentPoints,
            maxVal = maxVal,
            color = params.graphColor,
            leftMargin = leftMargin,
            topPadding = topPadding,
            bottomMargin = bottomMargin
        )
        drawGraphLine(
            points = currentPoints,
            maxVal = maxVal,
            color = params.graphColor,
            width = 4.dp.toPx(),
            leftMargin = leftMargin,
            topPadding = topPadding,
            bottomMargin = bottomMargin
        )
    }

    touchPosition?.let { pos ->
        if (pos.x >= leftMargin) {
            drawTooltipInteraction(
                pos = pos,
                currentPoints = currentPoints,
                maxVal = maxVal,
                color = params.graphColor,
                surfaceColor = params.surfaceColor,
                currencyFormat = currencyFormat,
                textMeasurer = textMeasurer,
                tooltipStyle = tooltipStyle,
                leftMargin = leftMargin,
                topPadding = topPadding,
                bottomMargin = bottomMargin
            )
        }
    }
}

@Preview(showBackground = true, name = "Tooltip Preview")
@Preview(
    showBackground = true, name = "Tooltip Preview",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun PreviewBudgetGraphTooltip() {
    MinusTheme {
        Surface {
            Box(Modifier.padding(16.dp)) {
                GraphCanvas(
                    currentPoints = listOf(
                        BigDecimal("10"), BigDecimal("25"), BigDecimal("45"),
                        BigDecimal("30"), BigDecimal("60"), BigDecimal("50"), BigDecimal("80")
                    ),
                    previousPoints = emptyList(),
                    maxVal = 100f,
                    oldMaxVal = 100f,
                    newMaxVal = 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    currencyCode = "USD",
                    forcedTouchPosition = Offset(200f, 100f),
                    startDate = Date(),
                    currentWindowIndex = 0,
                    oldWindowIndex = 0,
                    scrollStep = 1,
                    oldScrollStep = 1,
                    renderDataSize = 7,
                    oldDataSize = 7,
                    animProgress = 1f,
                    dateFormatter = DateTimeFormatter.ofPattern("dd MMM")
                )
            }
        }
    }
}
