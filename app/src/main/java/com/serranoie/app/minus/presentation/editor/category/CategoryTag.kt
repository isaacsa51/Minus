package com.serranoie.app.minus.presentation.editor.category

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
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

@Composable
fun CategoryTag(
	value: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier
) {
	Surface(
		shape = CircleShape,
		color = MaterialTheme.colorScheme.surface,
		contentColor = MaterialTheme.colorScheme.onSurface,
		modifier = modifier
			.clip(CircleShape)
			.clickable { onClick() }
	) {
		Text(
			text = value,
			style = MaterialTheme.typography.bodyMedium,
			modifier = Modifier
				.padding(horizontal = 12.dp, vertical = 8.dp)
				.heightIn(min = 28.dp)
				.wrapContentHeight(align = Alignment.CenterVertically),
			maxLines = 1,
			overflow = TextOverflow.Ellipsis,
		)
	}
}


@Preview(name = "Tag")
@Composable
private fun PreviewCategoryTag() {
	CategoryTag(
		value = "Mock Category",
		onClick = {}
	)
}