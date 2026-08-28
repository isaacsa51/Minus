package com.serranoie.app.minus.presentation.ui.analytics

import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.ArchivedBudget
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.domain.model.SavingsPreferences
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.model.UserSettings
import com.serranoie.app.minus.presentation.LocalWindowInsets
import com.serranoie.app.minus.presentation.ui.analytics.dialogs.CategoryAnalytics
import com.serranoie.app.minus.presentation.ui.analytics.dialogs.CategoryAnalyticsState
import com.serranoie.app.minus.presentation.ui.analytics.dialogs.PastPeriodsBottomSheet
import com.serranoie.app.minus.presentation.ui.analytics.util.previewAnalyticsState
import com.serranoie.app.minus.presentation.ui.history.History
import com.serranoie.app.minus.presentation.ui.history.HistoryUiIntent
import com.serranoie.app.minus.presentation.ui.history.HistoryUiState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.FinishedPeriodHeader
import com.serranoie.app.minus.presentation.ui.theme.component.MiddlePeriodHeader
import com.serranoie.app.minus.presentation.ui.theme.component.SavingsRecommendationCard
import com.serranoie.app.minus.presentation.ui.theme.component.budget.AverageSpendCard
import com.serranoie.app.minus.presentation.ui.theme.component.budget.CreditOwedCard
import com.serranoie.app.minus.presentation.ui.theme.component.budget.CreditTransactionsBottomSheet
import com.serranoie.app.minus.presentation.ui.theme.component.budget.MinMaxSpentCard
import com.serranoie.app.minus.presentation.ui.theme.component.budget.SpendBudgetCard
import com.serranoie.app.minus.presentation.ui.theme.component.budget.SpendsCountCard
import com.serranoie.app.minus.presentation.ui.theme.component.budget.graphs.BudgetGraph
import com.serranoie.app.minus.presentation.ui.theme.component.charts.CategoriesChartCard
import com.serranoie.app.minus.presentation.ui.theme.component.charts.SpendsChart
import com.serranoie.app.minus.presentation.ui.theme.component.date.CalendarHeatmap
import com.serranoie.app.minus.presentation.ui.tutorial.TutorialBox
import com.serranoie.app.minus.presentation.ui.tutorial.TutorialTooltip
import com.serranoie.app.minus.presentation.ui.tutorial.markForTutorial
import com.serranoie.app.minus.presentation.ui.tutorial.rememberTutorialBoxState
import com.serranoie.app.minus.presentation.util.Utils.confirmFeedback
import com.serranoie.app.minus.presentation.util.Utils.strongHapticFeedback
import com.serranoie.app.minus.presentation.util.Utils.weakHapticFeedback
import com.serranoie.app.minus.presentation.util.combineColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import logcat.logcat
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds

private val STABLE_DEFAULT_DATE = Date(0)


@Immutable
data class AnalyticsState(
    val periodFinished: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val allTransactions: List<Transaction> = emptyList(),
    val spends: List<Transaction> = emptyList(),
    val chartSpends: List<Transaction> = emptyList(),
    val recurringInPeriod: List<Transaction> = emptyList(),
    val oneTimeSpends: List<Transaction> = emptyList(),
    val wholeBudget: BigDecimal = BigDecimal.ZERO,
    val currencyCode: String = "USD",
    val finishPeriodActualDate: Date? = null,
    val startPeriodDate: Date = STABLE_DEFAULT_DATE,
    val finishPeriodDate: Date? = null,
    val extraAffordableDaysFromRemaining: Int = 0,
    val budgetSettingsForDisplay: BudgetSettings? = null,
    val budgetStateForDisplay: BudgetState? = null,
    val showRolloverStyleInBudgetDisplay: Boolean = false,
    val isLoading: Boolean = true,
    val savingsPreferences: SavingsPreferences = SavingsPreferences.DEFAULT,
    val creditOwed: BigDecimal = BigDecimal.ZERO,
    val debtAdjustedBalance: BigDecimal = BigDecimal.ZERO,
    val creditTransactions: List<Transaction> = emptyList(),
    val isHistoricalView: Boolean = false,
    val userSettings: UserSettings = UserSettings.DEFAULT,
    val previousPeriodTransactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val graphGranularity: GraphGranularity = GraphGranularity.TOTAL,
)

data class AnalyticsActions(
    val onCreateNewPeriod: () -> Unit = {},
    val onClose: () -> Unit = {},
    val onExportCSV: () -> Unit = {},
    val onMarkCreditPaid: () -> Unit = {},
    val onPayTransactionClick: (Long) -> Unit = {},
    val onCutoffDayChanged: (Int) -> Unit = {},
    val onHistoricalPeriodSelected: (Long) -> Unit = {},
    val onTutorialCompleted: (Boolean) -> Unit = {},
    val onGranularityChanged: (GraphGranularity) -> Unit = {},
)

data class Size(val width: Dp, val height: Dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Analytics(
    state: AnalyticsState = AnalyticsState(),
    archivedBudgets: List<ArchivedBudget> = emptyList(),
    categories: List<Category> = emptyList(),
    actions: AnalyticsActions = AnalyticsActions(),
    activityResultRegistryOwner: ActivityResultRegistryOwner? = null,
    showTutorialOverride: Boolean? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scrollState = rememberScrollState()
    var showHistorySheet by remember { mutableStateOf(false) }
    var historyExpandedDates by remember { mutableStateOf(setOf<LocalDate>()) }
    var showCreditSheet by remember { mutableStateOf(false) }
    var showPastPeriodsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val hasSpends = state.spends.isNotEmpty()
    val tutorialCompleted = if (hasSpends) {
        state.userSettings.analyticsSpendsTutorialCompleted
    } else {
        state.userSettings.analyticsTutorialCompleted
    }

    val effectiveTutorialCompleted =
        showTutorialOverride?.let { !it } ?: (tutorialCompleted || state.isHistoricalView)

    val tutorialOrder =
        remember(hasSpends, state.creditOwed, state.periodFinished, state.isHistoricalView) {
            buildList {
                // 1. Header (if visible)
                if (!state.periodFinished || state.isHistoricalView) {
                    add(1)
                }

                // 2. Budget Summary (Graph)
                add(2)

                if (hasSpends) {
                    add(3) // Heatmap
                    add(0) // MinMax
                    if (state.creditOwed > BigDecimal.ZERO) {
                        add(6) // Credit Owed
                    }
                    add(4) // Categories
                } else {
                    add(5) // Savings
                    if (state.creditOwed > BigDecimal.ZERO) {
                        add(6) // Credit Owed
                    }
                }
            }
        }

    val tutorialBoxState = rememberTutorialBoxState(
        order = tutorialOrder,
        virtual = emptySet(),
    )

    var scrollableContainerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val componentPositions = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }

    val markIfInOrder: Modifier.(Int) -> Modifier = { index ->
        if (index in tutorialOrder) {
            this
                .onGloballyPositioned { coords ->
                    scrollableContainerCoordinates?.let { container ->
                        val position = container.localPositionOf(
                            coords, androidx.compose.ui.geometry.Offset.Zero
                        )
                        componentPositions[index] = position.y to coords.size.height.toFloat()
                    }
                }
                .markForTutorial(tutorialBoxState, index)
        } else this
    }

    val bringIntoViewRequesters = remember {
        mapOf(
            0 to BringIntoViewRequester(),
            1 to BringIntoViewRequester(),
            2 to BringIntoViewRequester(),
            3 to BringIntoViewRequester(),
            4 to BringIntoViewRequester(),
            5 to BringIntoViewRequester(),
            6 to BringIntoViewRequester(),
        )
    }

    LaunchedEffect(tutorialBoxState.currentIndexState.value) {
        val index = tutorialBoxState.currentIndexState.value
        if ((index != -1) && !effectiveTutorialCompleted) {
            delay(600.milliseconds)

            val position = componentPositions[index]
            if (position != null && scrollableContainerCoordinates != null) {
                val (offsetY, height) = position
                val containerHeight = scrollableContainerCoordinates!!.size.height
                val targetScroll = (offsetY - (containerHeight - height) / 2f).toInt()
                scrollState.animateScrollTo(targetScroll.coerceIn(0, scrollState.maxValue))
            } else {
                bringIntoViewRequesters[index]?.bringIntoView()
            }
        }
    }

    var selectedCategory by remember { mutableStateOf<CategoryAnalyticsState?>(null) }
    var selectedDayData by remember { mutableStateOf<CategoryAnalyticsState?>(null) }

    val scope = rememberCoroutineScope()

    val navigationBarHeight =
        LocalWindowInsets.current.calculateBottomPadding().coerceAtLeast(16.dp)

    val locale = LocalConfiguration.current.locales[0]

    var selectedChartDate by remember { mutableStateOf<LocalDate?>(null) }

    val openDaySheet: (LocalDate) -> Unit = { date ->
        val daySpends = state.spends.filter { it.date?.toLocalDate() == date }
        val formatter = DateTimeFormatter.ofPattern("dd MMMM", locale)
        selectedDayData = state.toCategoryAnalyticsState(
            categoryName = date.format(formatter),
            categorySpends = daySpends
        ).copy(isDayView = true)
        view.weakHapticFeedback()
    }

    // Tapping a calendar day used to open the day's BottomSheet (openDaySheet); it now instead
    // surfaces that day directly on the Days-granularity DetailedChart.
    val onCalendarDayClick: (LocalDate) -> Unit = { date ->
        // openDaySheet(date)
        selectedChartDate = date
        if (state.graphGranularity != GraphGranularity.DAYS) {
            actions.onGranularityChanged(GraphGranularity.DAYS)
        }
        view.confirmFeedback()
    }

    TutorialBox(
        showTutorial = !effectiveTutorialCompleted && !state.isLoading,
        state = tutorialBoxState,
        onTutorialCompleted = {
            actions.onTutorialCompleted(hasSpends)
        },
        tutorialTarget = { index ->
            when (index) {
                0 -> TutorialTooltip(
                    title = stringResource(R.string.analytics_tutorial_minmax_title),
                    description = stringResource(R.string.analytics_tutorial_minmax_desc)
                )

                1 -> TutorialTooltip(
                    title = stringResource(R.string.analytics_tutorial_header_title),
                    description = stringResource(R.string.analytics_tutorial_header_desc)
                )

                2 -> TutorialTooltip(
                    title = stringResource(R.string.analytics_tutorial_budget_title),
                    description = stringResource(R.string.analytics_tutorial_budget_desc)
                )

                3 -> TutorialTooltip(
                    title = stringResource(R.string.analytics_tutorial_heatmap_title),
                    description = stringResource(R.string.analytics_tutorial_heatmap_desc)
                )

                4 -> TutorialTooltip(
                    title = stringResource(R.string.analytics_tutorial_charts_title),
                    description = stringResource(R.string.analytics_tutorial_charts_desc)
                )

                5 -> TutorialTooltip(
                    title = stringResource(R.string.analytics_tutorial_savings_title),
                    description = stringResource(R.string.analytics_tutorial_savings_desc)
                )

                6 -> TutorialTooltip(
                    title = stringResource(R.string.analytics_tutorial_credit_title),
                    description = stringResource(R.string.analytics_tutorial_credit_desc)
                )
            }
        }) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (!state.periodFinished || state.isHistoricalView) {
                    MiddlePeriodHeader(
                        onClose = actions.onClose,
                        onShowPastPeriods = { showPastPeriodsSheet = true },
                        historyIconModifier = Modifier.bringIntoViewRequester(
                            bringIntoViewRequesters[1]!!
                        ).markIfInOrder(1)
                    )
                }
            },
        ) { paddingValues ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val useWideAnalyticsLayout = maxWidth >= 840.dp

                if (state.periodFinished) {
                    FinishedPeriodBackgroundShapes(
                        scrollState = scrollState,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(top = 500.dp)
                            .height(800.dp),
                    )
                }

                val transitionKey = remember(state.periodFinished, state.isHistoricalView) {
                    "${state.periodFinished}-${state.isHistoricalView}"
                }
                AnimatedContent(
                    targetState = transitionKey, transitionSpec = {
                        fadeIn(animationSpec = tween(500)) togetherWith fadeOut(
                            animationSpec = tween(
                                500
                            )
                        )
                    }, label = "AnalyticsContentTransition"
                ) { target ->
                    logcat("Analytics") { "Displaying state for: $target" }
                    Column(
                        Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { scrollableContainerCoordinates = it }
                            .verticalScroll(scrollState)) {
                        if (state.periodFinished) {
                            FinishedPeriodHeader(
                                scrollState = scrollState,
                                hasSpends = state.spends.isNotEmpty(),
                                isOverBudget = state.spends.sumOf { it.amount } > state.wholeBudget,
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        BudgetGraph(
                            state = state,
                            onGranularityChanged = actions.onGranularityChanged,
                            modifier = Modifier.padding(horizontal = 16.dp)
                                .bringIntoViewRequester(bringIntoViewRequesters[2]!!)
                                .markIfInOrder(2),
                            onDayTap = openDaySheet,
                            onSelectedDateChanged = { date -> selectedChartDate = date },
                            selectedDate = selectedChartDate,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        AnalyticsResponsiveLayout(
                            useTabletLayout = useWideAnalyticsLayout,
                            state = state,
                            categories = categories,
                            onShowHistory = {
                                showHistorySheet = true
                                view.weakHapticFeedback()
                            },
                            onShowCreditDetails = { showCreditSheet = true },
                            onCategoryClick = { categoryName, categorySpends ->
                                selectedCategory = state.toCategoryAnalyticsState(
                                    categoryName, categorySpends
                                )
                                view.weakHapticFeedback()
                            },
                            onDayClick = onCalendarDayClick,
                            bringIntoViewRequesters = bringIntoViewRequesters,
                            markIfInOrder = markIfInOrder,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SavingsRecommendationCard(
                            budget = state.wholeBudget,
                            recurringInPeriod = state.recurringInPeriod,
                            oneTimeSpends = state.oneTimeSpends,
                            currency = state.currencyCode,
                            preferences = state.savingsPreferences,
                            modifier = Modifier.padding(horizontal = 16.dp)
                                .bringIntoViewRequester(bringIntoViewRequesters[5]!!)
                                .markIfInOrder(5),
                        )

                        Spacer(modifier = Modifier.height(80.dp + navigationBarHeight))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .zIndex(1f)
                        .padding(bottom = navigationBarHeight, start = 16.dp, end = 16.dp)
                ) {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(60.dp),
                        onClick = {
                            view.strongHapticFeedback()
                            actions.onCreateNewPeriod()
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.new_budget),
                            style = MaterialTheme.typography.labelMediumEmphasized,
                        )
                    }
                }
            }
        }
    }

    if (selectedCategory != null || selectedDayData != null) {
        ModalBottomSheet(
            onDismissRequest = {
                selectedCategory = null
                selectedDayData = null
            },
            sheetState = sheetState,
        ) {
            val displayState = selectedCategory ?: selectedDayData!!
            CategoryAnalytics(state = displayState)
        }
    }

    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle()
            }) {
            val groupedTransactions = remember(state.transactions) {
                state.transactions.sortedByDescending { it.date }.groupBy { it.date?.toLocalDate() }
                    .toSortedMap(compareByDescending { it })
            }

            LaunchedEffect(groupedTransactions) {
                if (historyExpandedDates.isEmpty() && groupedTransactions.isNotEmpty()) {
                    groupedTransactions.keys.firstOrNull()?.let {
                        historyExpandedDates = setOf(it)
                    }
                }
            }

            History(
                uiState = HistoryUiState(
                    transactions = state.transactions,
                    displayTransactions = state.transactions,
                    groupedCurrentTransactions = groupedTransactions,
                    budgetSettings = state.budgetSettingsForDisplay,
                    budgetState = state.budgetStateForDisplay,
                    creditOwed = state.creditOwed,
                    debtAdjustedBalance = state.debtAdjustedBalance,
                    expandedDates = historyExpandedDates,
                ), readOnly = true, onProcessIntent = { intent ->
                    if (intent is HistoryUiIntent.ToggleExpandedDate) {
                        historyExpandedDates = if (historyExpandedDates.contains(intent.date)) {
                            historyExpandedDates - intent.date
                        } else {
                            historyExpandedDates + intent.date
                        }
                    }
                })
        }
    }

    if (showPastPeriodsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPastPeriodsSheet = false },
            sheetState = sheetState,
        ) {
            PastPeriodsBottomSheet(
                periods = archivedBudgets,
                allTransactions = state.allTransactions,
                onPeriodClick = { archivedBudget ->
                    scope.launch {
                        sheetState.hide()
                        showPastPeriodsSheet = false
                        actions.onHistoricalPeriodSelected(archivedBudget.periodId)
                    }
                },
            )
        }
    }

    if (showCreditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreditSheet = false },
            sheetState = sheetState,
        ) {
           CreditTransactionsBottomSheet(
                transactions = state.creditTransactions,
                totalOwed = state.creditOwed,
                currency = state.currencyCode,
                onPayClick = {
                    actions.onMarkCreditPaid()
                    showCreditSheet = false
                },
                onPayTransactionClick = actions.onPayTransactionClick,
                creditCardCutoffDay = state.budgetSettingsForDisplay?.creditCardCutoffDay,
                onCutoffDayChanged = actions.onCutoffDayChanged
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FinishedPeriodBackgroundShapes(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    val localDensity = LocalDensity.current
    var containerSize by remember { mutableStateOf(Size(0.dp, 0.dp)) }
    val scroll = with(localDensity) { scrollState.value.toDp() }

    Box(
        modifier = modifier
            .onGloballyPositioned {
                containerSize = Size(
                    width = with(localDensity) { it.size.width.toDp() },
                    height = with(localDensity) { it.size.height.toDp() })
            },
        contentAlignment = Alignment.Center,
    ) {
        val halfWidth = containerSize.width / 2
        val halfHeight = containerSize.height / 2

        val shapeColor = combineColors(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.surface,
            0.5f,
        ).copy(alpha = 0.6f)

        val angleShape1 by rememberInfiniteTransition("angleBgShape1").animateFloat(
            label = "angleBgShape1",
            initialValue = -30f,
            targetValue = 30f,
            animationSpec = infiniteRepeatable(tween(16000), RepeatMode.Reverse)
        )

        val angleShape4 by rememberInfiniteTransition("angleBgShape4").animateFloat(
            label = "angleBgShape4",
            initialValue = 55f,
            targetValue = -55f,
            animationSpec = infiniteRepeatable(tween(26000), RepeatMode.Reverse)
        )

        Box(
            modifier = Modifier
                .requiredSize(256.dp)
                .absoluteOffset(x = halfWidth * 0.85f, y = -halfHeight * 0.7f - scroll * 0.1f)
                .rotate(angleShape1)
                .background(color = shapeColor, shape = MaterialShapes.Clover4Leaf.toShape())
        )

        Box(
            modifier = Modifier
                .requiredSize(256.dp)
                .absoluteOffset(x = -halfWidth * 0.55f, y = halfHeight * 0.35f - scroll * 0.25f)
                .rotate(angleShape4)
                .background(color = shapeColor, shape = MaterialShapes.Flower.toShape())
        )
    }
}

@Composable
private fun AnalyticsResponsiveLayout(
    useTabletLayout: Boolean,
    state: AnalyticsState,
    categories: List<Category>,
    onShowHistory: () -> Unit,
    onShowCreditDetails: () -> Unit,
    onCategoryClick: (String, List<Transaction>) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    bringIntoViewRequesters: Map<Int, BringIntoViewRequester>,
    markIfInOrder: Modifier.(Int) -> Modifier,
) {
    if (useTabletLayout) {
        AnalyticsTabletLayout(
            state = state,
            categories = categories,
            onShowHistory = onShowHistory,
            onShowCreditDetails = onShowCreditDetails,
            onCategoryClick = onCategoryClick,
            onDayClick = onDayClick,
            bringIntoViewRequesters = bringIntoViewRequesters,
            markIfInOrder = markIfInOrder,
        )
    } else {
        AnalyticsCompactLayout(
            state = state,
            categories = categories,
            onShowHistory = onShowHistory,
            onShowCreditDetails = onShowCreditDetails,
            onCategoryClick = onCategoryClick,
            onDayClick = onDayClick,
            bringIntoViewRequesters = bringIntoViewRequesters,
            markIfInOrder = markIfInOrder,
        )
    }
}

@Composable
private fun AnalyticsCompactLayout(
    state: AnalyticsState,
    categories: List<Category>,
    onShowHistory: () -> Unit,
    onShowCreditDetails: () -> Unit,
    onCategoryClick: (String, List<Transaction>) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    bringIntoViewRequesters: Map<Int, BringIntoViewRequester>,
    markIfInOrder: Modifier.(Int) -> Modifier,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            if (state.finishPeriodDate != null && state.transactions.isNotEmpty()) {
                CalendarHeatmap(
                    transactions = state.transactions,
                    budget = state.wholeBudget,
                    startDate = state.startPeriodDate,
                    finishDate = state.finishPeriodDate,
                    onDayClick = onDayClick,
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentHeight()
                        .bringIntoViewRequester(bringIntoViewRequesters[3]!!)
                        .markIfInOrder(3),
                )
            }
        }
        SpendsChart(
            spends = state.spends,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(0.dp, 400.dp)
                .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(IntrinsicSize.Min)
                .bringIntoViewRequester(bringIntoViewRequesters[0]!!)
                .markIfInOrder(0)
        ) {
            MinMaxSpentCard(
                isMin = true,
                spends = state.spends,
                currency = state.currencyCode,
                categories = categories,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            Spacer(modifier = Modifier.width(16.dp))
            MinMaxSpentCard(
                isMin = false,
                spends = state.spends,
                currency = state.currencyCode,
                categories = categories,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            SpendsCountCard(
                count = state.spends.size,
                onClick = onShowHistory,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            Spacer(modifier = Modifier.width(16.dp))
            AverageSpendCard(
                spends = state.spends,
                startDate = state.startPeriodDate,
                finishDate = state.finishPeriodDate,
                currency = state.currencyCode,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(IntrinsicSize.Min)
        ) {
            SpendBudgetCard(
                budget = state.wholeBudget,
                spend = state.spends.sumOf { it.amount },
                currency = state.currencyCode,
                modifier = Modifier.weight(1f),
            )
            if (state.creditOwed > BigDecimal.ZERO) {
                Spacer(modifier = Modifier.width(16.dp))
                CreditOwedCard(
                    owed = state.creditOwed,
                    currency = state.currencyCode,
                    onClick = onShowCreditDetails,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .bringIntoViewRequester(bringIntoViewRequesters[6]!!)
                        .markIfInOrder(6),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        CategoriesChartCard(
            spends = state.spends,
            currency = state.currencyCode,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequesters[4]!!)
                .markIfInOrder(4),
            onCategoryClick = onCategoryClick,
        )
    }
}

@Composable
private fun AnalyticsTabletLayout(
    state: AnalyticsState,
    categories: List<Category>,
    onShowHistory: () -> Unit,
    onShowCreditDetails: () -> Unit,
    onCategoryClick: (String, List<Transaction>) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    bringIntoViewRequesters: Map<Int, BringIntoViewRequester>,
    markIfInOrder: Modifier.(Int) -> Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .requiredHeightIn(min = 180.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (state.finishPeriodDate != null && state.transactions.isNotEmpty()) {
                    CalendarHeatmap(
                        transactions = state.transactions,
                        budget = state.wholeBudget,
                        startDate = state.startPeriodDate,
                        finishDate = state.finishPeriodDate,
                        onDayClick = onDayClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .bringIntoViewRequester(bringIntoViewRequesters[3]!!)
                            .markIfInOrder(3),
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .bringIntoViewRequester(bringIntoViewRequesters[0]!!)
                    .markIfInOrder(0)
            ) {
                MinMaxSpentCard(
                    isMin = true,
                    spends = state.spends,
                    currency = state.currencyCode,
                    categories = categories,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                Spacer(modifier = Modifier.width(16.dp))
                MinMaxSpentCard(
                    isMin = false,
                    spends = state.spends,
                    currency = state.currencyCode,
                    categories = categories,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            SpendBudgetCard(
                budget = state.wholeBudget,
                spend = state.spends.sumOf { it.amount },
                currency = state.currencyCode,
                modifier = Modifier.weight(1f),
            )
            if (state.creditOwed > BigDecimal.ZERO) {
                Spacer(modifier = Modifier.width(16.dp))
                CreditOwedCard(
                    owed = state.creditOwed,
                    currency = state.currencyCode,
                    onClick = onShowCreditDetails,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .bringIntoViewRequester(bringIntoViewRequesters[6]!!)
                        .markIfInOrder(6),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
            ) {
                SpendsCountCard(
                    count = state.spends.size,
                    onClick = onShowHistory,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                Spacer(modifier = Modifier.width(8.dp))
                AverageSpendCard(
                    spends = state.spends,
                    startDate = state.startPeriodDate,
                    finishDate = state.finishPeriodDate,
                    currency = state.currencyCode,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        CategoriesChartCard(
            spends = state.spends,
            currency = state.currencyCode,
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequesters[4]!!)
                .markIfInOrder(4),
            onCategoryClick = onCategoryClick,
        )
    }
}

private fun AnalyticsState.toCategoryAnalyticsState(
    categoryName: String,
    categorySpends: List<Transaction>,
): CategoryAnalyticsState = CategoryAnalyticsState(
    periodFinished = periodFinished,
    transactions = transactions,
    spends = spends,
    wholeBudget = wholeBudget,
    finishPeriodActualDate = finishPeriodActualDate,
    startPeriodDate = startPeriodDate,
    finishPeriodDate = finishPeriodDate,
    isLoading = isLoading,
    categoryName = categoryName,
    categorySpends = categorySpends,
    currencyCode = currencyCode,
    creditCardCutoffDay = budgetSettingsForDisplay?.creditCardCutoffDay,
)

@Preview
@Composable
fun AnalyticsMainPreview() {
    MinusTheme {
        Surface {
            Analytics(
                state = previewAnalyticsState(periodFinished = false),
            )
        }
    }
}

@PreviewScreenSizes
@Composable
fun PreviewAnalyticsNotFinished() {
    MinusTheme {
        Surface {
            Analytics(
                state = previewAnalyticsState(periodFinished = false),
            )
        }
    }
}

@PreviewScreenSizes
@Composable
fun PreviewAnalyticsFinished() {
    MinusTheme {
        Surface {
            Analytics(
                state = previewAnalyticsState(periodFinished = true),
            )
        }
    }
}
