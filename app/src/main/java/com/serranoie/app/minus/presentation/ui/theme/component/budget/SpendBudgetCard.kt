package com.serranoie.app.minus.presentation.ui.theme.component.budget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FloatTweenSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.harmonizeWithColor
import com.serranoie.app.minus.presentation.util.numberFormat
import com.serranoie.app.minus.presentation.util.toPaletteWithTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private val GoodColor = Color(0xFF81C784)
private val NotGoodColor = Color(0xFFFFB74D)
private val BadColor = Color(0xFFE57373)

private fun DrawScope.drawWavyPattern(
    color: Color,
    percent: Float,
    shift: Float,
    periodPx: Float = 60f,
    amplitudePx: Float = 8f,
) {
    val edgeX = size.width * percent.coerceIn(0f, 1f)
    if (edgeX <= 0f) return

    val height = size.height
    val halfPeriod = periodPx / 2

    val wavyPath = Path().apply {
        // Start at top-left
        moveTo(x = 0f, y = 0f)
        
        // Line to the wavy edge start at top
        lineTo(x = edgeX, y = 0f)
        
        // Draw the wavy vertical edge going down
        // Use shift to animate the wave up/down
        val phaseOffset = shift * halfPeriod
        val wavesNeeded = kotlin.math.ceil(height / halfPeriod + 2).toInt()
        
        for (i in 0 until wavesNeeded) {
            val baseY = i * halfPeriod - phaseOffset
            if (baseY > height + halfPeriod) break
            if (baseY < -halfPeriod) continue
            
            val direction = if (i % 2 == 0) 1 else -1
            val waveX = edgeX + amplitudePx * direction
            
            val startY = (baseY).coerceAtLeast(0f)
            val endY = (baseY + halfPeriod).coerceAtMost(height)
            
            if (startY < height && endY > startY) {
                val midY = (startY + endY) / 2
                quadraticBezierTo(
                    x1 = waveX,
                    y1 = midY,
                    x2 = edgeX,
                    y2 = endY
                )
            }
        }
        
        // Close the path at bottom
        lineTo(x = 0f, y = height)
        close()
    }

    drawPath(path = wavyPath, color = color)
}

@Composable
fun SpendBudgetCard(
    modifier: Modifier = Modifier,
    budget: BigDecimal,
    spend: BigDecimal,
) {
	val context: Context = LocalContext.current

    // Calculate percentage spent (0.0 to 1.0+)
    val percentSpent = remember(budget, spend) {
        if (budget > BigDecimal.ZERO) {
            spend.divide(budget, 4, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }

    // Calculate percentage remaining for display
    val percentRemaining = remember(percentSpent) {
        BigDecimal(1).minus(percentSpent).coerceAtLeast(BigDecimal.ZERO)
    }

    val percentFormatted = remember(percentRemaining) {
        val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
        formatter.maximumFractionDigits = 0
        formatter.minimumFractionDigits = 0
        formatter.format(percentRemaining.multiply(BigDecimal(100)))
    }

    // Animate the wavy pattern shift - use mutableFloatStateOf to avoid recompositions
    var shift by remember { mutableFloatStateOf(0f) }
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Infinite animation loop
        while (true) {
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = FloatTweenSpec(4000, 0, LinearEasing)
            )
            animatable.snapTo(0f)
        }
    }

    // Update shift value from animatable
    shift = animatable.value

    // Get colors - use static colors to avoid composable calls inside remember
    val primaryColor = MaterialTheme.colorScheme.primary
    val isDarkTheme = isSystemInDarkTheme()

    // Calculate the color based on percentage spent
    val combinedColor = remember(percentSpent) {
        combineColors(
            listOf(GoodColor, NotGoodColor, BadColor),
            percentSpent.coerceIn(BigDecimal.ZERO, BigDecimal.ONE).toFloat()
        )
    }

    val harmonizedColor = remember(combinedColor, primaryColor, isDarkTheme) {
        val harmonized = harmonizeWithColor(combinedColor, primaryColor)
        toPaletteWithTheme(harmonized, isDarkTheme)
    }

    val density = LocalDensity.current
    val periodPx = remember { with(density) { 30.dp.toPx() } }
    val amplitudePx = remember { with(density) { 4.dp.toPx() } }

    // Get current content color for the text
    val contentColor = LocalContentColor.current
    val currencyFormatter = remember { com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat("USD") }

	_root_ide_package_.com.serranoie.app.minus.presentation.ui.theme.component.StatCard(
		modifier = modifier
			.fillMaxWidth()
			.heightIn(min = 80.dp, max = 120.dp),
		colors = CardDefaults.cardColors(
			containerColor = harmonizedColor.container,
			contentColor = harmonizedColor.onContainer,
		),
		value = numberFormat(
			context,
			spend,
			"MXN"
		),
		label = "Gastado",
		content = {
			Spacer(modifier = Modifier.height(6.dp))
			Text("Disponible: $percentFormatted%", style = MaterialTheme.typography.bodyMedium)
		},
		backdropContent = {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.fillMaxHeight()
					.drawBehind {
						drawWavyPattern(
							color = harmonizedColor.main,
							percent = percentSpent.coerceIn(BigDecimal.ZERO, BigDecimal.ONE)
								.toFloat(),
							shift = shift,
							periodPx = periodPx,
							amplitudePx = amplitudePx,
						)
					}
			)
		}
	)
}

@Preview(name = "SpendBudgetCard")
@Preview(name = "SpendBudgetCard",
	uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun PreviewSpendBudgetCard() {
    MinusTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            // Low spend - green
            SpendBudgetCard(
                modifier = Modifier.height(IntrinsicSize.Min),
                spend = BigDecimal(3740),
                budget = BigDecimal(60000),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Medium spend - yellow/orange
            SpendBudgetCard(
                modifier = Modifier.height(IntrinsicSize.Min),
                spend = BigDecimal(30740),
                budget = BigDecimal(60000),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // High spend - red
            SpendBudgetCard(
                modifier = Modifier.height(IntrinsicSize.Min),
                spend = BigDecimal(45740),
                budget = BigDecimal(60000),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // No spend - full green
            SpendBudgetCard(
                modifier = Modifier.height(IntrinsicSize.Min),
                spend = BigDecimal.ZERO,
                budget = BigDecimal(60000),
            )
        }
    }
}
