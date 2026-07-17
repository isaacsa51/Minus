package com.serranoie.app.minus.presentation.ui.analytics

import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.SavingsPreferences
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.LocalWindowInsets
import com.serranoie.app.minus.presentation.ui.analytics.dialogs.CategoryAnalytics
import com.serranoie.app.minus.presentation.ui.analytics.dialogs.CategoryAnalyticsState
import com.serranoie.app.minus.presentation.ui.analytics.util.previewAnalyticsState
import com.serranoie.app.minus.presentation.ui.history.HistoryScreen
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.FinishedPeriodHeader
import com.serranoie.app.minus.presentation.ui.theme.component.MiddlePeriodHeader
import com.serranoie.app.minus.presentation.ui.theme.component.SavingsRecommendationCard
import com.serranoie.app.minus.presentation.ui.theme.component.budget.AverageSpendCard
import com.serranoie.app.minus.presentation.ui.theme.component.budget.BudgetDisplay
import com.serranoie.app.minus.presentation.ui.theme.component.budget.CreditOwedCard
import com.serranoie.app.minus.presentation.ui.theme.component.budget.MinMaxSpentCard
import com.serranoie.app.minus.presentation.ui.theme.component.budget.SpendBudgetCard
import com.serranoie.app.minus.presentation.ui.theme.component.budget.SpendsCountCard
import com.serranoie.app.minus.presentation.ui.theme.component.charts.CategoriesChartCard
import com.serranoie.app.minus.presentation.ui.theme.component.charts.SpendsChart
import com.serranoie.app.minus.presentation.ui.theme.component.date.CalendarHeatmap
import com.serranoie.app.minus.presentation.util.Utils.strongHapticFeedback
import com.serranoie.app.minus.presentation.util.Utils.weakHapticFeedback
import java.math.BigDecimal
import java.util.Date

data class AnalyticsState(
    val periodFinished: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val spends: List<Transaction> = emptyList(),
    val recurringInPeriod: List<Transaction> = emptyList(),
    val oneTimeSpends: List<Transaction> = emptyList(),
    val wholeBudget: BigDecimal = BigDecimal.ZERO,
    val currencyCode: String = "USD",
    val finishPeriodActualDate: Date? = null,
    val startPeriodDate: Date = Date(),
    val finishPeriodDate: Date? = null,
    val extraAffordableDaysFromRemaining: Int = 0,
    val budgetSettingsForDisplay: BudgetSettings? = null,
    val budgetStateForDisplay: BudgetState? = null,
    val showRolloverStyleInBudgetDisplay: Boolean = false,
    val isLoading: Boolean = false,
    val savingsPreferences: SavingsPreferences = SavingsPreferences.DEFAULT,
    val creditOwed: BigDecimal = BigDecimal.ZERO,
    val debtAdjustedBalance: BigDecimal = BigDecimal.ZERO,
    val creditTransactions: List<Transaction> = emptyList(),
)

data class AnalyticsActions(
    val onCreateNewPeriod: () -> Unit = {},
    val onClose: () -> Unit = {},
    val onExportCSV: () -> Unit = {},
    val onMarkCreditPaid: () -> Unit = {},
)

data class Size(val width: Dp, val height: Dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Analytics(
    state: AnalyticsState = AnalyticsState(),
    actions: AnalyticsActions = AnalyticsActions(),
    activityResultRegistryOwner: ActivityResultRegistryOwner? = null,
) {
    val view = LocalView.current
    val scrollState = rememberScrollState()
    var showHistorySheet by remember { mutableStateOf(false) }
    var showCreditSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedCategory by remember { mutableStateOf<CategoryAnalyticsState?>(null) }

    val navigationBarHeight =
        LocalWindowInsets.current.calculateBottomPadding().coerceAtLeast(16.dp)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (!state.periodFinished) {
                MiddlePeriodHeader(
                    onClose = actions.onClose,
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

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                if (state.periodFinished) {
                    FinishedPeriodHeader(
                        scrollState = scrollState,
                        hasSpends = state.spends.isNotEmpty(),
                        isOverBudget = state.spends.sumOf { it.amount } > state.wholeBudget,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                BudgetDisplay(
                    budget = state.wholeBudget,
                    budgetState = state.budgetStateForDisplay,
                    budgetSettings = state.budgetSettingsForDisplay,
                    currencyCode = state.currencyCode,
                    startDate = state.startPeriodDate,
                    finishDate = state.finishPeriodDate,
                    actualFinishDate = state.finishPeriodActualDate,
                    extraDaysFromRemaining = state.extraAffordableDaysFromRemaining,
                    showRolloverStyle = state.showRolloverStyleInBudgetDisplay,
                    debtAdjustedBalance = state.debtAdjustedBalance,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                AnalyticsResponsiveLayout(
                    useTabletLayout = useWideAnalyticsLayout,
                    state = state,
                    onShowHistory = {
                        showHistorySheet = true
                        view.weakHapticFeedback()
                    },
                    onShowCreditDetails = { showCreditSheet = true },
                    onCategoryClick = { categoryName, categorySpends ->
                        selectedCategory =
                            state.toCategoryAnalyticsState(categoryName, categorySpends)
                        view.weakHapticFeedback()
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                SavingsRecommendationCard(
                    budget = state.wholeBudget,
                    recurringInPeriod = state.recurringInPeriod,
                    oneTimeSpends = state.oneTimeSpends,
                    currency = state.currencyCode,
                    preferences = state.savingsPreferences,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(80.dp + navigationBarHeight))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(androidx.compose.ui.Alignment.BottomCenter)
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

    if (selectedCategory != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedCategory = null },
            sheetState = sheetState,
        ) {
            CategoryAnalytics(state = selectedCategory!!)
        }
    }

    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = sheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle()
            }
        ) {
            HistoryScreen(
                readOnly = true,
            )
        }
    }

    if (showCreditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreditSheet = false },
            sheetState = sheetState,
        ) {
            com.serranoie.app.minus.presentation.ui.theme.component.budget.CreditTransactionsBottomSheet(
                transactions = state.creditTransactions,
                totalOwed = state.creditOwed,
                currency = state.currencyCode,
                onPayClick = {
                    actions.onMarkCreditPaid()
                    showCreditSheet = false
                }
            )
        }
    }
}

@Composable
private fun AnalyticsResponsiveLayout(
    useTabletLayout: Boolean,
    state: AnalyticsState,
    onShowHistory: () -> Unit,
    onShowCreditDetails: () -> Unit,
    onCategoryClick: (String, List<Transaction>) -> Unit,
) {
    if (useTabletLayout) {
        AnalyticsTabletLayout(
            state = state,
            onShowHistory = onShowHistory,
            onShowCreditDetails = onShowCreditDetails,
            onCategoryClick = onCategoryClick,
        )
    } else {
        AnalyticsCompactLayout(
            state = state,
            onShowHistory = onShowHistory,
            onShowCreditDetails = onShowCreditDetails,
            onCategoryClick = onCategoryClick,
        )
    }
}

@Composable
private fun AnalyticsCompactLayout(
    state: AnalyticsState,
    onShowHistory: () -> Unit,
    onShowCreditDetails: () -> Unit,
    onCategoryClick: (String, List<Transaction>) -> Unit,
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
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentHeight(),
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
        ) {
            MinMaxSpentCard(
                isMin = true,
                spends = state.spends,
                currency = state.currencyCode,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            Spacer(modifier = Modifier.width(16.dp))
            MinMaxSpentCard(
                isMin = false,
                spends = state.spends,
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
                        .aspectRatio(1f),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        CategoriesChartCard(
            spends = state.spends,
            currency = state.currencyCode,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            onCategoryClick = onCategoryClick,
        )
    }
}

@Composable
private fun AnalyticsTabletLayout(
    state: AnalyticsState,
    onShowHistory: () -> Unit,
    onShowCreditDetails: () -> Unit,
    onCategoryClick: (String, List<Transaction>) -> Unit,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                MinMaxSpentCard(
                    isMin = true,
                    spends = state.spends,
                    currency = state.currencyCode,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                Spacer(modifier = Modifier.width(16.dp))
                MinMaxSpentCard(
                    isMin = false,
                    spends = state.spends,
                    currency = state.currencyCode,
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
                        .aspectRatio(1f),
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
            modifier = Modifier.fillMaxWidth(),
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
)

@Preview
@Composable
private fun PreviewAnalytics() {
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
private fun PreviewAnalyticsNotFinished() {
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
private fun PreviewAnalyticsFinished() {
    MinusTheme {
        Surface {
            Analytics(
                state = previewAnalyticsState(periodFinished = true),
            )
        }
    }
}
