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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.math.roundToInt

@Stable
private class CategoryChartTransitionState(
    initialEntries: List<CategoryDayEntry>,
    initialDayTotals: Map<LocalDate, BigDecimal>,
    initialWindowIndex: Int,
    initialScrollStep: Int,
    initialDataSize: Int,
) {
    private var targetEntries by mutableStateOf(initialEntries)
    private var targetDayTotals by mutableStateOf(initialDayTotals)
    private var targetWindowIndex by mutableIntStateOf(initialWindowIndex)
    private var targetScrollStep by mutableIntStateOf(initialScrollStep)
    private var targetDataSize by mutableIntStateOf(initialDataSize)

    var renderEntries by mutableStateOf(initialEntries)
        private set
    var renderDayTotals by mutableStateOf(initialDayTotals)
        private set
    var renderWindowIndex by mutableIntStateOf(initialWindowIndex)
        private set
    var renderScrollStep by mutableIntStateOf(initialScrollStep)
        private set
    var renderDataSize by mutableIntStateOf(initialDataSize)
        private set

    var oldEntries by mutableStateOf(initialEntries)
        private set
    var oldDayTotals by mutableStateOf(initialDayTotals)
        private set
    var oldWindowIndex by mutableIntStateOf(initialWindowIndex)
        private set
    var oldScrollStep by mutableIntStateOf(initialScrollStep)
        private set
    var oldDataSize by mutableIntStateOf(initialDataSize)
        private set

    var direction by mutableIntStateOf(1)
        private set

    val animProgress = Animatable(1f)

    fun updateTarget(
        entries: List<CategoryDayEntry>,
        dayTotals: Map<LocalDate, BigDecimal>,
        windowIndex: Int,
        scrollStep: Int,
        dataSize: Int,
    ) {
        targetEntries = entries
        targetDayTotals = dayTotals
        targetWindowIndex = windowIndex
        targetScrollStep = scrollStep
        targetDataSize = dataSize
    }

    suspend fun reconcile(animDuration: Int = 600) {
        val layoutChanged = targetWindowIndex != renderWindowIndex ||
            targetScrollStep != renderScrollStep ||
            targetDataSize != renderDataSize

        if (layoutChanged) {
            direction = when {
                targetWindowIndex > renderWindowIndex -> 1
                targetWindowIndex < renderWindowIndex -> -1
                else -> direction
            }

            oldEntries = renderEntries
            oldDayTotals = renderDayTotals
            oldWindowIndex = renderWindowIndex
            oldScrollStep = renderScrollStep
            oldDataSize = renderDataSize

            renderEntries = targetEntries
            renderDayTotals = targetDayTotals
            renderWindowIndex = targetWindowIndex
            renderScrollStep = targetScrollStep
            renderDataSize = targetDataSize

            animProgress.snapTo(0f)
            animProgress.animateTo(1f, animationSpec = tween(animDuration))
        } else if (targetEntries != renderEntries || targetDayTotals != renderDayTotals) {
            renderEntries = targetEntries
            renderDayTotals = targetDayTotals
        }
    }
}

@Composable
private fun rememberCategoryChartTransitionState(
    entries: List<CategoryDayEntry>,
    dayTotals: Map<LocalDate, BigDecimal>,
    windowIndex: Int,
    scrollStep: Int,
    dataSize: Int,
): CategoryChartTransitionState {
    val state = remember {
        CategoryChartTransitionState(
            initialEntries = entries,
            initialDayTotals = dayTotals,
            initialWindowIndex = windowIndex,
            initialScrollStep = scrollStep,
            initialDataSize = dataSize,
        )
    }
    LaunchedEffect(entries, dayTotals, windowIndex, scrollStep, dataSize) {
        state.updateTarget(entries, dayTotals, windowIndex, scrollStep, dataSize)
        state.reconcile()
    }
    return state
}

private fun categoryChartMaxVal(dayTotals: Map<LocalDate, BigDecimal>): Float {
    val rawMax = dayTotals.values.maxOrNull() ?: BigDecimal.ZERO
    return if (rawMax <= BigDecimal.ZERO) 100f else rawMax.multiply(BigDecimal("1.25")).toFloat()
}

private fun categoryEntriesByDayIndex(
    entries: List<CategoryDayEntry>,
    startLocalDate: LocalDate,
    windowStartIndex: Int,
    dataSize: Int,
): Map<Int, List<CategoryDayEntry>> = (0 until dataSize).associateWith { index ->
    val date = startLocalDate.plusDays((windowStartIndex + index).toLong())
    entries.filter { it.date == date }.sortedBy { it.label }
}

/**
 * Renders one day's spending as 24 hourly stacked-category bars instead of [MultiCategoryChart]'s
 * one-bar-per-day-across-a-window — this is what [GraphGranularity.DAYS] shows in categories mode,
 * since paginating through several individual days doesn't apply once the window is a single day.
 * Cross-fades between days the same way [MultiCategoryChart] cross-fades between windows.
 *
 * Bars don't show their total permanently — press and hold (optionally dragging across bars,
 * the same way [GraphCanvas]'s tooltip works) reveals each bar's total as you touch it, and
 * double-tapping a bar opens that day's transactions via [onDayTap]. [forcedTooltipDate] forces
 * the tooltip open for previewing its appearance.
 */
@Composable
internal fun MultiCategoryChart(
    entries: List<CategoryDayEntry>,
    dayTotals: Map<LocalDate, BigDecimal>,
    modifier: Modifier = Modifier,
    currencyCode: String,
    startDate: Date,
    windowIndex: Int,
    scrollStep: Int,
    dataSize: Int,
    dateFormatter: DateTimeFormatter,
    selectedDate: LocalDate? = null,
    onDayTap: ((LocalDate) -> Unit)? = null,
    forcedTooltipDate: LocalDate? = null,
) {
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surface
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

    val startLocalDate = remember(startDate) {
        startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val windowStartIndex = windowIndex * scrollStep

    val transitionState = rememberCategoryChartTransitionState(
        entries = entries,
        dayTotals = dayTotals,
        windowIndex = windowIndex,
        scrollStep = scrollStep,
        dataSize = dataSize,
    )

    val renderEntriesByDayIndex = remember(
        transitionState.renderEntries,
        startLocalDate,
        transitionState.renderWindowIndex,
        transitionState.renderScrollStep,
        transitionState.renderDataSize,
    ) {
        categoryEntriesByDayIndex(
            entries = transitionState.renderEntries,
            startLocalDate = startLocalDate,
            windowStartIndex = transitionState.renderWindowIndex * transitionState.renderScrollStep,
            dataSize = transitionState.renderDataSize,
        )
    }
    val oldEntriesByDayIndex = remember(
        transitionState.oldEntries,
        startLocalDate,
        transitionState.oldWindowIndex,
        transitionState.oldScrollStep,
        transitionState.oldDataSize,
    ) {
        categoryEntriesByDayIndex(
            entries = transitionState.oldEntries,
            startLocalDate = startLocalDate,
            windowStartIndex = transitionState.oldWindowIndex * transitionState.oldScrollStep,
            dataSize = transitionState.oldDataSize,
        )
    }

    val renderMaxVal = remember(transitionState.renderDayTotals) {
        categoryChartMaxVal(transitionState.renderDayTotals)
    }
    val oldMaxVal = remember(transitionState.oldDayTotals) {
        categoryChartMaxVal(transitionState.oldDayTotals)
    }

    var touchPosition by remember { mutableStateOf<Offset?>(null) }

    val gestureModifier = Modifier
        .pointerInput(windowIndex, scrollStep, dataSize, startDate) {
            detectDragGestures(
                onDragStart = { offset -> touchPosition = offset },
                onDrag = { change, _ -> touchPosition = change.position },
                onDragEnd = { touchPosition = null },
                onDragCancel = { touchPosition = null },
            )
        }
        .pointerInput(windowIndex, scrollStep, dataSize, startDate) {
            fun resolveDate(offset: Offset): LocalDate? {
                val leftMargin = 42.dp.toPx()
                if (offset.x < leftMargin) return null
                val drawableWidth = size.width - leftMargin
                val stepWidth = drawableWidth / (dataSize - 1).coerceAtLeast(1)
                val index = ((offset.x - leftMargin) / stepWidth).roundToInt().coerceIn(0, dataSize - 1)
                return startLocalDate.plusDays((windowStartIndex + index).toLong())
            }

            detectTapGestures(
                onPress = { offset ->
                    touchPosition = offset
                    tryAwaitRelease()
                    touchPosition = null
                },
                onDoubleTap = { offset ->
                    touchPosition = null
                    val date = resolveDate(offset) ?: return@detectTapGestures
                    onDayTap?.invoke(date)
                },
            )
        }

    Canvas(modifier = modifier.then(gestureModifier)) {
        val width = size.width
        val height = size.height
        val leftMargin = 42.dp.toPx()
        val bottomMargin = 20.dp.toPx()
        val topPadding = 10.dp.toPx()
        val drawableHeight = height - bottomMargin - topPadding
        val drawableWidth = width - leftMargin
        val segmentGap = 1.dp.toPx()
        val maxSegmentCornerRadius = 5.dp.toPx()
        val baseline = height - bottomMargin

        val progress = transitionState.animProgress.value
        val oldStepWidth = drawableWidth / (transitionState.oldDataSize - 1).coerceAtLeast(1)
        val newStepWidth = drawableWidth / (transitionState.renderDataSize - 1).coerceAtLeast(1)
        val stepWidth = lerp(oldStepWidth, newStepWidth, progress)
        val barWidth = (stepWidth * 0.55f).coerceAtMost(28.dp.toPx())

        val oldAlpha = (1f - progress).coerceIn(0f, 1f)
        val newAlpha = progress.coerceIn(0f, 1f)
        val slideDirection = transitionState.direction.toFloat()
        val oldOffsetX = -width * slideDirection * progress
        val newOffsetX = width * slideDirection * (1f - progress)

        if (oldAlpha > 0f && progress < 1f) {
            drawCoordinateSystem(
                params = drawParams,
                textMeasurer = textMeasurer,
                currencyFormat = currencyFormat,
                dateFormatter = dateFormatter,
                startDate = startDate,
                windowIndex = transitionState.oldWindowIndex,
                scrollStep = transitionState.oldScrollStep,
                dataSize = transitionState.oldDataSize,
                maxVal = oldMaxVal,
                alpha = oldAlpha,
                thousandsUnit = thousandsUnit,
                millionsUnit = millionsUnit,
            )
            translate(left = oldOffsetX) {
                drawCategoryBars(
                    entriesByDayIndex = oldEntriesByDayIndex,
                    startLocalDate = startLocalDate,
                    windowStartIndex = transitionState.oldWindowIndex * transitionState.oldScrollStep,
                    dataSize = transitionState.oldDataSize,
                    leftMargin = leftMargin,
                    baseline = baseline,
                    topPadding = topPadding,
                    stepWidth = stepWidth,
                    barWidth = barWidth,
                    segmentGap = segmentGap,
                    maxSegmentCornerRadius = maxSegmentCornerRadius,
                    drawableHeight = drawableHeight,
                    maxVal = oldMaxVal,
                    alpha = oldAlpha,
                    selectedDate = null,
                    tertiaryColor = tertiaryColor,
                )
            }
        }

        if (newAlpha > 0f) {
            drawCoordinateSystem(
                params = drawParams,
                textMeasurer = textMeasurer,
                currencyFormat = currencyFormat,
                dateFormatter = dateFormatter,
                startDate = startDate,
                windowIndex = transitionState.renderWindowIndex,
                scrollStep = transitionState.renderScrollStep,
                dataSize = transitionState.renderDataSize,
                maxVal = renderMaxVal,
                alpha = newAlpha,
                thousandsUnit = thousandsUnit,
                millionsUnit = millionsUnit,
                isTodayHighlighted = true,
            )
            translate(left = newOffsetX) {
                drawCategoryBars(
                    entriesByDayIndex = renderEntriesByDayIndex,
                    startLocalDate = startLocalDate,
                    windowStartIndex = transitionState.renderWindowIndex * transitionState.renderScrollStep,
                    dataSize = transitionState.renderDataSize,
                    leftMargin = leftMargin,
                    baseline = baseline,
                    topPadding = topPadding,
                    stepWidth = stepWidth,
                    barWidth = barWidth,
                    segmentGap = segmentGap,
                    maxSegmentCornerRadius = maxSegmentCornerRadius,
                    drawableHeight = drawableHeight,
                    maxVal = renderMaxVal,
                    alpha = newAlpha,
                    selectedDate = selectedDate,
                    tertiaryColor = tertiaryColor,
                )
            }
        }

        if (progress >= 1f) {
            val renderWindowStartIndex = transitionState.renderWindowIndex * transitionState.renderScrollStep
            val touchedDayOffset = if (forcedTooltipDate != null) {
                ChronoUnit.DAYS.between(startLocalDate, forcedTooltipDate).toInt() - renderWindowStartIndex
            } else {
                touchPosition?.let { pos ->
                    if (pos.x < leftMargin) null
                    else ((pos.x - leftMargin) / stepWidth).roundToInt()
                        .coerceIn(0, transitionState.renderDataSize - 1)
                }
            }

            if (touchedDayOffset != null && touchedDayOffset in 0 until transitionState.renderDataSize) {
                val date = startLocalDate.plusDays((renderWindowStartIndex + touchedDayOffset).toLong())
                val total = transitionState.renderDayTotals[date] ?: BigDecimal.ZERO
                if (total > BigDecimal.ZERO) {
                    val x = leftMargin + touchedDayOffset * stepWidth
                    val barTopY = baseline - (total.toFloat() / renderMaxVal * drawableHeight).coerceAtLeast(1f)
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

private fun DrawScope.drawCategoryBars(
    entriesByDayIndex: Map<Int, List<CategoryDayEntry>>,
    startLocalDate: LocalDate,
    windowStartIndex: Int,
    dataSize: Int,
    leftMargin: Float,
    baseline: Float,
    topPadding: Float,
    stepWidth: Float,
    barWidth: Float,
    segmentGap: Float,
    maxSegmentCornerRadius: Float,
    drawableHeight: Float,
    maxVal: Float,
    alpha: Float,
    selectedDate: LocalDate?,
    tertiaryColor: Color,
) {
    for (index in 0 until dataSize) {
        val dayEntries = entriesByDayIndex[index].orEmpty()
        if (dayEntries.isEmpty()) continue

        val x = leftMargin + index * stepWidth
        val date = startLocalDate.plusDays((windowStartIndex + index).toLong())

        if (date == selectedDate) {
            drawRoundRect(
                color = tertiaryColor.copy(alpha = 0.15f * alpha),
                topLeft = Offset(x - barWidth / 2 - 4.dp.toPx(), topPadding),
                size = Size(barWidth + 8.dp.toPx(), baseline - topPadding),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
        }

        var segmentBottom = baseline
        dayEntries.forEach { entry ->
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

internal fun DrawScope.drawCategoryTooltip(
    x: Float,
    barTopY: Float,
    baseline: Float,
    topPadding: Float,
    width: Float,
    text: String,
    textMeasurer: TextMeasurer,
    tooltipStyle: TextStyle,
    lineColor: Color,
) {
    drawLine(
        color = lineColor.copy(alpha = 0.5f),
        start = Offset(x, topPadding),
        end = Offset(x, baseline),
        strokeWidth = 1.dp.toPx(),
    )

    val textLayoutResult = textMeasurer.measure(text, tooltipStyle)
    val tooltipWidth = textLayoutResult.size.width + 16.dp.toPx()
    val tooltipHeight = textLayoutResult.size.height + 8.dp.toPx()
    val tooltipX = (x - tooltipWidth / 2).coerceIn(0f, width - tooltipWidth)
    val tooltipY = (barTopY - tooltipHeight - 8.dp.toPx()).coerceAtLeast(topPadding)

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.8f),
        topLeft = Offset(tooltipX, tooltipY),
        size = Size(tooltipWidth, tooltipHeight),
        cornerRadius = CornerRadius(8.dp.toPx()),
    )
    drawText(
        textMeasurer = textMeasurer,
        text = text,
        style = tooltipStyle,
        topLeft = Offset(tooltipX + 8.dp.toPx(), tooltipY + 4.dp.toPx()),
    )
}

private val previewCategoryColors = listOf(
    Color(0xFFF86BAE), // Food
    Color(0xFF5FC7E7), // Transport
    Color(0xFFFFD386), // Shopping
)

@Preview(showBackground = true, name = "Multi-Category Chart")
@Preview(
    showBackground = true, name = "Multi-Category Chart",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun PreviewMultiCategoryChart() {
    val startLocalDate = LocalDate.now().minusDays(6)
    val startDate = Date.from(startLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant())

    val entries = listOf(
        CategoryDayEntry(startLocalDate.plusDays(0), "Food", BigDecimal("18"), previewCategoryColors[0]),
        CategoryDayEntry(startLocalDate.plusDays(0), "Transport", BigDecimal("6"), previewCategoryColors[1]),
        CategoryDayEntry(startLocalDate.plusDays(1), "Shopping", BigDecimal("42"), previewCategoryColors[2]),
        CategoryDayEntry(startLocalDate.plusDays(3), "Food", BigDecimal("12"), previewCategoryColors[0]),
        CategoryDayEntry(startLocalDate.plusDays(3), "Shopping", BigDecimal("20"), previewCategoryColors[2]),
        CategoryDayEntry(startLocalDate.plusDays(4), "Transport", BigDecimal("30"), previewCategoryColors[1]),
        CategoryDayEntry(startLocalDate.plusDays(6), "Food", BigDecimal("15"), previewCategoryColors[0]),
        CategoryDayEntry(startLocalDate.plusDays(6), "Transport", BigDecimal("10"), previewCategoryColors[1]),
        CategoryDayEntry(startLocalDate.plusDays(6), "Shopping", BigDecimal("25"), previewCategoryColors[2]),
    )
    val dayTotals = entries.groupBy { it.date }.mapValues { (_, dayEntries) -> dayEntries.sumOf { it.amount } }

    MinusTheme {
        Surface {
            Box(Modifier.padding(16.dp)) {
                MultiCategoryChart(
                    entries = entries,
                    dayTotals = dayTotals,
                    currencyCode = "USD",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    startDate = startDate,
                    windowIndex = 0,
                    scrollStep = 1,
                    dataSize = 7,
                    dateFormatter = DateTimeFormatter.ofPattern("dd MMM"),
                    selectedDate = startLocalDate.plusDays(6),
                    forcedTooltipDate = startLocalDate.plusDays(6),
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Multi-Category Chart - Sparse")
@Preview(
    showBackground = true, name = "Multi-Category Chart - Sparse",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun PreviewMultiCategoryChartSparse() {
    val startLocalDate = LocalDate.now().minusDays(6)
    val startDate = Date.from(startLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant())

    val entries = listOf(
        CategoryDayEntry(startLocalDate.plusDays(2), "Food", BigDecimal("8"), previewCategoryColors[0]),
        CategoryDayEntry(startLocalDate.plusDays(5), "Shopping", BigDecimal("60"), previewCategoryColors[2]),
    )
    val dayTotals = entries.groupBy { it.date }.mapValues { (_, dayEntries) -> dayEntries.sumOf { it.amount } }

    MinusTheme {
        Surface {
            Box(Modifier.padding(16.dp)) {
                MultiCategoryChart(
                    entries = entries,
                    dayTotals = dayTotals,
                    currencyCode = "USD",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    startDate = startDate,
                    windowIndex = 0,
                    scrollStep = 1,
                    dataSize = 7,
                    dateFormatter = DateTimeFormatter.ofPattern("dd MMM"),
                )
            }
        }
    }
}
