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
 * - `lastSeen == null` -> [ChangelogDecision.Skip]
 *   (first install of the changelog-equipped build — the user lands in
 *   onboarding / main directly; the changelog only auto-shows on a real
 *   version upgrade, not as a debut reveal)
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
    val lastSeen = lastSeenVersionCode ?: return ChangelogDecision.Skip

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
                val reason = when {
                    lastSeen == null -> "first install or no prior recorded version, skipping"
                    currentVersionCode.toLong() < lastSeen -> "current=$currentVersionCode < lastSeen=$lastSeen (downgrade), skipping"
                    currentVersionCode.toLong() == lastSeen -> "current=$currentVersionCode == lastSeen=$lastSeen, skipping"
                    else -> "no upgrade detected (current=$currentVersionCode, lastSeen=$lastSeen), skipping"
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
