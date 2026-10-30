package com.serranoie.app.minus.presentation.ui.editor.category

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.calculator.CategoryLimitPace
import com.serranoie.app.minus.domain.calculator.CategoryLimitStatus
import com.serranoie.app.minus.domain.model.CategoryLimit
import com.serranoie.app.minus.domain.model.CategoryLimitUnit
import com.serranoie.app.minus.presentation.ui.theme.LocalBudgetStatusColors
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.util.Utils.confirmFeedback
import com.serranoie.app.minus.presentation.util.font.format.formatCurrencySymbolOnly
import java.math.BigDecimal
import java.time.LocalDate

data class CategoryLimitsUi(
    val statuses: Map<String, CategoryLimitStatus>,
    val pendingAmount: BigDecimal?,
    val currencyCode: String,
    val onSetLimit: (String, BigDecimal?, CategoryLimitUnit) -> Unit,
)

@Composable
fun categoryLimitColor(pace: CategoryLimitPace): Color {
    val colors = LocalBudgetStatusColors.current
    return when (pace) {
        CategoryLimitPace.ON_TRACK -> colors.good
        CategoryLimitPace.AHEAD -> colors.notGood
        CategoryLimitPace.OVER -> colors.bad
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CategoryLimitBar(
    status: CategoryLimitStatus,
    currencyCode: String,
    modifier: Modifier = Modifier,
    pendingAmount: BigDecimal? = null,
    showOverWarning: Boolean = true,
) {
    val overBy = status.overBy(pendingAmount)
    val pace = if (overBy != null) CategoryLimitPace.OVER else status.pace
    val color by animateColorAsState(categoryLimitColor(pace), label = "categoryLimitColor")
    val fraction = status.projectedFraction(pendingAmount)
    val progress by animateFloatAsState(
        targetValue = fraction,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>(),
        label = "categoryLimitProgress",
    )
    val atLimit = fraction >= 1f
    var shownAtLimit by remember { mutableStateOf(atLimit) }
    val projectedSpent = status.spent.add(pendingAmount?.takeIf { it.signum() > 0 } ?: BigDecimal.ZERO)
    val spentText = formatCurrencySymbolOnly(projectedSpent, currencyCode)
    val limitText = formatCurrencySymbolOnly(status.limit.amount, currencyCode)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(
                if (status.limit.unit == CategoryLimitUnit.WEEKLY) {
                    R.string.category_limit_progress_weekly
                } else {
                    R.string.category_limit_progress
                },
                spentText,
                limitText,
            ),
            style = MaterialTheme.typography.labelSmallCondensed,
            color = if (pace == CategoryLimitPace.ON_TRACK) MaterialTheme.colorScheme.onSurfaceVariant else color,
        )
        key(atLimit) {
            var wave by remember { mutableFloatStateOf(if (shownAtLimit) 0.1f else 0f) }
            LaunchedEffect(Unit) {
                withFrameNanos {}
                wave = if (atLimit) 1f else 0f
                shownAtLimit = atLimit
            }
            LinearWavyProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                gapSize = 0.dp,
                stopSize = 0.dp,
                amplitude = { wave },
                wavelength = 64.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
            )
        }
        if (overBy != null && showOverWarning) {
            Text(
                text = stringResource(
                    R.string.category_limit_would_exceed,
                    formatCurrencySymbolOnly(overBy, currencyCode),
                ),
                style = MaterialTheme.typography.labelSmallCondensed,
                color = color,
            )
        }
    }
}

@Composable
fun SelectedCategoryLimitCard(
    status: CategoryLimitStatus,
    pendingAmount: BigDecimal?,
    currencyCode: String,
    modifier: Modifier = Modifier,
) {
    val overBy = status.overBy(pendingAmount)
    val left = status.remaining.subtract(pendingAmount?.takeIf { it.signum() > 0 } ?: BigDecimal.ZERO)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = status.name,
                    style = MaterialTheme.typography.bodyMediumCondensed,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (overBy != null) {
                        stringResource(R.string.category_limit_over, formatCurrencySymbolOnly(overBy, currencyCode))
                    } else {
                        stringResource(R.string.category_limit_left, formatCurrencySymbolOnly(left.max(BigDecimal.ZERO), currencyCode))
                    },
                    style = MaterialTheme.typography.bodyMediumCondensed,
                    color = if (overBy != null) categoryLimitColor(CategoryLimitPace.OVER) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            CategoryLimitBar(
                status = status,
                currencyCode = currencyCode,
                pendingAmount = pendingAmount,
                showOverWarning = false,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CategoryLimitDialog(
    categoryName: String,
    current: CategoryLimit?,
    onDismiss: () -> Unit,
    onSave: (BigDecimal?, CategoryLimitUnit) -> Unit,
) {
    var amountText by rememberSaveable { mutableStateOf(current?.amount?.stripTrailingZeros()?.toPlainString().orEmpty()) }
    var unit by rememberSaveable { mutableStateOf(current?.unit ?: CategoryLimitUnit.PERIOD) }
    val view = LocalView.current
    val amount = remember(amountText) { amountText.replace(',', '.').toBigDecimalOrNull()?.takeIf { it.signum() > 0 } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.category_limit_dialog_title, categoryName),
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { value ->
                        if (value.length <= 12 && value.all { it.isDigit() || it == '.' || it == ',' }) {
                            amountText = value
                        }
                    },
                    label = { Text(stringResource(R.string.category_limit_amount_label)) },
                    singleLine = true,
                    isError = amountText.isNotBlank() && amount == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    val options = listOf(
                        CategoryLimitUnit.WEEKLY to R.string.category_limit_unit_weekly,
                        CategoryLimitUnit.PERIOD to R.string.category_limit_unit_period,
                    )
                    options.forEachIndexed { index, (option, label) ->
                        ToggleButton(
                            checked = unit == option,
                            onCheckedChange = {
                                view.confirmFeedback()
                                unit = option
                            },
                            modifier = Modifier
                                .weight(1f)
                                .semantics { role = Role.RadioButton },
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                                checkedContainerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.tertiary,
                                checkedContentColor = MaterialTheme.colorScheme.onTertiary,
                            ),
                            shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                        ) {
                            Text(stringResource(label), modifier = Modifier.basicMarquee())
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (current != null) {
                    OutlinedIconButton(
                        onClick = { onSave(null, unit) },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = stringResource(R.string.category_limit_remove),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel), style = MaterialTheme.typography.labelSmallEmphasized)
                }
                Button(onClick = { onSave(amount, unit) }, enabled = amount != null) {
                    Text(stringResource(R.string.save), style = MaterialTheme.typography.labelSmallEmphasized)
                }
            }
        },
    )
}

private fun previewStatus(
    name: String,
    spent: String,
    limit: String,
    unit: CategoryLimitUnit,
    pace: CategoryLimitPace,
) = CategoryLimitStatus(
    name = name,
    limit = CategoryLimit(BigDecimal(limit), unit),
    spent = BigDecimal(spent),
    windowStart = LocalDate.of(2026, 11, 9),
    windowEnd = LocalDate.of(2026, 11, 15),
    pace = pace,
)

@Preview(name = "Limit bars")
@Composable
private fun CategoryLimitBarPreview() {
    MinusTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CategoryLimitBar(
                    status = previewStatus("Transport", "30", "120", CategoryLimitUnit.PERIOD, CategoryLimitPace.ON_TRACK),
                    currencyCode = "USD",
                )
                CategoryLimitBar(
                    status = previewStatus("Food", "240", "300", CategoryLimitUnit.PERIOD, CategoryLimitPace.AHEAD),
                    currencyCode = "USD",
                )
                CategoryLimitBar(
                    status = previewStatus("Coffee", "48", "50", CategoryLimitUnit.WEEKLY, CategoryLimitPace.AHEAD),
                    currencyCode = "USD",
                    pendingAmount = BigDecimal("12"),
                )
                CategoryLimitBar(
                    status = previewStatus("Games", "70", "60", CategoryLimitUnit.PERIOD, CategoryLimitPace.OVER),
                    currencyCode = "USD",
                )
            }
        }
    }
}

@Preview(name = "Selected category limit card")
@Composable
private fun SelectedCategoryLimitCardPreview() {
    MinusTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SelectedCategoryLimitCard(
                    status = previewStatus("Transport", "30", "120", CategoryLimitUnit.PERIOD, CategoryLimitPace.ON_TRACK),
                    pendingAmount = null,
                    currencyCode = "USD",
                )
                SelectedCategoryLimitCard(
                    status = previewStatus("Transport", "30", "120", CategoryLimitUnit.PERIOD, CategoryLimitPace.ON_TRACK),
                    pendingAmount = BigDecimal("15"),
                    currencyCode = "USD",
                )
                SelectedCategoryLimitCard(
                    status = previewStatus("Coffee", "48", "50", CategoryLimitUnit.WEEKLY, CategoryLimitPace.AHEAD),
                    pendingAmount = BigDecimal("12"),
                    currencyCode = "USD",
                )
            }
        }
    }
}

@Preview(name = "Limit dialog, new")
@Composable
private fun CategoryLimitDialogNewPreview() {
    MinusTheme {
        CategoryLimitDialog(
            categoryName = "Coffee",
            current = null,
            onDismiss = {},
            onSave = { _, _ -> },
        )
    }
}

@Preview(name = "Limit dialog, existing")
@Composable
private fun CategoryLimitDialogExistingPreview() {
    MinusTheme {
        CategoryLimitDialog(
            categoryName = "Coffee",
            current = CategoryLimit(BigDecimal("50"), CategoryLimitUnit.WEEKLY),
            onDismiss = {},
            onSave = { _, _ -> },
        )
    }
}
