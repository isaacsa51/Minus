package com.serranoie.app.wear.minus.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.serranoie.app.wear.minus.presentation.theme.MinusTheme

@Composable
internal fun CategoryDecEntryScreen(
    amount: String,
    categories: List<String>,
    selectedCategory: String,
    onCategoryTap: (String) -> Unit,
    onCategoryInputChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    val shownCategories = categories.take(8)
    val listState = rememberTransformingLazyColumnState()

    AppScaffold(timeText = {}) {
        ScreenScaffold(
            scrollState = listState,
            timeText = null,
            edgeButton = {
                EdgeButton(onClick = onSave) {
                    Text("Save")
                }
            }
        ) {
            TransformingLazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 56.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Text(text = "$$amount", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))
                }

                item {
                    ManualCategoryInput(
                        value = selectedCategory,
                        onValueChange = onCategoryInputChanged,
                    )
                }

                if (shownCategories.isNotEmpty()) {
                    items(shownCategories) { category ->
                        CategoryChipButton(
                            label = category,
                            selected = category == selectedCategory,
                            onClick = { onCategoryTap(category) }
                        )
                    }
                } else {
                    item {
                        Text(text = "No categories yet", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualCategoryInput(
    value: String,
    onValueChange: (String) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.take(24)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done
        ),
        textStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSecondaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(5.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        decorationBox = { innerTextField ->
            if (value.isBlank()) {
                Text(
                    text = "Type category",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            innerTextField()
        }
    )
}

@Composable
private fun CategoryChipButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        colors = if (selected) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        }
    ) {
        if (selected) {
            Text(
                text = "✓",
                modifier = Modifier.padding(end = 4.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Preview(device = "id:wearos_small_round", showBackground = true, showSystemUi = true)
@Composable
private fun PreviewCategoryEntry() {
    MinusTheme {
        CategoryDecEntryScreen(
            amount = "123",
            categories = listOf("Groceries", "Coffee", "Food"),
            selectedCategory = "Coffee",
            onCategoryTap = {},
            onCategoryInputChanged = {},
            onSave = {},
        )
    }
}
