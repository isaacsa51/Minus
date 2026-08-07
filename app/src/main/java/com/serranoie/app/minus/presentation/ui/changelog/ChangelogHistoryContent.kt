package com.serranoie.app.minus.presentation.ui.changelog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.changelog.ChangelogItem
import com.serranoie.app.minus.domain.model.changelog.ReleaseType
import com.serranoie.app.minus.domain.model.changelog.VersionRelease
import com.serranoie.app.minus.presentation.ui.changelog.components.ChangelogBugFixItemCard
import com.serranoie.app.minus.presentation.ui.changelog.components.ChangelogItemCard
import com.serranoie.app.minus.presentation.ui.changelog.components.ChangelogSectionHeader
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.WavyDivider

// !INFO: Population of real data and how it's being rendered, check: [CHANGELOG_GENERATION.md]

@Composable
internal fun ChangelogHistoryContent(
    releases: List<VersionRelease>,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
    header: (LazyListScope.() -> Unit)? = null,
) {
    if (releases.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.changelog_release_header, "/", "/"),
                style = MaterialTheme.typography.titleMediumEmphasized.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        header?.invoke(this)
        changelogReleaseItems(releases)
    }
}

private fun LazyListScope.changelogReleaseItems(releases: List<VersionRelease>) {
    releases.forEachIndexed { index, release ->
        item(key = "meta-${release.versionCode}") {
            ChangelogReleaseMeta(release = release)
        }

        listOf(
            ReleaseType.FEATURE to "New Features",
            ReleaseType.BUG_FIX to "Bug Fixes",
            ReleaseType.IMPROVEMENT to "Improvements",
        ).forEach { (type, title) ->
            val items = release.items.filter { it.type == type }
            if (items.isEmpty()) return@forEach

            item(key = "header-${release.versionCode}-$type") {
                ChangelogSectionHeader(
                    title = title,
                    type = type,
                )
            }

            items.forEachIndexed { idx, item ->
                item(key = "${release.versionCode}-$type-$idx") {
                    val showDivider = idx < items.lastIndex
                    when (item.type) {
                        ReleaseType.BUG_FIX -> ChangelogBugFixItemCard(
                            item = item,
                            modifier = Modifier.fillMaxWidth(),
                            showDivider = showDivider,
                        )

                        else -> ChangelogItemCard(
                            item = item,
                            modifier = Modifier.fillMaxWidth(),
                            showDivider = showDivider,
                        )
                    }
                }
            }
        }

        if (index < releases.lastIndex) {
            item(key = "older-divider-${release.versionCode}") {
                WavyDivider(text = stringResource(R.string.changelog_previous_versions))
            }
        }
    }
}

@Composable
private fun ChangelogReleaseMeta(release: VersionRelease) {
    Text(
        text = stringResource(
            R.string.changelog_release_header,
            release.versionName,
            release.releaseDate,
        ),
        style = MaterialTheme.typography.titleMediumEmphasized,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun ChangelogHistoryContentPreview() {
    val sampleReleases = listOf(
        VersionRelease(
            versionCode = 100101,
            versionName = "1.1.1",
            releaseDate = "2026-06-17",
            items = listOf(
                ChangelogItem(
                    title = "Launcher icon fix",
                    type = ReleaseType.BUG_FIX,
                ),
            ),
        ),
        VersionRelease(
            versionCode = 100000,
            versionName = "1.0.0",
            releaseDate = "2026-05-18",
            items = listOf(
                ChangelogItem(
                    title = "Calculator-style expense entry",
                    type = ReleaseType.FEATURE,
                ),
                ChangelogItem(
                    title = "Budget tracking",
                    type = ReleaseType.FEATURE,
                ),
            ),
        ),
    )
    MinusTheme {
        ChangelogHistoryContent(
            releases = sampleReleases,
        )
    }
}
