@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.ui.settings.features

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.CustomPaddedListItem
import com.serranoie.app.minus.presentation.ui.theme.component.HelpBanner
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListGroup
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.SettingsToggleItem

@Composable
fun NotificationScanScreen(
    state: NotificationScanUiState,
    onEnabledToggle: () -> Unit,
    onAppToggle: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onOpenAccessSettings: () -> Unit,
    onBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    val visibleApps = remember(state.apps, state.query) {
        if (state.query.isBlank()) {
            state.apps
        } else {
            state.apps.filter { it.label.contains(state.query, ignoreCase = true) }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { NotificationScanTopBar(onBack, scrollBehavior) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                HelpBanner(
                    text = stringResource(R.string.notification_scan_header),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            if (!state.accessGranted) {
                item {
                    PaddedListGroup(
                        title = stringResource(R.string.notification_scan_access_title)
                    ) {
                        CustomPaddedListItem(position = PaddedListItemPosition.Single) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.notification_scan_access_missing),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onOpenAccessSettings,
                                    modifier = Modifier.testTag("NotificationScanAccessButton"),
                                ) {
                                    Text(
                                        text = stringResource(R.string.notification_scan_access_button)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                PaddedListGroup {
                    SettingsToggleItem(
                        icon = Icons.Rounded.NotificationsActive,
                        title = stringResource(R.string.notification_scan_enable_title),
                        description = stringResource(R.string.notification_scan_enable_description),
                        checked = state.enabled,
                        onToggle = onEnabledToggle,
                        position = PaddedListItemPosition.Single,
                        label = stringResource(R.string.settings_feature_experimental_label),
                        modifier = Modifier.testTag("NotificationScanEnable"),
                    )
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = stringResource(R.string.notification_scan_apps_title),
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 10.dp, start = 4.dp),
                    )
                    Text(
                        text = if (state.selectedPackages.isEmpty()) {
                            stringResource(R.string.notification_scan_apps_none_selected)
                        } else {
                            stringResource(
                                R.string.notification_scan_selected_count,
                                state.selectedPackages.size,
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp, start = 4.dp),
                    )
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        label = {
                            Text(text = stringResource(R.string.notification_scan_search_label))
                        },
                        leadingIcon = {
                            Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("NotificationScanSearch"),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (visibleApps.isEmpty() && !state.loadingApps) {
                item {
                    Text(
                        text = stringResource(R.string.notification_scan_apps_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }

            items(visibleApps, key = { it.packageName }) { app ->
                Column(modifier = Modifier.padding(PaddingValues(horizontal = 16.dp, vertical = 3.dp))) {
                    CustomPaddedListItem(
                        onClick = { onAppToggle(app.packageName) },
                        position = PaddedListItemPosition.Single,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Checkbox(
                            checked = app.packageName in state.selectedPackages,
                            onCheckedChange = { onAppToggle(app.packageName) },
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun NotificationScanTopBar(
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    LargeTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.notification_scan_screen_title),
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
private fun NotificationScanScreenPreview() {
    MinusTheme {
        NotificationScanScreen(
            state = NotificationScanUiState(
                accessGranted = true,
                enabled = true,
                selectedPackages = setOf("com.bank.app"),
                apps = listOf(
                    ScannableApp("com.bank.app", "My Bank"),
                    ScannableApp("com.wallet.app", "Wallet"),
                ),
                loadingApps = false,
            ),
            onEnabledToggle = {},
            onAppToggle = {},
            onQueryChange = {},
            onOpenAccessSettings = {},
            onBack = {},
        )
    }
}
