package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun DescriptionButton(
	modifier: Modifier = Modifier,
	title: @Composable () -> Unit,
	description: @Composable (() -> Unit)? = null,
	secondDescription: @Composable (() -> Unit)? = null,
	icon: ImageVector = Icons.Rounded.KeyboardArrowRight,
	onClick: () -> Unit,
	contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
	colors: CardColors = CardDefaults.cardColors(),
	elevation: CardElevation = CardDefaults.cardElevation(),
){
	Card(
		onClick = {
			android.util.Log.d("DescriptionButton", "Card clicked - invoking onClick")
			onClick()
		},
		modifier = modifier.fillMaxWidth(),
		shape = MaterialTheme.shapes.extraLarge,
		colors = colors,
		elevation = elevation,
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Column(
				Modifier
					.padding(contentPadding)
					.weight(weight = 1F, fill = true)
			) {
				ProvideTextStyle(MaterialTheme.typography.titleMedium) {
					title()
				}
				if (description !== null) {
					Spacer(Modifier.height(4.dp))
					ProvideTextStyle(MaterialTheme.typography.bodySmall) {
						description()
					}
				}
				if (secondDescription !== null) {
					Spacer(Modifier.height(4.dp))
					ProvideTextStyle(MaterialTheme.typography.bodySmall) {
						secondDescription()
					}
				}
			}
			Icon(
				modifier = Modifier
					.width(48.dp)
					.padding(end = 8.dp),
				imageVector = icon,
				contentDescription = null,
			)
		}
	}
}


@Preview(name = "DescriptionButton")
@Composable
private fun PreviewDescriptionButton() {
	DescriptionButton(
		title = { Text("Title") },
		description = { Text("Description of button itself (?") },
		onClick = {}
	)
}