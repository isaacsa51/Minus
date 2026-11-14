package com.serranoie.app.minus.presentation.ui.analytics

import androidx.compose.foundation.BorderStroke
import com.serranoie.app.minus.domain.calculator.PocketPeriodFlow
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.heightIn
import java.text.NumberFormat
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.calculator.SavingsPocketStatus
import com.serranoie.app.minus.domain.model.SavingsPocket
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.Utils.confirmFeedback
import com.serranoie.app.minus.presentation.util.font.format.formatCurrencySymbolOnly
import com.serranoie.app.minus.presentation.util.font.format.localizedFormatter
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun pocketDisplayName(pocket: SavingsPocket): String =
    if (pocket.isGoalPocket) stringResource(R.string.pockets_goal_name) else pocket.name

private sealed interface PocketDialog {
    data object Create : PocketDialog
    data class Deposit(val status: SavingsPocketStatus) : PocketDialog
    data class Withdraw(val status: SavingsPocketStatus) : PocketDialog
    data class Delete(val status: SavingsPocketStatus) : PocketDialog
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
@Suppress("LongParameterList")
fun SavingsPocketsCard(
    pockets: List<SavingsPocketStatus>,
    currencyCode: String,
    canMoveMoney: Boolean,
    onCreatePocket: (String, BigDecimal?) -> Unit,
    onDeposit: (Long, BigDecimal) -> Unit,
    onWithdraw: (Long, BigDecimal) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    periodFlow: PocketPeriodFlow? = null,
) {
    var dialog by remember { mutableStateOf<PocketDialog?>(null) }
    val view = LocalView.current

    val segments = maxOf(pockets.size, 1) + if (canMoveMoney) 1 else 0
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        periodFlow?.let { PocketPeriodSummary(it, currencyCode) }
        if (pockets.isEmpty()) {
            PocketsEmptyState(shape = segmentShape(0, segments))
        }
        pockets.forEachIndexed { index, status ->
            PocketRow(
                status = status,
                currencyCode = currencyCode,
                canMoveMoney = canMoveMoney,
                shape = segmentShape(index, segments),
                onDeposit = { dialog = PocketDialog.Deposit(status) },
                onWithdraw = { dialog = PocketDialog.Withdraw(status) },
                onDelete = { dialog = PocketDialog.Delete(status) },
            )
        }
        if (canMoveMoney) {
            NewPocketSegment(
                shape = segmentShape(segments - 1, segments),
                onClick = {
                    view.confirmFeedback()
                    dialog = PocketDialog.Create
                },
            )
        }
    }

    when (val current = dialog) {
        null -> Unit
        PocketDialog.Create -> NewPocketDialog(
            onDismiss = { dialog = null },
            onSave = { name, target ->
                onCreatePocket(name, target)
                dialog = null
            },
        )

        is PocketDialog.Deposit -> PocketAmountDialog(
            title = stringResource(
                R.string.pockets_add_money_title,
                pocketDisplayName(current.status.pocket)
            ),
            max = null,
            currencyCode = currencyCode,
            onDismiss = { dialog = null },
            onConfirm = {
                onDeposit(current.status.pocket.id, it)
                dialog = null
            },
        )

        is PocketDialog.Withdraw -> PocketAmountDialog(
            title = stringResource(
                R.string.pockets_take_out_title,
                pocketDisplayName(current.status.pocket)
            ),
            max = current.status.balance,
            currencyCode = currencyCode,
            onDismiss = { dialog = null },
            onConfirm = {
                onWithdraw(current.status.pocket.id, it)
                dialog = null
            },
        )

        is PocketDialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = {
                Text(
                    stringResource(
                        R.string.pockets_delete_title,
                        pocketDisplayName(current.status.pocket)
                    )
                )
            },
            text = { Text(stringResource(R.string.pockets_delete_desc)) },
            dismissButton = {
                TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.cancel)) }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(current.status.pocket.id)
                        dialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text(stringResource(R.string.delete)) }
            },
        )
    }
}

private val SegmentOuterRadius = 22.dp
private val SegmentInnerRadius = 6.dp

@Composable
private fun segmentColor() = combineColors(
    MaterialTheme.colorScheme.surface,
    MaterialTheme.colorScheme.surfaceVariant,
    t = 0.3f,
)

@Composable
private fun PocketPeriodSummary(flow: PocketPeriodFlow, currencyCode: String) {
    val added = formatCurrencySymbolOnly(flow.added, currencyCode)
    val withdrawn = formatCurrencySymbolOnly(flow.withdrawn, currencyCode)
    val body = when {
        flow.added.signum() > 0 && flow.withdrawn.signum() > 0 ->
            stringResource(R.string.pockets_period_added_and_withdrawn, added, withdrawn)
        flow.withdrawn.signum() > 0 -> stringResource(R.string.pockets_period_withdrawn, withdrawn)
        else -> stringResource(R.string.pockets_period_added, added)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.pockets_period_title),
            style = MaterialTheme.typography.titleMediumEmphasized,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMediumCondensed,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NewPocketSegment(shape: Shape, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = segmentColor(),
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.pockets_new), style = MaterialTheme.typography.labelLargeEmphasized)
        }
    }
}

private fun segmentShape(index: Int, count: Int): Shape {
    val top = if (index == 0) SegmentOuterRadius else SegmentInnerRadius
    val bottom = if (index == count - 1) SegmentOuterRadius else SegmentInnerRadius
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

@Composable
private fun PocketsEmptyState(shape: Shape) {
    Surface(
        shape = shape,
        color = segmentColor(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PocketAvatar(highlighted = true)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.pockets_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PocketAvatar(highlighted: Boolean) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = if (highlighted) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                shape = if (highlighted) MaterialShapes.Cookie9Sided.toShape() else CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Savings,
            contentDescription = null,
            tint = if (highlighted) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
            modifier = Modifier.size(20.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
@Suppress("LongMethod")
private fun PocketRow(
    status: SavingsPocketStatus,
    currencyCode: String,
    canMoveMoney: Boolean,
    shape: Shape,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit,
    onDelete: () -> Unit,
) {
    val target = status.pocket.target?.takeIf { it.signum() > 0 }
    val isEmpty = status.balance.signum() <= 0
    val progress = target?.let { status.balance.divide(it, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f) }
    Surface(shape = shape, color = segmentColor()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PocketAvatar(highlighted = status.pocket.isGoalPocket)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pocketDisplayName(status.pocket),
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (target != null) {
                        Text(
                            text = status.pocket.targetDate?.let {
                                stringResource(
                                    R.string.pockets_of_target_by,
                                    formatCurrencySymbolOnly(target, currencyCode),
                                    localizedFormatter("MMM yyyy")(it),
                                )
                            } ?: stringResource(R.string.pockets_of_target, formatCurrencySymbolOnly(target, currencyCode)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatCurrencySymbolOnly(status.balance, currencyCode),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = if (isEmpty) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                )
            }
            if (progress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearWavyProgressIndicator(
                        progress = { progress },
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = NumberFormat.getPercentInstance().format(progress),
                        style = MaterialTheme.typography.labelMediumEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (canMoveMoney) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isEmpty && !status.pocket.isGoalPocket) {
                        OutlinedButton(
                            onClick = onDelete,
                            shapes = ButtonDefaults.shapes(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                            Text(stringResource(R.string.pockets_delete), style = MaterialTheme.typography.labelMediumCondensed)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onWithdraw,
                            enabled = !isEmpty,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.pockets_take_out), style = MaterialTheme.typography.labelMediumCondensed)
                        }
                    }
                    FilledTonalButton(
                        onClick = onDeposit,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            stringResource(R.string.pockets_add_money),
                            style = MaterialTheme.typography.labelSmallEmphasized,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private fun parseAmount(text: String): BigDecimal? =
    text.replace(',', '.').toBigDecimalOrNull()?.takeIf { it.signum() > 0 }

private fun isAmountInput(value: String): Boolean =
    value.length <= 12 && value.all { it.isDigit() || it == '.' || it == ',' }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PocketAmountDialog(
    title: String,
    max: BigDecimal?,
    currencyCode: String,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal) -> Unit,
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    val amount = remember(amountText) { parseAmount(amountText) }
    val valid = amount != null && (max == null || amount <= max)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMediumEmphasized) },
        text = {
            OutlinedTextField(
                value = amountText,
                onValueChange = { if (isAmountInput(it)) amountText = it },
                label = { Text(stringResource(R.string.pockets_amount_label)) },
                supportingText = max?.let {
                    {
                        Text(
                            stringResource(
                                R.string.pockets_take_out_max,
                                formatCurrencySymbolOnly(it, currencyCode)
                            )
                        )
                    }
                },
                singleLine = true,
                isError = amountText.isNotBlank() && !valid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        confirmButton = {
            Button(onClick = { amount?.let(onConfirm) }, enabled = valid) {
                Text(stringResource(R.string.save))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NewPocketDialog(
    onDismiss: () -> Unit,
    onSave: (String, BigDecimal?) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var targetText by rememberSaveable { mutableStateOf("") }
    val target = remember(targetText) { parseAmount(targetText) }
    val valid = name.isNotBlank() && (targetText.isBlank() || target != null)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.pockets_new),
                style = MaterialTheme.typography.titleMediumEmphasized
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) name = it },
                    label = { Text(stringResource(R.string.pockets_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { if (isAmountInput(it)) targetText = it },
                    label = { Text(stringResource(R.string.pockets_target_label)) },
                    singleLine = true,
                    isError = targetText.isNotBlank() && target == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        confirmButton = {
            Button(onClick = { onSave(name, target) }, enabled = valid) {
                Text(stringResource(R.string.save))
            }
        },
    )
}

@Preview(name = "Savings pockets")
@Composable
private fun SavingsPocketsCardPreview() {
    MinusTheme {
        Surface {
            SavingsPocketsCard(
                pockets = listOf(
                    SavingsPocketStatus(
                        SavingsPocket(
                            id = 3,
                            name = "",
                            target = BigDecimal("15000"),
                            targetDate = java.time.LocalDate.of(2027, 5, 13)
                        ), BigDecimal.ZERO
                    ),
                    SavingsPocketStatus(
                        SavingsPocket(
                            id = 1,
                            name = "Japan trip",
                            target = BigDecimal("30000")
                        ), BigDecimal("12400")
                    ),
                    SavingsPocketStatus(SavingsPocket(id = 2, name = "Emergency"), BigDecimal.ZERO),
                ),
                currencyCode = "USD",
                canMoveMoney = true,
                onCreatePocket = { _, _ -> },
                onDeposit = { _, _ -> },
                onWithdraw = { _, _ -> },
                onDelete = {},
            )
        }
    }
}

@Preview(name = "Savings pockets - period ended")
@Composable
private fun SavingsPocketsCardPeriodEndedPreview() {
    MinusTheme {
        Surface {
            SavingsPocketsCard(
                pockets = listOf(
                    SavingsPocketStatus(
                        SavingsPocket(
                            id = 3,
                            name = "",
                            target = BigDecimal("15000"),
                            targetDate = java.time.LocalDate.of(2027, 5, 13)
                        ), BigDecimal("1917")
                    ),
                    SavingsPocketStatus(
                        SavingsPocket(
                            id = 1,
                            name = "Japan trip",
                            target = BigDecimal("30000")
                        ), BigDecimal("12400")
                    ),
                ),
                currencyCode = "USD",
                canMoveMoney = false,
                onCreatePocket = { _, _ -> },
                onDeposit = { _, _ -> },
                onWithdraw = { _, _ -> },
                onDelete = {},
                periodFlow = PocketPeriodFlow(added = BigDecimal("1200"), withdrawn = BigDecimal("150")),
            )
        }
    }
}
