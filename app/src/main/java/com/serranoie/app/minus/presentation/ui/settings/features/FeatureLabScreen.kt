@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.ui.settings.features

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.YoutubeSearchedFor
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.settings.SettingsUiState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListGroup
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.SelectableInfoPaddedItem

@Composable
fun FeatureLabScreen(
    state: SettingsUiState,
    onCreditQuickToggle: () -> Unit,
    onShowPastTransactionsToggle: () -> Unit,
    onCategoryPickerDirectPopupToggle: () -> Unit,
    onCategoryGridModeToggle: () -> Unit,
    onExtraNoteToggle: () -> Unit,
    onBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { FeatureLabTopBar(onBack, scrollBehavior) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_feature_lab_header),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }

            item {
                PaddedListGroup {
                    FeatureToggleCard(
                        icon = Icons.Rounded.CreditCard,
                        title = stringResource(R.string.settings_feature_credit_toggle_title),
                        description = stringResource(R.string.settings_feature_credit_toggle_details),
                        switchLabel = stringResource(R.string.settings_feature_credit_toggle_switch_label),
                        checked = state.isCreditQuickToggleEnabled,
                        onToggle = onCreditQuickToggle,
                        position = PaddedListItemPosition.First,
                        testTag = "FeatureLabCreditToggle",
                    )
                    FeatureToggleCard(
                        icon = Icons.Rounded.YoutubeSearchedFor,
                        title = stringResource(R.string.settings_feature_show_past_transactions_title),
                        description = stringResource(R.string.settings_feature_show_past_transactions_subtitle),
                        switchLabel = stringResource(R.string.settings_feature_show_past_transactions_switch_label),
                        checked = state.showPastTransactions,
                        onToggle = onShowPastTransactionsToggle,
                        position = PaddedListItemPosition.Middle,
                        testTag = "FeatureLabShowPastTransactions",
                    )
                    FeatureToggleCard(
                        icon = Icons.Rounded.Sell,
                        title = stringResource(R.string.settings_feature_category_direct_popup_title),
                        description = stringResource(R.string.settings_category_picker_direct_popup_description),
                        switchLabel = stringResource(R.string.settings_category_picker_direct_popup_switch_label),
                        checked = state.isCategoryPickerDirectPopupEnabled,
                        onToggle = onCategoryPickerDirectPopupToggle,
                        position = PaddedListItemPosition.Middle,
                        testTag = "FeatureLabCategoryDirectPopup",
                    )
                    FeatureToggleCard(
                        icon = Icons.AutoMirrored.Filled.ViewList,
                        title = stringResource(R.string.settings_feature_category_grid_title),
                        description = stringResource(R.string.settings_category_grid_mode_description),
                        switchLabel = stringResource(R.string.settings_category_grid_mode_switch_label),
                        checked = state.isCategoryGridModeEnabled,
                        onToggle = onCategoryGridModeToggle,
                        position = PaddedListItemPosition.Middle,
                        testTag = "FeatureLabCategoryGridMode",
                    )
                    FeatureToggleCard(
                        icon = Icons.Rounded.EditNote,
                        title = stringResource(R.string.settings_feature_extra_note_title),
                        description = stringResource(R.string.settings_feature_extra_note_description),
                        switchLabel = stringResource(R.string.settings_feature_extra_note_switch_label),
                        checked = state.isExtraNoteEnabled,
                        onToggle = onExtraNoteToggle,
                        position = PaddedListItemPosition.Last,
                        testTag = "FeatureLabExtraNote",
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun FeatureToggleCard(
    icon: ImageVector,
    title: String,
    description: String,
    switchLabel: String,
    checked: Boolean,
    onToggle: () -> Unit,
    position: PaddedListItemPosition,
    testTag: String,
) {
    SelectableInfoPaddedItem(
        isActive = checked,
        onClick = onToggle,
        position = position,
        modifier = Modifier.testTag(testTag),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = switchLabel,
                style = MaterialTheme.typography.bodyMediumEmphasized,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = checked,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("${testTag}Switch"),
            )
        }
    }
}

@Composable
private fun FeatureLabTopBar(
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    MediumTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.settings_feature_lab_title),
                style = MaterialTheme.typography.titleLargeEmphasized,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
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
private fun FeatureLabScreenPreview() {
    MinusTheme {
        FeatureLabScreen(
            state = SettingsUiState(
                isCreditQuickToggleEnabled = true,
                showPastTransactions = true,
                isCategoryPickerDirectPopupEnabled = false,
                isCategoryGridModeEnabled = false,
                isExtraNoteEnabled = true,
            ),
            onCreditQuickToggle = {},
            onShowPastTransactionsToggle = {},
            onCategoryPickerDirectPopupToggle = {},
            onCategoryGridModeToggle = {},
            onExtraNoteToggle = {},
            onBack = {},
        )
    }
}
