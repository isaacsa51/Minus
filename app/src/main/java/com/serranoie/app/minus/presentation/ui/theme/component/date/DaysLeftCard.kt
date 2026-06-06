package com.serranoie.app.minus.presentation.ui.theme.component.date

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.util.countDays
import com.serranoie.app.minus.presentation.util.countDaysToToday
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DaysLeftCard(
	modifier: Modifier = Modifier,
	startDate: Date,
	finishDate: Date?,
) {
	if (finishDate == null) {
		Box { }
		return
	}

	val days = countDays(finishDate, startDate)
	val restDays = countDaysToToday(finishDate)

	val density = LocalDensity.current
	val strokeWidthPx = with(density) { 6.dp.toPx() }

	Box(
		Modifier
			.widthIn(max = 120.dp)
			.aspectRatio(1f),
		contentAlignment = Alignment.Center
	) {
		CircularWavyProgressIndicator(
			// Subtract a tiny amount to show animation when completed
			progress = { restDays / days.toFloat() - 0.01f },
			modifier = modifier.fillMaxSize(),
			color = MaterialTheme.colorScheme.primary,
			trackColor = MaterialTheme.colorScheme.surfaceVariant,
			amplitude = { 2f },
			wavelength = 46.dp,
			stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
			trackStroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
		)

		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center,
			modifier = Modifier.padding(16.dp)
		) {
			Text(
				text = restDays.toString(),
				style = MaterialTheme.typography.displayLargeEmphasized.copy(
					fontSize = MaterialTheme.typography.titleLarge.fontSize
				)
			)
			Text(
				text = stringResource(R.string.days_remaining_label),
				style = MaterialTheme.typography.labelMediumCondensed.copy(lineHeight = 12.sp),
				textAlign = TextAlign.Center,
				color = LocalContentColor.current.copy(alpha = 0.6f),
			)
			Spacer(modifier = Modifier.height(4.dp))
		}
	}
}

@Deprecated("No longer used, using CircularWavyProgressIndicator instead")
class ArcShape(
	private val thickness: Dp,
	private val progress: Float,
) : Shape {
	override fun createOutline(
		size: Size,
		layoutDirection: LayoutDirection,
		density: Density,
	) = Outline.Generic(Path().apply {
		val fixedProgress = progress - 0.000001f
		val thicknessPx = with(density) { thickness.toPx() }
		val shift = -90f

		val wavyPath = Path().apply {
			arcTo(
				Rect(offset = Offset.Zero, size = size),
				shift,
				-360 * fixedProgress,
				forceMoveTo = true,
			)
			arcTo(
				Rect(
					offset = Offset(thicknessPx, thicknessPx),
					size = Size(
						width = size.width - thicknessPx * 2,
						height = size.height - thicknessPx * 2
					),
				),
				-360 * fixedProgress + shift,
				360 * fixedProgress,
				forceMoveTo = false,
			)
		}
		val boundsPath = Path().apply {
			addRect(Rect(offset = Offset.Zero, size = size))
		}
		op(wavyPath, boundsPath, PathOperation.Intersect)
	})
}

@Preview(showBackground = true)
@Composable
private fun PreviewDaysLeftCard() {
	val calendar = Calendar.getInstance()
	calendar.add(Calendar.DAY_OF_YEAR, -3)
	val startDate = calendar.time
	calendar.add(Calendar.DAY_OF_YEAR, 6)
	val finishDate = calendar.time

	MinusTheme {
		DaysLeftCard(
			startDate = startDate,
			finishDate = finishDate,
		)
	}
}
