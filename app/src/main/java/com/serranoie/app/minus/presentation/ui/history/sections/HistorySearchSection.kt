package com.serranoie.app.minus.presentation.ui.history.sections

import androidx.activity.compose.BackHandler
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
    SearchFieldLayout(query = query, onQueryChange = onQueryChange, modifier = modifier) {
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
