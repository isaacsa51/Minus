package com.serranoie.app.minus.presentation.ui.theme.component.ticket

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme

/**
 * A compact ticket card showing the next recurrent payment within a period.
 * Shows essential info: title, amount, and next charge date.
 *
 * @param title The name/description of the recurrent payment
 * @param amountFormatted The formatted amount to display
 * @param nextChargeDate The date of the next charge
 * @param frequencyLabel Optional label for the recurrence frequency
 * @param onClick Optional click handler
 * @param modifier Modifier for the card
 * @param backgroundColor Background color for the ticket
 * @param teethWidthDp Width of the ticket "teeth"
 * @param teethHeightDp Height of the ticket "teeth"
 */
@Composable
fun RecurrentTicketCard(
    title: String,
    amountFormatted: String,
    nextChargeDate: String,
    frequencyLabel: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    teethWidthDp: Float = 12f,
    teethHeightDp: Float = 2f
) {
    TicketCard(
        modifier = modifier,
        backgroundColor = backgroundColor,
        teethWidthDp = teethWidthDp,
        teethHeightDp = teethHeightDp,
        clickable = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title.ifEmpty { "Pago recurrente" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Amount
                Text(
                    text = amountFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = nextChargeDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                frequencyLabel?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Preview(device = "id:wearos_square")
@Composable
private fun RecurrentTicketCardPreview() {
    MinusTheme {
        RecurrentTicketCard(
            title = "Netflix",
            amountFormatted = "$15.99",
            nextChargeDate = "15 abr",
            frequencyLabel = "Mensual",
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Preview
@Composable
private fun RecurrentTicketCardSecondaryPreview() {
    MinusTheme {
        RecurrentTicketCard(
            title = "Spotify Premium",
            amountFormatted = "$9.99",
            nextChargeDate = "22 abr",
            frequencyLabel = "Mensual",
            backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}