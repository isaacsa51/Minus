package com.serranoie.app.minus.presentation.ui.theme.component.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.font.format.isZero
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

private data class ChartPoint(
    val date: LocalDate,
    val amount: BigDecimal,
    val categoryLabel: String,
)

/**
 * A detailed chart that renders points connected by lines, with labels for each value.
 *
 * When [onPointClick] is provided, tapping or holding a point shows a tooltip with that point's
 * category (resolved from [categories]) instead of its amount — the amount is already shown as
 * the point's permanent label — and releasing the tap invokes [onPointClick] with the point's date.
 *
 * [selectedDate], when set, shows that same category tooltip for the matching point even without
 * a direct touch — e.g. driven by an external selection like tapping a day on a calendar. An
 * active touch always takes priority over it.
 */
@Composable
fun DetailedChart(
    modifier: Modifier = Modifier,
    spends: List<Transaction>,
    categories: List<Category> = emptyList(),
    currencyCode: String = "USD",
    graphColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant,
    chartPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 32.dp),
    onPointClick: ((LocalDate) -> Unit)? = null,
    selectedDate: LocalDate? = null,
    forcedTouchPosition: Offset? = null,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle =
        MaterialTheme.typography.labelSmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    val pointLabelStyle =
        MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    val surfaceColor = MaterialTheme.colorScheme.surface
    val tooltipStyle = MaterialTheme.typography.labelSmallEmphasized.copy(
        color = Color.White,
        fontWeight = FontWeight.Bold,
    )
    val emptyStateStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    val localDensity = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val topOffset = with(localDensity) { chartPadding.calculateTopPadding().toPx() }
    val bottomOffset = with(localDensity) { chartPadding.calculateBottomPadding().toPx() }
    val startOffset = with(localDensity) { chartPadding.calculateStartPadding(layoutDirection).toPx() }
    val endOffset = with(localDensity) { chartPadding.calculateEndPadding(layoutDirection).toPx() }
    val leftMarginPx = with(localDensity) { 48.dp.toPx() }

    val uncategorizedLabel = stringResource(R.string.categories_chart_uncategorized)
    val noExpensesLabel = stringResource(R.string.day_no_expenses)

    val aggregatedData =
        remember(spends, categories, uncategorizedLabel) {
            spends
                .filter { it.date != null }
                .groupBy { it.date!!.toLocalDate() }
                .toSortedMap()
                .map { (date, transactions) ->
                    val categoryLabel = transactions
                        .map { tx -> categories.find { it.id == tx.categoryId }?.name ?: uncategorizedLabel }
                        .distinct()
                        .joinToString(", ")
                    ChartPoint(
                        date = date,
                        amount = transactions.sumOf { it.amount },
                        categoryLabel = categoryLabel,
                    )
                }
        }

    val rawMax = aggregatedData.maxOfOrNull { it.amount } ?: BigDecimal.ZERO
    val maxAmount = if (rawMax.isZero()) BigDecimal("100") else rawMax.multiply(BigDecimal("1.25")).setScale(0, RoundingMode.CEILING)
    val minAmount = BigDecimal.ZERO
    val range = maxAmount

    val currencyFormat = remember(currencyCode) { symbolOnlyCurrencyFormat(currencyCode, 0) }

    fun formatValue(value: BigDecimal): String =
        when {
            value >= BigDecimal("1000000") -> "${currencyFormat.format(value.divide(BigDecimal("1000000"), 1, RoundingMode.HALF_EVEN))}M"
            value >= BigDecimal("1000") -> "${currencyFormat.format(value.divide(BigDecimal("1000"), 0, RoundingMode.HALF_EVEN))}K"
            else -> currencyFormat.format(value)
        }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var touchPosition by remember(forcedTouchPosition) { mutableStateOf(forcedTouchPosition) }
    var confirmedTapOffset by remember { mutableStateOf<Offset?>(null) }

    val points = remember(aggregatedData, canvasSize, topOffset, bottomOffset, startOffset, endOffset, leftMarginPx) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return@remember emptyList()
        val drawableWidth = canvasSize.width - startOffset - endOffset - leftMarginPx
        val drawableHeight = canvasSize.height - topOffset - bottomOffset
        aggregatedData.mapIndexed { index, chartPoint ->
            val xFraction = if (aggregatedData.size > 1) index.toFloat() / (aggregatedData.size - 1) else 0.5f
            val x = startOffset + leftMarginPx + (xFraction * drawableWidth)
            val yFraction = (chartPoint.amount - minAmount).divide(range, 4, RoundingMode.HALF_EVEN).toFloat()
            val y = topOffset + drawableHeight - (yFraction * drawableHeight)
            Offset(x, y)
        }
    }

    val activeIndex: Int? = when {
        touchPosition != null && points.isNotEmpty() ->
            points.indices.minByOrNull { abs(points[it].x - touchPosition!!.x) }

        selectedDate != null ->
            aggregatedData.indexOfFirst { it.date == selectedDate }.takeIf { it >= 0 }

        else -> null
    }

    LaunchedEffect(confirmedTapOffset) {
        val offset = confirmedTapOffset ?: return@LaunchedEffect
        val callback = onPointClick
        if (callback != null && points.isNotEmpty()) {
            val nearestIndex = points.indices.minByOrNull { abs(points[it].x - offset.x) } ?: 0
            callback(aggregatedData[nearestIndex].date)
        }
        confirmedTapOffset = null
    }

    val gestureModifier = if (onPointClick != null) {
        Modifier
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
                    onTap = { offset ->
                        touchPosition = offset
                        confirmedTapOffset = offset
                    },
                )
            }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .then(gestureModifier),
    ) {
        val width = size.width
        val height = size.height

        val drawableWidth = width - startOffset - endOffset - leftMarginPx
        val drawableHeight = height - topOffset - bottomOffset

        val gridLinesCount = 5
        for (i in 0 until gridLinesCount) {
            val fraction = i.toFloat() / (gridLinesCount - 1)
            val y = topOffset + drawableHeight - (fraction * drawableHeight)
            val value = range.multiply(BigDecimal(i)).divide(BigDecimal(gridLinesCount - 1), 0, RoundingMode.HALF_EVEN)

            drawLine(
                color = gridColor,
                start = Offset(startOffset + leftMarginPx, y),
                end = Offset(width - endOffset, y),
                strokeWidth = 1.dp.toPx(),
            )

            val textLayoutResult = textMeasurer.measure(formatValue(value), labelStyle)
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(startOffset, y - textLayoutResult.size.height / 2),
            )
        }

        if (aggregatedData.isEmpty()) {
            val emptyTextLayout = textMeasurer.measure(noExpensesLabel, emptyStateStyle)
            drawText(
                textLayoutResult = emptyTextLayout,
                topLeft = Offset(
                    x = startOffset + leftMarginPx + (drawableWidth - emptyTextLayout.size.width) / 2f,
                    y = topOffset + (drawableHeight - emptyTextLayout.size.height) / 2f,
                ),
            )
        }

        if (points.size > 1) {
            val path =
                Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
            drawPath(path = path, color = graphColor, style = Stroke(width = 2.dp.toPx()))
        }

        points.forEachIndexed { index, point ->
            drawCircle(
                color = surfaceColor,
                radius = 3.dp.toPx(),
                center = point,
            )
            drawCircle(
                color = graphColor,
                radius = 5.dp.toPx(),
                center = point,
                style = Stroke(width = 2.dp.toPx()),
            )

            val amount = aggregatedData[index].amount
            val labelText = formatValue(amount)
            val textLayoutResult = textMeasurer.measure(labelText, pointLabelStyle)

            val labelX = point.x - textLayoutResult.size.width / 2
            val labelY = point.y - textLayoutResult.size.height - 6.dp.toPx()

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(labelX, labelY),
            )
        }

        activeIndex?.let { index ->
            val point = points.getOrNull(index)
            if (point != null) {
                val categoryLabel = aggregatedData[index].categoryLabel

                drawLine(
                    color = graphColor.copy(alpha = 0.5f),
                    start = Offset(point.x, topOffset),
                    end = Offset(point.x, height - bottomOffset),
                    strokeWidth = 1.dp.toPx(),
                )
                drawCircle(color = graphColor, radius = 7.dp.toPx(), center = point)
                drawCircle(color = surfaceColor, radius = 3.5.dp.toPx(), center = point)

                val tooltipText = categoryLabel
                val tooltipTextLayout = textMeasurer.measure(text = tooltipText, style = tooltipStyle)
                val tooltipWidth = tooltipTextLayout.size.width + 16.dp.toPx()
                val tooltipHeight = tooltipTextLayout.size.height + 8.dp.toPx()

                val tooltipX = (point.x - tooltipWidth / 2).coerceIn(0f, width - tooltipWidth)
                val tooltipY = (point.y - tooltipHeight - 20.dp.toPx()).coerceAtLeast(0f)

                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.8f),
                    topLeft = Offset(tooltipX, tooltipY),
                    size = androidx.compose.ui.geometry.Size(tooltipWidth, tooltipHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = tooltipText,
                    style = tooltipStyle,
                    topLeft = Offset(tooltipX + 8.dp.toPx(), tooltipY + 4.dp.toPx()),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewDetailedChart() {
    MinusTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DetailedChart(
                spends =
                    listOf(
                        Transaction(amount = BigDecimal("150.00"), date = LocalDateTime.now().minusDays(2)),
                        Transaction(amount = BigDecimal("85.50"), date = LocalDateTime.now().minusDays(1)),
                        Transaction(amount = BigDecimal("120.00"), date = LocalDateTime.now()),
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(250.dp),
            )
        }
    }
}
