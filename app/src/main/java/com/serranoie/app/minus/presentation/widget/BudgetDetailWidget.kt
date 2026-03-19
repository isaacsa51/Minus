package com.serranoie.app.minus.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.serranoie.app.minus.R

class BudgetDetailWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetDetailWidget()
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 180, heightDp = 110)
@Composable
fun BudgetDetailWidgetPreview() {
    GlanceTheme {
        BudgetDetailWidget().BudgetDetailContent(
            totalSpent = 450,
            remainingBudget = 550,
            daysRemaining = 12,
            currency = "$",
            topCategory = "Food",
            topCategoryAmount = 180
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 180, heightDp = 110)
@Composable
fun BudgetDetailWidgetPreviewOverBudget() {
    GlanceTheme {
        BudgetDetailWidget().BudgetDetailContent(
            totalSpent = 1200,
            remainingBudget = -200,
            daysRemaining = 2,
            currency = "$",
            topCategory = "Shopping",
            topCategoryAmount = 500
        )
    }
}

class BudgetDetailWidget : GlanceAppWidget() {

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
        val totalSpent = prefs[intPreferencesKey("total_spent")] ?: 0
        val remainingBudget = prefs[intPreferencesKey("remaining_budget")] ?: 0
        val daysRemaining = prefs[intPreferencesKey("days_remaining")] ?: 0
        val currency = prefs[stringPreferencesKey("currency")] ?: "$"
        val topCategory = prefs[stringPreferencesKey("top_category")] ?: "Food"
        val topCategoryAmount = prefs[intPreferencesKey("top_category_amount")] ?: 0

        BudgetDetailContent(totalSpent, remainingBudget, daysRemaining, currency, topCategory, topCategoryAmount)
    }

    @Composable
    internal fun BudgetDetailContent(
        totalSpent: Int,
        remainingBudget: Int,
        daysRemaining: Int,
        currency: String,
        topCategory: String,
        topCategoryAmount: Int
    ) {
        val isOverBudget = remainingBudget < 0

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with app icon
                Row(
                    horizontalAlignment = Alignment.Start,
					verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Budget Overview",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Top row: Remaining + Days
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Remaining Budget
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isOverBudget) "Over Budget" else "Remaining",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "$currency${kotlin.math.abs(remainingBudget)}",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                color = if (isOverBudget) 
                                    GlanceTheme.colors.error 
                                else 
                                    GlanceTheme.colors.primary,
                                textAlign = TextAlign.Center
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(24.dp))

                    // Vertical Divider
                    Box(
                        modifier = GlanceModifier
                            .width(1.dp)
                            .height(35.dp)
                            .background(GlanceTheme.colors.outline)
                    ) {}

                    Spacer(modifier = GlanceModifier.width(24.dp))

                    // Days Left
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Days Left",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "$daysRemaining",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                color = if (daysRemaining <= 3) 
                                    GlanceTheme.colors.error 
                                else 
                                    GlanceTheme.colors.onSurface,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Top Category
                Box(
                    modifier = GlanceModifier
                        .height(40.dp)
                        .background(GlanceTheme.colors.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Top: $topCategory",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                        Text(
                            text = "$currency$topCategoryAmount",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurface
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Total spent and add button
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Spent",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                        Text(
                            text = "$currency$totalSpent",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurface
                            )
                        )
                    }
                }
            }
        }
    }
}

// Helper to update widget data
suspend fun updateBudgetDetailWidget(
    context: Context,
    totalSpent: Int,
    remainingBudget: Int,
    daysRemaining: Int,
    currency: String,
    topCategory: String = "",
    topCategoryAmount: Int = 0
) {
    val manager = GlanceAppWidgetManager(context)
    val glanceIds = manager.getGlanceIds(BudgetDetailWidget::class.java)

    glanceIds.forEach { glanceId ->
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[intPreferencesKey("total_spent")] = totalSpent
            prefs[intPreferencesKey("remaining_budget")] = remainingBudget
            prefs[intPreferencesKey("days_remaining")] = daysRemaining
            prefs[stringPreferencesKey("currency")] = currency
            prefs[stringPreferencesKey("top_category")] = topCategory
            prefs[intPreferencesKey("top_category_amount")] = topCategoryAmount
        }
        BudgetDetailWidget().update(context, glanceId)
    }
}
