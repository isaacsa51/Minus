package com.serranoie.app.minus.presentation.ui.theme.component.budget.graphs

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Format
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.math.roundToInt

/** Shared draw params for [GraphCanvas] and [MultiCategoryChart]. */
internal data class GraphDrawParams(
    val graphColor: Color,
    val secondaryColor: Color,
    val surfaceColor: Color,
    val gridColor: Color,
    val dashEffect: PathEffect,
    val labelStyle: TextStyle,
    val tertiaryColor: Color
)

/** Draws the shared gridlines + date-axis backdrop used by both [GraphCanvas] and [MultiCategoryChart]. */
internal fun DrawScope.drawCoordinateSystem(
    params: GraphDrawParams,
    textMeasurer: TextMeasurer,
    currencyFormat: Format,
    dateFormatter: DateTimeFormatter,
    startDate: Date,
    windowIndex: Int,
    scrollStep: Int,
    dataSize: Int,
    maxVal: Float,
    alpha: Float,
    thousandsUnit: String,
    millionsUnit: String,
    isTodayHighlighted: Boolean = false
) {
    val width = size.width
    val height = size.height

    val leftMargin = 42.dp.toPx()
    val bottomMargin = 20.dp.toPx()
    val topPadding = 10.dp.toPx()
    val drawableHeight = height - bottomMargin - topPadding
    val drawableWidth = width - leftMargin

    val gridLinesCount = 4
    for (i in 0..gridLinesCount) {
        val fraction = i.toFloat() / gridLinesCount
        val y = height - bottomMargin - (fraction * drawableHeight)

        drawLine(
            color = params.gridColor.copy(alpha = params.gridColor.alpha * alpha),
            start = Offset(leftMargin, y),
            end = Offset(width, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = params.dashEffect
        )

        val value = (maxVal * fraction).toBigDecimal()
        val labelText = formatAxisValue(value, currencyFormat, thousandsUnit, millionsUnit)
        val textLayoutResult = textMeasurer.measure(
            text = labelText,
            style = params.labelStyle.copy(color = params.labelStyle.color.copy(alpha = params.labelStyle.color.alpha * alpha))
        )

        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                x = leftMargin - textLayoutResult.size.width - 8.dp.toPx(),
                y = y - textLayoutResult.size.height / 2
            )
        )
    }

    val startLocalDate = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    val windowStartIndex = windowIndex * scrollStep
    val stepWidth = drawableWidth / (dataSize - 1).coerceAtLeast(1)

    val labelsToDraw = mutableListOf<Triple<Float, String, Int>>()
    var lastLabelEndX = -1000f
    var lastLabelText = ""
    val minGap = 16.dp.toPx()

    for (index in 0 until dataSize) {
        val x = leftMargin + index * stepWidth
        val date = startLocalDate.plusDays((windowStartIndex + index).toLong())
        val dateText = date.format(dateFormatter)

        val isEnd = index == dataSize - 1
        val isFirst = index == 0
        val isToday = date == today

        val shouldAttemptLabel =
            isFirst || isEnd || (isToday && isTodayHighlighted) || dateText != lastLabelText

        if (shouldAttemptLabel) {
            val style = if (isToday && isTodayHighlighted) {
                params.labelStyle.copy(
                    color = params.tertiaryColor.copy(alpha = alpha),
                    fontWeight = FontWeight.Bold
                )
            } else {
                params.labelStyle.copy(color = params.labelStyle.color.copy(alpha = params.labelStyle.color.alpha * alpha))
            }
            val textLayoutResult = textMeasurer.measure(dateText, style)
            val labelWidth = textLayoutResult.size.width.toFloat()
            val labelStartX = x - labelWidth / 2

            if (labelStartX > lastLabelEndX + minGap) {
                labelsToDraw.add(Triple(x, dateText, index))
                lastLabelEndX = x + labelWidth / 2
                lastLabelText = dateText
            } else if (isEnd && labelsToDraw.isNotEmpty()) {
                labelsToDraw.removeAt(labelsToDraw.size - 1)
                labelsToDraw.add(Triple(x, dateText, index))
            } else if (isToday && isTodayHighlighted && labelsToDraw.isNotEmpty()) {
                val last = labelsToDraw.last()
                val lastX = last.first
                val lastText = last.second
                val lastMeasured = textMeasurer.measure(lastText, params.labelStyle)
                if (x - labelWidth / 2 < lastX + lastMeasured.size.width / 2 + minGap) {
                    labelsToDraw.removeAt(labelsToDraw.size - 1)
                }
                labelsToDraw.add(Triple(x, dateText, index))
                lastLabelEndX = x + labelWidth / 2
                lastLabelText = dateText
            }
        }
    }

    for (index in 0 until dataSize) {
        val x = leftMargin + index * stepWidth
        val date = startLocalDate.plusDays((windowStartIndex + index).toLong())
        val isToday = date == today && isTodayHighlighted

        drawLine(
            color = if (isToday) params.tertiaryColor.copy(alpha = 0.4f * alpha) else params.gridColor.copy(
                alpha = 0.2f * alpha
            ),
            start = Offset(x, topPadding),
            end = Offset(x, height - bottomMargin),
            strokeWidth = if (isToday) 2.dp.toPx() else 1.dp.toPx()
        )

        labelsToDraw.find { it.third == index }?.let { (_, dateText, _) ->
            val style = if (isToday) {
                params.labelStyle.copy(
                    color = params.tertiaryColor.copy(alpha = alpha),
                    fontWeight = FontWeight.ExtraBold
                )
            } else {
                params.labelStyle.copy(color = params.labelStyle.color.copy(alpha = params.labelStyle.color.alpha * alpha))
            }
            val textLayoutResult = textMeasurer.measure(dateText, style)
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x = x - textLayoutResult.size.width / 2,
                    y = height - bottomMargin + 4.dp.toPx()
                )
            )
        }
    }
}

internal fun plotY(
    value: Float,
    maxVal: Float,
    canvasHeight: Float,
    topPadding: Float,
    bottomMargin: Float,
): Float {
    val drawableHeight = canvasHeight - bottomMargin - topPadding
    val safeMax = if (maxVal <= 0f) 1f else maxVal
    val fraction = (value / safeMax).coerceIn(0f, 1f)
    return canvasHeight - bottomMargin - fraction * drawableHeight
}

internal fun formatAxisValue(
    value: BigDecimal,
    currencyFormat: Format,
    thousandsUnit: String,
    millionsUnit: String
): String {
    return when {
        value >= BigDecimal("1000000") -> {
            val millions = value.divide(BigDecimal("1000000"), 1, RoundingMode.HALF_EVEN)
            "${currencyFormat.format(millions)}$millionsUnit"
        }

        value >= BigDecimal("1000") -> {
            val thousands = value.divide(BigDecimal("1000"), 0, RoundingMode.HALF_EVEN)
            "${currencyFormat.format(thousands)}$thousandsUnit"
        }

        else -> currencyFormat.format(value)
    }
}

/** Draws a tap/drag tooltip (marker + value bubble) over [currentPoints] at [pos]; shared by [GraphCanvas] and [GraphHourCanvas]. */
internal fun DrawScope.drawTooltipInteraction(
    pos: Offset,
    currentPoints: List<BigDecimal>,
    maxVal: Float,
    color: Color,
    surfaceColor: Color,
    currencyFormat: Format,
    textMeasurer: TextMeasurer,
    tooltipStyle: TextStyle,
    leftMargin: Float,
    topPadding: Float,
    bottomMargin: Float
) {
    val width = size.width
    val height = size.height
    val drawableWidth = width - leftMargin

    val stepWidth = drawableWidth / (currentPoints.size - 1).coerceAtLeast(1)
    val index = ((pos.x - leftMargin) / stepWidth).roundToInt().coerceIn(0, currentPoints.size - 1)

    val amount = currentPoints[index]
    val pointX = leftMargin + index * stepWidth
    val pointY = plotY(amount.toFloat(), maxVal, height, topPadding, bottomMargin)

    drawLine(
        color = color.copy(alpha = 0.5f),
        start = Offset(pointX, topPadding),
        end = Offset(pointX, height - bottomMargin),
        strokeWidth = 1.dp.toPx()
    )

    drawCircle(
        color = color,
        radius = 6.dp.toPx(),
        center = Offset(pointX, pointY)
    )

    drawCircle(
        color = surfaceColor,
        radius = 3.dp.toPx(),
        center = Offset(pointX, pointY)
    )

    val textLayoutResult = textMeasurer.measure(
        text = currencyFormat.format(amount),
        style = tooltipStyle
    )
    val tooltipWidth = textLayoutResult.size.width + 16.dp.toPx()
    val tooltipHeight = textLayoutResult.size.height + 8.dp.toPx()

    val tooltipX = (pointX - tooltipWidth / 2).coerceIn(0f, width - tooltipWidth)
    val tooltipY = (pointY - tooltipHeight - 12.dp.toPx()).coerceAtLeast(topPadding)

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.8f),
        topLeft = Offset(tooltipX, tooltipY),
        size = Size(tooltipWidth, tooltipHeight),
        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
    )

    drawText(
        textMeasurer = textMeasurer,
        text = currencyFormat.format(amount),
        style = tooltipStyle,
        topLeft = Offset(tooltipX + 8.dp.toPx(), tooltipY + 4.dp.toPx())
    )
}

/** Draws [points] as a gradient-filled area under a line; shared by [GraphCanvas] and [GraphHourCanvas]. */
internal fun DrawScope.drawGraphArea(
    points: List<BigDecimal>,
    maxVal: Float,
    color: Color,
    leftMargin: Float,
    topPadding: Float,
    bottomMargin: Float
) {
    val canvasWidth = size.width
    val canvasHeight = size.height
    val drawableWidth = canvasWidth - leftMargin

    val stepWidth = drawableWidth / (points.size - 1).coerceAtLeast(1)

    val path = Path().apply {
        points.forEachIndexed { index, value ->
            val x = leftMargin + index * stepWidth
            val y = plotY(value.toFloat(), maxVal, canvasHeight, topPadding, bottomMargin)
            if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
        val lastX = leftMargin + (points.size - 1) * stepWidth
        lineTo(lastX, canvasHeight - bottomMargin)
        lineTo(leftMargin, canvasHeight - bottomMargin)
        close()
    }

    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(
                color.copy(alpha = 0.35f),
                color.copy(alpha = 0.15f),
                Color.Transparent
            ),
            startY = topPadding,
            endY = canvasHeight - bottomMargin
        ),
        style = Fill
    )
}

/** Draws [points] as a rounded stroke line; shared by [GraphCanvas] and [GraphHourCanvas]. */
internal fun DrawScope.drawGraphLine(
    points: List<BigDecimal>,
    maxVal: Float,
    color: Color,
    width: Float,
    leftMargin: Float,
    topPadding: Float,
    bottomMargin: Float
) {
    val canvasWidth = size.width
    val canvasHeight = size.height
    val drawableWidth = canvasWidth - leftMargin

    val stepWidth = drawableWidth / (points.size - 1).coerceAtLeast(1)

    val path = Path().apply {
        points.forEachIndexed { index, value ->
            val x = leftMargin + index * stepWidth
            val y = plotY(value.toFloat(), maxVal, canvasHeight, topPadding, bottomMargin)
            if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = width,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

/** The 12a.m./4a.m./etc-style tick hours [MultiCategoryHourChart] and [GraphHourCanvas] label their hour axis with. */
internal val AXIS_TICK_HOURS = listOf(0, 4, 8, 12, 16, 20, 23)

/** Formats an hour-of-day (0-23) as "12a.m.", "4a.m.", "11p.m.", etc. */
internal fun hourLabel(hour: Int): String {
    val displayHour = hour % 12
    val suffix = if (hour < 12) "a.m." else "p.m."
    return "${if (displayHour == 0) 12 else displayHour}$suffix"
}

/** Draws the shared $ gridlines for the 24-hour axis used by [MultiCategoryHourChart] and [GraphHourCanvas]. */
internal fun DrawScope.drawHourGridlines(
    leftMargin: Float,
    baseline: Float,
    drawableWidth: Float,
    drawableHeight: Float,
    width: Float,
    maxVal: Float,
    gridColor: Color,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
    currencyFormat: Format,
    alpha: Float = 1f,
) {
    val gridLinesCount = 3
    for (i in 0..gridLinesCount) {
        val fraction = i.toFloat() / gridLinesCount
        val y = baseline - (fraction * drawableHeight)

        drawLine(
            color = gridColor.copy(alpha = gridColor.alpha * alpha),
            start = Offset(leftMargin, y),
            end = Offset(width, y),
            strokeWidth = 1.dp.toPx(),
        )

        val value = (maxVal * fraction).toBigDecimal()
        val textLayoutResult = textMeasurer.measure(
            text = currencyFormat.format(value),
            style = labelStyle.copy(color = labelStyle.color.copy(alpha = labelStyle.color.alpha * alpha)),
        )
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                x = leftMargin - textLayoutResult.size.width - 8.dp.toPx(),
                y = y - textLayoutResult.size.height / 2
            )
        )
    }
}

/** Draws the shared 12a.m.-11p.m. tick labels used by [MultiCategoryHourChart] and [GraphHourCanvas]. */
internal fun DrawScope.drawHourAxisLabels(
    leftMargin: Float,
    baseline: Float,
    stepWidth: Float,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
) {
    AXIS_TICK_HOURS.forEach { hour ->
        val x = leftMargin + hour * stepWidth
        val textLayoutResult = textMeasurer.measure(hourLabel(hour), labelStyle)
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                x = x - textLayoutResult.size.width / 2,
                y = baseline + 4.dp.toPx()
            )
        )
    }
}
