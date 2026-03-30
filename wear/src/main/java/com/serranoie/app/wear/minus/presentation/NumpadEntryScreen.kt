package com.serranoie.app.wear.minus.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TextButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.serranoie.app.wear.minus.presentation.theme.MinusTheme

@Composable
internal fun NumpadEntryScreen(
	amount: String,
	onDigit: (String) -> Unit,
	onDot: () -> Unit,
	onBackspace: () -> Unit,
	onContinue: () -> Unit
) {
	val listState = rememberTransformingLazyColumnState()
	val canContinue = amountToBigDecimalOrZero(amount) > BigDecimal.ZERO

	AppScaffold(timeText = {}) {
		ScreenScaffold(
			scrollState = listState, timeText = null, edgeButton = {
				EdgeButton(onClick = onContinue, enabled = canContinue) {
					Text(text = "+")
				}
			}) {
			TransformingLazyColumn(
				state = listState,
				modifier = Modifier.fillMaxSize(),
				contentPadding = PaddingValues(top = 14.dp, bottom = 65.dp),
				verticalArrangement = Arrangement.spacedBy(4.dp),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				item {
					Text(
						text = visualTransformationAsCurrency(amount),
						style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
						color = MaterialTheme.colorScheme.onSurface
					)
				}

				items(listOf("1" to "2" to "3", "4" to "5" to "6", "7" to "8" to "9")) { row ->
					val (leftPair, right) = row
					val (left, middle) = leftPair
					KeypadRow(modifier = Modifier.fillMaxWidth(), left = { slot ->
						NumberKey(
							text = left, modifier = slot
						) { onDigit(left) }
					}, middle = { slot ->
						NumberKey(text = middle, modifier = slot) {
							onDigit(
								middle
							)
						}
					}, right = { slot ->
						NumberKey(
							text = right, modifier = slot
						) { onDigit(right) }
					})
				}

				item {
					KeypadRow(
						modifier = Modifier.fillMaxWidth(),
						left = { slot -> NumberKey(text = ".", modifier = slot) { onDot() } },
						middle = { slot ->
							NumberKey(
								text = "0", modifier = slot
							) { onDigit("0") }
						},
						right = { slot -> DeleteKey(modifier = slot) { onBackspace() } },
					)
				}
			}
		}
	}
}

@Composable
private fun KeypadRow(
	left: @Composable (Modifier) -> Unit,
	middle: @Composable (Modifier) -> Unit,
	right: @Composable (Modifier) -> Unit,
	modifier: Modifier
) {
	Row(
		horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
		modifier = modifier.padding(horizontal = 16.dp)
	) {
		val slotModifier = Modifier.weight(1f)
		left(slotModifier)
		middle(slotModifier)
		right(slotModifier)
	}
}

@Composable
private fun NumberKey(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
	TextButton(
		onClick = onClick,
		modifier = modifier
			.height(32.dp)
			.width(32.dp),
		colors = TextButtonDefaults.textButtonColors(
			containerColor = MaterialTheme.colorScheme.surfaceContainer,
			contentColor = MaterialTheme.colorScheme.primary
		),
		shapes = TextButtonDefaults.animatedShapes(),
	) {
		Text(
			text = text,
			modifier.fillMaxSize(),
			textAlign = TextAlign.Center,
			style = MaterialTheme.typography.labelMedium
		)
	}
}

@Composable
private fun DeleteKey(modifier: Modifier = Modifier, onClick: () -> Unit) {
	FilledIconButton(
		onClick = onClick,
		shapes = IconButtonDefaults.animatedShapes(),
		modifier = modifier
			.height(38.dp)
			.width(38.dp)
	) {
		Text(text = "⌫", fontSize = 18.sp)
	}
}

private fun visualTransformationAsCurrency(rawAmount: String): String {
	val amount = amountToBigDecimalOrZero(rawAmount)
	return NumberFormat.getCurrencyInstance(Locale.US).format(amount)
}

private fun amountToBigDecimalOrZero(rawAmount: String): BigDecimal {
	if (rawAmount.isBlank()) return BigDecimal.ZERO
	return rawAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
}

@Preview(device = "id:wearos_small_round", showSystemUi = false)
@Composable
private fun NumpadEntryScreenPreview() {
	MinusTheme {
		NumpadEntryScreen(
			amount = "12,500.00",
			onDigit = {},
			onDot = {},
			onBackspace = {},
			onContinue = {})
	}
}
