package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.analytics.AnalyticsState
import com.serranoie.app.minus.presentation.ui.analytics.GraphGranularity
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.charts.baseColors
import com.serranoie.app.minus.presentation.util.Utils.confirmFeedback
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.math.abs

const val BUDGET_GRAPH_TAG = "BUDGET_GRAPH_TAG"

/** The inclusive date span currently visible on the chart's paginated window. */
data class ChartDateRange(val start: LocalDate, val end: LocalDate) {
    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)
}

/** Which representation [BudgetGraph] draws its currently visible window with. */
enum class BudgetGraphViewMode { CUMULATIVE, CATEGORIES }

/** One category's total on one day within the visible window, colored for [MultiCategoryChart]. */
data class CategoryDayEntry(
    val date: LocalDate,
    val label: String,
    val amount: BigDecimal,
    val color: Color,
)

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

/**
 * Groups [spends] falling within [dateRange] by (day, resolved category), coloring each category
 * with the same [baseColors]-hashed palette CategoriesChartCard uses, so a category reads as the
 * same color across screens.
 */
@Composable
private fun rememberCategoryDayEntries(
    spends: List<Transaction>,
    categories: List<Category>,
    dateRange: ChartDateRange?,
): List<CategoryDayEntry> {
    val uncategorizedLabel = stringResource(R.string.categories_chart_uncategorized)
    return remember(spends, categories, dateRange, uncategorizedLabel) {
        if (dateRange == null) return@remember emptyList()
        spends
            .mapNotNull { tx ->
                tx.date?.toLocalDate()
                    ?.let { date -> if (dateRange.contains(date)) date to tx else null }
            }
            .groupBy { (date, tx) ->
                date to (categories.find { it.id == tx.categoryId }?.name ?: uncategorizedLabel)
            }
            .map { (key, entries) ->
                val (date, label) = key
                CategoryDayEntry(
                    date = date,
                    label = label,
                    amount = entries.sumOf { it.second.amount },
                    color = baseColors[abs(label.hashCode()) % baseColors.size],
                )
            }
    }
}

@Composable
fun BudgetGraph(
    state: AnalyticsState,
    onGranularityChanged: (GraphGranularity) -> Unit,
    modifier: Modifier = Modifier,
    onVisibleDateRangeChanged: (ChartDateRange?) -> Unit = {},
    onDayTap: ((LocalDate) -> Unit)? = null,
    selectedDate: LocalDate? = null,
    initialViewMode: BudgetGraphViewMode = BudgetGraphViewMode.CUMULATIVE,
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

    var viewMode by remember { mutableStateOf(initialViewMode) }

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

    val visibleDateRange = remember(
        graphState.currentWindowIndex,
        graphState.windowSize,
        graphState.scrollStep,
        hasPeriod,
        state.startPeriodDate,
    ) {
        if (!hasPeriod) return@remember null
        val startLocalDate =
            state.startPeriodDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        val windowStartIndex = graphState.currentWindowIndex * graphState.scrollStep
        val windowEndIndex = windowStartIndex + (graphState.windowSize - 1).coerceAtLeast(0)
        ChartDateRange(
            start = startLocalDate.plusDays(windowStartIndex.toLong()),
            end = startLocalDate.plusDays(windowEndIndex.toLong()),
        )
    }

    LaunchedEffect(visibleDateRange) {
        onVisibleDateRangeChanged(visibleDateRange)
    }

    val categoryDayEntries = rememberCategoryDayEntries(
        spends = state.spends,
        categories = state.categories,
        dateRange = visibleDateRange,
    )
    val dayTotals = remember(categoryDayEntries) {
        categoryDayEntries.groupBy { it.date }
            .mapValues { (_, entries) -> entries.sumOf { it.amount } }
    }
    val categoryLegendEntries = remember(categoryDayEntries) {
        categoryDayEntries
            .distinctBy { it.label }
            .sortedBy { it.label }
            .map { LegendEntry(it.label, it.color) }
    }

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
        val isCategoriesMode = viewMode == BudgetGraphViewMode.CATEGORIES

        Column(modifier = Modifier.padding(20.dp)) {
            BudgetGraphHeader(
                totalSpent = totalSpent,
                currencyFormat = currencyFormat,
            )

            if (hasPeriod && state.spends.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                BudgetGraphViewModeToggle(
                    selected = viewMode,
                    onSelected = {
                        view.confirmFeedback()
                        viewMode = it
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isCategoriesMode) {
                if (categoryLegendEntries.isNotEmpty()) {
                    ChartLegend(entries = categoryLegendEntries)
                }
            } else {
                BudgetGraphLegend()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                if (isCategoriesMode) {
                    MultiCategoryChart(
                        entries = categoryDayEntries,
                        dayTotals = dayTotals,
                        currencyCode = state.currencyCode,
                        modifier = Modifier.fillMaxSize(),
                        startDate = if (hasPeriod) state.startPeriodDate else Date(),
                        windowIndex = graphState.currentWindowIndex,
                        scrollStep = graphState.scrollStep,
                        dataSize = graphState.windowSize,
                        dateFormatter = dateLabelFormatter,
                        selectedDate = selectedDate,
                        onDayTap = onDayTap,
                    )
                } else {
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
                        dateFormatter = dateLabelFormatter,
                    )
                }
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
