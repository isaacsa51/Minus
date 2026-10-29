package com.serranoie.app.minus.presentation.ui.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.SymbolPosition

private val SheetHorizontalPadding = 24.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QuickExpenseSheetContent(
    state: QuickExpenseUiState,
    onAmountChange: (String) -> Unit,
    onCategoryToggle: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onOpenApp: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.widget_add_expense_compact_label),
            style = MaterialTheme.typography.headlineSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(horizontal = SheetHorizontalPadding)
                .semantics { heading() },
        )

        if (state.needsBudget) {
            Text(
                text = stringResource(R.string.quick_add_needs_budget),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = SheetHorizontalPadding),
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = SheetHorizontalPadding)) {
                TextButton(onClick = onCancel, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = onOpenApp, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.quick_add_open_app))
                }
            }
            return@Column
        }

        val symbol = state.currency?.symbol?.trim()
        val symbolAtEnd = state.currency?.symbolPosition == SymbolPosition.END
        val saveFailed = state.error == QuickExpenseUiState.Error.SaveFailed
        OutlinedTextField(
            value = state.amount,
            onValueChange = onAmountChange,
            label = { Text(stringResource(R.string.quick_add_amount_label)) },
            prefix = symbol?.takeUnless { symbolAtEnd }?.let { { Text(it) } },
            suffix = symbol?.takeIf { symbolAtEnd }?.let { { Text(it) } },
            isError = saveFailed,
            supportingText = if (saveFailed) {
                { Text(stringResource(R.string.quick_add_save_failed)) }
            } else {
                null
            },
            textStyle = MaterialTheme.typography.headlineMedium,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSave() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SheetHorizontalPadding)
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
        )

        if (state.categories.isEmpty()) {
            Text(
                text = stringResource(R.string.quick_add_no_categories),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = SheetHorizontalPadding),
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = SheetHorizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.categories, key = { it }) { name ->
                    val selected = name == state.selectedCategory
                    FilterChip(
                        selected = selected,
                        onClick = { onCategoryToggle(name) },
                        label = { Text(name, style = MaterialTheme.typography.labelLarge) },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = SheetHorizontalPadding)) {
            TextButton(onClick = onCancel, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.cancel))
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onSave, enabled = state.canSave, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
