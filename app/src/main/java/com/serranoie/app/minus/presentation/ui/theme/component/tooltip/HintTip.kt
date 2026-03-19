package com.serranoie.app.minus.presentation.ui.theme.component.tooltip

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme

enum class AnchorPosition {
	Start, Center, End,
}

@Composable
fun HintTip(
	modifier: Modifier = Modifier,
	position: AnchorPosition = AnchorPosition.Center,
	content: @Composable () -> Unit,
) {
	Column(
		modifier = modifier, horizontalAlignment = when (position) {
			AnchorPosition.Start -> Alignment.Start
			AnchorPosition.Center -> Alignment.CenterHorizontally
			AnchorPosition.End -> Alignment.End
		}
	) {
		Anchor(
			modifier = Modifier
				.height(12.dp)
				.width(48.dp),
			position = position,
			tint = MaterialTheme.colorScheme.secondary
		)

		Box(
			modifier = Modifier
				.background(
					MaterialTheme.colorScheme.secondary, when (position) {
						AnchorPosition.Start -> RoundedCornerShape(0.dp, 24.dp, 24.dp, 24.dp)
                        AnchorPosition.Center -> RoundedCornerShape(24.dp)
                        AnchorPosition.End -> RoundedCornerShape(24.dp, 0.dp, 24.dp, 24.dp)
					}
				)
				.padding(24.dp, 12.dp)
		) {
			content()
		}
	}
}

@Composable
private fun Anchor(
	modifier: Modifier = Modifier,
	position: AnchorPosition = AnchorPosition.Center,
	tint: Color = LocalContentColor.current,
) {
	Canvas(modifier) {
		val width = size.width
		val height = size.height
		val halfWidth = width / 2
		val thirdHeight = height / 3
		val quarterHalfWidth = halfWidth / 4

		val wavyPath = Path().apply {
			moveTo(x = 0f, y = height)

			if (position === AnchorPosition.End) {
				relativeMoveTo(dx = halfWidth, dy = 0f)
			}

			if (position === AnchorPosition.End || position === AnchorPosition.Center) {
				relativeQuadraticTo(
					dx1 = quarterHalfWidth * 2f,
					dy1 = 0f,
					dx2 = quarterHalfWidth * 3f,
					dy2 = -thirdHeight * 1.4f
				)
				relativeQuadraticTo(
					dx1 = quarterHalfWidth * 0.75f,
					dy1 = -thirdHeight * 1f,
					dx2 = quarterHalfWidth,
					dy2 = -thirdHeight * 1.6f
				)
			}

			if (position === AnchorPosition.End) {
				relativeLineTo(
					dx = 0f,
					dy = thirdHeight * 3f,
				)
			}

			if (position === AnchorPosition.Start) {
				relativeLineTo(
					dx = 0f,
					dy = -thirdHeight * 3f,
				)
			}

			if (position === AnchorPosition.Start || position === AnchorPosition.Center) {
				relativeQuadraticTo(
					dx1 = quarterHalfWidth * 0.25f,
					dy1 = thirdHeight * 0.4f,
					dx2 = quarterHalfWidth,
					dy2 = thirdHeight * 1.6f
				)
				relativeQuadraticTo(
					dx1 = quarterHalfWidth * 1f,
					dy1 = thirdHeight * 1.4f,
					dx2 = quarterHalfWidth * 3f,
					dy2 = thirdHeight * 1.4f
				)
			}

			lineTo(0f, height)

			close()
		}

		drawPath(
			path = wavyPath, SolidColor(tint), style = Fill
		)
	}
}

@Preview
@Composable
private fun HintTipPreview() {
	MinusTheme {
		Column {
			HintTip {
				Text(text = "This is a hint", style = MaterialTheme.typography.bodyMedium)
			}

			HintTip(position = AnchorPosition.Start) {
				Text(
					text = "This is a hint at the start",
					style = MaterialTheme.typography.bodyMedium
				)
			}

			HintTip(position = AnchorPosition.End) {
				Text(
					text = "This is a hint at the end", style = MaterialTheme.typography.bodyMedium
				)
			}
		}
	}
}
