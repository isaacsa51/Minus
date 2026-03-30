package com.serranoie.app.wear.minus.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.serranoie.app.minus.sync.contract.WearJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

private val Context.categorySuggestionDataStore by preferencesDataStore(name = "wear_category_suggestions")

class CategorySuggestionStore(private val context: Context) {
    private val key = stringPreferencesKey("categories_json")

    val categories: Flow<List<String>> = context.categorySuggestionDataStore.data.map { prefs ->
        val raw = prefs[key]
        if (raw.isNullOrBlank()) emptyList()
        else runCatching {
            WearJson.json.decodeFromString(ListSerializer(String.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    suspend fun saveFromComments(comments: List<String>) {
        val normalized = comments
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(12)

        context.categorySuggestionDataStore.edit { prefs ->
            prefs[key] = WearJson.json.encodeToString(ListSerializer(String.serializer()), normalized)
        }
    }

    suspend fun getAllOnce(): List<String> = categories.first()
}
