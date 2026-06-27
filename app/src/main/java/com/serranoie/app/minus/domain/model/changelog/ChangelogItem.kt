package com.serranoie.app.minus.domain.model.changelog

import kotlinx.serialization.Serializable

@Serializable
data class ChangelogItem(
    val title: String,
    val description: String? = null,
    val type: ReleaseType,
    val imageName: String? = null,
)