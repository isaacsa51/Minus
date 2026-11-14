@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.ui.theme.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.foundation.horizontalScroll
import com.serranoie.app.minus.presentation.util.haptic.HapticUtil.performUIHaptic
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.NextPlan
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Savings
import com.serranoie.app.minus.domain.model.SavingsPocket
import com.serranoie.app.minus.presentation.ui.analytics.pocketDisplayName
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import java.math.BigDecimal

private val HeaderBadgeSize = 56.dp
private const val BadgeSpinMillis = 12_000
private val ActionOuterRadius = 24.dp
private val ActionInnerRadius = 6.dp

private fun actionShape(index: Int, count: Int): Shape {
    val top = if (index == 0) ActionOuterRadius else ActionInnerRadius
    val bottom = if (index == count - 1) ActionOuterRadius else ActionInnerRadius
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
fun RolloverDialog(
    remainingAmount: BigDecimal,
    currencyCode: String,
    periodLabel: String,
    spentAmount: BigDecimal?,
    onSplitEqually: () -> Unit,
    onCarryToNextDay: () -> Unit,
    onDismiss: () -> Unit,
    onDiscardRemaining: (() -> Unit)? = null,
    pockets: List<SavingsPocket> = emptyList(),
    onSaveToPocket: (SavingsPocket) -> Unit = {},
) {
    if (LocalInspectionMode.current) {
        RolloverDialogContent(
            remainingAmount = remainingAmount,
            currencyCode = currencyCode,
            periodLabel = periodLabel,
            spentAmount = spentAmount,
            onSplitEqually = onSplitEqually,
            onCarryToNextDay = onCarryToNextDay,
            onDismiss = onDismiss,
            onDiscardRemaining = onDiscardRemaining,
            pockets = pockets,
            onSaveToPocket = onSaveToPocket,
        )
    } else {
        Dialog(onDismissRequest = onDismiss) {
            RolloverDialogContent(
                remainingAmount = remainingAmount,
                currencyCode = currencyCode,
                periodLabel = periodLabel,
                spentAmount = spentAmount,
                onSplitEqually = onSplitEqually,
                onCarryToNextDay = onCarryToNextDay,
                onDismiss = onDismiss,
                onDiscardRemaining = onDiscardRemaining,
                pockets = pockets,
                onSaveToPocket = onSaveToPocket,
            )
        }
    }
}

@Composable
@Suppress("LongMethod", "LongParameterList")
private fun RolloverDialogContent(
    remainingAmount: BigDecimal,
    currencyCode: String,
    periodLabel: String,
    spentAmount: BigDecimal?,
    onSplitEqually: () -> Unit,
    onCarryToNextDay: () -> Unit,
    onDismiss: () -> Unit,
    onDiscardRemaining: (() -> Unit)? = null,
    pockets: List<SavingsPocket> = emptyList(),
    onSaveToPocket: (SavingsPocket) -> Unit = {},
) {
    val currencyFormat = symbolOnlyCurrencyFormat(currencyCode)
    val formattedRemaining = currencyFormat.format(remainingAmount)
    val formattedSpent = spentAmount?.let { currencyFormat.format(it) }
    val locale = LocalLocale.current.platformLocale

    val scrollState = rememberScrollState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val badgeRotation by rememberInfiniteTransition(label = "rolloverBadge").animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(BadgeSpinMillis, easing = LinearEasing)),
                    label = "rolloverBadgeRotation",
                )
                Box(
                    modifier = Modifier.size(HeaderBadgeSize),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { rotationZ = badgeRotation }
                            .background(
                                color = MaterialTheme.colorScheme.tertiary,
                                shape = MaterialShapes.Cookie7Sided.toShape(),
                            ),
                    )
                    Icon(
                        imageVector = Icons.Rounded.Celebration,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(0.48f),
                        tint = MaterialTheme.colorScheme.onTertiary,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.rollover_dialog_period_finished_title),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = periodLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = formattedRemaining,
                    style = MaterialTheme.typography.displaySmallEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
                Text(
                    text = listOfNotNull(
                        stringResource(R.string.remaining),
                        formattedSpent?.let { "${stringResource(R.string.total_spent)} $it" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelMediumCondensed,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val segments = 2 + (if (pockets.isNotEmpty()) 1 else 0) + (if (onDiscardRemaining != null) 1 else 0)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                RolloverActionRow(
                    icon = Icons.AutoMirrored.Rounded.NextPlan,
                    title = stringResource(R.string.rollover_dialog_carry_to_tomorrow_title),
                    description = stringResource(R.string.rollover_dialog_carry_to_tomorrow_desc, formattedRemaining),
                    highlighted = true,
                    shape = actionShape(0, segments),
                    onClick = onCarryToNextDay,
                )
                RolloverActionRow(
                    icon = Icons.AutoMirrored.Rounded.CallSplit,
                    title = stringResource(R.string.rollover_dialog_split_equally_title),
                    description = stringResource(R.string.rollover_dialog_split_equally_desc, formattedRemaining),
                    highlighted = false,
                    shape = actionShape(1, segments),
                    onClick = onSplitEqually,
                )
                if (pockets.isNotEmpty()) {
                    SaveToPocketRow(
                        pockets = pockets,
                        formattedRemaining = formattedRemaining,
                        shape = actionShape(2, segments),
                        onSaveToPocket = onSaveToPocket,
                    )
                }
                if (onDiscardRemaining != null) {
                    RolloverActionRow(
                        icon = Icons.Outlined.Cancel,
                        title = stringResource(R.string.rollover_dialog_view_analytics_title),
                        description = stringResource(R.string.rollover_dialog_view_analytics_desc),
                        highlighted = false,
                        destructive = true,
                        shape = actionShape(segments - 1, segments),
                        onClick = onDiscardRemaining,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = MaterialTheme.typography.labelMediumEmphasized,
                )
            }
        }
    }
}

@Composable
private fun SaveToPocketRow(
    pockets: List<SavingsPocket>,
    formattedRemaining: String,
    shape: Shape,
    onSaveToPocket: (SavingsPocket) -> Unit,
) {
    var selectedId by remember(pockets) { mutableLongStateOf(pockets.first().id) }
    val selected = pockets.firstOrNull { it.id == selectedId } ?: pockets.first()
    val view = LocalView.current

    Card(
        onClick = { onSaveToPocket(selected) },
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = if (pockets.size > 1) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsLeadingIcon(icon = Icons.Rounded.Savings, active = false)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.rollover_dialog_save_to_pocket_title, pocketDisplayName(selected)),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    lineHeight = 1.2.em,
                )
                Text(
                    text = stringResource(R.string.rollover_dialog_save_to_pocket_desc, formattedRemaining),
                    style = MaterialTheme.typography.bodySmallCondensed,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
        if (pockets.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                pockets.forEachIndexed { index, pocket ->
                    ToggleButton(
                        checked = pocket.id == selected.id,
                        onCheckedChange = {
                            performUIHaptic(view)
                            selectedId = pocket.id
                        },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            pockets.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                        modifier = Modifier.semantics { role = Role.RadioButton },
                    ) {
                        Text(
                            text = pocketDisplayName(pocket),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RolloverActionRow(
    icon: ImageVector,
    title: String,
    description: String,
    highlighted: Boolean,
    shape: Shape,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val container = if (highlighted) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val onContainer = if (highlighted) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = onContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsLeadingIcon(
                icon = icon,
                active = highlighted,
                tint = if (destructive) MaterialTheme.colorScheme.error else null,
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    lineHeight = 1.2.em
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmallCondensed
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Estado Básico")
@Composable
private fun RolloverDialogBasicPreview() {
    MinusTheme {
        RolloverDialog(
            remainingAmount = BigDecimal("150.50"),
            currencyCode = "MXN",
            periodLabel = "1 abr - 30 abr",
            spentAmount = BigDecimal("850.00"),
            onSplitEqually = {},
            onCarryToNextDay = {},
            onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Con Info de Periodo")
@Composable
private fun RolloverDialogInfoPreview() {
    MinusTheme {
        RolloverDialog(
            remainingAmount = BigDecimal("2450.00"),
            currencyCode = "USD",
            periodLabel = "Periodo: 1 - 15 de Octubre",
            spentAmount = BigDecimal("5716.00"),
            onSplitEqually = {},
            onCarryToNextDay = {},
            onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Con Análisis")
@Composable
private fun RolloverDialogAnalyticsPreview() {
    MinusTheme {
        RolloverDialog(
            remainingAmount = BigDecimal("320.00"),
            currencyCode = "EUR",
            periodLabel = "1 may - 31 may",
            spentAmount = BigDecimal("1680.00"),
            onSplitEqually = {},
            onCarryToNextDay = {},
            onDismiss = {},
            onDiscardRemaining = {})
    }
}

@Preview(showBackground = true, name = "Estado Completo")
@Composable
private fun RolloverDialogFullPreview() {
    MinusTheme {
        RolloverDialog(
            remainingAmount = BigDecimal("1250.75"),
            currencyCode = "MXN",
            periodLabel = "Septiembre 2023",
            spentAmount = BigDecimal("15400.00"),
            onSplitEqually = {},
            onCarryToNextDay = {},
            onDismiss = {},
            onDiscardRemaining = {})
    }
}

@Preview(
    showBackground = true, name = "Modo Noche", uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun RolloverDialogDarkModePreview() {
    MinusTheme {
        RolloverDialog(
            remainingAmount = BigDecimal("500.00"),
            currencyCode = "MXN",
            periodLabel = "Ayer",
            spentAmount = BigDecimal("1200.00"),
            onSplitEqually = {},
            onCarryToNextDay = {},
            onDismiss = {},
            onDiscardRemaining = {})
    }
}
