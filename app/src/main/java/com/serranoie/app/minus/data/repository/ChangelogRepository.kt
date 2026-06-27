package com.serranoie.app.minus.data.repository

import android.content.Context
import com.serranoie.app.minus.domain.model.changelog.VersionRelease
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import logcat.logcat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChangelogRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val cachedReleases: List<VersionRelease> by lazy { loadFromAssets() }

    fun getAllReleases(): List<VersionRelease> = cachedReleases

    fun getLatestRelease(): VersionRelease? = cachedReleases.firstOrNull()

    fun getRelease(versionCode: Int): VersionRelease? =
        cachedReleases.firstOrNull { it.versionCode == versionCode }

    private fun loadFromAssets(): List<VersionRelease> {
        return try {
            val raw = context.assets.open(CHANGELOG_ASSET).bufferedReader().use { it.readText() }
            json.decodeFromString(ListSerializer(VersionRelease.serializer()), raw)
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            logcat { "ChangelogRepository: failed to load $CHANGELOG_ASSET: ${t::class.java.simpleName}: ${t.message}" }
            emptyList()
        }
    }

    companion object {
        const val CHANGELOG_ASSET = "changelog.json"
    }
}
