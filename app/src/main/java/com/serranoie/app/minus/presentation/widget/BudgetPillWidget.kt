@file:OptIn(ExperimentalGlancePreviewApi::class)

package com.serranoie.app.minus.presentation.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.presentation.ui.theme.BudgetStatusColors
import com.serranoie.app.minus.presentation.ui.theme.BudgetStatusSeedTone
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.font.format.formatCurrencySymbolOnly
import com.serranoie.app.minus.presentation.util.harmonizeWithColor
import com.serranoie.app.minus.presentation.util.toPaletteWithTheme
import com.serranoie.app.minus.presentation.util.withTone
import java.math.BigDecimal

private val PILL_PADDING = 4.dp

/** Below this the label/amount pair needs the tighter type and padding to stay on one line. */
private val COMPACT_WIDTH = 190.dp

private val WIDE_WIDTH = 300.dp

private val SECONDARY_LINE_MIN_HEIGHT = 58.dp

private const val CHROMA_MULTIPLIER_DARK = 2f
private const val CHROMA_MULTIPLIER_LIGHT = 1.35f
private const val FILL_TONE_DARK = 50.0
private const val FILL_TONE_LIGHT = 72.0
private const val TRACK_TONE_DARK = 26.0
private const val TRACK_TONE_LIGHT = 94.0

private val hasBudgetKey = booleanPreferencesKey("budget_pill_has_budget")
private val viewPeriodKey = stringPreferencesKey("budget_pill_view_period")
private val remainingKey = stringPreferencesKey("budget_pill_remaining")
private val currencyKey = stringPreferencesKey("budget_pill_currency")
private val progressKey = floatPreferencesKey("budget_pill_progress")
private val overBudgetKey = booleanPreferencesKey("budget_pill_over_budget")
private val overPeriodKey = booleanPreferencesKey("budget_pill_over_period")
private val nextAllocationKey = stringPreferencesKey("budget_pill_next_allocation")

class BudgetPillWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetPillWidget()
}

class BudgetPillWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

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
        val viewPeriod = prefs[viewPeriodKey]
            ?.let { name -> BudgetPeriod.entries.firstOrNull { it.name == name } }
            ?: BudgetPeriod.DAILY

        BudgetPillContent(
            hasBudget = prefs[hasBudgetKey] ?: false,
            viewPeriod = viewPeriod,
            remaining = prefs[remainingKey]?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            currency = prefs[currencyKey] ?: "USD",
            progress = prefs[progressKey] ?: 0f,
            isOverBudget = prefs[overBudgetKey] ?: false,
            isOverPeriodAllocation = prefs[overPeriodKey] ?: false,
            nextAllocation = prefs[nextAllocationKey]?.toBigDecimalOrNull(),
        )
    }

    @Composable
    internal fun BudgetPillContent(
        hasBudget: Boolean,
        viewPeriod: BudgetPeriod,
        remaining: BigDecimal,
        currency: String,
        progress: Float,
        isOverBudget: Boolean,
        isOverPeriodAllocation: Boolean,
        nextAllocation: BigDecimal? = null,
        context: Context = LocalContext.current,
    ) {
        val size = LocalSize.current
        val isCompact = size.width < COMPACT_WIDTH
        val isWide = size.width >= WIDE_WIDTH

        val horizontalInset = when {
            isCompact -> 14.dp
            isWide -> 20.dp
            else -> 18.dp
        }
        val labelSize = when {
            isCompact -> 13.sp
            isWide -> 16.sp
            else -> 15.sp
        }
        val amountSize = when {
            isCompact -> 15.sp
            isWide -> 20.sp
            else -> 17.sp
        }

        val colors = pillColors(context, progress, hasBudget)
        val amountText = formatCurrencySymbolOnly(value = remaining, currencyCode = currency)
        val label = when {
            !hasBudget -> context.getString(R.string.budget_pill_no_budget_action)
            isOverBudget -> context.getString(R.string.budget_pill_over_budget)
            isOverPeriodAllocation -> context.getString(viewPeriod.exceededLabelRes())
            else -> context.getString(viewPeriod.periodLabelRes())
        }
        // The pill hides the amount whenever the status label takes the whole width.
        val centreLabel = !hasBudget || isOverBudget || isOverPeriodAllocation
        val nextLine = nextAllocation
            ?.takeIf { hasBudget && size.height >= SECONDARY_LINE_MIN_HEIGHT }
            ?.let { context.getString(viewPeriod.nextAllocationRes(), formatCurrencySymbolOnly(it, currency)) }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(PILL_PADDING)
                .clickable(actionRunCallback<OpenAppAction>())
                .semantics {
                    contentDescription = if (centreLabel) label else "$label: $amountText"
                },
            contentAlignment = Alignment.Center,
        ) {
            PillTrack(colors.track)
            PillFill(
                color = colors.fill,
                progress = progress,
                pillWidth = (size.width - PILL_PADDING * 2).coerceAtLeast(0.dp),
                pillHeight = (size.height - PILL_PADDING * 2).coerceAtLeast(0.dp),
            )

            if (centreLabel) {
                CentredStatus(
                    label = label,
                    secondary = nextLine,
                    labelSize = labelSize,
                    color = colors.content,
                )
            } else {
                LabelAndAmount(
                    label = label,
                    secondary = nextLine,
                    amount = amountText,
                    labelSize = labelSize,
                    amountSize = amountSize,
                    horizontalInset = horizontalInset,
                    color = colors.content,
                )
            }
        }
    }
}

@Composable
private fun PillTrack(color: Color) {
    Image(
        provider = ImageProvider(R.drawable.widget_budget_pill_shape),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        colorFilter = ColorFilter.tint(ColorProvider(color)),
        modifier = GlanceModifier.fillMaxSize(),
    )
}

@Composable
private fun PillFill(color: Color, progress: Float, pillWidth: Dp, pillHeight: Dp) {
    val fraction = progress.coerceIn(0f, 1f)
    if (fraction <= 0f || pillWidth <= 0.dp) return

    val fillWidth = if (fraction >= 1f) {
        pillWidth
    } else {
        (pillWidth * fraction).coerceIn(pillHeight.coerceAtMost(pillWidth), pillWidth)
    }

    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Image(
            provider = ImageProvider(R.drawable.widget_budget_pill_shape),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(ColorProvider(color)),
            modifier = GlanceModifier.width(fillWidth).fillMaxHeight(),
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
    }
}

@Composable
private fun LabelAndAmount(
    label: String,
    secondary: String?,
    amount: String,
    labelSize: TextUnit,
    amountSize: TextUnit,
    horizontalInset: Dp,
    color: Color,
) {
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = horizontalInset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = TextStyle(
                    color = ColorProvider(color),
                    fontSize = labelSize,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
            if (secondary != null) {
                SecondaryLine(secondary, color, TextAlign.Start)
            }
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = amount,
            style = TextStyle(
                color = ColorProvider(color),
                fontSize = amountSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
    }
}

@Composable
private fun CentredStatus(label: String, secondary: String?, labelSize: TextUnit, color: Color) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(color),
                fontSize = labelSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            modifier = GlanceModifier.fillMaxWidth(),
        )
        if (secondary != null) {
            SecondaryLine(secondary, color, TextAlign.Center)
        }
    }
}

@Composable
private fun SecondaryLine(text: String, color: Color, textAlign: TextAlign) {
    Text(
        text = text,
        style = TextStyle(
            color = ColorProvider(color),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = textAlign,
        ),
        maxLines = 1,
    )
}

private data class PillColors(val track: Color, val fill: Color, val content: Color)

@Composable
private fun pillColors(context: Context, progress: Float, hasBudget: Boolean): PillColors {
    val isDark = context.isNightMode
    val primary = GlanceTheme.colors.primary.getColor(context)
    val error = if (isDark) GlanceTheme.colors.errorContainer else GlanceTheme.colors.error
    val status = BudgetStatusColors(bad = error.getColor(context).withTone(BudgetStatusSeedTone))

    val statusColor = combineColors(
        listOf(status.good, status.notGood, status.bad),
        if (hasBudget) progress.coerceIn(0f, 1f) else 0f,
    )
    val chromaMultiplier = if (isDark) CHROMA_MULTIPLIER_DARK else CHROMA_MULTIPLIER_LIGHT
    val seed = harmonizeWithColor(statusColor, primary, chromaMultiplier)

    return PillColors(
        track = seed.withTone(if (isDark) TRACK_TONE_DARK else TRACK_TONE_LIGHT),
        fill = seed.withTone(if (isDark) FILL_TONE_DARK else FILL_TONE_LIGHT),
        content = toPaletteWithTheme(harmonizeWithColor(statusColor, primary), isDark).onContainer,
    )
}

private val Context.isNightMode: Boolean
    get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES

private fun String.toBigDecimalOrNull(): BigDecimal? = runCatching { BigDecimal(this) }.getOrNull()

private fun BudgetPeriod.periodLabelRes(): Int = when (this) {
    BudgetPeriod.DAILY -> R.string.budget_period_label_today
    BudgetPeriod.WEEKLY -> R.string.budget_period_label_this_week
    BudgetPeriod.BIWEEKLY -> R.string.budget_period_label_this_biweek
    BudgetPeriod.MONTHLY -> R.string.budget_period_label_this_month
}

private fun BudgetPeriod.exceededLabelRes(): Int = when (this) {
    BudgetPeriod.DAILY -> R.string.budget_pill_label_daily_exceeded
    BudgetPeriod.WEEKLY -> R.string.budget_pill_label_weekly_exceeded
    BudgetPeriod.BIWEEKLY -> R.string.budget_pill_label_biweekly_exceeded
    BudgetPeriod.MONTHLY -> R.string.budget_pill_label_monthly_exceeded
}

private fun BudgetPeriod.nextAllocationRes(): Int = when (this) {
    BudgetPeriod.DAILY -> R.string.budget_pill_next_daily
    BudgetPeriod.WEEKLY -> R.string.budget_pill_next_weekly
    BudgetPeriod.BIWEEKLY -> R.string.budget_pill_next_biweekly
    BudgetPeriod.MONTHLY -> R.string.budget_pill_next_monthly
}

suspend fun updateBudgetPillWidget(
    context: Context,
    hasBudget: Boolean,
    viewPeriod: BudgetPeriod,
    remaining: BigDecimal,
    currency: String,
    progress: Float,
    isOverBudget: Boolean,
    isOverPeriodAllocation: Boolean,
    nextAllocation: BigDecimal?,
) {
    val glanceIds = boundGlanceIds(
        context,
        BudgetPillWidgetReceiver::class.java,
        BudgetPillWidget::class.java,
    )

    glanceIds.forEach { glanceId ->
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[hasBudgetKey] = hasBudget
            prefs[viewPeriodKey] = viewPeriod.name
            prefs[remainingKey] = remaining.toPlainString()
            prefs[currencyKey] = currency
            prefs[progressKey] = progress.coerceIn(0f, 1f)
            prefs[overBudgetKey] = isOverBudget
            prefs[overPeriodKey] = isOverPeriodAllocation
            prefs[nextAllocationKey] = nextAllocation?.toPlainString() ?: ""
        }
        BudgetPillWidget().update(context, glanceId)
    }
}

@Preview(widthDp = 250, heightDp = 50)
@Composable
private fun BudgetPillWidgetHealthyPreview() {
    GlanceTheme {
        BudgetPillWidget().BudgetPillContent(
            hasBudget = true,
            viewPeriod = BudgetPeriod.DAILY,
            remaining = BigDecimal("110.00"),
            currency = "USD",
            progress = 0.1f,
            isOverBudget = false,
            isOverPeriodAllocation = false,
        )
    }
}

@Preview(widthDp = 130, heightDp = 50)
@Composable
private fun BudgetPillWidgetCompactPreview() {
    GlanceTheme {
        BudgetPillWidget().BudgetPillContent(
            hasBudget = true,
            viewPeriod = BudgetPeriod.WEEKLY,
            remaining = BigDecimal("413.33"),
            currency = "EUR",
            progress = 0.45f,
            isOverBudget = false,
            isOverPeriodAllocation = false,
        )
    }
}

@Preview(widthDp = 380, heightDp = 60)
@Composable
private fun BudgetPillWidgetWidePreview() {
    GlanceTheme {
        BudgetPillWidget().BudgetPillContent(
            hasBudget = true,
            viewPeriod = BudgetPeriod.MONTHLY,
            remaining = BigDecimal("1240.50"),
            currency = "USD",
            progress = 0.68f,
            isOverBudget = false,
            isOverPeriodAllocation = false,
            nextAllocation = BigDecimal("320.00"),
        )
    }
}

@Preview(widthDp = 250, heightDp = 50)
@Composable
private fun BudgetPillWidgetPeriodExceededPreview() {
    GlanceTheme {
        BudgetPillWidget().BudgetPillContent(
            hasBudget = true,
            viewPeriod = BudgetPeriod.WEEKLY,
            remaining = BigDecimal.ZERO,
            currency = "USD",
            progress = 1f,
            isOverBudget = false,
            isOverPeriodAllocation = true,
        )
    }
}

@Preview(widthDp = 250, heightDp = 50)
@Composable
private fun BudgetPillWidgetOverBudgetPreview() {
    GlanceTheme {
        BudgetPillWidget().BudgetPillContent(
            hasBudget = true,
            viewPeriod = BudgetPeriod.DAILY,
            remaining = BigDecimal("-50.00"),
            currency = "USD",
            progress = 1f,
            isOverBudget = true,
            isOverPeriodAllocation = false,
        )
    }
}

@Preview(widthDp = 250, heightDp = 50)
@Composable
private fun BudgetPillWidgetNoBudgetPreview() {
    GlanceTheme {
        BudgetPillWidget().BudgetPillContent(
            hasBudget = false,
            viewPeriod = BudgetPeriod.DAILY,
            remaining = BigDecimal.ZERO,
            currency = "USD",
            progress = 0f,
            isOverBudget = false,
            isOverPeriodAllocation = false,
        )
    }
}
