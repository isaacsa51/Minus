package com.serranoie.app.minus.presentation.ui.theme.component.date

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import java.math.BigDecimal
import java.text.NumberFormat

@Composable
fun DayTotalItem(total: BigDecimal, currencyFormat: NumberFormat) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 4.dp),
		horizontalArrangement = Arrangement.End
	) {
		Text(
			text = "Day total: ${currencyFormat.format(total)}",
			style = MaterialTheme.typography.labelLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
	}
}

@Preview
@Composable
private fun DayTotalPreview() {
	MinusTheme {
		DayTotalItem(
			total = BigDecimal("10.00"), currencyFormat = NumberFormat.getCurrencyInstance()
		)
	}
}