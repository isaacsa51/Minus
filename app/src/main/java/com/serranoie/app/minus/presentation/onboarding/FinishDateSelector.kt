package com.serranoie.app.minus.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.LocalWindowInsets
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.presentation.ui.theme.component.LocalBottomSheetScrollState
import com.serranoie.app.minus.presentation.ui.theme.component.PeriodOptionChip
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date

const val FINISH_DATE_SELECTOR_SHEET = "finishDateSelector"

fun BudgetPeriod.toDays(): Int = when (this) {
	BudgetPeriod.DAILY -> 1
	BudgetPeriod.WEEKLY -> 7
	BudgetPeriod.BIWEEKLY -> 14
	BudgetPeriod.MONTHLY -> 30
}

@Composable
@ReadOnlyComposable
fun BudgetPeriod.label(): String = when (this) {
	BudgetPeriod.DAILY -> stringResource(R.string.budget_period_daily)
	BudgetPeriod.WEEKLY -> stringResource(R.string.budget_period_weekly)
	BudgetPeriod.BIWEEKLY -> stringResource(R.string.budget_period_biweekly)
	BudgetPeriod.MONTHLY -> stringResource(R.string.budget_period_monthly)
}

@Composable
@ReadOnlyComposable
fun BudgetPeriod.periodLabel(): String = when (this) {
	BudgetPeriod.DAILY -> stringResource(R.string.budget_period_label_today)
	BudgetPeriod.WEEKLY -> stringResource(R.string.budget_period_label_this_week)
	BudgetPeriod.BIWEEKLY -> stringResource(R.string.budget_period_label_this_biweek)
	BudgetPeriod.MONTHLY -> stringResource(R.string.budget_period_label_this_month)
}

/**
 * Returns the list of budget periods that can fit within the given [totalDays].
 * Only periods where at least one full period fits are included.
 *
 * Example: totalDays = 7
 *  - DAILY → included (1 day fits in 7)
 *  - WEEKLY → included (7 days fits in 7 exactly)
 *  - BIWEEKLY → excluded (14 days > 7)
 *  - MONTHLY → excluded (30 days > 7)
 */
fun availablePeriodsFor(totalDays: Int): List<BudgetPeriod> = buildList {
	add(BudgetPeriod.DAILY)
	if (totalDays >= 7) add(BudgetPeriod.WEEKLY)
	if (totalDays >= 14) add(BudgetPeriod.BIWEEKLY)
	if (totalDays >= 30) add(BudgetPeriod.MONTHLY)
}

/**
 * Calculates the budget amount for a specific period type given the total budget
 * distributed over [totalDays] days.
 *
 * The calculation determines how much budget is available per period by:
 * 1. Calculating how many full periods fit in the date range: floor(totalDays / periodDays)
 * 2. Dividing total budget by that number of periods
 *
 * Examples with totalBudget = $1000, totalDays = 7:
 *  - DAILY   → $1000 / 7 days = $142.86 per day
 *  - WEEKLY  → $1000 / 1 week = $1000 per week (7 days = 1 full week)
 *
 * With totalBudget = $1000, totalDays = 15:
 *  - DAILY   → $1000 / 15 = $66.67 per day
 *  - WEEKLY  → $1000 / 2 weeks (floor(15/7)) = $500 per week
 *  - BIWEEKLY → $1000 / 1 = $1000 per 2-week period
 *
 * @param totalBudget The total budget amount for the entire date range
 * @param totalDays Number of days in the budget period
 * @param period The period type to calculate budget for
 * @return The budget amount per that period type
 */
fun budgetForPeriod(
	totalBudget: BigDecimal,
	totalDays: Int,
	period: BudgetPeriod,
): BigDecimal {
	if (totalBudget == BigDecimal.ZERO || totalDays <= 0) return BigDecimal.ZERO
	
	val periodDays = period.toDays()
	// Calculate how many full periods fit in the total days (floor division)
	val numPeriods = totalDays / periodDays
	
	return totalBudget.divide(BigDecimal(numPeriods), 2, RoundingMode.HALF_UP)
}

/**
 * Two-step date-range + period selector sheet.
 *
 * Step 1 — [DateRangePicker]: user picks start → end date.
 * Step 2 — Period chips (inline, below the picker): user chooses how the budget
 *           should be split (Daily / Weekly / …).  Options that require more days
 *           than the selected range are hidden.
 *
 * @param totalBudget  Pass the current budget to show a "preview" amount on each chip.
 * @param currencyCode ISO 4217 code used when formatting the preview amounts.
 * @param onApply      Callback with the chosen start date, end date and period.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishDateSelector(
	selectDate: Date? = null,
	totalBudget: BigDecimal = BigDecimal.ZERO,
	currencyCode: String = "USD",
	onBackPressed: () -> Unit,
	onApply: (startDate: LocalDate, endDate: LocalDate, period: BudgetPeriod) -> Unit,
) {
	val localBottomSheetScrollState = LocalBottomSheetScrollState.current
	val windowInsets = LocalWindowInsets.current
	val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM") }

	val dateRangePickerState = rememberDateRangePickerState()

	val startDate: LocalDate? = remember(dateRangePickerState.selectedStartDateMillis) {
		dateRangePickerState.selectedStartDateMillis?.let { LocalDate.ofEpochDay(it / 86_400_000) }
	}
	val endDate: LocalDate? = remember(dateRangePickerState.selectedEndDateMillis) {
		dateRangePickerState.selectedEndDateMillis?.let { LocalDate.ofEpochDay(it / 86_400_000) }
	}
	val hasRange = startDate != null && endDate != null
	val totalDays = if (hasRange) ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1 else 0
	val available = if (totalDays > 0) availablePeriodsFor(totalDays) else emptyList()

	var selectedPeriod by remember { mutableStateOf<BudgetPeriod?>(null) }
	LaunchedEffect(startDate, endDate) { selectedPeriod = null }

	Surface(
		modifier = Modifier
			.fillMaxSize()
			.padding(
				top = maxOf(
					windowInsets.calculateTopPadding(), localBottomSheetScrollState.topPadding
				)
			),
	) {
		Column(modifier = Modifier.fillMaxSize()) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 8.dp, vertical = 4.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				IconButton(onClick = onBackPressed) {
					Icon(Icons.Default.Close, contentDescription = "Cerrar")
				}
				Spacer(Modifier.weight(1f))
				Column(horizontalAlignment = Alignment.CenterHorizontally) {
					Text(
						text = "Selecciona el período",
						style = MaterialTheme.typography.titleMedium,
					)
					if (totalDays > 0) {
						Text(
							text = "$totalDays días" + " · ${
								startDate?.format(
									dateFormatter
								)
							} - ${
								endDate?.format(
									dateFormatter
								)
							}",
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
						)
					}
				}
				Spacer(Modifier.weight(1f))
				Button(
					onClick = {
						if (selectedPeriod != null && startDate != null && endDate != null) {
							onApply(startDate, endDate, selectedPeriod!!)
						}
					},
					enabled = hasRange && selectedPeriod != null,
				) { Text("Aplicar") }
			}

			DateRangePicker(
				state = dateRangePickerState,
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f),
				title = null,
				headline = null,
				showModeToggle = false,
			)

			if (hasRange && available.isNotEmpty()) {
				HorizontalDivider()
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 16.dp, vertical = 12.dp),
				) {
					Text(
						text = "¿Cómo quieres ver tu presupuesto?",
						style = MaterialTheme.typography.titleSmall,
						modifier = Modifier.padding(bottom = 10.dp),
					)
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(8.dp),
					) {
						available.forEach { period ->
							val preview = if (totalBudget > BigDecimal.ZERO) {
								budgetForPeriod(totalBudget, totalDays, period)
							} else null

							PeriodOptionChip(
								period = period,
								budgetPreview = preview,
								currencyCode = currencyCode,
								isSelected = selectedPeriod == period,
								onClick = { selectedPeriod = period },
								modifier = Modifier.weight(1f),
							)
						}
					}
				}
			}
		}
	}
}
