package com.serranoie.app.minus.presentation.ui.theme.component.ticket

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.CustomPaddedListItem
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition

/**
 * A card component that uses TicketView as its container.
 *
 * @param header Optional header content to display at the top of the ticket
 * @param clickable Optional lambda to handle click events, can be null for non-clickable card
 * @param actions Optional actions to display at the bottom of the ticket (e.g., buttons)
 * @param modifier Modifier for the card
 * @param backgroundColor Background color for the ticket shape
 * @param teethWidthDp Width of the ticket "teeth" in dp
 * @param teethHeightDp Height of the ticket "teeth" in dp
 * @param content Main content of the ticket
 */
@Composable
fun TicketCard(
	modifier: Modifier = Modifier,
	header: (@Composable () -> Unit)? = null,
	clickable: (() -> Unit)? = null,
	actions: (@Composable () -> Unit)? = null,
	backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
	teethWidthDp: Float = 15f,
	teethHeightDp: Float = 3f,
	content: @Composable () -> Unit
) {
	TicketView(
		modifier = modifier.fillMaxWidth(),
		backgroundColor = backgroundColor,
		teethWidthDp = teethWidthDp,
		teethHeightDp = teethHeightDp,
		onClick = clickable
	) {
		Column(
			modifier = Modifier.fillMaxWidth()
		) {
			if (header != null) {
				header()
				Spacer(modifier = Modifier.height(12.dp))
			}

			content()

			if (actions != null) {
				Spacer(modifier = Modifier.height(12.dp))
				actions()
			}
		}
	}
}

/**
 * A simplified version of TicketCard that works with transaction data.
 * Shows transaction details in a ticket format.
 *
 * @param amountFormatted The formatted amount to display
 * @param comment The transaction comment/description
 * @param dateText Formatted date string
 * @param isRecurrent Whether this is a recurrent transaction
 * @param frequencyLabel Optional label for the recurrence frequency
 * @param onClick Optional click handler
 * @param modifier Modifier for the card
 */
@Composable
fun TransactionTicketCard(
	amountFormatted: String,
	comment: String,
	dateText: String,
	isRecurrent: Boolean,
	frequencyLabel: String? = null,
	onClick: (() -> Unit)? = null,
	modifier: Modifier = Modifier,
	backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
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
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.Top
			) {
				Column(modifier = Modifier.weight(1f)) {
					CustomPaddedListItem(
						onClick = {},
						position = PaddedListItemPosition.Single,
						background = Color.Transparent,
						contentColor = MaterialTheme.colorScheme.onSurface
					) {
						Column {
							Text(
								text = comment.ifEmpty { "Gasto sin nombre" },
								style = MaterialTheme.typography.titleMedium,
								color = MaterialTheme.colorScheme.onSurface
							)
							Text(
								text = if (isRecurrent) {
									"Frecuencia: ${frequencyLabel ?: "Recurrente"}"
								} else {
									"Transacción única"
								},
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant
							)
						}
					}
				}

				Text(
					text = amountFormatted,
					style = MaterialTheme.typography.headlineSmall,
					color = MaterialTheme.colorScheme.primary,
					modifier = Modifier.padding(end = 16.dp)
				)
			}

			Spacer(modifier = Modifier.height(8.dp))

			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text(
					text = "Fecha",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
				Text(
					text = dateText,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurface
				)
			}

			if (isRecurrent) {
				Spacer(modifier = Modifier.height(8.dp))
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 16.dp),
					horizontalArrangement = Arrangement.Start
				) {
					SuggestionChip(
						onClick = {},
						label = {
							Text(
								text = "Recurrente",
								style = MaterialTheme.typography.labelSmall
							)
						},
						icon = {
							Icon(
								imageVector = Icons.Rounded.Repeat,
								contentDescription = null,
								modifier = Modifier.padding(end = 4.dp)
							)
						}
					)
				}
			}
		}
	}
}

@Preview
@Composable
private fun TicketCardPreview() {
	MinusTheme {
		Column(
			modifier = Modifier.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			TicketCard(
				backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
				teethWidthDp = 15f,
				teethHeightDp = 3f
			) {
				Column(modifier = Modifier.fillMaxWidth()) {
					Text(
						text = "Ticket Simple",
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold
					)
					Spacer(modifier = Modifier.height(4.dp))
					Text(
						text = "Este es un ticket básico sin header ni actions",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}

			TicketCard(
				backgroundColor = MaterialTheme.colorScheme.primaryContainer,
				teethWidthDp = 12f,
				teethHeightDp = 4f,
				header = {
					Text(
						text = "EVENTO",
						style = MaterialTheme.typography.labelSmall,
						color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
					)
				}
			) {
				Column(modifier = Modifier.fillMaxWidth()) {
					Text(
						text = "Concierto de Rock",
						style = MaterialTheme.typography.titleLarge,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.onPrimaryContainer
					)
					Spacer(modifier = Modifier.height(4.dp))
					Text(
						text = "15 de Diciembre, 2025",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
					)
					Text(
						text = "8:00 PM",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
					)
				}
			}

			TicketCard(
				backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
				teethWidthDp = 10f,
				teethHeightDp = 2f,
				header = {
					Text(
						text = "PAGO RECURRENTE",
						style = MaterialTheme.typography.labelSmall,
						color = MaterialTheme.colorScheme.primary
					)
				},
				clickable = { /* Handle click */ },
				actions = {
					Button(
						onClick = { },
						modifier = Modifier.fillMaxWidth()
					) {
						Text("Ver Detalles")
					}
				}
			) {
				Column(modifier = Modifier.fillMaxWidth()) {
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.Top
					) {
						Column(modifier = Modifier.weight(1f)) {
							Text(
								text = "Netflix",
								style = MaterialTheme.typography.titleMedium,
								fontWeight = FontWeight.Bold
							)
							Text(
								text = "Suscripción mensual",
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant
							)
						}
						Text(
							text = "$15.99",
							style = MaterialTheme.typography.headlineSmall,
							color = MaterialTheme.colorScheme.primary,
							fontWeight = FontWeight.Bold,
							modifier = Modifier.padding(end = 16.dp)
						)
					}

					Spacer(modifier = Modifier.height(8.dp))

					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 16.dp),
						horizontalArrangement = Arrangement.SpaceBetween
					) {
						Text(
							text = "Próximo cobro",
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
						Text(
							text = "15 Feb 2026",
							style = MaterialTheme.typography.bodyMedium
						)
					}

					Spacer(modifier = Modifier.height(8.dp))

					SuggestionChip(
						onClick = { },
						label = { Text("Mensual", style = MaterialTheme.typography.labelSmall) },
						icon = {
							Icon(
								imageVector = Icons.Rounded.Repeat,
								contentDescription = null,
								modifier = Modifier.size(16.dp)
							)
						}
					)
				}
			}
		}
	}
}

@Preview
@Composable
private fun TransactionTicketCardPreview() {
	MinusTheme {
		Column(
			modifier = Modifier.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			TransactionTicketCard(
				amountFormatted = "$45.00",
				comment = "Supermercado",
				dateText = "28 Mar 2026",
				isRecurrent = false
			)

			TransactionTicketCard(
				amountFormatted = "$199.00",
				comment = "Netflix",
				dateText = "15 Mar 2026",
				isRecurrent = true,
				frequencyLabel = "Mensual"
			)

			TransactionTicketCard(
				amountFormatted = "$9.99",
				comment = "Spotify",
				dateText = "3 Abr 2026",
				isRecurrent = true,
				frequencyLabel = "Mensual",
				onClick = { }
			)

			// Empty comment transaction
			TransactionTicketCard(
				amountFormatted = "$25.50",
				comment = "",
				dateText = "30 Mar 2026",
				isRecurrent = false
			)
		}
	}
}