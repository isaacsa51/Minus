package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme

data class DeleteUndoSnackbarState(
	val message: String,
	val onUndo: (() -> Unit)? = null,
	val actionLabel: String? = null,
	val autoDismissMillis: Long = 2500L,
	val icon: ImageVector = Icons.Rounded.Delete,
	val iconTintIsError: Boolean = true
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeleteUndoSnackbar(
	message: String,
	onUndo: (() -> Unit)? = null,
	actionLabel: String? = null,
	icon: ImageVector = Icons.Rounded.Delete,
	iconTintIsError: Boolean = true,
	modifier: Modifier = Modifier
) {
	Surface(
		modifier = modifier.fillMaxWidth(),
		shape = RoundedCornerShape(18.dp),
		color = MaterialTheme.colorScheme.inverseSurface,
		contentColor = MaterialTheme.colorScheme.inverseOnSurface,
		shadowElevation = 8.dp
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 12.dp, vertical = 10.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(12.dp)
		) {
//			Box(
//				modifier = Modifier
//					.width(5.dp)
//					.height(28.dp)
//					.background(
//						color = MaterialTheme.colorScheme.error,
//						shape = RoundedCornerShape(99.dp)
//					)
//			)

			Icon(
				imageVector = icon,
				contentDescription = null,
				tint = if (iconTintIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
			)

			Text(
				text = message,
				modifier = Modifier.weight(1f),
				style = MaterialTheme.typography.bodyLarge,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis
			)

			actionLabel?.let { label ->
				onUndo?.let { undoAction ->
					TextButton(onClick = undoAction) {
						Text(
							text = label,
							color = MaterialTheme.colorScheme.primary,
							style = MaterialTheme.typography.titleMediumEmphasized
						)
					}
				}
			}
		}
	}
}

@Preview
@Composable
private fun SnackbarPreview() {
	MinusTheme {
		DeleteUndoSnackbar(
			message = "Gasto eliminado",
			onUndo = {},
		)
	}
}
