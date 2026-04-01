package com.serranoie.app.minus.presentation.ui.theme.component.ticket

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import kotlin.math.floor

class TicketShape(
	private val teethWidthDp: Float, private val teethHeightDp: Float
) : Shape {

	override fun createOutline(
		size: Size, layoutDirection: LayoutDirection, density: Density
	) = Outline.Generic(Path().apply {

		moveTo(
			size.width * 0.99f, size.height * 0.01f
		)

		val teethHeightPx = teethHeightDp * density.density
		var fullTeethWidthPx = teethWidthDp * density.density
		var halfTeethWidthPx = fullTeethWidthPx / 2
		var currentDrawPositionX = size.width * 0.99f
		var teethBasePositionY = size.height * 0.01f + teethHeightPx
		val shapeWidthPx = size.width * 0.99f - size.width * 0.01f

		val teethCount = shapeWidthPx / fullTeethWidthPx
		val minTeethCount = floor(teethCount)

		if (teethCount != minTeethCount) {
			val newTeethWidthPx = shapeWidthPx / minTeethCount
			fullTeethWidthPx = newTeethWidthPx
			halfTeethWidthPx = fullTeethWidthPx / 2
		}

		var drawnTeethCount = 1

		lineTo(
			currentDrawPositionX - halfTeethWidthPx, teethBasePositionY + teethHeightPx
		)

		while (drawnTeethCount < minTeethCount) {

			currentDrawPositionX -= halfTeethWidthPx

			lineTo(
				currentDrawPositionX - halfTeethWidthPx, teethBasePositionY - teethHeightPx
			)

			currentDrawPositionX -= halfTeethWidthPx

			lineTo(
				currentDrawPositionX - halfTeethWidthPx, teethBasePositionY + teethHeightPx
			)

			drawnTeethCount++
		}

		currentDrawPositionX -= halfTeethWidthPx

		lineTo(
			currentDrawPositionX - halfTeethWidthPx, teethBasePositionY - teethHeightPx
		)

		// draw left edge
		lineTo(
			size.width * 0.01f, size.height * 0.99f
		)

		drawnTeethCount = 1
		teethBasePositionY = size.height * 0.99f - teethHeightPx
		currentDrawPositionX = size.width * 0.01f

		lineTo(
			currentDrawPositionX, teethBasePositionY + teethHeightPx
		)

		lineTo(
			currentDrawPositionX + halfTeethWidthPx, teethBasePositionY - teethHeightPx
		)

		while (drawnTeethCount < minTeethCount) {

			currentDrawPositionX += halfTeethWidthPx

			lineTo(
				currentDrawPositionX + halfTeethWidthPx, teethBasePositionY + teethHeightPx
			)

			currentDrawPositionX += halfTeethWidthPx

			lineTo(
				currentDrawPositionX + halfTeethWidthPx, teethBasePositionY - teethHeightPx
			)

			drawnTeethCount++
		}

		currentDrawPositionX += halfTeethWidthPx

		lineTo(
			currentDrawPositionX + halfTeethWidthPx, teethBasePositionY + teethHeightPx
		)

		close()
	})
}

@Composable
fun TicketView(
	modifier: Modifier = Modifier,
	backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
	teethWidthDp: Float = 15f,
	teethHeightDp: Float = 3f,
	onClick: (() -> Unit)? = null,
	content: @Composable () -> Unit
) {
	val ticketShape = TicketShape(teethWidthDp, teethHeightDp)

	val clickableModifier = if (onClick != null) {
		Modifier.clickable { onClick() }
	} else {
		Modifier
	}

	Box(
		modifier = modifier
			.background(backgroundColor, shape = ticketShape)
			.then(clickableModifier)
			.padding(16.dp)) {
		content()
	}
}

@Preview
@Composable
private fun SimpleTicketPreview() {
	MinusTheme {
		Column(modifier = Modifier.padding(16.dp)) {
			// Basic ticket
			TicketView(
				backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
				teethWidthDp = 12f,
				teethHeightDp = 6f
			) {
				Column {
					Text(
						text = "Flight AB123",
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold
					)
					Text(
						text = "NYC → LAX",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}

			Spacer(modifier = Modifier.height(8.dp))

			// Clickable ticket with more content
			TicketView(
				backgroundColor = MaterialTheme.colorScheme.primaryContainer,
				teethWidthDp = 15f,
				teethHeightDp = 4f,
				onClick = { /* Handle ticket click */ }) {
				Column {
					Text(
						text = "Concert Ticket",
						style = MaterialTheme.typography.titleLarge,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.onPrimaryContainer
					)
					Spacer(modifier = Modifier.height(8.dp))
					Text(
						text = "The Rolling Stones",
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.onPrimaryContainer
					)
					Text(
						text = "Madison Square Garden",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
					)
					Text(
						text = "Dec 15, 2025 • 8:00 PM",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
					)
				}
			}

			Spacer(modifier = Modifier.height(8.dp))

			// Travel ticket
			TicketView(
				backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
				teethWidthDp = 10f,
				teethHeightDp = 5f,
				onClick = { /* Handle travel ticket click */ }) {
				Column {
					Text(
						text = "BOARDING PASS",
						style = MaterialTheme.typography.labelSmall,
						color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
					)
					Text(
						text = "American Airlines",
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.onTertiaryContainer
					)
					Spacer(modifier = Modifier.height(8.dp))
					Text(
						text = "JFK → LAX",
						style = MaterialTheme.typography.headlineSmall,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.onTertiaryContainer
					)
					Text(
						text = "Flight AA1234 • Seat 12A",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
					)
				}
			}
		}
	}
}
