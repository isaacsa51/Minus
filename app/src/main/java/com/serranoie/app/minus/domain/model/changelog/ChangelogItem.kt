package com.serranoie.app.minus.domain.model.changelog

import kotlinx.serialization.Serializable

@Serializable
data class ChangelogItem(
    val title: String,
    /**
     * Long-form prose for the card body. Defaults to null so items without a
     * PR description (or with empty bodies) render as title-only cards
     * rather than wasting vertical space on an empty Text.
     */
    val description: String? = null,
    val type: ReleaseType,
    val imageName: String? = null,
)