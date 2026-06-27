package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.presentation.ui.onboarding.periodLabel
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorBad
import com.serranoie.app.minus.presentation.ui.theme.colorGood
import com.serranoie.app.minus.presentation.ui.theme.colorNotGood
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed
import com.serranoie.app.minus.presentation.util.calcAdaptiveFont
import com.serranoie.app.minus.presentation.util.censor
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BudgetPill(
    budgetState: BudgetState?,
    budgetSettings: BudgetSettings? = null,
    viewPeriod: BudgetPeriod = budgetSettings?.period ?: BudgetPeriod.DAILY,
    currencyCode: String,
    onOpenSettings: () -> Unit = {},
    onOpenBudgetSheet: () -> Unit = {},
    bigVariant: Boolean = false,
    centerRemainingAmount: Boolean = false,
    modifier: Modifier = Modifier,
) {

    val currencyFormat = symbolOnlyCurrencyFormat(currencyCode)

    val dailyBudget = budgetState?.dailyBudget ?: BigDecimal.ZERO
    val totalSpentInPeriod = budgetState?.totalSpentInPeriod ?: BigDecimal.ZERO
    val totalSpentToday = budgetState?.totalSpentToday ?: BigDecimal.ZERO

    val period = viewPeriod
    val dailyBudgetAmount = dailyBudget
    val weeklyBudgetAmount = dailyBudget.multiply(BigDecimal(7))
    val biweeklyBudgetAmount = dailyBudget.multiply(BigDecimal(14))
    val monthlyBudgetAmount = dailyBudget.multiply(BigDecimal(30))
    val dailySpent = totalSpentToday
    val periodSpentAggregate = totalSpentInPeriod
    val dailyRemainingAmount = dailyBudgetAmount.subtract(dailySpent)
    val weeklyRemainingAmount = weeklyBudgetAmount.subtract(periodSpentAggregate)
    val biweeklyRemainingAmount = biweeklyBudgetAmount.subtract(periodSpentAggregate)
    val monthlyRemainingAmount = monthlyBudgetAmount.subtract(periodSpentAggregate)
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

    val isCurrentPeriodOverBudget = periodRemaining < BigDecimal.ZERO
    val isDailyExhausted = dailyRemainingAmount <= BigDecimal.ZERO
    val isWeeklyExhausted = weeklyRemainingAmount <= BigDecimal.ZERO
    val isBiweeklyExhausted = biweeklyRemainingAmount <= BigDecimal.ZERO
    val exhaustedMessage = when (period) {
        BudgetPeriod.WEEKLY -> {
            if (weeklyRemainingAmount > BigDecimal.ZERO && isDailyExhausted) {
                stringResource(
                    R.string.budget_pill_exhausted_single,
                    stringResource(R.string.budget_pill_exhausted_daily_label)
                )
            } else null
        }

        BudgetPeriod.BIWEEKLY -> {
            if (biweeklyRemainingAmount > BigDecimal.ZERO) {
                val exhausted = buildList {
                    if (isDailyExhausted) add(stringResource(R.string.budget_pill_exhausted_daily_label))
                    if (isWeeklyExhausted) add(stringResource(R.string.budget_pill_exhausted_weekly_label))
                }
                when (exhausted.size) {
                    0 -> null
                    1 -> stringResource(R.string.budget_pill_exhausted_single, exhausted.first())
                    2 -> stringResource(
                        R.string.budget_pill_exhausted_double, exhausted[0], exhausted[1]
                    )

                    else -> null
                }
            } else null
        }

        BudgetPeriod.MONTHLY -> {
            if (monthlyRemainingAmount > BigDecimal.ZERO) {
                val exhausted = buildList {
                    if (isDailyExhausted) add(stringResource(R.string.budget_pill_exhausted_daily_label))
                    if (isWeeklyExhausted) add(stringResource(R.string.budget_pill_exhausted_weekly_label))
                    if (isBiweeklyExhausted) add(stringResource(R.string.budget_pill_exhausted_biweekly_label))
                }
                when (exhausted.size) {
                    0 -> null
                    1 -> stringResource(R.string.budget_pill_exhausted_single, exhausted.first())
                    2 -> stringResource(
                        R.string.budget_pill_exhausted_double, exhausted[0], exhausted[1]
                    )

                    else -> stringResource(
                        R.string.budget_pill_exhausted_triple,
                        exhausted[0],
                        exhausted[1],
                        exhausted[2]
                    )
                }
            } else null
        }

        else -> null
    }

    val showExhaustedMessage = exhaustedMessage != null
    val shouldCenterRemainingAmount =
        centerRemainingAmount && !isCurrentPeriodOverBudget && !bigVariant
    val spendProgress = if (periodBudget > BigDecimal.ZERO) {
        periodSpent.divide(periodBudget, 2, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
    } else 0f

    val containerColor = when {
        isCurrentPeriodOverBudget -> colorBad.let { if (bigVariant) it else it.copy(alpha = 0.15f) }
        spendProgress > 0.5f -> colorNotGood.copy(alpha = 0.15f)
        else -> if (bigVariant) MaterialTheme.colorScheme.secondaryContainer else colorGood.copy(
            alpha = 0.15f
        )
    }
//	val containerColor = MaterialTheme.colorScheme.surfaceVariant

    val contentColor = when {
        isCurrentPeriodOverBudget -> if (bigVariant) Color.White else colorBad
        spendProgress > 0.65f -> colorNotGood
        else -> if (bigVariant) MaterialTheme.colorScheme.onSecondaryContainer else colorGood
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (isCurrentPeriodOverBudget) 1f else spendProgress.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "progress"
    )
    val centeredAmountScale by animateFloatAsState(
        targetValue = if (shouldCenterRemainingAmount) 1.30f else 1f,
        animationSpec = tween(220),
        label = "centeredAmountScale"
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
                modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                // Background progress indicator
                if (!bigVariant) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
							.fillMaxSize()
							.clip(CircleShape),
                        color = contentColor.copy(alpha = 0.5f),
                        trackColor = Color.Transparent,
                        drawStopIndicator = {})

//					val density  = LocalDensity.current
//					val strokeWidthPx = with(density) { 28.dp.toPx() }

//					LinearWavyProgressIndicator(
//						progress = { animatedProgress },
//						modifier = Modifier
//							.fillMaxSize()
//							.padding(horizontal = 16.dp),
//						color = contentColor.copy(alpha = 0.65f),
//						trackColor = containerColor.copy(alpha = 0.15f),
//						trackStroke = Stroke(width = 36f, cap = StrokeCap.Round),
//						amplitude = { 1f },
//						wavelength = 48.dp,
//						stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
//					)
                }

                AnimatedContent(
                    targetState = shouldCenterRemainingAmount,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        if (targetState) {
                            (slideInHorizontally(animationSpec = tween(220)) { it / 5 } + fadeIn(
                                tween(180)
                            )) togetherWith (slideOutHorizontally(animationSpec = tween(180)) { -it / 5 } + fadeOut(
                                tween(120)
                            ))
                        } else {
                            (slideInHorizontally(animationSpec = tween(220)) { -it / 5 } + fadeIn(
                                tween(180)
                            )) togetherWith (slideOutHorizontally(animationSpec = tween(180)) { it / 5 } + fadeOut(
                                tween(120)
                            ))
                        }
                    },
                    label = "budgetPillContent",
                ) { centerAmount ->
                    if (centerAmount) {
                        Box(
                            modifier = Modifier
								.fillMaxSize()
								.padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            AdaptiveSingleLineText(
                                text = currencyFormat.format(periodRemaining),
                                style = MaterialTheme.typography.titleMediumCondensed,
                                color = contentColor,
                                minFontSize = 16.sp,
                                modifier = Modifier
									.fillMaxWidth()
									.graphicsLayer {
										scaleX = centeredAmountScale
										scaleY = centeredAmountScale
									}
									.censor(),
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
								.fillMaxSize()
								.padding(horizontal = if (isCurrentPeriodOverBudget || bigVariant) 0.dp else 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (isCurrentPeriodOverBudget || bigVariant) Arrangement.Center else Arrangement.spacedBy(
                                8.dp
                            )
                        ) {
                            StatusLabel(
                                budgetState = budgetState,
                                budgetPeriod = period,
                                isOverBudget = isCurrentPeriodOverBudget,
                                exhaustedMessage = exhaustedMessage,
                                bigVariant = bigVariant,
                                modifier = if (isCurrentPeriodOverBudget || bigVariant) Modifier.padding(
                                    horizontal = 32.dp
                                ) else Modifier.weight(1f),
                            )

                            if (!isCurrentPeriodOverBudget && !bigVariant) {
                                AdaptiveSingleLineText(
                                    text = currencyFormat.format(periodRemaining),
                                    style = MaterialTheme.typography.titleMediumCondensed,
                                    color = contentColor,
                                    minFontSize = 16.sp,
                                    modifier = Modifier
										.weight(0.45f)
										.censor(),
                                    textAlign = TextAlign.End,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLabel(
    budgetState: BudgetState?,
    budgetPeriod: BudgetPeriod = BudgetPeriod.DAILY,
    isOverBudget: Boolean,
    exhaustedMessage: String? = null,
    bigVariant: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val textColor = LocalContentColor.current

    val label = when {
        isOverBudget -> stringResource(R.string.budget_pill_over_budget)
        budgetState == null -> stringResource(R.string.budget_pill_no_budget)
        else -> budgetPeriod.periodLabel()
    }

    val textStartOffset by animateDpAsState(
        label = "textStartOffset",
        targetValue = 0.dp,
        animationSpec = tween(250),
    )
    val labelVerticalOffset by animateDpAsState(
        label = "labelVerticalOffset",
        targetValue = if (exhaustedMessage != null && !bigVariant) (-2).dp else 0.dp,
        animationSpec = tween(300),
    )

    Column(
        modifier = modifier
			.height(if (bigVariant) 72.dp else 44.dp)
			.animateContentSize(animationSpec = tween(300)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = if (bigVariant) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Row(
            modifier = Modifier
				.fillMaxWidth()
				.offset(y = labelVerticalOffset),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(textStartOffset))
            AdaptiveSingleLineText(
                text = label,
                style = if (bigVariant) {
                    MaterialTheme.typography.titleMediumEmphasized
                } else if (isOverBudget) {
                    MaterialTheme.typography.titleMediumEmphasized
                } else {
                    MaterialTheme.typography.titleMediumCondensed
                },
                color = textColor,
                minFontSize = if (bigVariant) 14.sp else 12.sp,
                modifier = Modifier.weight(1f),
                textAlign = if (bigVariant) TextAlign.Center else TextAlign.Start,
            )
        }

        AnimatedVisibility(
            visible = exhaustedMessage != null && !bigVariant, enter = slideInVertically(
                initialOffsetY = { -it }, animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        ) {
            Text(
                text = exhaustedMessage.orEmpty(),
                style = MaterialTheme.typography.labelSmallCondensed,
                color = colorBad,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(),
            )
        }
    }
}

@Composable
private fun AdaptiveSingleLineText(
    text: String,
    style: TextStyle,
    color: Color,
    minFontSize: TextUnit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val availableWidth = with(density) { maxWidth.toPx() }
        val maxFontSize = style.fontSize.takeIf { it != TextUnit.Unspecified }
            ?: MaterialTheme.typography.bodyLarge.fontSize
        val adaptiveFontSize = calcAdaptiveFont(
            height = with(density) { maxFontSize.toPx() },
            width = availableWidth,
            minFontSize = minFontSize,
            maxFontSize = maxFontSize,
            text = text,
            style = style,
        )

        Text(
            text = text,
            style = style.copy(fontSize = adaptiveFontSize),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(name = "Healthy status")
@Composable
private fun PreviewBudgetPill() {
    MinusTheme {
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
}

@Preview(name = "Editing Centered", widthDp = 240, fontScale = 1.8f)
@Composable
private fun PreviewBudgetPillEditingCentered() {
    MinusTheme {
        BudgetPill(
            budgetState = BudgetState(
                remainingToday = BigDecimal("15200.62"),
                totalSpentToday = BigDecimal("80.50"),
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
            centerRemainingAmount = true,
            modifier = Modifier.fillMaxWidth(),
            onOpenSettings = { },
            onOpenBudgetSheet = { },
        )
    }
}

@Preview(name = "Caution")
@Composable
private fun PreviewBudgetPillCaution() {
    MinusTheme {
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
}


@Preview(name = "Bad status")
@Composable
private fun PreviewBudgetPillBad() {
    MinusTheme {
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
}

@Preview(name = "Weekly with Daily Exhausted")
@Composable
private fun PreviewBudgetPillWeeklyDailyExhausted() {
    MinusTheme {
        BudgetPill(
            budgetState = BudgetState(
                remainingToday = BigDecimal("-50.00"),
                totalSpentToday = BigDecimal("150.00"),
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
}