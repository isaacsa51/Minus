package com.serranoie.app.wear.minus.presentation

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.tooling.preview.devices.WearDevices
import com.serranoie.app.wear.minus.data.CategorySuggestionStore
import com.serranoie.app.wear.minus.data.PendingExpense
import com.serranoie.app.wear.minus.data.PendingExpenseStore
import com.serranoie.app.wear.minus.data.SyncState
import com.serranoie.app.wear.minus.presentation.theme.MinusTheme
import com.serranoie.app.wear.minus.sync.WearSyncScheduler
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		installSplashScreen()
		super.onCreate(savedInstanceState)

		setTheme(android.R.style.Theme_DeviceDefault)

		val store = PendingExpenseStore(applicationContext)
		val categoryStore = CategorySuggestionStore(applicationContext)
		WearSyncScheduler.ensurePeriodic(applicationContext)

		setContent {
			WearCalculatorApp(
				store = store, categoryStore = categoryStore, onEnqueueAndSync = {
					WearSyncScheduler.enqueueImmediate(applicationContext)
					vibrateSuccess()
				})
		}
	}

	private fun vibrateSuccess() {
		val vibrator = getSystemService(Vibrator::class.java) ?: return
		vibrator.vibrate(VibrationEffect.createOneShot(80L, VibrationEffect.DEFAULT_AMPLITUDE))
	}
}

@Composable
private fun WearCalculatorApp(
	store: PendingExpenseStore, categoryStore: CategorySuggestionStore, onEnqueueAndSync: () -> Unit
) {
	val scope = rememberCoroutineScope()
	val categories by categoryStore.categories.collectAsState(initial = emptyList())

	WearCalculatorContent(categories = categories, onRequestSuggestionsRefresh = {
		onEnqueueAndSync()
	}, onConfirmEntry = { amount, comment ->
		val entry = PendingExpense(
			clientGeneratedId = UUID.randomUUID().toString(),
			amount = amount,
			comment = comment,
			eventTime = System.currentTimeMillis(),
			syncState = SyncState.PENDING
		)
		scope.launch {
			store.enqueue(entry)
			val normalizedComment = comment.trim()
			if (normalizedComment.isNotBlank()) {
				val existing = categoryStore.getAllOnce()
				categoryStore.saveFromComments(listOf(normalizedComment) + existing)
			}
			onEnqueueAndSync()
		}
	})
}

private enum class EntryStep { AMOUNT, CATEGORY }

@Composable
private fun WearCalculatorContent(
	categories: List<String>,
	onRequestSuggestionsRefresh: () -> Unit,
	onConfirmEntry: (amount: String, comment: String) -> Unit
) {
	MinusTheme {
		val amountState = remember { mutableStateOf("") }
		val commentState = remember { mutableStateOf("") }
		val step = remember { mutableStateOf(EntryStep.AMOUNT) }

		BackHandler(enabled = step.value == EntryStep.CATEGORY) {
			step.value = EntryStep.AMOUNT
		}

		if (step.value == EntryStep.AMOUNT) {
			NumpadEntryScreen(
				amount = amountState.value,
				onDigit = { key ->
					appendDigit(amountState, key)
				},
				onDot = {
					appendDot(amountState)
				},
				onBackspace = {
					amountState.value = amountState.value.dropLast(1)
				},
				onContinue = {
					if (amountState.value.isBlank()) return@NumpadEntryScreen
					step.value = EntryStep.CATEGORY
					if (categories.isEmpty()) {
						onRequestSuggestionsRefresh()
					}
				}
			)
		} else {
			CategoryDecEntryScreen(
				amount = amountState.value,
				categories = categories,
				selectedCategory = commentState.value,
				onCategoryTap = { selected ->
					commentState.value = selected
				},
				onCategoryInputChanged = { typed ->
					commentState.value = typed
				},
				onSave = {
					onConfirmEntry(amountState.value, commentState.value.trim())
					amountState.value = ""
					commentState.value = ""
					step.value = EntryStep.AMOUNT
				}
			)
		}
	}
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun WearCalculatorContentPreview() {
	WearCalculatorContent(
		categories = listOf("Groceries", "Coffee"),
		onRequestSuggestionsRefresh = {},
		onConfirmEntry = { _, _ -> })
}

private fun appendDigit(amountState: MutableState<String>, digit: String) {
	if (amountState.value.length >= 10) return
	amountState.value += digit
}

private fun appendDot(amountState: MutableState<String>) {
	if (amountState.value.contains(".")) return
	if (amountState.value.isBlank()) {
		amountState.value = "0."
		return
	}
	amountState.value += "."
}

