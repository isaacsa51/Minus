package com.serranoie.app.minus.presentation.ui.theme.component.expense

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed

@Composable
fun RecurrentPaymentsDivider(
	title: String,
	isExpanded: Boolean,
	onToggleClick: () -> Unit,
	itemCount: Int,
	isSecondary: Boolean = false,
	modifier: Modifier = Modifier
) {
	val interactionSource = remember { MutableInteractionSource() }
	val color = if (isSecondary) MaterialTheme.colorScheme.onSurfaceVariant
	else MaterialTheme.colorScheme.primary

	Row(
		modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onToggleClick, interactionSource = interactionSource, indication = null
            )
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.SpaceBetween
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp)
		) {
			Icon(
				imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
				contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
				tint = color,
				modifier = Modifier
			)

			Text(
				text = title, style = MaterialTheme.typography.labelMediumCondensed, color = color
			)
		}

		Text(
			text = "$itemCount",
			style = MaterialTheme.typography.labelMediumCondensed,
			color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
		)
	}
}

@Preview
@Composable
private fun RecurrentPaymentsDividerPreview() {
	MinusTheme {
		RecurrentPaymentsDivider(
			title = stringResource(R.string.recurrent_payments_divider_title_upcoming),
			isExpanded = true,
			onToggleClick = {},
			itemCount = 5,
			isSecondary = false,
			modifier = Modifier.fillMaxWidth()
		)
	}
}