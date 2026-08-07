package com.serranoie.app.minus.domain.model

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
