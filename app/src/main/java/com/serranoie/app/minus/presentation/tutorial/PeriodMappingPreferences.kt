package com.serranoie.app.minus.presentation.tutorial

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.serranoie.app.minus.domain.model.PeriodMappingMode
import com.serranoie.app.minus.settingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val PERIOD_MAPPING_MODE_KEY = stringPreferencesKey("period_mapping_mode")

fun Context.periodMappingModeFlow(): Flow<PeriodMappingMode> {
    return settingsDataStore.data.map { prefs ->
        val raw = prefs[PERIOD_MAPPING_MODE_KEY]
        PeriodMappingMode.entries.firstOrNull { it.name == raw } ?: PeriodMappingMode.ACTIVE_BUDGET
    }
}
