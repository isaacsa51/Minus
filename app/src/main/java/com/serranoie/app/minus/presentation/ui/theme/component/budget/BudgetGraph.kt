@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.ui.theme.component.budget

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.analytics.AnalyticsState
import com.serranoie.app.minus.presentation.ui.analytics.GraphGranularity
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.math.roundToInt

const val BUDGET_GRAPH_TAG = "BUDGET_GRAPH_TAG"
const val BUDGET_GRAPH_PREV_PAGE_TAG = "BUDGET_GRAPH_PREV_PAGE_TAG"
const val BUDGET_GRAPH_NEXT_PAGE_TAG = "BUDGET_GRAPH_NEXT_PAGE_TAG"
const val BUDGET_GRAPH_WINDOW_LABEL_TAG = "BUDGET_GRAPH_WINDOW_LABEL_TAG"
fun budgetGraphGranularityToggleTag(granularity: GraphGranularity) = "BUDGET_GRAPH_TOGGLE_${granularity.name}"

@Stable
class BudgetGraphState(
    private val allCurrentPoints: List<BigDecimal>,
    private val allPreviousPoints: List<BigDecimal>,
    val windowSize: Int,
    val scrollStep: Int,
    initialWindowIndex: Int,
) {
    var currentWindowIndex by mutableIntStateOf(initialWindowIndex)
        private set

    val totalWindows: Int = if (allCurrentPoints.size <= windowSize) 1
    else ((allCurrentPoints.size - windowSize) / scrollStep) + 1

    private var _renderCurrentPoints by mutableStateOf(calculatePointsForWindow(allCurrentPoints, initialWindowIndex, windowSize, scrollStep))
    private var _renderPreviousPoints by mutableStateOf(calculatePointsForWindow(allPreviousPoints, initialWindowIndex, windowSize, scrollStep))
    private var _renderWindowIndex by mutableIntStateOf(initialWindowIndex)

    val renderWindowIndex: Int get() = _renderWindowIndex

    private var _oldCurrentPoints by mutableStateOf(_renderCurrentPoints)
    private var _oldPreviousPoints by mutableStateOf(_renderPreviousPoints)

    val animProgress = Animatable(1f)

    val interpolatedCurrent by derivedStateOf {
        interpolatePoints(_oldCurrentPoints, _renderCurrentPoints, animProgress.value)
    }

    val interpolatedPrevious by derivedStateOf {
        interpolatePoints(_oldPreviousPoints, _renderPreviousPoints, animProgress.value)
    }

    val maxVal by derivedStateOf {
        val currentMax = interpolatedCurrent.maxOfOrNull { it } ?: BigDecimal.ZERO
        val previousMax = interpolatedPrevious.maxOfOrNull { it } ?: BigDecimal.ZERO
        val absoluteMax = currentMax.max(previousMax)
        if (absoluteMax <= BigDecimal.ZERO) 100f else absoluteMax.toFloat()
    }

    private val targetCurrentPoints: List<BigDecimal>
        get() = calculatePointsForWindow(allCurrentPoints, currentWindowIndex, windowSize, scrollStep)

    private val targetPreviousPoints: List<BigDecimal>
        get() = calculatePointsForWindow(allPreviousPoints, currentWindowIndex, windowSize, scrollStep)

    fun nextWindow() {
        if (currentWindowIndex < totalWindows - 1) {
            currentWindowIndex++
        }
    }

    fun prevWindow() {
        if (currentWindowIndex > 0) {
            currentWindowIndex--
        }
    }

    suspend fun reconcile(animDuration: Int = 600) {
        if (_renderCurrentPoints != targetCurrentPoints || _renderWindowIndex != currentWindowIndex) {
            _oldCurrentPoints = interpolatedCurrent
            _oldPreviousPoints = interpolatedPrevious

            _renderCurrentPoints = targetCurrentPoints
            _renderPreviousPoints = targetPreviousPoints
            _renderWindowIndex = currentWindowIndex

            animProgress.snapTo(0f)
            animProgress.animateTo(1f, animationSpec = tween(animDuration))
        }
    }

    private fun calculatePointsForWindow(
        points: List<BigDecimal>,
        index: Int,
        size: Int,
        step: Int
    ): List<BigDecimal> {
        if (points.isEmpty()) return listOf(BigDecimal.ZERO)
        val start = index * step
        val end = (start + size).coerceAtMost(points.size)
        val sub = if (start < points.size) points.subList(start, end) else listOf(BigDecimal.ZERO)
        return if (sub.size < 2) sub + sub else sub
    }
}

@Composable
fun rememberBudgetGraphState(
    allCurrentPoints: List<BigDecimal>,
    allPreviousPoints: List<BigDecimal>,
    granularity: GraphGranularity,
): BudgetGraphState {
    val (windowSize, scrollStep) = remember(granularity, allCurrentPoints.size) {
        when (granularity) {
            GraphGranularity.DAYS -> 7 to 1
            GraphGranularity.WEEK -> 7 to 7
            GraphGranularity.BIWEEK -> 14 to 14
            GraphGranularity.MONTH -> 30 to 30
            GraphGranularity.TOTAL -> allCurrentPoints.size to allCurrentPoints.size
        }
    }

    val totalWindows = remember(allCurrentPoints.size, windowSize, scrollStep, granularity) {
        if (granularity == GraphGranularity.TOTAL) 1
        else if (allCurrentPoints.size <= windowSize) 1
        else ((allCurrentPoints.size - windowSize) / scrollStep) + 1
    }

    val state = remember(allCurrentPoints, allPreviousPoints, windowSize, scrollStep) {
        BudgetGraphState(
            allCurrentPoints = allCurrentPoints,
            allPreviousPoints = allPreviousPoints,
            windowSize = windowSize,
            scrollStep = scrollStep,
            initialWindowIndex = (totalWindows - 1).coerceAtLeast(0)
        )
    }

    LaunchedEffect(state.currentWindowIndex, granularity) {
        delay(150)
        state.reconcile()
    }

    return state
}

@Composable
fun BudgetGraph(
    state: AnalyticsState,
    onGranularityChanged: (GraphGranularity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val dateLabelFormatter = remember(state.graphGranularity, locale) {
        when (state.graphGranularity) {
            GraphGranularity.DAYS -> DateTimeFormatter.ofPattern("dd MMM", locale)
            GraphGranularity.WEEK, GraphGranularity.BIWEEK -> DateTimeFormatter.ofPattern("dd MMM", locale)
            GraphGranularity.MONTH, GraphGranularity.TOTAL -> DateTimeFormatter.ofPattern("dd MMM", locale)
        }
    }

    val currencyFormat = remember(state.currencyCode) {
        symbolOnlyCurrencyFormat(state.currencyCode)
    }

    val totalSpent = remember(state.spends) {
        state.spends.sumOf { it.amount }
    }

    val totalDaysCount = remember(state.startPeriodDate, state.finishPeriodDate) {
        val startLocalDate = state.startPeriodDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val endLocalDate = (state.finishPeriodDate ?: Date()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        ChronoUnit.DAYS.between(startLocalDate, endLocalDate).toInt().coerceAtLeast(1)
    }

    val allCurrentPoints = remember(state.spends, state.startPeriodDate, state.finishPeriodDate) {
        calculateCumulativePoints(
            transactions = state.spends,
            startDate = state.startPeriodDate,
            endDate = state.finishPeriodDate ?: Date(),
            granularity = GraphGranularity.DAYS
        )
    }

    val allPreviousPoints = remember(state.previousPeriodTransactions) {
        if (state.previousPeriodTransactions.isEmpty()) return@remember emptyList<BigDecimal>()

        val start = state.previousPeriodTransactions.mapNotNull { it.date?.toLocalDate() }.minOrNull()
        val end = state.previousPeriodTransactions.mapNotNull { it.date?.toLocalDate() }.maxOrNull()

        if (start != null && end != null) {
            calculateCumulativePoints(
                transactions = state.previousPeriodTransactions,
                startDate = Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                endDate = Date.from(end.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                granularity = GraphGranularity.DAYS
            )
        } else {
            emptyList()
        }
    }

    val graphState = rememberBudgetGraphState(
        allCurrentPoints = allCurrentPoints,
        allPreviousPoints = allPreviousPoints,
        granularity = state.graphGranularity
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(BUDGET_GRAPH_TAG),
        colors = CardDefaults.cardColors(
            containerColor = combineColors(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant,
                t = 0.3f,
            ),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            BudgetGraphHeader(
                totalSpent = totalSpent,
                currencyFormat = currencyFormat,
            )

            BudgetGraphLegend(showPrevious = state.graphGranularity != GraphGranularity.TOTAL)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                GraphCanvas(
                    currentPoints = graphState.interpolatedCurrent,
                    previousPoints = graphState.interpolatedPrevious,
                    maxVal = graphState.maxVal,
                    modifier = Modifier.fillMaxSize(),
                    currencyCode = state.currencyCode,
                    startDate = state.startPeriodDate,
                    currentWindowIndex = graphState.renderWindowIndex,
                    scrollStep = graphState.scrollStep,
                    dateFormatter = dateLabelFormatter
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            GranularityToggle(
                selected = state.graphGranularity,
                totalDays = totalDaysCount,
                onSelected = onGranularityChanged,
                modifier = Modifier.fillMaxWidth()
            )

            if (graphState.totalWindows > 1) {
                BudgetGraphNavigation(
                    currentWindow = graphState.currentWindowIndex + 1,
                    totalWindows = graphState.totalWindows,
                    onPrevWindow = { graphState.prevWindow() },
                    onNextWindow = { graphState.nextWindow() }
                )
            }
        }
    }
}

@Composable
private fun BudgetGraphNavigation(
    currentWindow: Int,
    totalWindows: Int,
    onPrevWindow: () -> Unit,
    onNextWindow: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            modifier = Modifier
                .size(32.dp)
                .testTag(BUDGET_GRAPH_PREV_PAGE_TAG),
            onClick = onPrevWindow,
            enabled = currentWindow > 1
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.budget_graph_nav_prev)
            )
        }
        Text(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .testTag(BUDGET_GRAPH_WINDOW_LABEL_TAG),
            text = stringResource(R.string.budget_graph_page_indicator, currentWindow, totalWindows),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(
            modifier = Modifier
                .size(32.dp)
                .testTag(BUDGET_GRAPH_NEXT_PAGE_TAG),
            onClick = onNextWindow,
            enabled = currentWindow < totalWindows
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.budget_graph_nav_next)
            )
        }
    }
}

@Composable
private fun BudgetGraphLegend(showPrevious: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(
            color = MaterialTheme.colorScheme.primary,
            label = stringResource(R.string.budget_graph_legend_current)
        )
        if (showPrevious) {
            Spacer(modifier = Modifier.width(16.dp))
            LegendItem(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                label = stringResource(R.string.budget_graph_legend_previous)
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BudgetGraphHeader(
    totalSpent: BigDecimal,
    currencyFormat: java.text.Format,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.total_spent),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = currencyFormat.format(totalSpent),
                style = MaterialTheme.typography.displaySmallEmphasized,
                fontSize = 28.sp,
                maxLines = 2,
                softWrap = true
            )
        }
    }
}

@Composable
private fun GranularityToggle(
    selected: GraphGranularity,
    totalDays: Int,
    onSelected: (GraphGranularity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val availableGranularities = remember(totalDays) {
        GraphGranularity.entries.filter { granularity ->
            when (granularity) {
                GraphGranularity.DAYS -> true
                GraphGranularity.WEEK -> totalDays >= 7
                GraphGranularity.BIWEEK -> totalDays >= 14
                GraphGranularity.MONTH -> totalDays >= 30
                GraphGranularity.TOTAL -> true
            }
        }
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        availableGranularities.forEachIndexed { index, granularity ->
            val isSelected = selected == granularity
            ToggleButton(
                checked = isSelected,
                onCheckedChange = { onSelected(granularity) },
                modifier = Modifier
                    .testTag(budgetGraphGranularityToggleTag(granularity))
                    .height(34.dp),
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    availableGranularities.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
            ) {
                Text(
                    text = when (granularity) {
                        GraphGranularity.DAYS -> stringResource(R.string.graph_granularity_days)
                        GraphGranularity.WEEK -> stringResource(R.string.graph_granularity_week)
                        GraphGranularity.BIWEEK -> stringResource(R.string.graph_granularity_biweek)
                        GraphGranularity.MONTH -> stringResource(R.string.graph_granularity_month)
                        GraphGranularity.TOTAL -> stringResource(R.string.graph_granularity_total)
                    },
                    style = MaterialTheme.typography.labelSmallCondensed,
                )
            }
        }
    }
}

@Composable
private fun GraphCanvas(
    currentPoints: List<BigDecimal>,
    previousPoints: List<BigDecimal>,
    maxVal: Float,
    modifier: Modifier = Modifier,
    currencyCode: String,
    forcedTouchPosition: Offset? = null,
    startDate: Date,
    currentWindowIndex: Int,
    scrollStep: Int,
    dateFormatter: DateTimeFormatter
) {
    val graphColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    val currencyFormat = remember(currencyCode) {
        symbolOnlyCurrencyFormat(currencyCode)
    }

    val textMeasurer = rememberTextMeasurer()
    val tooltipStyle = MaterialTheme.typography.labelSmallEmphasized.copy(
        color = Color.White,
        fontWeight = FontWeight.Bold
    )

    val labelStyle = MaterialTheme.typography.labelSmallCondensed.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )

    var touchPosition by remember(forcedTouchPosition) { mutableStateOf(forcedTouchPosition) }

    val thousandsUnit = stringResource(R.string.unit_thousands)
    val millionsUnit = stringResource(R.string.unit_millions)

    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }
    val drawParams = remember(graphColor, secondaryColor, surfaceColor, gridColor, dashEffect, labelStyle) {
        GraphDrawParams(graphColor, secondaryColor, surfaceColor, gridColor, dashEffect, labelStyle)
    }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        touchPosition = offset
                    },
                    onDrag = { change, _ ->
                        touchPosition = change.position
                    },
                    onDragEnd = {
                        touchPosition = null
                    },
                    onDragCancel = {
                        touchPosition = null
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        touchPosition = offset
                        tryAwaitRelease()
                        touchPosition = null
                    },
                    onTap = { offset ->
                        touchPosition = offset
                    }
                )
            }
    ) {
        drawGraphContent(
            currentPoints = currentPoints,
            previousPoints = previousPoints,
            maxVal = maxVal,
            params = drawParams,
            touchPosition = touchPosition,
            currencyFormat = currencyFormat,
            textMeasurer = textMeasurer,
            tooltipStyle = tooltipStyle,
            startDate = startDate,
            currentWindowIndex = currentWindowIndex,
            scrollStep = scrollStep,
            dateFormatter = dateFormatter,
            thousandsUnit = thousandsUnit,
            millionsUnit = millionsUnit
        )
    }
}

private fun interpolatePoints(
    oldPoints: List<BigDecimal>,
    newPoints: List<BigDecimal>,
    factor: Float
): List<BigDecimal> {
    if (factor >= 1f) return newPoints
    if (factor <= 0f) return oldPoints

    val targetSize = 20
    val oldNormalized = normalizeList(oldPoints, targetSize)
    val newNormalized = normalizeList(newPoints, targetSize)

    return List(targetSize) { i ->
        val oldVal = oldNormalized[i].toFloat()
        val newVal = newNormalized[i].toFloat()
        (oldVal + (newVal - oldVal) * factor).toBigDecimal()
    }
}

private fun normalizeList(list: List<BigDecimal>, targetSize: Int): List<BigDecimal> {
    if (list.isEmpty()) return List(targetSize) { BigDecimal.ZERO }
    if (list.size == targetSize) return list

    return List(targetSize) { i ->
        val index = i.toFloat() / (targetSize - 1) * (list.size - 1)
        val lower = index.toInt()
        val upper = (lower + 1).coerceAtMost(list.size - 1)
        val fraction = index - lower

        val lowerVal = list[lower].toFloat()
        val upperVal = list[upper].toFloat()
        (lowerVal + (upperVal - lowerVal) * fraction).toBigDecimal()
    }
}

private data class GraphDrawParams(
    val graphColor: Color,
    val secondaryColor: Color,
    val surfaceColor: Color,
    val gridColor: Color,
    val dashEffect: PathEffect,
    val labelStyle: TextStyle
)

private fun DrawScope.drawGraphContent(
    currentPoints: List<BigDecimal>,
    previousPoints: List<BigDecimal>,
    maxVal: Float,
    params: GraphDrawParams,
    touchPosition: Offset?,
    currencyFormat: java.text.Format,
    textMeasurer: TextMeasurer,
    tooltipStyle: TextStyle,
    startDate: Date,
    currentWindowIndex: Int,
    scrollStep: Int,
    dateFormatter: DateTimeFormatter,
    thousandsUnit: String,
    millionsUnit: String
) {
    val width = size.width
    val height = size.height

    val leftMargin = 42.dp.toPx()
    val bottomMargin = 20.dp.toPx()
    val topPadding = 10.dp.toPx()
    val drawableHeight = height - bottomMargin - topPadding
    val drawableWidth = width - leftMargin

    val gridLinesCount = 4
    for (i in 0..gridLinesCount) {
        val fraction = i.toFloat() / gridLinesCount
        val y = height - bottomMargin - (fraction * drawableHeight)
        
        drawLine(
            color = params.gridColor,
            start = Offset(leftMargin, y),
            end = Offset(width, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = params.dashEffect
        )

        val value = (maxVal * fraction).toBigDecimal()
        val labelText = formatAxisValue(value, currencyFormat, thousandsUnit, millionsUnit)
        val textLayoutResult = textMeasurer.measure(labelText, params.labelStyle)
        
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                x = leftMargin - textLayoutResult.size.width - 8.dp.toPx(),
                y = y - textLayoutResult.size.height / 2
            )
        )
    }

    val startLocalDate = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val windowStartIndex = currentWindowIndex * scrollStep
    val stepWidth = drawableWidth / (currentPoints.size - 1).coerceAtLeast(1)

    val labelsToDraw = mutableListOf<Triple<Float, String, Int>>()
    var lastLabelEndX = -1000f
    var lastLabelText = ""

    val minGap = 16.dp.toPx()

    currentPoints.forEachIndexed { index, _ ->
        val x = leftMargin + index * stepWidth
        val date = startLocalDate.plusDays((windowStartIndex + index).toLong())
        val dateText = date.format(dateFormatter)

        val isEnd = index == currentPoints.size - 1
        val isFirst = index == 0

        val shouldAttemptLabel = isFirst || isEnd || dateText != lastLabelText

        if (shouldAttemptLabel) {
            val textLayoutResult = textMeasurer.measure(dateText, params.labelStyle)
            val labelWidth = textLayoutResult.size.width.toFloat()
            val labelStartX = x - labelWidth / 2

            if (labelStartX > lastLabelEndX + minGap) {
                labelsToDraw.add(Triple(x, dateText, index))
                lastLabelEndX = x + labelWidth / 2
                lastLabelText = dateText
            } else if (isEnd && labelsToDraw.isNotEmpty()) {
                labelsToDraw.removeAt(labelsToDraw.size - 1)
                labelsToDraw.add(Triple(x, dateText, index))
            }
        }
    }

    currentPoints.forEachIndexed { index, _ ->
        val x = leftMargin + index * stepWidth
        
        drawLine(
            color = params.gridColor.copy(alpha = 0.2f),
            start = Offset(x, topPadding),
            end = Offset(x, height - bottomMargin),
            strokeWidth = 1.dp.toPx()
        )

        labelsToDraw.find { it.third == index }?.let { (_, dateText, _) ->
            val textLayoutResult = textMeasurer.measure(dateText, params.labelStyle)
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x = x - textLayoutResult.size.width / 2,
                    y = height - bottomMargin + 4.dp.toPx()
                )
            )
        }
    }

    if (previousPoints.size > 1) {
        drawGraphLine(
            points = previousPoints,
            maxVal = maxVal,
            color = params.secondaryColor.copy(alpha = 0.3f),
            width = 3.dp.toPx(),
            leftMargin = leftMargin,
            topPadding = topPadding,
            bottomMargin = bottomMargin
        )
    }

    if (currentPoints.size > 1) {
        drawGraphLine(
            points = currentPoints,
            maxVal = maxVal,
            color = params.graphColor,
            width = 4.dp.toPx(),
            leftMargin = leftMargin,
            topPadding = topPadding,
            bottomMargin = bottomMargin
        )
    }

    touchPosition?.let { pos ->
        if (pos.x >= leftMargin) {
            drawTooltipInteraction(
                pos = pos,
                currentPoints = currentPoints,
                maxVal = maxVal,
                color = params.graphColor,
                surfaceColor = params.surfaceColor,
                currencyFormat = currencyFormat,
                textMeasurer = textMeasurer,
                tooltipStyle = tooltipStyle,
                leftMargin = leftMargin,
                topPadding = topPadding,
                bottomMargin = bottomMargin
            )
        }
    }
}

private fun formatAxisValue(value: BigDecimal, currencyFormat: java.text.Format, thousandsUnit: String, millionsUnit: String): String {
    return when {
        value >= BigDecimal("1000000") -> {
            val millions = value.divide(BigDecimal("1000000"), 1, RoundingMode.HALF_EVEN)
            "${currencyFormat.format(millions)}$millionsUnit"
        }
        value >= BigDecimal("1000") -> {
            val thousands = value.divide(BigDecimal("1000"), 0, RoundingMode.HALF_EVEN)
            "${currencyFormat.format(thousands)}$thousandsUnit"
        }
        else -> currencyFormat.format(value)
    }
}

private fun DrawScope.drawTooltipInteraction(
    pos: Offset,
    currentPoints: List<BigDecimal>,
    maxVal: Float,
    color: Color,
    surfaceColor: Color,
    currencyFormat: java.text.Format,
    textMeasurer: TextMeasurer,
    tooltipStyle: TextStyle,
    leftMargin: Float,
    topPadding: Float,
    bottomMargin: Float
) {
    val width = size.width
    val height = size.height
    val drawableWidth = width - leftMargin
    val drawableHeight = height - bottomMargin - topPadding
    
    val stepWidth = drawableWidth / (currentPoints.size - 1).coerceAtLeast(1)
    val index = ((pos.x - leftMargin) / stepWidth).roundToInt().coerceIn(0, currentPoints.size - 1)

    val amount = currentPoints[index]
    val pointX = leftMargin + index * stepWidth
    val pointY = height - bottomMargin - (amount.toFloat() / maxVal * drawableHeight)

    drawLine(
        color = color.copy(alpha = 0.5f),
        start = Offset(pointX, topPadding),
        end = Offset(pointX, height - bottomMargin),
        strokeWidth = 1.dp.toPx()
    )

    drawCircle(
        color = color,
        radius = 6.dp.toPx(),
        center = Offset(pointX, pointY)
    )

    drawCircle(
        color = surfaceColor,
        radius = 3.dp.toPx(),
        center = Offset(pointX, pointY)
    )

    val textLayoutResult = textMeasurer.measure(
        text = currencyFormat.format(amount),
        style = tooltipStyle
    )
    val tooltipWidth = textLayoutResult.size.width + 16.dp.toPx()
    val tooltipHeight = textLayoutResult.size.height + 8.dp.toPx()

    val tooltipX = (pointX - tooltipWidth / 2).coerceIn(0f, width - tooltipWidth)
    val tooltipY = (pointY - tooltipHeight - 12.dp.toPx()).coerceAtLeast(topPadding)

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.8f),
        topLeft = Offset(tooltipX, tooltipY),
        size = androidx.compose.ui.geometry.Size(tooltipWidth, tooltipHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
    )

    drawText(
        textMeasurer = textMeasurer,
        text = currencyFormat.format(amount),
        style = tooltipStyle,
        topLeft = Offset(tooltipX + 8.dp.toPx(), tooltipY + 4.dp.toPx())
    )
}

private fun DrawScope.drawGraphLine(
    points: List<BigDecimal>,
    maxVal: Float,
    color: Color,
    width: Float,
    leftMargin: Float,
    topPadding: Float,
    bottomMargin: Float
) {
    val canvasWidth = size.width
    val canvasHeight = size.height
    val drawableWidth = canvasWidth - leftMargin
    val drawableHeight = canvasHeight - bottomMargin - topPadding
    
    val stepWidth = drawableWidth / (points.size - 1).coerceAtLeast(1)

    val path = Path().apply {
        points.forEachIndexed { index, value ->
            val x = leftMargin + index * stepWidth
            val y = canvasHeight - bottomMargin - (value.toFloat() / maxVal * drawableHeight)
            if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = width,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

internal fun calculateCumulativePoints(
    transactions: List<Transaction>,
    startDate: Date,
    endDate: Date,
    granularity: GraphGranularity,
): List<BigDecimal> {
    val startLocalDate = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val endLocalDate = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

    val totalDays = ChronoUnit.DAYS.between(startLocalDate, endLocalDate).toInt().coerceAtLeast(1)

    val groupedTransactions = transactions
        .filter { it.date != null }
        .groupBy { it.date!!.toLocalDate() }
        .mapValues { it.value.sumOf { t -> t.amount } }

    val points = mutableListOf<BigDecimal>()
    points.add(BigDecimal.ZERO)
    var cumulativeSum = BigDecimal.ZERO

    val step = when (granularity) {
        GraphGranularity.DAYS -> 1
        GraphGranularity.WEEK -> 7
        GraphGranularity.BIWEEK -> 14
        GraphGranularity.MONTH -> 30
        GraphGranularity.TOTAL -> 1
    }

    for (i in 0..totalDays step step) {
        val currentStepEnd = (i + step - 1).coerceAtMost(totalDays)
        for (dayOffset in i..currentStepEnd) {
            val date = startLocalDate.plusDays(dayOffset.toLong())
            cumulativeSum += groupedTransactions[date] ?: BigDecimal.ZERO
        }
        points.add(cumulativeSum)
    }

    return if (points.isEmpty()) listOf(BigDecimal.ZERO) else points
}

@Preview(showBackground = true)
@Composable
private fun PreviewBudgetGraph() {
    MinusTheme {
        Surface {
            BudgetGraph(
                state = AnalyticsState(
                    spends = listOf(
                        Transaction(
                            amount = BigDecimal("10"),
                            date = java.time.LocalDateTime.now().minusDays(5)
                        ),
                        Transaction(
                            amount = BigDecimal("20"),
                            date = java.time.LocalDateTime.now().minusDays(3)
                        ),
                        Transaction(
                            amount = BigDecimal("50"),
                            date = java.time.LocalDateTime.now().minusDays(1)
                        )
                    ),
                    startPeriodDate = Date.from(
                        java.time.LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                    ),
                    finishPeriodDate = Date(),
                    currencyCode = "USD"
                ),
                onGranularityChanged = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Tooltip Preview")
@Preview(showBackground = true, name = "Tooltip Preview",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun PreviewBudgetGraphTooltip() {
    MinusTheme {
        Surface {
            Box(Modifier.padding(16.dp)) {
                GraphCanvas(
                    currentPoints = listOf(
                        BigDecimal("10"), BigDecimal("25"), BigDecimal("45"),
                        BigDecimal("30"), BigDecimal("60"), BigDecimal("50"), BigDecimal("80")
                    ),
                    previousPoints = emptyList(),
                    maxVal = 100f,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    currencyCode = "USD",
                    forcedTouchPosition = Offset(200f, 100f),
                    startDate = Date(),
                    currentWindowIndex = 0,
                    scrollStep = 1,
                    dateFormatter = DateTimeFormatter.ofPattern("dd MMM")
                )
            }
        }
    }
}
