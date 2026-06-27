package com.serranoie.app.minus.presentation.ui.theme.component.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import java.math.RoundingMode
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DonutChart(
    modifier: Modifier = Modifier,
    items: List<CategoryUsage>,
    selectedIndex: Int = -1,
    holeColor: Color = Color.White,
    chartPadding: PaddingValues = PaddingValues(0.dp),
) {
    if (items.isEmpty()) return

    val localDensity = LocalDensity.current
    val configuration = LocalConfiguration.current
    val layoutDirection = if (configuration.layoutDirection == 1) LayoutDirection.Ltr else LayoutDirection.Rtl

    val total = items.map { it.amount }.reduce { acc, next -> acc + next }
    if (total.compareTo(java.math.BigDecimal.ZERO) == 0) return

    val textPaint = remember(localDensity) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = with(localDensity) { 14.sp.toPx() }
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val innerRadius = radius * 0.45f
        val outerRadius = radius
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        val initialAngles = items.map {
            it.amount.divide(total, 5, RoundingMode.HALF_DOWN).multiply(360.toBigDecimal()).toFloat()
        }

        val maxTotalMinAngle = 250f
        val minSweepAngle = (maxTotalMinAngle / items.size).coerceAtMost(15f)
        val smallSlices = initialAngles.filter { it < minSweepAngle }
        val largeSlices = initialAngles.filter { it >= minSweepAngle }
        val extraNeeded = (smallSlices.size * minSweepAngle) - smallSlices.sum()

        val finalAngles = initialAngles.map { angle ->
            if (angle < minSweepAngle) minSweepAngle
            else if (largeSlices.isNotEmpty()) angle - (extraNeeded * (angle / largeSlices.sum()))
            else angle
        }

        var currentStartAngle = -90f

        items.forEachIndexed { index, tag ->
            val sweepAngle = finalAngles[index]
            val isSelected = index == selectedIndex
            
            var arcCenterX = centerX
            var arcCenterY = centerY
            if (isSelected) {
                val midAngle = currentStartAngle + (sweepAngle / 2f)
                val offsetDist = 12.dp.toPx()
                arcCenterX += cos(Math.toRadians(midAngle.toDouble())).toFloat() * offsetDist
                arcCenterY += sin(Math.toRadians(midAngle.toDouble())).toFloat() * offsetDist
            }

            // Draw a proper donut slice using Path
            val midAngle = currentStartAngle + (sweepAngle / 2f)
            val path = Path().apply {
                // Outer arc
                arcTo(
                    rect = Size(outerRadius * 2, outerRadius * 2).let { 
                        androidx.compose.ui.geometry.Rect(arcCenterX - outerRadius, arcCenterY - outerRadius, arcCenterX + outerRadius, arcCenterY + outerRadius) 
                    },
                    startAngleDegrees = currentStartAngle,
                    sweepAngleDegrees = sweepAngle,
                    forceMoveTo = true
                )
                // Inner arc (backwards)
                arcTo(
                    rect = Size(innerRadius * 2, innerRadius * 2).let { 
                        androidx.compose.ui.geometry.Rect(arcCenterX - innerRadius, arcCenterY - innerRadius, arcCenterX + innerRadius, arcCenterY + innerRadius) 
                    },
                    startAngleDegrees = currentStartAngle + sweepAngle,
                    sweepAngleDegrees = -sweepAngle,
                    forceMoveTo = false
                )
                close()
            }

            drawPath(
                path = path,
                color = tag.color?.main ?: Color.Black,
                style = Fill
            )

            // Draw percentage ONLY if selected
            if (isSelected) {
                val percentage = items[index].amount
                    .divide(total, 4, RoundingMode.HALF_UP)
                    .multiply(java.math.BigDecimal(100))
                    .toInt()
                
                val labelRadius = (outerRadius + innerRadius) / 2f
                val labelX = arcCenterX + cos(Math.toRadians(midAngle.toDouble())).toFloat() * labelRadius
                val labelY = arcCenterY + sin(Math.toRadians(midAngle.toDouble())).toFloat() * labelRadius
                
                drawContext.canvas.nativeCanvas.drawText(
                    "$percentage%",
                    labelX,
                    labelY + (textPaint.textSize / 3),
                    textPaint
                )
            }

            currentStartAngle += sweepAngle
        }
    }
}

@Preview
@Composable
private fun PreviewDonutChart() {
    MinusTheme {
        DonutChart(
            items = listOf(
                CategoryUsage("Alimentacion", 100.toBigDecimal()),
                CategoryUsage("Transporte", 200.toBigDecimal()),
                CategoryUsage("Salud", 300.toBigDecimal()),
            ),
            selectedIndex = 1
        )
    }
}
