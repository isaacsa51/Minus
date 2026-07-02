package com.serranoie.app.minus.presentation.ui.editor.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed

@Composable
fun CurrencySelectorDialog(
    currentCode: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.select_currency),
                style = MaterialTheme.typography.titleLargeEmphasized
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                SupportedCurrency.ALL.forEach { currency ->
                    val isSelected = currency.code == currentCode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(currency.code) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = currency.symbol,
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(50.dp),
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                            Text(
                                text = currency.code,
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                            Text(
                                text = currency.displayName(),
                                style = MaterialTheme.typography.bodySmallCondensed,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.currency_selected),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.close),
                    style = MaterialTheme.typography.labelMediumEmphasized
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun CurrencySelectorDialogPreview() {
    MinusTheme {
        CurrencySelectorDialog(
            currentCode = "USD",
            onDismiss = {},
            onSelect = {}
        )
    }
}
