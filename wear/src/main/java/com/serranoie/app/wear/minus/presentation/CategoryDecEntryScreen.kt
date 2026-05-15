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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.serranoie.app.wear.minus.presentation.theme.MinusTheme
import com.serranoie.app.wear.minus.presentation.theme.component.CustomPaddedListItem
import com.serranoie.app.wear.minus.presentation.theme.component.PaddedListGroup
import com.serranoie.app.wear.minus.presentation.theme.component.PaddedListItemPosition

@Composable
internal fun CategoryDecEntryScreen(
	amount: String,
	categories: List<String>,
	selectedCategory: String,
	onCategoryTap: (String) -> Unit,
	onCategoryInputChanged: (String) -> Unit,
	onSave: () -> Unit
) {
	val listState = rememberTransformingLazyColumnState()

	AppScaffold(timeText = {}) {
		ScreenScaffold(
			scrollState = listState, timeText = null, edgeButton = {
				EdgeButton(onClick = onSave) {
					Text("Save")
				}
			}) {
			TransformingLazyColumn(
				state = listState,
				modifier = Modifier
					.fillMaxSize()
					.padding(horizontal = 8.dp),
				contentPadding = PaddingValues(top = 8.dp, bottom = 56.dp),
				verticalArrangement = Arrangement.spacedBy(6.dp)
			) {
				item {
					Text(
						text = "$$amount",
						style = MaterialTheme.typography.displayMedium,
						modifier = Modifier.padding(top = 8.dp)
					)
				}

				item {
					ManualCategoryInput(
						value = selectedCategory,
						onValueChange = onCategoryInputChanged,
					)
				}

				item {
					if (categories.isNotEmpty()) {
						PaddedListGroup {
							categories.forEachIndexed { index, category ->
								CategoryListItem(
									label = category,
									selected = category == selectedCategory,
									position = categoryPosition(index, categories.lastIndex),
									onClick = { onCategoryTap(category) })
							}
						}
					} else {
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
			capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done
		),
		textStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onTertiaryContainer),
		modifier = Modifier
			.fillMaxWidth()
			.height(36.dp)
			.background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(5.dp))
			.padding(horizontal = 16.dp, vertical = 8.dp),
		decorationBox = { innerTextField ->
			if (value.isBlank()) {
				Text(
					text = "Type category",
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
				)
			}
			innerTextField()
		})
}

@Composable
private fun CategoryListItem(
	label: String, selected: Boolean, position: PaddedListItemPosition, onClick: () -> Unit
) {
	CustomPaddedListItem(
		onClick = onClick, position = position, background = if (selected) {
			MaterialTheme.colorScheme.secondary
		} else {
			MaterialTheme.colorScheme.surfaceContainer
		}, contentColor = if (selected) {
			MaterialTheme.colorScheme.onSecondary
		} else {
			MaterialTheme.colorScheme.onSurface
		}
	) {
		Text(
			text = label, maxLines = 1, style = MaterialTheme.typography.labelSmall.copy(
				fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
			), color = if (selected) {
				MaterialTheme.colorScheme.onPrimary
			} else {
				MaterialTheme.colorScheme.onSurface
			}, modifier = Modifier.fillMaxWidth()
		)
	}
}

private fun categoryPosition(index: Int, lastIndex: Int): PaddedListItemPosition {
	return when {
		lastIndex == 0 -> PaddedListItemPosition.Single
		index == 0 -> PaddedListItemPosition.First
		index == lastIndex -> PaddedListItemPosition.Last
		else -> PaddedListItemPosition.Middle
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
