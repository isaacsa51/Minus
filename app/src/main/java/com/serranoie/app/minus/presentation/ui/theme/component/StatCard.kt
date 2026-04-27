package com.serranoie.app.minus.presentation.ui.theme.component

import android.content.res.Configuration
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.budget.Cross
import com.serranoie.app.minus.presentation.ui.theme.displayMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed

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
	valueFontSize: TextUnit = MaterialTheme.typography.titleLargeEmphasized.fontSize,
	valueFontStyle: TextStyle = MaterialTheme.typography.displayMediumCondensed.copy(fontWeight = FontWeight.Light),
	crossedValue: String? = null,
	crossedValueColor: Color? = null,
	valueOffsetWhenCrossedY: Int = -4,
	labelFontStyle: TextStyle = MaterialTheme.typography.labelMediumCondensed,
	horizontalAlignment: Alignment.Horizontal = Alignment.Start,
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
					.align(Alignment.Center)
					.fillMaxWidth()
					.padding(contentPadding),
				horizontalAlignment = horizontalAlignment
			) {
				Box(modifier = Modifier.fillMaxWidth()) {
					val valueAlignment = when (horizontalAlignment) {
						Alignment.Start -> Alignment.CenterStart
						Alignment.CenterHorizontally -> Alignment.Center
						Alignment.End -> Alignment.CenterEnd
						else -> Alignment.CenterStart
					}

					if (crossedValue != null) {
						Cross(
							modifier = Modifier.align(valueAlignment)
						) {
							Text(
							text = crossedValue,
							style = valueFontStyle.copy(fontWeight = FontWeight.Light),
							fontSize = valueFontSize,
							color = crossedValueColor ?: textColor.copy(alpha = 0.18f),
								overflow = TextOverflow.Ellipsis,
								softWrap = false,
								lineHeight = TextUnit(0.2f, TextUnitType.Em),
							)
						}
					}
					Text(
						text = value,
						modifier = Modifier
							.fillMaxWidth()
							.then(
								if (crossedValue != null) {
									Modifier
										.offset(y = valueOffsetWhenCrossedY.dp)
										.rotate(2f)
								} else Modifier
							),
						style = valueFontStyle,
						fontSize = valueFontSize,
						overflow = TextOverflow.Ellipsis,
						softWrap = false,
						lineHeight = TextUnit(0.2f, TextUnitType.Em),
						textAlign = when (horizontalAlignment) {
							Alignment.Start -> TextAlign.Start
							Alignment.CenterHorizontally -> TextAlign.Center
							Alignment.End -> TextAlign.End
							else -> TextAlign.Unspecified
						}
					)
				}
				Text(
					text = label,
					modifier = Modifier.fillMaxWidth().basicMarquee(),
					style = labelFontStyle,
					color = textColor.copy(alpha = 0.6f),
					overflow = TextOverflow.Ellipsis,
					softWrap = false,
					textAlign = when (horizontalAlignment) {
						Alignment.Start -> TextAlign.Start
						Alignment.CenterHorizontally -> TextAlign.Center
						Alignment.End -> TextAlign.End
						else -> TextAlign.Unspecified
					}
				)
				Spacer(modifier = Modifier.height(4.dp))

				CompositionLocalProvider(
					LocalContentColor provides textColor,
				) {
					Column(
						modifier = Modifier.fillMaxWidth(),
						horizontalAlignment = horizontalAlignment,
						content = content,
					)
				}
			}
		}
	}
}


@Preview(device = "spec:width=800px,height=350px,dpi=480")
@Composable
private fun PreviewStatCard() {
	MinusTheme {
		StatCard(
			value = "Value",
			label = "Label"
		)
	}
}

@Preview(device = "spec:width=800px,height=350px,dpi=480",
	uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun PreviewStatCardWithCrossedValue() {
	MinusTheme {
		StatCard(
			value = "$1,533.50",
			crossedValue = "$1,333.50",
			label = "Presupuesto total"
		)
	}
}