package com.serranoie.app.minus.presentation.ui.history.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.component.ticket.TicketCard

@Composable
internal fun TransactionDetailTicketCard(
    transaction: Transaction,
    totalAmountText: String,
    details: List<Pair<String, String>>,
    onMarkAsPaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    readOnly: Boolean,
    modifier: Modifier = Modifier,
) {
    TicketCard(
        backgroundColor = MaterialTheme.colorScheme.background,
        teethWidthDp = 20f,
        teethHeightDp = 4f,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
					.background(Color.Black)
					.padding(vertical = 8.dp, horizontal = 18.dp)
            ) {
                Text(
                    text = if (transaction.isRecurrent) "GASTO RECURRENTE" else "GASTO",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = TextUnit(26f, TextUnitType.Sp),
                )
            }

            Text(
                text = "Num. de Operación: #${transaction.id}",
                style = MaterialTheme.typography.bodyMediumCondensed,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            HorizontalDivider()

            Text(
                text = "MONTO TOTAL",
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Text(
                text = totalAmountText,
                style = MaterialTheme.typography.headlineLargeEmphasized,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            HorizontalDivider()

            details.forEach { (label, value) ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMediumCondensed,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMediumCondensed,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            }

            if (transaction.isRecurrent) {
                Button(
                    onClick = onMarkAsPaid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Marcar como pagado",
                        style = MaterialTheme.typography.labelSmallEmphasized,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Editar", style = MaterialTheme.typography.labelSmallEmphasized)
                }

                if (!readOnly) {
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text("Eliminar", style = MaterialTheme.typography.labelSmallEmphasized)
                    }
                }
            }
        }
    }
}
