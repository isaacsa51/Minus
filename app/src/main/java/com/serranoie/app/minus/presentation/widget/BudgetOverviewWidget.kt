@file:OptIn(ExperimentalGlancePreviewApi::class)

package com.serranoie.app.minus.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BudgetOverviewWidgetReceiver : GlanceAppWidgetReceiver() {
	override val glanceAppWidget: GlanceAppWidget = BudgetOverviewWidget()
}

@Preview
@Composable
fun BudgetOverviewWidgetPreview() {
	GlanceTheme {
		BudgetOverviewWidget().BudgetOverviewContent(
			budget = "$500.00", startDate = "06 Mar", endDate = "21 Mar", daysCount = "16 día(s)"
		)
	}
}

@Preview
@Composable
fun BudgetOverviewWidgetPreviewShort() {
	GlanceTheme {
		BudgetOverviewWidget().BudgetOverviewContent(
			budget = "$300.00", startDate = "06 Mar", endDate = "09 Mar", daysCount = "3 día(s)"
		)
	}
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
		val budget = prefs[stringPreferencesKey("budget")] ?: "$0.00"
		val startDate = prefs[stringPreferencesKey("start_date")] ?: "-"
		val endDate = prefs[stringPreferencesKey("end_date")] ?: "-"
		val daysCount = prefs[stringPreferencesKey("days_count")] ?: "-"

		BudgetOverviewContent(budget, startDate, endDate, daysCount)
	}

	@Composable
	internal fun BudgetOverviewContent(
		budget: String, startDate: String, endDate: String, daysCount: String
	) {
		Box(
			modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.surface)
				.padding(horizontal = 16.dp, vertical = 12.dp),
			contentAlignment = Alignment.CenterStart
		) {
			Column {
				// Budget amount
				Text(
					text = budget, style = TextStyle(
						fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface
					)
				)

				Spacer(modifier = GlanceModifier.height(2.dp))

				// Label
				Text(
					text = "Total Budget", style = TextStyle(
						color = GlanceTheme.colors.onSurfaceVariant
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
					color = GlanceTheme.colors.onSurfaceVariant
				)
			)

			Spacer(modifier = GlanceModifier.width(8.dp))

			// Arrow line with days chip
			Box(
				modifier = GlanceModifier.defaultWeight().height(20.dp),
				contentAlignment = Alignment.Center
			) {
				// Horizontal line (arrow body)
				Box(
					modifier = GlanceModifier.fillMaxWidth().height(2.dp)
						.background(GlanceTheme.colors.onSurfaceVariant)
				) {}

				// Arrow head as text
				Box(
					modifier = GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd
				) {
					Text(
						text = "→", style = TextStyle(
							color = GlanceTheme.colors.onSurfaceVariant
						)
					)
				}

				// Days count chip positioned above the arrow
				if (daysCount != "-") {
					Box(
						modifier = GlanceModifier.fillMaxWidth().padding(bottom = 2.dp),
						contentAlignment = Alignment.TopCenter
					) {
						Box(
							modifier = GlanceModifier.background(GlanceTheme.colors.onSurfaceVariant)
								.padding(horizontal = 8.dp, vertical = 2.dp)
						) {
							Text(
								text = daysCount, style = TextStyle(
									color = GlanceTheme.colors.surface, textAlign = TextAlign.Center
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
					color = GlanceTheme.colors.onSurfaceVariant
				)
			)
		}
	}
}

// Helper to update widget data
suspend fun updateBudgetOverviewWidget(
	context: Context, budget: String, startDate: Date, endDate: Date, daysCount: Int
) {
	val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

	val manager = GlanceAppWidgetManager(context)
	val glanceIds = manager.getGlanceIds(BudgetOverviewWidget::class.java)

	glanceIds.forEach { glanceId ->
		updateAppWidgetState(context, glanceId) { prefs ->
			prefs[stringPreferencesKey("budget")] = budget
			prefs[stringPreferencesKey("start_date")] = dateFormat.format(startDate)
			prefs[stringPreferencesKey("end_date")] = dateFormat.format(endDate)
			prefs[stringPreferencesKey("days_count")] = "$daysCount día(s)"
		}
		BudgetOverviewWidget().update(context, glanceId)
	}
}
