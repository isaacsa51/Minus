@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.ui.theme.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.rollover_dialog_period_finished_title),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .size(HeaderBadgeSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Celebration,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(0.52f),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceDim)
                    .padding(16.dp)
            ) {
                if (formattedSpent != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.total_spent),
                            style = MaterialTheme.typography.labelMediumCondensed,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formattedSpent,
                            style = MaterialTheme.typography.titleLargeEmphasized,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.remaining),
                        style = MaterialTheme.typography.labelMediumCondensed,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formattedRemaining,
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            RolloverActionRow(
                icon = Icons.AutoMirrored.Rounded.CallSplit,
                title = stringResource(R.string.rollover_dialog_split_equally_title),
                description = stringResource(
                    R.string.rollover_dialog_split_equally_desc,
                    formattedRemaining
                ),
                highlighted = false,
                onClick = onSplitEqually
            )

            Spacer(modifier = Modifier.height(10.dp))

            RolloverActionRow(
                icon = Icons.AutoMirrored.Rounded.NextPlan,
                title = stringResource(R.string.rollover_dialog_carry_to_tomorrow_title),
                description = stringResource(
                    R.string.rollover_dialog_carry_to_tomorrow_desc,
                    formattedRemaining
                ),
                highlighted = true,
                onClick = onCarryToNextDay
            )

            if (onDiscardRemaining != null) {
                Spacer(modifier = Modifier.height(10.dp))

                RolloverActionRow(
                    icon = Icons.Outlined.Cancel,
                    title = stringResource(R.string.rollover_dialog_view_analytics_title),
                    description = stringResource(R.string.rollover_dialog_view_analytics_desc),
                    highlighted = false,
                    onClick = onDiscardRemaining
                )
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
private fun RolloverActionRow(
    icon: ImageVector,
    title: String,
    description: String,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val container = if (highlighted) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val onContainer = if (highlighted) {
        MaterialTheme.colorScheme.onSecondary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
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
            SettingsLeadingIcon(icon = icon, active = highlighted)

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
