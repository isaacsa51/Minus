package com.serranoie.app.minus.presentation.ui.theme.component.numpad

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorButton
import com.serranoie.app.minus.presentation.ui.theme.colorOnButton
import java.lang.Integer.MAX_VALUE
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min


enum class NumpadButtonType { DEFAULT, PRIMARY, SECONDARY, TERTIARY, DELETE }

@Composable
fun NumpadButton(
	modifier: Modifier = Modifier,
	type: NumpadButtonType = NumpadButtonType.DEFAULT,
	text: String? = null,
	icon: ImageVector? = null,
	onClick: () -> Unit = {},
	onLongClick: () -> Unit = {},
) {
	val localDensity = LocalDensity.current
	var minSize by remember { mutableStateOf(MAX_VALUE.dp) }
	var minSizeFloat by remember { mutableStateOf(MAX_VALUE.toFloat()) }
	val interactionSource = remember { MutableInteractionSource() }
	val isPressed = interactionSource.collectIsPressedAsState()
	val radius = animateDpAsState(targetValue = if (isPressed.value) 20.dp else minSize / 2)

	val color = when (type) {
		 NumpadButtonType.DEFAULT -> colorButton
		 NumpadButtonType.PRIMARY -> MaterialTheme.colorScheme.primaryContainer
		 NumpadButtonType.SECONDARY -> MaterialTheme.colorScheme.secondaryContainer
		 NumpadButtonType.TERTIARY -> MaterialTheme.colorScheme.tertiaryContainer
		 NumpadButtonType.DELETE -> MaterialTheme.colorScheme.errorContainer
	}

	val contentColor = when (type) {
		 NumpadButtonType.DEFAULT -> colorOnButton
		 NumpadButtonType.PRIMARY -> MaterialTheme.colorScheme.onPrimaryContainer
		 NumpadButtonType.SECONDARY -> MaterialTheme.colorScheme.onSecondaryContainer
		 NumpadButtonType.TERTIARY -> MaterialTheme.colorScheme.onTertiaryContainer
		 NumpadButtonType.DELETE -> MaterialTheme.colorScheme.onErrorContainer
	}

	Surface(
		tonalElevation = 10.dp,
		modifier = modifier
			.fillMaxSize()
			.onGloballyPositioned {
				minSize = with(localDensity) { min(it.size.height, it.size.width).toDp() }
				minSizeFloat = min(it.size.height, it.size.width).toFloat()
			}
			.clip(RoundedCornerShape(radius.value))
	) {
		Box(
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
				val fontSize = min(
					calcMaxFont(minSizeFloat),
					46.sp,
				)

				Text(
					text = text,
					color = contentColor,
					style = MaterialTheme.typography.displaySmall.copy(
						fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
					),
					fontSize = fontSize,
				)
			}
			if (icon != null) {
				Icon(
					imageVector = icon,
					tint = contentColor,
					modifier = Modifier.size(min(minSize * 0.34f, 154.dp)),
					contentDescription = null,
				)
			}
		}
	}
}

@Stable
fun min(a: TextUnit, b: TextUnit): TextUnit = min(a.value, b.value).sp

@Stable
fun max(a: TextUnit, b: TextUnit): TextUnit = max(a.value, b.value).sp

@Composable
fun calcMaxFont(
	height: Float,
	text: String = "SAMPLE 1234567890",
	style: TextStyle = MaterialTheme.typography.displayLarge,
): TextUnit {
	val measureFontSize = 100.sp

	val intrinsics = ParagraphIntrinsics(
		text = text,
		style = style.copy(fontSize = measureFontSize),
		density = LocalDensity.current,
		fontFamilyResolver = createFontFamilyResolver(LocalContext.current)
	)

	val paragraph = Paragraph(
		paragraphIntrinsics = intrinsics,
		constraints = Constraints(maxWidth = ceil(1000f).toInt()),
		maxLines = 1,
		overflow = TextOverflow.Clip
	)

	return with(LocalDensity.current) {
		((measureFontSize.toPx() / paragraph.firstBaseline) * height).toSp()
	}
}

@Composable
fun calcAdaptiveFont(
	height: Float,
	width: Float,
	minFontSize: TextUnit,
	maxFontSize: TextUnit,
	text: String = "SAMPLE 1234567890",
	style: TextStyle = MaterialTheme.typography.displayLarge,
): TextUnit {
	var measureFontSize = calcMaxFont(height = height, text = text, style = style)

	var intrinsics = ParagraphIntrinsics(
		text = text,
		style = style.copy(fontSize = measureFontSize),
		density = LocalDensity.current,
		fontFamilyResolver = createFontFamilyResolver(LocalContext.current)
	)

	while (intrinsics.maxIntrinsicWidth > width && measureFontSize > minFontSize) {
		measureFontSize *= 0.9f
		intrinsics = ParagraphIntrinsics(
			text = text,
			style = style.copy(fontSize = measureFontSize),
			density = LocalDensity.current,
			fontFamilyResolver = createFontFamilyResolver(LocalContext.current)
		)
	}

	return min(max(minFontSize, measureFontSize), maxFontSize)
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
				NumpadButton(type = NumpadButtonType.SECONDARY, icon = Icons.Default.ArrowBack)
				NumpadButton(type = NumpadButtonType.TERTIARY, icon = Icons.Default.Close)
			}
		}
	}
}