@file:OptIn(ExperimentalGlancePreviewApi::class)

package com.serranoie.app.minus.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
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
import androidx.glance.layout.Spacer
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
import com.serranoie.app.minus.presentation.util.formatCurrencySymbolOnly
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Date

private val averageSpendValueKey = stringPreferencesKey("average_spend_value")
private val averageSpendDaysKey = intPreferencesKey("average_spend_days")
private val averageSpendCountKey = intPreferencesKey("average_spend_count")
private val averageSpendHasSpendsKey = intPreferencesKey("average_spend_has_spends")

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
					.cornerRadius(24.dp)
					.background(Color(0x263F7DD5)),
				contentAlignment = Alignment.Center,
			) {
				AverageSpendBackdrop()

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
private fun AverageSpendBackdrop() {
	Box(
		modifier = GlanceModifier.fillMaxSize(),
		contentAlignment = Alignment.BottomCenter,
	) {
		Column(
			modifier = GlanceModifier
				.fillMaxWidth()
				.height(42.dp)
				.background(Color(0x143F7DD5)),
		) {}
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

	val manager = GlanceAppWidgetManager(context)
	val glanceIds = manager.getGlanceIds(AverageSpendWidget::class.java)

	glanceIds.forEach { glanceId ->
		updateAppWidgetState(context, glanceId) { prefs ->
			prefs[averageSpendValueKey] = averageDisplay
			prefs[averageSpendDaysKey] = days
			prefs[averageSpendCountKey] = activeSpends.size
			prefs[averageSpendHasSpendsKey] = if (activeSpends.isNotEmpty()) 1 else 0
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
		)
	}
}
