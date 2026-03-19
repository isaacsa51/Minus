package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.component.StatCard
import com.serranoie.app.minus.presentation.util.numberFormat
import java.math.RoundingMode
import java.time.LocalDateTime

@Composable
fun AverageSpendCard(
	modifier: Modifier = Modifier,
	spends: List<Transaction>,
	currency: String = "MXN",
) {
	val context = LocalContext.current

	StatCard(
		modifier = modifier,
		value = if (spends.isNotEmpty()) {
			numberFormat(
				context,
				spends
					.reduce { acc, spent -> acc.copy(amount = acc.amount + spent.amount) }
					.amount
					.divide(spends.size.toBigDecimal(), 2, RoundingMode.HALF_EVEN),
				currency = currency,
			)
		} else {
			"-"
		},
		label = "Promedio gastado",
		contentPadding = PaddingValues(vertical = 8.dp, horizontal = 32.dp)
	)
}


@Preview(name = "AverageSpendCard")
@Composable
private fun PreviewAverageSpendCard() {
	AverageSpendCard(
		spends = listOf(
			Transaction(
				amount = 100.toBigDecimal(),
				date = LocalDateTime.now(),
				comment = "Category",
			),
		)
	)
}