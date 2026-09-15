package com.serranoie.app.minus.presentation.ui.subscriptions

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.history.dialogs.DeleteRecurrentExpenseDialog
import com.serranoie.app.minus.presentation.ui.history.dialogs.TransactionEditDialog
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.date.DayTotalItem
import com.serranoie.app.minus.presentation.ui.theme.component.expense.SwipeableUpcomingRecurrentItem
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.time.LocalDate

/**
 * This screen's composables are split across several files in this package: the calendar
 * ([SubscriptionsCalendarSection]), the paginated weekly view ([SubscriptionsWeeklyCalendar]), the
 * category graph ([SubscriptionsCategoryGraph]), the frequency breakdown
 * ([SubscriptionsFrequencyBreakdown]), the hero card ([SubscriptionsHeroCard]), the view-mode
 * toggle ([SubscriptionsViewModeToggle]), the day-details sheet ([SubscriptionDayDetailsSheet]),
 * [DueTodayCard], and [SubscriptionsEmptyState] — plus the shared [frequencyText]
 * (`SubscriptionFormatting.kt`) helper they all draw on (everything lives in the same package, so
 * none of it needs importing here). The avatar/palette/badge helpers
 * ([com.serranoie.app.minus.presentation.ui.theme.component.expense.SubscriptionAvatar] and
 * friends, in `RecurringItemVisuals.kt`) live with [UpcomingRecurrentItemRow] instead, since
 * they're shared with the recurring-item rows used outside this screen too (e.g. History).
 */
data class SubscriptionsActions(
    val onBack: () -> Unit = {},
    val onConfirmPaid: (Transaction, LocalDate) -> Unit = { _, _ -> },
    val onSkip: (Transaction, LocalDate) -> Unit = { _, _ -> },
    val onToggleExpanded: (Long) -> Unit = {},
    val onEditRequested: (Transaction) -> Unit = {},
    val onEditCancelled: () -> Unit = {},
    val onSaveEdited: (Transaction) -> Unit = {},
    val onDeleteRequested: (Transaction) -> Unit = {},
    val onDeleteCancelled: () -> Unit = {},
    val onConfirmDelete: (Transaction) -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun Subscriptions(
    state: SubscriptionsUiState = SubscriptionsUiState(),
    actions: SubscriptionsActions = SubscriptionsActions(),
    expandedTransactionId: Long? = null,
    editingTransaction: Transaction? = null,
    recurrentToDelete: Transaction? = null,
    tags: List<String> = emptyList(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    modifier: Modifier = Modifier,
) {
    val currencyFormat = remember(state.currencyCode) { symbolOnlyCurrencyFormat(state.currencyCode) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var viewMode by remember { mutableStateOf(SubscriptionsViewMode.WHOLE_PERIOD) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.subscriptions_title),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = actions.onBack,
                        modifier = Modifier.testTag("SubscriptionsBackButton"),
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        if (!state.isLoading && state.dueSoon.isEmpty() && state.upcoming.isEmpty()) {
            SubscriptionsEmptyState(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .testTag("SubscriptionsScreen"),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("SubscriptionsScreen"),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item("hero") {
                SubscriptionsHeroCard(
                    monthlyTotal = state.monthlyTotal,
                    activeCount = state.activeCount,
                    daysUntilNextCharge = state.daysUntilNextCharge,
                    currencyFormatted = currencyFormat.format(state.monthlyTotal),
                    periodBudgetTotal = state.periodBudgetTotal,
                    periodCommittedTotal = state.periodCommittedTotal,
                    budgetFormatted = currencyFormat.format(state.periodBudgetTotal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }

            item("view-mode-toggle") {
                SubscriptionsViewModeToggle(
                    selected = viewMode,
                    onSelected = { viewMode = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            item("calendar") {
                when (viewMode) {
                    SubscriptionsViewMode.WHOLE_PERIOD -> SubscriptionsCalendarSection(
                        periodStart = state.periodStart,
                        periodEnd = state.periodEnd,
                        chargesByDay = state.chargesByDay,
                        onDayClick = { selectedDay = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    SubscriptionsViewMode.WEEKLY -> SubscriptionsWeeklyCalendar(
                        periodStart = state.periodStart,
                        periodEnd = state.periodEnd,
                        chargesByDay = state.chargesByDay,
                        onDayClick = { selectedDay = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    SubscriptionsViewMode.CATEGORY -> SubscriptionsCategoryGraph(
                        chargesByDay = state.chargesByDay,
                        currencyFormat = currencyFormat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(220.dp),
                    )
                }
            }

            item("frequency-breakdown") {
                SubscriptionsFrequencyBreakdown(
                    monthlyTotalByFrequency = state.monthlyTotalByFrequency,
                    currencyFormat = currencyFormat,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (state.dueSoon.isNotEmpty()) {
                item("due-soon-header") {
                    SectionHeader(stringResource(R.string.subscriptions_due_soon_header))
                }
                itemsIndexed(
                    items = state.dueSoon,
                    key = { _, item -> "due-${item.transaction.id}" },
                ) { _, item ->
                    val isDueToday = item.nextChargeDate == LocalDate.now()
                    if (isDueToday) {
                        DueTodayCard(
                            item = item,
                            currencyFormat = currencyFormat,
                            onConfirmPaid = { actions.onConfirmPaid(item.transaction, item.nextChargeDate) },
                            onSkip = { actions.onSkip(item.transaction, item.nextChargeDate) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                        ) {
                            SwipeableUpcomingRecurrentItem(
                                item = item,
                                currencyFormat = currencyFormat,
                                position = PaddedListItemPosition.Single,
                                isExpanded = expandedTransactionId == item.transaction.id,
                                onDelete = { actions.onDeleteRequested(item.transaction) },
                                onEdit = { actions.onEditRequested(item.transaction) },
                                onMarkAsPaid = { actions.onConfirmPaid(item.transaction, item.nextChargeDate) },
                                onSkip = { actions.onSkip(item.transaction, item.nextChargeDate) },
                                onClick = { actions.onToggleExpanded(item.transaction.id) },
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                            )
                        }
                    }
                }
            }

            if (state.upcoming.isNotEmpty()) {
                item("upcoming-header") {
                    SectionHeader(stringResource(R.string.subscriptions_upcoming_header))
                }
                itemsIndexed(
                    items = state.upcoming,
                    key = { _, item -> "upcoming-${item.transaction.id}" },
                ) { _, item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        SwipeableUpcomingRecurrentItem(
                            item = item,
                            currencyFormat = currencyFormat,
                            position = PaddedListItemPosition.Single,
                            isOutOfPeriod = true,
                            isExpanded = expandedTransactionId == item.transaction.id,
                            onDelete = { actions.onDeleteRequested(item.transaction) },
                            onEdit = { actions.onEditRequested(item.transaction) },
                            onMarkAsPaid = { actions.onConfirmPaid(item.transaction, item.nextChargeDate) },
                            onSkip = { actions.onSkip(item.transaction, item.nextChargeDate) },
                            onClick = { actions.onToggleExpanded(item.transaction.id) },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                    }
                }
                item("upcoming-total") {
                    DayTotalItem(
                        total = state.upcoming.sumOf { it.transaction.amount },
                        currencyFormat = currencyFormat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }

    val dayForSheet = selectedDay
    if (dayForSheet != null) {
        SubscriptionDayDetailsSheet(
            date = dayForSheet,
            charges = state.chargesByDay[dayForSheet].orEmpty(),
            currencyFormat = currencyFormat,
            onConfirmPaid = { transaction -> actions.onConfirmPaid(transaction, dayForSheet) },
            onSkip = { transaction -> actions.onSkip(transaction, dayForSheet) },
            onDismiss = { selectedDay = null },
        )
    }

    TransactionEditDialog(
        transaction = editingTransaction,
        budgetStartDate = state.periodStart,
        budgetEndDate = state.periodEnd,
        currencyCode = state.currencyCode,
        tags = tags,
        onCancel = actions.onEditCancelled,
        onSave = actions.onSaveEdited,
    )

    DeleteRecurrentExpenseDialog(
        transaction = recurrentToDelete,
        onDismiss = actions.onDeleteCancelled,
        onConfirm = actions.onConfirmDelete,
    )
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmallEmphasized,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Preview
@Composable
private fun SubscriptionsPreview() {
    val today = LocalDate.now()
    val dueTodayItem = UpcomingRecurrentItem(
        transaction = Transaction(
            id = 1L,
            amount = BigDecimal("16.99"),
            comment = "Netflix",
            date = today.minusMonths(2).atStartOfDay(),
            isDeleted = false,
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
        ),
        nextChargeDate = today,
        isInCurrentPeriod = true,
    )
    val soonItem = dueTodayItem.copy(
        transaction = dueTodayItem.transaction.copy(id = 2L, comment = "Spotify", amount = BigDecimal("9.99"), recurrentFrequency = RecurrentFrequency.WEEKLY),
        nextChargeDate = today.plusDays(2),
    )
    val upcomingItem = dueTodayItem.copy(
        transaction = dueTodayItem.transaction.copy(id = 3L, comment = "Gym membership", amount = BigDecimal("49.99")),
        nextChargeDate = today.plusDays(12),
    )
    val monthStart = today.withDayOfMonth(1)
    val monthEnd = today.plusDays(10)
    MinusTheme {
        Subscriptions(
            state = SubscriptionsUiState(
                isLoading = false,
                dueSoon = listOf(dueTodayItem, soonItem),
                upcoming = listOf(upcomingItem),
                monthlyTotal = BigDecimal("76.97"),
                activeCount = 3,
                currencyCode = "USD",
                daysUntilNextCharge = 0L,
                periodStart = monthStart,
                periodEnd = monthEnd,
                chargesByDay = mapOf(
                    today to listOf(DayCharge(dueTodayItem.transaction, ChargeStatus.PENDING)),
                    today.minusDays(3) to listOf(DayCharge(soonItem.transaction, ChargeStatus.PAID)),
                    today.plusDays(2) to listOf(DayCharge(soonItem.transaction, ChargeStatus.PENDING)),
                    today.plusDays(12) to listOf(
                        DayCharge(upcomingItem.transaction, ChargeStatus.PAID),
                        DayCharge(soonItem.transaction, ChargeStatus.SKIPPED),
                    ),
                ),
                periodBudgetTotal = BigDecimal("200.00"),
                periodCommittedTotal = BigDecimal("76.97"),
                monthlyTotalByFrequency = mapOf(
                    RecurrentFrequency.MONTHLY to BigDecimal("66.98"),
                    RecurrentFrequency.WEEKLY to BigDecimal("43.30"),
                ),
            ),
        )
    }
}
