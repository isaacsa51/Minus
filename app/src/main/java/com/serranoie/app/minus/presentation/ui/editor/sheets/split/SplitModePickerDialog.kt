package com.serranoie.app.minus.presentation.ui.editor.sheets.split

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed

@Composable
fun SplitModePickerDialog(
    currentMode: BudgetSplitMode,
    onDismiss: () -> Unit,
    onSelect: (BudgetSplitMode) -> Unit,
) {
    val modes = listOf(BudgetSplitMode.STATIC, BudgetSplitMode.DYNAMIC)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.split_mode_dialog_title),
                style = MaterialTheme.typography.titleLargeEmphasized,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.split_mode_dialog_description),
                    style = MaterialTheme.typography.bodySmallCondensed,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                modes.forEach { mode ->
                    val isSelected = mode == currentMode
                    val title = when (mode) {
                        BudgetSplitMode.STATIC -> stringResource(R.string.split_mode_static)
                        BudgetSplitMode.DYNAMIC -> stringResource(R.string.split_mode_dynamic)
                    }
                    val description = when (mode) {
                        BudgetSplitMode.STATIC -> stringResource(R.string.split_mode_static_desc)
                        BudgetSplitMode.DYNAMIC -> stringResource(R.string.split_mode_dynamic_desc)
                    }
                    OutlinedCard(
                        onClick = { onSelect(mode) },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            },
                        ),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMediumEmphasized,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.close),
                    style = MaterialTheme.typography.labelMediumEmphasized,
                )
            }
        },
    )
}
