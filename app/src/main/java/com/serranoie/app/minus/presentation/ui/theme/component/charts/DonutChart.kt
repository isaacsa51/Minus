package com.serranoie.app.minus.presentation.ui.theme.component.charts

import android.graphics.Paint
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import java.math.RoundingMode
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun DonutChart(
    modifier: Modifier = Modifier,
    items: List<CategoryUsage>,
    selectedIndex: Int = -1,
    onItemClick: (Int) -> Unit = {},
) {
    if (items.isEmpty()) return

    val localDensity = LocalDensity.current
    val total = items.map { it.amount }.reduce { acc, next -> acc + next }
    if (total.compareTo(java.math.BigDecimal.ZERO) == 0) return

    val finalAngles = remember(items, total) {
        val initialAngles = items.map {
            it.amount.divide(total, 5, RoundingMode.HALF_DOWN).multiply(360.toBigDecimal()).toFloat()
        }

        val maxTotalMinAngle = 250f
        val minSweepAngle = (maxTotalMinAngle / items.size).coerceAtMost(15f)
        val smallSlices = initialAngles.filter { it < minSweepAngle }
        val largeSlices = initialAngles.filter { it >= minSweepAngle }
        val extraNeeded = (smallSlices.size * minSweepAngle) - smallSlices.sum()

        initialAngles.map { angle ->
            if (angle < minSweepAngle) minSweepAngle
            else if (largeSlices.isNotEmpty()) angle - (extraNeeded * (angle / largeSlices.sum()))
            else angle
        }
    }

    val animatedOffsets = items.mapIndexed { index, _ ->
        animateFloatAsState(
            targetValue = if (index == selectedIndex) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "offset_$index"
        )
    }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textPaint = remember(localDensity, onSurfaceColor, surfaceColor) {
        Paint().apply {
            color = onSurfaceColor.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = with(localDensity) { 16.sp.toPx() }
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }

    Canvas(
        modifier = modifier
            .pointerInput(items, finalAngles) {
                detectTapGestures { offset ->
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val dx = offset.x - centerX
                    val dy = offset.y - centerY
                    val distance = sqrt(dx * dx + dy * dy)

                    val radius = min(size.width, size.height) / 2f
                    val innerRadius = radius * 0.45f

                    if (distance in innerRadius..radius) {
                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        if (angle < -90) angle += 360

                        var currentAngle = -90f
                        finalAngles.forEachIndexed { index, sweepAngle ->
                            if (angle >= currentAngle && angle <= (currentAngle + sweepAngle)) {
                                onItemClick(index)
                                return@detectTapGestures
                            }
                            currentAngle += sweepAngle
                        }
                    }
                }
            }
    ) {
        val radius = min(size.width, size.height) / 2f
        val innerRadius = radius * 0.45f
        val outerRadius = radius
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        var currentStartAngle = -90f

        items.forEachIndexed { index, tag ->
            val sweepAngle = finalAngles[index]
            val offsetProgress = animatedOffsets[index].value
            
            var arcCenterX = centerX
            var arcCenterY = centerY
            
            val midAngle = currentStartAngle + (sweepAngle / 2f)
            if (offsetProgress > 0f) {
                val offsetDist = 12.dp.toPx() * offsetProgress
                arcCenterX += cos(Math.toRadians(midAngle.toDouble())).toFloat() * offsetDist
                arcCenterY += sin(Math.toRadians(midAngle.toDouble())).toFloat() * offsetDist
            }

            val path = Path().apply {
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(arcCenterX - outerRadius, arcCenterY - outerRadius, arcCenterX + outerRadius, arcCenterY + outerRadius),
                    startAngleDegrees = currentStartAngle,
                    sweepAngleDegrees = sweepAngle,
                    forceMoveTo = true
                )
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(arcCenterX - innerRadius, arcCenterY - innerRadius, arcCenterX + innerRadius, arcCenterY + innerRadius),
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

            if (offsetProgress > 0.5f) {
                val percentage = items[index].amount
                    .divide(total, 4, RoundingMode.HALF_UP)
                    .multiply(java.math.BigDecimal(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .toInt()
                
                val labelRadius = (outerRadius + innerRadius) / 2f
                val labelX = arcCenterX + cos(Math.toRadians(midAngle.toDouble())).toFloat() * labelRadius
                val labelY = arcCenterY + sin(Math.toRadians(midAngle.toDouble())).toFloat() * labelRadius
                
                textPaint.color = (if (items[index].isSpecial) surfaceColor else onSurfaceColor).toArgb()
                textPaint.alpha = (255 * ((offsetProgress - 0.5f) * 2f)).toInt().coerceIn(0, 255)
                
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
