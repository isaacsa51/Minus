package com.serranoie.app.minus.presentation.ui.theme.component.budget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleLargeCondensed
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
	if (size.height <= 0f || size.width <= 0f) {
		android.util.Log.w("drawWavyPattern", "skipping draw: invalid size $size")
		return
	}

	val clampedPercent = percent.coerceIn(0f, 1f)
	val edgeX = size.width * clampedPercent
	val height = size.height

	if (clampedPercent <= 0f) return

	if (clampedPercent >= 1f || edgeX >= size.width) {
		drawRect(color = color, size = size)
		return
	}

	val halfPeriod = periodPx / 2

	val wavyPath = Path().apply {
		moveTo(x = 0f, y = 0f)
		lineTo(x = edgeX, y = 0f)

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
				quadraticTo(
					x1 = waveX,
					y1 = midY,
					x2 = edgeX,
					y2 = endY
				)
			}
		}

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

	val percentSpent = remember(budget, spend) {
		if (budget > BigDecimal.ZERO) {
			spend.divide(budget, 4, RoundingMode.HALF_UP)
		} else BigDecimal.ZERO
	}

	val percentRemaining = remember(percentSpent) {
		BigDecimal(1).minus(percentSpent).coerceAtLeast(BigDecimal.ZERO)
	}

	val percentFormatted = remember(percentRemaining) {
		val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
		formatter.maximumFractionDigits = 0
		formatter.minimumFractionDigits = 0
		formatter.format(percentRemaining.multiply(BigDecimal(100)))
	}

	val primaryColor = MaterialTheme.colorScheme.primary
	val isDarkTheme = isSystemInDarkTheme()

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
	val periodPx = remember { with(density) { 42.dp.toPx() } }
	val amplitudePx = remember { with(density) { 6.dp.toPx() } }

	Card(
		modifier = modifier.fillMaxWidth(),
		shape = MaterialTheme.shapes.extraLarge,
		colors = CardDefaults.cardColors(
			containerColor = harmonizedColor.container,
			contentColor = harmonizedColor.onContainer,
		),
	) {
		val textColor = LocalContentColor.current

		Box(
			modifier = Modifier
				.fillMaxWidth()
				.fillMaxHeight()
				.drawBehind {
					drawWavyPattern(
						color = harmonizedColor.main,
						percent = percentSpent.coerceIn(BigDecimal.ZERO, BigDecimal.ONE)
							.toFloat(),
						shift = 0f,
						periodPx = periodPx,
						amplitudePx = amplitudePx,
					)
				}
		) {
			// Content
			Column(
				modifier = Modifier
					.align(Alignment.CenterStart)
					.fillMaxWidth()
					.padding(vertical = 16.dp, horizontal = 24.dp),
			) {
				Text(
					text = numberFormat(context, spend, "MXN"),
					style = MaterialTheme.typography.titleLargeCondensed.copy(fontWeight = FontWeight.Light),
					overflow = TextOverflow.Ellipsis,
					softWrap = false,
					lineHeight = TextUnit(0.2f, TextUnitType.Em),
				)

				Text(
					text = stringResource(R.string.spend_budget_card_spent_label),
					style = MaterialTheme.typography.labelMediumCondensed,
					color = textColor.copy(alpha = 0.6f),
					overflow = TextOverflow.Ellipsis,
					softWrap = false,
					modifier = Modifier
						.fillMaxWidth()
						.basicMarquee(),
					textAlign = TextAlign.Start,
				)

				Spacer(modifier = Modifier.height(6.dp))

				CompositionLocalProvider(
					LocalContentColor provides textColor,
				) {
					Text(
						text = stringResource(
							R.string.spend_budget_card_available_percent_format,
							percentFormatted
						),
						style = MaterialTheme.typography.bodySmallCondensed.copy(fontWeight = FontWeight.Light)
					)
				}
			}
		}
	}
}

@Preview
@Preview(
	name = "SpendBudgetCard",
	uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun PreviewSpendBudgetCard() {
	MinusTheme {
		Column(modifier = Modifier.padding(16.dp)) {
			SpendBudgetCard(
				modifier = Modifier.height(IntrinsicSize.Min),
				spend = BigDecimal(3740),
				budget = BigDecimal(60000),
			)

			Spacer(modifier = Modifier.height(16.dp))

			SpendBudgetCard(
				modifier = Modifier.height(IntrinsicSize.Min),
				spend = BigDecimal(30740),
				budget = BigDecimal(60000),
			)

			Spacer(modifier = Modifier.height(16.dp))

			SpendBudgetCard(
				modifier = Modifier.height(IntrinsicSize.Min),
				spend = BigDecimal(45740),
				budget = BigDecimal(60000),
			)

			Spacer(modifier = Modifier.height(16.dp))

			SpendBudgetCard(
				modifier = Modifier.height(IntrinsicSize.Min),
				spend = BigDecimal.ZERO,
				budget = BigDecimal(60000),
			)
		}
	}
}
