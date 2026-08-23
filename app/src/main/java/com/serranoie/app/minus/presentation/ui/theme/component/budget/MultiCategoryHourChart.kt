package com.serranoie.app.minus.presentation.ui.theme.component.budget

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

private fun categoryHourEntriesMaxVal(entries: List<CategoryHourEntry>): Float {
    val rawMax = entries.groupBy { it.hour }
        .mapValues { (_, hourEntries) -> hourEntries.sumOf { it.amount } }
        .values
        .maxOrNull() ?: BigDecimal.ZERO
    return if (rawMax <= BigDecimal.ZERO) 100f else rawMax.multiply(BigDecimal("1.25")).toFloat()
}

private fun categoryHourEntriesByHour(entries: List<CategoryHourEntry>): Map<Int, List<CategoryHourEntry>> =
    (0 until 24).associateWith { hour -> entries.filter { it.hour == hour }.sortedBy { it.label } }

@Stable
private class CategoryHourTransitionState(
    initialEntries: List<CategoryHourEntry>,
    initialDate: LocalDate,
) {
    var renderEntries by mutableStateOf(initialEntries)
        private set
    var oldEntries by mutableStateOf(initialEntries)
        private set
    var renderDate by mutableStateOf(initialDate)
        private set
    var direction by mutableIntStateOf(1)
        private set

    val animProgress = Animatable(1f)

    suspend fun updateTo(
        newEntries: List<CategoryHourEntry>,
        newDate: LocalDate,
        animDuration: Int = 600,
    ) {
        if (newDate == renderDate) return
        direction = if (newDate.isAfter(renderDate)) 1 else -1
        oldEntries = renderEntries
        renderEntries = newEntries
        renderDate = newDate
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(animDuration))
    }
}

@Composable
private fun rememberCategoryHourTransitionState(
    entries: List<CategoryHourEntry>,
    date: LocalDate,
): CategoryHourTransitionState {
    val state = remember { CategoryHourTransitionState(entries, date) }
    LaunchedEffect(entries, date) { state.updateTo(entries, date) }
    return state
}

@Composable
internal fun MultiCategoryHourChart(
    entries: List<CategoryHourEntry>,
    date: LocalDate,
    modifier: Modifier = Modifier,
    currencyCode: String,
    isToday: Boolean = false,
) {
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    val currencyFormat = remember(currencyCode) { symbolOnlyCurrencyFormat(currencyCode) }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmallCondensed.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )
    val tooltipStyle = MaterialTheme.typography.labelSmallEmphasized.copy(
        color = Color.White,
        fontWeight = FontWeight.Bold,
    )

    val transitionState = rememberCategoryHourTransitionState(entries, date)

    var touchPosition by remember { mutableStateOf<Offset?>(null) }

    val gestureModifier = Modifier
        .pointerInput(date) {
            detectDragGestures(
                onDragStart = { offset -> touchPosition = offset },
                onDrag = { change, _ -> touchPosition = change.position },
                onDragEnd = { touchPosition = null },
                onDragCancel = { touchPosition = null },
            )
        }
        .pointerInput(date) {
            detectTapGestures(
                onPress = { offset ->
                    touchPosition = offset
                    tryAwaitRelease()
                    touchPosition = null
                },
            )
        }

    val renderEntriesByHour = remember(transitionState.renderEntries) {
        categoryHourEntriesByHour(transitionState.renderEntries)
    }
    val oldEntriesByHour = remember(transitionState.oldEntries) {
        categoryHourEntriesByHour(transitionState.oldEntries)
    }
    val renderMaxVal = remember(transitionState.renderEntries) {
        categoryHourEntriesMaxVal(transitionState.renderEntries)
    }
    val oldMaxVal = remember(transitionState.oldEntries) {
        categoryHourEntriesMaxVal(transitionState.oldEntries)
    }

    Canvas(modifier = modifier.then(gestureModifier)) {
        val width = size.width
        val height = size.height
        val leftMargin = 42.dp.toPx()
        val bottomMargin = 20.dp.toPx()
        val topPadding = 10.dp.toPx()
        val drawableHeight = height - bottomMargin - topPadding
        val drawableWidth = width - leftMargin
        val stepWidth = drawableWidth / 23
        val barWidth = (stepWidth * 0.55f).coerceAtMost(14.dp.toPx())
        val segmentGap = 1.dp.toPx()
        val maxSegmentCornerRadius = 4.dp.toPx()
        val baseline = height - bottomMargin

        val progress = transitionState.animProgress.value
        val oldAlpha = (1f - progress).coerceIn(0f, 1f)
        val newAlpha = progress.coerceIn(0f, 1f)
        val direction = transitionState.direction.toFloat()
        val oldOffsetX = -width * direction * progress
        val newOffsetX = width * direction * (1f - progress)

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

        if (oldAlpha > 0f && progress < 1f) {
            drawHourGridlines(
                leftMargin = leftMargin,
                baseline = baseline,
                drawableWidth = drawableWidth,
                drawableHeight = drawableHeight,
                width = width,
                maxVal = oldMaxVal,
                gridColor = gridColor,
                textMeasurer = textMeasurer,
                labelStyle = labelStyle,
                currencyFormat = currencyFormat,
                alpha = oldAlpha,
            )
            translate(left = oldOffsetX) {
                drawCategoryHourBars(
                    entriesByHour = oldEntriesByHour,
                    leftMargin = leftMargin,
                    baseline = baseline,
                    stepWidth = stepWidth,
                    barWidth = barWidth,
                    segmentGap = segmentGap,
                    maxSegmentCornerRadius = maxSegmentCornerRadius,
                    drawableHeight = drawableHeight,
                    maxVal = oldMaxVal,
                    alpha = oldAlpha,
                )
            }
        }

        if (newAlpha > 0f) {
            drawHourGridlines(
                leftMargin = leftMargin,
                baseline = baseline,
                drawableWidth = drawableWidth,
                drawableHeight = drawableHeight,
                width = width,
                maxVal = renderMaxVal,
                gridColor = gridColor,
                textMeasurer = textMeasurer,
                labelStyle = labelStyle,
                currencyFormat = currencyFormat,
                alpha = newAlpha,
            )
            translate(left = newOffsetX) {
                drawCategoryHourBars(
                    entriesByHour = renderEntriesByHour,
                    leftMargin = leftMargin,
                    baseline = baseline,
                    stepWidth = stepWidth,
                    barWidth = barWidth,
                    segmentGap = segmentGap,
                    maxSegmentCornerRadius = maxSegmentCornerRadius,
                    drawableHeight = drawableHeight,
                    maxVal = renderMaxVal,
                    alpha = newAlpha,
                )
            }
        }

        if (progress >= 1f) {
            val touchedHour = touchPosition?.let { pos ->
                if (pos.x < leftMargin) null
                else ((pos.x - leftMargin) / stepWidth).roundToInt().coerceIn(0, 23)
            }

            if (touchedHour != null) {
                val total = transitionState.renderEntries
                    .filter { it.hour == touchedHour }
                    .sumOf { it.amount }
                if (total > BigDecimal.ZERO) {
                    val x = leftMargin + touchedHour * stepWidth
                    val barTopY =
                        baseline - (total.toFloat() / renderMaxVal * drawableHeight).coerceAtLeast(1f)
                    drawCategoryTooltip(
                        x = x,
                        barTopY = barTopY,
                        baseline = baseline,
                        topPadding = topPadding,
                        width = width,
                        text = currencyFormat.format(total),
                        textMeasurer = textMeasurer,
                        tooltipStyle = tooltipStyle,
                        lineColor = tertiaryColor,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawCategoryHourBars(
    entriesByHour: Map<Int, List<CategoryHourEntry>>,
    leftMargin: Float,
    baseline: Float,
    stepWidth: Float,
    barWidth: Float,
    segmentGap: Float,
    maxSegmentCornerRadius: Float,
    drawableHeight: Float,
    maxVal: Float,
    alpha: Float,
) {
    for (hour in 0 until 24) {
        val hourEntries = entriesByHour[hour].orEmpty()
        if (hourEntries.isEmpty()) continue

        val x = leftMargin + hour * stepWidth
        var segmentBottom = baseline
        hourEntries.forEach { entry ->
            val segmentHeight =
                (entry.amount.toFloat() / maxVal * drawableHeight).coerceAtLeast(1f)
            val segmentTop = segmentBottom - segmentHeight
            val cornerRadiusPx =
                maxSegmentCornerRadius.coerceAtMost(minOf(barWidth, segmentHeight) / 2f)
            drawRoundRect(
                color = entry.color.copy(alpha = entry.color.alpha * alpha),
                topLeft = Offset(x - barWidth / 2, segmentTop),
                size = Size(barWidth, segmentHeight),
                cornerRadius = CornerRadius(cornerRadiusPx),
            )
            segmentBottom = segmentTop - segmentGap
        }
    }
}

private val previewHourCategoryColors = listOf(
    Color(0xFFF86BAE),
    Color(0xFF5FC7E7),
)

@Preview(showBackground = true, name = "Multi-Category Hour Chart")
@Preview(
    showBackground = true, name = "Multi-Category Hour Chart",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun PreviewMultiCategoryHourChart() {
    val entries = listOf(
        CategoryHourEntry(0, "Food", BigDecimal("18"), previewHourCategoryColors[0]),
        CategoryHourEntry(1, "Food", BigDecimal("4"), previewHourCategoryColors[0]),
        CategoryHourEntry(8, "Transport", BigDecimal("6"), previewHourCategoryColors[1]),
        CategoryHourEntry(9, "Transport", BigDecimal("12"), previewHourCategoryColors[1]),
        CategoryHourEntry(9, "Food", BigDecimal("22"), previewHourCategoryColors[0]),
        CategoryHourEntry(10, "Transport", BigDecimal("30"), previewHourCategoryColors[1]),
    )

    MinusTheme {
        Surface {
            Box(Modifier.padding(16.dp)) {
                MultiCategoryHourChart(
                    entries = entries,
                    date = LocalDate.now(),
                    currencyCode = "USD",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                )
            }
        }
    }
}
