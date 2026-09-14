package com.serranoie.app.minus.presentation.ui.subscriptions

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit,
    viewModel: SubscriptionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val effect by viewModel.effects.collectAsStateWithLifecycle()

    BackHandler(onBack = onBack)

    SubscriptionsScreenContent(
        uiState = uiState,
        effect = effect,
        onBack = onBack,
        onConfirmPaid = viewModel::onConfirmPaid,
        onSkip = viewModel::onSkip,
        onConsumeEffect = viewModel::consumeEffect,
    )
}

@Composable
private fun SubscriptionsScreenContent(
    uiState: SubscriptionsUiState,
    effect: SubscriptionsUiEffect?,
    onBack: () -> Unit,
    onConfirmPaid: (Transaction, LocalDate) -> Unit,
    onSkip: (Transaction, LocalDate) -> Unit,
    onConsumeEffect: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(effect) {
        when (effect) {
            is SubscriptionsUiEffect.ShowSnackbar -> {
                onConsumeEffect()
                coroutineScope.launch { snackbarHostState.showSnackbar(effect.message) }
            }

            null -> { /* no-op */ }
        }
    }

    Subscriptions(
        state = uiState,
        actions = SubscriptionsActions(
            onBack = onBack,
            onConfirmPaid = onConfirmPaid,
            onSkip = onSkip,
        ),
        snackbarHostState = snackbarHostState,
    )
}

@Preview(showBackground = true)
@Composable
private fun SubscriptionsScreenPreview() {
    val today = LocalDate.now()
    val sampleItem = UpcomingRecurrentItem(
        transaction = Transaction(
            id = 1L,
            amount = BigDecimal("12.99"),
            comment = "Spotify",
            date = today.minusMonths(1).atStartOfDay(),
            isDeleted = false,
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
        ),
        nextChargeDate = today,
        isInCurrentPeriod = true,
    )
    MinusTheme {
        SubscriptionsScreenContent(
            uiState = SubscriptionsUiState(
                isLoading = false,
                dueSoon = listOf(sampleItem),
                upcoming = listOf(
                    sampleItem.copy(
                        transaction = sampleItem.transaction.copy(id = 2L, comment = "iCloud"),
                        nextChargeDate = today.plusDays(15),
                    )
                ),
                monthlyTotal = BigDecimal("13.98"),
                activeCount = 2,
                currencyCode = "USD",
            ),
            effect = null,
            onBack = {},
            onConfirmPaid = { _, _ -> },
            onSkip = { _, _ -> },
        ) { }
    }
}
