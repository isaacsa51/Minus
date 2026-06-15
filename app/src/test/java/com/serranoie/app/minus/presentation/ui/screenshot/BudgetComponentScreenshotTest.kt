package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.budget.BudgetDisplay
import com.serranoie.app.minus.presentation.ui.theme.component.budget.SpendBudgetCard
import com.serranoie.app.minus.presentation.ui.theme.component.date.DaysLeftCard
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

class BudgetComponentScreenshotTest {
	@get:Rule
	val paparazzi = Paparazzi(
		deviceConfig = DeviceConfig.PIXEL_5,
		renderingMode = SessionParams.RenderingMode.SHRINK,
	)

	@Test
	fun budgetDisplayHealthyBudget() {
		Locale.setDefault(Locale.US)

		paparazzi.snapshot {
			MinusTheme {
				BudgetDisplay(
					budget = BigDecimal("500.00"),
					budgetState = BudgetState(
						remainingToday = BigDecimal("45.50"),
						totalSpentToday = BigDecimal("12.50"),
						dailyBudget = BigDecimal("58.00"),
						daysRemaining = 15,
						progress = 0.21f,
						isOverBudget = false,
						totalBudget = BigDecimal("500.00"),
						totalSpentInPeriod = BigDecimal("100.00"),
					),
					budgetSettings = BudgetSettings(
						totalBudget = BigDecimal("500.00"),
						period = BudgetPeriod.MONTHLY,
						startDate = LocalDate.of(2026, 1, 1),
						endDate = LocalDate.of(2026, 1, 30),
						currencyCode = "USD",
						daysInPeriod = 30,
					),
					currencyCode = "USD",
					startDate = fixedDate(2026, 1, 1),
					finishDate = fixedDate(2026, 1, 30),
				)
			}
		}
	}

	@Test
	fun budgetDisplayOverBudget() {
		Locale.setDefault(Locale.US)

		paparazzi.snapshot {
			MinusTheme {
				BudgetDisplay(
					budget = BigDecimal("300.00"),
					budgetState = BudgetState(
						remainingToday = BigDecimal("-15.30"),
						totalSpentToday = BigDecimal("73.30"),
						dailyBudget = BigDecimal("58.00"),
						daysRemaining = 3,
						progress = 1.15f,
						isOverBudget = true,
						totalBudget = BigDecimal("300.00"),
						totalSpentInPeriod = BigDecimal("345.30"),
					),
					budgetSettings = BudgetSettings(
						totalBudget = BigDecimal("300.00"),
						period = BudgetPeriod.WEEKLY,
						startDate = LocalDate.of(2026, 1, 1),
						endDate = LocalDate.of(2026, 1, 7),
						currencyCode = "USD",
						daysInPeriod = 7,
					),
					currencyCode = "USD",
					startDate = fixedDate(2026, 1, 1),
					finishDate = fixedDate(2026, 1, 7),
					actualFinishDate = fixedDate(2026, 1, 4),
				)
			}
		}
	}

	@Test
	fun budgetOverviewCards() {
		Locale.setDefault(Locale.US)

		paparazzi.snapshot {
			MinusTheme {
				Column(modifier = Modifier.padding(16.dp)) {
					SpendBudgetCard(
						modifier = Modifier.height(IntrinsicSize.Min),
						budget = BigDecimal("60000"),
						spend = BigDecimal("30740"),
					)
					Spacer(modifier = Modifier.height(16.dp))
					DaysLeftCard(
						startDate = fixedDate(2026, 1, 1),
						finishDate = fixedDate(2026, 1, 30),
					)
				}
			}
		}
	}

	private fun fixedDate(year: Int, month: Int, day: Int): Date {
		return Date.from(
			LocalDate.of(year, month, day)
				.atStartOfDay()
				.toInstant(ZoneOffset.UTC)
		)
	}
}
