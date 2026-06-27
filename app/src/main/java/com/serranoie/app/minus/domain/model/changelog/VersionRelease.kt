package com.serranoie.app.minus.domain.model.changelog

import kotlinx.serialization.Serializable

@Serializable
data class VersionRelease(
    val versionCode: Int,
    val versionName: String,
    val releaseDate: String,
    val items: List<ChangelogItem>,
)
