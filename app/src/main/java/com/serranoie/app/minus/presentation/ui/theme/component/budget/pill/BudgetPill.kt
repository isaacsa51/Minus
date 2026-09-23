package com.serranoie.app.minus.presentation.ui.theme.component.budget.pill

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.domain.model.SymbolPosition
import com.serranoie.app.minus.presentation.ui.onboarding.periodLabel
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorBad
import com.serranoie.app.minus.presentation.ui.theme.colorGood
import com.serranoie.app.minus.presentation.ui.theme.colorNotGood
import com.serranoie.app.minus.presentation.ui.theme.component.budget.formula.BudgetFormulaRequest
import com.serranoie.app.minus.presentation.ui.theme.component.budget.formula.BudgetFormulaSource
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleSmallCondensed
import com.serranoie.app.minus.presentation.util.Utils.strongHapticFeedback
import com.serranoie.app.minus.presentation.util.censor
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import com.serranoie.app.minus.presentation.util.haptic.HapticUtil
import com.serranoie.app.minus.presentation.util.harmonizeWithColor
import com.serranoie.app.minus.presentation.util.toPaletteWithTheme
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.math.abs

internal const val BUDGET_PILL_FORMULA_KEY = "budget_pill"
private const val FORMULA_HOLD_MILLIS = 300
private const val FORMULA_HOLD_SCALE = 0.9f
private const val TODAY_FACE_MILLIS = 2000L
private const val VIEW_FACE_MILLIS = 5000L
private const val FACE_SLIDE_MILLIS = 300
private const val FACE_FADE_MILLIS = 150
private const val FACE_SCALE = 0.85f
private val FACE_SWIPE_THRESHOLD = 24.dp

private data class PillFace(
    val period: BudgetPeriod,
    val metrics: BudgetMetrics,
    val exhaustedMessage: String?,
    val projectionLabel: String?,
    val projectionAmount: String?,
    val amountText: String,
    val annotatedAmount: AnnotatedString,
)

/**
 * The compact budget "pill": a circular-ended card with a progress fill, the amount left in the current view period, and a status label.
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
    hasUnresolvedSurplus: Boolean = false,
    unresolvedSurplusAmount: BigDecimal? = null,
    onUnresolvedSurplusClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val currencyFormat = remember(currencyCode) { symbolOnlyCurrencyFormat(currencyCode) }

    val periodExpired = budgetSettings?.let { LocalDate.now().isAfter(it.getPeriodEndDate()) } ?: false
    val isNoBudget = budgetState == null || (hasUnresolvedSurplus && periodExpired)
    val showSurplusFace = hasUnresolvedSurplus && unresolvedSurplusAmount != null

    val canAlternate = viewPeriod != BudgetPeriod.DAILY && !isNoBudget && !centerRemainingAmount &&
        calculationPreview == null && !showSurplusFace
    var showToday by remember { mutableStateOf(false) }
    var swipeDirection by remember { mutableIntStateOf(0) }
    var swipes by remember { mutableIntStateOf(0) }
    LaunchedEffect(canAlternate, swipes) {
        if (!canAlternate) {
            showToday = false
            return@LaunchedEffect
        }
        while (true) {
            delay(if (showToday) TODAY_FACE_MILLIS else VIEW_FACE_MILLIS)
            swipeDirection = 0
            showToday = !showToday
        }
    }
    fun swipeFace(direction: Int) {
        swipeDirection = direction
        showToday = !showToday
        swipes++
    }
    val shownPeriod = if (showToday && canAlternate) BudgetPeriod.DAILY else viewPeriod

    val metrics = remember(budgetState, shownPeriod, splitMode, draftAmount) {
        budgetState?.let {
            calculateBudgetMetrics(it, shownPeriod, splitMode, draftAmount ?: BigDecimal.ZERO)
        } ?: BudgetMetrics(BigDecimal.ZERO, 0f, false, false)
    }

    val exhaustedMessage = resolveExhaustedMessage(budgetState, shownPeriod, splitMode)

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
            when (shownPeriod) {
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
    val face = PillFace(
        shownPeriod, metrics, exhaustedMessage, projectionLabel, projectionAmount, amountText, annotatedAmount,
    )

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

    val surplusAmountText = unresolvedSurplusAmount?.let { currencyFormat.format(it) }
    var showingSurplusMessage by remember { mutableStateOf(false) }
    LaunchedEffect(showSurplusFace) {
        if (!showSurplusFace) {
            showingSurplusMessage = false
            return@LaunchedEffect
        }
        while (true) {
            delay(4000)
            showingSurplusMessage = !showingSurplusMessage
        }
    }
    val isShowingSurplusFace = showSurplusFace && showingSurplusMessage

    val view = LocalView.current
    LaunchedEffect(isShowingSurplusFace) {
        if (isShowingSurplusFace) {
            HapticUtil.performMicroHaptic(view)
        }
    }

    val surplusPalette = remember(notGood, primaryColor, isDarkTheme) {
        toPaletteWithTheme(harmonizeWithColor(notGood, primaryColor), isDarkTheme)
    }
    val animatedContainerColor by animateColorAsState(
        targetValue = if (isShowingSurplusFace) {
            surplusPalette.container.copy(alpha = 0.6f)
        } else {
            harmonizedColor.container.copy(alpha = 0.6f)
        },
        animationSpec = tween(220),
        label = "pillContainerColor",
    )
    val animatedContentColor by animateColorAsState(
        targetValue = if (isShowingSurplusFace) surplusPalette.onContainer else harmonizedColor.onContainer,
        animationSpec = tween(220),
        label = "pillContentColor",
    )
    Column(
        modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BudgetFormulaSource(key = BUDGET_PILL_FORMULA_KEY) { sharedBoundsModifier, showFormula ->
            val pressScale = remember { Animatable(1f) }
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            var holdFired by remember { mutableStateOf(false) }
            val formulaTip = stringResource(R.string.budget_formula_tip_tap_card)
            val openFormula by rememberUpdatedState(
                if (showFormula != null && budgetState != null && !isNoBudget) {
                    val request = BudgetFormulaRequest(
                        budgetState, budgetSettings, shownPeriod, splitMode, currencyCode,
                        draftAmount = draftAmount ?: BigDecimal.ZERO,
                        tip = formulaTip,
                    )
                    fun() = showFormula(request)
                } else {
                    null
                }
            )
            LaunchedEffect(pressed) {
                if (pressed && openFormula != null) {
                    holdFired = false
                    pressScale.animateTo(
                        targetValue = FORMULA_HOLD_SCALE,
                        animationSpec = keyframes {
                            durationMillis = FORMULA_HOLD_MILLIS
                            0.96f at 120 using FastOutSlowInEasing
                            FORMULA_HOLD_SCALE at FORMULA_HOLD_MILLIS using LinearEasing
                        },
                    )
                    holdFired = true
                    view.strongHapticFeedback()
                    openFormula?.invoke()
                } else {
                    pressScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
            }
            val formulaHint = stringResource(R.string.budget_formula_hold_hint)
            val otherFaceLabel = (if (shownPeriod == BudgetPeriod.DAILY) viewPeriod else BudgetPeriod.DAILY).periodLabel()

            Card(
                modifier = Modifier
                    .fillMaxHeight()
                    .heightIn(min = 50.dp)
                    .then(sharedBoundsModifier)
                    .graphicsLayer {
                        scaleX = pressScale.value
                        scaleY = pressScale.value
                    }
                    .pointerInput(canAlternate) {
                        if (!canAlternate) return@pointerInput
                        var dragged = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragged = 0f },
                            onDragEnd = {
                                if (abs(dragged) > FACE_SWIPE_THRESHOLD.toPx()) swipeFace(if (dragged > 0) 1 else -1)
                            },
                        ) { change, amount ->
                            change.consume()
                            dragged += amount
                        }
                    }
                    .semantics {
                        if (openFormula != null) {
                            onLongClick(label = formulaHint) {
                                openFormula?.invoke()
                                true
                            }
                        }
                        if (canAlternate) {
                            customActions = listOf(
                                CustomAccessibilityAction(otherFaceLabel) {
                                    swipeFace(1)
                                    true
                                }
                            )
                        }
                    },
                shape = CircleShape, colors = CardDefaults.cardColors(
                    containerColor = animatedContainerColor,
                    contentColor = animatedContentColor,
                ),
                interactionSource = interactionSource,
                onClick = {
                    if (holdFired) {
                        holdFired = false
                    } else if (isShowingSurplusFace) {
                        onUnresolvedSurplusClick()
                    } else {
                        onOpenBudgetSheet()
                    }
                }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    if (!bigVariant && !isShowingSurplusFace) {
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
                        targetState = isShowingSurplusFace,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                        },
                        label = "budgetPillSurplusToggle",
                    ) { showSurplus ->
                        if (showSurplus && surplusAmountText != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 18.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                AdaptiveSingleLineText(
                                    text = stringResource(R.string.unresolved_surplus_title),
                                    style = MaterialTheme.typography.titleMediumEmphasized,
                                    minFontSize = 14.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${stringResource(R.string.unresolved_surplus_tap_to_manage)}: ",
                                        style = MaterialTheme.typography.labelSmallEmphasized,
                                        maxLines = 1,
                                    )
                                    SegmentedAmountText(
                                        text = surplusAmountText,
                                        style = MaterialTheme.typography.labelSmallEmphasized,
                                        color = LocalContentColor.current,
                                        minFontSize = 12.sp,
                                        currencySymbol = currencySymbol,
                                        symbolAtEnd = symbolAtEnd,
                                        textAlign = TextAlign.Start,
                                        fillWidth = false,
                                    )
                                }
                            }
                        } else {
                            AnimatedContent(
                                targetState = shouldCenterRemainingAmount,
                                modifier = Modifier.fillMaxSize(),
                                transitionSpec = {
                                    val fadeSpec = tween<Float>(180)
                                    if (targetState) {
                                        (slideInHorizontally(animationSpec = tween(220)) { it / 5 } + fadeIn(
                                            fadeSpec
                                        )) togetherWith slideOutHorizontally(animationSpec = tween(180)) { -it / 5 } + fadeOut(
                                            tween(120)
                                        )
                                    } else {
                                        (slideInHorizontally(animationSpec = tween(220)) { -it / 5 } + fadeIn(
                                            fadeSpec
                                        )) togetherWith slideOutHorizontally(animationSpec = tween(180)) { it / 5 } + fadeOut(
                                            tween(120)
                                        )
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
                                    AnimatedContent(
                                        targetState = face,
                                        modifier = Modifier.fillMaxSize(),
                                        contentKey = { it.period },
                                        transitionSpec = {
                                            val move = tween<IntOffset>(FACE_SLIDE_MILLIS)
                                            val scale = tween<Float>(FACE_SLIDE_MILLIS)
                                            val fade = tween<Float>(FACE_FADE_MILLIS)
                                            val direction = swipeDirection
                                            val slideIn = if (direction == 0) {
                                                slideInVertically(move) { it }
                                            } else {
                                                slideInHorizontally(move) { -direction * it }
                                            }
                                            val slideOut = if (direction == 0) {
                                                slideOutVertically(move) { -it }
                                            } else {
                                                slideOutHorizontally(move) { direction * it }
                                            }
                                            (slideIn + scaleIn(scale, FACE_SCALE) + fadeIn(fade)) togetherWith
                                                (slideOut + scaleOut(scale, FACE_SCALE) + fadeOut(fade))
                                        },
                                        label = "budgetPillFace",
                                    ) { shown ->
                                        val isOver = shown.metrics.isCurrentPeriodOverBudget || shown.metrics.isOverCurrentSubPeriod
                                        val isCentered = isOver || bigVariant
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
                                                budgetPeriod = shown.period,
                                                isOverBudget = shown.metrics.isCurrentPeriodOverBudget,
                                                isOverSubPeriodAllocation = shown.metrics.isOverCurrentSubPeriod,
                                                exhaustedMessage = shown.exhaustedMessage,
                                                projectionLabel = shown.projectionLabel,
                                                projectionAmount = shown.projectionAmount,
                                                currencySymbol = currencySymbol,
                                                symbolAtEnd = symbolAtEnd,
                                                bigVariant = bigVariant,
                                                wrapContent = true,
                                                modifier = if (isCentered) Modifier.padding(horizontal = 32.dp) else Modifier.wrapContentWidth(),
                                            )

                                            if (!isOver && !bigVariant) {
                                                AdaptiveSingleLineText(
                                                    text = shown.amountText,
                                                    annotatedText = shown.annotatedAmount,
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
                    periodTotalDays = 10,
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
                    periodTotalDays = 3,
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
                    periodTotalDays = 21,
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

@Preview(showBackground = true, locale = "bg", device = "id:4in WVGA (Nexus S)")
@Preview(showBackground = true, locale = "bg", device = "id:4in WVGA (Nexus S)",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    wallpaper = Wallpapers.GREEN_DOMINATED_EXAMPLE
)
@Composable
private fun PreviewBudgetPillWeeklyExceededWithProjection() {
    MinusTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
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
                    periodTotalDays = 21,
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


