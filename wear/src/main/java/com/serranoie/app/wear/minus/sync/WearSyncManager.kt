package com.serranoie.app.wear.minus.sync

import android.content.Context
import logcat.logcat
import logcat.asLog
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import com.serranoie.app.wear.minus.data.PendingExpense
import com.serranoie.app.minus.sync.contract.ExpensePayload
import com.serranoie.app.minus.sync.contract.SnapshotRequestPayload
import com.serranoie.app.minus.sync.contract.WearJson
import com.serranoie.app.minus.sync.contract.WearPaths
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString

class WearSyncManager(private val context: Context) {



    private val nodeClient by lazy { Wearable.getNodeClient(context) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(context) }
    private val messageClient by lazy { Wearable.getMessageClient(context) }

    suspend fun sendExpense(expense: PendingExpense): Boolean {
        val payload = ExpensePayload(
            clientGeneratedId = expense.clientGeneratedId,
            amount = expense.amount,
            comment = expense.comment,
            eventTime = expense.eventTime
        )
        val bytes = WearJson.json.encodeToString(payload).encodeToByteArray()
        val nodes = getPhoneReceiverNodes()
        logcat { "sendExpense: receiverNodes=${nodes.size}, id=${expense.clientGeneratedId}" }
        if (nodes.isEmpty()) {
            logcat { "sendExpense: no receiver nodes for capability minus_phone_receiver" }
            return false
        }

        var sentAny = false
        for (node in nodes) {
            runCatching {
                messageClient.sendMessage(node.id, WearPaths.EXPENSE_ADD, bytes).await()
            }.onSuccess {
                sentAny = true
                logcat { "sendExpense: sent to node=${node.id}" }
            }.onFailure { e ->
                logcat { "sendExpense: failed for node=${node.id}\n${e.asLog()}" }
            }
        }
        return sentAny
    }

    suspend fun requestSnapshot(limit: Int = 20): Boolean {
        val request = SnapshotRequestPayload(limit)
        val bytes = WearJson.json.encodeToString(request).encodeToByteArray()
        val nodes = getPhoneReceiverNodes()
        logcat { "requestSnapshot: receiverNodes=${nodes.size}" }
        if (nodes.isEmpty()) {
            logcat { "requestSnapshot: no receiver nodes for capability minus_phone_receiver" }
            return false
        }

        var sentAny = false
        for (node in nodes) {
            runCatching {
                messageClient.sendMessage(node.id, WearPaths.EXPENSE_SNAPSHOT, bytes).await()
            }.onSuccess {
                sentAny = true
                logcat { "requestSnapshot: sent to node=${node.id}" }
            }.onFailure { e ->
                logcat { "requestSnapshot: failed for node=${node.id}\n${e.asLog()}" }
            }
        }
        return sentAny
    }

    private suspend fun getPhoneReceiverNodes(): List<com.google.android.gms.wearable.Node> {
        val capabilityInfo = capabilityClient
            .getCapability("minus_phone_receiver", CapabilityClient.FILTER_REACHABLE)
            .await()

        val nodes = capabilityInfo.nodes.toList()
        if (nodes.isEmpty()) {
            val fallbackNodes = nodeClient.connectedNodes.await()
            logcat { "getPhoneReceiverNodes: capability empty, fallback connectedNodes=${fallbackNodes.size}" }
            return fallbackNodes
        }
        return nodes
    }
}
