package com.serranoie.app.minus.sync.contract

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object WearPaths {
    const val EXPENSE_ADD = "/expense/add"
    const val EXPENSE_ACK = "/expense/ack"
    const val EXPENSE_SNAPSHOT = "/expense/snapshot"
    const val EXPENSE_SNAPSHOT_RESPONSE = "/expense/snapshot/response"
    const val BUDGET_STATE_REQUEST = "/budget/state/request"
    const val BUDGET_STATE_RESPONSE = "/budget/state/response"
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

@Serializable
data class BudgetStateRequestPayload(
    val since: Long = 0L
)

@Serializable
data class BudgetStatePayload(
    val hasBudget: Boolean,
    val currencyCode: String = "USD",
    val currencySymbol: String = "$",
    val symbolAtEnd: Boolean = false,
    val period: String = "MONTHLY",
    val periodStartEpochDay: Long = 0L,
    val periodEndEpochDay: Long = 0L,
    val daysRemaining: Int = 0,
    val periodTotalDays: Int = 0,
    val totalBudget: String = "0",
    val spentInPeriod: String = "0",
    val remainingInPeriod: String = "0",
    val spentToday: String = "0",
    val remainingToday: String = "0",
    val dailyBudget: String = "0",
    val progress: Float = 0f,
    val isOverBudget: Boolean = false,
    val generatedAtMillis: Long = 0L
)
