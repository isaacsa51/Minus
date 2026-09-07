package com.serranoie.app.wear.minus.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.serranoie.app.minus.sync.contract.BudgetStatePayload
import com.serranoie.app.minus.sync.contract.WearJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.budgetStateDataStore by preferencesDataStore(name = "wear_budget_state")

class BudgetStateStore(private val context: Context) {

    private val key = stringPreferencesKey("budget_state_json")

    val budgetState: Flow<BudgetStatePayload?> = context.budgetStateDataStore.data.map { prefs ->
        val raw = prefs[key]
        if (raw.isNullOrBlank()) {
            null
        } else {
            runCatching {
                WearJson.json.decodeFromString(BudgetStatePayload.serializer(), raw)
            }.getOrNull()
        }
    }

    suspend fun save(payload: BudgetStatePayload) {
        context.budgetStateDataStore.edit { prefs ->
            prefs[key] = WearJson.json.encodeToString(BudgetStatePayload.serializer(), payload)
        }
    }
}
