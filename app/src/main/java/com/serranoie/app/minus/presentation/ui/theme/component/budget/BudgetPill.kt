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
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.runtime.remember
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
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
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.harmonizeWithColor
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import com.serranoie.app.minus.presentation.util.toPaletteWithTheme
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
    onOpenBudgetSheet: () -> Unit = {},
    bigVariant: Boolean = false,
    centerRemainingAmount: Boolean = false,
    splitMode: BudgetSplitMode = BudgetSplitMode.STATIC,
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

    val periodAllocation = when (period) {
        BudgetPeriod.DAILY -> budgetState?.dailyAllocation ?: BigDecimal.ZERO
        BudgetPeriod.WEEKLY -> budgetState?.weeklyAllocation ?: BigDecimal.ZERO
        BudgetPeriod.BIWEEKLY -> budgetState?.biweeklyAllocation ?: BigDecimal.ZERO
        BudgetPeriod.MONTHLY -> budgetState?.monthlyAllocation ?: BigDecimal.ZERO
    }

    val (periodBudget, periodSpent, staticPeriodRemaining) = when (period) {
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

    val periodRemaining = when (splitMode) {
        BudgetSplitMode.DYNAMIC -> when (period) {
            BudgetPeriod.DAILY -> periodAllocation.subtract(dailySpent)
                .coerceAtLeast(BigDecimal.ZERO)

            else -> periodAllocation.subtract(periodSpentAggregate)
                .coerceAtLeast(BigDecimal.ZERO)
        }
        BudgetSplitMode.STATIC -> staticPeriodRemaining
    }

    val isCurrentPeriodOverBudget = when (splitMode) {
        BudgetSplitMode.DYNAMIC -> budgetState?.isOverBudget == true
        BudgetSplitMode.STATIC -> staticPeriodRemaining < BigDecimal.ZERO
    }
    val isOverDailyAllocation = budgetState?.isTodayOverDailyAllocation == true
    val weeklyAllocation = budgetState?.weeklyAllocation ?: BigDecimal.ZERO
    val biweeklyAllocation = budgetState?.biweeklyAllocation ?: BigDecimal.ZERO
    val monthlyAllocation = budgetState?.monthlyAllocation ?: BigDecimal.ZERO
    val isOverWeeklyAllocation = totalSpentInPeriod > weeklyAllocation && weeklyAllocation > BigDecimal.ZERO
    val isOverBiweeklyAllocation = totalSpentInPeriod > biweeklyAllocation && biweeklyAllocation > BigDecimal.ZERO
    val isOverMonthlyAllocation = totalSpentInPeriod > monthlyAllocation && monthlyAllocation > BigDecimal.ZERO

    val isOverCurrentSubPeriod = when (period) {
        BudgetPeriod.DAILY -> isOverDailyAllocation
        BudgetPeriod.WEEKLY -> isOverWeeklyAllocation
        BudgetPeriod.BIWEEKLY -> isOverBiweeklyAllocation
        BudgetPeriod.MONTHLY -> isOverMonthlyAllocation
    }

    val isDailyExhausted = dailyRemainingAmount <= BigDecimal.ZERO
    val isWeeklyExhausted = weeklyRemainingAmount <= BigDecimal.ZERO
    val isBiweeklyExhausted = biweeklyRemainingAmount <= BigDecimal.ZERO

    val staticExhaustedMessage = when (period) {
        BudgetPeriod.WEEKLY -> {
            if (weeklyRemainingAmount > BigDecimal.ZERO && isDailyExhausted) {
                stringResource(
                    R.string.budget_pill_exhausted_single,
                    stringResource(R.string.budget_pill_exhausted_daily_label)
                )
            } else {
                null
            }
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
            } else {
                null
            }
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
            } else {
                null
            }
        }

        else -> null
    }

    val dynamicExceededSubMessage = when (period) {
        BudgetPeriod.DAILY -> null
        BudgetPeriod.WEEKLY -> {
            if (!isOverWeeklyAllocation && isOverDailyAllocation) {
                stringResource(
                    R.string.budget_pill_sub_exceeded_single,
                    stringResource(R.string.budget_pill_exhausted_daily_label)
                )
            } else null
        }

        BudgetPeriod.BIWEEKLY -> {
            if (!isOverBiweeklyAllocation) {
                val overspent = buildList {
                    if (isOverDailyAllocation) add(stringResource(R.string.budget_pill_exhausted_daily_label))
                    if (isOverWeeklyAllocation) add(stringResource(R.string.budget_pill_exhausted_weekly_label))
                }
                when (overspent.size) {
                    0 -> null
                    1 -> stringResource(R.string.budget_pill_sub_exceeded_single, overspent.first())
                    2 -> stringResource(
                        R.string.budget_pill_sub_exceeded_double,
                        overspent[0], overspent[1]
                    )

                    else -> null
                }
            } else null
        }

        BudgetPeriod.MONTHLY -> {
            if (!isOverMonthlyAllocation) {
                val overspent = buildList {
                    if (isOverDailyAllocation) add(stringResource(R.string.budget_pill_exhausted_daily_label))
                    if (isOverWeeklyAllocation) add(stringResource(R.string.budget_pill_exhausted_weekly_label))
                    if (isOverBiweeklyAllocation) add(stringResource(R.string.budget_pill_exhausted_biweekly_label))
                }
                when (overspent.size) {
                    0 -> null
                    1 -> stringResource(R.string.budget_pill_sub_exceeded_single, overspent.first())
                    2 -> stringResource(
                        R.string.budget_pill_sub_exceeded_double,
                        overspent[0], overspent[1]
                    )

                    else -> stringResource(
                        R.string.budget_pill_sub_exceeded_triple,
                        overspent[0], overspent[1], overspent[2]
                    )
                }
            } else null
        }

        else -> null
    }

    val exhaustedMessage = when (splitMode) {
        BudgetSplitMode.STATIC -> staticExhaustedMessage
        BudgetSplitMode.DYNAMIC -> dynamicExceededSubMessage
    }

    val showExhaustedMessage = exhaustedMessage != null
    val shouldCenterRemainingAmount =
        centerRemainingAmount && !isCurrentPeriodOverBudget && !bigVariant
    val spendProgress = if (periodBudget > BigDecimal.ZERO) {
        periodSpent.divide(periodBudget, 2, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    val isDarkTheme = isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary
    val good = colorGood
    val notGood = colorNotGood
    val bad = colorBad
    val harmonizedColor = remember(spendProgress, primaryColor, isDarkTheme, good, notGood, bad) {
        val combined = combineColors(
            listOf(good, notGood, bad), spendProgress.coerceIn(0f, 1f)
        )
        val harmonized = harmonizeWithColor(combined, primaryColor)
        toPaletteWithTheme(harmonized, isDarkTheme)
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
                containerColor = harmonizedColor.container.copy(alpha = 0.6f),
                contentColor = harmonizedColor.onContainer,
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
                        color = harmonizedColor.main,
                        trackColor = Color.Transparent,
                        drawStopIndicator = {})

// 					val density  = LocalDensity.current
// 					val strokeWidthPx = with(density) { 28.dp.toPx() }

// 					LinearWavyProgressIndicator(
// 						progress = { animatedProgress },
// 						modifier = Modifier
// 							.fillMaxSize()
// 							.padding(horizontal = 16.dp),
// 						color = harmonizedColor.onContainer.copy(alpha = 0.65f),
// 						trackColor = harmonizedColor.container.copy(alpha = 0.15f),
// 						trackStroke = Stroke(width = 36f, cap = StrokeCap.Round),
// 						amplitude = { 1f },
// 						wavelength = 48.dp,
// 						stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
// 					)
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
                    val textColor = LocalContentColor.current
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
                                color = textColor,
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
                        val isCentered = isCurrentPeriodOverBudget || isOverCurrentSubPeriod || bigVariant
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = if (isCentered) 0.dp else 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (isCentered) {
                                Arrangement.Center
                            } else {
                                Arrangement.spacedBy(
                                    8.dp
                                )
                            }
                        ) {
                            StatusLabel(
                                budgetState = budgetState,
                                budgetPeriod = period,
                                isOverBudget = isCurrentPeriodOverBudget,
                                isOverSubPeriodAllocation = isOverCurrentSubPeriod,
                                exhaustedMessage = exhaustedMessage,
                                bigVariant = bigVariant,
                                splitMode = splitMode,
                                wrapContent = true,
                                modifier = when {
                                    isCentered -> Modifier.padding(
                                        horizontal = 32.dp
                                    )

                                    else -> Modifier.wrapContentWidth()
                                },
                            )

                            if (!isCurrentPeriodOverBudget && !isOverCurrentSubPeriod && !bigVariant) {
                                AdaptiveSingleLineText(
                                    text = currencyFormat.format(periodRemaining),
                                    style = MaterialTheme.typography.titleMediumCondensed,
                                    color = textColor,
                                    minFontSize = 16.sp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .censor(),
                                    textAlign = TextAlign.End
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
    isOverSubPeriodAllocation: Boolean = false,
    exhaustedMessage: String? = null,
    bigVariant: Boolean = false,
    splitMode: BudgetSplitMode = BudgetSplitMode.STATIC,
    wrapContent: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val textColor = LocalContentColor.current

    val label = when {
        isOverBudget -> stringResource(R.string.budget_pill_over_budget)
        budgetState == null -> stringResource(R.string.budget_pill_no_budget)
        isOverSubPeriodAllocation -> stringResource(
            when (budgetPeriod) {
                BudgetPeriod.DAILY -> R.string.budget_pill_label_daily_exceeded
                BudgetPeriod.WEEKLY -> R.string.budget_pill_label_weekly_exceeded
                BudgetPeriod.BIWEEKLY -> R.string.budget_pill_label_biweekly_exceeded
                BudgetPeriod.MONTHLY -> R.string.budget_pill_label_monthly_exceeded
            }
        )

        splitMode == BudgetSplitMode.DYNAMIC -> stringResource(
            when (budgetPeriod) {
                BudgetPeriod.DAILY -> R.string.budget_pill_label_per_daily
                BudgetPeriod.WEEKLY -> R.string.budget_pill_label_per_weekly
                BudgetPeriod.BIWEEKLY -> R.string.budget_pill_label_per_biweekly
                BudgetPeriod.MONTHLY -> R.string.budget_pill_label_per_monthly
            }
        )

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
                .then(if (wrapContent) Modifier.wrapContentWidth() else Modifier.fillMaxWidth())
                .offset(y = labelVerticalOffset), verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(textStartOffset))
            AdaptiveSingleLineText(
                text = label,
                style = if (bigVariant) {
                    MaterialTheme.typography.titleMediumEmphasized
                } else if (isOverBudget || isOverSubPeriodAllocation) {
                    MaterialTheme.typography.titleMediumEmphasized
                } else {
                    MaterialTheme.typography.titleMediumCondensed
                },
                color = textColor,
                minFontSize = if (bigVariant) 14.sp else 12.sp,
                modifier = if (wrapContent) Modifier.wrapContentWidth() else Modifier.weight(1f),
                textAlign = if (bigVariant) TextAlign.Center else TextAlign.Start,
                fillWidth = !wrapContent,
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
    color: Color = LocalContentColor.current,
    minFontSize: TextUnit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    fillWidth: Boolean = true,
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
            modifier = (if (fillWidth) Modifier
                .fillMaxWidth()
                .basicMarquee()
                .wrapContentHeight() else Modifier).align(Alignment.Center),
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewBudgetPill() {
    MinusTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.background(
                MaterialTheme.colorScheme.surface
            )
        ) {
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
                onOpenBudgetSheet = { },
            )

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
                onOpenBudgetSheet = { },
            )

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
                onOpenBudgetSheet = { },
            )

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
                onOpenBudgetSheet = { },
            )

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
                onOpenBudgetSheet = { },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewBudgetPillDynamic() {
    MinusTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.background(
                MaterialTheme.colorScheme.surface
            )
        ) {
            BudgetPill(
                budgetState = BudgetState(
                    remainingToday = BigDecimal("110.00"),
                    totalSpentToday = BigDecimal("12.50"),
                    dailyBudget = BigDecimal("122.50"),
                    daysRemaining = 15,
                    progress = 0.1f,
                    isOverBudget = false,
                    totalBudget = BigDecimal("500.00"),
                    totalSpentInPeriod = BigDecimal("12.50"),
                    dailyAllocation = BigDecimal("32.50"),
                    weeklyAllocation = BigDecimal("162.50"),
                    biweeklyAllocation = BigDecimal("487.50"),
                    monthlyAllocation = BigDecimal("487.50"),
                    isTodayOverDailyAllocation = false,
                ),
                budgetSettings = BudgetSettings(
                    totalBudget = BigDecimal("500.00"),
                    period = BudgetPeriod.DAILY,
                    startDate = LocalDate.now(),
                    currencyCode = "MXN",
                    splitMode = BudgetSplitMode.DYNAMIC,
                ),
                viewPeriod = BudgetPeriod.DAILY,
                currencyCode = "MXN",
                splitMode = BudgetSplitMode.DYNAMIC,
                onOpenBudgetSheet = { },
            )

            BudgetPill(
                budgetState = BudgetState(
                    remainingToday = BigDecimal("15200.62"),
                    totalSpentToday = BigDecimal("80.50"),
                    dailyBudget = BigDecimal("122.50"),
                    daysRemaining = 10,
                    progress = 0.1f,
                    isOverBudget = false,
                    totalBudget = BigDecimal("1500.00"),
                    totalSpentInPeriod = BigDecimal("80.50"),
                    dailyAllocation = BigDecimal("149.15"),
                    weeklyAllocation = BigDecimal("1044.05"),
                    biweeklyAllocation = BigDecimal("1491.50"),
                    monthlyAllocation = BigDecimal("1491.50"),
                    isTodayOverDailyAllocation = false,
                ),
                budgetSettings = BudgetSettings(
                    totalBudget = BigDecimal("1500.00"),
                    period = BudgetPeriod.DAILY,
                    startDate = LocalDate.now(),
                    currencyCode = "MXN",
                    splitMode = BudgetSplitMode.DYNAMIC,
                ),
                viewPeriod = BudgetPeriod.DAILY,
                currencyCode = "MXN",
                centerRemainingAmount = true,
                modifier = Modifier.fillMaxWidth(),
                splitMode = BudgetSplitMode.DYNAMIC,
                onOpenBudgetSheet = { },
            )

            BudgetPill(
                budgetState = BudgetState(
                    remainingToday = BigDecimal("110.00"),
                    totalSpentToday = BigDecimal("12.50"),
                    dailyBudget = BigDecimal("122.50"),
                    daysRemaining = 15,
                    progress = 0.1f,
                    isOverBudget = false,
                    totalBudget = BigDecimal("500.00"),
                    totalSpentInPeriod = BigDecimal("12.50"),
                    dailyAllocation = BigDecimal("32.50"),
                    weeklyAllocation = BigDecimal("162.50"),
                    biweeklyAllocation = BigDecimal("487.50"),
                    monthlyAllocation = BigDecimal("487.50"),
                    isTodayOverDailyAllocation = false,
                ),
                budgetSettings = BudgetSettings(
                    totalBudget = BigDecimal("500.00"),
                    period = BudgetPeriod.DAILY,
                    startDate = LocalDate.now(),
                    currencyCode = "MXN",
                    splitMode = BudgetSplitMode.DYNAMIC,
                ),
                viewPeriod = BudgetPeriod.WEEKLY,
                currencyCode = "MXN",
                splitMode = BudgetSplitMode.DYNAMIC,
                onOpenBudgetSheet = { },
            )

            BudgetPill(
                budgetState = BudgetState(
                    remainingToday = BigDecimal("40.00"),
                    totalSpentToday = BigDecimal("500.00"),
                    dailyBudget = BigDecimal("100.00"),
                    daysRemaining = 10,
                    progress = 0.6f,
                    isOverBudget = false,
                    totalBudget = BigDecimal("1000.00"),
                    totalSpentInPeriod = BigDecimal("600.00"),
                    dailyAllocation = BigDecimal("40.00"),
                    weeklyAllocation = BigDecimal("200.00"),
                    biweeklyAllocation = BigDecimal("400.00"),
                    monthlyAllocation = BigDecimal("400.00"),
                    isTodayOverDailyAllocation = true,
                ),
                budgetSettings = BudgetSettings(
                    totalBudget = BigDecimal("1000.00"),
                    period = BudgetPeriod.DAILY,
                    startDate = LocalDate.now(),
                    currencyCode = "MXN",
                    splitMode = BudgetSplitMode.DYNAMIC,
                ),
                viewPeriod = BudgetPeriod.DAILY,
                currencyCode = "MXN",
                splitMode = BudgetSplitMode.DYNAMIC,
                onOpenBudgetSheet = { },
            )

            BudgetPill(
                budgetState = BudgetState(
                    remainingToday = BigDecimal("-25.00"),
                    totalSpentToday = BigDecimal("150.00"),
                    dailyBudget = BigDecimal("100.00"),
                    daysRemaining = 5,
                    progress = 1.0f,
                    isOverBudget = true,
                    totalBudget = BigDecimal("500.00"),
                    totalSpentInPeriod = BigDecimal("600.00"),
                    dailyAllocation = BigDecimal.ZERO,
                    weeklyAllocation = BigDecimal.ZERO,
                    biweeklyAllocation = BigDecimal.ZERO,
                    monthlyAllocation = BigDecimal.ZERO,
                    isTodayOverDailyAllocation = true,
                ),
                budgetSettings = BudgetSettings(
                    totalBudget = BigDecimal("500.00"),
                    period = BudgetPeriod.DAILY,
                    startDate = LocalDate.now(),
                    currencyCode = "MXN",
                    splitMode = BudgetSplitMode.DYNAMIC,
                ),
                viewPeriod = BudgetPeriod.DAILY,
                currencyCode = "MXN",
                splitMode = BudgetSplitMode.DYNAMIC,
                onOpenBudgetSheet = { },
            )
        }
    }
}
