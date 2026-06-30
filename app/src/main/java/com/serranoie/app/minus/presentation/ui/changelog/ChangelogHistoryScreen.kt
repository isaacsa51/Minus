package com.serranoie.app.minus.presentation.ui.changelog

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.changelog.ChangelogItem
import com.serranoie.app.minus.domain.model.changelog.ReleaseType
import com.serranoie.app.minus.domain.model.changelog.VersionRelease
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogHistoryScreen(
    releases: List<VersionRelease>,
    onBack: () -> Unit,
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.changelog_settings_item_title),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.changelog_close),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        ChangelogHistoryContent(
            releases = releases,
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp,
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
        )
    }
}

private val sampleReleases = listOf(
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

@Preview(showBackground = true)
@Composable
private fun ChangelogHistoryScreenPreview() {
    MinusTheme {
        ChangelogHistoryScreen(
            releases = sampleReleases,
            onBack = {},
        )
    }
}
