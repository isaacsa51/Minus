package com.serranoie.app.minus.presentation.ui.settings.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.SavingsPreferences
import com.serranoie.app.minus.domain.model.SavingsSplitPreset
import java.math.BigDecimal

@Composable
internal fun savingsPreferencesSummary(prefs: SavingsPreferences): String = when (prefs.preset) {
    SavingsSplitPreset.BALANCED -> stringResource(
        R.string.settings_savings_dialog_split_format,
        prefs.needsPct, prefs.wantsPct, prefs.savingsPct,
    ) + " — " + stringResource(R.string.settings_savings_preset_balanced_title)
    SavingsSplitPreset.AGGRESSIVE_SAVER -> stringResource(
        R.string.settings_savings_dialog_split_format,
        prefs.needsPct, prefs.wantsPct, prefs.savingsPct,
    ) + " — " + stringResource(R.string.settings_savings_preset_aggressive_title)
    SavingsSplitPreset.CONSERVATIVE -> stringResource(
        R.string.settings_savings_dialog_split_format,
        prefs.needsPct, prefs.wantsPct, prefs.savingsPct,
    ) + " — " + stringResource(R.string.settings_savings_preset_conservative_title)
    SavingsSplitPreset.CUSTOM -> stringResource(
        R.string.settings_savings_dialog_split_format,
        prefs.needsPct, prefs.wantsPct, prefs.savingsPct,
    ) + " — " + stringResource(R.string.settings_savings_preset_custom_title)
}

@Composable
fun SavingsPreferencesEditor(
    current: SavingsPreferences,
    onChange: (SavingsPreferences) -> Unit,
) {
    val selectedPreset = current.preset
    val customNeedsPct = current.needsPct
    val customWantsPct = current.wantsPct
    val customSavingsPct = current.savingsPct

    var goalAmountText by remember(current.savingsGoalAmount) {
        mutableStateOf(current.savingsGoalAmount?.toPlainString().orEmpty())
    }
    var goalMonthsText by remember(current.savingsGoalMonths) {
        mutableStateOf(current.savingsGoalMonths?.toString().orEmpty())
    }

    val customTotal = customNeedsPct + customWantsPct + customSavingsPct
    val isCustomSplitValid = customTotal == 100

    val parsedGoalAmount = remember(goalAmountText) {
        goalAmountText.trim().takeIf { it.isNotEmpty() }?.toBigDecimalOrNull()
    }
    val parsedGoalMonths = remember(goalMonthsText) {
        goalMonthsText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
            ?.takeIf { it in 1..600 }
    }
    val isGoalValid = remember(parsedGoalAmount, parsedGoalMonths) {
        val amountEmpty = goalAmountText.isBlank()
        val monthsEmpty = goalMonthsText.isBlank()
        when {
            amountEmpty && monthsEmpty -> true
            amountEmpty || monthsEmpty -> false
            else -> (parsedGoalAmount ?: BigDecimal.ZERO) > BigDecimal.ZERO &&
                (parsedGoalMonths ?: 0) > 0
        }
    }

    LaunchedEffect(parsedGoalAmount, parsedGoalMonths, isGoalValid) {
        if (isGoalValid) {
            val newAmount = parsedGoalAmount
            val newMonths = parsedGoalMonths
            if (newAmount != current.savingsGoalAmount || newMonths != current.savingsGoalMonths) {
                onChange(
                    current.copy(
                        savingsGoalAmount = newAmount,
                        savingsGoalMonths = newMonths,
                    )
                )
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val latestCurrent = remember { mutableStateOf(current) }
    latestCurrent.value = current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_STOP) return@LifecycleEventObserver
            val final = latestCurrent.value
            val total = final.needsPct + final.wantsPct + final.savingsPct
            if (total != 100 && final.preset == SavingsSplitPreset.CUSTOM) {
                onChange(
                    SavingsPreferences.DEFAULT.copy(
                        savingsGoalAmount = final.savingsGoalAmount,
                        savingsGoalMonths = final.savingsGoalMonths,
                    )
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_savings_dialog_split_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
        )

        SavingsPresetRow(
            title = stringResource(R.string.settings_savings_preset_balanced_title),
            subtitle = stringResource(R.string.settings_savings_preset_balanced_subtitle),
            isSelected = selectedPreset == SavingsSplitPreset.BALANCED,
            onClick = {
                onChange(
                    SavingsPreferences.fromPreset(
                        preset = SavingsSplitPreset.BALANCED,
                        savingsGoalAmount = current.savingsGoalAmount,
                        savingsGoalMonths = current.savingsGoalMonths,
                    )
                )
            },
        )
        SavingsPresetRow(
            title = stringResource(R.string.settings_savings_preset_aggressive_title),
            subtitle = stringResource(R.string.settings_savings_preset_aggressive_subtitle),
            isSelected = selectedPreset == SavingsSplitPreset.AGGRESSIVE_SAVER,
            onClick = {
                onChange(
                    SavingsPreferences.fromPreset(
                        preset = SavingsSplitPreset.AGGRESSIVE_SAVER,
                        savingsGoalAmount = current.savingsGoalAmount,
                        savingsGoalMonths = current.savingsGoalMonths,
                    )
                )
            },
        )
        SavingsPresetRow(
            title = stringResource(R.string.settings_savings_preset_conservative_title),
            subtitle = stringResource(R.string.settings_savings_preset_conservative_subtitle),
            isSelected = selectedPreset == SavingsSplitPreset.CONSERVATIVE,
            onClick = {
                onChange(
                    SavingsPreferences.fromPreset(
                        preset = SavingsSplitPreset.CONSERVATIVE,
                        savingsGoalAmount = current.savingsGoalAmount,
                        savingsGoalMonths = current.savingsGoalMonths,
                    )
                )
            },
        )
        SavingsPresetRow(
            title = stringResource(R.string.settings_savings_preset_custom_title),
            subtitle = stringResource(
                R.string.settings_savings_preset_custom_subtitle,
                customNeedsPct,
                customWantsPct,
                customSavingsPct,
            ),
            isSelected = selectedPreset == SavingsSplitPreset.CUSTOM,
            onClick = {
                onChange(
                    current.copy(
                        preset = SavingsSplitPreset.CUSTOM,
                    )
                )
            },
        )

        if (selectedPreset == SavingsSplitPreset.CUSTOM) {
            Text(
                text = stringResource(R.string.settings_savings_dialog_custom_section),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PercentageSliderRow(
                label = stringResource(
                    R.string.settings_savings_dialog_needs_label, customNeedsPct
                ),
                value = customNeedsPct,
                isError = !isCustomSplitValid,
                onValueChange = { newValue ->
                    onChange(current.copy(needsPct = newValue))
                },
            )

            PercentageSliderRow(
                label = stringResource(
                    R.string.settings_savings_dialog_wants_label, customWantsPct
                ),
                value = customWantsPct,
                isError = !isCustomSplitValid,
                onValueChange = { newValue ->
                    onChange(current.copy(wantsPct = newValue))
                },
            )

            PercentageSliderRow(
                label = stringResource(
                    R.string.settings_savings_dialog_savings_label, customSavingsPct
                ),
                value = customSavingsPct,
                isError = !isCustomSplitValid,
                onValueChange = { newValue ->
                    onChange(current.copy(savingsPct = newValue))
                },
            )

            if (isCustomSplitValid) {
                Text(
                    text = stringResource(R.string.settings_savings_dialog_split_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(
                        R.string.settings_savings_dialog_split_invalid, customTotal
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        Text(
            text = stringResource(R.string.settings_savings_dialog_goal_section),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = goalAmountText,
            onValueChange = { goalAmountText = it },
            label = {
                Text(stringResource(R.string.settings_savings_dialog_goal_amount_label))
            },
            singleLine = true,
            isError = !isGoalValid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("SavingsGoalAmountField"),
        )

        OutlinedTextField(
            value = goalMonthsText,
            onValueChange = { goalMonthsText = it },
            label = {
                Text(stringResource(R.string.settings_savings_dialog_goal_months_label))
            },
            singleLine = true,
            isError = !isGoalValid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("SavingsGoalMonthsField"),
        )

        Text(
            text = stringResource(
                R.string.settings_savings_dialog_goal_hint,
                SavingsPreferences.DEFAULT_SAVINGS_PCT,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = {
                    onChange(SavingsPreferences.DEFAULT)
                },
                modifier = Modifier.testTag("SavingsPreferencesResetButton"),
            ) {
                Text(stringResource(R.string.settings_savings_dialog_reset))
            }
        }
    }
}

@Composable
internal fun SavingsPresetRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMediumEmphasized,
                color = contentColor,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) {
                    contentColor.copy(alpha = 0.75f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        RadioButton(
            selected = isSelected,
            onClick = null,
        )
    }
}

@Composable
internal fun PercentageSliderRow(
    label: String,
    value: Int,
    isError: Boolean,
    onValueChange: (Int) -> Unit,
) {
    val sliderColors = if (isError) {
        SliderDefaults.colors(
            activeTrackColor = MaterialTheme.colorScheme.error,
            inactiveTrackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
            thumbColor = MaterialTheme.colorScheme.error,
        )
    } else {
        SliderDefaults.colors()
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(roundHalfDownToInt(it)) },
            valueRange = 0f..100f,
            colors = sliderColors,
        )
    }
}

private fun roundHalfDownToInt(value: Float): Int {
    val floor = value.toInt()
    return if (value - floor > 0.5f) floor + 1 else floor
}
