package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme

@Composable
fun StatCard(
	modifier: Modifier = Modifier,
	value: String,
	label: String,
	contentPadding: PaddingValues = PaddingValues(vertical = 16.dp, horizontal = 24.dp),
	colors: CardColors = CardDefaults.cardColors(
		containerColor = MaterialTheme.colorScheme.surfaceDim,
		contentColor = MaterialTheme.colorScheme.onSurfaceVariant
	),
	valueFontSize: TextUnit = MaterialTheme.typography.titleLarge.fontSize,
	valueFontStyle: TextStyle = MaterialTheme.typography.displayMedium,
	labelFontStyle: TextStyle = MaterialTheme.typography.labelMedium,
	content: @Composable ColumnScope.() -> Unit = {},
	backdropContent: @Composable () -> Unit = {},
) {
	Card(
		modifier = modifier,
		shape = MaterialTheme.shapes.extraLarge,
		colors = colors,
	) {
		val textColor = LocalContentColor.current

		Box(
			Modifier
				.fillMaxWidth()
				.fillMaxHeight()
		) {
			Box(
				Modifier
					.fillMaxHeight()
					.fillMaxWidth()
			) {
				backdropContent()
			}

			Column(
				Modifier
					.fillMaxWidth()
					.padding(contentPadding)
			) {
				Text(
					text = value,
					style = valueFontStyle,
					fontSize = valueFontSize,
					overflow = TextOverflow.Ellipsis,
					softWrap = false,
					lineHeight = TextUnit(0.2f, TextUnitType.Em)
				)
				Text(
					text = label,
					style = labelFontStyle,
					color = textColor.copy(alpha = 0.6f),
					overflow = TextOverflow.Ellipsis,
					softWrap = false,
				)
				Spacer(modifier = Modifier.height(4.dp))

				CompositionLocalProvider(
					LocalContentColor provides textColor,
				) {
					Column(
						//modifier = Modifier.fillMaxWidth(),
						content = content,
					)
				}
			}
		}
	}
}


@Preview
@Composable
private fun PreviewStatCard() {
	MinusTheme {
		StatCard(
			value = "Value",
			label = "Label"
		)
	}
}