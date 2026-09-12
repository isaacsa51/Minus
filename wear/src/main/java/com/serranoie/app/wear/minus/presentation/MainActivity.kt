package com.serranoie.app.wear.minus.presentation

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.tooling.preview.devices.WearDevices
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import com.serranoie.app.wear.minus.data.BudgetStateStore
import com.serranoie.app.wear.minus.data.CategorySuggestionStore
import com.serranoie.app.wear.minus.data.PendingExpense
import com.serranoie.app.wear.minus.data.PendingExpenseStore
import com.serranoie.app.wear.minus.data.SyncState
import com.serranoie.app.wear.minus.presentation.theme.MinusTheme
import com.serranoie.app.wear.minus.sync.WearInboundMessageHandler
import com.serranoie.app.wear.minus.sync.WearSyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import logcat.logcat
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val inboundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val messageListener = MessageClient.OnMessageReceivedListener { event ->
        logcat { "foreground inbound: path=${event.path}, sourceNode=${event.sourceNodeId}" }
        if (!WearInboundMessageHandler.handles(event.path)) return@OnMessageReceivedListener
        val path = event.path
        val data = event.data
        inboundScope.launch { WearInboundMessageHandler.handle(applicationContext, path, data) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        val store = PendingExpenseStore(applicationContext)
        val categoryStore = CategorySuggestionStore(applicationContext)
        val budgetStateStore = BudgetStateStore(applicationContext)
        WearSyncScheduler.ensurePeriodic(applicationContext)
        WearSyncScheduler.enqueueImmediate(applicationContext)

        setContent {
            WearCalculatorApp(
                store = store,
                categoryStore = categoryStore,
                budgetStateStore = budgetStateStore,
                onEnqueueAndSync = {
                    WearSyncScheduler.enqueueImmediate(applicationContext)
                    vibrateSuccess()
                },
                onRefreshOverview = {
                    WearSyncScheduler.enqueueImmediate(applicationContext)
                },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        Wearable.getMessageClient(this).addListener(messageListener)
    }

    override fun onStop() {
        super.onStop()
        Wearable.getMessageClient(this).removeListener(messageListener)
    }

    override fun onDestroy() {
        inboundScope.cancel()
        super.onDestroy()
    }

    private fun vibrateSuccess() {
        val vibrator = getSystemService(Vibrator::class.java) ?: return
        vibrator.vibrate(VibrationEffect.createOneShot(80L, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

@Composable
private fun WearCalculatorApp(
    store: PendingExpenseStore,
    categoryStore: CategorySuggestionStore,
    budgetStateStore: BudgetStateStore,
    onEnqueueAndSync: () -> Unit,
    onRefreshOverview: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val categories by categoryStore.categories.collectAsState(initial = emptyList())
    val budgetPayload by budgetStateStore.budgetState.collectAsState(initial = null)
    val overviewState = budgetPayload.toOverviewUiState()
    val syncWorkInfos by remember {
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(WearSyncScheduler.MANUAL_SYNC_WORK_NAME)
    }.collectAsState(initial = emptyList())
    val isSyncing = syncWorkInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }

    WearCalculatorContent(
        categories = categories,
        overviewState = overviewState,
        isSyncing = isSyncing,
        onRefreshOverview = onRefreshOverview,
        onRequestSuggestionsRefresh = { onEnqueueAndSync() },
        onConfirmEntry = { amount, comment ->
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
        },
    )
}

private enum class EntryStep { OVERVIEW, AMOUNT, CATEGORY }

@Composable
private fun WearCalculatorContent(
    categories: List<String>,
    overviewState: PeriodOverviewUiState,
    isSyncing: Boolean,
    onRefreshOverview: () -> Unit,
    onRequestSuggestionsRefresh: () -> Unit,
    onConfirmEntry: (amount: String, comment: String) -> Unit
) {
    MinusTheme {
        val amountState = remember { mutableStateOf("") }
        val commentState = remember { mutableStateOf("") }
        val step = remember { mutableStateOf(EntryStep.OVERVIEW) }

        BackHandler(enabled = step.value == EntryStep.AMOUNT) {
            step.value = EntryStep.OVERVIEW
        }
        BackHandler(enabled = step.value == EntryStep.CATEGORY) {
            step.value = EntryStep.AMOUNT
        }

        LaunchedEffect(step.value) {
            if (step.value == EntryStep.OVERVIEW) {
                onRefreshOverview()
            }
        }

        when (step.value) {
            EntryStep.OVERVIEW -> PeriodOverviewScreen(
                state = overviewState,
                onAddExpense = {
                    amountState.value = ""
                    commentState.value = ""
                    step.value = EntryStep.AMOUNT
                },
                onManualSync = onRefreshOverview,
                isSyncing = isSyncing,
            )

            EntryStep.AMOUNT -> NumpadEntryScreen(
                amount = amountState.value,
                currencySymbol = overviewState.currencySymbol,
                symbolAtEnd = overviewState.symbolAtEnd,
                onDigit = { key -> appendDigit(amountState, key) },
                onDot = { appendDot(amountState) },
                onBackspace = { amountState.value = amountState.value.dropLast(1) },
                onClear = { amountState.value = "" },
                onContinue = {
                    if (amountState.value.isBlank()) return@NumpadEntryScreen
                    step.value = EntryStep.CATEGORY
                    if (categories.isEmpty()) {
                        onRequestSuggestionsRefresh()
                    }
                }
            )

            EntryStep.CATEGORY -> CategoryDecEntryScreen(
                amount = amountState.value,
                currencySymbol = overviewState.currencySymbol,
                symbolAtEnd = overviewState.symbolAtEnd,
                categories = categories,
                selectedCategory = commentState.value,
                onCategoryTap = { selected -> commentState.value = selected },
                onCategoryInputChanged = { typed -> commentState.value = typed },
                onSave = {
                    onConfirmEntry(amountState.value, commentState.value.trim())
                    amountState.value = ""
                    commentState.value = ""
                    step.value = EntryStep.OVERVIEW
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
        overviewState = PeriodOverviewUiState(
            loading = false,
            hasBudget = true,
            headlineAmount = "$42.50",
            progress = 0.42f,
            isOverBudget = false,
            budgetTotalText = "$800.00",
            periodStartText = "Sep 1",
            periodEndText = "Sep 30",
            period = BudgetPeriodKind.MONTHLY,
            daysRemaining = 12,
        ),
        isSyncing = false,
        onRefreshOverview = {},
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
