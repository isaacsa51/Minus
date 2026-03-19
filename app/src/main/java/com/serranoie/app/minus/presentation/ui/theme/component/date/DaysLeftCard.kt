package com.serranoie.app.minus.presentation.ui.theme.component.date

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.countDays
import com.serranoie.app.minus.presentation.util.countDaysToToday
import java.util.Date

private const val TAG = "DaysLeftCard - ISAAC"

@Composable
fun DaysLeftCard(
	modifier: Modifier = Modifier,
	startDate: Date,
	finishDate: Date?,
) {
	Log.d(TAG, "DaysLeftCard: startDate=$startDate, finishDate=$finishDate")

	if (finishDate == null) {
		Log.d(TAG, "finishDate is null - returning empty Box")
		Box { }
		return
	}

	val days = countDays(finishDate, startDate)
	val restDays = countDaysToToday(finishDate)
	Log.d(TAG, "DaysLeftCard calculated: totalDays=$days, restDays=$restDays")
	Log.d(TAG, "DaysLeftCard progress: ${restDays / days.toFloat()}")

	Box(
		Modifier
			.widthIn(max = 120.dp)
			.aspectRatio(1f)
	) {
		Card(
			modifier = modifier
				.fillMaxSize()
				.clip(CircleShape),
			shape = CircleShape,
		) {
			val textColor = LocalContentColor.current

			Box(Modifier.fillMaxSize()) {
				Box(
					modifier = Modifier
						.background(
							MaterialTheme.colorScheme.primary,
							shape = ArcShape(
								thickness = 6.dp,
								progress = restDays / days.toFloat(),
							),
						)
						.fillMaxHeight()
						.fillMaxWidth(),
				)

				Column(
					modifier = Modifier
						.fillMaxSize(),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.Center,
				) {
					Text(
						text = restDays.toString(),
						style = MaterialTheme.typography.displayLarge.copy(
							fontSize = MaterialTheme.typography.titleLarge.fontSize
						)
					)
					Text(
						text = "Días restantes",
						style = MaterialTheme.typography.labelMedium,
						color = textColor.copy(alpha = 0.6f),
					)
					Spacer(modifier = Modifier.height(4.dp))
				}
			}
		}
	}
}

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
					size = Size(width = size.width - thicknessPx * 2, height = size.height - thicknessPx * 2),
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
@Preview(name = "DaysLeftCard")
@Composable
private fun PreviewDaysLeftCard() {
	MinusTheme {
		Surface {
			DaysLeftCard(
				startDate = Date(),
				finishDate = Date(),
			)
		}
	}
}