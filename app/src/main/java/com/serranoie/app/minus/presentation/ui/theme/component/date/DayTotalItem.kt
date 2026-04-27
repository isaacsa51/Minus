package com.serranoie.app.minus.presentation.ui.theme.component.date

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed
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
		modifier = modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp),
		horizontalArrangement = Arrangement.End
	) {
		Text(
			text = if (showLabel) {
				stringResource(R.string.day_total_format, currencyFormat.format(total))
			} else {
				currencyFormat.format(total)
			},
			style = MaterialTheme.typography.bodySmallCondensed,
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