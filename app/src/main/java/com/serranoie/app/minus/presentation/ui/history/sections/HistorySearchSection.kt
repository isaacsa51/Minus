package com.serranoie.app.minus.presentation.ui.history.sections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.tooling.preview.Preview
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.util.censor
import kotlinx.coroutines.delay

private const val SEARCH_LABEL_MILLIS = 4_000L
private const val SEARCH_EXAMPLE_MILLIS = 2_000L
private const val LABEL_SLIDE_MILLIS = 300
private const val LABEL_FADE_MILLIS = 150
private const val LABEL_SCALE = 0.85f

internal fun LazyListScope.historySearchSection(
    query: String,
    appliedQuery: String,
    resultCount: Int,
    resultTotal: String,
    filterActive: Boolean,
    pending: Boolean,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
) {
    item("history-search") {
        HistorySearchField(
            query = query,
            onQueryChange = onQueryChange,
            filterActive = filterActive,
            onFilterClick = onFilterClick,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )
    }
    val searching = appliedQuery.isNotBlank() || filterActive
    if (pending) {
        item("history-search-skeleton") {
            HistorySearchSkeleton(modifier = Modifier.padding(vertical = 8.dp))
        }
    } else if (searching && resultCount > 0) {
        item("history-search-summary") {
            HistorySearchSummary(count = resultCount, total = resultTotal)
        }
    } else if (searching) {
        item("history-search-empty") {
            Text(
                text = if (appliedQuery.isNotBlank()) {
                    stringResource(R.string.history_search_no_results, appliedQuery.trim())
                } else {
                    stringResource(R.string.history_search_no_results_filtered)
                },
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
    filterActive: Boolean = false,
    onFilterClick: (() -> Unit)? = null,
) {
    SearchFieldLayout(
        query = query,
        onQueryChange = onQueryChange,
        modifier = modifier,
        filterActive = filterActive,
        onFilterClick = onFilterClick,
    ) {
        RotatingSearchPlaceholder()
    }
}

@Composable
internal fun HistorySearchPlaceholderField(
    example: String?,
    modifier: Modifier = Modifier,
) {
    SearchFieldLayout(query = "", onQueryChange = {}, modifier = modifier) {
        SearchPlaceholder(example = example)
    }
}

@Composable
private fun SearchFieldLayout(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier,
    filterActive: Boolean = false,
    onFilterClick: (() -> Unit)? = null,
    placeholder: @Composable () -> Unit,
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
        placeholder = placeholder,
        leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty() || onFilterClick != null) {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
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
                    if (onFilterClick != null) {
                        SearchFilterButton(active = filterActive, onClick = onFilterClick)
                    }
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
private fun SearchFilterButton(active: Boolean, onClick: () -> Unit) {
    val description = stringResource(if (active) R.string.history_filter_open_active else R.string.history_filter_open)
    val colors = if (active) {
        IconButtonDefaults.filledTonalIconButtonColors()
    } else {
        IconButtonDefaults.iconButtonColors()
    }
    IconButton(onClick = onClick, colors = colors, modifier = Modifier.padding(end = 4.dp)) {
        BadgedBox(badge = { if (active) Badge() }) {
            Icon(imageVector = Icons.Rounded.FilterList, contentDescription = description)
        }
    }
}

@Composable
private fun RotatingSearchPlaceholder() {
    val examples = stringArrayResource(R.array.history_search_examples)
    var nextExample by rememberSaveable { mutableIntStateOf(0) }
    var shownExample by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(examples.size) {
        while (true) {
            delay(SEARCH_LABEL_MILLIS)
            shownExample = examples[nextExample % examples.size]
            delay(SEARCH_EXAMPLE_MILLIS)
            shownExample = null
            nextExample = (nextExample + 1) % examples.size
        }
    }
    SearchPlaceholder(example = shownExample)
}

@Composable
private fun SearchPlaceholder(example: String?) {
    AnimatedContent(
        targetState = example,
        transitionSpec = {
            val move = tween<IntOffset>(LABEL_SLIDE_MILLIS)
            val scale = tween<Float>(LABEL_SLIDE_MILLIS)
            val fade = tween<Float>(LABEL_FADE_MILLIS)
            slideInVertically(move) { it } + scaleIn(scale, LABEL_SCALE) + fadeIn(fade) togetherWith
                slideOutVertically(move) { -it } + scaleOut(scale, LABEL_SCALE) + fadeOut(fade)
        },
        label = "historySearchPlaceholder",
    ) { shown ->
        Text(
            text = shown ?: stringResource(R.string.history_search_placeholder),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
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
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .censor(),
    )
}

@Preview(showBackground = true)
@Composable
private fun HistorySearchFieldPreview() {
    MinusTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            HistorySearchField(
                query = "",
                onQueryChange = {},
            )
            HistorySearchField(
                query = "coffee",
                onQueryChange = {},
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
