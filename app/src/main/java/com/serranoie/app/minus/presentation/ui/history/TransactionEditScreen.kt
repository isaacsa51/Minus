package com.serranoie.app.minus.presentation.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.editor.category.CategoryToolbar
import com.serranoie.app.minus.presentation.ui.editor.category.FocusController
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditMode
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditStage
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditorState
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.Numpad
import com.serranoie.app.minus.presentation.ui.theme.displayLargeCondensed
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.util.prettyDate
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.Transaction as NumpadTransaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditScreen(
	transaction: Transaction,
	budgetStartDate: LocalDate,
	budgetEndDate: LocalDate,
	currencyCode: String = "USD",
	onCancel: () -> Unit = {},
	onSave: (
		newAmount: BigDecimal, newComment: String, newDateTime: LocalDateTime, newIsRecurrent: Boolean, newFrequency: RecurrentFrequency?, newEndDate: LocalDate?, newSubscriptionDay: Int?
	) -> Unit = { _, _, _, _, _, _, _ -> },
	modifier: Modifier = Modifier
) {
	val currencyFormat = symbolOnlyCurrencyFormat(currencyCode)
	val scope = rememberCoroutineScope()

	var editedAmount by remember { mutableStateOf(transaction.amount.toString()) }
	var editedComment by remember { mutableStateOf(transaction.comment) }
	var editedDate by remember {
		mutableStateOf(
			transaction.date?.toLocalDate() ?: LocalDate.now()
		)
	}
	var editedTime by remember {
		mutableStateOf(transaction.date?.toLocalTime() ?: LocalTime.now())
	}

	var isRecurrent by remember { mutableStateOf(transaction.isRecurrent) }
	var selectedFrequency by remember {
		mutableStateOf(transaction.recurrentFrequency ?: RecurrentFrequency.MONTHLY)
	}
	var subscriptionDay by remember {
		mutableIntStateOf(transaction.subscriptionDay ?: transaction.date?.dayOfMonth ?: 1)
	}
	var recurrentEndDate by remember {
		mutableStateOf(transaction.recurrentEndDate?.toLocalDate() ?: budgetEndDate.plusMonths(3))
	}

	var showDatePicker by remember { mutableStateOf(false) }
	var showTimePicker by remember { mutableStateOf(false) }
	var showRecurrentBottomSheet by remember { mutableStateOf(false) }

	val focusController = remember { FocusController() }

	var isCalculation by remember { mutableStateOf(false) }

	// Calculate dynamic target height for numpad to take 48% of screen height
	val configuration = LocalConfiguration.current
	val screenHeight = configuration.screenHeightDp.dp
	val targetNumpadHeight = screenHeight * 0.48f

	val baseTextStyle = MaterialTheme.typography.displayLargeCondensed.copy(
		fontWeight = FontWeight.W500
	)

	val editorState = remember(editedAmount) {
		EditorState(
			mode = EditMode.EDIT,
			rawSpentValue = editedAmount,
			stage = EditStage.EDIT_SPENT,
			currentSpent = editedAmount,
			currentComment = editedComment,
			editedTransaction = transaction?.let {
				NumpadTransaction(
					id = it.id,
					amount = it.amount.toPlainString(),
					comment = it.comment,
					date = it.date?.atZone(ZoneId.systemDefault())?.toInstant()
						?.let { instant -> java.util.Date.from(instant) } ?: java.util.Date())
			})
	}

	Column(
		modifier = modifier
			.fillMaxSize()
			.statusBarsPadding()
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp, vertical = 8.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			IconButton(
				onClick = onCancel, modifier = Modifier.size(48.dp)
			) {
				Icon(
					imageVector = Icons.Default.Close,
					contentDescription = stringResource(R.string.cancel_edit_content_desc),
					tint = MaterialTheme.colorScheme.onSurface
				)
			}

			Text(
				text = if (transaction.isRecurrent) stringResource(R.string.edit_recurrent_expense_title) else stringResource(
					R.string.edit_expense_title
				),
				style = MaterialTheme.typography.titleMediumEmphasized,
				color = MaterialTheme.colorScheme.onSurface,
				modifier = Modifier
					.padding(start = 8.dp)
					.basicMarquee()
			)
		}

		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp, vertical = 8.dp),
			horizontalArrangement = Arrangement.End,
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = prettyDate(
					editedDate.atStartOfDay(), forceShowDate = true, showTime = false, human = false
				),
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.onSurface,
				modifier = Modifier
					.clickable(
						interactionSource = remember { MutableInteractionSource() },
						indication = null
					) { showDatePicker = true }
					.padding(horizontal = 2.dp, vertical = 4.dp))

			Text(
				text = "—",
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
				modifier = Modifier.padding(horizontal = 4.dp)
			)

			Text(
				text = String.format("%02d:%02d", editedTime.hour, editedTime.minute),
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.onSurface,
				modifier = Modifier
					.clickable(
						interactionSource = remember { MutableInteractionSource() },
						indication = null
					) { showTimePicker = true }
					.padding(horizontal = 2.dp, vertical = 4.dp))
		}

		Box(
			modifier = Modifier
				.fillMaxWidth()
				.weight(1f)
				.padding(16.dp),
			contentAlignment = Alignment.TopEnd
		) {
			val formattedAmount = remember(editedAmount) {
				try {
					val value = editedAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
					currencyFormat.format(value)
				} catch (e: Exception) {
					editedAmount
				}
			}

			Text(
				text = formattedAmount,
				style = baseTextStyle,
				color = MaterialTheme.colorScheme.onSurface,
				textAlign = TextAlign.End,
				modifier = Modifier.fillMaxWidth()
			)
		}

		if (transaction.isRecurrent) {
			Button(
				onClick = { showRecurrentBottomSheet = true },
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp, vertical = 8.dp),
				shape = MaterialTheme.shapes.medium
			) {
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
				) {
					Row(
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.spacedBy(8.dp)
					) {
						Icon(
							imageVector = Icons.Rounded.Repeat,
							contentDescription = null,
							modifier = Modifier.size(20.dp)
						)
						Text(
							text = if (isRecurrent) stringResource(R.string.configure_recurrence) else stringResource(
								R.string.make_recurrent
							), style = MaterialTheme.typography.labelSmallEmphasized
						)
					}

					if (isRecurrent) {
						val freqText = when (selectedFrequency) {
							RecurrentFrequency.WEEKLY -> stringResource(R.string.weekly_with_desc)
							RecurrentFrequency.BIWEEKLY -> stringResource(R.string.biweekly_with_desc)
							RecurrentFrequency.MONTHLY -> stringResource(
								R.string.recurrent_frequency_monthly, subscriptionDay
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

		Surface(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp, vertical = 8.dp),
			shape = MaterialTheme.shapes.medium
		) {
			CategoryToolbar(
				tags = emptyList(),
				currentComment = editedComment,
				stage = EditStage.EDIT_SPENT,
				onCommentUpdate = { editedComment = it },
				editorFocusController = focusController,
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp, vertical = 12.dp)
			)
		}

		Spacer(modifier = Modifier.height(16.dp))

		Numpad(
			modifier = Modifier
				.fillMaxWidth()
				.height(targetNumpadHeight),
			editorState = editorState,
			onNumberInput = { digit ->
				editedAmount = if (editedAmount == "0") {
					digit.toString()
				} else {
					editedAmount + digit.toString()
				}
			},
			onDotInput = {
				val lastChar = editedAmount.lastOrNull()
				if (editedAmount.isEmpty() || (lastChar != null && lastChar in "+-×÷")) {
					editedAmount += "0."
				} else {
					val lastOperatorIndex = editedAmount.indexOfLast { it in "+-×÷" }
					val currentSegment = editedAmount.substring(lastOperatorIndex + 1)
					if (!currentSegment.contains(".")) {
						editedAmount += "."
					}
				}
			},
			onBackspace = {
				editedAmount = editedAmount.dropLast(1).ifEmpty { "0" }
			},
			onBackspaceLongPress = {
				editedAmount = "0"
			},
			onApply = {
				val newAmount = editedAmount.toBigDecimalOrNull() ?: transaction.amount
				val frequency = if (isRecurrent) selectedFrequency else null
				val endDate = if (isRecurrent) recurrentEndDate else null
				val subDay = if (isRecurrent && selectedFrequency == RecurrentFrequency.MONTHLY) {
					subscriptionDay
				} else null

				onSave(
					newAmount,
					editedComment,
					editedDate.atTime(editedTime),
					isRecurrent,
					frequency,
					endDate,
					subDay
				)
			},
			isCalculation = isCalculation,
			onCalculationModeChanged = { isCalculation = it },
			onOperatorInput = { operator ->
				val lastChar = editedAmount.lastOrNull()
				if (editedAmount.isNotEmpty() && lastChar != null && lastChar !in "+-×÷" && lastChar != '.') {
					editedAmount += operator.toString()
				}
			},
			onEqualsInput = {
				val result = evaluateCalculation(editedAmount)
				if (result != null) {
					editedAmount = result
				}
			},
			onDelete = {
				onCancel()
			})
	}

	if (showDatePicker) {
		val datePickerState = rememberDatePickerState(
			initialSelectedDateMillis = editedDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
				.toEpochMilli()
		)

		DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
			TextButton(
				onClick = {
					datePickerState.selectedDateMillis?.let { millis ->
						val selectedDate =
							Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
								.toLocalDate()
						// Ensure date is within budget period
						editedDate = when {
							selectedDate.isBefore(budgetStartDate) -> budgetStartDate
							selectedDate.isAfter(budgetEndDate) -> budgetEndDate
							else -> selectedDate
						}
					}
					showDatePicker = false
				}) {
				Text(stringResource(R.string.accept))
			}
		}, dismissButton = {
			TextButton(onClick = { showDatePicker = false }) {
				Text(stringResource(R.string.cancel))
			}
		}) {
			DatePicker(state = datePickerState)
		}
	}

	if (showTimePicker) {
		val timePickerState = rememberTimePickerState(
			initialHour = editedTime.hour, initialMinute = editedTime.minute
		)

		Dialog(onDismissRequest = { showTimePicker = false }) {
			Surface(
				shape = MaterialTheme.shapes.large, tonalElevation = 6.dp
			) {
				Column(
					modifier = Modifier.padding(16.dp),
					horizontalAlignment = Alignment.CenterHorizontally
				) {
					Text(
						text = stringResource(R.string.select_time),
						style = MaterialTheme.typography.titleMedium,
						modifier = Modifier.padding(bottom = 16.dp)
					)

					TimePicker(state = timePickerState)

					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(top = 16.dp),
						horizontalArrangement = Arrangement.End
					) {
						TextButton(onClick = { showTimePicker = false }) {
							Text(stringResource(R.string.cancel))
						}

						TextButton(
							onClick = {
								editedTime = LocalTime.of(
									timePickerState.hour, timePickerState.minute
								)
								showTimePicker = false
							}) {
							Text(stringResource(R.string.accept))
						}
					}
				}
			}
		}
	}

	if (showRecurrentBottomSheet) {
		val sheetState = rememberModalBottomSheetState(
			skipPartiallyExpanded = true
		)

		ModalBottomSheet(
			onDismissRequest = { showRecurrentBottomSheet = false }, sheetState = sheetState
		) {
			RecurrentConfigBottomSheetContent(
				isRecurrent = isRecurrent,
				selectedFrequency = selectedFrequency,
				subscriptionDay = subscriptionDay,
				recurrentEndDate = recurrentEndDate,
				onSaveConfiguration = { newIsRecurrent, newFrequency, newSubscriptionDay, newEndDate ->
					isRecurrent = newIsRecurrent
					selectedFrequency = newFrequency
					subscriptionDay = newSubscriptionDay
					recurrentEndDate = newEndDate
				},
				onDismiss = {
					scope.launch { sheetState.hide() }.invokeOnCompletion {
						showRecurrentBottomSheet = false
					}
				})
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RecurrentConfigBottomSheetContent(
	isRecurrent: Boolean,
	selectedFrequency: RecurrentFrequency,
	subscriptionDay: Int,
	recurrentEndDate: LocalDate,
	onSaveConfiguration: (Boolean, RecurrentFrequency, Int, LocalDate) -> Unit,
	onDismiss: () -> Unit
) {
	var showEndDatePicker by remember { mutableStateOf(false) }
	var localIsRecurrent by remember(isRecurrent) { mutableStateOf(isRecurrent) }
	var localSelectedFrequency by remember(selectedFrequency) { mutableStateOf(selectedFrequency) }
	var localSubscriptionDay by remember(subscriptionDay) { mutableIntStateOf(subscriptionDay) }
	var localRecurrentEndDate by remember(recurrentEndDate) { mutableStateOf(recurrentEndDate) }
	val today = LocalDate.now()
	val maxSelectableDate = today.plusMonths(12)
	val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()) }

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 24.dp, vertical = 16.dp)
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = stringResource(R.string.configure_recurrence),
				style = MaterialTheme.typography.titleMediumEmphasized,
			)

			IconButton(onClick = {
				onSaveConfiguration(
					localIsRecurrent,
					localSelectedFrequency,
					localSubscriptionDay,
					localRecurrentEndDate,
				)
				onDismiss()
			}) {
				Icon(
					imageVector = Icons.Default.SaveAlt,
					contentDescription = stringResource(R.string.save),
					tint = MaterialTheme.colorScheme.onSurface
				)
			}
		}

		Spacer(modifier = Modifier.height(24.dp))

		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = stringResource(R.string.recurrent_expense),
				style = MaterialTheme.typography.bodyLarge
			)
			Switch(
				checked = localIsRecurrent,
				onCheckedChange = { localIsRecurrent = it }
			)
		}

		if (localIsRecurrent) {
			Spacer(modifier = Modifier.height(16.dp))

			Text(
				stringResource(R.string.recurrent_expense_frequency_subtitle),
				style = MaterialTheme.typography.labelMediumCondensed
			)

			val options = listOf(
				stringResource(R.string.recurrent_frequency_weekly),
				stringResource(R.string.recurrent_frequency_biweekly),
				stringResource(R.string.recurrent_frequency_monthly)
			)
			val frequencies = listOf(
				RecurrentFrequency.WEEKLY,
				RecurrentFrequency.BIWEEKLY,
				RecurrentFrequency.MONTHLY,
			)
			val selectedIndex = frequencies.indexOf(localSelectedFrequency).coerceAtLeast(0)

			Row(
				Modifier.padding(horizontal = 8.dp),
				horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
			) {
				val modifiers =
					listOf(Modifier.weight(1f), Modifier.weight(1.5f), Modifier.weight(1f))

				options.forEachIndexed { index, label ->
					ToggleButton(
						checked = selectedIndex == index,
						onCheckedChange = { localSelectedFrequency = frequencies[index] },
						modifier = modifiers[index].semantics { role = Role.RadioButton },
						shapes = when (index) {
							0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
							options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
							else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
						},
					) {
						Text(label)
					}
				}
			}

			Spacer(modifier = Modifier.height(16.dp))

			if (localSelectedFrequency == RecurrentFrequency.MONTHLY) {
				Surface(
					shape = MaterialTheme.shapes.medium,
					color = MaterialTheme.colorScheme.surfaceContainer
				) {
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 8.dp, vertical = 4.dp),
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.SpaceBetween
					) {
						IconButton(onClick = {
							localSubscriptionDay = (localSubscriptionDay - 1).coerceAtLeast(1)
						}) {
							Icon(
								Icons.AutoMirrored.Filled.KeyboardArrowLeft,
								contentDescription = stringResource(R.string.previous_day),
								modifier = Modifier.size(18.dp)
							)
						}

						Text(
							text = stringResource(R.string.monthly_on_day_format, localSubscriptionDay),
							style = MaterialTheme.typography.titleMedium
						)

						IconButton(onClick = {
							localSubscriptionDay = (localSubscriptionDay + 1).coerceAtMost(31)
						}) {
							Icon(
								Icons.AutoMirrored.Filled.KeyboardArrowRight,
								contentDescription = stringResource(R.string.next_day),
								modifier = Modifier.size(18.dp)
							)
						}
					}
				}
				Spacer(modifier = Modifier.height(16.dp))
			}

			Text(
				text = stringResource(R.string.limit_date),
				style = MaterialTheme.typography.labelMediumCondensed,
				modifier = Modifier.padding(bottom = 4.dp)
			)

			OutlinedTextField(
				value = localRecurrentEndDate.format(dateFormatter),
				onValueChange = {},
				readOnly = true,
				placeholder = { Text(stringResource(R.string.date_placeholder)) },
				trailingIcon = {
					Icon(
						imageVector = Icons.Default.CalendarToday,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.outlineVariant,
					)
				},
				shape = RoundedCornerShape(14.dp),
				colors = OutlinedTextFieldDefaults.colors(
					focusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
					unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
					focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
					unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
				),
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
			)

			TextButton(
				onClick = { showEndDatePicker = true },
				modifier = Modifier.align(Alignment.End)
			) {
				Text(stringResource(R.string.change_date))
			}

			OutlinedCard(
				modifier = Modifier.fillMaxWidth(),
				shape = MaterialTheme.shapes.large,
				border = BorderStroke(
					1.dp,
					MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
				),
				colors = CardDefaults.outlinedCardColors(
					containerColor = Color.Transparent
				),
			) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 12.dp, vertical = 14.dp),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(10.dp),
				) {
					Icon(
						imageVector = Icons.Default.Info,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.outline,
					)
					Text(
						text = buildRecurrentSummary(
							frequency = localSelectedFrequency,
							selectedDay = localSubscriptionDay,
							selectedEndDate = localRecurrentEndDate,
							formatter = dateFormatter,
						),
						style = MaterialTheme.typography.bodySmallCondensed,
						color = MaterialTheme.colorScheme.onSurface,
					)
				}
			}
		}

		Spacer(modifier = Modifier.height(32.dp))
	}

	if (showEndDatePicker) {
		val datePickerState = rememberDatePickerState(
			initialSelectedDateMillis = localRecurrentEndDate.atStartOfDay(ZoneId.systemDefault())
				.toInstant().toEpochMilli(),
			selectableDates = object : SelectableDates {
				override fun isSelectableDate(utcTimeMillis: Long): Boolean {
					val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneId.systemDefault())
						.toLocalDate()
					return date.isAfter(today) && !date.isAfter(maxSelectableDate)
				}
			}
		)

		DatePickerDialog(onDismissRequest = { showEndDatePicker = false }, confirmButton = {
			TextButton(
				onClick = {
					datePickerState.selectedDateMillis?.let { millis ->
						localRecurrentEndDate =
							Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
								.toLocalDate()
					}
					showEndDatePicker = false
				}
			) {
				Text(stringResource(R.string.accept))
			}
		}, dismissButton = {
			TextButton(onClick = { showEndDatePicker = false }) {
				Text(stringResource(R.string.cancel))
			}
		}) {
			DatePicker(state = datePickerState)
		}
	}
}

@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun TransactionEditScreenPreview() {
	MinusTheme {
		TransactionEditScreen(
			transaction = Transaction(
				id = 1L,
				amount = BigDecimal("50.00"),
				comment = "ani",
				date = LocalDateTime.now(),
				isDeleted = false
			),
			budgetStartDate = LocalDate.now().minusDays(15),
			budgetEndDate = LocalDate.now().plusDays(15),
			currencyCode = "USD",
			onCancel = {},
			onSave = { _, _, _, _, _, _, _ -> })
	}
}

@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun TransactionEditScreenRecurringPreview() {
	MinusTheme {
		TransactionEditScreen(
			transaction = Transaction(
				id = 1L,
				amount = BigDecimal("99.00"),
				comment = "Netflix",
				date = LocalDateTime.now(),
				isDeleted = false,
				isRecurrent = true,
				recurrentFrequency = RecurrentFrequency.MONTHLY,
				subscriptionDay = 15,
				recurrentEndDate = LocalDateTime.now().plusMonths(6)
			),
			budgetStartDate = LocalDate.now().minusDays(15),
			budgetEndDate = LocalDate.now().plusDays(15),
			currencyCode = "USD",
			onCancel = {},
			onSave = { _, _, _, _, _, _, _ -> })
	}
}

@Composable
private fun buildRecurrentSummary(
	frequency: RecurrentFrequency,
	selectedDay: Int,
	selectedEndDate: LocalDate,
	formatter: DateTimeFormatter
): String {
	val formattedDate = selectedEndDate.format(formatter)
	return when (frequency) {
		RecurrentFrequency.WEEKLY -> stringResource(R.string.summary_weekly_format, formattedDate)
		RecurrentFrequency.BIWEEKLY -> stringResource(
			R.string.summary_biweekly_format, formattedDate
		)

		RecurrentFrequency.MONTHLY -> stringResource(
			R.string.summary_monthly_format, selectedDay, formattedDate
		)
	}
}

private fun evaluateCalculation(input: String): String? {
	if (input.isBlank()) return null

	return try {
		val normalized = input.trim().replace("×", "*").replace("÷", "/")

		normalized.lastOrNull()?.let { if (it in "+-*/") return null }

		val hasOperator = normalized.any { it in "+-*/" }

		if (!hasOperator) {
			val num = normalized.toBigDecimalOrNull() ?: return null
			return if (num.scale() <= 0 || num.stripTrailingZeros().scale() <= 0) {
				num.toBigInteger().toString()
			} else {
				num.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
			}
		}

		val tokenPattern = Regex("([+\\-*/])")
		val parts = tokenPattern.split(normalized).filter { it.isNotEmpty() }
		val operators = tokenPattern.findAll(normalized).map { it.value }.toList()

		if (parts.isEmpty() || parts[0].isEmpty()) return null

		if (operators.size > parts.size - 1) return null

		var result = parts[0].toBigDecimalOrNull() ?: return null

		for (i in operators.indices) {
			if (i + 1 >= parts.size) break
			val operator = operators[i]
			val nextNum = parts[i + 1].toBigDecimalOrNull() ?: return null

			result = when (operator) {
				"+" -> result + nextNum
				"-" -> result - nextNum
				"*" -> result * nextNum
				"/" -> {
					if (nextNum.compareTo(BigDecimal.ZERO) == 0) return null // Division by zero
					result.divide(nextNum, 2, RoundingMode.HALF_UP)
				}

				else -> return null
			}
		}

		if (result.scale() <= 0 || result.stripTrailingZeros().scale() <= 0) {
			result.toBigInteger().toString()
		} else {
			result.setScale(2, RoundingMode.HALF_UP).toPlainString()
		}
	} catch (e: Exception) {
		null
	}
}
