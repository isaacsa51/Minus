package com.serranoie.app.minus.domain.usecase

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.serranoie.app.minus.data.repository.ChangelogRepository
import com.serranoie.app.minus.domain.model.changelog.ChangelogDecision
import com.serranoie.app.minus.domain.model.changelog.VersionRelease
import com.serranoie.app.minus.presentation.settingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import logcat.logcat
import javax.inject.Inject

/**
 * - `currentVersionCode <= 0` -> [ChangelogDecision.Skip] (defensive)
 * - `latest == null` -> [ChangelogDecision.Skip]
 * - `lastSeen == null` -> treat `lastSeen` as `0L` so the gate bootstraps on
 *   the first ever launch of a changelog-equipped build. This covers upgrades
 *   from pre-gate versions where the DataStore key was never seeded — without
 *   this, the gate can never write `lastSeen` for the first time and stays
 *   silent forever.
 * - `currentVersionCode > lastSeen` -> [ChangelogDecision.Show] of `latest`
 * - else -> [ChangelogDecision.Skip]
 */
fun decideChangelog(
    currentVersionCode: Int,
    lastSeenVersionCode: Long?,
    latestRelease: VersionRelease?,
): ChangelogDecision {
    if (currentVersionCode <= 0) return ChangelogDecision.Skip
    val latest = latestRelease ?: return ChangelogDecision.Skip
    val lastSeen = lastSeenVersionCode ?: 0L

    return when {
        currentVersionCode.toLong() > lastSeen -> ChangelogDecision.Show(latest)
        else -> ChangelogDecision.Skip
    }
}

class ChangelogTriggerEvaluator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val changelogRepository: ChangelogRepository,
) {
    suspend operator fun invoke(currentVersionCode: Int): ChangelogDecision {
        if (currentVersionCode <= 0) {
            logcat("Changelog") { "ChangelogTriggerEvaluator: currentVersionCode=$currentVersionCode, skipping" }
            return ChangelogDecision.Skip
        }

        val latest: VersionRelease = changelogRepository.getLatestRelease()
            ?: return ChangelogDecision.Skip.also {
                logcat("Changelog") { "ChangelogTriggerEvaluator: no latest release available, skipping" }
            }

        val lastSeen = readLastSeen()
        val decision = decideChangelog(currentVersionCode, lastSeen, latest)

        when (decision) {
            is ChangelogDecision.Show -> {
                logcat("Changelog") { "ChangelogTriggerEvaluator: upgrade detected (current=$currentVersionCode > lastSeen=$lastSeen), showing release ${latest.versionName}" }
            }
            ChangelogDecision.Skip -> {
                // lastSeen == null is no longer reachable here — decideChangelog
                // now seeds it as 0L so a first-ever launch of a changelog-
                // equipped build returns Show. Skip can only happen for
                // downgrade or same-version.
                val lastSeenLogged = lastSeen ?: 0L
                val reason = when {
                    currentVersionCode.toLong() < lastSeenLogged -> "current=$currentVersionCode < lastSeen=$lastSeenLogged (downgrade), skipping"
                    else -> "current=$currentVersionCode == lastSeen=$lastSeenLogged (no upgrade), skipping"
                }
                logcat("Changelog") { "ChangelogTriggerEvaluator: $reason" }
            }
        }

        if (decision is ChangelogDecision.Show) {
            writeLastSeen(currentVersionCode)
        }

        return decision
    }

    private suspend fun readLastSeen(): Long? =
        context.settingsDataStore.data.first()[LAST_SEEN_VERSION_CODE]

    private suspend fun writeLastSeen(code: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[LAST_SEEN_VERSION_CODE] = code.toLong()
        }
    }

    suspend fun resetLastSeen() {
        context.settingsDataStore.edit { prefs ->
            prefs.remove(LAST_SEEN_VERSION_CODE)
        }
    }

    companion object {
        val LAST_SEEN_VERSION_CODE = longPreferencesKey("changelog_last_seen_version_code")
    }
}
