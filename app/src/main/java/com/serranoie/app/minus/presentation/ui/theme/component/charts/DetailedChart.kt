package com.serranoie.app.minus.presentation.ui.theme.component.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.isZero
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

/**
 * A detailed chart that renders points connected by lines, with labels for each value.
 * Designed to show trends over time for specific categories.
 */
@Composable
fun DetailedChart(
    modifier: Modifier = Modifier,
    spends: List<Transaction>,
    currencyCode: String = "USD",
    graphColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant,
    chartPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 32.dp),
) {
    if (spends.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val pointLabelStyle = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    val surfaceColor = Color.Transparent

    val localDensity = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val topOffset = with(localDensity) { chartPadding.calculateTopPadding().toPx() }
    val bottomOffset = with(localDensity) { chartPadding.calculateBottomPadding().toPx() }
    val startOffset = with(localDensity) { chartPadding.calculateStartPadding(layoutDirection).toPx() }
    val endOffset = with(localDensity) { chartPadding.calculateEndPadding(layoutDirection).toPx() }

    val aggregatedData = remember(spends) {
        spends.filter { it.date != null }
            .groupBy { it.date!!.toLocalDate() }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toSortedMap()
            .toList()
    }

    if (aggregatedData.isEmpty()) return

    val rawMax = aggregatedData.maxOf { it.second }
    val maxAmount = if (rawMax.isZero()) BigDecimal("100") else rawMax.multiply(BigDecimal("1.25")).setScale(0, RoundingMode.CEILING)
    val minAmount = BigDecimal.ZERO
    val background = MaterialTheme.colorScheme.surface

    val currencyFormat = remember(currencyCode) { symbolOnlyCurrencyFormat(currencyCode, 0) }

    fun formatValue(value: BigDecimal): String {
        return when {
            value >= BigDecimal("1000000") -> "${currencyFormat.format(value.divide(BigDecimal("1000000"), 1, RoundingMode.HALF_EVEN))}M"
            value >= BigDecimal("1000") -> "${currencyFormat.format(value.divide(BigDecimal("1000"), 0, RoundingMode.HALF_EVEN))}K"
            else -> currencyFormat.format(value)
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // Reserve space on the left for Y-axis labels
        val leftMargin = 48.dp.toPx()
        val drawableWidth = width - startOffset - endOffset - leftMargin
        val drawableHeight = height - topOffset - bottomOffset
        
        // Draw horizontal grid lines and Y-axis labels
        val gridLinesCount = 5
        for (i in 0 until gridLinesCount) {
            val fraction = i.toFloat() / (gridLinesCount - 1)
            // y goes from bottom (fraction=0) to top (fraction=1)
            val y = topOffset + drawableHeight - (fraction * drawableHeight)
            val value = maxAmount.multiply(BigDecimal(i)).divide(BigDecimal(gridLinesCount - 1), 0, RoundingMode.HALF_EVEN)
            
            drawLine(
                color = gridColor,
                start = Offset(startOffset + leftMargin, y),
                end = Offset(width - endOffset, y),
                strokeWidth = 1.dp.toPx()
            )
            
            val textLayoutResult = textMeasurer.measure(formatValue(value), labelStyle)
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(startOffset, y - textLayoutResult.size.height / 2)
            )
        }
        
        // Calculate point coordinates
        val points = aggregatedData.mapIndexed { index, pair ->
            val xFraction = if (aggregatedData.size > 1) index.toFloat() / (aggregatedData.size - 1) else 0.5f
            val x = startOffset + leftMargin + (xFraction * drawableWidth)
            
            val yFraction = (pair.second - minAmount).divide(maxAmount, 4, RoundingMode.HALF_EVEN).toFloat()
            val y = topOffset + drawableHeight - (yFraction * drawableHeight)
            
            Offset(x, y)
        }
        
        // Draw connecting lines (linear)
        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(path = path, color = graphColor, style = Stroke(width = 2.dp.toPx()))
        }

        // Draw points (circles) and labels above them
        points.forEachIndexed { index, point ->
            drawCircle(
                color = background,
                radius = 3.dp.toPx(),
                center = point
            )
            // The point circle itself
            drawCircle(
                color = graphColor,
                radius = 5.dp.toPx(),
                center = point,
                style = Stroke(width = 2.dp.toPx())
            )
            
            val amount = aggregatedData[index].second
            val labelText = formatValue(amount)
            val textLayoutResult = textMeasurer.measure(labelText, pointLabelStyle)
            
            // Center label horizontally above the point
            val labelX = point.x - textLayoutResult.size.width / 2
            val labelY = point.y - textLayoutResult.size.height - 6.dp.toPx()
            
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(labelX, labelY)
            )
        }
    }
}

@Preview
@Composable
private fun PreviewDetailedChart() {
    MinusTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DetailedChart(
                spends = listOf(
                    Transaction(amount = BigDecimal("150.00"), date = LocalDateTime.now().minusDays(2)),
                    Transaction(amount = BigDecimal("85.50"), date = LocalDateTime.now().minusDays(1)),
                    Transaction(amount = BigDecimal("120.00"), date = LocalDateTime.now()),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )
        }
    }
}
