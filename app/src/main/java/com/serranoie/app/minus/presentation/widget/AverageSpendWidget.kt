@file:OptIn(ExperimentalGlancePreviewApi::class)

package com.serranoie.app.minus.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
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
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.util.font.format.formatCurrencySymbolOnly
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Date

private const val AVERAGE_CHART_BAR_COUNT = 10

private val averageSpendValueKey = stringPreferencesKey("average_spend_value")
private val averageSpendDaysKey = intPreferencesKey("average_spend_days")
private val averageSpendCountKey = intPreferencesKey("average_spend_count")
private val averageSpendHasSpendsKey = intPreferencesKey("average_spend_has_spends")

private fun averageChartRatioKey(index: Int) =
    floatPreferencesKey("average_spend_chart_ratio_$index")

class AverageSpendWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AverageSpendWidget()
}

class AverageSpendWidget : GlanceAppWidget() {
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
        val chartRatios = (0 until AVERAGE_CHART_BAR_COUNT).map { index ->
            prefs[averageChartRatioKey(index)] ?: 0f
        }

        AverageSpendContent(
            averageValue = prefs[averageSpendValueKey] ?: context.getString(R.string.empty),
            spendsCount = prefs[averageSpendCountKey] ?: 0,
            hasSpends = (prefs[averageSpendHasSpendsKey] ?: 0) == 1,
            label = context.getString(R.string.daily_average),
            noExpensesLabel = context.getString(R.string.no_transactions_title),
            daysLabel = context.resources.getQuantityString(
                R.plurals.days,
                prefs[averageSpendDaysKey] ?: 1,
                prefs[averageSpendDaysKey] ?: 1,
            ),
            chartRatios = chartRatios,
        )
    }

    @Composable
    internal fun AverageSpendContent(
        averageValue: String,
        spendsCount: Int,
        hasSpends: Boolean,
        label: String,
        noExpensesLabel: String,
        daysLabel: String,
        chartRatios: List<Float> = emptyList(),
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .clickable(actionRunCallback<OpenAppAction>())
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(16.dp)
                    .background(Color(0x263F7DD5)),
                contentAlignment = Alignment.Center,
            ) {
                AverageSpendBackdrop(chartRatios)

                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (hasSpends) averageValue else "-",
                        style = TextStyle(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                    )

                    Text(
                        text = label,
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                    )

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    Text(
                        text = if (hasSpends) "$spendsCount · $daysLabel" else noExpensesLabel,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun AverageSpendBackdrop(ratios: List<Float>) {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(42.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            ratios.forEach { ratio ->
                val barHeight = (42.dp * ratio.coerceIn(0.1f, 1f))
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(barHeight)
                            .padding(horizontal = 1.dp)
                            .background(Color(0x143F7DD5)),
                    ) {}
                }
            }
        }
    }
}

suspend fun updateAverageSpendWidget(
    context: Context,
    spends: List<Transaction>,
    currency: String,
    startDate: Date,
    endDate: Date,
) {
    val activeSpends = spends.filterNot { it.isDeleted }
    val days = calculateAverageSpendDays(startDate, endDate)
    val average = calculateAverageSpend(activeSpends, days)
    val averageDisplay = average?.let {
        formatCurrencySymbolOnly(
            value = it,
            currencyCode = currency,
            maximumFractionDigits = 2,
            minimumFractionDigits = 0,
        )
    } ?: context.getString(R.string.empty)

    val maxAmount = activeSpends.maxOfOrNull { it.amount } ?: BigDecimal.ONE
    val recentSpends =
        activeSpends.sortedByDescending { it.date }.take(AVERAGE_CHART_BAR_COUNT).reversed()
    val chartRatios =
        recentSpends.map { (it.amount.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f) }

    val manager = GlanceAppWidgetManager(context)
    val glanceIds = manager.getGlanceIds(AverageSpendWidget::class.java)

    glanceIds.forEach { glanceId ->
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[averageSpendValueKey] = averageDisplay
            prefs[averageSpendDaysKey] = days
            prefs[averageSpendCountKey] = activeSpends.size
            prefs[averageSpendHasSpendsKey] = if (activeSpends.isNotEmpty()) 1 else 0
            for (i in 0 until AVERAGE_CHART_BAR_COUNT) {
                prefs[averageChartRatioKey(i)] = chartRatios.getOrNull(i) ?: 0f
            }
        }
        AverageSpendWidget().update(context, glanceId)
    }
}

private fun calculateAverageSpendDays(startDate: Date, endDate: Date): Int {
    val diff = endDate.time - startDate.time
    return (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
}

private fun calculateAverageSpend(spends: List<Transaction>, days: Int): BigDecimal? {
    if (spends.isEmpty()) return null
    val totalAmount = spends.sumOf { it.amount }
    return totalAmount.divide(days.toBigDecimal(), 2, RoundingMode.HALF_EVEN)
}

@Preview(widthDp = 180, heightDp = 120)
@Composable
private fun AverageSpendWidgetPreview() {
    GlanceTheme {
        AverageSpendWidget().AverageSpendContent(
            averageValue = "$72",
            spendsCount = 8,
            hasSpends = true,
            label = "Daily average",
            noExpensesLabel = "No recorded expenses",
            daysLabel = "12 days",
            chartRatios = listOf(
                0.4f,
                0.2f,
                0.8f,
                0.5f,
                0.9f,
                0.3f,
                0.7f,
                0.6f,
                1.0f,
                0.4f,
                0.2f,
                0.5f
            )
        )
    }
}
