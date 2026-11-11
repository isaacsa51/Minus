package com.serranoie.app.minus.presentation.ui.history.sections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R

internal fun LazyListScope.historySearchSection(
    query: String,
    appliedQuery: String,
    resultCount: Int,
    resultTotal: String,
    onQueryChange: (String) -> Unit,
) {
    item("history-search") {
        HistorySearchField(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )
    }
    if (appliedQuery.isNotBlank() && resultCount > 0) {
        item("history-search-summary") {
            HistorySearchSummary(count = resultCount, total = resultTotal)
        }
    }
    if (appliedQuery.isNotBlank() && resultCount == 0) {
        item("history-search-empty") {
            Text(
                text = stringResource(R.string.history_search_no_results, appliedQuery.trim()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 24.dp),
            )
        }
    }
}

@Composable
internal fun HistorySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    BackHandler(enabled = query.isNotEmpty()) {
        onQueryChange("")
        focusManager.clearFocus()
    }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        shape = CircleShape,
        placeholder = { Text(text = stringResource(R.string.history_search_placeholder)) },
        leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = {
                    onQueryChange("")
                    focusManager.clearFocus()
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.history_search_clear),
                    )
                }
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
internal fun HistorySearchSummary(
    count: Int,
    total: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = pluralStringResource(R.plurals.history_search_results_summary, count, count, total),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 8.dp),
    )
}
