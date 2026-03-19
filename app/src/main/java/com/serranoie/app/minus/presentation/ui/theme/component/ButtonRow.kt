package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme


@Composable
fun ButtonRow(
	modifier: Modifier = Modifier,
	icon: ImageVector? = null,
	iconInset: Boolean = true,
	endIcon: ImageVector? = null,
	text: String,
	wrapMainText: Boolean = false,
	description: String? = null,
	denseDescriptionOffset: Boolean = true,
	onClick: () -> Unit,
	endContent: @Composable (() -> Unit)? = null,
	endCaption: String? = null,
) {
	val interactionSource = remember { MutableInteractionSource() }

	TextRow(
		modifier
			.clickable(
				interactionSource = interactionSource,
				indication = ripple()
			) { onClick.invoke() },
		icon = icon,
		iconInset = iconInset,
		endIcon = endIcon,
		wrapMainText = wrapMainText,
		text = text,
		description = description,
		denseDescriptionOffset = denseDescriptionOffset,
		endContent = endContent,
		endCaption = endCaption,
	)
}



@Preview(name = "ButtonRow")
@Composable
private fun PreviewButtonRow() {
	MinusTheme {
		ButtonRow(
			icon = Icons.Rounded.Home,
			text = "Just another row",
			onClick = { }
		)
	}
}