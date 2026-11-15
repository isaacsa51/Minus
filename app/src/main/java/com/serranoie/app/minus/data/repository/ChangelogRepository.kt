package com.serranoie.app.minus.data.repository

import android.content.Context
import com.serranoie.app.minus.domain.model.changelog.CHANGELOG_MEDIA_DIR
import com.serranoie.app.minus.domain.model.changelog.VersionRelease
import com.serranoie.app.minus.domain.model.changelog.mediaDir
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
        val folders = context.listAssets(CHANGELOG_MEDIA_DIR).toSet()
        GeneratedChangelog.releases.map { release ->
            if (release.versionCode.toString() in folders) {
                release.withMedia(context.listAssets(release.mediaDir()))
            } else {
                release
            }
        }
    }

    fun getAllReleases(): List<VersionRelease> = cachedReleases

    fun getLatestRelease(): VersionRelease? = cachedReleases.firstOrNull()

    fun getRelease(versionCode: Int): VersionRelease? =
        cachedReleases.firstOrNull { it.versionCode == versionCode }

    private fun Context.listAssets(path: String): List<String> =
        runCatching { assets.list(path) }.getOrNull().orEmpty().toList()
}
