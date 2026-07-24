@file:OptIn(ExperimentalGlancePreviewApi::class)

package com.serranoie.app.minus.presentation.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.serranoie.app.minus.R

class ExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpenseWidget()
}

@Preview(widthDp = 180, heightDp = 180)
@Composable
fun ExpenseWidgetNormalPreview() {
    GlanceTheme {
        ExpenseWidget().ExpenseWidgetContent(
            spend = 3740,
            budget = 60000,
            currency = "USD"
        )
    }
}

@Preview(widthDp = 180, heightDp = 80)
@Composable
fun ExpenseWidgetCompactPreview() {
    GlanceTheme {
        ExpenseWidget().ExpenseWidgetContent(
            spend = 3740,
            budget = 60000,
            currency = "USD"
        )
    }
}

class ExpenseWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(100.dp, 50.dp),
            DpSize(100.dp, 100.dp)
        )
    )

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
        val currency = prefs[stringPreferencesKey("currency")] ?: "USD"

        ExpenseWidgetContent(
            spend = spend,
            budget = budget,
            currency = currency
        )
    }

    @Composable
    internal fun ExpenseWidgetContent(
        spend: Int,
        budget: Int,
        currency: String,
        context: Context = LocalContext.current,
        totalSpentLabel: String = context.getString(R.string.total_spent),
        addExpenseContentDescription: String = context.getString(R.string.widget_add_expense_label)
    ) {
        val size = LocalSize.current
        val isHorizontal = size.height < 100.dp

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .clickable(actionRunCallback<OpenAppAction>())
                .padding(16.dp)
        ) {
            if (isHorizontal) {
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = totalSpentLabel,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        )
                        Text(
                            text = formatWidgetCurrency(currency, spend),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    PlusButton(addExpenseContentDescription)
                }
            } else {
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    Text(
                        text = totalSpentLabel,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 16.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = formatWidgetCurrency(currency, spend),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        PlusButton(addExpenseContentDescription)
                    }
                }
            }
        }
    }

    @Composable
    private fun PlusButton(contentDescription: String) {
        Box(
            modifier = GlanceModifier
                .size(48.dp)
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
                modifier = GlanceModifier.size(24.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary)
            )
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
