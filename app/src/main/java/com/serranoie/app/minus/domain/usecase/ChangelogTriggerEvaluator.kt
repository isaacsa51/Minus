package com.serranoie.app.minus.domain.usecase

import com.serranoie.app.minus.data.repository.ChangelogRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.changelog.ChangelogDecision
import com.serranoie.app.minus.domain.model.changelog.VersionRelease
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
    private val changelogRepository: ChangelogRepository,
    private val settingsRepository: SettingsRepository,
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
        settingsRepository.getLastSeenVersionCode()

    private suspend fun writeLastSeen(code: Int) {
        settingsRepository.setLastSeenVersionCode(code.toLong())
    }

    suspend fun resetLastSeen() {
        settingsRepository.resetLastSeenVersionCode()
    }
}
