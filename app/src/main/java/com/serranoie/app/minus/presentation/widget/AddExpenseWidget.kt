@file:OptIn(ExperimentalGlancePreviewApi::class)

package com.serranoie.app.minus.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.serranoie.app.minus.R

class AddExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AddExpenseWidget()
}

class AddExpenseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                AddExpenseContent()
            }
        }
    }

    @Composable
    internal fun AddExpenseContent(
        context: Context = LocalContext.current,
        label: String = context.getString(R.string.widget_add_expense_label),
        compactLabel: String = context.getString(R.string.widget_add_expense_compact_label),
        addExpenseContentDescription: String = context.getString(R.string.widget_add_expense_label)
    ) {
        val widgetSize = LocalSize.current
        val compactMode = widgetSize.width < 150.dp

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(4.dp)
                .cornerRadius(999.dp)
                .background(GlanceTheme.colors.surface)
                .clickable(actionRunCallback<OpenAppAction>())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (compactMode) compactLabel else label,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface,
                        fontSize = if (compactMode) 13.sp else 15.sp,
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                Box(
                    modifier = GlanceModifier.size(40.dp),
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
                        contentDescription = addExpenseContentDescription,
                        modifier = GlanceModifier.size(20.dp),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary)
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 280, heightDp = 120)
@Composable
private fun AddExpenseWidgetPreview() {
    GlanceTheme {
        AddExpenseWidget().run {
            AddExpenseContent()
        }
    }
}
