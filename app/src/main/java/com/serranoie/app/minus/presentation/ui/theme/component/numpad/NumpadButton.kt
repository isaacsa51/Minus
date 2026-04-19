package com.serranoie.app.minus.presentation.ui.theme.component.numpad

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
	val isPressed by interactionSource.collectIsPressedAsState()
	
	val radius by animateDpAsState(
		targetValue = if (isPressed) 24.dp else 100.dp,
		animationSpec = if (isPressed) {
			tween(durationMillis = 140, easing = LinearOutSlowInEasing)
		} else {
			tween(durationMillis = 620, easing = LinearEasing)
		},
		label = "ButtonRadius"
	)

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
			.clip(RoundedCornerShape(radius))
	) {
		Box(
			modifier = Modifier
				.background(color = color)
				.fillMaxSize()
				.combinedClickable(
					interactionSource = interactionSource,
					indication = ripple(),
					onClick = onClick,
					onLongClick = onLongClick,
				),
			contentAlignment = Alignment.Center
		) {
			if (text != null) {
				Text(
					text = text,
					color = contentColor,
					style = MaterialTheme.typography.displaySmall.copy(
						fontWeight = FontWeight.Bold,
						fontSize = 44.sp
					),
					maxLines = 1,
				)
			}
			if (icon != null) {
				Icon(
					imageVector = icon,
					tint = contentColor,
					modifier = Modifier.size(40.dp),
					contentDescription = "Editor action",
				)
			}
		}
	}
}

@Preview
@Composable
private fun NumpadButtonPreviews() {
	MinusTheme {
		Column {
			Row {
				NumpadButton(type = NumpadButtonType.DEFAULT, text = "1")
				NumpadButton(type = NumpadButtonType.SECONDARY, text = "2")
				NumpadButton(type = NumpadButtonType.TERTIARY, text = "3")
			}
			Spacer(Modifier.height(8.dp))

			Row {
				NumpadButton(type = NumpadButtonType.DEFAULT, icon = Icons.Default.Check)
				NumpadButton(
					type = NumpadButtonType.SECONDARY, icon = Icons.AutoMirrored.Filled.ArrowBack
				)
				NumpadButton(type = NumpadButtonType.TERTIARY, icon = Icons.Default.Close)
			}
		}
	}
}
