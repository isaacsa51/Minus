package com.serranoie.app.minus.presentation.editor.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.serranoie.app.minus.presentation.budget.BudgetViewModel
import com.serranoie.app.minus.presentation.budget.mvi.BudgetUiIntent
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditStage

/**
 * Controller for managing focus between the custom tag editor and system components.
 */
class FocusController {
    var onFocus: MutableState<(() -> Unit)?> = mutableStateOf(null)
    var onBlur: MutableState<(() -> Unit)?> = mutableStateOf(null)

    fun focus() {
        onFocus.value?.let { it() }
    }

    fun blur() {
        onBlur.value?.let { it() }
    }
}



@Composable
fun CategoryToolbar(
    tags: List<String>,
    currentComment: String,
    stage: EditStage,
    onCommentUpdate: (String) -> Unit,
    editorFocusController: FocusController,
    modifier: Modifier = Modifier
) {
    val localDensity = LocalDensity.current

    var showAddComment by remember { mutableStateOf(false) }
    var isEdit by remember { mutableStateOf(false) }

    LaunchedEffect(stage) {
        showAddComment = stage == EditStage.EDIT_SPENT
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val width = maxWidth - 48.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(44.dp)
                .horizontalScroll(
                    state = rememberScrollState(),
                    enabled = !isEdit,
                    reverseScrolling = true,
                )
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            // Show recent tags (excluding current comment to avoid duplicates)
            tags.take(5).reversed().filter { it != currentComment }.forEach { tag ->
                AnimatedVisibility(
                    visible = showAddComment,
                    enter = fadeIn(
                        tween(
                            durationMillis = 150,
                            easing = EaseInOutQuad,
                        )
                    ) + slideInHorizontally(
                        tween(
                            durationMillis = 150,
                            easing = EaseInOutQuad,
                        )
                    ) { with(localDensity) { 24.dp.toPx().toInt() } },
                    exit = fadeOut(
                        tween(
                            durationMillis = 150,
                            easing = EaseInOutQuad,
                        )
                    ) + slideOutHorizontally(
                        tween(
                            durationMillis = 150,
                            easing = EaseInOutQuad,
                        )
                    ) { with(localDensity) { 24.dp.toPx().toInt() } },
                ) {
	                CategoryTag(value = tag, onClick = {
		                onCommentUpdate(tag)
	                })
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

	        // ???????
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            AnimatedVisibility(
                visible = showAddComment,
                enter = fadeIn(
                    tween(
                        durationMillis = 150,
                        easing = EaseInOutQuad,
                    )
                ) + slideInHorizontally(
                    tween(
                        durationMillis = 150,
                        easing = EaseInOutQuad,
                    )
                ) { with(localDensity) { 30.dp.toPx().toInt() } },
                exit = fadeOut(
                    tween(
                        durationMillis = 150,
                        easing = EaseInOutQuad,
                    )
                ) + slideOutHorizontally(
                    tween(
                        durationMillis = 150,
                        easing = EaseInOutQuad,
                    )
                ) { with(localDensity) { 30.dp.toPx().toInt() } },
            ) {
                EditableCategoryTag(
                    currentComment = currentComment,
                    tags = tags,
                    onCommentUpdate = onCommentUpdate,
                    editorFocusController = editorFocusController,
                    extendWidth = width,
                    onlyIcon = tags.isNotEmpty(),
                    onEdit = { isEdit = it },
                )
            }
        }
    }
}

/**
 * Position provider for the dropdown menu popup.
 */
internal data class DropdownMenuPositionProvider(
    val contentOffset: DpOffset,
    val density: Density,
    val topBarHeight: Dp,
    val onPositionCalculated: (IntRect, IntRect) -> Unit = { _, _ -> }
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        // The min margin above and below the menu, relative to the screen.
        val verticalMargin = with(density) { 48.dp.roundToPx() }
        val topBarHeightPx = with(density) { topBarHeight.roundToPx() }
        // The content offset specified using the dropdown offset parameter.
        val contentOffsetX = with(density) { contentOffset.x.roundToPx() }
        val contentOffsetY = with(density) { contentOffset.y.roundToPx() }

        // Compute horizontal position.
        val toRight = anchorBounds.left + contentOffsetX
        val toLeft = anchorBounds.right - contentOffsetX - popupContentSize.width
        val toDisplayRight = windowSize.width - popupContentSize.width
        val toDisplayLeft = 0
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            sequenceOf(
                toRight,
                toLeft,
                // If the anchor gets outside of the window on the left, we want to position
                // toDisplayLeft for proximity to the anchor. Otherwise, toDisplayRight.
                if (anchorBounds.left >= 0) toDisplayRight else toDisplayLeft
            )
        } else {
            sequenceOf(
                toLeft,
                toRight,
                // If the anchor gets outside of the window on the right, we want to position
                // toDisplayRight for proximity to the anchor. Otherwise, toDisplayLeft.
                if (anchorBounds.right <= windowSize.width) toDisplayLeft else toDisplayRight
            )
        }.firstOrNull {
            it >= 0 && it + popupContentSize.width <= windowSize.width
        } ?: toLeft

        // Compute vertical position.
        val yBottom = anchorBounds.top - contentOffsetY

        onPositionCalculated(
            anchorBounds,
            IntRect(x, topBarHeightPx, x + popupContentSize.width, yBottom)
        )
        return IntOffset(x, topBarHeightPx)
    }
}

/**
 * Comment editor text field with focus management.
 */
@Composable
fun CommentEditor(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onChange: (comment: TextFieldValue) -> Unit,
    onApply: () -> Unit,
) {
    var focusIsTracking by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    TextField(
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (!focusState.hasFocus && focusIsTracking) {
                    onApply()
                }
            },
        value = value,
        onValueChange = {
            onChange(it)
        },
        trailingIcon = {
            IconButton(
                modifier = Modifier.padding(end = 4.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                onClick = { onApply() },
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                )
            }
        },
        textStyle = MaterialTheme.typography.bodyLarge,
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = { onApply() }
        ),
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        focusIsTracking = true
    }
}

/**
 * ViewModel-backed version of the tagging toolbar.
 */
@Composable
fun TaggingToolbarWithViewModel(
    viewModel: BudgetViewModel = hiltViewModel(),
    editorFocusController: FocusController,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CategoryToolbar(
        tags = uiState.tags,
        currentComment = uiState.currentComment,
        stage = if (uiState.numpadInput.isNotEmpty()) EditStage.EDIT_SPENT else EditStage.IDLE,
        onCommentUpdate = { comment ->
            viewModel.processIntent(BudgetUiIntent.CommentUpdated(comment))
        },
        editorFocusController = editorFocusController,
        modifier = modifier
    )
}

@Preview(name = "Tagging Toolbar", showSystemUi = false, showBackground = false)
@Composable
private fun CategoryToolbarPreview() {
    MinusTheme {
			CategoryToolbar(
				tags = listOf("Food", "Transport", "Shopping", "Entertainment"),
				currentComment = "Groceries",
				stage = EditStage.EDIT_SPENT,
				onCommentUpdate = {},
				editorFocusController = remember { FocusController() },
				modifier = Modifier.padding(vertical = 16.dp)
			)
		}
}


@Preview(name = "Empty with suggestion")
@Composable
private fun CategoryToolbarPreviewEmpty() {
	MinusTheme {
			CategoryToolbar(
				tags = listOf("Food", "Transport", "Shopping", "Entertainment"),
				currentComment = "",
				stage = EditStage.EDIT_SPENT,
				onCommentUpdate = {},
				editorFocusController = remember { FocusController() },
				modifier = Modifier.padding(vertical = 16.dp)
			)
		}
}
