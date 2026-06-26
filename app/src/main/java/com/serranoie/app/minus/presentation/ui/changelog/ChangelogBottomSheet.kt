package com.serranoie.app.minus.presentation.ui.changelog

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.changelog.VersionRelease

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
        Text(
            text = stringResource(
                R.string.changelog_release_header,
                release.versionName,
                release.releaseDate,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        )
        ChangelogHistoryContent(
            releases = listOf(release),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
