package com.serranoie.app.minus.presentation.ui.theme.component.numpad

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorButton
import com.serranoie.app.minus.presentation.ui.theme.colorOnButton


enum class NumpadButtonType { DEFAULT, PRIMARY, SECONDARY, TERTIARY, DELETE, OPERATOR }

@Composable
fun NumpadButton(
	modifier: Modifier = Modifier,
	type: NumpadButtonType = NumpadButtonType.DEFAULT,
	text: String? = null,
	icon: ImageVector? = null,
	onClick: () -> Unit = {},
	onLongClick: () -> Unit = {},
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isPressed = interactionSource.collectIsPressedAsState()
	val radius = animateDpAsState(targetValue = if (isPressed.value) 20.dp else 999.dp)

	val color = when (type) {
		 NumpadButtonType.DEFAULT -> colorButton
		 NumpadButtonType.PRIMARY -> MaterialTheme.colorScheme.primaryContainer
		 NumpadButtonType.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer
		 NumpadButtonType.TERTIARY -> MaterialTheme.colorScheme.tertiaryContainer
		 NumpadButtonType.DELETE -> MaterialTheme.colorScheme.errorContainer
		 NumpadButtonType.OPERATOR -> MaterialTheme.colorScheme.secondaryContainer
	}

	val contentColor = when (type) {
		 NumpadButtonType.DEFAULT -> colorOnButton
		 NumpadButtonType.PRIMARY -> MaterialTheme.colorScheme.onPrimaryContainer
		 NumpadButtonType.SECONDARY -> MaterialTheme.colorScheme.onSecondaryContainer
		 NumpadButtonType.TERTIARY -> MaterialTheme.colorScheme.onTertiaryContainer
		 NumpadButtonType.DELETE -> MaterialTheme.colorScheme.onErrorContainer
		 NumpadButtonType.OPERATOR -> MaterialTheme.colorScheme.secondary
	}

	Surface(
		tonalElevation = 10.dp,
		modifier = modifier
			.fillMaxSize()
			.clip(RoundedCornerShape(radius.value))
	) {
		BoxWithConstraints(
			modifier = Modifier
				.background(color = color)
				.fillMaxSize()
				.clip(RoundedCornerShape(radius.value))
				.combinedClickable(
					interactionSource = interactionSource,
					indication = ripple(),
					onClick = { onClick.invoke() },
					onLongClick = { onLongClick.invoke() },
				),
			contentAlignment = Alignment.Center
		) {
			if (text !== null) {
				Text(
					text = text,
					color = contentColor,
					style = MaterialTheme.typography.displaySmall.copy(
						fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
						fontSize = 42.sp,
					),
					maxLines = 1,
				)
			}
			if (icon != null) {
				val iconSize = min(maxWidth * 0.34f, 48.dp)
				Icon(
					imageVector = icon,
					tint = contentColor,
					modifier = Modifier.size(iconSize),
					contentDescription = null,
				)
			}
		}
	}
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview
@Composable
private fun NumpadButtonPreviews() {
	MinusTheme{
		Column {
			Row {
				NumpadButton(type = NumpadButtonType.DEFAULT, text = "1")
				NumpadButton(type = NumpadButtonType.SECONDARY, text = "2")
				NumpadButton(type = NumpadButtonType.TERTIARY, text = "3")
			}
			Spacer(Modifier.height(8.dp))

			Row {
				NumpadButton(type = NumpadButtonType.DEFAULT, icon = Icons.Default.Check)
				NumpadButton(type = NumpadButtonType.SECONDARY, icon = Icons.AutoMirrored.Filled.ArrowBack)
				NumpadButton(type = NumpadButtonType.TERTIARY, icon = Icons.Default.Close)
			}
		}
	}
}