package com.serranoie.app.minus.presentation.ui.editor.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryTag(
	value: String, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier
) {
	var showDeleteButton by remember { mutableStateOf(false) }

	Surface(
		shape = CircleShape,
		color = MaterialTheme.colorScheme.surface,
		contentColor = MaterialTheme.colorScheme.onSurface,
		modifier = modifier
			.clip(CircleShape)
			.combinedClickable(onClick = {
				if (showDeleteButton) {
					showDeleteButton = false
				} else {
					onClick()
				}
			}, onLongClick = {
				showDeleteButton = true
			})
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
		) {
			Text(
				text = value,
				style = MaterialTheme.typography.bodyMediumCondensed,
				modifier = Modifier
					.padding(horizontal = 12.dp, vertical = 8.dp)
					.heightIn(min = 28.dp)
					.wrapContentHeight(align = Alignment.CenterVertically),
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
			)

			AnimatedVisibility(
				visible = showDeleteButton,
				enter = scaleIn(tween(durationMillis = 150)) + fadeIn(tween(durationMillis = 150)),
				exit = scaleOut(tween(durationMillis = 150)) + fadeOut(tween(durationMillis = 150)),
			) {
				IconButton(
					onClick = {
						onDelete()
						showDeleteButton = false
					},
					modifier = Modifier.size(32.dp),
					colors = IconButtonDefaults.iconButtonColors(
						contentColor = MaterialTheme.colorScheme.error,
					),
				) {
					Icon(
						imageVector = Icons.Default.Close,
						contentDescription = "Delete category",
						modifier = Modifier.size(16.dp),
					)
				}
			}

			if (!showDeleteButton) {
				Spacer(modifier = Modifier.width(4.dp))
			}
		}
	}
}


@Preview(name = "Tag")
@Composable
private fun PreviewCategoryTag() {
	CategoryTag(
		value = "Mock Category",
		onClick = {},
		onDelete = {},
	)
}
