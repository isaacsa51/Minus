package com.serranoie.app.minus.wearsync

import android.content.Context
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.sync.contract.AckPayload
import com.serranoie.app.minus.sync.contract.AckStatus
import com.serranoie.app.minus.sync.contract.BudgetStatePayload
import com.serranoie.app.minus.sync.contract.ExpensePayload
import com.serranoie.app.minus.sync.contract.SnapshotExpenseItem
import com.serranoie.app.minus.sync.contract.SnapshotRequestPayload
import com.serranoie.app.minus.sync.contract.SnapshotResponsePayload
import com.serranoie.app.minus.sync.contract.WearJson
import com.serranoie.app.minus.sync.contract.WearPaths
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import logcat.logcat
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneWearMessageListener @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: BudgetRepository,
    private val ingestor: WearExpenseIngestor,
    private val budgetStateProvider: WearBudgetStateProvider,
) : MessageClient.OnMessageReceivedListener {

    companion object {
        private const val BUDGET_STATE_TIMEOUT_MS = 5_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        Wearable.getMessageClient(context).addListener(this)
        logcat { "foreground listener registered" }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        logcat { "onMessageReceived: path=${messageEvent.path}, sourceNode=${messageEvent.sourceNodeId}" }
        when (messageEvent.path) {
            WearPaths.EXPENSE_ADD -> handleExpenseAdd(messageEvent)
            WearPaths.EXPENSE_SNAPSHOT -> handleSnapshotRequest(messageEvent)
            WearPaths.BUDGET_STATE_REQUEST -> handleBudgetStateRequest(messageEvent)
            else -> Unit
        }
    }

    private fun handleExpenseAdd(messageEvent: MessageEvent) {
        val node = messageEvent.sourceNodeId
        val payload = runCatching {
            WearJson.json.decodeFromString(ExpensePayload.serializer(), messageEvent.data.decodeToString())
        }.getOrNull()

        if (payload == null) {
            scope.launch {
                sendAck(node, AckPayload("unknown", AckStatus.ERROR, "Invalid payload"))
            }
            return
        }

        scope.launch {
            when (val result = ingestor.ingest(payload)) {
                is WearExpenseIngestor.IngestResult.Ok -> {
                    sendAck(node, AckPayload(payload.clientGeneratedId, AckStatus.OK))
                    logcat { "handleExpenseAdd: ack sent id=${payload.clientGeneratedId}" }
                }
                is WearExpenseIngestor.IngestResult.Error -> {
                    sendAck(node, AckPayload(payload.clientGeneratedId, AckStatus.ERROR, result.reason))
                    logcat { "handleExpenseAdd: error id=${payload.clientGeneratedId}, reason=${result.reason}" }
                }
            }
        }
    }

    private fun handleBudgetStateRequest(messageEvent: MessageEvent) {
        val node = messageEvent.sourceNodeId
        scope.launch {
            logcat { "handleBudgetStateRequest: building state for node=$node" }
            val built = runCatching {
                withTimeoutOrNull(BUDGET_STATE_TIMEOUT_MS) { budgetStateProvider.currentState() }
            }.onFailure {
                logcat { "handleBudgetStateRequest: provider threw: ${it.message}" }
            }.getOrNull()
            if (built == null) {
                logcat { "handleBudgetStateRequest: state build failed/timed out, sending empty" }
            }
            val payload = built ?: BudgetStatePayload(hasBudget = false)

            val bytes = WearJson.json.encodeToString(payload).encodeToByteArray()
            val ok = sendToWatch(context, node, WearPaths.BUDGET_STATE_RESPONSE, bytes)
            logcat { "handleBudgetStateRequest: response sent (hasBudget=${payload.hasBudget}, ok=$ok)" }
        }
    }

    private fun handleSnapshotRequest(messageEvent: MessageEvent) {
        val node = messageEvent.sourceNodeId
        val request = runCatching {
            WearJson.json.decodeFromString(SnapshotRequestPayload.serializer(), messageEvent.data.decodeToString())
        }.getOrDefault(SnapshotRequestPayload())

        scope.launch {
            val items = repository.getRecentTransactions(request.limit.coerceIn(1, 50)).map { tx ->
                SnapshotExpenseItem(
                    clientGeneratedId = tx.clientGeneratedId ?: tx.id.toString(),
                    amount = tx.amount.toPlainString(),
                    comment = tx.comment,
                    eventTime = tx.date?.toInstant(ZoneOffset.UTC)?.toEpochMilli() ?: tx.createdAt
                )
            }

            val response = SnapshotResponsePayload(items)
            val bytes = WearJson.json.encodeToString(response).encodeToByteArray()
            sendToWatch(context, node, WearPaths.EXPENSE_SNAPSHOT_RESPONSE, bytes)
        }
    }

    private suspend fun sendAck(nodeId: String, ackPayload: AckPayload) {
        val bytes = WearJson.json.encodeToString(ackPayload).encodeToByteArray()
        sendToWatch(context, nodeId, WearPaths.EXPENSE_ACK, bytes)
    }
}
