package com.serranoie.app.minus.sync.contract

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object WearPaths {
    const val EXPENSE_ADD = "/expense/add"
    const val EXPENSE_ACK = "/expense/ack"
    const val EXPENSE_SNAPSHOT = "/expense/snapshot"
    const val EXPENSE_SNAPSHOT_RESPONSE = "/expense/snapshot/response"
}

object WearJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}

@Serializable
data class ExpensePayload(
    val clientGeneratedId: String,
    val amount: String,
    val comment: String,
    val eventTime: Long,
    val periodId: Long? = null
)

@Serializable
enum class AckStatus {
    OK,
    ERROR
}

@Serializable
data class AckPayload(
    val clientGeneratedId: String,
    val status: AckStatus,
    val reason: String? = null
)

@Serializable
data class SnapshotRequestPayload(
    val limit: Int = 20
)

@Serializable
data class SnapshotExpenseItem(
    val clientGeneratedId: String,
    val amount: String,
    val comment: String,
    val eventTime: Long
)

@Serializable
data class SnapshotResponsePayload(
    val items: List<SnapshotExpenseItem>
)
