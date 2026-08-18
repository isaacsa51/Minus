package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.math.roundToInt

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
) {
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val currencyFormat = remember(currencyCode) { symbolOnlyCurrencyFormat(currencyCode) }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmallCondensed.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )
    val totalLabelStyle = MaterialTheme.typography.labelSmallCondensed.copy(
        color = onSurfaceColor.copy(alpha = 0.8f),
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

    val maxVal = remember(dayTotals) {
        val rawMax = dayTotals.values.maxOrNull() ?: BigDecimal.ZERO
        if (rawMax <= BigDecimal.ZERO) 100f else rawMax.multiply(BigDecimal("1.25")).toFloat()
    }

    val startLocalDate = remember(startDate) {
        startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val windowStartIndex = windowIndex * scrollStep

    val entriesByDayIndex = remember(entries, startLocalDate, windowStartIndex, dataSize) {
        (0 until dataSize).associateWith { index ->
            val date = startLocalDate.plusDays((windowStartIndex + index).toLong())
            entries.filter { it.date == date }.sortedBy { it.label }
        }
    }

    val gestureModifier = if (onDayTap != null) {
        Modifier.pointerInput(windowIndex, scrollStep, dataSize, startDate) {
            detectTapGestures(onTap = { offset ->
                val leftMargin = 42.dp.toPx()
                if (offset.x >= leftMargin) {
                    val drawableWidth = size.width - leftMargin
                    val stepWidth = drawableWidth / (dataSize - 1).coerceAtLeast(1)
                    val index =
                        ((offset.x - leftMargin) / stepWidth).roundToInt().coerceIn(0, dataSize - 1)
                    val date = startLocalDate.plusDays((windowStartIndex + index).toLong())
                    onDayTap(date)
                }
            })
        }
    } else {
        Modifier
    }

    Canvas(modifier = modifier.then(gestureModifier)) {
        val width = size.width
        val height = size.height
        val leftMargin = 42.dp.toPx()
        val bottomMargin = 20.dp.toPx()
        val topPadding = 10.dp.toPx()
        val drawableHeight = height - bottomMargin - topPadding
        val drawableWidth = width - leftMargin
        val stepWidth = drawableWidth / (dataSize - 1).coerceAtLeast(1)
        val barWidth = (stepWidth * 0.55f).coerceAtMost(28.dp.toPx())
        val segmentGap = 3.dp.toPx()
        val maxSegmentCornerRadius = 6.dp.toPx()
        val baseline = height - bottomMargin

        drawCoordinateSystem(
            params = drawParams,
            textMeasurer = textMeasurer,
            currencyFormat = currencyFormat,
            dateFormatter = dateFormatter,
            startDate = startDate,
            windowIndex = windowIndex,
            scrollStep = scrollStep,
            dataSize = dataSize,
            maxVal = maxVal,
            alpha = 1f,
            thousandsUnit = thousandsUnit,
            millionsUnit = millionsUnit,
            isTodayHighlighted = true,
        )

        for (index in 0 until dataSize) {
            val dayEntries = entriesByDayIndex[index].orEmpty()
            if (dayEntries.isEmpty()) continue

            val x = leftMargin + index * stepWidth
            val date = startLocalDate.plusDays((windowStartIndex + index).toLong())
            val total = dayTotals[date] ?: BigDecimal.ZERO

            if (date == selectedDate) {
                drawRoundRect(
                    color = tertiaryColor.copy(alpha = 0.15f),
                    topLeft = Offset(x - barWidth / 2 - 4.dp.toPx(), topPadding),
                    size = Size(barWidth + 8.dp.toPx(), baseline - topPadding),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
            }

            var segmentBottom = baseline
            var barTop = baseline
            dayEntries.forEach { entry ->
                val segmentHeight =
                    (entry.amount.toFloat() / maxVal * drawableHeight).coerceAtLeast(1f)
                val segmentTop = segmentBottom - segmentHeight
                val cornerRadiusPx =
                    maxSegmentCornerRadius.coerceAtMost(minOf(barWidth, segmentHeight) / 2f)
                drawRoundRect(
                    color = entry.color,
                    topLeft = Offset(x - barWidth / 2, segmentTop),
                    size = Size(barWidth, segmentHeight),
                    cornerRadius = CornerRadius(cornerRadiusPx),
                )
                barTop = segmentTop
                segmentBottom = segmentTop - segmentGap
            }

            val labelText = formatAxisValue(total, currencyFormat, thousandsUnit, millionsUnit)
            val textLayoutResult = textMeasurer.measure(labelText, totalLabelStyle)
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x = x - textLayoutResult.size.width / 2,
                    y = barTop - textLayoutResult.size.height - 4.dp.toPx(),
                ),
            )
        }
    }
}
