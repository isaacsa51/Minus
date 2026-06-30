package com.serranoie.app.minus.presentation.ui.changelog.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
internal fun ChangelogItemCard(
    item: ChangelogItem,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            ChangelogItemTitleWithLinks(
                text = item.title,
                style = MaterialTheme.typography.bodyMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
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
private fun ChangelogItemTitleWithLinks(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val linkColor = Color(0xFF247ED5)
    val annotated = remember(text, linkColor) {
        buildChangelogTitleAnnotatedString(text, context, linkColor)
    }

    Text(
        text = annotated,
        style = style,
        color = color,
        modifier = modifier,
    )
}

private fun buildChangelogTitleAnnotatedString(
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

@Preview(showBackground = true, name = "Item card - FEATURE title only")
@Composable
private fun ChangelogItemCardFeaturePreview() {
    MinusTheme {
        ChangelogItemCard(
            modifier = Modifier.fillMaxWidth(),
            item = ChangelogItem(
                title = "Changed donut chart view with bigger graph (#46)",
                type = ReleaseType.FEATURE,
            ),
        )
    }
}

@Preview(showBackground = true, name = "Item card - IMPROVEMENT title only")
@Composable
private fun ChangelogItemCardImprovementPreview() {
    MinusTheme {
        ChangelogItemCard(
            modifier = Modifier.fillMaxWidth(),
            item = ChangelogItem(
                title = "Launch Optimization",
                type = ReleaseType.IMPROVEMENT,
            ),
        )
    }
}

@Preview(
    name = "Item card - title with PR link - DARK",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Preview(showBackground = true, name = "Item card - title with PR link")
@Composable
private fun ChangelogItemCardWithPrLinkPreview() {
    MinusTheme {
        ChangelogItemCard(
            modifier = Modifier.fillMaxWidth(),
            item = ChangelogItem(
                title = "Added badge and inf on settings about mode (#41)",
                type = ReleaseType.FEATURE,
            ),
        )
    }
}