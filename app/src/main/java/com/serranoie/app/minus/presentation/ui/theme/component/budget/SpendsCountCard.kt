package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.util.combineColors

@Composable
fun SpendsCountCard(
    modifier: Modifier = Modifier,
    count: Int,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = modifier.heightIn(min = 56.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = combineColors(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant,
                t = 0.3f,
            ),
        ),
        onClick = onClick
    ) {
        val textColor = LocalContentColor.current

        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp, 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = count.toString(),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = true,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.total_expenses),
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(),
                    style = MaterialTheme.typography.labelMediumCondensed,
                    color = textColor.copy(alpha = 0.6f),
                    softWrap = true,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Icon(
                modifier = Modifier.align(Alignment.CenterEnd),
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
            )
        }
    }
}

@Preview(
    name = "SpendsCountCard",
    device = "spec:width=500px,height=300px"
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
