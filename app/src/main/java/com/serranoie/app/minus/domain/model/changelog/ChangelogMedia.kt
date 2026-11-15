package com.serranoie.app.minus.domain.model.changelog

const val CHANGELOG_MEDIA_DIR = "changelog"

fun VersionRelease.mediaDir(): String = "$CHANGELOG_MEDIA_DIR/$versionCode"

fun VersionRelease.withMedia(fileNames: List<String>): VersionRelease {
    val remaining = fileNames.filter { it.mediaKeyword().isNotBlank() }.toMutableList()
    return copy(
        items = items.map { item ->
            val file = remaining.firstOrNull { item.title.contains(it.mediaKeyword(), ignoreCase = true) }
                ?: return@map item
            remaining.remove(file)
            item.copy(media = "${mediaDir()}/$file")
        },
    )
}

fun VersionRelease.unmatchedMedia(fileNames: List<String>): List<String> {
    val used = withMedia(fileNames).items.mapNotNull { it.media?.substringAfterLast('/') }.toSet()
    return fileNames.filterNot { it in used }
}

private fun String.mediaKeyword(): String = substringBeforeLast('.').replace('_', ' ').replace('-', ' ').trim()
