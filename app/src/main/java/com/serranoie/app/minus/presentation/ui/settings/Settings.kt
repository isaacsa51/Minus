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
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.serranoie.app.minus.presentation.ui.settings.bugreport.buildAppEnvironmentMetadata
import com.serranoie.app.minus.presentation.ui.settings.components.NotificationPermissionItem
import com.serranoie.app.minus.presentation.ui.settings.savings.SavingsPreferencesEditor
import com.serranoie.app.minus.presentation.ui.settings.savings.savingsPreferencesSummary
import com.serranoie.app.minus.presentation.ui.settings.widgets.WidgetGallerySheet
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.component.CustomPaddedExpandableItem
import com.serranoie.app.minus.presentation.ui.theme.component.CustomPaddedListItem
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListGroup
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.SettingsLeadingIcon
import com.serranoie.app.minus.presentation.ui.theme.component.SettingsToggleItem
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
    notificationHour: Int,
    notificationMinute: Int,
    recurrentNotificationHour: Int,
    recurrentNotificationMinute: Int,
    exactAlarmEnabled: Boolean,
    notificationPermissionGranted: Boolean,
    onCensorModeToggle: () -> Unit = {},
    onNavigateToFeatureLab: () -> Unit = {},
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
    currencyConversionContent: @Composable () -> Unit = {},
) {
    var showNotificationTimePicker by remember { mutableStateOf(false) }
    var showRecurrentNotificationTimePicker by remember { mutableStateOf(false) }
    var showWidgetsSheet by remember { mutableStateOf(false) }
    val widgetsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSavingsExpanded by remember { mutableStateOf(false) }
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
            item { currencyConversionContent() }
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
                        SettingsLeadingIcon(icon = Icons.Default.Palette)
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

                    SettingsToggleItem(
                        icon = Icons.Outlined.RemoveRedEye,
                        title = stringResource(R.string.settings_censor_mode_title),
                        description = stringResource(R.string.settings_censor_mode_subtitle),
                        checked = isCensored,
                        onToggle = onCensorModeToggle,
                        position = PaddedListItemPosition.Middle,
                        modifier = Modifier.testTag("SettingsCensorModeItem"),
                        expandDescription = false,
                    )

                    CustomPaddedListItem(
                        onClick = {
                            showWidgetsSheet = true
                            view.weakHapticFeedback()
                        },
                        position = PaddedListItemPosition.Last,
                        modifier = Modifier.testTag("SettingsWidgetsItem")
                    ) {
                        SettingsLeadingIcon(icon = Icons.Default.Widgets)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_widgets_title),
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_widgets_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                PaddedListGroup(
                    title = stringResource(R.string.settings_section_features)
                ) {
                    CustomPaddedListItem(
                        onClick = {
                            onNavigateToFeatureLab()
                            view.weakHapticFeedback()
                        },
                        position = PaddedListItemPosition.First,
                        modifier = Modifier.testTag("SettingsFeatureLabItem")
                    ) {
                        SettingsLeadingIcon(icon = Icons.Rounded.Science)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_feature_lab_title),
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_feature_lab_subtitle),
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
                            view.toggleFeedback()
                            onResetTutorial()
                        },
                        position = PaddedListItemPosition.Last,
                        modifier = Modifier.testTag("SettingsResetTutorialItem")
                    ) {
                        SettingsLeadingIcon(icon = Icons.Rounded.TipsAndUpdates)
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
                            SettingsLeadingIcon(icon = Icons.Rounded.Savings)
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
                        SettingsLeadingIcon(icon = Icons.Rounded.AccessTime)
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
                        SettingsLeadingIcon(icon = Icons.Rounded.Repeat)
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
                        SettingsLeadingIcon(
                            icon = Icons.Rounded.Alarm,
                            tint = if (exactAlarmEnabled) {
                                null
                            } else {
                                MaterialTheme.colorScheme.error
                            },
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
                        SettingsLeadingIcon(icon = Icons.Rounded.Backup)
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
                        SettingsLeadingIcon(icon = Icons.Rounded.Publish)
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
                        SettingsLeadingIcon(icon = Icons.Rounded.AutoAwesome)
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
                            Utils.openWebLink(context, "https://github.com/isaacsa51/Minus/wiki")
                            view.weakHapticFeedback()
                        }, position = PaddedListItemPosition.Middle
                    ) {
                        SettingsLeadingIcon(icon = Icons.Rounded.QuestionMark)
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
                        SettingsLeadingIcon(icon = Icons.Rounded.BugReport)
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
                        SettingsLeadingIcon(icon = Icons.Rounded.Info)
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

        if (showWidgetsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showWidgetsSheet = false },
                sheetState = widgetsSheetState,
            ) {
                WidgetGallerySheet()
            }
        }

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
            notificationHour = 9,
            notificationMinute = 0,
            recurrentNotificationHour = 8,
            recurrentNotificationMinute = 0,
            exactAlarmEnabled = true,
            notificationPermissionGranted = true,
            onNotificationTimeChange = { _, _ -> },
            onRecurrentNotificationTimeChange = { _, _ -> },
            onOpenExactAlarmSettings = {},
            onOpenNotificationSettings = {},
            periodMappingMode = PeriodMappingMode.ACTIVE_BUDGET,
            onPeriodMappingModeChange = {},
            onNavigateToAppearance = {})
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
