@file:OptIn(ExperimentalGlancePreviewApi::class)

package com.serranoie.app.minus.presentation.widget

import android.content.Context
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BudgetOverviewWidgetReceiver : GlanceAppWidgetReceiver() {
	override val glanceAppWidget: GlanceAppWidget = BudgetOverviewWidget()
}

class BudgetOverviewWidget : GlanceAppWidget() {

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
		val budgetAmount = prefs[androidx.datastore.preferences.core.intPreferencesKey("budget_amount")] ?: 0
		val currency = prefs[stringPreferencesKey("currency_code")] ?: "USD"
		val startDate = prefs[stringPreferencesKey("start_date")] ?: "-"
		val endDate = prefs[stringPreferencesKey("end_date")] ?: "-"
		val daysCount = prefs[stringPreferencesKey("days_count")] ?: "-"

		BudgetOverviewContent(budgetAmount, currency, startDate, endDate, daysCount)
	}

	@Composable
	internal fun BudgetOverviewContent(
		budgetAmount: Int,
		currency: String,
		startDate: String,
		endDate: String,
		daysCount: String
	) {
		Box(
			modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.surface)
				.clickable(actionRunCallback<OpenAppAction>())
				.padding(horizontal = 16.dp, vertical = 2.dp),
			contentAlignment = Alignment.CenterStart
		) {
			Column {
				// Budget amount
				Text(
					text = formatWidgetCurrency(currency, budgetAmount), style = TextStyle(
						fontSize = MaterialTheme.typography.h4.fontSize,
						fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface
					)
				)

				Spacer(modifier = GlanceModifier.height(2.dp))

				// Label
				Text(
					text = "Total Budget", style = TextStyle(
						color = GlanceTheme.colors.onSurfaceVariant,
						fontSize = MaterialTheme.typography.subtitle1.fontSize
					)
				)

				Spacer(modifier = GlanceModifier.height(12.dp))

				// Date range with arrow and days chip
				DateRangeRow(startDate, endDate, daysCount)
			}
		}
	}

	@Composable
	private fun DateRangeRow(startDate: String, endDate: String, daysCount: String) {
		Row(
			verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()
		) {
			// Start date
			Text(
				text = startDate, style = TextStyle(
					fontSize = MaterialTheme.typography.subtitle2.fontSize,
					color = GlanceTheme.colors.onSurfaceVariant
				),
				modifier = GlanceModifier.padding(end = 8.dp)
			)

			Spacer(modifier = GlanceModifier.width(8.dp))

			// Arrow line with days chip
			Box(
				modifier = GlanceModifier.defaultWeight().height(20.dp),
				contentAlignment = Alignment.Center
			) {
				// Horizontal line and arrow in a row
				Row(
					modifier = GlanceModifier.fillMaxWidth().height(2.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					// Horizontal line (arrow body)
					Box(
						modifier = GlanceModifier.defaultWeight().fillMaxHeight()
							.background(GlanceTheme.colors.onSurfaceVariant)
					) {}
				}

				// Days count chip positioned above the line with rounded corners
				if (daysCount != "-") {
					Box(
						modifier = GlanceModifier.fillMaxWidth(),
//							.padding(bottom = 1.dp),
						contentAlignment = Alignment.TopCenter
					) {
						Box(
							modifier = GlanceModifier
								.background(GlanceTheme.colors.onSurfaceVariant)
								.cornerRadius(16.dp)
								.padding(horizontal = 8.dp, vertical = 2.dp)
						) {
							Text(
								text = daysCount, style = TextStyle(
									color = GlanceTheme.colors.surface,
									textAlign = TextAlign.Center,
									fontSize = MaterialTheme.typography.caption.fontSize
								)
							)
						}
					}
				}
			}

			Spacer(modifier = GlanceModifier.width(8.dp))

			// End date
			Text(
				text = endDate, style = TextStyle(
					fontSize = MaterialTheme.typography.subtitle2.fontSize,
					color = GlanceTheme.colors.onSurfaceVariant
				),
				modifier = GlanceModifier.padding(start = 8.dp)
			)
		}
	}
}


@Preview(widthDp = 420, heightDp = 210)
@Composable
private fun BudgetOverviewWidgetPreview() {
	GlanceTheme {
		BudgetOverviewWidget().BudgetOverviewContent(
			budgetAmount = 500,
			currency = "USD",
			startDate = "06 Mar",
			endDate = "21 Mar",
			daysCount = "16 días"
		)
	}
}

@Preview(widthDp = 420, heightDp = 210)
@Composable
private fun BudgetOverviewWidgetPreviewShort() {
	GlanceTheme {
		BudgetOverviewWidget().BudgetOverviewContent(
			budgetAmount = 300,
			currency = "USD",
			startDate = "06 Mar",
			endDate = "09 Mar",
			daysCount = "3 días"
		)
	}
}

suspend fun updateBudgetOverviewWidget(
	context: Context,
	budgetAmount: Int,
	currency: String,
	startDate: Date,
	endDate: Date,
	daysCount: Int,
) {
	val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

	val manager = GlanceAppWidgetManager(context)
	val glanceIds = manager.getGlanceIds(BudgetOverviewWidget::class.java)

	glanceIds.forEach { glanceId ->
		updateAppWidgetState(context, glanceId) { prefs ->
			prefs[androidx.datastore.preferences.core.intPreferencesKey("budget_amount")] = budgetAmount
			prefs[stringPreferencesKey("currency_code")] = currency
			prefs[stringPreferencesKey("start_date")] = dateFormat.format(startDate)
			prefs[stringPreferencesKey("end_date")] = dateFormat.format(endDate)
			prefs[stringPreferencesKey("days_count")] =
				if (daysCount > 1) "$daysCount días" else "$daysCount día"
		}
		BudgetOverviewWidget().update(context, glanceId)
	}
}
