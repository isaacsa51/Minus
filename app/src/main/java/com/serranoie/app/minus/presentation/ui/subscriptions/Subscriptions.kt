package com.serranoie.app.minus.presentation.ui.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.history.sections.paddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItemRow
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.time.LocalDate

data class SubscriptionsActions(
    val onBack: () -> Unit = {},
    val onConfirmPaid: (Transaction, LocalDate) -> Unit = { _, _ -> },
    val onSkip: (Transaction, LocalDate) -> Unit = { _, _ -> },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Subscriptions(
    state: SubscriptionsUiState = SubscriptionsUiState(),
    actions: SubscriptionsActions = SubscriptionsActions(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier,
) {
    val currencyFormat = remember(state.currencyCode) { symbolOnlyCurrencyFormat(state.currencyCode) }
    var expandedTransactionId by rememberSaveable { mutableStateOf<Long?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

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
                    currencyFormatted = currencyFormat.format(state.monthlyTotal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }

            if (state.dueSoon.isNotEmpty()) {
                item("due-soon-header") {
                    Text(
                        text = stringResource(R.string.subscriptions_due_soon_header),
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                itemsIndexed(
                    items = state.dueSoon,
                    key = { _, item -> "due-${item.transaction.id}" },
                ) { index, item ->
                    UpcomingRecurrentItemRow(
                        item = item,
                        currencyFormat = currencyFormat,
                        position = paddedListItemPosition(index, state.dueSoon.lastIndex, state.dueSoon.size),
                        isExpanded = expandedTransactionId == item.transaction.id,
                        onClick = {
                            expandedTransactionId =
                                if (expandedTransactionId == item.transaction.id) null else item.transaction.id
                        },
                        onMarkAsPaid = { actions.onConfirmPaid(item.transaction, item.nextChargeDate) },
                        onSkip = { actions.onSkip(item.transaction, item.nextChargeDate) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    if (index < state.dueSoon.lastIndex) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }

            if (state.upcoming.isNotEmpty()) {
                item("upcoming-header") {
                    Text(
                        text = stringResource(R.string.subscriptions_upcoming_header),
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                itemsIndexed(
                    items = state.upcoming,
                    key = { _, item -> "upcoming-${item.transaction.id}" },
                ) { index, item ->
                    UpcomingRecurrentItemRow(
                        item = item,
                        currencyFormat = currencyFormat,
                        position = paddedListItemPosition(index, state.upcoming.lastIndex, state.upcoming.size),
                        isOutOfPeriod = true,
                        isExpanded = expandedTransactionId == item.transaction.id,
                        onClick = {
                            expandedTransactionId =
                                if (expandedTransactionId == item.transaction.id) null else item.transaction.id
                        },
                        onMarkAsPaid = { actions.onConfirmPaid(item.transaction, item.nextChargeDate) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    if (index < state.upcoming.lastIndex) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionsHeroCard(
    monthlyTotal: BigDecimal,
    activeCount: Int,
    currencyFormatted: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.subscriptions_monthly_commitment_label),
                style = MaterialTheme.typography.labelMediumCondensed,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )
            Text(
                text = currencyFormatted,
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.subscriptions_active_count, activeCount),
                style = MaterialTheme.typography.bodyMediumCondensed,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun SubscriptionsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Rounded.EventRepeat,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.height(48.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.subscriptions_empty_title),
            style = MaterialTheme.typography.titleMediumCondensed,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.subscriptions_empty_body),
            style = MaterialTheme.typography.bodyMediumCondensed,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun SubscriptionsPreview() {
    val today = LocalDate.now()
    val sampleItem = UpcomingRecurrentItem(
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
    MinusTheme {
        Subscriptions(
            state = SubscriptionsUiState(
                isLoading = false,
                dueSoon = listOf(sampleItem),
                upcoming = listOf(sampleItem.copy(transaction = sampleItem.transaction.copy(id = 2L, comment = "Gym"), nextChargeDate = today.plusDays(12))),
                monthlyTotal = BigDecimal("64.98"),
                activeCount = 2,
                currencyCode = "USD",
            ),
        )
    }
}
