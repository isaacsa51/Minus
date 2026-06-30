package com.serranoie.app.minus.data.repository

import com.serranoie.app.minus.domain.model.changelog.VersionRelease
import com.serranoie.app.minus.generated.GeneratedChangelog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChangelogRepository @Inject constructor() {

    private val cachedReleases: List<VersionRelease> = GeneratedChangelog.releases

    fun getAllReleases(): List<VersionRelease> = cachedReleases

    fun getLatestRelease(): VersionRelease? = cachedReleases.firstOrNull()

    fun getRelease(versionCode: Int): VersionRelease? =
        cachedReleases.firstOrNull { it.versionCode == versionCode }
}