package com.serranoie.app.minus.presentation.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import com.serranoie.app.minus.presentation.util.prettyDate
import java.text.NumberFormat
import java.time.LocalDate

@Composable
internal fun TransactionDetailDialog(
	transaction: Transaction?,
	currencyFormat: NumberFormat,
	readOnly: Boolean,
	isDismissingTransactionDialog: Boolean,
	onDismissStart: () -> Unit,
	onDismiss: () -> Unit,
	onMarkAsPaid: () -> Unit,
	onEdit: (Transaction) -> Unit,
	onDelete: (Transaction) -> Unit,
) {
	if (transaction == null) return

	val transactionDateText = transaction.date?.let { date ->
		prettyDate(date, showTime = true, forceHideDate = false, human = true)
	} ?: "Sin fecha"
	val recurrenceLabel = when (transaction.recurrentFrequency) {
		RecurrentFrequency.WEEKLY -> "Semanal"
		RecurrentFrequency.BIWEEKLY -> "Quincenal"
		RecurrentFrequency.MONTHLY -> "Mensual"
		null -> ""
	}

	val details = buildList {
		add("Descripción" to transaction.comment.ifEmpty { "Sin nombre" })
		add("Fecha" to transactionDateText)
		if (transaction.isRecurrent && recurrenceLabel.isNotEmpty()) {
			add("Frecuencia" to recurrenceLabel)
		}
		transaction.subscriptionDay?.let { day ->
			if (transaction.isRecurrent) {
				add("Día de cobro" to "Día $day")
			}
		}
		transaction.recurrentEndDate?.let { endDate ->
			if (transaction.isRecurrent) {
				add(
					"Fin recurrencia" to prettyDate(
						endDate,
						showTime = false,
						forceHideDate = false,
						human = true,
					)
				)
			}
		}
	}

	AnimatedVisibility(
		visible = true,
		enter = EnterTransition.None,
		exit = ExitTransition.None,
	) {
		Box(modifier = Modifier.fillMaxSize()) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(Color.Black.copy(alpha = 0.5f))
					.clickable(
						interactionSource = remember { MutableInteractionSource() },
						indication = null,
					) {
						if (!isDismissingTransactionDialog) {
							onDismissStart()
							onDismiss()
						}
					},
			)
			TransactionDetailTicketCard(
				transaction = transaction,
				totalAmountText = currencyFormat.format(transaction.amount),
				details = details,
				onMarkAsPaid = onMarkAsPaid,
				onEdit = { onEdit(transaction) },
				onDelete = { onDelete(transaction) },
				readOnly = readOnly,
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 24.dp)
					.align(Alignment.Center),
			)
		}
	}
}

@Composable
internal fun TransactionEditDialog(
	transaction: Transaction?,
	budgetStartDate: LocalDate,
	budgetEndDate: LocalDate,
	currencyCode: String,
	onCancel: () -> Unit,
	onSave: (Transaction) -> Unit,
) {
	if (transaction == null) return

	Dialog(
		onDismissRequest = onCancel,
		properties = DialogProperties(
			usePlatformDefaultWidth = false,
			dismissOnBackPress = true,
			dismissOnClickOutside = false,
		),
	) {
		Surface(
			modifier = Modifier.fillMaxSize(),
			color = MaterialTheme.colorScheme.background,
		) {
			TransactionEditScreen(
				transaction = transaction,
				budgetStartDate = budgetStartDate,
				budgetEndDate = budgetEndDate,
				currencyCode = currencyCode,
				onCancel = onCancel,
				onSave = { newAmount, newComment, newDateTime, newIsRecurrent, newFrequency, newEndDate, newSubscriptionDay ->
					val updatedTransaction = transaction.copy(
						id = transaction.sourceTransactionId ?: transaction.id,
						amount = newAmount,
						comment = newComment,
						date = newDateTime,
						isRecurrent = newIsRecurrent,
						recurrentFrequency = newFrequency,
						recurrentEndDate = newEndDate?.atStartOfDay(),
						subscriptionDay = newSubscriptionDay,
						sourceTransactionId = null,
					)
					onSave(updatedTransaction)
				},
			)
		}
	}
}

@Composable
internal fun DeleteRecurrentExpenseDialog(
	transaction: Transaction?,
	onDismiss: () -> Unit,
	onConfirm: (Transaction) -> Unit,
) {
	if (transaction == null) return

	AlertDialog(
		onDismissRequest = onDismiss,
		title = {
			Text(
				stringResource(R.string.delete_recurrent_expense_title),
				style = MaterialTheme.typography.titleLargeEmphasized,
			)
		},
		text = {
			Column {
				val recurrentExpenseName = transaction.comment.ifEmpty {
					stringResource(R.string.delete_recurrent_expense_fallback_name)
				}
				Text(
					text = stringResource(
						R.string.delete_recurrent_expense_message,
						recurrentExpenseName,
					),
					style = MaterialTheme.typography.bodyMediumCondensed,
				)
				Spacer(modifier = Modifier.height(8.dp))
				Text(
					text = stringResource(R.string.delete_recurrent_expense_warning),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
		},
		confirmButton = {
			Button(
				onClick = { onConfirm(transaction) },
				colors = ButtonDefaults.buttonColors(
					containerColor = MaterialTheme.colorScheme.error,
				),
			) {
				Text(
					stringResource(R.string.delete),
					style = MaterialTheme.typography.labelMediumEmphasized,
				)
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text(
					text = stringResource(R.string.cancel),
					style = MaterialTheme.typography.labelMediumEmphasized,
				)
			}
		},
	)
}
