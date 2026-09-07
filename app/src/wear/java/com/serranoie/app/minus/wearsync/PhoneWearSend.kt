package com.serranoie.app.minus.wearsync

import android.content.Context
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import logcat.logcat

private const val SEND_TIMEOUT_MS = 10_000L

internal suspend fun sendToWatch(
    context: Context,
    preferredNodeId: String?,
    path: String,
    bytes: ByteArray,
): Boolean {
    val messageClient = Wearable.getMessageClient(context)
    val connected = runCatching {
        Wearable.getNodeClient(context).connectedNodes.await().map { it.id }
    }.getOrElse { emptyList() }

    val targets = when {
        preferredNodeId != null && preferredNodeId in connected -> listOf(preferredNodeId)
        connected.isNotEmpty() -> connected
        preferredNodeId != null -> listOf(preferredNodeId)
        else -> emptyList()
    }

    if (targets.isEmpty()) {
        logcat("PhoneWearSend") { "sendToWatch: no target nodes for path=$path (preferred=$preferredNodeId)" }
        return false
    }

    var anyOk = false
    for (node in targets) {
        val result = withTimeoutOrNull(SEND_TIMEOUT_MS) {
            runCatching { messageClient.sendMessage(node, path, bytes).await() }
        }
        when {
            result == null ->
                logcat("PhoneWearSend") { "sendToWatch: TIMEOUT path=$path node=$node" }
            result.isFailure ->
                logcat("PhoneWearSend") { "sendToWatch: failed path=$path node=$node err=${result.exceptionOrNull()?.message}" }
            else -> {
                anyOk = true
                logcat("PhoneWearSend") { "sendToWatch: ok path=$path node=$node" }
            }
        }
    }
    return anyOk
}
