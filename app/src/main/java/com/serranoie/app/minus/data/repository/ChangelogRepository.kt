package com.serranoie.app.minus.data.repository

import android.content.Context
import com.serranoie.app.minus.domain.model.changelog.CHANGELOG_MEDIA_DIR
import com.serranoie.app.minus.domain.model.changelog.VersionRelease
import com.serranoie.app.minus.domain.model.changelog.withMedia
import com.serranoie.app.minus.generated.GeneratedChangelog
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChangelogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val cachedReleases: List<VersionRelease> by lazy {
        val media = runCatching { context.assets.list(CHANGELOG_MEDIA_DIR) }.getOrNull().orEmpty().toList()
        GeneratedChangelog.releases.mapIndexed { index, release ->
            if (index == 0) release.withMedia(media) else release
        }
    }

    fun getAllReleases(): List<VersionRelease> = cachedReleases

    fun getLatestRelease(): VersionRelease? = cachedReleases.firstOrNull()

    fun getRelease(versionCode: Int): VersionRelease? =
        cachedReleases.firstOrNull { it.versionCode == versionCode }
}
