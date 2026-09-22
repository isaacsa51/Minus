@file:OptIn(ExperimentalGlanceRemoteViewsApi::class)

package com.serranoie.app.minus.presentation.ui.settings.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.children
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.background
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.material3.ColorProviders
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.widget.AddExpenseWidget
import com.serranoie.app.minus.presentation.widget.AddExpenseWidgetReceiver
import com.serranoie.app.minus.presentation.widget.AverageSpendWidget
import com.serranoie.app.minus.presentation.widget.AverageSpendWidgetReceiver
import com.serranoie.app.minus.presentation.widget.BudgetOverviewWidget
import com.serranoie.app.minus.presentation.widget.BudgetOverviewWidgetReceiver
import com.serranoie.app.minus.presentation.widget.CompleteBudgetWidget
import com.serranoie.app.minus.presentation.widget.CompleteBudgetWidgetReceiver
import com.serranoie.app.minus.presentation.widget.DaysCountdownWidget
import com.serranoie.app.minus.presentation.widget.DaysCountdownWidgetReceiver
import com.serranoie.app.minus.presentation.widget.ExpenseWidget
import com.serranoie.app.minus.presentation.widget.ExpenseWidgetReceiver
import com.serranoie.app.minus.presentation.widget.HeatmapContent
import com.serranoie.app.minus.presentation.widget.HeatmapWidgetReceiver
import com.serranoie.app.minus.presentation.widget.MinMaxSpentWidget
import com.serranoie.app.minus.presentation.widget.MinMaxSpentWidgetReceiver
import com.serranoie.app.minus.presentation.widget.MonthCellState
import com.serranoie.app.minus.presentation.widget.MonthHeatmapContent
import com.serranoie.app.minus.presentation.widget.MonthHeatmapWidgetReceiver
import com.serranoie.app.minus.presentation.widget.MonthWidgetCellState
import com.serranoie.app.minus.presentation.widget.formatWidgetCurrency
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import java.time.YearMonth
import androidx.glance.LocalContext as GlanceLocalContext
import androidx.glance.layout.Box as GlanceBox

private const val TAG = "WidgetGallerySheet"
private const val PREVIEW_CURRENCY = "USD"
private val PREVIEW_HEIGHT = 150.dp

private data class WidgetGalleryItem(
    val titleRes: Int,
    val descriptionRes: Int,
    val receiver: Class<out GlanceAppWidgetReceiver>,
    /** Width the preview is composed at; `null` fills the card. */
    val previewWidth: Dp? = null,
    val previewHeight: Dp = PREVIEW_HEIGHT,
    /** The real widget content, composed with Glance and rendered with the app's color scheme. */
    val content: @Composable () -> Unit,
)

private val widgetGalleryItems = listOf(
    WidgetGalleryItem(
        titleRes = R.string.widget_complete_budget_title,
        descriptionRes = R.string.widget_complete_budget_description,
        receiver = CompleteBudgetWidgetReceiver::class.java,
        previewHeight = 220.dp,
    ) {
        val context = GlanceLocalContext.current
        CompleteBudgetWidget().CompleteBudgetContent(
            spendAmount = 180,
            budgetAmount = 500,
            currency = PREVIEW_CURRENCY,
            startDate = "06 Mar",
            endDate = "21 Mar",
            daysCount = 16,
            totalSpentLabel = context.getString(R.string.total_spent),
            totalBudgetLabel = context.getString(R.string.total_budget),
            daysCountFormat = { count -> context.daysLeftLabel(count) },
            addExpenseContentDescription = context.getString(R.string.widget_add_expense_label),
        )
    },
    WidgetGalleryItem(
        titleRes = R.string.budget_overview,
        descriptionRes = R.string.widget_budget_overview_description,
        receiver = BudgetOverviewWidgetReceiver::class.java,
    ) {
        val context = GlanceLocalContext.current
        BudgetOverviewWidget().BudgetOverviewContent(
            budgetAmount = 500,
            currency = PREVIEW_CURRENCY,
            startDate = "06 Mar",
            endDate = "21 Mar",
            daysCount = 16,
            totalBudgetLabel = context.getString(R.string.total_budget),
            daysCountFormat = { count -> context.daysLeftLabel(count) },
        )
    },
    WidgetGalleryItem(
        titleRes = R.string.widget_heatmap_title,
        descriptionRes = R.string.widget_heatmap_description,
        receiver = HeatmapWidgetReceiver::class.java,
        previewHeight = 180.dp,
    ) {
        val months = remember { (3L downTo 0L).map { YearMonth.now().minusMonths(it) } }
        val monthCells = remember(months) {
            months.mapIndexed { monthIndex, month ->
                (1..month.lengthOfMonth()).map { day ->
                    MonthCellState(
                        dayNumber = day,
                        ratio = sampleHeatRatio(day),
                        hasSpending = (day + monthIndex) % 4 != 0,
                    )
                }
            }
        }
        WidgetSurface {
            HeatmapContent(months = months, monthCells = monthCells)
        }
    },
    WidgetGalleryItem(
        titleRes = R.string.widget_min_max_spent_title,
        descriptionRes = R.string.widget_min_max_spent_description,
        receiver = MinMaxSpentWidgetReceiver::class.java,
    ) {
        val context = GlanceLocalContext.current
        MinMaxSpentWidget().MinMaxSpentContent(
            hasSpends = true,
            currency = PREVIEW_CURRENCY,
            minAmount = 42,
            maxAmount = 186,
            minDate = context.getString(R.string.widget_min_max_spent_preview_min_date),
            maxDate = context.getString(R.string.widget_min_max_spent_preview_max_date),
            minComment = "",
            maxComment = "",
            minimumSpentLabel = context.getString(R.string.minimum_spent),
            maximumSpentLabel = context.getString(R.string.maximum_spent),
            noExpensesLabel = context.getString(R.string.no_transactions_title),
            minChartRatios = listOf(0.24f, 0.4f, 0.31f, 0.8f, 0.56f, 1f, 0.62f, 0.22f),
            maxChartRatios = listOf(0.4f, 0.31f, 0.8f, 0.56f, 1f, 0.62f, 0.22f, 0.18f),
            minChartIndex = 0,
            maxChartIndex = 4,
        )
    },
    WidgetGalleryItem(
        titleRes = R.string.widget_month_heatmap_title,
        descriptionRes = R.string.widget_month_heatmap_description,
        receiver = MonthHeatmapWidgetReceiver::class.java,
        previewWidth = 150.dp,
        previewHeight = 150.dp,
    ) {
        val yearMonth = remember { YearMonth.now() }
        val monthCells = remember(yearMonth) {
            (1..yearMonth.lengthOfMonth()).map { day ->
                MonthWidgetCellState(
                    dayNumber = day,
                    ratio = sampleHeatRatio(day),
                    hasSpending = day % 4 != 0,
                )
            }
        }
        WidgetSurface {
            MonthHeatmapContent(
                yearMonth = yearMonth,
                monthCells = monthCells,
                totalSpent = 1250,
                currency = PREVIEW_CURRENCY,
            )
        }
    },
    WidgetGalleryItem(
        titleRes = R.string.widget_average_spend_title,
        descriptionRes = R.string.widget_average_spend_description,
        receiver = AverageSpendWidgetReceiver::class.java,
    ) {
        val context = GlanceLocalContext.current
        AverageSpendWidget().AverageSpendContent(
            averageValue = formatWidgetCurrency(PREVIEW_CURRENCY, 72),
            spendsCount = 8,
            hasSpends = true,
            label = context.getString(R.string.daily_average),
            noExpensesLabel = context.getString(R.string.no_transactions_title),
            daysLabel = context.resources.getQuantityString(R.plurals.days, 12, 12),
            chartRatios = listOf(0.4f, 0.2f, 0.8f, 0.5f, 0.9f, 0.3f, 0.7f, 0.6f, 1.0f, 0.4f),
        )
    },
    WidgetGalleryItem(
        titleRes = R.string.widget_expense_title,
        descriptionRes = R.string.widget_description,
        receiver = ExpenseWidgetReceiver::class.java,
    ) {
        ExpenseWidget().ExpenseWidgetContent(
            spend = 1250,
            budget = 5000,
            currency = PREVIEW_CURRENCY,
        )
    },
    WidgetGalleryItem(
        titleRes = R.string.widget_add_expense_label,
        descriptionRes = R.string.widget_add_expense_description,
        receiver = AddExpenseWidgetReceiver::class.java,
        previewHeight = 48.dp,
    ) {
        AddExpenseWidget().AddExpenseContent()
    },
    WidgetGalleryItem(
        titleRes = R.string.widget_days_countdown_title,
        descriptionRes = R.string.widget_days_countdown_description,
        receiver = DaysCountdownWidgetReceiver::class.java,
        previewWidth = 120.dp,
        previewHeight = 120.dp,
    ) {
        val context = GlanceLocalContext.current
        DaysCountdownWidget().DaysCountdownContent(
            daysRemaining = 12,
            totalDays = 30,
            periodLabel = context.getString(R.string.days_left),
        )
    },
)

private fun Context.daysLeftLabel(count: Int): String =
    resources.getQuantityString(R.plurals.analytics_days_left, count, count)

private fun sampleHeatRatio(day: Int): Float = when {
    day % 11 == 0 -> 1.3f
    day % 7 == 0 -> 0.9f
    day % 5 == 0 -> 0.65f
    day % 3 == 0 -> 0.35f
    else -> 0.1f
}

/** Mirrors the surface container the heatmap widgets wrap their content with in `provideGlance`. */
@Composable
private fun WidgetSurface(content: @Composable () -> Unit) {
    GlanceBox(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(8.dp),
    ) {
        content()
    }
}

private fun pinWidget(context: Context, receiver: Class<out GlanceAppWidgetReceiver>) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    if (appWidgetManager.isRequestPinAppWidgetSupported) {
        appWidgetManager.requestPinAppWidget(ComponentName(context, receiver), null, null)
    }
}

@Composable
fun WidgetGallerySheet(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isInspectionMode = LocalInspectionMode.current
    val isPinSupported = remember {
        isInspectionMode || AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_widgets_title),
            style = MaterialTheme.typography.titleLargeEmphasized,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        )
        Text(
            text = stringResource(R.string.settings_widgets_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp)
        )

        if (isPinSupported) {
            // A plain scrolling column keeps every preview composed once; a lazy list would
            // rebuild (and re-inflate) a preview each time its card scrolls back into view.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                widgetGalleryItems.forEach { item ->
                    WidgetGalleryRow(
                        item = item,
                        onClick = { pinWidget(context, item.receiver) },
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.settings_widgets_pin_not_supported),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            )
        }
    }
}

@Composable
private fun WidgetGalleryRow(item: WidgetGalleryItem, onClick: () -> Unit) {
    val cardBackground = combineColors(
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceVariant,
        t = 0.3f,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBackground)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        WidgetGlancePreview(
            item = item,
            modifier = Modifier
                .fillMaxWidth()
                .height(item.previewHeight),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(item.titleRes),
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(item.descriptionRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Renders [WidgetGalleryItem.content] through Glance into [RemoteViews], themed with the app's
 * current [MaterialTheme.colorScheme] so the preview follows the user's theme (light/dark,
 * AMOLED, color scheme) instead of the device default theme.
 */
@Composable
private fun WidgetGlancePreview(item: WidgetGalleryItem, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme

    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(14.dp))
                .background(colorScheme.surface),
        )
        return
    }

    val context = LocalContext.current
    val host = remember(context) { FrameLayout(context) }
    val glanceRemoteViews = remember { GlanceRemoteViews() }
    // ColorScheme compares by identity; key the composition on the colors the widgets use.
    val themeKey = with(colorScheme) {
        listOf(surface, onSurface, onSurfaceVariant, primary, onPrimary, primaryContainer, onPrimaryContainer, error)
    }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val previewSize = DpSize(item.previewWidth ?: maxWidth, item.previewHeight)
        val previewView by produceState<View?>(initialValue = null, previewSize, themeKey) {
            value = try {
                renderWidgetPreview(context, host, glanceRemoteViews, previewSize, colorScheme, item.content)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logcat(TAG, LogPriority.WARN) { "Failed to render widget preview: ${error.asLog()}" }
                null
            }
        }

        AndroidView(
            factory = { host },
            update = { hostView ->
                hostView.removeAllViews()
                previewView?.let { view ->
                    (view.parent as? ViewGroup)?.removeView(view)
                    hostView.addView(
                        view,
                        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
                    )
                }
            },
            modifier = Modifier
                .size(previewSize)
                .clip(RoundedCornerShape(14.dp)),
        )
    }
}

/**
 * Composes [content] into [RemoteViews] off the main thread (the Glance composition and its
 * translation are the expensive part) and then inflates it. Inflation has to stay on the main
 * thread, but with the non-lazy column it happens once per preview.
 */
private suspend fun renderWidgetPreview(
    context: Context,
    host: ViewGroup,
    glanceRemoteViews: GlanceRemoteViews,
    size: DpSize,
    colorScheme: ColorScheme,
    content: @Composable () -> Unit,
): View {
    val remoteViews = withContext(Dispatchers.Default) {
        glanceRemoteViews.compose(context = context, size = size) {
            GlanceTheme(colors = ColorProviders(colorScheme)) {
                content()
            }
        }.remoteViews
    }
    // Inflate with the application context: the Activity's LayoutInflater carries AppCompat's
    // view factory, and its AppCompatImageView/AppCompatTextView reject RemoteViews actions
    // (e.g. the ripple image Glance adds to every clickable → "can't use method setImageResource").
    val view = remoteViews.apply(context.applicationContext, host)
    // The widget content carries its own click actions; the card handles taps instead.
    view.disableClicks()
    return view
}

private fun View.disableClicks() {
    setOnClickListener(null)
    isClickable = false
    isLongClickable = false
    if (this is ViewGroup) children.forEach { it.disableClicks() }
}

@PreviewLightDark
@Composable
private fun WidgetGallerySheetPreview() {
    MinusTheme {
        Surface {
            WidgetGallerySheet()
        }
    }
}
