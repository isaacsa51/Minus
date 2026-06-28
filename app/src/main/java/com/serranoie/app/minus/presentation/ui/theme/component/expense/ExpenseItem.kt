package com.serranoie.app.minus.presentation.ui.theme.component.expense

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.CustomPaddedListItem
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed
import com.serranoie.app.minus.presentation.util.censor
import com.serranoie.app.minus.presentation.util.prettyDate
import java.text.NumberFormat
import java.time.LocalDateTime
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpenseItem(
    transaction: Transaction,
    currencyFormat: NumberFormat,
    position: PaddedListItemPosition = PaddedListItemPosition.Middle,
    onClick: () -> Unit = {},
    modifier: Modifier
) {
	CustomPaddedListItem(
		onClick = onClick,
		position = position,
		background = MaterialTheme.colorScheme.surface,
		contentColor = MaterialTheme.colorScheme.onSurface
	) {
		Column(modifier = Modifier.weight(1f)) {
			Text(
				text = transaction.comment.ifEmpty { stringResource(R.string.expense_item_unnamed_expense) },
				style = MaterialTheme.typography.titleMediumCondensed,
				color = MaterialTheme.colorScheme.onSurface,
				fontWeight = FontWeight.Medium
			)
			val timeText = prettyDate(
				date = transaction.date, showTime = true, forceHideDate = true
			)
			val subtitle = if (transaction.isRecurrent) {
				stringResource(R.string.expense_item_recurrent_subtitle_format, timeText)
			} else {
				timeText
			}
			Text(
				text = subtitle,
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
			)
		}

		Text(
			text = currencyFormat.format(transaction.amount),
			style = MaterialTheme.typography.titleSmallEmphasized,
			color = MaterialTheme.colorScheme.onSurface,
			fontWeight = FontWeight.SemiBold,
			modifier = Modifier.censor()
		)
	}
}

@Preview
@Composable
private fun ExpenseItemPreview() {
	MinusTheme {
		ExpenseItem(
            modifier = Modifier,
            transaction = Transaction(
                id = 1L,
                amount = java.math.BigDecimal("150.50"),
                comment = "Compra en supermercado",
                date = LocalDateTime.now(),
                isDeleted = false,
                isRecurrent = false
            ),
            currencyFormat = NumberFormat.getCurrencyInstance(Locale.US),
            position = PaddedListItemPosition.Single,
        )
	}
}