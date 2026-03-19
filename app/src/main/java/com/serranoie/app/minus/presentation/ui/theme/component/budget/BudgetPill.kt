package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.presentation.onboarding.periodLabel
import com.serranoie.app.minus.presentation.ui.theme.colorBad
import com.serranoie.app.minus.presentation.ui.theme.colorGood
import com.serranoie.app.minus.presentation.ui.theme.colorNotGood
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Composable
fun BudgetPill(
	budgetState: BudgetState?,
	budgetSettings: BudgetSettings? = null,
	viewPeriod: BudgetPeriod = budgetSettings?.period ?: BudgetPeriod.DAILY,
	currencyCode: String,
	onOpenSettings: () -> Unit,
	onOpenBudgetSheet: () -> Unit = {},
	modifier: Modifier = Modifier,
) {

	val currencyFormat =
		com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat(currencyCode)

	val dailyBudget = budgetState?.dailyBudget ?: BigDecimal.ZERO
	val totalSpentInPeriod = budgetState?.totalSpentInPeriod ?: BigDecimal.ZERO
	val totalSpentToday = budgetState?.totalSpentToday ?: BigDecimal.ZERO

	// Use the viewPeriod parameter instead of reading from budgetSettings
	val period = viewPeriod

	// Calculate budgets for different periods based on daily budget
	val dailyBudgetAmount = dailyBudget
	val weeklyBudgetAmount = dailyBudget.multiply(BigDecimal(7))
	val biweeklyBudgetAmount = dailyBudget.multiply(BigDecimal(14))
	val monthlyBudgetAmount = dailyBudget.multiply(BigDecimal(30))

	// Spent values
	val dailySpent = totalSpentToday
	val periodSpentAggregate = totalSpentInPeriod

	// Calculate remaining for each split option
	val dailyRemainingAmount = dailyBudgetAmount.subtract(dailySpent)
	val weeklyRemainingAmount = weeklyBudgetAmount.subtract(periodSpentAggregate)
	val biweeklyRemainingAmount = biweeklyBudgetAmount.subtract(periodSpentAggregate)
	val monthlyRemainingAmount = monthlyBudgetAmount.subtract(periodSpentAggregate)

	// Determine which budget values to use based on selected view period
	val (periodBudget, periodSpent, periodRemaining) = when (period) {
		BudgetPeriod.DAILY -> Triple(dailyBudgetAmount, dailySpent, dailyRemainingAmount)
		BudgetPeriod.WEEKLY -> Triple(
			weeklyBudgetAmount, periodSpentAggregate, weeklyRemainingAmount
		)

		BudgetPeriod.BIWEEKLY -> Triple(
			biweeklyBudgetAmount, periodSpentAggregate, biweeklyRemainingAmount
		)

		BudgetPeriod.MONTHLY -> Triple(
			monthlyBudgetAmount, periodSpentAggregate, monthlyRemainingAmount
		)
	}

	// Check if current period is over budget
	val isCurrentPeriodOverBudget = periodRemaining < BigDecimal.ZERO

	val isDailyExhausted = dailyRemainingAmount <= BigDecimal.ZERO
	val isWeeklyExhausted = weeklyRemainingAmount <= BigDecimal.ZERO
	val isBiweeklyExhausted = biweeklyRemainingAmount <= BigDecimal.ZERO

	// Determine exhausted message for smaller splits of the selected period
	val exhaustedMessage = when (period) {
		BudgetPeriod.WEEKLY -> {
			if (weeklyRemainingAmount > BigDecimal.ZERO && isDailyExhausted) {
				"Presupuesto diario agotado"
			} else null
		}

		BudgetPeriod.BIWEEKLY -> {
			if (biweeklyRemainingAmount > BigDecimal.ZERO) {
				val exhausted = buildList {
					if (isDailyExhausted) add("diario")
					if (isWeeklyExhausted) add("semanal")
				}
				when (exhausted.size) {
					0 -> null
					1 -> "Presupuesto ${exhausted.first()} agotado"
					2 -> "Presupuesto ${exhausted[0]} y ${exhausted[1]} agotado"
					else -> null
				}
			} else null
		}

		BudgetPeriod.MONTHLY -> {
			if (monthlyRemainingAmount > BigDecimal.ZERO) {
				val exhausted = buildList {
					if (isDailyExhausted) add("diario")
					if (isWeeklyExhausted) add("semanal")
					if (isBiweeklyExhausted) add("quincenal")
				}
				when (exhausted.size) {
					0 -> null
					1 -> "Presupuesto ${exhausted.first()} agotado"
					2 -> "Presupuesto ${exhausted[0]} y ${exhausted[1]} agotado"
					else -> "Presupuesto ${exhausted[0]}, ${exhausted[1]} y ${exhausted[2]} agotado"
				}
			} else null
		}

		else -> null
	}

	// Show exhausted message if current period is over budget but we're showing a longer period with budget
	val showExhaustedMessage = exhaustedMessage != null

	// Calculate progress based on the selected period
	val spendProgress = if (periodBudget > BigDecimal.ZERO) {
		periodSpent.divide(periodBudget, 2, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
	} else 0f

	val containerColor = when {
		isCurrentPeriodOverBudget -> colorBad.copy(alpha = 0.25f)
		spendProgress > 0.65f -> colorNotGood.copy(alpha = 0.25f)
		else -> colorGood.copy(alpha = 0.25f)
	}

	val contentColor = when {
		isCurrentPeriodOverBudget -> colorBad
		spendProgress > 0.65f -> colorNotGood
		else -> colorGood
	}

	val animatedProgress by animateFloatAsState(
		targetValue = if (isCurrentPeriodOverBudget) 1f else spendProgress.coerceIn(0f, 1f),
		animationSpec = tween(500),
		label = "progress"
	)

	Column(
		modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally
	) {
		Card(
			modifier = Modifier.height(if (showExhaustedMessage) 50.dp else 50.dp),
			shape = CircleShape,
			colors = CardDefaults.cardColors(
				containerColor = containerColor,
				contentColor = contentColor,
			),
			onClick = onOpenBudgetSheet
		) {
			Box(
				modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center
			) {
				// Background progress indicator
				LinearProgressIndicator(
					progress = { animatedProgress },
					modifier = Modifier
						.fillMaxSize()
						.clip(CircleShape),
					color = contentColor.copy(alpha = 0.5f),
					trackColor = Color.Transparent,
					drawStopIndicator = {})

				Row(
					modifier = Modifier.fillMaxSize(),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.SpaceBetween
				) {
					// Status label on left
					StatusLabel(
						budgetState = budgetState,
						budgetPeriod = period,
						isOverBudget = isCurrentPeriodOverBudget,
						exhaustedMessage = exhaustedMessage,
						modifier = Modifier.padding(start = 18.dp),
					)

					Spacer(modifier = Modifier.weight(1f))

					// Value label on right
					Text(
						text = if (isCurrentPeriodOverBudget) {
							""
						} else {
							currencyFormat.format(periodRemaining)
						}, style = MaterialTheme.typography.titleMedium.copy(
							fontWeight = FontWeight.Bold
						), color = contentColor, modifier = Modifier.padding(end = 16.dp)
					)
				}
			}
		}
	}
}


/**
 * Status label showing budget period text.
 */
@Composable
private fun StatusLabel(
	budgetState: BudgetState?,
	budgetPeriod: BudgetPeriod = BudgetPeriod.DAILY,
	isOverBudget: Boolean,
	exhaustedMessage: String? = null,
	modifier: Modifier = Modifier,
) {
	val textColor = LocalContentColor.current

	val label = when {
		isOverBudget -> "Presupuesto agotado"
		budgetState == null -> "Sin presupuesto"
		else -> budgetPeriod.periodLabel()
	}

	val textStartOffset by animateDpAsState(
		label = "textStartOffset",
		targetValue = if (isOverBudget) 44.dp else 0.dp,
		animationSpec = tween(250),
	)
	val labelVerticalOffset by animateDpAsState(
		label = "labelVerticalOffset",
		targetValue = if (exhaustedMessage != null) (-2).dp else 0.dp,
		animationSpec = tween(300),
	)

	Column(
		modifier = modifier
			.height(if (exhaustedMessage != null) 44.dp else 44.dp)
			.animateContentSize(animationSpec = tween(300)),
		verticalArrangement = Arrangement.Center,
	) {
		Row(
			modifier = Modifier.offset(y = labelVerticalOffset),
			verticalAlignment = Alignment.CenterVertically
		) {
			Spacer(modifier = Modifier.width(textStartOffset))
			Text(
				text = label,
				style = MaterialTheme.typography.titleMedium.copy(
					fontSize = MaterialTheme.typography.titleMedium.fontSize
				),
				color = textColor,
				overflow = TextOverflow.Ellipsis,
				softWrap = false,
			)
		}

		AnimatedVisibility(
			visible = exhaustedMessage != null, enter = slideInVertically(
				initialOffsetY = { -it }, animationSpec = tween(300)
			) + fadeIn(animationSpec = tween(300))
		) {
			Text(
				text = exhaustedMessage.orEmpty(),
				style = MaterialTheme.typography.labelSmall,
				color = colorBad,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
			)
		}
	}
}


@Preview(name = "BudgetPill")
@Composable
private fun PreviewBudgetPill() {
	BudgetPill(
		budgetState = BudgetState(
			remainingToday = BigDecimal("110.00"),
			totalSpentToday = BigDecimal("12.50"),
			dailyBudget = BigDecimal("122.50"),
			daysRemaining = 15,
			progress = 0.1f,
			isOverBudget = false,
			totalBudget = BigDecimal("500.00"),
			totalSpentInPeriod = BigDecimal("12.50")
		),
		budgetSettings = BudgetSettings(
			totalBudget = BigDecimal("500.00"),
			period = BudgetPeriod.DAILY,
			startDate = LocalDate.now(),
			currencyCode = "MXN"
		),
		viewPeriod = BudgetPeriod.DAILY,
		currencyCode = "MXN",
		onOpenSettings = { },
		onOpenBudgetSheet = { },
	)
}

@Preview
@Composable
private fun PreviewBudgetPillCaution() {
	BudgetPill(
		budgetState = BudgetState(
			remainingToday = BigDecimal("110.00"),
			totalSpentToday = BigDecimal("85.50"),
			dailyBudget = BigDecimal("122.50"),
			daysRemaining = 15,
			progress = 0.1f,
			isOverBudget = false,
			totalBudget = BigDecimal("500.00"),
			totalSpentInPeriod = BigDecimal("12.50")
		),
		budgetSettings = BudgetSettings(
			totalBudget = BigDecimal("500.00"),
			period = BudgetPeriod.DAILY,
			startDate = LocalDate.now(),
			currencyCode = "MXN"
		),
		viewPeriod = BudgetPeriod.DAILY,
		currencyCode = "MXN",
		onOpenSettings = { },
		onOpenBudgetSheet = { },
	)
}


@Preview
@Composable
private fun PreviewBudgetPillBad() {
	BudgetPill(
		budgetState = BudgetState(
			remainingToday = BigDecimal("110.00"),
			totalSpentToday = BigDecimal("115.50"),
			dailyBudget = BigDecimal("110.50"),
			daysRemaining = 15,
			progress = 0.1f,
			isOverBudget = false,
			totalBudget = BigDecimal("500.00"),
			totalSpentInPeriod = BigDecimal("12.50")
		),
		budgetSettings = BudgetSettings(
			totalBudget = BigDecimal("500.00"),
			period = BudgetPeriod.DAILY,
			startDate = LocalDate.now(),
			currencyCode = "MXN"
		),
		viewPeriod = BudgetPeriod.DAILY,
		currencyCode = "MXN",
		onOpenSettings = { },
		onOpenBudgetSheet = { },
	)
}

@Preview(name = "BudgetPill Weekly with Daily Exhausted")
@Composable
private fun PreviewBudgetPillWeeklyDailyExhausted() {
	// Simulate scenario where daily budget is exhausted but weekly still has budget
	BudgetPill(
		budgetState = BudgetState(
			remainingToday = BigDecimal("-50.00"), // Daily over budget
			totalSpentToday = BigDecimal("150.00"), // Spent more than daily budget
			dailyBudget = BigDecimal("100.00"),
			daysRemaining = 15,
			progress = 0.1f,
			isOverBudget = false,
			totalBudget = BigDecimal("1500.00"),
			totalSpentInPeriod = BigDecimal("150.00")
		),
		budgetSettings = BudgetSettings(
			totalBudget = BigDecimal("1500.00"),
			period = BudgetPeriod.DAILY,
			startDate = LocalDate.now(),
			currencyCode = "MXN"
		),
		viewPeriod = BudgetPeriod.WEEKLY,
		currencyCode = "MXN",
		onOpenSettings = { },
		onOpenBudgetSheet = { },
	)
}