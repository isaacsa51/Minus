package com.serranoie.app.minus.presentation.ui.theme.component.ticket

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.tooling.preview.Preview
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp

/**
 * A custom popup that displays content inside a TicketView container.
 * Shows a fullscreen background with fade/scale animation and dismisses when clicking outside.
 *
 * @param showPopup Whether the popup is visible
 * @param onClickOutside Callback when user clicks outside the popup
 * @param backgroundColor Background color for the overlay (default: semi-transparent dark)
 * @param ticketBackgroundColor Background color for the ticket itself
 * @param teethWidthDp Width of the ticket "teeth" in dp
 * @param teethHeightDp Height of the ticket "teeth" in dp
 * @param modifier Modifier for the popup
 * @param content Content to display inside the ticket
 */
@Composable
fun TicketPopup(
	showPopup: Boolean,
	onClickOutside: () -> Unit,
	modifier: Modifier = Modifier,
	backgroundColor: Color = Color.Black.copy(alpha = 0.5f),
	ticketBackgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
	teethWidthDp: Float = 12f,
	teethHeightDp: Float = 3f,
	content: @Composable () -> Unit
) {
	var isVisible by remember { mutableStateOf(false) }

	LaunchedEffect(showPopup) {
		isVisible = showPopup
	}

	val scale by animateFloatAsState(
		targetValue = if (isVisible) 1f else 0.85f,
		animationSpec = tween(200),
		label = "scale"
	)

	val backgroundAlpha by animateFloatAsState(
		targetValue = if (isVisible) backgroundColor.alpha else 0f,
		animationSpec = tween(200),
		label = "backgroundAlpha"
	)

	AnimatedVisibility(
		visible = showPopup,
		enter = fadeIn(animationSpec = tween(200)) + androidx.compose.animation.scaleIn(
			initialScale = 0.85f,
			animationSpec = tween(200)
		),
		exit = fadeOut(animationSpec = tween(200)) + androidx.compose.animation.scaleOut(
			targetScale = 0.85f,
			animationSpec = tween(200)
		)
	) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(backgroundColor.copy(alpha = backgroundAlpha))
				.clickable(
					interactionSource = remember { MutableInteractionSource() }, indication = null
				) {
					onClickOutside()
				}
				.then(modifier), contentAlignment = Alignment.Center) {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 24.dp)
					.graphicsLayer {
						scaleX = scale
						scaleY = scale
					}) {
				TicketView(
					backgroundColor = ticketBackgroundColor,
					teethWidthDp = teethWidthDp,
					teethHeightDp = teethHeightDp,
					modifier = Modifier.fillMaxWidth()
				) {
					content()
				}
			}
		}
	}
}

/**
 * A simplified version of TicketPopup specifically for showing transaction details.
 */
@Composable
fun TransactionTicketPopup(
	showPopup: Boolean,
	onClickOutside: () -> Unit,
	isRecurrentExpense: Boolean,
	operationNumber: String,
	operationTime: String,
	totalAmountText: String,
	details: List<Pair<String, String>>,
	modifier: Modifier = Modifier,
	actions: (@Composable () -> Unit)? = null,
	onMarkAsPaid: (() -> Unit)? = null,
	backgroundColor: Color = Color.Black.copy(alpha = 0.5f),
	ticketBackgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
	teethWidthDp: Float = 12f,
	teethHeightDp: Float = 3f
) {
	TicketPopup(
		showPopup = showPopup,
		onClickOutside = onClickOutside,
		backgroundColor = backgroundColor,
		ticketBackgroundColor = ticketBackgroundColor,
		teethWidthDp = teethWidthDp,
		teethHeightDp = teethHeightDp,
		modifier = modifier
	) {
		TransactionTicketContent(
			isRecurrentExpense = isRecurrentExpense,
			operationNumber = operationNumber,
			operationTime = operationTime,
			totalAmountText = totalAmountText,
			details = details,
			actions = actions,
			onMarkAsPaid = onMarkAsPaid
		)
	}
}

@Composable
private fun TransactionTicketContent(
	isRecurrentExpense: Boolean,
	operationNumber: String,
	operationTime: String,
	totalAmountText: String,
	details: List<Pair<String, String>>,
	actions: (@Composable () -> Unit)? = null,
	onMarkAsPaid: (() -> Unit)? = null
) {
	Column(
		modifier = Modifier
			.padding(16.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Box(
			modifier = Modifier
				.background(Color.Black)
				.padding(vertical = 8.dp, horizontal = 18.dp),
			contentAlignment = Alignment.Center
		) {
			Text(
				text = if (isRecurrentExpense) "GASTO RECURRENTE" else "GASTO",
				color = Color.White,
				style = MaterialTheme.typography.labelLarge,
				fontWeight = FontWeight.Bold,
				fontSize = TextUnit(26f, TextUnitType.Sp),
				fontFamily = FontFamily.Monospace
			)
		}

		Text(
			text = "Num. de Operación: $operationNumber",
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurface,
			textAlign = TextAlign.Center
		)

		HorizontalDivider()

		Text(
			text = "MONTO TOTAL",
			style = MaterialTheme.typography.labelLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			textAlign = TextAlign.Center
		)
		Text(
			text = totalAmountText,
			style = MaterialTheme.typography.headlineLarge,
			color = MaterialTheme.colorScheme.error,
			fontWeight = FontWeight.Bold,
			textAlign = TextAlign.Center
		)

		HorizontalDivider()

		details.forEach { (label, value) ->
			PopupDetailRow(label = label, value = value)
		}

		if (isRecurrentExpense && onMarkAsPaid != null) {
			Button(
				onClick = onMarkAsPaid,
				modifier = Modifier.fillMaxWidth()
			) {
				Text(text = "Marcar como pagado")
			}
		}

		if (actions != null) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.Center
			) {
				actions()
			}
		}
	}
}

@Composable
private fun PopupDetailRow(
	label: String, value: String
) {
	Box(modifier = Modifier.fillMaxWidth()) {
		Text(
			text = label,
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.align(Alignment.CenterStart)
		)
		Text(
			text = value,
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurface,
			modifier = Modifier.align(Alignment.CenterEnd)
		)
	}
}

@Preview
@Composable
fun TicketPopupPreview() {
	MinusTheme {
		TransactionTicketPopup(
			showPopup = true,
			onClickOutside = { },
			isRecurrentExpense = true,
			operationNumber = "TXN-350543",
			operationTime = "01/04/2026 14:35",
			totalAmountText = "$123.00",
			details = listOf(
				"Ref No" to "12252576",
				"Approval No" to "12345478"
			),
			actions = {
				Text(
					text = "[ Edit ]   [ Delete ]",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.primary,
					modifier = Modifier.padding(top = 6.dp)
				)
			},
			onMarkAsPaid = { }
		)
	}
}