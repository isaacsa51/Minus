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
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Date
import java.util.Locale

private const val MIN_MAX_CHART_BAR_COUNT = 8

private val minAmountKey = intPreferencesKey("min_max_spent_min_amount")
private val maxAmountKey = intPreferencesKey("min_max_spent_max_amount")
private val currencyKey = stringPreferencesKey("min_max_spent_currency")
private val minDateKey = stringPreferencesKey("min_max_spent_min_date")
private val maxDateKey = stringPreferencesKey("min_max_spent_max_date")
private val minCommentKey = stringPreferencesKey("min_max_spent_min_comment")
private val maxCommentKey = stringPreferencesKey("min_max_spent_max_comment")
private val hasSpendsKey = intPreferencesKey("min_max_spent_has_spends")
private val minChartIndexKey = intPreferencesKey("min_max_spent_min_chart_index")
private val maxChartIndexKey = intPreferencesKey("min_max_spent_max_chart_index")
private fun minChartRatioKey(index: Int) =
    floatPreferencesKey("min_max_spent_min_chart_ratio_$index")

private fun maxChartRatioKey(index: Int) =
    floatPreferencesKey("min_max_spent_max_chart_ratio_$index")

class MinMaxSpentWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MinMaxSpentWidget()
}

class MinMaxSpentWidget : GlanceAppWidget() {
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
        val hasSpends = (prefs[hasSpendsKey] ?: 0) == 1
        val minChartRatios = (0 until MIN_MAX_CHART_BAR_COUNT).map { index ->
            prefs[minChartRatioKey(index)] ?: 0f
        }
        val maxChartRatios = (0 until MIN_MAX_CHART_BAR_COUNT).map { index ->
            prefs[maxChartRatioKey(index)] ?: 0f
        }

        MinMaxSpentContent(
            hasSpends = hasSpends,
            currency = prefs[currencyKey] ?: "USD",
            minAmount = prefs[minAmountKey] ?: 0,
            maxAmount = prefs[maxAmountKey] ?: 0,
            minDate = prefs[minDateKey] ?: "-",
            maxDate = prefs[maxDateKey] ?: "-",
            minComment = prefs[minCommentKey] ?: "",
            maxComment = prefs[maxCommentKey] ?: "",
            minimumSpentLabel = context.getString(R.string.minimum_spent),
            maximumSpentLabel = context.getString(R.string.maximum_spent),
            noExpensesLabel = context.getString(R.string.no_transactions_title),
            minChartRatios = minChartRatios,
            maxChartRatios = maxChartRatios,
            minChartIndex = prefs[minChartIndexKey] ?: -1,
            maxChartIndex = prefs[maxChartIndexKey] ?: -1,
        )
    }

    @Composable
    internal fun MinMaxSpentContent(
        hasSpends: Boolean,
        currency: String,
        minAmount: Int,
        maxAmount: Int,
        minDate: String,
        maxDate: String,
        minComment: String,
        maxComment: String,
        minimumSpentLabel: String,
        maximumSpentLabel: String,
        noExpensesLabel: String,
        minChartRatios: List<Float>,
        maxChartRatios: List<Float>,
        minChartIndex: Int,
        maxChartIndex: Int,
    ) {
        Box(
            modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.surface)
                .clickable(actionRunCallback<OpenAppAction>()).padding(8.dp),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MinMaxSpentStatPanel(
                    title = minimumSpentLabel,
                    amount = if (hasSpends) formatWidgetCurrency(currency, minAmount) else "-",
                    date = if (hasSpends) minDate else noExpensesLabel,
                    comment = minComment,
                    isMin = true,
                    chartRatios = minChartRatios,
                    selectedChartIndex = minChartIndex,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                MinMaxSpentStatPanel(
                    title = maximumSpentLabel,
                    amount = if (hasSpends) formatWidgetCurrency(currency, maxAmount) else "-",
                    date = if (hasSpends) maxDate else noExpensesLabel,
                    comment = maxComment,
                    isMin = false,
                    chartRatios = maxChartRatios,
                    selectedChartIndex = maxChartIndex,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun MinMaxSpentStatPanel(
    title: String,
    amount: String,
    date: String,
    comment: String,
    isMin: Boolean,
    chartRatios: List<Float>,
    selectedChartIndex: Int,
    modifier: GlanceModifier = GlanceModifier,
) {
    val containerColor = if (isMin) {
        Color(0x263F7DD5)
    } else {
        Color(0x26DD1414)
    }

    Box(
        modifier = modifier.cornerRadius(16.dp).background(containerColor),
    ) {
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            SpendingBarsBackdrop(
                ratios = chartRatios,
                selectedIndex = selectedChartIndex,
                isMin = isMin,
                modifier = GlanceModifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp),
            )
        }

        Column(
            modifier = GlanceModifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = amount,
                style = TextStyle(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface,
                ),
                maxLines = 1,
            )

            Text(
                text = title,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
                maxLines = 1,
            )

            Spacer(modifier = GlanceModifier.height(6.dp))

            Text(
                text = date,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
                maxLines = 1,
            )

            if (comment.isNotBlank()) {
                Spacer(modifier = GlanceModifier.height(3.dp))
                Text(
                    text = "# $comment",
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SpendingBarsBackdrop(
    ratios: List<Float>,
    selectedIndex: Int,
    isMin: Boolean,
    modifier: GlanceModifier = GlanceModifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(MIN_MAX_CHART_BAR_COUNT) { index ->
            val ratio = ratios.getOrNull(index)?.coerceIn(0f, 1f) ?: 0f
            val hasData = ratio > 0f
            val isSelected = index == selectedIndex
            val barHeight = if (hasData) {
                6.dp + (34.dp * ratio)
            } else {
                0.dp
            }
            Box(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = GlanceModifier.fillMaxWidth().height(barHeight)
                        .padding(horizontal = 2.dp).cornerRadius(if (isSelected) 4.dp else 16.dp)
                        .background(chartBarColor(ratio, isSelected, isMin)),
                ) {}
            }
        }
    }
}

private fun chartBarColor(ratio: Float, isSelected: Boolean, isMin: Boolean): Color {
    if (ratio <= 0f) return Color.Transparent

    if (isSelected) {
        return if (isMin) Color(0x993F7DD5) else Color(0x99DD1414)
    }

    return when {
        ratio <= 0.25f -> Color(0x333F7DD5)
        ratio <= 0.6f -> Color(0x33A96776)
        else -> Color(0x33DD1414)
    }
}

suspend fun updateMinMaxSpentWidget(
    context: Context,
    spends: List<Transaction>,
    currency: String,
) {
    val activeSpends = spends.filterNot { it.isDeleted }
    val minSpent = activeSpends.minByOrNull { it.amount }
    val maxSpent = activeSpends.maxByOrNull { it.amount }
    val maxAmount = activeSpends.maxOfOrNull { it.amount } ?: BigDecimal.ZERO
    val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
    val orderedSpends = activeSpends.sortedBy { it.date }
    val minChart = buildSelectedChartWindow(orderedSpends, minSpent, maxAmount)
    val maxChart = buildSelectedChartWindow(orderedSpends, maxSpent, maxAmount)

    val manager = GlanceAppWidgetManager(context)
    val glanceIds = manager.getGlanceIds(MinMaxSpentWidget::class.java)

    glanceIds.forEach { glanceId ->
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[hasSpendsKey] = if (activeSpends.isNotEmpty()) 1 else 0
            prefs[currencyKey] = currency
            prefs[minAmountKey] = minSpent?.amount?.toInt() ?: 0
            prefs[maxAmountKey] = maxSpent?.amount?.toInt() ?: 0
            prefs[minDateKey] = minSpent?.date?.let {
                dateFormat.format(
                    Date.from(
                        it.atZone(ZoneId.systemDefault()).toInstant()
                    )
                )
            } ?: "-"
            prefs[maxDateKey] = maxSpent?.date?.let {
                dateFormat.format(
                    Date.from(
                        it.atZone(ZoneId.systemDefault()).toInstant()
                    )
                )
            } ?: "-"
            prefs[minCommentKey] = minSpent?.comment.orEmpty()
            prefs[maxCommentKey] = maxSpent?.comment.orEmpty()
            prefs[minChartIndexKey] = minChart.selectedIndex
            prefs[maxChartIndexKey] = maxChart.selectedIndex
            for (index in 0 until MIN_MAX_CHART_BAR_COUNT) {
                prefs[minChartRatioKey(index)] = minChart.ratios.getOrNull(index) ?: 0f
                prefs[maxChartRatioKey(index)] = maxChart.ratios.getOrNull(index) ?: 0f
            }
        }
        MinMaxSpentWidget().update(context, glanceId)
    }
}

private data class WidgetChartWindow(
    val ratios: List<Float>,
    val selectedIndex: Int,
)

private fun buildSelectedChartWindow(
    orderedSpends: List<Transaction>,
    selectedSpend: Transaction?,
    maxAmount: BigDecimal,
): WidgetChartWindow {
    if (selectedSpend == null || orderedSpends.isEmpty()) {
        return WidgetChartWindow(
            ratios = List(MIN_MAX_CHART_BAR_COUNT) { 0f },
            selectedIndex = -1,
        )
    }

    val selectedIndexInAll = orderedSpends.indexOfFirst { it == selectedSpend }
    if (selectedIndexInAll < 0) {
        return WidgetChartWindow(
            ratios = List(MIN_MAX_CHART_BAR_COUNT) { 0f },
            selectedIndex = -1,
        )
    }

    val itemsBeforeSelected = 4
    val maxStart = (orderedSpends.size - MIN_MAX_CHART_BAR_COUNT).coerceAtLeast(0)
    val startIndex = (selectedIndexInAll - itemsBeforeSelected).coerceIn(0, maxStart)
    val window = orderedSpends.drop(startIndex).take(MIN_MAX_CHART_BAR_COUNT)
    val leadingEmptyCount = MIN_MAX_CHART_BAR_COUNT - window.size

    return WidgetChartWindow(
        ratios = buildMinMaxChartRatios(window, maxAmount),
        selectedIndex = leadingEmptyCount + (selectedIndexInAll - startIndex),
    )
}

private fun buildMinMaxChartRatios(
    spends: List<Transaction>,
    maxAmount: BigDecimal,
): List<Float> {
    if (spends.isEmpty() || maxAmount <= BigDecimal.ZERO) {
        return List(MIN_MAX_CHART_BAR_COUNT) { 0f }
    }

    return spends.map { spend ->
        (spend.amount.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f)
    }.let { ratios ->
        List(MIN_MAX_CHART_BAR_COUNT - ratios.size) { 0f } + ratios
    }
}

@Preview(widthDp = 360, heightDp = 160)
@Composable
private fun MinMaxSpentWidgetPreview() {
    GlanceTheme {
        MinMaxSpentWidget().MinMaxSpentContent(
            hasSpends = true,
            currency = "MXN",
            minAmount = 42,
            maxAmount = 186,
            minDate = "14 Jun 2026 10:20",
            maxDate = "15 Jun 2026 18:45",
            minComment = "Coffee",
            maxComment = "Groceries",
            minimumSpentLabel = "Minimum spent",
            maximumSpentLabel = "Maximum spent",
            noExpensesLabel = "No recorded expenses",
            minChartRatios = listOf(0.24f, 0.4f, 0.31f, 0.8f, 0.56f, 1f, 0.62f, 0.22f),
            maxChartRatios = listOf(0.4f, 0.31f, 0.8f, 0.56f, 1f, 0.62f, 0.22f, 0.18f),
            minChartIndex = 0,
            maxChartIndex = 4,
        )
    }
}

@Preview(widthDp = 360, heightDp = 160)
@Composable
private fun MinMaxSpentWidgetEmptyPreview() {
    GlanceTheme {
        MinMaxSpentWidget().MinMaxSpentContent(
            hasSpends = false,
            currency = "MXN",
            minAmount = 0,
            maxAmount = 0,
            minDate = "-",
            maxDate = "-",
            minComment = "",
            maxComment = "",
            minimumSpentLabel = "Minimum spent",
            maximumSpentLabel = "Maximum spent",
            noExpensesLabel = "No recorded expenses",
            minChartRatios = emptyList(),
            maxChartRatios = emptyList(),
            minChartIndex = -1,
            maxChartIndex = -1,
        )
    }
}
