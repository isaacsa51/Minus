package com.serranoie.app.minus.presentation.ui.theme.component.budget.pill

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.domain.model.SymbolPosition
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorBad
import com.serranoie.app.minus.presentation.ui.theme.colorGood
import com.serranoie.app.minus.presentation.ui.theme.colorNotGood
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleSmallCondensed
import com.serranoie.app.minus.presentation.util.censor
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import com.serranoie.app.minus.presentation.util.harmonizeWithColor
import com.serranoie.app.minus.presentation.util.toPaletteWithTheme
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The compact budget "pill": a circular-ended card with a progress fill, the amount left in
 * the current view period, and a status label. Supporting code lives alongside in the same
 * package — population in [BudgetPillMetrics], the animated sub-composables in
 * [BudgetPillAmountText] and [BudgetPillStatusLabel].
 */
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
    calculationPreview: String? = null,
    draftAmount: BigDecimal? = null,
    modifier: Modifier = Modifier,
) {
    val currencyFormat = remember(currencyCode) { symbolOnlyCurrencyFormat(currencyCode) }

    val metrics = remember(budgetState, viewPeriod, splitMode, draftAmount) {
        budgetState?.let {
            calculateBudgetMetrics(it, viewPeriod, splitMode, draftAmount ?: BigDecimal.ZERO)
        } ?: BudgetMetrics(BigDecimal.ZERO, 0f, false, false)
    }

    val exhaustedMessage = resolveExhaustedMessage(budgetState, viewPeriod, splitMode)

    val isNoBudget = budgetState == null

    val currency = remember(currencyCode) { SupportedCurrency.findByCode(currencyCode) }
    val currencySymbol = currency?.symbol ?: ""
    val symbolAtEnd = currency?.symbolPosition == SymbolPosition.END

    val animateCount = draftAmount != null && !isNoBudget

    val projectionTarget = metrics.nextPeriodAllocation?.toFloat() ?: 0f
    val animatedProjection by animateFloatAsState(
        targetValue = projectionTarget,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "projectionCount",
    )

    val projectionLabel = metrics.nextPeriodAllocation?.let {
        stringResource(
            when (viewPeriod) {
                BudgetPeriod.DAILY -> R.string.budget_pill_next_daily
                BudgetPeriod.WEEKLY -> R.string.budget_pill_next_weekly
                BudgetPeriod.BIWEEKLY -> R.string.budget_pill_next_biweekly
                BudgetPeriod.MONTHLY -> R.string.budget_pill_next_monthly
            },
            "",
        ).trim()
    }

    val projectionAmount = metrics.nextPeriodAllocation?.let { exact ->
        val shown = if (animateCount && animatedProjection != projectionTarget) {
            BigDecimal.valueOf(animatedProjection.toDouble())
        } else {
            exact
        }
        currencyFormat.format(shown)
    }

    val amountText = if (isNoBudget) {
        stringResource(R.string.budget_pill_no_budget_action)
    } else {
        currencyFormat.format(metrics.periodRemaining)
    }
    val symbolStyle = MaterialTheme.typography.titleSmallCondensed.toSpanStyle()
    val annotatedAmount = remember(amountText, currencyCode, symbolStyle, isNoBudget) {
        val currencySymbol = SupportedCurrency.findByCode(currencyCode)?.symbol ?: ""
        if (!isNoBudget && currencySymbol.length > 2 && amountText.startsWith(currencySymbol)) {
            val amount = amountText.removePrefix(currencySymbol).trim()
            AnnotatedString.Builder().apply {
                pushStyle(
                    symbolStyle.copy(
                        fontSize = 16.sp * 0.75f,
                        fontWeight = FontWeight.Bold,
                        baselineShift = BaselineShift(0f)
                    )
                )
                append(currencySymbol)
                pop()
                pushStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Light
                    )
                )
                append(amount)
                pop()
            }.toAnnotatedString()
        } else {
            AnnotatedString(amountText)
        }
    }

    val annotatedCalculationPreview = remember(calculationPreview, currencyCode, symbolStyle) {
        if (calculationPreview == null) return@remember null
        val currencySymbol = SupportedCurrency.findByCode(currencyCode)?.symbol ?: ""
        if (currencySymbol.length > 2 && calculationPreview.startsWith(currencySymbol)) {
            val rest = calculationPreview.removePrefix(currencySymbol)
            AnnotatedString.Builder().apply {
                pushStyle(
                    symbolStyle.copy(
                        fontSize = 16.sp * 0.75f,
                        fontWeight = FontWeight.Bold,
                        baselineShift = BaselineShift(0f)
                    )
                )
                append(currencySymbol)
                pop()
                pushStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Light
                    )
                )
                append(rest)
                pop()
            }.toAnnotatedString()
        } else {
            AnnotatedString(calculationPreview)
        }
    }

    val shouldCenterRemainingAmount =
        remember(
            centerRemainingAmount,
            metrics.isCurrentPeriodOverBudget,
            metrics.isOverCurrentSubPeriod,
            bigVariant,
            isNoBudget,
            calculationPreview
        ) {
            (centerRemainingAmount && !metrics.isCurrentPeriodOverBudget &&
                !metrics.isOverCurrentSubPeriod && !bigVariant) ||
                isNoBudget || calculationPreview != null
        }

    val isDarkTheme = isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary
    val good = colorGood
    val notGood = colorNotGood
    val bad = colorBad

    val harmonizedColor =
        remember(metrics.spendProgress, primaryColor, isDarkTheme, good, notGood, bad) {
            val combined = combineColors(listOf(good, notGood, bad), metrics.spendProgress)
            val harmonized = harmonizeWithColor(combined, primaryColor)
            toPaletteWithTheme(harmonized, isDarkTheme)
        }

    val animatedProgress by animateFloatAsState(
        targetValue = if (metrics.isCurrentPeriodOverBudget) 1f else metrics.spendProgress,
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
            modifier = Modifier
                .fillMaxHeight()
                .heightIn(min = 50.dp),
            shape = CircleShape, colors = CardDefaults.cardColors(
                containerColor = harmonizedColor.container.copy(alpha = 0.6f),
                contentColor = harmonizedColor.onContainer,
            ), onClick = onOpenBudgetSheet
        ) {
            Box(
                modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                if (!bigVariant) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(topEndPercent = 100, bottomEndPercent = 100))
                            .background(harmonizedColor.main)
                    )
                }

                AnimatedContent(
                    targetState = shouldCenterRemainingAmount,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        val fadeSpec = tween<Float>(180)
                        if (targetState) {
                            (slideInHorizontally(animationSpec = tween(220)) { it / 5 } + fadeIn(
                                fadeSpec
                            )) togetherWith (slideOutHorizontally(animationSpec = tween(180)) { -it / 5 } + fadeOut(
                                tween(120)
                            ))
                        } else {
                            (slideInHorizontally(animationSpec = tween(220)) { -it / 5 } + fadeIn(
                                fadeSpec
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
                            val baseAmountModifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    if (!isNoBudget && calculationPreview == null) {
                                        scaleX = centeredAmountScale
                                        scaleY = centeredAmountScale
                                    }
                                }
                            when {
                                calculationPreview != null -> AdaptiveSingleLineText(
                                    text = calculationPreview,
                                    annotatedText = annotatedCalculationPreview,
                                    style = MaterialTheme.typography.titleMediumCondensed,
                                    color = textColor,
                                    minFontSize = 16.sp,
                                    modifier = baseAmountModifier,
                                    textAlign = TextAlign.Center,
                                )

                                isNoBudget -> AdaptiveSingleLineText(
                                    text = stringResource(R.string.budget_pill_no_budget_action),
                                    style = MaterialTheme.typography.titleMediumCondensed,
                                    color = textColor,
                                    minFontSize = 16.sp,
                                    modifier = baseAmountModifier.censor(),
                                    textAlign = TextAlign.Center,
                                )

                                else -> SegmentedAmountText(
                                    text = amountText,
                                    style = MaterialTheme.typography.titleMediumCondensed,
                                    color = textColor,
                                    minFontSize = 16.sp,
                                    currencySymbol = currencySymbol,
                                    symbolAtEnd = symbolAtEnd,
                                    modifier = baseAmountModifier,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    } else {
                        val isCentered =
                            metrics.isCurrentPeriodOverBudget || metrics.isOverCurrentSubPeriod || bigVariant
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = if (isCentered) 0.dp else 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (isCentered) Arrangement.Center else Arrangement.spacedBy(
                                8.dp
                            )
                        ) {
                            StatusLabel(
                                budgetState = budgetState,
                                budgetPeriod = viewPeriod,
                                isOverBudget = metrics.isCurrentPeriodOverBudget,
                                isOverSubPeriodAllocation = metrics.isOverCurrentSubPeriod,
                                exhaustedMessage = exhaustedMessage,
                                projectionLabel = projectionLabel,
                                projectionAmount = projectionAmount,
                                currencySymbol = currencySymbol,
                                symbolAtEnd = symbolAtEnd,
                                bigVariant = bigVariant,
                                splitMode = splitMode,
                                wrapContent = true,
                                modifier = if (isCentered) Modifier.padding(horizontal = 32.dp) else Modifier.wrapContentWidth(),
                            )

                            if (!metrics.isCurrentPeriodOverBudget && !metrics.isOverCurrentSubPeriod && !bigVariant) {
                                AdaptiveSingleLineText(
                                    text = amountText,
                                    annotatedText = annotatedAmount,
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

@PreviewLightDark
@Composable
private fun PreviewBudgetPillSmallHeight() {
    MinusTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
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
                    currencyCode = "MAD"
                ),
                viewPeriod = BudgetPeriod.DAILY,
                currencyCode = "MAD",
                centerRemainingAmount = true,
                onOpenBudgetSheet = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            )

            BudgetPill(
                budgetState = BudgetState(
                    remainingToday = BigDecimal("60.00"),
                    totalSpentToday = BigDecimal("60.00"),
                    dailyBudget = BigDecimal("120.00"),
                    daysRemaining = 15,
                    progress = 0.5f,
                    isOverBudget = false,
                    totalBudget = BigDecimal("840.00"),
                    totalSpentInPeriod = BigDecimal("420.00"),
                    totalSpentThisWeek = BigDecimal("420.00"),
                ),
                budgetSettings = BudgetSettings(
                    totalBudget = BigDecimal("840.00"),
                    period = BudgetPeriod.WEEKLY,
                    startDate = LocalDate.now(),
                    currencyCode = "MXN"
                ),
                viewPeriod = BudgetPeriod.WEEKLY,
                currencyCode = "MXN",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                onOpenBudgetSheet = { },
            )

            BudgetPill(
                budgetState = BudgetState(
                    remainingToday = BigDecimal("-20.00"),
                    totalSpentToday = BigDecimal("120.00"),
                    dailyBudget = BigDecimal("100.00"),
                    daysRemaining = 10,
                    progress = 0.5f,
                    isOverBudget = false,
                    totalBudget = BigDecimal("700.00"),
                    totalSpentInPeriod = BigDecimal("350.00"),
                    totalSpentThisWeek = BigDecimal("350.00"),
                ),
                budgetSettings = BudgetSettings(
                    totalBudget = BigDecimal("700.00"),
                    period = BudgetPeriod.WEEKLY,
                    startDate = LocalDate.now(),
                    currencyCode = "MXN"
                ),
                viewPeriod = BudgetPeriod.WEEKLY,
                currencyCode = "MXN",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                onOpenBudgetSheet = { },
            )

            BudgetPill(
                budgetState = BudgetState(
                    remainingToday = BigDecimal("-20.00"),
                    totalSpentToday = BigDecimal("120.00"),
                    dailyBudget = BigDecimal("100.00"),
                    daysRemaining = 10,
                    progress = 0.6f,
                    isOverBudget = false,
                    totalBudget = BigDecimal("1000.00"),
                    totalSpentInPeriod = BigDecimal("350.00"),
                    totalSpentThisWeek = BigDecimal("350.00"),
                    dailyAllocation = BigDecimal("100.00"),
                    weeklyAllocation = BigDecimal("500.00"),
                    isTodayOverDailyAllocation = true,
                ),
                budgetSettings = BudgetSettings(
                    totalBudget = BigDecimal("1000.00"),
                    period = BudgetPeriod.WEEKLY,
                    startDate = LocalDate.now(),
                    currencyCode = "MXN",
                    splitMode = BudgetSplitMode.DYNAMIC,
                ),
                viewPeriod = BudgetPeriod.WEEKLY,
                currencyCode = "MXN",
                splitMode = BudgetSplitMode.DYNAMIC,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                onOpenBudgetSheet = { },
            )

            BudgetPill(
                budgetState = BudgetState(
                    remainingToday = BigDecimal("-150.00"),
                    totalSpentToday = BigDecimal("250.00"),
                    dailyBudget = BigDecimal("100.00"),
                    daysRemaining = 5,
                    progress = 1.0f,
                    isOverBudget = true,
                    totalBudget = BigDecimal("700.00"),
                    totalSpentInPeriod = BigDecimal("850.00"),
                    totalSpentThisWeek = BigDecimal("850.00"),
                ),
                budgetSettings = BudgetSettings(
                    totalBudget = BigDecimal("700.00"),
                    period = BudgetPeriod.WEEKLY,
                    startDate = LocalDate.now(),
                    currencyCode = "MXN"
                ),
                viewPeriod = BudgetPeriod.WEEKLY,
                currencyCode = "MXN",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                onOpenBudgetSheet = { },
            )

            BudgetPill(
                budgetState = BudgetState(
                    remainingToday = BigDecimal("-10.00"),
                    totalSpentToday = BigDecimal("50.00"),
                    dailyBudget = BigDecimal("40.00"),
                    daysRemaining = 3,
                    progress = 0.42f,
                    isOverBudget = false,
                    totalBudget = BigDecimal("120.00"),
                    totalSpentInPeriod = BigDecimal("50.00"),
                    nextDailyAllocation = BigDecimal("35.00"),
                ),
                budgetSettings = BudgetSettings(
                    totalBudget = BigDecimal("120.00"),
                    period = BudgetPeriod.DAILY,
                    startDate = LocalDate.now(),
                    currencyCode = "MXN",
                ),
                viewPeriod = BudgetPeriod.DAILY,
                currencyCode = "MXN",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                onOpenBudgetSheet = { },
            )

            BudgetPill(
                budgetState = BudgetState(
                    remainingToday = BigDecimal("-500.00"),
                    totalSpentToday = BigDecimal("600.00"),
                    dailyBudget = BigDecimal("47.62"),
                    daysRemaining = 14,
                    progress = 0.6f,
                    isOverBudget = false,
                    totalBudget = BigDecimal("1000.00"),
                    totalSpentInPeriod = BigDecimal("600.00"),
                    totalSpentThisWeek = BigDecimal("600.00"),
                    dailyAllocation = BigDecimal("40.00"),
                    weeklyAllocation = BigDecimal("333.33"),
                    biweeklyAllocation = BigDecimal("466.67"),
                    isTodayOverDailyAllocation = true,
                    nextWeeklyAllocation = BigDecimal("200.00"),
                ),
                budgetSettings = BudgetSettings(
                    totalBudget = BigDecimal("1000.00"),
                    period = BudgetPeriod.BIWEEKLY,
                    startDate = LocalDate.now(),
                    currencyCode = "MXN",
                    splitMode = BudgetSplitMode.DYNAMIC,
                ),
                viewPeriod = BudgetPeriod.WEEKLY,
                currencyCode = "MXN",
                splitMode = BudgetSplitMode.DYNAMIC,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                onOpenBudgetSheet = { },
            )
        }
    }
}

