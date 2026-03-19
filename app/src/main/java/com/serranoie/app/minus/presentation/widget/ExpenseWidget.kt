@file:OptIn(ExperimentalGlancePreviewApi::class)

package com.serranoie.app.minus.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview

class ExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpenseWidget()
}

@Preview(widthDp = 180, heightDp = 110)
@Composable
fun ExpenseWidgetPreviewLowSpend() {
    GlanceTheme {
        ExpenseWidget().ExpenseWidgetContent(
            spend = 3740,
            budget = 60000,
            currency = "$"
        )
    }
}

@Preview(widthDp = 180, heightDp = 110)
@Composable
fun ExpenseWidgetPreviewMediumSpend() {
    GlanceTheme {
        ExpenseWidget().ExpenseWidgetContent(
            spend = 30740,
            budget = 60000,
            currency = "$"
        )
    }
}

@Preview(widthDp = 180, heightDp = 110)
@Composable
fun ExpenseWidgetPreviewHighSpend() {
    GlanceTheme {
        ExpenseWidget().ExpenseWidgetContent(
            spend = 45740,
            budget = 60000,
            currency = "$"
        )
    }
}

@Preview(widthDp = 180, heightDp = 110)
@Composable
fun ExpenseWidgetPreviewEmpty() {
    GlanceTheme {
        ExpenseWidget().ExpenseWidgetContent(
            spend = 0,
            budget = 60000,
            currency = "$"
        )
    }
}

class ExpenseWidget : GlanceAppWidget() {

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
        val spend = prefs[intPreferencesKey("spend")] ?: 0
        val budget = prefs[intPreferencesKey("budget")] ?: 1
        val currency = prefs[stringPreferencesKey("currency")] ?: "$"

        ExpenseWidgetContent(spend, budget, currency)
    }

    @Composable
    internal fun ExpenseWidgetContent(spend: Int, budget: Int, currency: String) {
        // Calculate percentage spent (0.0 to 1.0+)
        val percentSpent = if (budget > 0) {
            spend.toFloat() / budget.toFloat()
        } else 0f

        val percentRemaining = ((1f - percentSpent) * 100).toInt().coerceAtLeast(0)

        // Get color based on percentage spent
        val progressColor = when {
            percentSpent < 0.33f -> Color(0xFF81C784) // Good - Green
            percentSpent < 0.66f ->Color(0xFFFFB74D) // Not Good - Orange
            else -> Color(0xFFE57373) // Bad - Red
        }

        val progressPercent = percentSpent.coerceIn(0f, 1f)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(16.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                // Main content row (spend amount + plus button)
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Spend amount
                    Column {
                        Text(
                            text = "Gastado",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "$currency${formatAmount(spend)}",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurface
                            )
                        )
                    }

                    // Push button to the right
                    Spacer(modifier = GlanceModifier.fillMaxWidth())

                    // Plus button
                    Button(
                        text = "+",
                        onClick = actionRunCallback<OpenAppAction>(),
                        modifier = GlanceModifier.size(32.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Percentage remaining text
                Text(
                    text = "Disponible: $percentRemaining%",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Progress bar (simplified wavy pattern as horizontal bar)
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(GlanceTheme.colors.surfaceVariant)
                ) {
                    // Progress fill
                    Box(
                        modifier = GlanceModifier
                            .fillMaxHeight()
                            .width(percentSpent.coerceIn(0f, 1f) * 1000.dp / 1000f) // Will be calculated at runtime
                            .background(progressColor)
                    ) {}
                }
            }
        }
    }

    private fun formatAmount(amount: Int): String {
        return amount.toString()
            .reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()
    }
}

class OpenAppAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.let {
            it.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(it)
        }
    }
}

suspend fun updateExpenseWidget(context: Context, spend: Int, budget: Int, currency: String) {
    val manager = GlanceAppWidgetManager(context)
    val glanceIds = manager.getGlanceIds(ExpenseWidget::class.java)

    glanceIds.forEach { glanceId ->
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[intPreferencesKey("spend")] = spend
            prefs[intPreferencesKey("budget")] = budget
            prefs[stringPreferencesKey("currency")] = currency
        }
        ExpenseWidget().update(context, glanceId)
    }
}
