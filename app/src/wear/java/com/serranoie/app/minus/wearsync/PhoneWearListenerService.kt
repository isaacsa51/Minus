package com.serranoie.app.minus.wearsync

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import logcat.logcat
import com.serranoie.app.minus.sync.contract.AckPayload
import com.serranoie.app.minus.sync.contract.AckStatus
import com.serranoie.app.minus.sync.contract.BudgetStatePayload
import com.serranoie.app.minus.sync.contract.ExpensePayload
import com.serranoie.app.minus.sync.contract.SnapshotExpenseItem
import com.serranoie.app.minus.sync.contract.SnapshotRequestPayload
import com.serranoie.app.minus.sync.contract.SnapshotResponsePayload
import com.serranoie.app.minus.sync.contract.WearJson
import com.serranoie.app.minus.sync.contract.WearPaths
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import java.time.ZoneOffset

class PhoneWearListenerService : WearableListenerService() {

    companion object {
        private const val BUDGET_STATE_TIMEOUT_MS = 5_000L
    }

    override fun onCreate() {
        super.onCreate()
        logcat { "service created" }
    }

    override fun onDestroy() {
        logcat { "service destroyed" }
        super.onDestroy()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        logcat { "onMessageReceived: path=${messageEvent.path}, sourceNode=${messageEvent.sourceNodeId}" }
        when (messageEvent.path) {
            WearPaths.EXPENSE_ADD -> handleExpenseAdd(messageEvent)
            WearPaths.EXPENSE_SNAPSHOT -> handleSnapshotRequest(messageEvent)
            WearPaths.BUDGET_STATE_REQUEST -> handleBudgetStateRequest(messageEvent)
            else -> {
                logcat { "onMessageReceived: unhandled path=${messageEvent.path}" }
                super.onMessageReceived(messageEvent)
            }
        }
    }

    private fun handleExpenseAdd(messageEvent: MessageEvent) {
        val payload = runCatching {
            WearJson.json.decodeFromString<ExpensePayload>(messageEvent.data.decodeToString())
        }.getOrNull()

        if (payload == null) {
            scope.launch {
                sendAck(messageEvent.sourceNodeId, AckPayload("unknown", AckStatus.ERROR, "Invalid payload"))
            }
            return
        }

        scope.launch {
            val ingestor = EntryPointAccessors.fromApplication(
                applicationContext,
                WearSyncEntryPoint::class.java
            ).wearExpenseIngestor()

            when (val result = ingestor.ingest(payload)) {
                is WearExpenseIngestor.IngestResult.Ok -> {
                    sendAck(messageEvent.sourceNodeId, AckPayload(payload.clientGeneratedId, AckStatus.OK))
                    logcat { "handleExpenseAdd: ack sent id=${payload.clientGeneratedId}" }
                }
                is WearExpenseIngestor.IngestResult.Error -> {
                    sendAck(messageEvent.sourceNodeId, AckPayload(payload.clientGeneratedId, AckStatus.ERROR, result.reason))
                    logcat { "handleExpenseAdd: error id=${payload.clientGeneratedId}, reason=${result.reason}" }
                }
            }
        }
    }

    private fun handleBudgetStateRequest(messageEvent: MessageEvent) {
        val node = messageEvent.sourceNodeId
        scope.launch {
            logcat { "handleBudgetStateRequest: building state for node=$node" }
            val provider = EntryPointAccessors.fromApplication(
                applicationContext,
                WearSyncEntryPoint::class.java
            ).wearBudgetStateProvider()

            val built = runCatching {
                withTimeoutOrNull(BUDGET_STATE_TIMEOUT_MS) { provider.currentState() }
            }.onFailure {
                logcat { "handleBudgetStateRequest: provider threw: ${it.message}" }
            }.getOrNull()
            if (built == null) {
                logcat { "handleBudgetStateRequest: state build failed/timed out, sending empty" }
            }
            val payload = built ?: BudgetStatePayload(hasBudget = false)

            val bytes = WearJson.json.encodeToString(payload).encodeToByteArray()
            val ok = sendToWatch(applicationContext, node, WearPaths.BUDGET_STATE_RESPONSE, bytes)
            logcat { "handleBudgetStateRequest: response sent (hasBudget=${payload.hasBudget}, ok=$ok)" }
        }
    }

    private fun handleSnapshotRequest(messageEvent: MessageEvent) {
        val request = runCatching {
            WearJson.json.decodeFromString<SnapshotRequestPayload>(messageEvent.data.decodeToString())
        }.getOrDefault(SnapshotRequestPayload())

        scope.launch {
            val repository = EntryPointAccessors.fromApplication(
                applicationContext,
                WearSyncEntryPoint::class.java
            ).budgetRepository()

            val items = repository.getRecentTransactions(request.limit.coerceIn(1, 50)).map { tx ->
                SnapshotExpenseItem(
                    clientGeneratedId = tx.clientGeneratedId ?: tx.id.toString(),
                    amount = tx.amount.toPlainString(),
                    comment = tx.comment,
                    eventTime = tx.date?.toInstant(ZoneOffset.UTC)?.toEpochMilli() ?: tx.createdAt
                )
            }

            val response = SnapshotResponsePayload(items)
            val bytes = WearJson.json
                .encodeToString(response)
                .encodeToByteArray()

            sendToWatch(applicationContext, messageEvent.sourceNodeId, WearPaths.EXPENSE_SNAPSHOT_RESPONSE, bytes)
        }
    }

    private suspend fun sendAck(nodeId: String, ackPayload: AckPayload) {
        val bytes = WearJson.json.encodeToString(ackPayload).encodeToByteArray()
        sendToWatch(applicationContext, nodeId, WearPaths.EXPENSE_ACK, bytes)
    }
}
