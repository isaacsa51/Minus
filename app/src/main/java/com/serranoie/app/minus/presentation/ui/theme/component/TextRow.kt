package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.combineColors

@Composable
fun TextRow(
	modifier: Modifier = Modifier,
	icon: ImageVector? = null,
	iconTint: Color = contentColorFor(
		combineColors(
			MaterialTheme.colorScheme.secondaryContainer,
			MaterialTheme.colorScheme.surfaceVariant,
			angle = 0.3F,
		)
	),
	endIcon: ImageVector? = null,
	endContent: @Composable (() -> Unit)? = null,
	endCaption: String? = null,
	iconInset: Boolean = true,
	text: String,
	wrapMainText: Boolean = false,
	description: String? = null,
	denseDescriptionOffset: Boolean = true,
	textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
	descriptionTextStyle: TextStyle = MaterialTheme.typography.bodySmall
		.copy(color = LocalContentColor.current.copy(alpha = 0.6f)),
) {
	Column(modifier) {
		Box(Modifier.padding(top = 16.dp)) {
			Row(
				Modifier
					.fillMaxWidth()
					.heightIn(24.dp)
					.padding(horizontal = 24.dp)
					.height(IntrinsicSize.Min),
				verticalAlignment = Alignment.Top
			) {
				Box(
					Modifier
						.padding(
							start = if (!iconInset && icon === null) 8.dp else (24 + 16).dp,
							top = 0.dp,
							bottom = if (description !== null) 0.dp else 16.dp,
						)
						.heightIn(24.dp)
						.widthIn(min = 100.dp)
						.weight(1f),
					contentAlignment = Alignment.CenterStart
				) {
					Text(
						text = text,
						style = textStyle,
						softWrap = wrapMainText,
						overflow = if (wrapMainText) TextOverflow.Visible else TextOverflow.Ellipsis,
					)
				}

				if (endCaption !== null) {
					Text(
						modifier = Modifier.widthIn(max = 200.dp),
						text = endCaption,
						softWrap = false,
						overflow = TextOverflow.Ellipsis,
						style = MaterialTheme.typography.bodyLarge,
						color = LocalContentColor.current.copy(alpha = 0.6f),
					)
				}

				if (endContent !== null || endIcon !== null) {
					Box(
						modifier = Modifier.fillMaxHeight(),
						contentAlignment = Alignment.TopEnd,
					) {
						Row(
							Modifier
								.height(24.dp)
								.padding(start = 16.dp),
							verticalAlignment = Alignment.CenterVertically,
						) {
							if (endContent !== null) {
								endContent()
								if (endIcon == null) {
									Spacer(modifier = Modifier.width(8.dp))
								}
							}
							if (endIcon !== null) {
								Spacer(modifier = Modifier.width(16.dp))
								Icon(
									imageVector = endIcon,
									contentDescription = null
								)
							}
						}
					}
				}
			}

			if (icon !== null) {
				Box(
					Modifier
						.height(24.dp)
						.width(64.dp)
						.padding(start = 24.dp, end = 16.dp),
					contentAlignment = Alignment.CenterStart
				) {
					Icon(
						imageVector = icon,
						tint = iconTint,
						contentDescription = null
					)
				}
			}
		}
		if (description !== null) {
			Text(
				text = description,
				style = descriptionTextStyle,
				softWrap = true,
				modifier = Modifier
					.padding(
						start = if (!iconInset && icon === null) 32.dp else (24 + 24 + 16).dp,
						top = if (denseDescriptionOffset) 0.dp else 8.dp,
						end = 24.dp,
						bottom = 24.dp,
					)
			)
		}
	}
}

@Preview
@Composable
private fun PreviewWithDescription() {
	MinusTheme {
		TextRow(
			text = "Text row",
			description = "Description of text row",
		)
	}
}

@Preview
@Composable
private fun PreviewWithDescriptionIconInset() {
	MinusTheme {
		TextRow(
			text = "Text row",
			description = "Description of text row",
			iconInset = false,
		)
	}
}

@Preview()
@Composable
private fun PreviewTWithIcon() {
	MinusTheme {
		TextRow(
			icon = Icons.Rounded.Home,
			text = "Text row",
		)
	}
}

@Preview()
@Composable
private fun PreviewWithDescriptionWithIcon() {
	MinusTheme {
		TextRow(
			icon = Icons.Rounded.Home,
			text = "Text row",
			description = "Description of text row",
		)
	}
}

@Preview()
@Composable
private fun PreviewTWithIconWithEndContent() {
	MinusTheme {
		TextRow(
			icon = Icons.Rounded.Home,
			text = "Text row",
			endCaption = "Very looooooooooooong end content as text"
		)
	}
}

@Preview()
@Composable
private fun PreviewWithIconWithEndContentWithLongTitle() {
	MinusTheme {
		TextRow(
			icon = Icons.Rounded.Home,
			text = "Text row loooooooooooooooooooooooooooooooong",
			endCaption = "Very looooooooooooong end content as text"
		)
	}
}

@Preview()
@Composable
private fun PreviewWithIcons() {
	MinusTheme {
		TextRow(
			icon = Icons.Rounded.Home,
			endIcon = Icons.Rounded.Edit,
			text = "Text row loooooooooooooooooooooooooooooooong",
		)
	}
}

@Preview()
@Composable
private fun PreviewWithIconsWithChip() {
	MinusTheme {
		TextRow(
			icon = Icons.Rounded.Home,
			endContent = {
				SuggestionChip(
					label = { Text(text = "Suggestion") },
					onClick = { /*TODO*/ },
				)
			},
			text = "Text row loooooooooooooooooooooooooooooooong",
		)
	}
}

@Preview()
@Composable
private fun PreviewWithIconsWithChipAndEndIcon() {
	MinusTheme {
		TextRow(
			icon = Icons.Rounded.Home,
			endIcon = Icons.Rounded.Close,
			endContent = {
				SuggestionChip(
					label = { Text(text = "Suggestion") },
					onClick = { /*TODO*/ },
				)
			},
			text = "Text row loooooooooooooooooooooooooooooooong",
		)
	}
}

@Preview()
@Composable
private fun PreviewWithIconsWithChipWithWrapText() {
	MinusTheme {
		TextRow(
			icon = Icons.Rounded.Home,
			endIcon = Icons.Rounded.Edit,
			endContent = {
				SuggestionChip(
					label = { Text(text = "Suggestion") },
					onClick = { /*TODO*/ },
				)
			},
			wrapMainText = true,
			text = "Text row loooooooooooooooooooooooooooooooong",
		)
	}
}

@Preview(name = "With icon, end content, end icon and description")
@Composable
private fun PreviewWithIconsWithChipWithDescriptionWithEndContentAndEnIcon() {
	MinusTheme {
		TextRow(
			icon = Icons.Rounded.Home,
			endIcon = Icons.Rounded.Edit,
			endContent = {
				SuggestionChip(
					label = { Text(text = "Suggestion") },
					onClick = { /*TODO*/ },
				)
			},
			wrapMainText = true,
			text = "Text row loooooooooooooooooooooooooooooooong",
			description = "Description looooooooooooooooooooooooooooooooooooong text",
		)
	}
}


@Preview(name = "With icon, end content, end icon and description")
@Composable
private fun PreviewWithIconsWithChipWithDescriptionWithEndContent() {
	MinusTheme {
		TextRow(
			icon = Icons.Rounded.Home,
			endContent = {
				SuggestionChip(
					label = { Text(text = "Suggestion") },
					onClick = { /*TODO*/ },
				)
			},
			wrapMainText = true,
			text = "Text row",
			description = "Description loooooooooooooooooooooong text",
		)
	}
}
