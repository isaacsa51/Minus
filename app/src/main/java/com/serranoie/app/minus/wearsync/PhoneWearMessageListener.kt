package com.serranoie.app.minus.wearsync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.sync.contract.AckPayload
import com.serranoie.app.minus.sync.contract.AckStatus
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
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneWearMessageListener @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: BudgetRepository,
    private val ingestor: WearExpenseIngestor
) : MessageClient.OnMessageReceivedListener {

    companion object {
        private const val TAG = "PhoneWearForeground"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        Wearable.getMessageClient(context).addListener(this)
        Log.d(TAG, "foreground listener registered")
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "onMessageReceived: path=${messageEvent.path}, sourceNode=${messageEvent.sourceNodeId}")
        when (messageEvent.path) {
            WearPaths.EXPENSE_ADD -> handleExpenseAdd(messageEvent)
            WearPaths.EXPENSE_SNAPSHOT -> handleSnapshotRequest(messageEvent)
            else -> Unit
        }
    }

    private fun handleExpenseAdd(messageEvent: MessageEvent) {
        val payload = runCatching {
            WearJson.json.decodeFromString(ExpensePayload.serializer(), messageEvent.data.decodeToString())
        }.getOrNull()

        if (payload == null) {
            scope.launch {
                sendAck(messageEvent.sourceNodeId, AckPayload("unknown", AckStatus.ERROR, "Invalid payload"))
            }
            return
        }

        scope.launch {
            when (val result = ingestor.ingest(payload)) {
                is WearExpenseIngestor.IngestResult.Ok -> {
                    sendAck(messageEvent.sourceNodeId, AckPayload(payload.clientGeneratedId, AckStatus.OK))
                    Log.d(TAG, "handleExpenseAdd: ack sent id=${payload.clientGeneratedId}")
                }
                is WearExpenseIngestor.IngestResult.Error -> {
                    sendAck(messageEvent.sourceNodeId, AckPayload(payload.clientGeneratedId, AckStatus.ERROR, result.reason))
                    Log.w(TAG, "handleExpenseAdd: error id=${payload.clientGeneratedId}, reason=${result.reason}")
                }
            }
        }
    }

    private fun handleSnapshotRequest(messageEvent: MessageEvent) {
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

            runCatching {
                Wearable.getMessageClient(context)
                    .sendMessage(messageEvent.sourceNodeId, WearPaths.EXPENSE_SNAPSHOT_RESPONSE, bytes)
                    .await()
            }
        }
    }

    private suspend fun sendAck(nodeId: String, ackPayload: AckPayload) {
        val bytes = WearJson.json.encodeToString(ackPayload).encodeToByteArray()
        runCatching {
            Wearable.getMessageClient(context)
                .sendMessage(nodeId, WearPaths.EXPENSE_ACK, bytes)
                .await()
        }
    }
}
