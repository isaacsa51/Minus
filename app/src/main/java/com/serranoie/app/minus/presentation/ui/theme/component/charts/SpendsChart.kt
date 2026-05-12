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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorMax
import com.serranoie.app.minus.presentation.ui.theme.colorMin
import com.serranoie.app.minus.presentation.ui.theme.isNightMode
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.harmonize
import com.serranoie.app.minus.presentation.util.isZero
import com.serranoie.app.minus.presentation.util.toPalette
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import kotlin.math.abs

private data class SpendsChartGeometry(
	val filledPath: Path,
	val linePath: Path,
	val markedPoint: Offset?,
)

@Composable
fun SpendsChart(
	modifier: Modifier = Modifier,
	spends: List<Transaction>,
	markedTransaction: Transaction? = null,
	showBeforeMarked: Int = spends.size,
	showAfterMarked: Int = spends.size,
	chartPadding: PaddingValues = PaddingValues(0.dp),
) {
	// ! Handle empty spends list to avoid NoSuchElementException
	if (spends.isEmpty()) {
		return
	}

	val harmonizeColorMax = if (isNightMode()) {
		toPalette(harmonize(colorMax)).onContainer
	} else {
		toPalette(harmonize(colorMax)).main
	}
	val harmonizeColorMin = if (isNightMode()) {
		toPalette(harmonize(colorMin)).onContainer
	} else {
		toPalette(harmonize(colorMin)).main
	}

	val colors = listOf(
		harmonizeColorMax,
		harmonize(combineColors(harmonizeColorMax, harmonizeColorMin, 0.5f)),
		harmonizeColorMin,
	)
	val minSpentValue = spends.minBy { spent -> spent.amount }.amount
	val maxSpentValue = spends.maxBy { spent -> spent.amount }.amount
	val range = maxSpentValue - minSpentValue
	val localDensity = LocalDensity.current

	val layoutDirection = when (LocalConfiguration.current.layoutDirection) {
		0 -> LayoutDirection.Rtl
		1 -> LayoutDirection.Ltr
		else -> LayoutDirection.Rtl
	}
	val topOffset = with(localDensity) { chartPadding.calculateTopPadding().toPx() }
	val bottomOffset = with(localDensity) { chartPadding.calculateBottomPadding().toPx() }
	val startOffset =
		with(localDensity) { chartPadding.calculateStartPadding(layoutDirection).toPx() }
	val endOffset = with(localDensity) { chartPadding.calculateEndPadding(layoutDirection).toPx() }


	val (indexMarked, firstShowIndex, lastShowIndex) = if (markedTransaction != null) {
		val index = spends.indexOfFirst { it.date === markedTransaction.date }

		Triple(
			index,
			(index - showBeforeMarked).coerceAtLeast(0),
			(index + showAfterMarked + 1).coerceAtMost(spends.size),
		)
	} else {
		Triple(null, 0, spends.size)
	}

	Canvas(modifier = modifier) {
		val width = this.size.width
		val height = this.size.height
		val heightWithPaddings = height - topOffset - bottomOffset
		val widthWithPaddings = width - startOffset - endOffset
		val size = (lastShowIndex - firstShowIndex - 1).toFloat().coerceAtLeast(1f)
		val geometry = buildSpendsChartGeometry(
			spends = spends,
			markedTransaction = markedTransaction,
			firstShowIndex = firstShowIndex,
			lastShowIndex = lastShowIndex,
			indexMarked = indexMarked,
			minSpentValue = minSpentValue,
			range = range,
			canvasWidth = width,
			canvasHeight = height,
			topOffset = topOffset,
			startOffset = startOffset,
			heightWithPaddings = heightWithPaddings,
			widthWithPaddings = widthWithPaddings,
			size = size,
		)

		val chartColors = if (markedTransaction != null) {
			val scale = if (range.isZero()) {
				0.5f
			} else {
				1f - markedTransaction.amount
				.minus(minSpentValue)
				.divide(range, 2, RoundingMode.HALF_EVEN)
				.toFloat()
			}

			colors.mapIndexed { index, color ->
				color.copy(alpha = 0.3f - abs(scale - (index / (colors.size - 1))) * 0.25f)
			}
		} else {
			colors.mapIndexed { index, color ->
				color.copy(alpha = 0.3f - (index / (colors.size - 1)) * 0.25f)
			}
		}

		drawPath(
			path = geometry.filledPath,
			Brush.verticalGradient(colors = chartColors),
			style = Fill
		)

		if (markedTransaction != null) {
			val scale = if (range.isZero()) {
				0.5f
			} else {
				markedTransaction.amount
					.minus(minSpentValue)
					.divide(range, 2, RoundingMode.HALF_EVEN)
					.toFloat()
			}

			val color = combineColors(
				harmonizeColorMin,
				harmonizeColorMax,
				scale,
			)

			geometry.markedPoint?.let { markedPoint ->
				drawCircle(
					color = color.copy(0.2f),
					radius = with(localDensity) { 8.dp.toPx() },
					center = markedPoint
				)

				drawCircle(
					color = color,
					radius = with(localDensity) { 3.dp.toPx() },
					center = markedPoint
				)
			}
		}
	}
}

@Composable
fun OutlinedSpendsChart(
	modifier: Modifier = Modifier,
	spends: List<Transaction>,
	markedTransaction: Transaction? = null,
	showBeforeMarked: Int = spends.size,
	showAfterMarked: Int = spends.size,
	chartPadding: PaddingValues = PaddingValues(0.dp),
	graphBackgroundColor: Color,
	graphOutlineColor: Color,
	graphColor: Color,
	graphOutlineWidth: Dp = 2.dp,
) {
	if (spends.isEmpty()) {
		return
	}

	val minSpentValue = spends.minBy { spent -> spent.amount }.amount
	val maxSpentValue = spends.maxBy { spent -> spent.amount }.amount
	val range = maxSpentValue - minSpentValue
	val localDensity = LocalDensity.current

	val layoutDirection = when (LocalConfiguration.current.layoutDirection) {
		0 -> LayoutDirection.Rtl
		1 -> LayoutDirection.Ltr
		else -> LayoutDirection.Rtl
	}
	val topOffset = with(localDensity) { chartPadding.calculateTopPadding().toPx() }
	val bottomOffset = with(localDensity) { chartPadding.calculateBottomPadding().toPx() }
	val startOffset =
		with(localDensity) { chartPadding.calculateStartPadding(layoutDirection).toPx() }
	val endOffset = with(localDensity) { chartPadding.calculateEndPadding(layoutDirection).toPx() }

	val (indexMarked, firstShowIndex, lastShowIndex) = if (markedTransaction != null) {
		val index = spends.indexOfFirst { it.date === markedTransaction.date }

		Triple(
			index,
			(index - showBeforeMarked).coerceAtLeast(0),
			(index + showAfterMarked + 1).coerceAtMost(spends.size),
		)
	} else {
		Triple(null, 0, spends.size)
	}

	Canvas(modifier = modifier) {
		val width = this.size.width
		val height = this.size.height
		val heightWithPaddings = height - topOffset - bottomOffset
		val widthWithPaddings = width - startOffset - endOffset
		val size = (lastShowIndex - firstShowIndex - 1).toFloat().coerceAtLeast(1f)
		val geometry = buildSpendsChartGeometry(
			spends = spends,
			markedTransaction = markedTransaction,
			firstShowIndex = firstShowIndex,
			lastShowIndex = lastShowIndex,
			indexMarked = indexMarked,
			minSpentValue = minSpentValue,
			range = range,
			canvasWidth = width,
			canvasHeight = height,
			topOffset = topOffset,
			startOffset = startOffset,
			heightWithPaddings = heightWithPaddings,
			widthWithPaddings = widthWithPaddings,
			size = size,
		)

		drawPath(
			path = geometry.filledPath,
			color = graphBackgroundColor,
			style = Fill,
		)
		drawPath(
			path = geometry.filledPath,
			color = graphOutlineColor,
			style = Stroke(width = with(localDensity) { graphOutlineWidth.toPx() }),
		)
		drawPath(
			path = geometry.linePath,
			color = graphColor,
			style = Stroke(width = with(localDensity) { graphOutlineWidth.toPx() }),
		)

		geometry.markedPoint?.let { markedPoint ->
			drawCircle(
				color = graphColor.copy(alpha = 0.2f),
				radius = with(localDensity) { 8.dp.toPx() },
				center = markedPoint,
			)
			drawCircle(
				color = graphColor,
				radius = with(localDensity) { 3.dp.toPx() },
				center = markedPoint,
			)
		}
	}
}

private fun buildSpendsChartGeometry(
	spends: List<Transaction>,
	markedTransaction: Transaction?,
	firstShowIndex: Int,
	lastShowIndex: Int,
	indexMarked: Int?,
	minSpentValue: BigDecimal,
	range: BigDecimal,
	canvasWidth: Float,
	canvasHeight: Float,
	topOffset: Float,
	startOffset: Float,
	heightWithPaddings: Float,
	widthWithPaddings: Float,
	size: Float,
): SpendsChartGeometry {
	val linePath = Path()
	val filledPath = Path()
	var lastY = 0f

	spends.subList(firstShowIndex, lastShowIndex).forEachIndexed { index, spent ->
		val scale = if (range.isZero()) {
			0.5f
		} else {
			spent.amount
				.minus(minSpentValue)
				.divide(range, 2, RoundingMode.HALF_EVEN)
				.toFloat()
		}
		val y = topOffset + heightWithPaddings * (1 - scale)
		val x = startOffset + widthWithPaddings * (index / size)
		val controlX = startOffset + widthWithPaddings * ((index - 0.5f).coerceAtLeast(0f) / size)

		if (index == 0) {
			lastY = y
			linePath.moveTo(0f, lastY)
			filledPath.moveTo(0f, lastY)
		}

		linePath.cubicTo(controlX, lastY, controlX, y, x, y)
		filledPath.cubicTo(controlX, lastY, controlX, y, x, y)
		lastY = y
	}

	filledPath.lineTo(canvasWidth, lastY)
	filledPath.lineTo(canvasWidth, canvasHeight)
	filledPath.lineTo(0f, canvasHeight)
	filledPath.close()

	val markedPoint = if (markedTransaction != null && indexMarked != null && indexMarked >= 0) {
		val markedScale = if (range.isZero()) {
			0.5f
		} else {
			markedTransaction.amount
				.minus(minSpentValue)
				.divide(range, 2, RoundingMode.HALF_EVEN)
				.toFloat()
		}
		Offset(
			x = startOffset + widthWithPaddings * ((indexMarked - firstShowIndex).toFloat() / size),
			y = topOffset + heightWithPaddings * (1 - markedScale),
		)
	} else {
		null
	}

	return SpendsChartGeometry(
		filledPath = filledPath,
		linePath = linePath,
		markedPoint = markedPoint,
	)
}

@Preview(name = "SpendsChart", showBackground = true)
@Composable
private fun PreviewSpendsChart() {
	MinusTheme {
		Column(modifier = Modifier.padding(16.dp)) {
			SpendsChart(
				spends = previewSpendsChartTransactions(),
				modifier = Modifier
					.fillMaxWidth()
					.height(160.dp),
			)
		}
	}
}

@Preview(name = "OutlinedSpendsChart", showBackground = true)
@Composable
private fun PreviewOutlinedSpendsChart() {
	MinusTheme {
		Column(modifier = Modifier.padding(16.dp)) {
			OutlinedSpendsChart(
				spends = previewSpendsChartTransactions(),
				markedTransaction = previewSpendsChartTransactions()[2],
				modifier = Modifier
					.fillMaxWidth()
					.height(160.dp),
				chartPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
				graphBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
				graphOutlineColor = MaterialTheme.colorScheme.outline,
				graphColor = MaterialTheme.colorScheme.primary,
			)
		}
	}
}

private fun previewSpendsChartTransactions(): List<Transaction> = listOf(
	Transaction(
		id = 1,
		amount = BigDecimal("10.00"),
		comment = "Food",
		date = LocalDateTime.now(),
		isDeleted = false
	),
	Transaction(
		id = 2,
		amount = BigDecimal("25.00"),
		comment = "Food",
		date = LocalDateTime.now().plusDays(1),
		isDeleted = false
	),
	Transaction(
		id = 3,
		amount = BigDecimal("52.00"),
		comment = "Food",
		date = LocalDateTime.now().plusDays(2),
		isDeleted = false
	),
	Transaction(
		id = 4,
		amount = BigDecimal("18.00"),
		comment = "Food",
		date = LocalDateTime.now().plusDays(3),
		isDeleted = false
	),
)