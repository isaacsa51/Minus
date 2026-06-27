package com.serranoie.app.minus.domain.model.changelog

import kotlinx.serialization.Serializable

enum class ReleaseType {
    @Serializable
    FEATURE,
    @Serializable
    IMPROVEMENT,
    @Serializable
    BUG_FIX,
}
