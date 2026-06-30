package com.serranoie.app.minus.domain.model.changelog

import kotlinx.serialization.Serializable

@Serializable
data class ChangelogItem(
    val title: String,
    val type: ReleaseType,
)