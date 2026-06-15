@file:OptIn(ExperimentalMaterial3Api::class)

package com.serranoie.app.minus.presentation.ui.settings

import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.serranoie.app.minus.BuildConfig
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.PeriodMappingMode
import com.serranoie.app.minus.presentation.ui.history.RecurrentPaymentsViewMode
import com.serranoie.app.minus.presentation.ui.settings.bugreport.buildAppEnvironmentMetadata
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.CustomPaddedExpandableItem
import com.serranoie.app.minus.presentation.ui.theme.component.CustomPaddedListItem
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListGroup
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.labelLargeCondensed
import com.serranoie.app.minus.presentation.util.Utils
import com.serranoie.app.minus.presentation.util.Utils.toggleFeedback
import com.serranoie.app.minus.presentation.util.Utils.weakHapticFeedback
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Settings(
	modifier: Modifier = Modifier,
	currentTheme: String,
	currentTypography: String,
	isMaterialYouEnabled: Boolean,
	isCreditQuickToggleFeatureEnabled: Boolean,
	recurrentPaymentsViewMode: RecurrentPaymentsViewMode,
	notificationHour: Int,
	notificationMinute: Int,
	exactAlarmEnabled: Boolean,
	onThemeChange: (String) -> Unit,
	onTypographyChange: (String) -> Unit,
	onMaterialYouToggle: () -> Unit,
	onCreditQuickToggleFeatureToggle: () -> Unit,
	onRecurrentPaymentsViewModeChange: (RecurrentPaymentsViewMode) -> Unit,
	onNotificationTimeChange: (Int, Int) -> Unit,
	onOpenExactAlarmSettings: () -> Unit,
	periodMappingMode: PeriodMappingMode,
	onPeriodMappingModeChange: (PeriodMappingMode) -> Unit,
	onExportCsv: () -> Unit = {},
	onImportCsv: () -> Unit = {},
	onResetTutorial: () -> Unit = {},
	onBugReportClick: () -> Unit = {},
	onBack: () -> Unit = {},
) {
	var showThemeDialog by remember { mutableStateOf(false) }
	var showTypographyDialog by remember { mutableStateOf(false) }
	var showRecurrentPaymentsViewModeDialog by remember { mutableStateOf(false) }
	var showNotificationTimePicker by remember { mutableStateOf(false) }
	var isCreditFeatureExpanded by remember { mutableStateOf(false) }
	val dismissThemeDialog = { showThemeDialog = false }
	val dismissTypographyDialog = { showTypographyDialog = false }
	val dismissRecurrentPaymentsViewModeDialog = { showRecurrentPaymentsViewModeDialog = false }
	val dismissNotificationTimePicker = { showNotificationTimePicker = false }
	val scrollBehavior =
		TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
	val context = LocalContext.current
	val view = LocalView.current
	val snackbarHostState = remember { SnackbarHostState() }
	val coroutineScope = rememberCoroutineScope()
	val appVersionName = "v${BuildConfig.VERSION_NAME}"
	val metadataCopiedMessage = stringResource(R.string.settings_version_metadata_copied)

	Scaffold(
		modifier = modifier
			.nestedScroll(scrollBehavior.nestedScrollConnection),
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
			item {
				PaddedListGroup(
					title = stringResource(R.string.settings_section_appearance)
				) {
					CustomPaddedListItem(
						onClick = {
							showThemeDialog = true
							view.weakHapticFeedback()
						},
						position = PaddedListItemPosition.First,
						modifier = Modifier.testTag("SettingsThemeItem")
					) {
						Icon(
							imageVector = Icons.Default.Brightness4,
							contentDescription = null,
							tint = MaterialTheme.colorScheme.primary
						)
						Spacer(modifier = Modifier.width(16.dp))
						Column(modifier = Modifier.weight(1f)) {
							Spacer(modifier = Modifier.width(16.dp))

							Text(
								text = stringResource(R.string.settings_theme_title),
								style = MaterialTheme.typography.bodyMediumEmphasized,
								color = MaterialTheme.colorScheme.onSurface
							)
							Text(
								text = stringResource(R.string.settings_theme_subtitle),
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant
							)
						}
						Text(
							text = currentTheme,
							style = MaterialTheme.typography.labelLargeCondensed,
							color = MaterialTheme.colorScheme.primary
						)
					}

					CustomPaddedListItem(
						onClick = {
							showTypographyDialog = true
							view.weakHapticFeedback()
						},
						position = PaddedListItemPosition.Middle,
						modifier = Modifier.testTag("SettingsTypoItem")
					) {
						Icon(
							imageVector = Icons.Default.TextFields,
							contentDescription = null,
							tint = MaterialTheme.colorScheme.primary
						)
						Spacer(modifier = Modifier.width(16.dp))

						Column(modifier = Modifier.weight(1f)) {
							Spacer(modifier = Modifier.width(16.dp))

							Text(
								text = stringResource(R.string.settings_typography_title),
								style = MaterialTheme.typography.bodyMediumEmphasized,
								color = MaterialTheme.colorScheme.onSurface
							)
							Text(
								text = stringResource(R.string.settings_typography_subtitle),
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant
							)
						}
						Text(
							text = currentTypography,
							style = MaterialTheme.typography.labelLargeCondensed,
							color = MaterialTheme.colorScheme.primary
						)
					}

					CustomPaddedListItem(
						onClick = onMaterialYouToggle,
						position = PaddedListItemPosition.Last,
						modifier = Modifier.testTag("SettingsMaterialYouItem")
					) {
						Icon(
							imageVector = Icons.Default.Palette,
							contentDescription = null,
							tint = MaterialTheme.colorScheme.primary
						)
						Spacer(modifier = Modifier.width(16.dp))
						Column(modifier = Modifier.weight(1f)) {
							Text(
								text = stringResource(R.string.settings_material_you_title),
								style = MaterialTheme.typography.bodyMediumEmphasized,
								color = MaterialTheme.colorScheme.onSurface
							)
							Text(
								text = stringResource(R.string.settings_material_you_subtitle),
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
								modifier = Modifier.padding(end = 4.dp)
							)
						}
						Switch(
							checked = isMaterialYouEnabled, onCheckedChange = {
								onMaterialYouToggle()
//								view.toggleFeedback()
							}, modifier = Modifier.testTag("SettingsMaterialYouSwitch")
						)
					}
				}
			}

			item {
				PaddedListGroup(
					title = stringResource(R.string.settings_section_features)
				) {
					CustomPaddedExpandableItem(
						isExpanded = isCreditFeatureExpanded,
						onToggleExpanded = { isCreditFeatureExpanded = !isCreditFeatureExpanded },
						position = PaddedListItemPosition.First,
						modifier = Modifier.testTag("SettingsCreditQuickToggleFeatureItem"),
						defaultContent = {
							Icon(
								imageVector = Icons.Default.CreditCard,
								contentDescription = null,
								tint = MaterialTheme.colorScheme.primary
							)
							Spacer(modifier = Modifier.width(16.dp))
							Column(modifier = Modifier.weight(1f)) {
								Text(
									text = stringResource(R.string.settings_feature_credit_toggle_title),
									style = MaterialTheme.typography.bodyMediumEmphasized,
									color = MaterialTheme.colorScheme.onSurface
								)
								Text(
									text = stringResource(R.string.settings_feature_credit_toggle_subtitle),
									style = MaterialTheme.typography.bodySmall,
									color = MaterialTheme.colorScheme.onSurfaceVariant
								)
							}
							Icon(
								imageVector = if (isCreditFeatureExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
								contentDescription = null,
								tint = MaterialTheme.colorScheme.onSurfaceVariant
							)
						},
						expandedContent = {
							Text(
								text = stringResource(R.string.settings_feature_credit_toggle_details),
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
								modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
							)
							Row(
								modifier = Modifier
									.fillMaxWidth()
									.padding(top = 8.dp, start = 6.dp, end = 6.dp),
								verticalAlignment = Alignment.CenterVertically
							) {
								Text(
									text = stringResource(R.string.settings_feature_credit_toggle_switch_label),
									style = MaterialTheme.typography.bodyMedium,
									color = MaterialTheme.colorScheme.onSurface,
									modifier = Modifier.weight(1f)
								)
								Switch(
									checked = isCreditQuickToggleFeatureEnabled,
									onCheckedChange = { onCreditQuickToggleFeatureToggle() },
									modifier = Modifier.testTag("SettingsCreditQuickToggleFeatureSwitch")
								)
							}
						}
					)

					CustomPaddedListItem(
						onClick = {
							showRecurrentPaymentsViewModeDialog = true
							view.weakHapticFeedback()
						},
						position = PaddedListItemPosition.Last,
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
				}
			}

			item {
				PaddedListGroup(
					title = stringResource(R.string.settings_section_notifications)
				) {
					CustomPaddedListItem(
						onClick = {
							showNotificationTimePicker = true
							view.weakHapticFeedback()
						},
						position = PaddedListItemPosition.First,
					) {
						Icon(
							imageVector = Icons.Default.AccessTime,
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
							onOpenExactAlarmSettings()
							view.weakHapticFeedback()
						},
						position = PaddedListItemPosition.Last,
					) {
						Icon(
							imageVector = Icons.Default.Alarm,
							contentDescription = null,
							tint = MaterialTheme.colorScheme.primary
						)
						Spacer(modifier = Modifier.width(16.dp))
						Column(modifier = Modifier.weight(1f)) {
							Text(
								text = stringResource(R.string.settings_exact_alarm_title),
								style = MaterialTheme.typography.bodyMediumEmphasized,
								color = MaterialTheme.colorScheme.onSurface
							)
							Text(
								text = if (exactAlarmEnabled) {
									stringResource(R.string.settings_exact_alarm_enabled_subtitle)
								} else {
									stringResource(R.string.settings_exact_alarm_disabled_subtitle)
								},
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant
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
							imageVector = Icons.Default.Backup,
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
							imageVector = Icons.Default.Publish,
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

			item {
				PaddedListGroup(
					title = stringResource(R.string.settings_section_app_info)
				) {
					CustomPaddedListItem(
						onClick = {
							Utils.openWebLink(context, "https://www.github.com/isaacsa51/Minus")
							view.weakHapticFeedback()
						}, position = PaddedListItemPosition.First
					) {
						Icon(
							imageVector = Icons.Default.QuestionMark,
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
							imageVector = Icons.Default.BugReport,
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
							imageVector = Icons.Default.Info,
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

//
//			item {
//				PaddedListGroup(
//					title = stringResource(R.string.settings_section_tutorial)
//				) {
//					CustomPaddedListItem(
//						onClick = {
//							onResetTutorial()
//							view.toggleFeedback()
//						},
//						position = PaddedListItemPosition.Single,
//						modifier = Modifier.testTag("SettingsResetTutorialItem")
//					) {
//						Icon(
//							imageVector = Icons.Default.Refresh,
//							contentDescription = null,
//							tint = MaterialTheme.colorScheme.primary
//						)
//						Spacer(modifier = Modifier.width(16.dp))
//						Column(modifier = Modifier.weight(1f)) {
//							Text(
//								text = stringResource(R.string.settings_reset_tutorial_title),
//								style = MaterialTheme.typography.bodyMediumEmphasized,
//								color = MaterialTheme.colorScheme.onSurface
//							)
//							Text(
//								text = stringResource(R.string.settings_reset_tutorial_subtitle),
//								style = MaterialTheme.typography.bodySmall,
//								color = MaterialTheme.colorScheme.onSurfaceVariant
//							)
//						}
//					}
//				}
//			}
		}

		if (showThemeDialog) {
			ThemePickerDialog(
				currentTheme = currentTheme,
				onThemeSelected = onThemeChange,
				onDismiss = dismissThemeDialog
			)
		}

		if (showTypographyDialog) {
			TypographyPickerDialog(
				currentTypography = currentTypography,
				onTypographySelected = onTypographyChange,
				onDismiss = dismissTypographyDialog,
			)
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
	}
}

@Composable
fun ThemePickerDialog(
	currentTheme: String, onThemeSelected: (String) -> Unit, onDismiss: () -> Unit
) {
	Dialog(onDismissRequest = onDismiss) {
		Surface(
			shape = RoundedCornerShape(28.dp),
			color = MaterialTheme.colorScheme.surfaceContainerHigh,
			tonalElevation = 6.dp,
			modifier = Modifier.testTag("ThemePickerDialog")
		) {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(24.dp)
			) {
				Text(
					text = stringResource(R.string.settings_theme_dialog_title),
					style = MaterialTheme.typography.headlineSmall,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurface,
					modifier = Modifier.padding(bottom = 16.dp)
				)

				ThemeOption(
					title = stringResource(R.string.settings_theme_light_title),
					subtitle = stringResource(R.string.settings_theme_light_subtitle),
					icon = Icons.Default.LightMode,
					isSelected = currentTheme == "Light",
					onClick = {
						onThemeSelected("Light")
						onDismiss()
					})

				Spacer(modifier = Modifier.height(8.dp))

				ThemeOption(
					title = stringResource(R.string.settings_theme_dark_title),
					subtitle = stringResource(R.string.settings_theme_dark_subtitle),
					icon = Icons.Default.DarkMode,
					isSelected = currentTheme == "Dark",
					onClick = {
						onThemeSelected("Dark")
						onDismiss()
					})

				Spacer(modifier = Modifier.height(8.dp))

				ThemeOption(
					title = stringResource(R.string.settings_theme_system_title),
					subtitle = stringResource(R.string.settings_theme_system_subtitle),
					icon = Icons.Default.Brightness4,
					isSelected = currentTheme == "System",
					onClick = {
						onThemeSelected("System")
						onDismiss()
					})
			}
		}
	}
}

@Composable
private fun ThemeOption(
	title: String, subtitle: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(16.dp))
			.background(
				if (isSelected) {
					MaterialTheme.colorScheme.primaryContainer
				} else {
					MaterialTheme.colorScheme.surface
				}
			)
			.clickable(onClick = onClick)
			.padding(16.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(
			imageVector = icon, contentDescription = null, tint = if (isSelected) {
				MaterialTheme.colorScheme.onPrimaryContainer
			} else {
				MaterialTheme.colorScheme.onSurfaceVariant
			}, modifier = Modifier.size(24.dp)
		)

		Spacer(modifier = Modifier.width(16.dp))

		Column(modifier = Modifier.weight(1f)) {
			Text(
				text = title,
				style = MaterialTheme.typography.bodyMediumEmphasized,
				fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
				color = if (isSelected) {
					MaterialTheme.colorScheme.onPrimaryContainer
				} else {
					MaterialTheme.colorScheme.onSurface
				}
			)
			Text(
				text = subtitle,
				style = MaterialTheme.typography.bodySmall,
				color = if (isSelected) {
					MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
				} else {
					MaterialTheme.colorScheme.onSurfaceVariant
				}
			)
		}

		if (isSelected) {
			RadioButton(
				selected = true, onClick = null, colors = RadioButtonDefaults.colors(
					selectedColor = MaterialTheme.colorScheme.primary
				)
			)
		}
	}
}

@Composable
fun TypographyPickerDialog(
	currentTypography: String,
	onTypographySelected: (String) -> Unit,
	onDismiss: () -> Unit,
) {
	Dialog(onDismissRequest = onDismiss) {
		Surface(
			shape = RoundedCornerShape(28.dp),
			color = MaterialTheme.colorScheme.surfaceContainerHigh,
			tonalElevation = 6.dp,
			modifier = Modifier.testTag("TypographyPickerDialog")
		) {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(24.dp)
			) {
				Text(
					text = stringResource(R.string.settings_typography_dialog_title),
					style = MaterialTheme.typography.headlineSmall,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurface,
					modifier = Modifier.padding(bottom = 16.dp)
				)

				ThemeOption(
					title = stringResource(R.string.settings_typography_default_title),
					subtitle = stringResource(R.string.settings_typography_default_subtitle),
					icon = Icons.Default.TextFields,
					isSelected = currentTypography == "Default",
					onClick = {
						onTypographySelected("Default")
						onDismiss()
					}
				)

				Spacer(modifier = Modifier.height(8.dp))

				ThemeOption(
					title = stringResource(R.string.settings_typography_condensed_title),
					subtitle = stringResource(R.string.settings_typography_condensed_subtitle),
					icon = Icons.Default.TextFields,
					isSelected = currentTypography == "Condensed",
					onClick = {
						onTypographySelected("Condensed")
						onDismiss()
					}
				)

				Spacer(modifier = Modifier.height(8.dp))

				ThemeOption(
					title = stringResource(R.string.settings_typography_expressive_title),
					subtitle = stringResource(R.string.settings_typography_expressive_subtitle),
					icon = Icons.Default.TextFields,
					isSelected = currentTypography == "Expressive",
					onClick = {
						onTypographySelected("Expressive")
						onDismiss()
					}
				)
			}
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

				ThemeOption(
					title = stringResource(R.string.settings_recurrent_payments_view_mode_horizontal_title),
					subtitle = stringResource(R.string.settings_recurrent_payments_view_mode_horizontal_subtitle),
					icon = Icons.Default.Repeat,
					isSelected = currentMode == RecurrentPaymentsViewMode.HORIZONTAL_LIST,
					onClick = {
						onModeSelected(RecurrentPaymentsViewMode.HORIZONTAL_LIST)
						onDismiss()
					}
				)

				Spacer(modifier = Modifier.height(8.dp))

				ThemeOption(
					title = stringResource(R.string.settings_recurrent_payments_view_mode_vertical_title),
					subtitle = stringResource(R.string.settings_recurrent_payments_view_mode_vertical_subtitle),
					icon = Icons.Default.Repeat,
					isSelected = currentMode == RecurrentPaymentsViewMode.VERTICAL_LIST,
					onClick = {
						onModeSelected(RecurrentPaymentsViewMode.VERTICAL_LIST)
						onDismiss()
					}
				)
			}
		}
	}
}

@Composable
private fun NotificationTimePickerDialog(
	initialHour: Int, initialMinute: Int, onDismiss: () -> Unit, onTimeSelected: (Int, Int) -> Unit
) {
	val context = LocalContext.current
	DisposableEffect(context, initialHour, initialMinute) {
		val dialog = TimePickerDialog(
			context,
			{ _, hour, minute -> onTimeSelected(hour, minute) },
			initialHour,
			initialMinute,
			android.text.format.DateFormat.is24HourFormat(context)
		)
		dialog.setOnDismissListener { onDismiss() }
		dialog.show()
		onDispose {
			dialog.setOnDismissListener(null)
			dialog.dismiss()
		}
	}
}

@Composable
private fun RecurrentPaymentsViewMode.label(): String {
	return when (this) {
		RecurrentPaymentsViewMode.HORIZONTAL_LIST -> stringResource(R.string.settings_recurrent_payments_view_mode_horizontal_title)
		RecurrentPaymentsViewMode.VERTICAL_LIST -> stringResource(R.string.settings_recurrent_payments_view_mode_vertical_title)
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

@PreviewScreenSizes
@Composable
private fun PreviewSettings() {
	MinusTheme {
		Settings(
			currentTheme = "System",
			currentTypography = "Expressive",
			isMaterialYouEnabled = true,
			isCreditQuickToggleFeatureEnabled = false,
			recurrentPaymentsViewMode = RecurrentPaymentsViewMode.HORIZONTAL_LIST,
			notificationHour = 9,
			notificationMinute = 0,
			exactAlarmEnabled = true,
			onThemeChange = {},
			onTypographyChange = {},
			onMaterialYouToggle = {},
			onCreditQuickToggleFeatureToggle = {},
			onRecurrentPaymentsViewModeChange = {},
			onNotificationTimeChange = { _, _ -> },
			onOpenExactAlarmSettings = {},
			periodMappingMode = PeriodMappingMode.ACTIVE_BUDGET,
			onPeriodMappingModeChange = {})
	}
}