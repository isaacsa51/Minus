package com.serranoie.app.minus.presentation.ui.theme.component.date

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.prettyDate
import java.time.LocalDate

/**
 * A clickable date divider for history items that can expand/collapse.
 *
 * @param date The date to display
 * @param isExpanded Whether the section is currently expanded
 * @param onToggleClick Callback when the divider is clicked
 * @param totalAmount Optional total amount to display for the day
 * @param currencyCode Optional currency code for formatting
 */
@Composable
fun HistoryDateDivider(
	date: LocalDate?,
	isExpanded: Boolean = true,
	onToggleClick: () -> Unit = {},
	totalAmount: String? = null,
	currencyCode: String = "",
) {
	val interactionSource = remember { MutableInteractionSource() }

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onToggleClick,
				interactionSource = interactionSource,
				indication = null
			)
			.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.SpaceBetween
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp)
		) {
			Icon(
				imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
				contentDescription = if (isExpanded) "Collapse" else "Expand",
				tint = MaterialTheme.colorScheme.primary,
				modifier = Modifier
			)

			Text(
				text = prettyDate(date?.atStartOfDay(), forceShowDate = true, showTime = false, human = true),
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.primary
			)
		}

		// Show total amount if provided
		if (totalAmount != null) {
			val totalText = "$currencyCode$totalAmount"
			Text(
				text = totalText,
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
			)
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun DayPreviewDividerExpanded() {
	MinusTheme {
		HistoryDateDivider(
			date = LocalDate.now(),
			isExpanded = true,
			totalAmount = "150.00",
			currencyCode = "$"
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun DayPreviewDividerCollapsed() {
	MinusTheme {
		HistoryDateDivider(
			date = LocalDate.now(),
			isExpanded = false,
			totalAmount = "150.00",
			currencyCode = "$"
		)
	}
}
