package com.serranoie.app.minus.presentation.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.PeriodOptionChip
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date

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

fun availablePeriodsFor(totalDays: Int): List<BudgetPeriod> = buildList {
	add(BudgetPeriod.DAILY)
	if (totalDays >= 7) add(BudgetPeriod.WEEKLY)
	if (totalDays >= 14) add(BudgetPeriod.BIWEEKLY)
	if (totalDays >= 30) add(BudgetPeriod.MONTHLY)
}

/**
 * Static split: how much you can spend per period if the totalBudget is divided
 * equally across all periods in the range. Unaffected by how much you've already spent.
 *
 * Kept for the onboarding flow where the period hasn't started yet, so dynamic
 * has nothing meaningful to compute. Use [splitBudget] for everything else.
 */
fun budgetForPeriod(
	totalBudget: BigDecimal,
	totalDays: Int,
	period: BudgetPeriod,
): BigDecimal {
	if (totalBudget == BigDecimal.ZERO || totalDays <= 0) return BigDecimal.ZERO

	val periodDays = period.toDays()
	val numPeriods = totalDays / periodDays

	return totalBudget.divide(BigDecimal(numPeriods), 2, RoundingMode.HALF_UP)
}

/**
 * Per-period budget amount, honoring the [BudgetSplitMode].
 *
 * - STATIC: totalBudget / (totalDays / period.toDays()).
 * - DYNAMIC: (totalBudget - totalSpent) / daysRemaining, then multiplied by the
 *   period's day count. Falls back to 0 if [daysRemaining] is 0 (period ended)
 *   or if the remaining balance is non-positive (don't show negative numbers).
 */
fun splitBudget(
	totalBudget: BigDecimal,
	totalSpent: BigDecimal,
	totalDays: Int,
	daysRemaining: Int,
	period: BudgetPeriod,
	mode: com.serranoie.app.minus.domain.model.BudgetSplitMode,
): BigDecimal {
	if (totalBudget == BigDecimal.ZERO || totalDays <= 0) return BigDecimal.ZERO
	return when (mode) {
		com.serranoie.app.minus.domain.model.BudgetSplitMode.STATIC ->
			budgetForPeriod(totalBudget, totalDays, period)

		com.serranoie.app.minus.domain.model.BudgetSplitMode.DYNAMIC -> {
			if (daysRemaining <= 0) return BigDecimal.ZERO
			val remaining = totalBudget.subtract(totalSpent)
			if (remaining <= BigDecimal.ZERO) return BigDecimal.ZERO
			val daily = remaining.divide(BigDecimal(daysRemaining), 2, RoundingMode.HALF_UP)
			daily.multiply(BigDecimal(period.toDays()))
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishDateSelector(
	selectDate: Date? = null,
	totalBudget: BigDecimal = BigDecimal.ZERO,
	currencyCode: String = "USD",
	onBackPressed: () -> Unit,
	onApply: (startDate: LocalDate, endDate: LocalDate, period: BudgetPeriod) -> Unit,
) {
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

	LaunchedEffect(startDate, endDate, available) {
		if (selectedPeriod == null && available.isNotEmpty()) {
			selectedPeriod = available.first()
		} else if (selectedPeriod !in available) {
			selectedPeriod = available.firstOrNull()
		}
	}

	var pickerVisible by remember { mutableStateOf(false) }
	LaunchedEffect(Unit) {
		pickerVisible = true
	}

	Surface(
		modifier = Modifier
			.fillMaxSize()
			.systemBarsPadding(),
	) {
		Column(modifier = Modifier.fillMaxSize()) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 8.dp, vertical = 4.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				IconButton(onClick = onBackPressed) {
					Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
				}
				Spacer(Modifier.weight(1f))
				Column(horizontalAlignment = Alignment.CenterHorizontally) {
					Text(
						text = stringResource(R.string.onboarding_finish_date_selector_title),
						style = MaterialTheme.typography.titleSmallEmphasized,
					)
					if (totalDays > 0) {
						Text(
							text = stringResource(
								R.string.onboarding_finish_date_selector_days_range,
								totalDays,
								startDate?.format(dateFormatter).orEmpty(),
								endDate?.format(dateFormatter).orEmpty(),
							),
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
				) { Text(stringResource(R.string.apply), style = MaterialTheme.typography.labelMediumEmphasized) }
			}

			AnimatedVisibility(
				visible = pickerVisible,
				enter = fadeIn(animationSpec = tween(300, delayMillis = 50)) +
					scaleIn(initialScale = 0.95f, animationSpec = tween(300, delayMillis = 50)),
			) {
				DateRangePicker(
					state = dateRangePickerState,
					colors = DatePickerDefaults.colors(
						titleContentColor = MaterialTheme.colorScheme.onSurface,
						headlineContentColor = MaterialTheme.colorScheme.onSurface,
						containerColor = MaterialTheme.colorScheme.surface,
					),
					modifier = Modifier
						.fillMaxWidth()
						.weight(1f),
					title = null,
					headline = null,
					showModeToggle = false,
				)
			}

			if (hasRange && available.isNotEmpty()) {
				HorizontalDivider()
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 16.dp, vertical = 12.dp),
				) {
					Text(
						text = stringResource(R.string.onboarding_finish_date_selector_budget_view_question),
						style = MaterialTheme.typography.titleSmall,
						modifier = Modifier.padding(bottom = 10.dp),
					)
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(8.dp),
					) {
						available.forEach { period ->
							val preview = if (totalBudget > BigDecimal.ZERO) {
								// Onboarding is pre-period, so the only meaningful view
								// is the static split. Dynamic degenerates to the same
								// number once the period starts, so we hard-code STATIC
								// here to avoid showing misleading previews.
								splitBudget(
									totalBudget = totalBudget,
									totalSpent = BigDecimal.ZERO,
									totalDays = totalDays,
									daysRemaining = totalDays,
									period = period,
									mode = BudgetSplitMode.STATIC,
								)
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

@Preview(
	showBackground = true,
	device = Devices.NEXUS_5X,
	widthDp = 412,
	heightDp = 860,
)
@Composable
private fun FinishDateSelectorPreview() {
	MinusTheme {
		Surface(
			modifier = Modifier.fillMaxSize(),
			color = MaterialTheme.colorScheme.background,
		) {
			FinishDateSelector(
				totalBudget = BigDecimal("5000"),
				currencyCode = "USD",
				onBackPressed = {},
				onApply = { start, end, period ->
					println("Apply: start=$start, end=$end, period=$period")
				},
			)
		}
	}
}
