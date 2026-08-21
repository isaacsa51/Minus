package com.serranoie.app.minus.presentation.ui.settings.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.widget.AddExpenseWidgetReceiver
import com.serranoie.app.minus.presentation.widget.AverageSpendWidgetReceiver
import com.serranoie.app.minus.presentation.widget.BudgetOverviewWidgetReceiver
import com.serranoie.app.minus.presentation.widget.CompleteBudgetWidgetReceiver
import com.serranoie.app.minus.presentation.widget.DaysCountdownWidgetReceiver
import com.serranoie.app.minus.presentation.widget.ExpenseWidgetReceiver
import com.serranoie.app.minus.presentation.widget.HeatmapWidgetReceiver
import com.serranoie.app.minus.presentation.widget.MinMaxSpentWidgetReceiver
import com.serranoie.app.minus.presentation.widget.MonthHeatmapWidgetReceiver

private data class WidgetGalleryItem(
    val previewLayoutRes: Int,
    val titleRes: Int,
    val descriptionRes: Int,
    val receiver: Class<*>,
)

private val widgetGalleryItems = listOf(
    WidgetGalleryItem(
        previewLayoutRes = R.layout.widget_complete_budget_preview,
        titleRes = R.string.widget_complete_budget_title,
        descriptionRes = R.string.widget_complete_budget_description,
        receiver = CompleteBudgetWidgetReceiver::class.java,
    ),
    WidgetGalleryItem(
        previewLayoutRes = R.layout.widget_budget_overview_preview,
        titleRes = R.string.budget_overview,
        descriptionRes = R.string.widget_budget_overview_description,
        receiver = BudgetOverviewWidgetReceiver::class.java,
    ),
    WidgetGalleryItem(
        previewLayoutRes = R.layout.widget_heatmap_preview,
        titleRes = R.string.widget_heatmap_title,
        descriptionRes = R.string.widget_heatmap_description,
        receiver = HeatmapWidgetReceiver::class.java,
    ),
    WidgetGalleryItem(
        previewLayoutRes = R.layout.widget_min_max_spent_preview,
        titleRes = R.string.widget_min_max_spent_title,
        descriptionRes = R.string.widget_min_max_spent_description,
        receiver = MinMaxSpentWidgetReceiver::class.java,
    ),
    WidgetGalleryItem(
        previewLayoutRes = R.layout.widget_month_heatmap_preview,
        titleRes = R.string.widget_month_heatmap_title,
        descriptionRes = R.string.widget_month_heatmap_description,
        receiver = MonthHeatmapWidgetReceiver::class.java,
    ),
    WidgetGalleryItem(
        previewLayoutRes = R.layout.widget_average_spend_preview,
        titleRes = R.string.widget_average_spend_title,
        descriptionRes = R.string.widget_average_spend_description,
        receiver = AverageSpendWidgetReceiver::class.java,
    ),
    WidgetGalleryItem(
        previewLayoutRes = R.layout.expense_widget_preview,
        titleRes = R.string.widget_expense_title,
        descriptionRes = R.string.widget_description,
        receiver = ExpenseWidgetReceiver::class.java,
    ),
    WidgetGalleryItem(
        previewLayoutRes = R.layout.widget_add_expense_preview,
        titleRes = R.string.widget_add_expense_label,
        descriptionRes = R.string.widget_add_expense_description,
        receiver = AddExpenseWidgetReceiver::class.java,
    ),
    WidgetGalleryItem(
        previewLayoutRes = R.layout.widget_days_countdown_preview,
        titleRes = R.string.widget_days_countdown_title,
        descriptionRes = R.string.widget_days_countdown_description,
        receiver = DaysCountdownWidgetReceiver::class.java,
    ),
)

private fun pinWidget(context: Context, receiver: Class<*>) {
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
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(widgetGalleryItems) { item ->
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PREVIEW_HEIGHT)
                .clip(RoundedCornerShape(14.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    val themedContext = ContextThemeWrapper(ctx, android.R.style.Theme_DeviceDefault_DayNight)
                    LayoutInflater.from(themedContext)
                        .inflate(item.previewLayoutRes, FrameLayout(themedContext), false)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
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

private val PREVIEW_HEIGHT = 150.dp

@PreviewLightDark
@Composable
private fun WidgetGallerySheetPreview() {
    MinusTheme {
        Surface {
            WidgetGallerySheet()
        }
    }
}
