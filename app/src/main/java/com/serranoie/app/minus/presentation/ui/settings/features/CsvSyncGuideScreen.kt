@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.ui.settings.features

import android.net.Uri
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.data.csv.CsvSyncWorker
import com.serranoie.app.minus.presentation.ui.settings.SyncStatus
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.CustomPaddedListItem
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListGroup
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.SettingsLeadingIcon
import com.serranoie.app.minus.presentation.util.Utils.toggleFeedback

@Composable
fun CsvSyncGuideScreen(
    syncFolderName: String? = null,
    syncStatus: SyncStatus? = null,
    onSyncFolderResult: (Uri?) -> Unit = {},
    onBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    val view = LocalView.current
    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> onSyncFolderResult(uri) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { CsvSyncGuideTopBar(onBack, scrollBehavior) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                GuideBody(stringResource(R.string.csv_sync_guide_intro))
            }

            item {
                GuideHeading(stringResource(R.string.csv_sync_guide_setup_title))
                PaddedListGroup(paddingValues = PaddingValues(horizontal = 20.dp)) {
                    CustomPaddedListItem(
                        onClick = {
                            folderLauncher.launch(null)
                            view.toggleFeedback()
                        },
                        position = PaddedListItemPosition.Single,
                        modifier = Modifier.testTag("CsvSyncGuidePickFolder"),
                    ) {
                        SettingsLeadingIcon(icon = Icons.Rounded.FolderOpen)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_sync_folder_title),
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = syncFolderName?.let {
                                    stringResource(R.string.settings_sync_folder_subtitle_set, it)
                                } ?: stringResource(R.string.settings_sync_folder_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                SyncStatusLine(syncStatus)
                GuideBody(stringResource(R.string.csv_sync_guide_setup_body))
            }

            item {
                GuideHeading(stringResource(R.string.csv_sync_guide_adb_title))
                GuideLabel(stringResource(R.string.csv_sync_guide_adb_push_label))
                GuideCode(stringResource(R.string.csv_sync_guide_adb_push))
                GuideLabel(stringResource(R.string.csv_sync_guide_adb_trigger_label))
                GuideCode(
                    stringResource(
                        R.string.csv_sync_guide_adb_trigger,
                        LocalContext.current.packageName
                    )
                )
                GuideLabel(stringResource(R.string.csv_sync_guide_adb_pull_label))
                GuideCode(stringResource(R.string.csv_sync_guide_adb_pull))
                GuideBody(stringResource(R.string.csv_sync_guide_adb_body))
            }

            item {
                GuideSection(
                    title = stringResource(R.string.csv_sync_guide_export_title),
                    body = stringResource(R.string.csv_sync_guide_export_body),
                )
            }

            item {
                GuideHeading(stringResource(R.string.csv_sync_guide_script_title))
                GuideBody(stringResource(R.string.csv_sync_guide_script_body))
                GuideCode(stringResource(R.string.csv_sync_guide_sample))
                GuideBody(stringResource(R.string.csv_sync_guide_columns_body))
            }

            item {
                GuideSection(
                    title = stringResource(R.string.csv_sync_guide_rules_title),
                    body = stringResource(R.string.csv_sync_guide_rules_body),
                )
            }

            item {
                GuideSection(
                    title = stringResource(R.string.csv_sync_guide_pluggy_title),
                    body = stringResource(R.string.csv_sync_guide_pluggy_body),
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SyncStatusLine(status: SyncStatus?) {
    val when_ = status?.atMillis?.takeIf { it > 0L }?.let {
        DateUtils.getRelativeTimeSpanString(
            it,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }.orEmpty()

    val text = when (status?.code) {
        null -> stringResource(R.string.csv_sync_status_never)
        CsvSyncWorker.STATUS_OK -> stringResource(
            R.string.csv_sync_status_ok, when_, status.detail.ifBlank { "0" }
        )

        CsvSyncWorker.STATUS_RESTORED -> stringResource(
            R.string.csv_sync_status_restored, when_
        )

        CsvSyncWorker.STATUS_REJECTED -> stringResource(
            R.string.csv_sync_status_rejected, when_, status.detail
        )

        CsvSyncWorker.STATUS_NO_ACCESS -> stringResource(R.string.csv_sync_status_no_access)
        else -> stringResource(R.string.csv_sync_status_error, when_, status.detail)
    }

    val isProblem = status?.code == CsvSyncWorker.STATUS_NO_ACCESS ||
            status?.code == CsvSyncWorker.STATUS_ERROR

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isProblem) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
    )
}

@Composable
private fun GuideSection(title: String, body: String) {
    GuideHeading(title)
    GuideBody(body)
}

@Composable
private fun GuideHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun GuideLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 6.dp),
    )
}

@Composable
private fun GuideBody(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )
}

@Composable
private fun GuideCode(text: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        SelectionContainer {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
private fun CsvSyncGuideTopBar(
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    LargeTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.csv_sync_guide_title),
                style = MaterialTheme.typography.titleLargeEmphasized,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        scrollBehavior = scrollBehavior,
    )
}

@PreviewLightDark
@Composable
private fun CsvSyncGuideScreenPreview() {
    MinusTheme {
        CsvSyncGuideScreen(onBack = {})
    }
}
