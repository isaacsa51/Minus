package com.serranoie.app.minus.presentation.tutorial

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.serranoie.app.minus.settingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val FIRST_LAUNCH_TUTORIAL_STAGE_KEY = stringPreferencesKey("first_launch_tutorial_stage")

enum class FirstLaunchTutorialStage {
    TAP_ANY_NUMBER,
    TAP_DONE_SAVE,
    TAP_BUDGET_PILL,
    TAP_ANALYTICS,
    HISTORY_GESTURES,
    COMPLETED;

    fun next(): FirstLaunchTutorialStage = when (this) {
        TAP_ANY_NUMBER -> TAP_DONE_SAVE
        TAP_DONE_SAVE -> TAP_BUDGET_PILL
        TAP_BUDGET_PILL -> TAP_ANALYTICS
        TAP_ANALYTICS -> HISTORY_GESTURES
        HISTORY_GESTURES -> COMPLETED
        COMPLETED -> COMPLETED
    }

    companion object {
        fun from(value: String?): FirstLaunchTutorialStage {
            return entries.firstOrNull { it.name == value } ?: COMPLETED
        }
    }
}

fun Context.firstLaunchTutorialStageFlow(): Flow<FirstLaunchTutorialStage> {
    return settingsDataStore.data.map { prefs ->
        FirstLaunchTutorialStage.from(prefs[FIRST_LAUNCH_TUTORIAL_STAGE_KEY])
    }
}
