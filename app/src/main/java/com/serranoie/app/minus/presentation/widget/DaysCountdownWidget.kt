@file:OptIn(ExperimentalGlancePreviewApi::class)

package com.serranoie.app.minus.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview

class DaysCountdownWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DaysCountdownWidget()
}

@Preview(widthDp = 60, heightDp = 60)
@Composable
fun DaysCountdownWidgetPreview() {
    GlanceTheme {
        DaysCountdownWidget().DaysCountdownContent(daysRemaining = 12, totalDays = 30, periodLabel = "days left")
    }
}

@Preview(widthDp = 60, heightDp = 60)
@Composable
fun DaysCountdownWidgetPreviewUrgent() {
    GlanceTheme {
        DaysCountdownWidget().DaysCountdownContent(daysRemaining = 2, totalDays = 30, periodLabel = "days left")
    }
}

class DaysCountdownWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }

    @Composable
    private fun WidgetContent() {
        val prefs = currentState<Preferences>()
        val daysRemaining = prefs[intPreferencesKey("days_remaining")] ?: 0
        val totalDays = prefs[intPreferencesKey("total_days")] ?: 30
        val periodLabel = prefs[stringPreferencesKey("period_label")] ?: "days"

        DaysCountdownContent(daysRemaining, totalDays, periodLabel)
    }

    @Composable
    internal fun DaysCountdownContent(daysRemaining: Int, totalDays: Int, periodLabel: String) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .clickable(actionRunCallback<OpenAppAction>())
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Days remaining (large)
                Text(
                    text = "$daysRemaining",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = if (daysRemaining <= 3) 
                            GlanceTheme.colors.error 
                        else 
                            GlanceTheme.colors.primary,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = GlanceModifier.height(2.dp))

                // Period label
                Text(
                    text = periodLabel,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

suspend fun updateDaysCountdownWidget(
    context: Context,
    daysRemaining: Int,
    totalDays: Int,
    periodLabel: String = "days left"
) {
    val manager = GlanceAppWidgetManager(context)
    val glanceIds = manager.getGlanceIds(DaysCountdownWidget::class.java)

    glanceIds.forEach { glanceId ->
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[intPreferencesKey("days_remaining")] = daysRemaining
            prefs[intPreferencesKey("total_days")] = totalDays
            prefs[stringPreferencesKey("period_label")] = periodLabel
        }
        DaysCountdownWidget().update(context, glanceId)
    }
}
