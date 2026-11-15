package com.serranoie.app.minus.domain.model.changelog

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.serranoie.app.minus.generated.GeneratedChangelog
import org.junit.Test
import java.io.File

class ChangelogMediaTest {

    private val release = VersionRelease(
        versionCode = 11001,
        versionName = "1.10.1",
        releaseDate = "2026-10-01",
        items = listOf(
            ChangelogItem("Integrate Essentials island SDK and budget provider service", ReleaseType.FEATURE),
            ChangelogItem("Add Persian Calendar support and RTL view", ReleaseType.FEATURE),
            ChangelogItem("Widgets now render with corner radius no matter OEM", ReleaseType.BUG_FIX),
        ),
    )

    @Test
    fun `file name words attach media to the matching item ignoring case`() {
        val result = release.withMedia(listOf("persian_calendar.webp"))

        assertThat(result.items.map { it.media })
            .containsExactly(null, "changelog/11001/persian_calendar.webp", null)
            .inOrder()
    }

    @Test
    fun `hyphens in the file name count as spaces too`() {
        val result = release.withMedia(listOf("integrate-essentials.gif"))

        assertThat(result.items.map { it.media })
            .containsExactly("changelog/11001/integrate-essentials.gif", null, null)
            .inOrder()
    }

    @Test
    fun `no files leaves every item without media`() {
        assertThat(release.withMedia(emptyList())).isEqualTo(release)
    }

    @Test
    fun `a file attaches to the first matching item only`() {
        val result = release.withMedia(listOf("budget.gif"))

        assertThat(result.items.map { it.media })
            .containsExactly("changelog/11001/budget.gif", null, null)
            .inOrder()
    }

    @Test
    fun `an item takes the first matching file and the extra one is reported`() {
        val files = listOf("render_with.webp", "island_sdk.webp", "corner_radius.webp")
        val result = release.withMedia(files)

        assertThat(result.items.map { it.media })
            .containsExactly("changelog/11001/island_sdk.webp", null, "changelog/11001/render_with.webp")
            .inOrder()
        assertThat(release.unmatchedMedia(files)).containsExactly("corner_radius.webp")
    }

    @Test
    fun `files that match no item are reported`() {
        val unmatched = release.unmatchedMedia(listOf("persian_calender.webp", "island_sdk.webp", ".webp"))

        assertThat(unmatched).containsExactly("persian_calender.webp", ".webp")
    }

    @Test
    fun `media paths live in the release version folder`() {
        assertThat(release.mediaDir()).isEqualTo("changelog/11001")
    }

    @Test
    fun `every shipped media folder belongs to a release and every file matches one of its items`() {
        val root = File("src/main/assets/$CHANGELOG_MEDIA_DIR")
        val entries = root.listFiles().orEmpty().toList()
        assertThat(entries.filter { it.isFile }.map { it.name }).isEmpty()
        val releases = GeneratedChangelog.releases.associateBy { it.versionCode.toString() }

        entries.filter { it.isDirectory }.forEach { dir ->
            val owner = releases[dir.name]
            assertWithMessage("folder ${dir.name} has no matching release").that(owner).isNotNull()
            val files = dir.list().orEmpty().toList()
            assertWithMessage("unmatched media in ${dir.name}").that(owner!!.unmatchedMedia(files)).isEmpty()
        }
    }
}
