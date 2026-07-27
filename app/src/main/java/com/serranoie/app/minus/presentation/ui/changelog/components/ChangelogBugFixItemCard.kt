package com.serranoie.app.minus.presentation.ui.changelog.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.changelog.ChangelogItem
import com.serranoie.app.minus.domain.model.changelog.ReleaseType
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.Utils

private const val CHANGELOG_PR_BASE_URL = "https://github.com/isaacsa51/Minus/pull/"
private val CHANGELOG_PR_REGEX = Regex("""\(#(\d+)\)""")

@Composable
internal fun ChangelogBugFixItemCard(
    item: ChangelogItem,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BugFixTitleWithLinks(
                text = item.title,
                style = MaterialTheme.typography.bodySmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp,
            )
        }
    }
}

@Composable
private fun BugFixTitleWithLinks(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val linkColor = Color(0xFF247ED5)
    val annotated = remember(text, linkColor) {
        buildBugFixTitleAnnotatedString(text, context, linkColor)
    }

    Text(
        text = annotated,
        style = style,
        color = color,
        modifier = modifier,
    )
}

private fun buildBugFixTitleAnnotatedString(
    text: String,
    context: android.content.Context,
    linkColor: Color,
): AnnotatedString = buildAnnotatedString {
    var lastIndex = 0
    CHANGELOG_PR_REGEX.findAll(text).forEach { match ->
        if (match.range.first > lastIndex) {
            append(text.substring(lastIndex, match.range.first))
        }

        val prNumber = match.groupValues[1]
        val url = CHANGELOG_PR_BASE_URL + prNumber

        withLink(
            LinkAnnotation.Url(
                url = url,
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                    ),
                ),
                linkInteractionListener = {
                    Utils.openWebLink(context, url)
                },
            ),
        ) {
            append(match.value)
        }

        lastIndex = match.range.last + 1
    }
    if (lastIndex < text.length) {
        append(text.substring(lastIndex))
    }
}

@Preview(showBackground = true, name = "Bug fix item - bullet only")
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

@Preview(showBackground = true, name = "Bug fix item - title with PR ref")
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
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES or android.content.res.Configuration.UI_MODE_TYPE_NORMAL,
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
