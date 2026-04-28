package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import kotlin.math.sin

/**
 * A wavy line divider with customizable amplitude and text in the center.
 *
 * @param text The text to display in the center of the divider
 * @param amplitude The height of the wave peaks (default 8f)
 * @param wavelength The length of each wave cycle (default 20f)
 * @param strokeWidth The thickness of the line (default 2.dp)
 * @param color The color of the wave line
 */
@Composable
fun WavyDivider(
	text: String,
	modifier: Modifier = Modifier,
	amplitude: Float = 8f,
	wavelength: Float = 20f,
	strokeWidth: Float = 3f,
	color: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
) {
	BoxWithConstraints(
		modifier = modifier.fillMaxWidth()
	) {
		val maxTextWidth = maxWidth * 0.7f
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp, vertical = 8.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(12.dp)
		) {
			WavyLine(
				modifier = Modifier
					.weight(1f)
					.widthIn(min = 24.dp),
				amplitude = amplitude,
				wavelength = wavelength,
				strokeWidth = strokeWidth,
				color = color
			)

			Text(
				text = text,
				modifier = Modifier
					.widthIn(max = maxTextWidth)
					.basicMarquee(),
				textAlign = TextAlign.Center,
				style = MaterialTheme.typography.labelMediumCondensed,
				color = MaterialTheme.colorScheme.outline,
				maxLines = 1
			)

			WavyLine(
				modifier = Modifier
					.weight(1f)
					.widthIn(min = 24.dp),
				amplitude = amplitude,
				wavelength = wavelength,
				strokeWidth = strokeWidth,
				color = color
			)
		}
	}
}

/**
 * A composable that draws a single wavy line using Canvas.
 *
 * @param amplitude The height of the wave peaks
 * @param wavelength The length of each wave cycle
 * @param strokeWidth The thickness of the line
 * @param color The color of the wave line
 */
@Composable
private fun WavyLine(
	modifier: Modifier = Modifier,
	amplitude: Float = 8f,
	wavelength: Float = 20f,
	strokeWidth: Float = 3f,
	color: Color,
) {
	Canvas(
		modifier = modifier.height((amplitude * 2 + strokeWidth).dp)
	) {
		val width = size.width
		val height = size.height
		val centerY = height / 2

		val path = Path().apply {
			moveTo(0f, centerY)

			var x = 0f
			while (x < width) {
				val y = centerY + sin((x / wavelength) * 2 * Math.PI.toFloat()) * amplitude
				lineTo(x, y)
				x += 2f
			}
		}

		drawPath(
			path = path,
			color = color,
			style = Stroke(
				width = strokeWidth,
				cap = StrokeCap.Round
			)
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun WavyDividerPreview() {
	MinusTheme {
		WavyDivider(
			text = stringResource(R.string.wavy_divider_show_all_past_period_expenses),
			amplitude = 3f,
			wavelength = 60f
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun WavyDividerHighAmplitudePreview() {
	MinusTheme {
		WavyDivider(
			text = "Testiiiing...",
			amplitude = 4f,
			wavelength = 50f
		)
	}
}
