package com.serranoie.app.minus.domain.usecase

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.changelog.ChangelogDecision
import com.serranoie.app.minus.domain.model.changelog.ChangelogItem
import com.serranoie.app.minus.domain.model.changelog.ReleaseType
import com.serranoie.app.minus.domain.model.changelog.VersionRelease
import org.junit.Test

class ChangelogTriggerEvaluatorTest {

    private val latestRelease = VersionRelease(
        versionCode = 200,
        versionName = "2.0.0",
        releaseDate = "2026-06-24",
        items = listOf(
            ChangelogItem(
                title = "Sample",
                description = "Sample item",
                type = ReleaseType.FEATURE,
            ),
        ),
    )

    @Test
    fun `when current version code is zero then decision is Skip`() {
        val decision = decideChangelog(
            currentVersionCode = 0,
            lastSeenVersionCode = null,
            latestRelease = latestRelease,
        )

        assertThat(decision).isEqualTo(ChangelogDecision.Skip)
    }

    @Test
    fun `when current version code is negative then decision is Skip`() {
        val decision = decideChangelog(
            currentVersionCode = -1,
            lastSeenVersionCode = 100L,
            latestRelease = latestRelease,
        )

        assertThat(decision).isEqualTo(ChangelogDecision.Skip)
    }

    @Test
    fun `when latest release is null then decision is Skip`() {
        val decision = decideChangelog(
            currentVersionCode = 200,
            lastSeenVersionCode = 100L,
            latestRelease = null,
        )

        assertThat(decision).isEqualTo(ChangelogDecision.Skip)
    }

    @Test
    fun `when lastSeen is null then decision is Skip`() {
        val decision = decideChangelog(
            currentVersionCode = 200,
            lastSeenVersionCode = null,
            latestRelease = latestRelease,
        )

        assertThat(decision).isEqualTo(ChangelogDecision.Skip)
    }

    @Test
    fun `when current version is greater than lastSeen then decision is Show`() {
        val decision = decideChangelog(
            currentVersionCode = 200,
            lastSeenVersionCode = 150L,
            latestRelease = latestRelease,
        )

        assertThat(decision).isInstanceOf(ChangelogDecision.Show::class.java)
        val show = decision as ChangelogDecision.Show
        assertThat(show.release).isEqualTo(latestRelease)
    }

    @Test
    fun `when current version equals lastSeen then decision is Skip`() {
        val decision = decideChangelog(
            currentVersionCode = 200,
            lastSeenVersionCode = 200L,
            latestRelease = latestRelease,
        )

        assertThat(decision).isEqualTo(ChangelogDecision.Skip)
    }

    @Test
    fun `when current version is less than lastSeen then decision is Skip`() {
        val decision = decideChangelog(
            currentVersionCode = 100,
            lastSeenVersionCode = 200L,
            latestRelease = latestRelease,
        )

        assertThat(decision).isEqualTo(ChangelogDecision.Skip)
    }

    @Test
    fun `when current version is exactly lastSeen plus one then decision is Show`() {
        val decision = decideChangelog(
            currentVersionCode = 101,
            lastSeenVersionCode = 100L,
            latestRelease = latestRelease,
        )

        assertThat(decision).isInstanceOf(ChangelogDecision.Show::class.java)
        val show = decision as ChangelogDecision.Show
        assertThat(show.release).isEqualTo(latestRelease)
    }

    @Test
    fun `when current version is far above lastSeen then decision is Show`() {
        val decision = decideChangelog(
            currentVersionCode = 9_999,
            lastSeenVersionCode = 100L,
            latestRelease = latestRelease,
        )

        assertThat(decision).isInstanceOf(ChangelogDecision.Show::class.java)
    }

    @Test
    fun `when current versionCode is at Int MAX and lastSeen is one less then Show`() {
        // Boundary case: handles Int.MAX_VALUE without Long-overflow surprises
        // since currentVersionCode is widened to Long for the comparison.
        val decision = decideChangelog(
            currentVersionCode = Int.MAX_VALUE,
            lastSeenVersionCode = (Int.MAX_VALUE - 1).toLong(),
            latestRelease = latestRelease,
        )

        assertThat(decision).isInstanceOf(ChangelogDecision.Show::class.java)
    }

    @Test
    fun `when current versionCode equals lastSeen at Int MAX boundary then Skip`() {
        val decision = decideChangelog(
            currentVersionCode = Int.MAX_VALUE,
            lastSeenVersionCode = Int.MAX_VALUE.toLong(),
            latestRelease = latestRelease,
        )

        assertThat(decision).isEqualTo(ChangelogDecision.Skip)
    }

    @Test
    fun `when current versionCode is one and lastSeen is null then Skip (first install)`() {
        // First-install path: the caller is expected to seed lastSeen with
        // the current versionCode after seeing Skip so the sheet doesn't
        // re-fire on every fresh install.
        val decision = decideChangelog(
            currentVersionCode = 1,
            lastSeenVersionCode = null,
            latestRelease = latestRelease,
        )

        assertThat(decision).isEqualTo(ChangelogDecision.Skip)
    }

    @Test
    fun `when Show is returned it carries the same release instance passed in`() {
        // The gate's ViewModel stores this exact reference in its pending
        // StateFlow, so it must be the same object — not a copy.
        val decision = decideChangelog(
            currentVersionCode = 200,
            lastSeenVersionCode = 100L,
            latestRelease = latestRelease,
        )

        val show = decision as ChangelogDecision.Show
        Truth.assertThat(show.release).isSameInstanceAs(latestRelease)
    }
}
