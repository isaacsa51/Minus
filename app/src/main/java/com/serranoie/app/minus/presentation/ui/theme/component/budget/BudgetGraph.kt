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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
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
import com.serranoie.app.minus.presentation.util.Utils.confirmFeedback
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.math.roundToInt

const val BUDGET_GRAPH_TAG = "BUDGET_GRAPH_TAG"
const val BUDGET_GRAPH_PREV_PAGE_TAG = "BUDGET_GRAPH_PREV_PAGE_TAG"
const val BUDGET_GRAPH_NEXT_PAGE_TAG = "BUDGET_GRAPH_NEXT_PAGE_TAG"
const val BUDGET_GRAPH_WINDOW_LABEL_TAG = "BUDGET_GRAPH_WINDOW_LABEL_TAG"
fun budgetGraphGranularityToggleTag(granularity: GraphGranularity) =
    "BUDGET_GRAPH_TOGGLE_${granularity.name}"

@Stable
class BudgetGraphState(
    initialAllCurrentPoints: List<BigDecimal>,
    initialAllPreviousPoints: List<BigDecimal>,
    initialWindowSize: Int,
    initialScrollStep: Int,
    initialWindowIndex: Int,
) {
    private var allCurrentPoints by mutableStateOf(initialAllCurrentPoints)
    private var allPreviousPoints by mutableStateOf(initialAllPreviousPoints)
    var windowSize by mutableIntStateOf(initialWindowSize)
        private set
    var scrollStep by mutableIntStateOf(initialScrollStep)
        private set

    var currentWindowIndex by mutableIntStateOf(initialWindowIndex)
        private set

    val totalWindows: Int
        get() = if (allCurrentPoints.size <= windowSize) 1
        else ((allCurrentPoints.size - windowSize) / scrollStep) + 1

    private var _renderCurrentPoints by mutableStateOf(
        calculatePointsForWindow(
            allCurrentPoints,
            initialWindowIndex,
            initialWindowSize,
            initialScrollStep
        )
    )
    private var _renderPreviousPoints by mutableStateOf(
        calculatePointsForWindow(
            allPreviousPoints,
            initialWindowIndex,
            initialWindowSize,
            initialScrollStep
        )
    )
    private var _renderWindowIndex by mutableIntStateOf(initialWindowIndex)
    private var _oldWindowIndex by mutableIntStateOf(initialWindowIndex)

    private var _renderScrollStep by mutableIntStateOf(initialScrollStep)
    private var _oldScrollStep by mutableIntStateOf(initialScrollStep)

    private var _renderDataSize by mutableIntStateOf(_renderCurrentPoints.size)
    private var _oldDataSize by mutableIntStateOf(_renderCurrentPoints.size)

    val renderWindowIndex: Int get() = _renderWindowIndex
    val oldWindowIndex: Int get() = _oldWindowIndex
    val renderScrollStep: Int get() = _renderScrollStep
    val oldScrollStep: Int get() = _oldScrollStep
    val renderDataSize: Int get() = _renderDataSize
    val oldDataSize: Int get() = _oldDataSize

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

    val oldMaxVal by derivedStateOf {
        val currentMax = _oldCurrentPoints.maxOfOrNull { it } ?: BigDecimal.ZERO
        val previousMax = _oldPreviousPoints.maxOfOrNull { it } ?: BigDecimal.ZERO
        val absoluteMax = currentMax.max(previousMax)
        if (absoluteMax <= BigDecimal.ZERO) 100f else absoluteMax.toFloat()
    }

    val newMaxVal by derivedStateOf {
        val currentMax = _renderCurrentPoints.maxOfOrNull { it } ?: BigDecimal.ZERO
        val previousMax = _renderPreviousPoints.maxOfOrNull { it } ?: BigDecimal.ZERO
        val absoluteMax = currentMax.max(previousMax)
        if (absoluteMax <= BigDecimal.ZERO) 100f else absoluteMax.toFloat()
    }

    private val targetCurrentPoints: List<BigDecimal>
        get() = calculatePointsForWindow(
            allCurrentPoints,
            currentWindowIndex,
            windowSize,
            scrollStep
        )

    private val targetPreviousPoints: List<BigDecimal>
        get() = calculatePointsForWindow(
            allPreviousPoints,
            currentWindowIndex,
            windowSize,
            scrollStep
        )

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

    fun updateConfig(
        newAllCurrentPoints: List<BigDecimal>,
        newAllPreviousPoints: List<BigDecimal>,
        newWindowSize: Int,
        newScrollStep: Int,
        newWindowIndex: Int
    ) {
        allCurrentPoints = newAllCurrentPoints
        allPreviousPoints = newAllPreviousPoints
        windowSize = newWindowSize
        scrollStep = newScrollStep
        currentWindowIndex = newWindowIndex
    }

    suspend fun reconcile(animDuration: Int = 600) {
        val targetCurrent = targetCurrentPoints
        val targetPrevious = targetPreviousPoints

        if (_renderCurrentPoints != targetCurrent || _renderWindowIndex != currentWindowIndex) {
            _oldCurrentPoints = interpolatedCurrent
            _oldPreviousPoints = interpolatedPrevious
            _oldWindowIndex = _renderWindowIndex
            _oldScrollStep = _renderScrollStep
            _oldDataSize = _renderDataSize

            _renderCurrentPoints = targetCurrent
            _renderPreviousPoints = targetPrevious
            _renderWindowIndex = currentWindowIndex
            _renderScrollStep = scrollStep
            _renderDataSize = targetCurrent.size

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

    val initialWindowIndex = remember(granularity) { (totalWindows - 1).coerceAtLeast(0) }

    val state = remember {
        BudgetGraphState(
            initialAllCurrentPoints = allCurrentPoints,
            initialAllPreviousPoints = allPreviousPoints,
            initialWindowSize = windowSize,
            initialScrollStep = scrollStep,
            initialWindowIndex = initialWindowIndex
        )
    }

    LaunchedEffect(allCurrentPoints, allPreviousPoints, windowSize, scrollStep, granularity) {
        val targetWindowIndex = if (granularity == GraphGranularity.TOTAL) 0
        else (totalWindows - 1).coerceAtLeast(0)

        state.updateConfig(
            newAllCurrentPoints = allCurrentPoints,
            newAllPreviousPoints = allPreviousPoints,
            newWindowSize = windowSize,
            newScrollStep = scrollStep,
            newWindowIndex = targetWindowIndex
        )
        delay(150)
        state.reconcile()
    }

    LaunchedEffect(state.currentWindowIndex) {
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
    val view = LocalView.current
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val dateLabelFormatter = remember(state.graphGranularity, locale) {
        when (state.graphGranularity) {
            GraphGranularity.DAYS -> DateTimeFormatter.ofPattern("dd MMM", locale)
            GraphGranularity.WEEK, GraphGranularity.BIWEEK -> DateTimeFormatter.ofPattern(
                "dd MMM",
                locale
            )

            GraphGranularity.MONTH, GraphGranularity.TOTAL -> DateTimeFormatter.ofPattern(
                "dd MMM",
                locale
            )
        }
    }

    val currencyFormat = remember(state.currencyCode) {
        symbolOnlyCurrencyFormat(state.currencyCode)
    }

    val totalSpent = remember(state.spends) {
        state.spends.sumOf { it.amount }
    }

    val hasPeriod = state.budgetSettingsForDisplay != null

    val totalDaysCount = remember(state.startPeriodDate, state.finishPeriodDate, hasPeriod) {
        if (!hasPeriod) return@remember 1
        val startLocalDate =
            state.startPeriodDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val endLocalDate =
            (state.finishPeriodDate ?: Date()).toInstant().atZone(ZoneId.systemDefault())
                .toLocalDate()
        ChronoUnit.DAYS.between(startLocalDate, endLocalDate).toInt().coerceAtLeast(1)
    }

    val allCurrentPoints =
        remember(state.spends, state.startPeriodDate, state.finishPeriodDate, hasPeriod) {
            if (!hasPeriod) return@remember listOf(BigDecimal.ZERO, BigDecimal.ZERO)
            calculateCumulativePoints(
                transactions = state.spends,
                startDate = state.startPeriodDate,
                endDate = state.finishPeriodDate ?: Date(),
                granularity = GraphGranularity.DAYS
            )
        }

    val allPreviousPoints = remember(state.previousPeriodTransactions, hasPeriod) {
        if (!hasPeriod || state.previousPeriodTransactions.isEmpty()) return@remember emptyList<BigDecimal>()

        val start =
            state.previousPeriodTransactions.mapNotNull { it.date?.toLocalDate() }.minOrNull()
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

            BudgetGraphLegend()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                GraphCanvas(
                    currentPoints = graphState.interpolatedCurrent,
                    previousPoints = graphState.interpolatedPrevious,
                    maxVal = graphState.maxVal,
                    oldMaxVal = graphState.oldMaxVal,
                    newMaxVal = graphState.newMaxVal,
                    modifier = Modifier.fillMaxSize(),
                    currencyCode = state.currencyCode,
                    startDate = if (hasPeriod) state.startPeriodDate else Date(),
                    currentWindowIndex = graphState.renderWindowIndex,
                    oldWindowIndex = graphState.oldWindowIndex,
                    scrollStep = graphState.renderScrollStep,
                    oldScrollStep = graphState.oldScrollStep,
                    renderDataSize = graphState.renderDataSize,
                    oldDataSize = graphState.oldDataSize,
                    animProgress = graphState.animProgress.value,
                    dateFormatter = dateLabelFormatter
                )
            }

            if (hasPeriod && state.spends.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                GranularityToggle(
                    selected = state.graphGranularity,
                    totalDays = totalDaysCount,
                    onSelected = {
                        view.confirmFeedback()
                        onGranularityChanged(it)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (hasPeriod && graphState.totalWindows > 1) {
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
    val view = LocalView.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            modifier = Modifier
                .size(32.dp)
                .testTag(BUDGET_GRAPH_PREV_PAGE_TAG),
            onClick = {
                view.confirmFeedback()
                onPrevWindow()
            },
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
            text = stringResource(
                R.string.budget_graph_page_indicator,
                currentWindow,
                totalWindows
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(
            modifier = Modifier
                .size(32.dp)
                .testTag(BUDGET_GRAPH_NEXT_PAGE_TAG),
            onClick = {
                view.confirmFeedback()
                onNextWindow()
            },
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
private fun BudgetGraphLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(
            color = MaterialTheme.colorScheme.tertiary,
            label = stringResource(R.string.budget_graph_legend_current)
        )
        Spacer(modifier = Modifier.width(16.dp))
        LegendItem(
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
            label = stringResource(R.string.budget_graph_legend_previous)
        )
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
            color = MaterialTheme.colorScheme.onSurface
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
                    .height(38.dp),
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
    oldMaxVal: Float,
    newMaxVal: Float,
    modifier: Modifier = Modifier,
    currencyCode: String,
    forcedTouchPosition: Offset? = null,
    startDate: Date,
    currentWindowIndex: Int,
    oldWindowIndex: Int,
    scrollStep: Int,
    oldScrollStep: Int,
    renderDataSize: Int,
    oldDataSize: Int,
    animProgress: Float,
    dateFormatter: DateTimeFormatter
) {
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
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
    val drawParams =
        remember(tertiaryColor, secondaryColor, surfaceColor, gridColor, dashEffect, labelStyle) {
            GraphDrawParams(
                graphColor = tertiaryColor,
                secondaryColor = secondaryColor,
                surfaceColor = surfaceColor,
                gridColor = gridColor,
                dashEffect = dashEffect,
                labelStyle = labelStyle,
                tertiaryColor = tertiaryColor
            )
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
        val oldAlpha = (1f - animProgress * 2.5f).coerceIn(0f, 1f)
        val newAlpha = (animProgress * 2.5f - 1.5f).coerceIn(0f, 1f)

        if (oldAlpha > 0f && animProgress < 1f) {
            drawCoordinateSystem(
                params = drawParams,
                textMeasurer = textMeasurer,
                currencyFormat = currencyFormat,
                dateFormatter = dateFormatter,
                startDate = startDate,
                windowIndex = oldWindowIndex,
                scrollStep = oldScrollStep,
                dataSize = oldDataSize,
                maxVal = oldMaxVal,
                alpha = oldAlpha,
                thousandsUnit = thousandsUnit,
                millionsUnit = millionsUnit
            )
        }

        val effectiveNewAlpha = if (animProgress >= 1f) 1f else newAlpha

        if (effectiveNewAlpha > 0f) {
            drawCoordinateSystem(
                params = drawParams,
                textMeasurer = textMeasurer,
                currencyFormat = currencyFormat,
                dateFormatter = dateFormatter,
                startDate = startDate,
                windowIndex = currentWindowIndex,
                scrollStep = scrollStep,
                dataSize = renderDataSize,
                maxVal = newMaxVal,
                alpha = effectiveNewAlpha,
                thousandsUnit = thousandsUnit,
                millionsUnit = millionsUnit,
                isTodayHighlighted = true
            )
        }

        drawGraphLinesContent(
            currentPoints = currentPoints,
            previousPoints = previousPoints,
            maxVal = maxVal,
            params = drawParams,
            touchPosition = touchPosition,
            currencyFormat = currencyFormat,
            textMeasurer = textMeasurer,
            tooltipStyle = tooltipStyle,
            thousandsUnit = thousandsUnit,
            millionsUnit = millionsUnit
        )
    }
}

private fun DrawScope.drawCoordinateSystem(
    params: GraphDrawParams,
    textMeasurer: TextMeasurer,
    currencyFormat: java.text.Format,
    dateFormatter: DateTimeFormatter,
    startDate: Date,
    windowIndex: Int,
    scrollStep: Int,
    dataSize: Int,
    maxVal: Float,
    alpha: Float,
    thousandsUnit: String,
    millionsUnit: String,
    isTodayHighlighted: Boolean = false
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
            color = params.gridColor.copy(alpha = params.gridColor.alpha * alpha),
            start = Offset(leftMargin, y),
            end = Offset(width, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = params.dashEffect
        )

        val value = (maxVal * fraction).toBigDecimal()
        val labelText = formatAxisValue(value, currencyFormat, thousandsUnit, millionsUnit)
        val textLayoutResult = textMeasurer.measure(
            text = labelText,
            style = params.labelStyle.copy(color = params.labelStyle.color.copy(alpha = params.labelStyle.color.alpha * alpha))
        )

        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                x = leftMargin - textLayoutResult.size.width - 8.dp.toPx(),
                y = y - textLayoutResult.size.height / 2
            )
        )
    }

    val startLocalDate = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    val windowStartIndex = windowIndex * scrollStep
    val stepWidth = drawableWidth / (dataSize - 1).coerceAtLeast(1)

    val labelsToDraw = mutableListOf<Triple<Float, String, Int>>()
    var lastLabelEndX = -1000f
    var lastLabelText = ""
    val minGap = 16.dp.toPx()

    for (index in 0 until dataSize) {
        val x = leftMargin + index * stepWidth
        val date = startLocalDate.plusDays((windowStartIndex + index).toLong())
        val dateText = date.format(dateFormatter)

        val isEnd = index == dataSize - 1
        val isFirst = index == 0
        val isToday = date == today

        val shouldAttemptLabel = isFirst || isEnd || (isToday && isTodayHighlighted) || dateText != lastLabelText

        if (shouldAttemptLabel) {
            val style = if (isToday && isTodayHighlighted) {
                params.labelStyle.copy(
                    color = params.tertiaryColor.copy(alpha = alpha),
                    fontWeight = FontWeight.Bold
                )
            } else {
                params.labelStyle.copy(color = params.labelStyle.color.copy(alpha = params.labelStyle.color.alpha * alpha))
            }
            val textLayoutResult = textMeasurer.measure(dateText, style)
            val labelWidth = textLayoutResult.size.width.toFloat()
            val labelStartX = x - labelWidth / 2

            if (labelStartX > lastLabelEndX + minGap) {
                labelsToDraw.add(Triple(x, dateText, index))
                lastLabelEndX = x + labelWidth / 2
                lastLabelText = dateText
            } else if (isEnd && labelsToDraw.isNotEmpty()) {
                labelsToDraw.removeAt(labelsToDraw.size - 1)
                labelsToDraw.add(Triple(x, dateText, index))
            } else if (isToday && isTodayHighlighted && labelsToDraw.isNotEmpty()) {
                val last = labelsToDraw.last()
                val lastX = last.first
                val lastText = last.second
                val lastMeasured = textMeasurer.measure(lastText, params.labelStyle)
                if (x - labelWidth / 2 < lastX + lastMeasured.size.width / 2 + minGap) {
                    labelsToDraw.removeAt(labelsToDraw.size - 1)
                }
                labelsToDraw.add(Triple(x, dateText, index))
                lastLabelEndX = x + labelWidth / 2
                lastLabelText = dateText
            }
        }
    }

    for (index in 0 until dataSize) {
        val x = leftMargin + index * stepWidth
        val date = startLocalDate.plusDays((windowStartIndex + index).toLong())
        val isToday = date == today && isTodayHighlighted

        drawLine(
            color = if (isToday) params.tertiaryColor.copy(alpha = 0.4f * alpha) else params.gridColor.copy(
                alpha = 0.2f * alpha
            ),
            start = Offset(x, topPadding),
            end = Offset(x, height - bottomMargin),
            strokeWidth = if (isToday) 2.dp.toPx() else 1.dp.toPx()
        )

        labelsToDraw.find { it.third == index }?.let { (_, dateText, _) ->
            val style = if (isToday) {
                params.labelStyle.copy(
                    color = params.tertiaryColor.copy(alpha = alpha),
                    fontWeight = FontWeight.ExtraBold
                )
            } else {
                params.labelStyle.copy(color = params.labelStyle.color.copy(alpha = params.labelStyle.color.alpha * alpha))
            }
            val textLayoutResult = textMeasurer.measure(dateText, style)
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x = x - textLayoutResult.size.width / 2,
                    y = height - bottomMargin + 4.dp.toPx()
                )
            )
        }
    }
}

private fun DrawScope.drawGraphLinesContent(
    currentPoints: List<BigDecimal>,
    previousPoints: List<BigDecimal>,
    maxVal: Float,
    params: GraphDrawParams,
    touchPosition: Offset?,
    currencyFormat: java.text.Format,
    textMeasurer: TextMeasurer,
    tooltipStyle: TextStyle,
    thousandsUnit: String,
    millionsUnit: String
) {
    val leftMargin = 42.dp.toPx()
    val bottomMargin = 20.dp.toPx()
    val topPadding = 10.dp.toPx()

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
        drawGraphArea(
            points = currentPoints,
            maxVal = maxVal,
            color = params.graphColor,
            leftMargin = leftMargin,
            topPadding = topPadding,
            bottomMargin = bottomMargin
        )
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

private fun interpolatePoints(
    oldPoints: List<BigDecimal>,
    newPoints: List<BigDecimal>,
    factor: Float
): List<BigDecimal> {
    if (factor >= 1f) return newPoints
    if (factor <= 0f) return oldPoints

    val targetSize = maxOf(oldPoints.size, newPoints.size).coerceAtLeast(20)
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
    val labelStyle: TextStyle,
    val tertiaryColor: Color
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
    val today = LocalDate.now()
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
        val isToday = date == today

        val shouldAttemptLabel = isFirst || isEnd || isToday || dateText != lastLabelText

        if (shouldAttemptLabel) {
            val style = if (isToday) {
                params.labelStyle.copy(color = params.tertiaryColor, fontWeight = FontWeight.Bold)
            } else {
                params.labelStyle
            }
            val textLayoutResult = textMeasurer.measure(dateText, style)
            val labelWidth = textLayoutResult.size.width.toFloat()
            val labelStartX = x - labelWidth / 2

            if (labelStartX > lastLabelEndX + minGap) {
                labelsToDraw.add(Triple(x, dateText, index))
                lastLabelEndX = x + labelWidth / 2
                lastLabelText = dateText
            } else if (isEnd && labelsToDraw.isNotEmpty()) {
                labelsToDraw.removeAt(labelsToDraw.size - 1)
                labelsToDraw.add(Triple(x, dateText, index))
            } else if (isToday && labelsToDraw.isNotEmpty()) {
                val last = labelsToDraw.last()
                val lastX = last.first
                val lastText = last.second
                val lastMeasured = textMeasurer.measure(lastText, params.labelStyle)
                if (x - labelWidth / 2 < lastX + lastMeasured.size.width / 2 + minGap) {
                    labelsToDraw.removeAt(labelsToDraw.size - 1)
                }
                labelsToDraw.add(Triple(x, dateText, index))
                lastLabelEndX = x + labelWidth / 2
                lastLabelText = dateText
            }
        }
    }

    currentPoints.forEachIndexed { index, _ ->
        val x = leftMargin + index * stepWidth
        val date = startLocalDate.plusDays((windowStartIndex + index).toLong())
        val isToday = date == today

        drawLine(
            color = if (isToday) params.tertiaryColor.copy(alpha = 0.4f) else params.gridColor.copy(
                alpha = 0.2f
            ),
            start = Offset(x, topPadding),
            end = Offset(x, height - bottomMargin),
            strokeWidth = if (isToday) 2.dp.toPx() else 1.dp.toPx()
        )

        labelsToDraw.find { it.third == index }?.let { (_, dateText, _) ->
            val style = if (isToday) {
                params.labelStyle.copy(
                    color = params.tertiaryColor,
                    fontWeight = FontWeight.ExtraBold
                )
            } else {
                params.labelStyle
            }
            val textLayoutResult = textMeasurer.measure(dateText, style)
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
        drawGraphArea(
            points = currentPoints,
            maxVal = maxVal,
            color = params.graphColor,
            leftMargin = leftMargin,
            topPadding = topPadding,
            bottomMargin = bottomMargin
        )
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

private fun formatAxisValue(
    value: BigDecimal,
    currencyFormat: java.text.Format,
    thousandsUnit: String,
    millionsUnit: String
): String {
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

private fun DrawScope.drawGraphArea(
    points: List<BigDecimal>,
    maxVal: Float,
    color: Color,
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
        val lastX = leftMargin + (points.size - 1) * stepWidth
        lineTo(lastX, canvasHeight - bottomMargin)
        lineTo(leftMargin, canvasHeight - bottomMargin)
        close()
    }

    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(
                color.copy(alpha = 0.35f),
                color.copy(alpha = 0.15f),
                Color.Transparent
            ),
            startY = topPadding,
            endY = canvasHeight - bottomMargin
        ),
        style = Fill
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
                            date = LocalDateTime.now().minusDays(5)
                        ),
                        Transaction(
                            amount = BigDecimal("20"),
                            date = LocalDateTime.now().minusDays(3)
                        ),
                        Transaction(
                            amount = BigDecimal("50"),
                            date = LocalDateTime.now().minusDays(1)
                        )
                    ),
                    startPeriodDate = Date.from(
                        LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault())
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
@Preview(
    showBackground = true, name = "Tooltip Preview",
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
                    oldMaxVal = 100f,
                    newMaxVal = 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    currencyCode = "USD",
                    forcedTouchPosition = Offset(200f, 100f),
                    startDate = Date(),
                    currentWindowIndex = 0,
                    oldWindowIndex = 0,
                    scrollStep = 1,
                    oldScrollStep = 1,
                    renderDataSize = 7,
                    oldDataSize = 7,
                    animProgress = 1f,
                    dateFormatter = DateTimeFormatter.ofPattern("dd MMM")
                )
            }
        }
    }
}
