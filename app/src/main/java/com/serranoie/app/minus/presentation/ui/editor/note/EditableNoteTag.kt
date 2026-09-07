package com.serranoie.app.minus.presentation.ui.editor.note

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.editor.category.CommentEditor
import com.serranoie.app.minus.presentation.ui.editor.category.FocusController
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Inline, single-line editor for a transaction's optional free-form note. Mirrors
 * [com.serranoie.app.minus.presentation.ui.editor.category.EditableCategoryTag]: a compact chip that
 * morphs into a text field in place, growing up to [extendWidth] and pushing the category tag aside
 * while it is focused. Unlike the category tag there is no suggestion popup — a note has no corpus
 * to autocomplete against.
 *
 * Whatever is in the field is committed via [onNoteUpdate] on apply (check button, IME "Done", or
 * focus loss) — a note is low stakes, so nothing is discarded.
 */
@Composable
fun EditableNoteTag(
    currentNote: String,
    onNoteUpdate: (String) -> Unit,
    editorFocusController: FocusController,
    modifier: Modifier = Modifier,
    extendWidth: Dp = 0.dp,
    onlyIcon: Boolean = false,
    onEdit: (Boolean) -> Unit = {},
    startInEditMode: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var isEdit by remember { mutableStateOf(startInEditMode) }
    var value by remember(currentNote) {
        mutableStateOf(
            TextFieldValue(currentNote, TextRange(currentNote.length))
        )
    }

    val close = {
        isEdit = false
        onEdit(false)
        onNoteUpdate(value.text.trim())
        focusManager.clearFocus()
    }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .clip(CircleShape)
            .then(
                if (isEdit) {
                    Modifier
                } else {
                    Modifier.clickable {
                        editorFocusController.blur()
                        focusManager.clearFocus()
                        scope.launch {
                            delay(120)
                            isEdit = true
                            onEdit(true)
                        }
                    }
                }
            ),
    ) {
        Row(
            modifier = Modifier
                // Grow/shrink the pill smoothly between the collapsed chip and the full-width
                // field instead of snapping. Anchored to the trailing edge so it expands
                // leftwards from where the chip sat in the End-arranged toolbar.
                .animateContentSize(
                    animationSpec = tween(durationMillis = 220, easing = EaseInOutQuad),
                    alignment = Alignment.CenterEnd,
                )
                .then(
                    // While editing, take the full track so the field fills the row; the
                    // horizontalScroll parent leaks unbounded width, so a plain widthIn cap
                    // isn't enough to make the inner fillMaxWidth resolve.
                    if (isEdit && extendWidth > 0.dp) {
                        Modifier.width(extendWidth)
                    } else {
                        Modifier.widthIn(0.dp, extendWidth)
                    }
                )
                .heightIn(min = 44.dp)
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = isEdit,
                enter = scaleIn(tween(durationMillis = 150)),
                exit = scaleOut(tween(durationMillis = 150)),
            ) {
                Spacer(Modifier.width(8.dp))
            }

            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.Rounded.EditNote,
                contentDescription = null,
            )

            AnimatedVisibility(
                visible = !isEdit,
                enter = scaleIn(tween(durationMillis = 150)),
                exit = scaleOut(tween(durationMillis = 150)),
            ) {
                Spacer(Modifier.width(if (onlyIcon) 12.dp else 4.dp))
            }

            AnimatedContent(
                label = "openCloseNoteEditor",
                targetState = isEdit,
                // Just crossfade the chip label and the field; snap the container size so the
                // pill's grow/shrink is owned once, by animateContentSize on the Row above.
                transitionSpec = {
                    (fadeIn(tween(durationMillis = 180)) togetherWith
                        fadeOut(tween(durationMillis = 180)))
                        .using(SizeTransform(clip = false) { _, _ -> snap() })
                },
            ) { targetIsEdit ->
                if (targetIsEdit) {
                    CommentEditor(
                        value = value,
                        onChange = { value = it },
                        onApply = { close() },
                    )
                } else if (!onlyIcon || value.text.isNotEmpty()) {
                    Text(
                        modifier = Modifier
                            .padding(start = 4.dp, top = 8.dp, bottom = 8.dp, end = 12.dp)
                            .heightIn(min = 28.dp)
                            .wrapContentHeight(align = Alignment.CenterVertically),
                        text = value.text.ifEmpty { stringResource(R.string.add_extra_note) },
                        style = MaterialTheme.typography.bodyMediumCondensed,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun EditableNoteTagPreview() {
    MinusTheme {
        Box(modifier = Modifier.padding(8.dp)) {
            EditableNoteTag(
                currentNote = "Airport taxi — split with Sam",
                onNoteUpdate = {},
                editorFocusController = remember { FocusController() },
                extendWidth = 300.dp,
            )
        }
    }
}

@Preview
@Composable
private fun EditableNoteTagEmptyPreview() {
    MinusTheme {
        Box(modifier = Modifier.padding(8.dp)) {
            EditableNoteTag(
                currentNote = "",
                onNoteUpdate = {},
                editorFocusController = remember { FocusController() },
                extendWidth = 300.dp,
            )
        }
    }
}

@Preview
@Composable
private fun EditableNoteTagFocusedPreview() {
    MinusTheme {
        Box(modifier = Modifier.padding(8.dp)) {
            EditableNoteTag(
                currentNote = "Airport taxi",
                onNoteUpdate = {},
                editorFocusController = remember { FocusController() },
                extendWidth = 300.dp,
                startInEditMode = true,
            )
        }
    }
}
