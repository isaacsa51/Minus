@file:OptIn(ExperimentalMaterial3Api::class)

package com.serranoie.app.minus.presentation.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.YoutubeSearchedFor
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.serranoie.app.minus.BuildConfig
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.PeriodMappingMode
import com.serranoie.app.minus.domain.model.SavingsPreferences
import com.serranoie.app.minus.presentation.ui.history.RecurrentPaymentsViewMode
import com.serranoie.app.minus.presentation.ui.settings.bugreport.buildAppEnvironmentMetadata
import com.serranoie.app.minus.presentation.ui.settings.components.NotificationPermissionItem
import com.serranoie.app.minus.presentation.ui.settings.savings.SavingsPreferencesEditor
import com.serranoie.app.minus.presentation.ui.settings.savings.savingsPreferencesSummary
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.component.CustomPaddedExpandableItem
import com.serranoie.app.minus.presentation.ui.theme.component.CustomPaddedListItem
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedExpandableList
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListGroup
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.SelectableInfoPaddedItem
import com.serranoie.app.minus.presentation.ui.theme.labelLargeCondensed
import com.serranoie.app.minus.presentation.util.Utils
import com.serranoie.app.minus.presentation.util.Utils.toggleFeedback
import com.serranoie.app.minus.presentation.util.Utils.weakHapticFeedback
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Settings(
    modifier: Modifier = Modifier,
    isCensored: Boolean = false,
    isCreditQuickToggleFeatureEnabled: Boolean,
    showPastTransactions: Boolean = true,
    recurrentPaymentsViewMode: RecurrentPaymentsViewMode,
    notificationHour: Int,
    notificationMinute: Int,
    recurrentNotificationHour: Int,
    recurrentNotificationMinute: Int,
    exactAlarmEnabled: Boolean,
    notificationPermissionGranted: Boolean,
    onCensorModeToggle: () -> Unit = {},
    onCreditQuickToggleFeatureToggle: () -> Unit,
    onShowPastTransactionsToggle: () -> Unit = {},
    isCategoryPickerDirectPopupEnabled: Boolean = false,
    isCategoryGridModeEnabled: Boolean = false,
    onCategoryPickerDirectPopupFeatureToggle: () -> Unit = {},
    onCategoryGridModeToggle: () -> Unit = {},
    onRecurrentPaymentsViewModeChange: (RecurrentPaymentsViewMode) -> Unit,
    onNotificationTimeChange: (Int, Int) -> Unit,
    onRecurrentNotificationTimeChange: (Int, Int) -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    periodMappingMode: PeriodMappingMode,
    onPeriodMappingModeChange: (PeriodMappingMode) -> Unit,
    savingsPreferences: SavingsPreferences = SavingsPreferences.DEFAULT,
    onSavingsPreferencesChange: (SavingsPreferences) -> Unit = {},
    onExportCsv: () -> Unit = {},
    onImportCsv: () -> Unit = {},
    onResetTutorial: () -> Unit = {},
    onBugReportClick: () -> Unit = {},
    onNavigateToChangelog: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    var showRecurrentPaymentsViewModeDialog by remember { mutableStateOf(false) }
    var showNotificationTimePicker by remember { mutableStateOf(false) }
    var showRecurrentNotificationTimePicker by remember { mutableStateOf(false) }
    var isCreditFeatureExpanded by remember { mutableStateOf(false) }
    var isCategoryFeatureExpanded by remember { mutableStateOf(false) }
    var isSavingsExpanded by remember { mutableStateOf(false) }
    val dismissRecurrentPaymentsViewModeDialog = { showRecurrentPaymentsViewModeDialog = false }
    val dismissNotificationTimePicker = { showNotificationTimePicker = false }
    val dismissRecurrentNotificationTimePicker = { showRecurrentNotificationTimePicker = false }
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val appVersionName = "v${BuildConfig.VERSION_NAME}"
    val metadataCopiedMessage = stringResource(R.string.settings_version_metadata_copied)

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                }, navigationIcon = {
                    IconButton(
                        onClick = onBack, modifier = Modifier.testTag("SettingsBackButton")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }, colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ), scrollBehavior = scrollBehavior
            )
        }) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("SettingsScreen"),
        ) {
            if (isCensored) {
                item {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(
                            1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = Color.Transparent
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.RemoveRedEye,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.censor_mode_card_label),
                                    style = MaterialTheme.typography.bodySmallCondensed,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                            Text(
                                text = stringResource(R.string.censor_mode_card_body),
                                modifier = Modifier.padding(top = 4.dp),
                                style = MaterialTheme.typography.bodySmallCondensed,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
            item {
                PaddedListGroup(
                    title = stringResource(R.string.settings_section_appearance)
                ) {
                    CustomPaddedListItem(
                        onClick = {
                            onNavigateToAppearance()
                            view.weakHapticFeedback()
                        },
                        position = PaddedListItemPosition.First,
                        modifier = Modifier.testTag("SettingsAppearanceItem")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_appearance_title),
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_appearance_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    CustomPaddedListItem(
                        onClick = onCensorModeToggle,
                        position = PaddedListItemPosition.Last,
                        modifier = Modifier.testTag("SettingsCensorModeItem")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RemoveRedEye,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_censor_mode_title),
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_censor_mode_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        Switch(
                            checked = isCensored, onCheckedChange = {
                                onCensorModeToggle()
                            }, modifier = Modifier.testTag("SettingsCensorModeSwitch")
                        )
                    }
                }
            }

            item {
                PaddedListGroup(
                    title = stringResource(R.string.settings_section_features)
                ) {
                    PaddedExpandableList(
                        isExpanded = isCreditFeatureExpanded,
                        onToggleExpanded = { isCreditFeatureExpanded = !isCreditFeatureExpanded },
                        modifier = Modifier
                            .testTag("SettingsCreditQuickToggleFeatureItem"),
                        headerLabel = stringResource(R.string.settings_feature_credit_toggle_title),
                        containerPosition = PaddedListItemPosition.First,
                        headerVerticalPadding = 20.dp,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.CreditCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        expandedContent = {
                            SelectableInfoPaddedItem(
                                isActive = isCreditQuickToggleFeatureEnabled,
                                onClick = onCreditQuickToggleFeatureToggle,
                                position = PaddedListItemPosition.Middle,
                            ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_feature_credit_toggle_details),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_feature_credit_toggle_switch_label),
                                        style = MaterialTheme.typography.bodyMediumEmphasized,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = isCreditQuickToggleFeatureEnabled,
                                        onCheckedChange = { onCreditQuickToggleFeatureToggle() },
                                        modifier = Modifier.testTag("SettingsCreditQuickToggleFeatureSwitch")
                                    )
                                }
                            }
                        })

                    CustomPaddedListItem(
                        onClick = onShowPastTransactionsToggle,
                        position = PaddedListItemPosition.Middle,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.YoutubeSearchedFor,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_feature_show_past_transactions_title),
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_feature_show_past_transactions_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showPastTransactions,
                            onCheckedChange = { onShowPastTransactionsToggle() },
                        )
                    }

                    PaddedExpandableList(
                        isExpanded = isCategoryFeatureExpanded,
                        onToggleExpanded = {
                            isCategoryFeatureExpanded = !isCategoryFeatureExpanded
                        },
                        headerLabel = stringResource(R.string.settings_category_behavior_title),
                        containerPosition = PaddedListItemPosition.Middle,
                        headerSubtitle = stringResource(R.string.settings_category_behavior_subtitle),
                        headerVerticalPadding = 20.dp,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Sell,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        expandedContent = {
                            SelectableInfoPaddedItem(
                                isActive = isCategoryPickerDirectPopupEnabled,
                                onClick = onCategoryPickerDirectPopupFeatureToggle,
                                position = PaddedListItemPosition.Middle,
                            ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_category_picker_direct_popup_description),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_category_picker_direct_popup_switch_label),
                                        style = MaterialTheme.typography.bodyMediumEmphasized,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = isCategoryPickerDirectPopupEnabled,
                                        onCheckedChange = { onCategoryPickerDirectPopupFeatureToggle() },
                                    )
                                }
                            }
                            SelectableInfoPaddedItem(
                                isActive = isCategoryGridModeEnabled,
                                onClick = onCategoryGridModeToggle,
                                position = PaddedListItemPosition.Middle,
                            ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Text(
                                        text = stringResource(R.string.settings_category_grid_mode_description),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_category_grid_mode_switch_label),
                                        style = MaterialTheme.typography.bodyMediumEmphasized,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = isCategoryGridModeEnabled,
                                        onCheckedChange = { onCategoryGridModeToggle() },
                                    )
                                }
                            }
                        })

                    CustomPaddedListItem(
                        onClick = {
                            showRecurrentPaymentsViewModeDialog = true
                            view.weakHapticFeedback()
                        },
                        position = PaddedListItemPosition.Middle,
                        modifier = Modifier.testTag("SettingsRecurrentPaymentsViewModeItem")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_recurrent_payments_view_mode_title),
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_recurrent_payments_view_mode_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = recurrentPaymentsViewMode.label(),
                            style = MaterialTheme.typography.labelLargeCondensed,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    CustomPaddedListItem(
                        onClick = {
                            view.toggleFeedback()
                            onResetTutorial()
                        },
                        position = PaddedListItemPosition.Last,
                        modifier = Modifier.testTag("SettingsResetTutorialItem")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TipsAndUpdates,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_reset_tutorial_title),
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_reset_tutorial_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                PaddedListGroup(
                    title = stringResource(R.string.settings_section_savings)
                ) {
                    CustomPaddedExpandableItem(
                        isExpanded = isSavingsExpanded,
                        onToggleExpanded = { isSavingsExpanded = !isSavingsExpanded },
                        position = PaddedListItemPosition.Single,
                        modifier = Modifier.testTag("SettingsSavingsPreferencesItem"),
                        defaultContent = {
                            Icon(
                                imageVector = Icons.Rounded.Savings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_savings_preferences_title),
                                    style = MaterialTheme.typography.bodyMediumEmphasized,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = savingsPreferencesSummary(savingsPreferences),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (isSavingsExpanded) {
                                    Icons.Rounded.ExpandLess
                                } else {
                                    Icons.Rounded.ExpandMore
                                },
                                contentDescription = if (isSavingsExpanded) {
                                    "Collapse"
                                } else {
                                    "Expand"
                                },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        expandedContent = {
                            SavingsPreferencesEditor(
                                current = savingsPreferences,
                                onChange = onSavingsPreferencesChange,
                            )
                        },
                    )
                }
            }

            item {
                PaddedListGroup(
                    title = stringResource(R.string.settings_section_notifications)
                ) {
                    NotificationPermissionItem(
                        granted = notificationPermissionGranted,
                        onClick = onOpenNotificationSettings,
                        position = PaddedListItemPosition.First,
                    )

                    CustomPaddedListItem(
                        onClick = {
                            showNotificationTimePicker = true
                            view.weakHapticFeedback()
                        },
                        position = PaddedListItemPosition.Middle,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_period_end_time_title),
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_period_end_time_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = formatNotificationTime(
                                context, notificationHour, notificationMinute
                            ),
                            style = MaterialTheme.typography.labelLargeCondensed,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    CustomPaddedListItem(
                        onClick = {
                            showRecurrentNotificationTimePicker = true
                            view.weakHapticFeedback()
                        },
                        position = PaddedListItemPosition.Middle,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_recurrent_notification_time_title),
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_recurrent_notification_time_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = formatNotificationTime(
                                context, recurrentNotificationHour, recurrentNotificationMinute
                            ),
                            style = MaterialTheme.typography.labelLargeCondensed,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    CustomPaddedListItem(
                        onClick = {
                            onOpenExactAlarmSettings()
                            view.weakHapticFeedback()
                        },
                        position = PaddedListItemPosition.Last,
                        borderStroke = if (!exactAlarmEnabled) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        } else {
                            null
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Alarm,
                            contentDescription = null,
                            tint = if (exactAlarmEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_exact_alarm_title),
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                color = if (exactAlarmEnabled) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                            Text(
                                text = if (exactAlarmEnabled) {
                                    stringResource(R.string.settings_exact_alarm_enabled_subtitle)
                                } else {
                                    stringResource(R.string.settings_exact_alarm_disabled_subtitle)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (exactAlarmEnabled) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                }
            }

            item {
                PaddedListGroup(
                    title = stringResource(R.string.settings_section_data_backup)
                ) {
                    CustomPaddedListItem(
                        onClick = {
                            onExportCsv()
                            view.toggleFeedback()
                        }, position = PaddedListItemPosition.First
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_backup_title),
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_backup_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    CustomPaddedListItem(
                        onClick = {
                            onImportCsv()
                            view.toggleFeedback()
                        }, position = PaddedListItemPosition.Last
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Publish,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_import_csv_title),
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_import_csv_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item(key = "settings_app_info_section") {
                PaddedListGroup(
                    title = stringResource(R.string.settings_section_app_info)
                ) {
                    CustomPaddedListItem(
                        onClick = {
                            onNavigateToChangelog()
                            view.weakHapticFeedback()
                        },
                        position = PaddedListItemPosition.First,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.changelog_settings_item_title),
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(
                                    R.string.changelog_settings_item_subtitle,
                                    BuildConfig.VERSION_NAME,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    CustomPaddedListItem(
                        onClick = {
                            Utils.openWebLink(context, "https://www.github.com/isaacsa51/Minus")
                            view.weakHapticFeedback()
                        }, position = PaddedListItemPosition.Middle
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QuestionMark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_about_title),
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_about_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    CustomPaddedListItem(
                        onClick = {
                            onBugReportClick()
                            view.weakHapticFeedback()
                        }, position = PaddedListItemPosition.Middle
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_bug_report_title),
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_bug_report_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    CustomPaddedListItem(
                        onClick = {
                            view.weakHapticFeedback()
                        },
                        position = PaddedListItemPosition.Last,
                        onLongClick = {
                            context.copyAppEnvironmentMetadataToClipboard()
                            view.toggleFeedback()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = metadataCopiedMessage,
                                    duration = SnackbarDuration.Short,
                                )
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_version_title),
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = appVersionName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (showRecurrentPaymentsViewModeDialog) {
            RecurrentPaymentsViewModePickerDialog(
                currentMode = recurrentPaymentsViewMode,
                onModeSelected = onRecurrentPaymentsViewModeChange,
                onDismiss = dismissRecurrentPaymentsViewModeDialog,
            )
        }

        if (showNotificationTimePicker) {
            NotificationTimePickerDialog(
                initialHour = notificationHour,
                initialMinute = notificationMinute,
                onDismiss = dismissNotificationTimePicker,
                onTimeSelected = { hour, minute ->
                    onNotificationTimeChange(hour, minute)
                    dismissNotificationTimePicker()
                })
        }

        if (showRecurrentNotificationTimePicker) {
            NotificationTimePickerDialog(
                initialHour = recurrentNotificationHour,
                initialMinute = recurrentNotificationMinute,
                onDismiss = dismissRecurrentNotificationTimePicker,
                onTimeSelected = { hour, minute ->
                    onRecurrentNotificationTimeChange(hour, minute)
                    dismissRecurrentNotificationTimePicker()
                })
        }

    }
}

@Composable
fun RecurrentPaymentsViewModePickerDialog(
    currentMode: RecurrentPaymentsViewMode,
    onModeSelected: (RecurrentPaymentsViewMode) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.testTag("RecurrentPaymentsViewModePickerDialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_recurrent_payments_view_mode_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (currentMode == RecurrentPaymentsViewMode.HORIZONTAL_LIST) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                        .clickable(onClick = {
                            onModeSelected(RecurrentPaymentsViewMode.HORIZONTAL_LIST)
                            onDismiss()
                        })
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Repeat,
                        contentDescription = null,
                        tint = if (currentMode == RecurrentPaymentsViewMode.HORIZONTAL_LIST) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_recurrent_payments_view_mode_horizontal_title),
                            style = MaterialTheme.typography.bodyMediumEmphasized,
                            fontWeight = if (currentMode == RecurrentPaymentsViewMode.HORIZONTAL_LIST) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (currentMode == RecurrentPaymentsViewMode.HORIZONTAL_LIST) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = stringResource(R.string.settings_recurrent_payments_view_mode_horizontal_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (currentMode == RecurrentPaymentsViewMode.HORIZONTAL_LIST) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    if (currentMode == RecurrentPaymentsViewMode.HORIZONTAL_LIST) {
                        RadioButton(
                            selected = true, onClick = null, colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (currentMode == RecurrentPaymentsViewMode.VERTICAL_LIST) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                        .clickable(onClick = {
                            onModeSelected(RecurrentPaymentsViewMode.VERTICAL_LIST)
                            onDismiss()
                        })
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Repeat,
                        contentDescription = null,
                        tint = if (currentMode == RecurrentPaymentsViewMode.VERTICAL_LIST) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_recurrent_payments_view_mode_vertical_title),
                            style = MaterialTheme.typography.bodyMediumEmphasized,
                            fontWeight = if (currentMode == RecurrentPaymentsViewMode.VERTICAL_LIST) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (currentMode == RecurrentPaymentsViewMode.VERTICAL_LIST) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = stringResource(R.string.settings_recurrent_payments_view_mode_vertical_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (currentMode == RecurrentPaymentsViewMode.VERTICAL_LIST) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    if (currentMode == RecurrentPaymentsViewMode.VERTICAL_LIST) {
                        RadioButton(
                            selected = true, onClick = null, colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationTimePickerDialog(
    initialHour: Int, initialMinute: Int, onDismiss: () -> Unit, onTimeSelected: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = android.text.format.DateFormat.is24HourFormat(context),
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                TimePicker(state = state)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.settings_time_picker_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        onTimeSelected(state.hour, state.minute)
                        onDismiss()
                    }) {
                        Text(stringResource(R.string.settings_time_picker_confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurrentPaymentsViewMode.label(): String {
    return when (this) {
        RecurrentPaymentsViewMode.HORIZONTAL_LIST -> stringResource(
            R.string.settings_recurrent_payments_view_mode_horizontal_title
        )

        RecurrentPaymentsViewMode.VERTICAL_LIST -> stringResource(
            R.string.settings_recurrent_payments_view_mode_vertical_title
        )
    }
}

private fun Context.copyAppEnvironmentMetadataToClipboard() {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(
        ClipData.newPlainText(
            "Minus app environment metadata",
            buildAppEnvironmentMetadata(),
        )
    )
}

private fun formatNotificationTime(
    context: Context, hour: Int, minute: Int
): String {
    val pattern = if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return LocalTime.of(hour, minute)
        .format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

@Preview
@Composable
private fun PreviewSettings() {
    MinusTheme {
        Settings(
            isCreditQuickToggleFeatureEnabled = false,
            recurrentPaymentsViewMode = RecurrentPaymentsViewMode.HORIZONTAL_LIST,
            notificationHour = 9,
            notificationMinute = 0,
            recurrentNotificationHour = 8,
            recurrentNotificationMinute = 0,
            exactAlarmEnabled = true,
            notificationPermissionGranted = true,
            onCreditQuickToggleFeatureToggle = {},
            onRecurrentPaymentsViewModeChange = {},
            onNotificationTimeChange = { _, _ -> },
            onRecurrentNotificationTimeChange = { _, _ -> },
            onOpenExactAlarmSettings = {},
            onOpenNotificationSettings = {},
            periodMappingMode = PeriodMappingMode.ACTIVE_BUDGET,
            onPeriodMappingModeChange = {},
            onNavigateToAppearance = {})
    }
}
