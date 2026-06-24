package com.serranoie.app.minus.presentation.ui.history.edit

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme

@Composable
internal fun RecurrenceToggleButton(
    isRecurrent: Boolean,
    selectedFrequency: RecurrentFrequency,
    subscriptionDay: Int,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.EventRepeat,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isRecurrent) {
                        stringResource(R.string.configure_recurrence)
                    } else {
                        stringResource(R.string.make_recurrent)
                    },
                    style = MaterialTheme.typography.labelSmallEmphasized
                )
            }

            if (isRecurrent) {
                val freqText = when (selectedFrequency) {
                    RecurrentFrequency.WEEKLY -> stringResource(R.string.weekly_with_desc)
                    RecurrentFrequency.BIWEEKLY -> stringResource(R.string.biweekly_with_desc)
                    RecurrentFrequency.MONTHLY -> stringResource(
                        R.string.recurrent_frequency_monthly,
                        subscriptionDay
                    )
                }
                Text(
                    text = freqText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecurrenceToggleButtonPreview() {
    MinusTheme {
        RecurrenceToggleButton(
            isRecurrent = true,
            selectedFrequency = RecurrentFrequency.MONTHLY,
            subscriptionDay = 15,
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecurrenceToggleButtonNotRecurrentPreview() {
    MinusTheme {
        RecurrenceToggleButton(
            isRecurrent = false,
            selectedFrequency = RecurrentFrequency.WEEKLY,
            subscriptionDay = 1,
            onClick = {},
        )
    }
}
