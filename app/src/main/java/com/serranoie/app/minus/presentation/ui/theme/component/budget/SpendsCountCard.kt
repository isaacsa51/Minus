package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme

@Composable
fun SpendsCountCard(
	modifier: Modifier = Modifier,
	count: Int,
	onClick: () -> Unit = {},
) {
	Card(
		modifier = modifier.height(IntrinsicSize.Min),
		shape = CircleShape,
		onClick = onClick
	) {
		val textColor = LocalContentColor.current

		Row(
			modifier = Modifier
				.padding(24.dp, 0.dp)
				.fillMaxWidth(),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Spacer(modifier = Modifier.width(24.dp))

			Column(
				Modifier
					.padding(14.dp, 8.dp)
					.weight(1f),
				verticalArrangement = Arrangement.Center,
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Text(
					text = count.toString(),
					style = MaterialTheme.typography.displayMedium,
					fontSize = MaterialTheme.typography.titleLarge.fontSize,

					overflow = TextOverflow.Ellipsis,
					softWrap = true,
					textAlign = TextAlign.Center,
				)
				Text(
					text = "Total gastado",
					style = MaterialTheme.typography.labelMedium,
					color = textColor.copy(alpha = 0.6f),
					softWrap = false,
					textAlign = TextAlign.Center,
				)
				Spacer(modifier = Modifier.height(4.dp))
			}

			Icon(
				imageVector = Icons.Rounded.ArrowForward,
				contentDescription = null,
			)
		}
	}
}


@Preview(name = "SpendsCountCard",
	device = "spec:width=800px,height=400px"
)
@Composable
private fun PreviewSpendsCountCard() {
	MinusTheme {
		Column {
			SpendsCountCard(
				count = 10,
			)
		}
	}
}