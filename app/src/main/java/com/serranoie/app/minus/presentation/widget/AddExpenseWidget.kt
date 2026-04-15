@file:OptIn(ExperimentalGlancePreviewApi::class)

package com.serranoie.app.minus.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview

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
    internal fun AddExpenseContent() {
        val widgetSize = LocalSize.current
        val compactMode = widgetSize.width < 150.dp

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(6.dp)
                .cornerRadius(999.dp)
                .background(GlanceTheme.colors.surface)
                .clickable(actionRunCallback<OpenAppAction>())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (compactMode) "Gasto" else "Añadir gasto",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 15.sp
                    )
                )

                Text(
                    text = "   +",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 22.sp
                    )
                )
            }
        }
    }
}

@Preview(widthDp = 180, heightDp = 90)
@Composable
private fun AddExpenseWidgetPreview() {
    GlanceTheme {
        AddExpenseWidget().run {
            AddExpenseContent()
        }
    }
}
