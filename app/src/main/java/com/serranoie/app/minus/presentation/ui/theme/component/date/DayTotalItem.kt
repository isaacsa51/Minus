package com.serranoie.app.minus.presentation.ui.theme.component.date

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import java.math.BigDecimal
import java.text.NumberFormat

@Composable
fun DayTotalItem(
	total: BigDecimal,
	currencyFormat: NumberFormat,
	modifier: Modifier = Modifier,
	showLabel: Boolean = true,
) {
	Row(
		modifier = modifier,
		horizontalArrangement = Arrangement.End
	) {
		val isIncome = total < BigDecimal.ZERO
		val absoluteTotal = total.abs()
		val formattedValue = if (isIncome) {
			"+${currencyFormat.format(absoluteTotal)}"
		} else {
			currencyFormat.format(total)
		}

		Text(
			text = if (showLabel) {
				stringResource(R.string.day_total_format, formattedValue)
			} else {
				formattedValue
			},
			style = MaterialTheme.typography.labelMediumCondensed,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun DayTotalPreview() {
	MinusTheme {
		DayTotalItem(
			total = BigDecimal("10.00"), currencyFormat = NumberFormat.getCurrencyInstance()
		)
	}
}
