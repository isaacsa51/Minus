package com.serranoie.app.minus.presentation.ui.theme.component.expense

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.component.CustomPaddedListItem
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed
import com.serranoie.app.minus.presentation.util.censor
import com.serranoie.app.minus.presentation.util.prettyDate
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale

data class UpcomingRecurrentItem(
	val transaction: Transaction,
	val nextChargeDate: LocalDate,
	val isInCurrentPeriod: Boolean
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpcomingRecurrentItemRow(
	item: UpcomingRecurrentItem,
	currencyFormat: NumberFormat,
	position: PaddedListItemPosition,
	isOutOfPeriod: Boolean = false,
	onClick: () -> Unit = {}
) {
	val transaction = item.transaction
	val nextChargeDate = item.nextChargeDate

	val shape = when (position) {
		PaddedListItemPosition.First -> androidx.compose.foundation.shape.RoundedCornerShape(
			topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp
		)

		PaddedListItemPosition.Last -> androidx.compose.foundation.shape.RoundedCornerShape(
			bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, topEnd = 4.dp
		)

		PaddedListItemPosition.Single -> androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
		PaddedListItemPosition.Middle -> androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
	}

	val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), nextChargeDate)
	val daysText = when {
		daysUntil == 0L -> stringResource(R.string.upcoming_recurrent_today)
		daysUntil == 1L -> stringResource(R.string.upcoming_recurrent_tomorrow)
		daysUntil < 7 -> stringResource(R.string.upcoming_recurrent_in_days, daysUntil)
		else -> stringResource(R.string.upcoming_recurrent_in_weeks, daysUntil / 7)
	}

	val alpha = if (isOutOfPeriod) 0.6f else 1f

	Surface(
		shape = shape, color = if (isOutOfPeriod) MaterialTheme.colorScheme.surfaceVariant
		else MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()
	) {
		CustomPaddedListItem(
			onClick = onClick,
			position = position,
			background = MaterialTheme.colorScheme.surface,
			contentColor = MaterialTheme.colorScheme.onSurface
		) {
			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = transaction.comment.ifEmpty { stringResource(R.string.upcoming_recurrent_unnamed_expense) },
					style = MaterialTheme.typography.titleMediumCondensed,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
					fontWeight = FontWeight.Medium
				)
				Text(
					text = daysText,
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.6f)
				)
			}

			Text(
				text = currencyFormat.format(transaction.amount),
				style = MaterialTheme.typography.titleSmallEmphasized,
				color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
				fontWeight = FontWeight.SemiBold,
				modifier = Modifier.censor()
			)
		}
	}
}

@Preview
@Composable
private fun UpcomingRecurrentItemRowPreview() {
	MinusTheme {
		UpcomingRecurrentItemRow(
			item = UpcomingRecurrentItem(
				transaction = Transaction(
					id = 1L,
					amount = java.math.BigDecimal("200.00"),
					comment = "Netflix Subscription",
					date = LocalDateTime.now(),
					isDeleted = false,
					isRecurrent = true
				),
				nextChargeDate = LocalDate.now().plusDays(3),
				isInCurrentPeriod = true
			),
			currencyFormat = NumberFormat.getCurrencyInstance(Locale.US),
			position = PaddedListItemPosition.Single,
			isOutOfPeriod = false,
			onClick = {}
		)
	}
}