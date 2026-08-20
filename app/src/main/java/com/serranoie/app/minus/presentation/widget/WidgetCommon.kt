@file:OptIn(ExperimentalGlancePreviewApi::class)

package com.serranoie.app.minus.presentation.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import com.serranoie.app.minus.R

@Composable
internal fun AddExpenseButton(
    contentDescription: String,
    size: Dp,
    modifier: GlanceModifier = GlanceModifier,
) {
    val iconSize = (size.value / 2).dp
    Box(
        modifier = modifier
            .size(size)
            .clickable(actionRunCallback<OpenAppAction>()),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.shape_soft_star_1),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
        )
        Image(
            provider = ImageProvider(R.drawable.ic_plus),
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(iconSize),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary)
        )
    }
}

@Preview(widthDp = 120, heightDp = 60)
@Composable
private fun AddExpenseButtonPreview() {
    GlanceTheme {
        Row(modifier = GlanceModifier.size(120.dp, 60.dp), verticalAlignment = Alignment.CenterVertically) {
            AddExpenseButton(contentDescription = "Add new expense", size = 40.dp)
            AddExpenseButton(contentDescription = "Add new expense", size = 32.dp)
        }
    }
}
