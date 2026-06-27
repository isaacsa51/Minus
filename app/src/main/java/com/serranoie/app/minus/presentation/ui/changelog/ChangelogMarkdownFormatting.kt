package com.serranoie.app.minus.presentation.ui.changelog

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import com.serranoie.app.minus.presentation.util.Utils

@Composable
fun ChangelogMarkdownDescription(text: String) {
    val context = LocalContext.current
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    val annotated = remember(text, linkColor, codeBackground) {
        changelogMarkdownToAnnotatedString(text, context, linkColor, codeBackground)
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMediumCondensed,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun changelogMarkdownToAnnotatedString(
    markdown: String,
    context: android.content.Context,
    linkColor: Color,
    codeBackground: Color,
): AnnotatedString = buildAnnotatedString {
    var inCodeBlock = false

    for (rawLine in markdown.lines()) {
        val trimmed = rawLine.trim()

        when {
            trimmed.startsWith("```") -> {
                inCodeBlock = !inCodeBlock
                append("\n")
            }

            inCodeBlock -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackground,
                    ),
                ) {
                    append(rawLine)
                }
                append("\n")
            }

            trimmed.startsWith("### ") -> {
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    ),
                ) {
                    append(trimmed.removePrefix("### "))
                }
                append("\n")
            }

            trimmed.startsWith("## ") -> {
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    ),
                ) {
                    append(trimmed.removePrefix("## "))
                }
                append("\n")
            }

            trimmed.startsWith("# ") -> {
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                ) {
                    append(trimmed.removePrefix("# "))
                }
                append("\n")
            }

            rawLine.isBlank() -> {}

            else -> {
                changelogRenderInlineMarkdown(rawLine, context, linkColor, codeBackground)
                append("\n")
            }
        }
    }
}

private fun AnnotatedString.Builder.changelogRenderInlineMarkdown(
    line: String,
    context: android.content.Context,
    linkColor: Color,
    codeBackground: Color,
) {
    val inlinePattern = Regex(
        "\\*\\*[^*]+\\*\\*" +
            "|\\*[^*]+\\*" +
            "|`[^`]+`" +
            "|\\[([^\\]]+)\\]\\(([^)]+)\\)",
    )

    var lastEnd = 0
    for (match in inlinePattern.findAll(line)) {
        if (match.range.first > lastEnd) {
            append(line.substring(lastEnd, match.range.first))
        }

        val value = match.value
        when {
            value.startsWith("**") && value.endsWith("**") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(value.removeSurrounding("**"))
                }
            }

            value.startsWith("`") && value.endsWith("`") -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackground,
                    ),
                ) {
                    append(value.removeSurrounding("`"))
                }
            }

            value.startsWith("[") -> {
                val linkText = match.groups[1]?.value ?: ""
                val url = match.groups[2]?.value ?: ""
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
                    append(linkText)
                }
            }

            else -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(value.removeSurrounding("*"))
                }
            }
        }
        lastEnd = match.range.last + 1
    }

    if (lastEnd < line.length) {
        append(line.substring(lastEnd))
    }
}
