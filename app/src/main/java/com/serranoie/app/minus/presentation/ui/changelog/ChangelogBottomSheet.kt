package com.serranoie.app.minus.presentation.ui.changelog

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
            contentPadding = PaddingValues(vertical = 8.dp),
        )
    }
}

@Preview(showBackground = true)
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
        ),
    )
    MinusTheme {
        ChangelogBottomSheet(
            release = sampleRelease,
            onDismiss = {},
        )
    }
}
