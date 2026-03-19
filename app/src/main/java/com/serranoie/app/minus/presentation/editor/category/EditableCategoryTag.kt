package com.serranoie.app.minus.presentation.editor.category

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.serranoie.app.minus.LocalWindowInsets
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableCategoryTag(
	currentComment: String,
	tags: List<String>,
	onCommentUpdate: (String) -> Unit,
	editorFocusController: FocusController,
	extendWidth: Dp = 0.dp,
	onlyIcon: Boolean = false,
	onEdit: (Boolean) -> Unit = {},
) {
	val focusManager = LocalFocusManager.current
	val localDensity = LocalDensity.current

	var isEdit by remember { mutableStateOf(false) }
	var value by remember(currentComment) {
		mutableStateOf(
			TextFieldValue(
				currentComment,
				TextRange(currentComment.length),
			)
		)
	}
	var isShowSuggestions by remember { mutableStateOf(false) }
	var renderPopup by remember { mutableStateOf(false) }

	val close = {
		isEdit = false
		isShowSuggestions = false
		onEdit(false)
		onCommentUpdate(value.text.trim())
		focusManager.clearFocus()
	}

	ExposedDropdownMenuBox(expanded = isShowSuggestions, onExpandedChange = {}) {
		Surface(
			shape = CircleShape,
			color = MaterialTheme.colorScheme.surface,
			contentColor = MaterialTheme.colorScheme.onSurface,
			modifier = Modifier
				.menuAnchor()
				.clip(CircleShape)
				.then(
					if (isEdit) {
					Modifier
				} else {
					Modifier.clickable {
						editorFocusController.blur()
						focusManager.clearFocus()
						isEdit = true
						onEdit(true)
					}
				})) {
			Row(
				modifier = Modifier
					.widthIn(0.dp, extendWidth)
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
					imageVector = Icons.Default.Label,
					contentDescription = null,
				)

				AnimatedVisibility(
					visible = !isEdit,
					enter = scaleIn(tween(durationMillis = 150)),
					exit = scaleOut(tween(durationMillis = 150)),
				) {
					if (onlyIcon) {
						Spacer(Modifier.width(12.dp))
					} else {
						Spacer(Modifier.width(4.dp))
					}
				}

				AnimatedContent(
					label = "openCloseTaggingEditor", targetState = isEdit, transitionSpec = {
						(fadeIn(
							tween(durationMillis = 250)
						) togetherWith fadeOut(
							tween(durationMillis = 250)
						)).using(
							SizeTransform(clip = false)
						)
					}) { targetIsEdit ->
					if (this.transition.currentState == this.transition.targetState && targetIsEdit) {
						renderPopup = true
					}

					if (targetIsEdit) {
						CommentEditor(
							value = value,
							onChange = { value = it },
							onApply = { close() })
					} else if (!onlyIcon || value.text.isNotEmpty()) {
						Text(
							modifier = Modifier
								.padding(start = 4.dp, top = 8.dp, bottom = 8.dp, end = 12.dp)
								.heightIn(min = 28.dp)
								.wrapContentHeight(align = Alignment.CenterVertically),
							text = value.text.ifEmpty { "Add comment" },
							style = MaterialTheme.typography.bodyMedium,
							softWrap = false,
							overflow = TextOverflow.Ellipsis,
						)
					}
				}
			}
		}

		// Suggestions popup
		if (renderPopup) {
			val filteredItems = tags.filter { tag ->
				tag.contains(value.text, ignoreCase = true) && tag != value.text
			}

			val topBarHeight = LocalWindowInsets.current.calculateTopPadding()

			val height = remember { mutableStateOf(1000.dp) }
			val popupPositionProvider = DropdownMenuPositionProvider(
				DpOffset(0.dp, 8.dp),
				localDensity,
				topBarHeight,
			) { _, menuBounds ->
				height.value = with(localDensity) { menuBounds.height.toDp() }
			}

			Popup(
				popupPositionProvider = popupPositionProvider,
				onDismissRequest = { },
			) {
				val dismissEvent = remember { mutableStateOf(false) }
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.height(height.value)
						.pointerInput(Unit) {
							detectTapUnconsumed {
								if (!dismissEvent.value) close()
								dismissEvent.value = false
							}
						},
					contentAlignment = Alignment.BottomCenter,
				) {
					AnimatedVisibility(
						visible = isShowSuggestions,
						enter = expandVertically(tween(150)),
						exit = shrinkVertically(tween(150)),
					) {
						val filteredSize = filteredItems.size
						if (filteredSize > 0) {
							Surface(
								modifier = Modifier
									.width(extendWidth)
									.pointerInput(Unit) {
										detectTapGestures {
											dismissEvent.value = true
										}
									}, shape = RoundedCornerShape(16.dp)
							) {
								LazyColumn(
									userScrollEnabled = true,
									contentPadding = PaddingValues(vertical = 8.dp),
								) {
									filteredItems.forEach { item ->
										itemSuggest(item) {
											dismissEvent.value = true
											value = TextFieldValue(
												item,
												TextRange(item.length),
											)
										}
									}
								}
							}
						}

						DisposableEffect(Unit) {
							onDispose {
								renderPopup = false
							}
						}
					}
				}
			}

			LaunchedEffect(Unit) {
				isShowSuggestions = true
			}
		}
	}
}


/**
 * Suggestion item in the dropdown list.
 */
fun LazyListScope.itemSuggest(
	name: String,
	onClick: () -> Unit,
) {
	item(name) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween,
			modifier = Modifier
				.clickable { onClick() }
				.fillMaxWidth()
				.heightIn(42.dp)
				.padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
		) {
			Text(
				text = name,
				overflow = TextOverflow.Ellipsis,
				softWrap = false,
				modifier = Modifier.weight(1f)
			)
		}
	}
}


/**
 * Detects tap gestures that are not consumed by other components.
 */
suspend fun PointerInputScope.detectTapUnconsumed(
	onTap: () -> Unit
) {
	awaitPointerEventScope {
		while (true) {
			val event = awaitPointerEvent()
			if (event.changes.all { !it.isConsumed }) {
				onTap()
			}
		}
	}
}

@Preview(name = "EditableCategoryTag")
@Composable
private fun PreviewEditableCategoryTag() {
	MinusTheme {
		Column {
			EditableCategoryTag(
				currentComment = "Groceries",
				tags = listOf("Food", "Transport", "Shopping", "Entertainment"),
				onCommentUpdate = {},
				editorFocusController = remember { FocusController() },
			)
		}
	}
}