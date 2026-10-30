package com.serranoie.app.minus.presentation.ui.editor.category

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.calculator.SplitPreview
import com.serranoie.app.minus.presentation.util.font.format.formatCurrencySymbolOnly

@Composable
fun SplitCategoryPills(
    preview: List<SplitPreview>,
    currencyCode: String,
    showErrors: Boolean,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(start = 4.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        preview.forEachIndexed { index, part ->
            val isError = showErrors && (part.category == null || part.amount == null)
            Surface(
                shape = CircleShape,
                color = if (isError) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
            ) {
                Row(
                    modifier = Modifier
                        .heightIn(min = 32.dp)
                        .padding(start = 12.dp, end = if (part.category != null) 2.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    part.category?.let { Text(text = it, style = MaterialTheme.typography.labelLarge) }
                    part.amount?.let { amount ->
                        if (part.category != null) Spacer(Modifier.width(6.dp))
                        Text(
                            text = formatCurrencySymbolOnly(amount, currencyCode),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    part.category?.let { category ->
                        IconButton(onClick = { onRemove(index) }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.split_category_remove, category),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
