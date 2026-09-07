package com.serranoie.app.wear.minus.sync

import android.content.Context
import com.serranoie.app.minus.sync.contract.AckPayload
import com.serranoie.app.minus.sync.contract.AckStatus
import com.serranoie.app.minus.sync.contract.BudgetStatePayload
import com.serranoie.app.minus.sync.contract.SnapshotResponsePayload
import com.serranoie.app.minus.sync.contract.WearJson
import com.serranoie.app.minus.sync.contract.WearPaths
import com.serranoie.app.wear.minus.data.BudgetStateStore
import com.serranoie.app.wear.minus.data.CategorySuggestionStore
import com.serranoie.app.wear.minus.data.PendingExpenseStore
import logcat.logcat

object WearInboundMessageHandler {

	fun handles(path: String): Boolean =
		path == WearPaths.EXPENSE_ACK ||
			path == WearPaths.EXPENSE_SNAPSHOT_RESPONSE ||
			path == WearPaths.BUDGET_STATE_RESPONSE

	suspend fun handle(context: Context, path: String, data: ByteArray) {
		when (path) {
			WearPaths.EXPENSE_ACK -> handleAck(context, data)
			WearPaths.EXPENSE_SNAPSHOT_RESPONSE -> handleSnapshotResponse(context, data)
			WearPaths.BUDGET_STATE_RESPONSE -> handleBudgetState(context, data)
			else -> Unit
		}
	}

	private suspend fun handleAck(context: Context, data: ByteArray) {
		val ack = runCatching {
			WearJson.json.decodeFromString(AckPayload.serializer(), data.decodeToString())
		}.getOrNull() ?: return

		val store = PendingExpenseStore(context)
		if (ack.status == AckStatus.OK) {
			store.remove(ack.clientGeneratedId)
		} else {
			store.markFailedRetryable(ack.clientGeneratedId)
		}
		logcat { "inbound ack: id=${ack.clientGeneratedId}, status=${ack.status}" }
	}

	private suspend fun handleSnapshotResponse(context: Context, data: ByteArray) {
		val payload = runCatching {
			WearJson.json.decodeFromString(SnapshotResponsePayload.serializer(), data.decodeToString())
		}.getOrNull() ?: return

		CategorySuggestionStore(context).saveFromComments(payload.items.map { it.comment })
		logcat { "inbound snapshot: categories=${payload.items.size}" }
	}

	private suspend fun handleBudgetState(context: Context, data: ByteArray) {
		val payload = runCatching {
			WearJson.json.decodeFromString(BudgetStatePayload.serializer(), data.decodeToString())
		}.getOrNull() ?: return

		BudgetStateStore(context).save(payload)
		logcat { "inbound budget state: hasBudget=${payload.hasBudget}" }
	}
}
