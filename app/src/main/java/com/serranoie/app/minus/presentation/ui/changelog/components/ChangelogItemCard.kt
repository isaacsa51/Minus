package com.serranoie.app.minus.presentation.ui.changelog.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
import com.serranoie.app.minus.presentation.ui.changelog.ChangelogMarkdownDescription
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.Utils

private const val CHANGELOG_PR_BASE_URL = "https://github.com/isaacsa51/Minus/pull/"
private val CHANGELOG_PR_REGEX = Regex("""\(#(\d+)\)""")

@Composable
internal fun ChangelogItemCard(
    item: ChangelogItem,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(true) }
    val hasDescription = !item.description.isNullOrBlank()

    Card(
        onClick = { isExpanded = !isExpanded },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.outline,
            disabledContentColor = MaterialTheme.colorScheme.outline,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            item.imageName?.takeIf { it.isNotBlank() }?.let { name ->
                ChangelogMedia(
                    imageName = name,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ChangelogItemTitleWithLinks(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmallEmphasized.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (hasDescription) {
                        val chevronRotation by animateFloatAsState(
                            targetValue = if (isExpanded) 0f else 180f,
                            label = "changelog-chevron-rotation",
                        )
                        Icon(
                            imageVector = Icons.Rounded.ExpandMore,
                            contentDescription = if (isExpanded) {
                                "Collapse description"
                            } else {
                                "Expand description"
                            },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(chevronRotation),
                        )
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    item.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        ChangelogMarkdownDescription(text = desc)
                    }
                }
            }
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

@Preview(showBackground = true, name = "Item card - FEATURE (no image)")
@Composable
private fun ChangelogItemCardFeaturePreview() {
    MinusTheme {
        ChangelogItemCard(
            modifier = Modifier.fillMaxWidth(),
            item = ChangelogItem(
                title = "Deep Insights v2",
                description = "Predictive AI analysis for recurring subscriptions with enhanced 99% accuracy model.",
                type = ReleaseType.FEATURE,
            ),
        )
    }
}

@Preview(showBackground = true, name = "Item card - IMPROVEMENT (no image)")
@Composable
private fun ChangelogItemCardImprovementPreview() {
    MinusTheme {
        ChangelogItemCard(
            modifier = Modifier.fillMaxWidth(),
            item = ChangelogItem(
                title = "Launch Optimization",
                type = ReleaseType.IMPROVEMENT,
                description = null
            ),
        )
    }
}

@Preview(showBackground = true, name = "Item card - BUG_FIX (no image)")
@Composable
private fun ChangelogItemCardBugFixPreview() {
    MinusTheme {
        ChangelogItemCard(
            modifier = Modifier.fillMaxWidth(),
            item = ChangelogItem(
                title = "Launcher icon fix",
                description = "Added the missing launcher icon into the app so it shows up correctly in the system app drawer.",
                type = ReleaseType.BUG_FIX,
            ),
        )
    }
}

@Preview(
    name = "Item card - title with PR link - DARK",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Preview(showBackground = true, name = "Item card - title with PR link")
@Composable
private fun ChangelogItemCardWithPrLinkPreview() {
    MinusTheme {
        ChangelogItemCard(
            modifier = Modifier.fillMaxWidth(),
            item = ChangelogItem(
                title = "Added badge and inf on settings about mode (#41)",
                description = "Adds a Settings icon badge and inline info card so users can see at a glance whether censor mode is active.",
                type = ReleaseType.FEATURE,
            ),
        )
    }
}

@Preview(showBackground = true, name = "Item card - Markdown description")
@Composable
private fun ChangelogItemCardWithMarkdownPreview() {
    MinusTheme {
        ChangelogItemCard(
            modifier = Modifier.fillMaxWidth(),
            item = ChangelogItem(
                title = "Numpad operators (#42)",
                description = """
                    ## What changed

                    Added support for `+`, `-`, `*`, `/` operators in the numpad so
                    users can compute totals inline. **Long-press** the result to
                    copy it to the clipboard.

                    See the [design spec](https://example.com/spec) for the
                    rationale and edge cases covered.

                    ```
                    100 + 50 * 2 = 200
                    ```
                """.trimIndent(),
                type = ReleaseType.FEATURE,
            ),
        )
    }
}
