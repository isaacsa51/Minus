@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.ui.settings.features

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.NewLabel
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.YoutubeSearchedFor
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.settings.SettingsUiState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListGroup
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.SettingsToggleItem

@Composable
fun FeatureLabScreen(
    state: SettingsUiState,
    onCreditQuickToggle: () -> Unit,
    onShowPastTransactionsToggle: () -> Unit,
    onCategoryPickerDirectPopupToggle: () -> Unit,
    onCategoryGridModeToggle: () -> Unit,
    onExtraNoteToggle: () -> Unit,
    onReserveUpcomingChargesToggle: () -> Unit,
    onNewCategoryTagToggle: () -> Unit,
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
            item { FeatureLabIntro() }

            item {
                PaddedListGroup(
                    title = stringResource(R.string.settings_feature_group_categories)
                ) {
                    SettingsToggleItem(
                        icon = Icons.Rounded.Sell,
                        title = stringResource(R.string.settings_feature_category_direct_popup_title),
                        description = stringResource(R.string.settings_category_picker_direct_popup_description),
                        checked = state.isCategoryPickerDirectPopupEnabled,
                        onToggle = onCategoryPickerDirectPopupToggle,
                        position = PaddedListItemPosition.First,
                        modifier = Modifier.testTag("FeatureLabCategoryDirectPopup"),
                    )
                    SettingsToggleItem(
                        icon = Icons.AutoMirrored.Filled.ViewList,
                        title = stringResource(R.string.settings_feature_category_grid_title),
                        description = stringResource(R.string.settings_category_grid_mode_description),
                        checked = state.isCategoryGridModeEnabled,
                        onToggle = onCategoryGridModeToggle,
                        position = PaddedListItemPosition.Middle,
                        modifier = Modifier.testTag("FeatureLabCategoryGridMode"),
                        label = stringResource(R.string.settings_feature_experimental),
                    )
                    SettingsToggleItem(
                        icon = Icons.Rounded.NewLabel,
                        title = stringResource(R.string.settings_feature_new_category_tag_title),
                        description = stringResource(R.string.settings_feature_new_category_tag_description),
                        checked = state.isNewCategoryTagEnabled,
                        onToggle = onNewCategoryTagToggle,
                        position = PaddedListItemPosition.Last,
                        modifier = Modifier.testTag("FeatureLabNewCategoryTag"),
                    )
                }
            }

            item {
                PaddedListGroup(
                    title = stringResource(R.string.settings_feature_group_transactions)
                ) {
                    SettingsToggleItem(
                        icon = Icons.Rounded.EditNote,
                        title = stringResource(R.string.settings_feature_extra_note_title),
                        description = stringResource(R.string.settings_feature_extra_note_description),
                        checked = state.isExtraNoteEnabled,
                        onToggle = onExtraNoteToggle,
                        position = PaddedListItemPosition.First,
                        modifier = Modifier.testTag("FeatureLabExtraNote"),
                    )
                    SettingsToggleItem(
                        icon = Icons.Rounded.YoutubeSearchedFor,
                        title = stringResource(R.string.settings_feature_show_past_transactions_title),
                        description = stringResource(R.string.settings_feature_show_past_transactions_subtitle),
                        checked = state.showPastTransactions,
                        onToggle = onShowPastTransactionsToggle,
                        position = PaddedListItemPosition.Last,
                        modifier = Modifier.testTag("FeatureLabShowPastTransactions"),
                    )
                }
            }

            item {
                PaddedListGroup(
                    title = stringResource(R.string.settings_feature_group_budget)
                ) {
                    SettingsToggleItem(
                        icon = Icons.Rounded.CreditCard,
                        title = stringResource(R.string.settings_feature_credit_toggle_title),
                        description = stringResource(R.string.settings_feature_credit_toggle_details),
                        checked = state.isCreditQuickToggleEnabled,
                        onToggle = onCreditQuickToggle,
                        position = PaddedListItemPosition.First,
                        modifier = Modifier.testTag("FeatureLabCreditToggle"),
                    )
                    SettingsToggleItem(
                        icon = Icons.Rounded.EventRepeat,
                        title = stringResource(R.string.settings_feature_reserve_upcoming_charges_title),
                        description = stringResource(R.string.settings_feature_reserve_upcoming_charges_description),
                        checked = state.isReserveUpcomingChargesEnabled,
                        onToggle = onReserveUpcomingChargesToggle,
                        position = PaddedListItemPosition.Last,
                        modifier = Modifier.testTag("FeatureLabReserveUpcomingCharges"),
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun FeatureLabIntro() {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Help,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.settings_what_is_this_for),
                style = MaterialTheme.typography.bodySmallCondensed,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.settings_feature_lab_header),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FeatureLabTopBar(
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    LargeTopAppBar(
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
            onReserveUpcomingChargesToggle = {},
            onNewCategoryTagToggle = {},
            onBack = {},
        )
    }
}
