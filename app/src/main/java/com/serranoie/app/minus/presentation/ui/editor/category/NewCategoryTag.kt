@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.ui.editor.category

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SavedConfirmationMillis = 1_500L

private sealed interface NewCategoryStage {
    data object Collapsed : NewCategoryStage
    data object Editing : NewCategoryStage
    data class Saved(val name: String) : NewCategoryStage
}

@Composable
fun NewCategoryTag(
    onCreateCategory: suspend (String) -> Boolean,
    modifier: Modifier = Modifier,
    extendWidth: Dp = 0.dp,
    startSavedName: String? = null,
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var stage by remember {
        mutableStateOf<NewCategoryStage>(
            startSavedName?.let { NewCategoryStage.Saved(it) } ?: NewCategoryStage.Collapsed
        )
    }
    var value by remember { mutableStateOf(TextFieldValue()) }

    val fadeSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val sizeSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
    val label = stringResource(R.string.new_category)

    val isEditing = stage == NewCategoryStage.Editing
    val containerColor by animateColorAsState(
        targetValue = if (isEditing) {
            MaterialTheme.colorScheme.surface
        } else {
            Color.Transparent
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "newCategoryContainer",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isEditing) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "newCategoryBorder",
    )
    val tagContentColor by animateColorAsState(
        targetValue = if (isEditing) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "newCategoryContent",
    )

    LaunchedEffect(stage) {
        if (stage is NewCategoryStage.Saved) {
            delay(SavedConfirmationMillis)
            stage = NewCategoryStage.Collapsed
        }
    }

    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = tagContentColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .clip(CircleShape)
            .then(
                if (isEditing) Modifier else Modifier.clickable {
                    focusManager.clearFocus()
                    value = TextFieldValue()
                    stage = NewCategoryStage.Editing
                }
            ),
    ) {
        Row(
            modifier = Modifier
                .widthIn(0.dp, extendWidth)
                .heightIn(min = 44.dp)
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.Rounded.Sell,
                contentDescription = null,
            )

            AnimatedContent(
                label = "newCategoryEditor",
                targetState = stage,
                transitionSpec = {
                    (fadeIn(fadeSpec) togetherWith fadeOut(fadeSpec))
                        .using(SizeTransform(clip = false) { _, _ -> sizeSpec })
                },
            ) { targetStage ->
                when (targetStage) {
                    NewCategoryStage.Editing -> CommentEditor(
                        value = value,
                        onChange = { value = it },
                        onApply = {
                            if (stage == NewCategoryStage.Editing) {
                                val name = value.text.trim()
                                if (name.isEmpty()) {
                                    stage = NewCategoryStage.Collapsed
                                    focusManager.clearFocus()
                                } else {
                                    scope.launch {
                                        val success = onCreateCategory(name)
                                        stage = if (success) {
                                            NewCategoryStage.Saved(name)
                                        } else {
                                            NewCategoryStage.Collapsed
                                        }
                                        focusManager.clearFocus()
                                    }
                                }
                            }
                        },
                        placeholder = label,
                    )

                    is NewCategoryStage.Saved -> Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 16.dp)
                            .heightIn(min = 28.dp)
                            .wrapContentHeight(align = Alignment.CenterVertically),
                        text = stringResource(R.string.new_category_saved, targetStage.name),
                        style = MaterialTheme.typography.bodyMediumCondensed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    NewCategoryStage.Collapsed -> Text(
                        modifier = Modifier
                            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 16.dp)
                            .heightIn(min = 28.dp)
                            .wrapContentHeight(align = Alignment.CenterVertically),
                        text = label,
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
private fun NewCategoryTagPreview() {
    MinusTheme {
        Box(modifier = Modifier.padding(8.dp)) {
            NewCategoryTag(onCreateCategory = { true }, extendWidth = 300.dp)
        }
    }
}

@Preview
@Composable
private fun NewCategoryTagSavedPreview() {
    MinusTheme {
        Box(modifier = Modifier.padding(8.dp)) {
            NewCategoryTag(
                onCreateCategory = { true },
                extendWidth = 300.dp,
                startSavedName = "Groceries",
            )
        }
    }
}

@Preview
@Composable
private fun NewCategoryTagSavedLongNamePreview() {
    MinusTheme {
        Box(modifier = Modifier.padding(8.dp)) {
            NewCategoryTag(
                onCreateCategory = { true },
                extendWidth = 300.dp,
                startSavedName = "Weekend groceries and household supplies",
            )
        }
    }
}
