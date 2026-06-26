package com.serranoie.app.minus.presentation.ui.changelog.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.changelog.ChangelogItem
import com.serranoie.app.minus.domain.model.changelog.ReleaseType
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme

@Composable
internal fun ChangelogBugFixItemCard(
    item: ChangelogItem,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.outline,
            disabledContentColor = MaterialTheme.colorScheme.outline,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = item.title
                    .replace(CHANGELOG_BUGFIX_PR_REGEX, "")
                    .trim(),
                style = MaterialTheme.typography.bodySmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private val CHANGELOG_BUGFIX_PR_REGEX = Regex("""\s*\(#\d+\)\s*$""")

@Preview(showBackground = true, name = "Bug fix item - check icon")
@Composable
private fun ChangelogBugFixItemCardPreview() {
    MinusTheme {
        ChangelogBugFixItemCard(
            modifier = Modifier.fillMaxWidth(),
            item = ChangelogItem(
                title = "Multi-currency transfer date conversion",
                type = ReleaseType.BUG_FIX,
            ),
        )
    }
}

@Preview(showBackground = true, name = "Bug fix item - title with PR ref (stripped)")
@Composable
private fun ChangelogBugFixItemCardWithPrRefPreview() {
    MinusTheme {
        ChangelogBugFixItemCard(
            modifier = Modifier.fillMaxWidth(),
            item = ChangelogItem(
                title = "Implement correct usage of History actions (#40)",
                type = ReleaseType.BUG_FIX,
            ),
        )
    }
}

@Preview(
    name = "Bug fix item - DARK",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
            or android.content.res.Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun ChangelogBugFixItemCardDarkPreview() {
    MinusTheme {
        ChangelogBugFixItemCard(
            modifier = Modifier.fillMaxWidth(),
            item = ChangelogItem(
                title = "Multi-currency transfer date conversion",
                type = ReleaseType.BUG_FIX,
            ),
        )
    }
}