@file:OptIn(ExperimentalGlancePreviewApi::class)

package com.serranoie.app.minus.presentation.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
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
import logcat.logcat

class ExpenseWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(60.dp, 40.dp),
            DpSize(80.dp, 40.dp),
            DpSize(180.dp, 50.dp), // 3x1
            DpSize(100.dp, 100.dp) // 2x2
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
        logcat("ExpenseWidget") { "ExpenseWidget size: width=${size.width}, height=${size.height}" }
        val useHorizontal = size.height < 180.dp && size.width >= size.height
        val isSmall = size.height <= 50.dp
        val isExtreme = size.height <= 40.dp

        val paddingV = when {
            isExtreme -> 2.dp
            isSmall -> 4.dp
            useHorizontal -> 8.dp
            else -> 16.dp
        }
        val paddingH = when {
            isExtreme -> 4.dp
            isSmall -> 8.dp
            useHorizontal -> 12.dp
            else -> 16.dp
        }
        val labelSize = when {
            isExtreme -> 12.sp
            isSmall -> 12.sp
            useHorizontal -> 14.sp
            else -> 16.sp
        }
        val amountSize = when {
            isExtreme -> 12.sp
            isSmall -> 14.sp
            useHorizontal -> 16.sp
            else -> 28.sp
        }
        val buttonSize = when {
            isExtreme -> 12.dp
            isSmall -> 28.dp
            useHorizontal -> 28.dp
            else -> 48.dp
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .clickable(actionRunCallback<OpenAppAction>())
                .padding(horizontal = paddingH, vertical = paddingV)
        ) {
            if (useHorizontal) {
                HorizontalExpenseContent(
                    totalSpentLabel = totalSpentLabel,
                    amount = formatWidgetCurrency(currency, spend),
                    labelSize = labelSize,
                    amountSize = amountSize,
                    buttonSize = buttonSize,
                    buttonDescription = addExpenseContentDescription
                )
            } else {
                VerticalExpenseContent(
                    totalSpentLabel = totalSpentLabel,
                    amount = formatWidgetCurrency(currency, spend),
                    labelSize = labelSize,
                    amountSize = amountSize,
                    buttonSize = buttonSize,
                    buttonDescription = addExpenseContentDescription
                )
            }
        }
    }

    @Composable
    private fun HorizontalExpenseContent(
        totalSpentLabel: String,
        amount: String,
        labelSize: TextUnit,
        amountSize: TextUnit,
        buttonSize: Dp,
        buttonDescription: String
    ) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = totalSpentLabel,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = labelSize
                    ),
                    maxLines = 1
                )
                Text(
                    text = amount,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = amountSize,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }
            PlusButton(buttonDescription, buttonSize)
        }
    }

    @Composable
    private fun VerticalExpenseContent(
        totalSpentLabel: String,
        amount: String,
        labelSize: TextUnit,
        amountSize: TextUnit,
        buttonSize: Dp,
        buttonDescription: String
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            Text(
                text = totalSpentLabel,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = labelSize
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = amount,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = amountSize,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                PlusButton(buttonDescription, buttonSize)
            }
        }
    }

    @Composable
    private fun PlusButton(contentDescription: String, size: Dp) {
        val iconSize = (size.value / 2).dp
        Box(
            modifier = GlanceModifier
                .size(size)
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
                modifier = GlanceModifier.size(iconSize),
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
    logcat("ExpenseWidget") { "Updating ExpenseWidget: spend=$spend, budget=$budget, currency=$currency" }
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

@Preview(widthDp = 80, heightDp = 40)
@Composable
fun ExpenseWidgetWidePreview() {
    GlanceTheme {
        ExpenseWidget().ExpenseWidgetContent(
            spend = 3740,
            budget = 60000,
            currency = "USD"
        )
    }
}

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

@Preview(widthDp = 80, heightDp = 40)
@Composable
fun ExpenseWidgetCompactPreview() {
    GlanceTheme {
        ExpenseWidget().ExpenseWidgetContent(
            spend = 3740,
            budget = 60000,
            currency = "MAD"
        )
    }
}
