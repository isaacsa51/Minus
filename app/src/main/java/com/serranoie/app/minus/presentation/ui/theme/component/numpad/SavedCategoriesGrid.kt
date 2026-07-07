package com.serranoie.app.minus.presentation.ui.theme.component.numpad

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition

private const val MAX_ITEMS_FOR_FILL = 4

@Composable
fun SavedCategoriesGrid(
    tags: List<String>,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedCategory: String? = null,
) {
    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        tags.forEachIndexed { index, tag ->
            val position = when {
                tags.size == 1 -> PaddedListItemPosition.Single
                index == 0 -> PaddedListItemPosition.First
                index == tags.lastIndex -> PaddedListItemPosition.Last
                else -> PaddedListItemPosition.Middle
            }
            val isSelected = tag == selectedCategory

            val itemModifier = if (tags.size <= MAX_ITEMS_FOR_FILL) {
                Modifier.weight(1f)
            } else {
                Modifier
            }

            CategoryListItem(
                label = tag,
                isSelected = isSelected,
                position = position,
                onClick = { onCategorySelected(tag) },
                modifier = itemModifier,
            )
        }
    }
}

@Composable
private fun CategoryListItem(
    label: String,
    isSelected: Boolean,
    position: PaddedListItemPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = when {
        isSelected -> RoundedCornerShape(16.dp)

        position == PaddedListItemPosition.Single -> RoundedCornerShape(16.dp)

        position == PaddedListItemPosition.First -> RoundedCornerShape(
            topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp,
        )

        position == PaddedListItemPosition.Last -> RoundedCornerShape(
            bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, topEnd = 4.dp,
        )

        else -> RoundedCornerShape(4.dp)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(shape)
            .clickable { onClick() },
        shape = shape,
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (isSelected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMediumEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1B1F)
@Composable
private fun SavedCategoriesGridPreview() {
    MinusTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                SavedCategoriesGrid(
                    tags = listOf(
                        "Groceries", "Transport", "Entertainment", "Shopping",
                        "Bills", "Health", "Food", "Coffee", "Gym",
                    ),
                    selectedCategory = "Transport",
                    onCategorySelected = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1B1F)
@Composable
private fun SavedCategoriesGridFewItemsPreview() {
    MinusTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                SavedCategoriesGrid(
                    tags = listOf("Groceries", "Test"),
                    selectedCategory = null,
                    onCategorySelected = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
