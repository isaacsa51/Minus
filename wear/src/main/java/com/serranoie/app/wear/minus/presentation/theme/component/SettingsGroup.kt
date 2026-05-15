package com.serranoie.app.wear.minus.presentation.theme.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.LocalContentColor
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.serranoie.app.wear.minus.presentation.theme.MinusTheme

/**
 * A flexible settings group container that can hold any composable content.
 *
 * @param modifier Modifier to be applied to the container
 * @param title Optional title displayed above the group
 * @param content The composable content to be displayed inside the group
 */
@Composable
fun FlexibleListGroup(
	modifier: Modifier = Modifier,
	title: String? = null,
	content: @Composable ColumnScope.() -> Unit
) {
	Column(
		modifier = modifier
			.padding(8.dp)
	) {
		title?.let {
			Text(
				text = it,
				color = MaterialTheme.colorScheme.outline,
				style = MaterialTheme.typography.labelLarge,
				modifier = Modifier.padding(bottom = 10.dp)
			)
		}

		SettingsContainer(
			background = MaterialTheme.colorScheme.surfaceContainer,
			shape = RoundedCornerShape(16.dp),
			contentColor = MaterialTheme.colorScheme.onSurface,
		) {
			Column {
				content()
			}
		}
	}
}

/**
 * A standard settings item with title, subtitle, and customizable content.
 *
 * @param title The main title text
 * @param subtitle Optional subtitle text
 * @param onClick Click handler for the item
 * @param leadingIcon Optional leading icon composable
 * @param trailingContent Optional trailing content composable (defaults to arrow icon)
 * @param showDivider Whether to show a divider below this item
 */
@Composable
fun ListItem(
	title: String,
	subtitle: String? = null,
	onClick: () -> Unit,
	leadingIcon: (@Composable () -> Unit)? = null,
	trailingContent: (@Composable () -> Unit)? = null,
	showDivider: Boolean = false
) {
	Column {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.clickable(onClick = onClick)
				.padding(8.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			leadingIcon?.invoke()

			if (leadingIcon != null) {
				Spacer(modifier = Modifier.width(16.dp))
			}

			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = title,
					style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
					color = MaterialTheme.colorScheme.onSurface
				)

				subtitle?.let {
					Text(
						text = it,
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}

			trailingContent?.invoke() ?: Icon(
				imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
				contentDescription = "Navigate",
				tint = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}

		if (showDivider) {
			SettingsDivider(modifier = Modifier.padding(horizontal = 8.dp))
		}
	}
}

/**
 * A completely customizable settings item that provides only the clickable container.
 *
 * @param onClick Click handler for the item
 * @param content The custom content layout
 */
@Composable
fun CustomSettingsItem(
	onClick: () -> Unit,
	content: @Composable RowScope.() -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick)
			.padding(16.dp),
		verticalAlignment = Alignment.CenterVertically,
		content = content
	)
}

/**
 * A padded list group container with rounded corners that handles item positioning automatically.
 *
 * @param modifier Modifier to be applied to the container
 * @param title Optional title displayed above the group
 * @param content The composable content to be displayed inside the group
 */
@Composable
fun PaddedListGroup(
	modifier: Modifier = Modifier,
	content: @Composable ColumnScope.() -> Unit
) {
	Column(modifier = modifier.padding(8.dp)) {
		Column(
			verticalArrangement = Arrangement.spacedBy(2.dp)
		) {
			content()
		}
	}
}

/**
 * A padded list item with automatic corner rounding based on position.
 *
 * @param title The main title text
 * @param subtitle Optional subtitle text
 * @param icon Leading icon
 * @param onClick Click handler for the item
 * @param position The position of this item in the list (affects corner rounding)
 */
@Composable
fun PaddedListItem(
	title: String,
	subtitle: String? = null,
	icon: ImageVector,
	onClick: () -> Unit,
	position: PaddedListItemPosition = PaddedListItemPosition.Middle,
) {
	val shape = shapeForPosition(position)

	SettingsContainer(
		background = MaterialTheme.colorScheme.surfaceContainer,
		contentColor = MaterialTheme.colorScheme.onSurface,
		shape = shape,
		modifier = Modifier.clip(shape),
	) {
		Row(
			modifier = Modifier
				.clickable { onClick() }
				.padding(12.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Icon(imageVector = icon, contentDescription = null)
			Spacer(modifier = Modifier.width(8.dp))
			Column {
				Text(
					text = title,
					style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
				)
				subtitle?.let {
					Text(
						text = it,
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}
		}
	}
}

/**
 * A customizable padded list item with automatic corner rounding.
 *
 * @param onClick Click handler for the item
 * @param position The position of this item in the list (affects corner rounding)
 * @param background The background color for the item
 * @param contentColor The content color for the item
 * @param content The custom content layout
 */
@Composable
fun CustomPaddedListItem(
	onClick: () -> Unit,
	position: PaddedListItemPosition = PaddedListItemPosition.Middle,
	modifier: Modifier = Modifier,
	background: Color = MaterialTheme.colorScheme.surfaceContainer,
	contentColor: Color = MaterialTheme.colorScheme.onSurface,
	content: @Composable RowScope.() -> Unit
) {
	val shape = shapeForPosition(position)

	SettingsContainer(
		background = background,
		contentColor = contentColor,
		shape = shape,
		modifier = modifier.clip(shape),
	) {
		Row(
			modifier = Modifier
				.clickable { onClick() }
				.padding(horizontal = 16.dp, vertical = 12.dp),
			verticalAlignment = Alignment.CenterVertically,
			content = content
		)
	}
}

/**
 * A customizable expandable padded list item with automatic corner rounding.
 *
 * @param isExpanded Whether the item is currently expanded
 * @param onToggleExpanded Callback when the item is clicked to toggle expansion
 * @param position The position of this item in the list (affects corner rounding)
 * @param defaultContent The content to show when collapsed
 * @param expandedContent The content to show when expanded
 */
@Composable
fun CustomPaddedExpandableItem(
	isExpanded: Boolean,
	onToggleExpanded: () -> Unit,
	position: PaddedListItemPosition = PaddedListItemPosition.Middle,
	modifier: Modifier = Modifier,
	defaultContent: @Composable RowScope.() -> Unit,
	expandedContent: @Composable ColumnScope.() -> Unit
) {
	val shape = when (position) {
		PaddedListItemPosition.First -> RoundedCornerShape(
			topStart = 16.dp,
			topEnd = 16.dp,
		)

		PaddedListItemPosition.Last -> RoundedCornerShape(
			bottomStart = 16.dp,
			bottomEnd = 16.dp,
		)

		PaddedListItemPosition.Single -> RoundedCornerShape(16.dp)
		PaddedListItemPosition.Middle -> RoundedCornerShape(8.dp)
	}

	SettingsContainer(
		background = MaterialTheme.colorScheme.surfaceContainer,
		contentColor = MaterialTheme.colorScheme.onSurface,
		shape = shape,
		modifier = modifier.clip(shape),
	) {
		Column {
			Row(
				modifier = Modifier
					.clickable { onToggleExpanded() }
					.padding(16.dp),
				verticalAlignment = Alignment.CenterVertically,
				content = defaultContent
			)

			AnimatedVisibility(
				visible = isExpanded,
				enter = expandVertically() + fadeIn(),
				exit = shrinkVertically() + fadeOut()
			) {
				Column(
					modifier = Modifier.padding(10.dp),
					content = expandedContent
				)
			}
		}
	}
}

/**
 * Enum to define the position of an item in a padded list for proper corner rounding.
 */
enum class PaddedListItemPosition {
	First, Middle, Last, Single
}

data class SettingItem(
	val title: String,
	val subtitle: String? = null,
	val icon: ImageVector,
	val onClick: () -> Unit
)

@Composable
private fun SettingsContainer(
	background: Color,
	contentColor: Color,
	shape: RoundedCornerShape,
	modifier: Modifier = Modifier,
	content: @Composable () -> Unit
) {
	CompositionLocalProvider(LocalContentColor provides contentColor) {
		Box(
			modifier = modifier
				.fillMaxWidth()
				.background(background, shape)
		) {
			content()
		}
	}
}

@Composable
private fun SettingsDivider(modifier: Modifier = Modifier) {
	Box(
		modifier = modifier
			.fillMaxWidth()
			.padding(vertical = 4.dp)
			.background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
	)
}

private fun shapeForPosition(position: PaddedListItemPosition): RoundedCornerShape {
	return when (position) {
		PaddedListItemPosition.First -> RoundedCornerShape(
			topStart = 16.dp,
			topEnd = 16.dp,
			bottomStart = 4.dp,
			bottomEnd = 4.dp,
		)

		PaddedListItemPosition.Last -> RoundedCornerShape(
			bottomStart = 16.dp,
			bottomEnd = 16.dp,
			topStart = 4.dp,
			topEnd = 4.dp,
		)

		PaddedListItemPosition.Single -> RoundedCornerShape(16.dp)
		PaddedListItemPosition.Middle -> RoundedCornerShape(4.dp)
	}
}

@Preview(device = "id:wearos_small_round", showBackground = true, showSystemUi = true)
@Composable
private fun FlexibleSettingsGroupPreview() {
	MinusTheme {
		TransformingLazyColumn {
			item {
				FlexibleListGroup(title = "Standard Items") {
					ListItem(
						title = "Setting 1",
						subtitle = "Description",
						onClick = {},
						showDivider = true
					)
					ListItem(title = "Setting 2", onClick = {})
				}
			}

			item {
				FlexibleListGroup(title = "Custom Content") {
					CustomSettingsItem(onClick = {}) {
						Icon(
							imageVector = Icons.Default.Settings,
							contentDescription = null,
							tint = MaterialTheme.colorScheme.primary
						)
						Spacer(modifier = Modifier.width(16.dp))
						Column(modifier = Modifier.weight(1f)) {
							Text(
								text = "Custom Item with Icon",
								style = MaterialTheme.typography.titleMedium
							)
							Text(
								text = "This shows custom layout",
								style = MaterialTheme.typography.bodySmall
							)
						}
						Text(
							text = "Value",
							style = MaterialTheme.typography.labelMedium,
							color = MaterialTheme.colorScheme.primary
						)
					}

					Box(
						modifier = Modifier
							.fillMaxWidth()
							.padding(16.dp)
					) {
						Text(
							text = "Any composable content can go here.",
							style = MaterialTheme.typography.bodyMedium,
							textAlign = TextAlign.Center,
							modifier = Modifier.fillMaxWidth()
						)
					}
				}
			}
		}
	}
}

@Preview(device = "id:wearos_small_round", showBackground = true, showSystemUi = true)
@Composable
private fun PaddedListGroupPreview() {
	MinusTheme {
		TransformingLazyColumn {
			item {
				PaddedListGroup {
					PaddedListItem(
						title = "Google",
						subtitle = "Services",
						icon = Icons.Default.Settings,
						onClick = {},
						position = PaddedListItemPosition.First
					)

					PaddedListItem(
						title = "Item",
						subtitle = "Text",
						icon = Icons.Default.Star,
						onClick = {},
						position = PaddedListItemPosition.Last
					)
				}
			}
		}
	}
}
