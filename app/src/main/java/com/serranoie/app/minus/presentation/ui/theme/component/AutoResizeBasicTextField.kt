package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.font.calcAdaptiveFont

/**
 * Auto-resizing text field that scales font size based on content and available space.
 * Uses stable sizing - measures text independently to avoid feedback loops that cause shaking.
 */
@Composable
fun AutoResizeBasicTextField(
	value: String,
	onValueChange: (String) -> Unit,
	modifier: Modifier = Modifier,
	textStyle: TextStyle = MaterialTheme.typography.displayLarge,
	minFontSize: TextUnit = 20.sp,
	maxFontSize: TextUnit = 76.sp,
	cursorBrush: Brush = SolidColor(MaterialTheme.colorScheme.primary),
	singleLine: Boolean = true,
	enabled: Boolean = true,
	readOnly: Boolean = false,
	visualTransformation: VisualTransformation = VisualTransformation.None,
	keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
	keyboardActions: KeyboardActions = KeyboardActions.Default,
	decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit = { innerTextField ->
		innerTextField()
	},
	containerSize: IntSize = IntSize.Zero,
	annotatedValue: AnnotatedString? = null,
) {
	val transformedValue = remember(value, visualTransformation, annotatedValue) {
		val base = annotatedValue ?: AnnotatedString(value)
		visualTransformation.filter(base).text
	}

	val adaptiveFontSize = if (containerSize.width > 0 && transformedValue.text.isNotEmpty()) {
		calcAdaptiveFont(
			height = containerSize.height.toFloat(),
			width = containerSize.width.toFloat(),
			minFontSize = minFontSize,
			maxFontSize = maxFontSize,
			text = transformedValue.text,
			style = textStyle,
		)
	} else {
		maxFontSize
	}

	val resolvedTextStyle = textStyle.copy(
		fontSize = adaptiveFontSize,
		lineHeight = adaptiveFontSize,
		platformStyle = PlatformTextStyle(includeFontPadding = false)
	)

	BasicTextField(
		value = value,
		onValueChange = onValueChange,
		modifier = modifier,
		textStyle = resolvedTextStyle,
		cursorBrush = cursorBrush,
		singleLine = singleLine,
		enabled = enabled,
		readOnly = readOnly,
		visualTransformation = if (annotatedValue != null) {
			VisualTransformation { original ->
				val offsetMapping = object : OffsetMapping {
					override fun originalToTransformed(offset: Int): Int {
						val prefixLen = annotatedValue.length - original.length
						return (offset + prefixLen).coerceIn(0, annotatedValue.length)
					}

					override fun transformedToOriginal(offset: Int): Int {
						val prefixLen = annotatedValue.length - original.length
						return (offset - prefixLen).coerceIn(0, original.length)
					}
				}
				TransformedText(annotatedValue, offsetMapping)
			}
		} else {
			visualTransformation
		},
		keyboardOptions = keyboardOptions,
		keyboardActions = keyboardActions,
		decorationBox = decorationBox,
	)
}

@Preview(showBackground = true)
@Composable
private fun AutoResizeBasicTextFieldPreview() {
	MinusTheme {
		var value by remember { mutableStateOf("123456789012345") }

		Box(modifier = Modifier.fillMaxWidth()) {
			AutoResizeBasicTextField(
				value = value,
				onValueChange = { value = it },
				textStyle = MaterialTheme.typography.displayLarge,
			)
		}
	}
}
