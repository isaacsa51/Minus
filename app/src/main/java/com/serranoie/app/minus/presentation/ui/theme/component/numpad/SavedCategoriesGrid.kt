package com.serranoie.app.minus.presentation.ui.theme.component.numpad

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition

private val MinCategoryItemHeight = 52.dp

private data class CornerRadii(
    val topStart: Dp,
    val topEnd: Dp,
    val bottomStart: Dp,
    val bottomEnd: Dp
)

private fun positionFor(index: Int, lastIndex: Int): PaddedListItemPosition = when {
    lastIndex == 0 -> PaddedListItemPosition.Single
    index == 0 -> PaddedListItemPosition.First
    index == lastIndex -> PaddedListItemPosition.Last
    else -> PaddedListItemPosition.Middle
}

@Composable
fun SavedCategoriesGrid(
    modifier: Modifier = Modifier,
    tags: List<String>,
    onCategorySelected: (String) -> Unit,
    selectedCategory: String? = null,
    applyWindowInsets: Boolean = true,
) {
    val baseModifier = if (applyWindowInsets) {
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    } else {
        modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    }

    BoxWithConstraints(baseModifier) {
        val wouldSquash = tags.isNotEmpty() &&
                (maxHeight / tags.size) < MinCategoryItemHeight

        if (wouldSquash) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(tags, key = { _, tag -> tag }) { index, tag ->
                    CategoryListItem(
                        label = tag,
                        isSelected = tag == selectedCategory,
                        position = positionFor(index, tags.lastIndex),
                        onClick = { onCategorySelected(tag) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = MinCategoryItemHeight),
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                tags.forEachIndexed { index, tag ->
                    CategoryListItem(
                        label = tag,
                        isSelected = tag == selectedCategory,
                        position = positionFor(index, tags.lastIndex),
                        onClick = { onCategorySelected(tag) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryListItem(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    position: PaddedListItemPosition,
    onClick: () -> Unit,
) {
    val (targetTopStart, targetTopEnd, targetBottomStart, targetBottomEnd) = when {
        isSelected || position == PaddedListItemPosition.Single -> CornerRadii(
            16.dp,
            16.dp,
            16.dp,
            16.dp
        )

        position == PaddedListItemPosition.First -> CornerRadii(16.dp, 16.dp, 4.dp, 4.dp)
        position == PaddedListItemPosition.Last -> CornerRadii(4.dp, 4.dp, 16.dp, 16.dp)
        else -> CornerRadii(4.dp, 4.dp, 4.dp, 4.dp)
    }
    val cornerAnimSpec = tween<Dp>(durationMillis = 200)
    val topStart by animateDpAsState(targetTopStart, cornerAnimSpec, label = "categoryItemTopStart")
    val topEnd by animateDpAsState(targetTopEnd, cornerAnimSpec, label = "categoryItemTopEnd")
    val bottomStart by animateDpAsState(
        targetBottomStart,
        cornerAnimSpec,
        label = "categoryItemBottomStart"
    )
    val bottomEnd by animateDpAsState(
        targetBottomEnd,
        cornerAnimSpec,
        label = "categoryItemBottomEnd"
    )
    val shape = RoundedCornerShape(
        topStart = topStart, topEnd = topEnd, bottomStart = bottomStart, bottomEnd = bottomEnd,
    )

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(durationMillis = 200),
        label = "categoryItemContainerColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 200),
        label = "categoryItemContentColor",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(shape)
            .clickable { onClick() },
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
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
