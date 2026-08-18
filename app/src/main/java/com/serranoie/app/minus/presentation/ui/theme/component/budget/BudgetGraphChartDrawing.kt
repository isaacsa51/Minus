package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

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
    currencyFormat: java.text.Format,
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

internal fun formatAxisValue(
    value: BigDecimal,
    currencyFormat: java.text.Format,
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
