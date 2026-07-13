@file:OptIn(ExperimentalGlancePreviewApi::class)

package com.serranoie.app.minus.presentation.widget

import android.content.Context
import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.serranoie.app.minus.R

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
            currency = "USD",
            totalSpentLabel = "Total Spent",
            addExpenseContentDescription = "Add expense",
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
            currency = "USD",
            totalSpentLabel = "Total Spent",
            addExpenseContentDescription = "Add expense",
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
            currency = "USD",
            totalSpentLabel = "Total Spent",
            addExpenseContentDescription = "Add expense",
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
            currency = "USD",
            totalSpentLabel = "Total Spent",
            addExpenseContentDescription = "Add expense",
        )
    }
}

class ExpenseWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent(context)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context) {
        val prefs = currentState<Preferences>()
        val spend = prefs[intPreferencesKey("spend")] ?: 0
        val budget = prefs[intPreferencesKey("budget")] ?: 1
        val currency = prefs[stringPreferencesKey("currency")] ?: "USD"

        ExpenseWidgetContent(
            spend = spend,
            budget = budget,
            currency = currency,
            totalSpentLabel = context.getString(R.string.total_spent),
            addExpenseContentDescription = context.getString(R.string.widget_add_expense_label),
        )
    }

    @Composable
    internal fun ExpenseWidgetContent(
        spend: Int,
        budget: Int,
        currency: String,
        totalSpentLabel: String = "Total Spent",
        addExpenseContentDescription: String = "Add expense",
    ) {
        val percentSpent = if (budget > 0) {
            spend.toFloat() / budget.toFloat()
        } else 0f

        val percentRemaining = ((1f - percentSpent) * 100).toInt().coerceAtLeast(0)

        val progressColor = when {
            percentSpent < 0.33f -> Color(0xFF81C784)
            percentSpent < 0.66f ->Color(0xFFFFB74D)
            else -> Color(0xFFE57373)
        }

        val progressPercent = percentSpent.coerceIn(0f, 1f)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .clickable(actionRunCallback<OpenAppAction>())
                .padding(16.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = totalSpentLabel,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = formatWidgetCurrency(currency, spend),
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                                color = GlanceTheme.colors.onSurface
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.fillMaxWidth())

                    Button(
                        text = "+",
                        onClick = actionRunCallback<OpenAppAction>(),
                        modifier = GlanceModifier.size(32.dp)
                    )
                }
            }

            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_plus),
                    contentDescription = addExpenseContentDescription,
                    modifier = GlanceModifier.size(20.dp)
                )
            }
        }
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
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
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
