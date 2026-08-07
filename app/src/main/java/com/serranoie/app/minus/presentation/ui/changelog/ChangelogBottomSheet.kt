package com.serranoie.app.minus.presentation.ui.changelog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SecurityUpdateGood
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.changelog.ChangelogItem
import com.serranoie.app.minus.domain.model.changelog.ReleaseType
import com.serranoie.app.minus.domain.model.changelog.VersionRelease
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChangelogBottomSheet(
    release: VersionRelease,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        sheetState.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        ChangelogHistoryContent(
            releases = listOf(release),
            contentPadding = PaddingValues(bottom = 32.dp),
            header = {
                item {
                    InformationContent(versionName = release.versionName)
                }
            }
        )
    }
}

@Composable
private fun InformationContent(versionName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.SecurityUpdateGood,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.changelog_updated_to, versionName),
            style = MaterialTheme.typography.titleLargeEmphasized.copy(
                fontSize = 24.sp,
                lineHeight = 32.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.changelog_see_changes),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview
@Composable
private fun ChangelogBottomSheetPreview() {
    val sampleRelease = VersionRelease(
        versionCode = 100101,
        versionName = "1.1.1",
        releaseDate = "2026-06-17",
        items = listOf(
            ChangelogItem(
                title = "Launcher icon fix",
                type = ReleaseType.BUG_FIX,
            ),
            ChangelogItem(
                title = "Calculator-style expense entry",
                type = ReleaseType.FEATURE,
            ),
            ChangelogItem(
                title = "Launcher icon fix",
                type = ReleaseType.BUG_FIX,
            ),
            ChangelogItem(
                title = "Calculator-style expense entry",
                type = ReleaseType.FEATURE,
            ),
            ChangelogItem(
                title = "Launcher icon fix",
                type = ReleaseType.BUG_FIX,
            ),
            ChangelogItem(
                title = "Launcher icon fix",
                type = ReleaseType.BUG_FIX,
            ),
            ChangelogItem(
                title = "Launcher icon fix",
                type = ReleaseType.BUG_FIX,
            ),
            ChangelogItem(
                title = "Launcher icon fix",
                type = ReleaseType.BUG_FIX,
            ),
            ChangelogItem(
                title = "Launcher icon fix",
                type = ReleaseType.BUG_FIX,
            ),
            ChangelogItem(
                title = "Launcher icon fix",
                type = ReleaseType.BUG_FIX,
            ),
            ChangelogItem(
                title = "Launcher icon fix",
                type = ReleaseType.BUG_FIX,
            ),
            ChangelogItem(
                title = "Launcher icon fix",
                type = ReleaseType.BUG_FIX,
            ),
            ChangelogItem(
                title = "Launcher icon fix",
                type = ReleaseType.BUG_FIX,
            ),
            ChangelogItem(
                title = "Launcher icon fix",
                type = ReleaseType.BUG_FIX,
            ),
        ),
    )
    MinusTheme {
        ChangelogBottomSheet(
            release = sampleRelease,
            onDismiss = {},
        )
    }
}
