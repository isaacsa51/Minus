package com.serranoie.app.minus.presentation.ui.subscriptions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.R
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.calculator.RecurringExpenseCalculator
import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.usecase.GetCurrentPeriodIdUseCase
import com.serranoie.app.minus.presentation.ui.budget.BudgetTransactionHandler
import com.serranoie.app.minus.presentation.ui.history.buildUpcomingRecurrentItems
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import com.serranoie.app.minus.presentation.util.ErrorLogRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

private const val DUE_SOON_WINDOW_DAYS = 7L

data class SubscriptionsUiState(
    val isLoading: Boolean = true,
    val dueSoon: List<UpcomingRecurrentItem> = emptyList(),
    val upcoming: List<UpcomingRecurrentItem> = emptyList(),
    val monthlyTotal: BigDecimal = BigDecimal.ZERO,
    val activeCount: Int = 0,
    val currencyCode: String = "USD",
)

sealed interface SubscriptionsUiEffect {
    data class ShowSnackbar(val message: String) : SubscriptionsUiEffect
}

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val budgetTransactionHandler: BudgetTransactionHandler,
    private val getCurrentPeriodIdUseCase: GetCurrentPeriodIdUseCase,
    private val recurringExpenseCalculator: RecurringExpenseCalculator,
    private val errorLogRecorder: ErrorLogRecorder,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val uiState: StateFlow<SubscriptionsUiState> = combine(
        budgetRepository.getTransactions().distinctUntilChanged(),
        budgetRepository.getPaidRecurrentOccurrences().distinctUntilChanged(),
        budgetRepository.getBudgetSettings().distinctUntilChanged(),
    ) { transactions, paidOccurrences, settings ->
        try {
            buildUiState(transactions, paidOccurrences, settings?.currencyCode ?: "USD")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // This combine() drives the whole screen — fall back to a safe, empty state
            // instead of crashing the StateFlow's upstream collection.
            errorLogRecorder.record("SubscriptionsViewModel.uiState combine", e)
            SubscriptionsUiState(isLoading = false)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = SubscriptionsUiState()
    )

    private val _effects = MutableStateFlow<SubscriptionsUiEffect?>(null)
    val effects: StateFlow<SubscriptionsUiEffect?> = _effects.asStateFlow()

    private fun buildUiState(
        transactions: List<Transaction>,
        paidOccurrences: Set<PaidRecurrentOccurrence>,
        currencyCode: String,
    ): SubscriptionsUiState {
        val today = LocalDate.now()
        val activeRecurrent = transactions.filter { transaction ->
            transaction.isRecurrent && !transaction.isDeleted &&
                (transaction.recurrentEndDate == null || !transaction.recurrentEndDate.toLocalDate().isBefore(today))
        }

        val (dueSoon, upcoming) = buildUpcomingRecurrentItems(
            transactions = activeRecurrent,
            budgetStartDate = today,
            budgetEndDate = today.plusDays(DUE_SOON_WINDOW_DAYS),
            today = today,
            paidOccurrences = paidOccurrences,
        )

        return SubscriptionsUiState(
            isLoading = false,
            dueSoon = dueSoon,
            upcoming = upcoming,
            monthlyTotal = recurringExpenseCalculator.calculateMonthlyEquivalent(activeRecurrent),
            activeCount = activeRecurrent.size,
            currencyCode = currencyCode,
        )
    }

    fun onConfirmPaid(transaction: Transaction, occurrenceDate: LocalDate) {
        viewModelScope.launch {
            val activePeriodId = getCurrentPeriodIdUseCase()
            val occurrenceTransaction = transaction.copy(
                date = occurrenceDate.atTime(transaction.date?.toLocalTime() ?: LocalTime.MIDNIGHT),
                sourceTransactionId = transaction.sourceTransactionId ?: transaction.id,
            )
            budgetTransactionHandler.markRecurrentOccurrencePaid(occurrenceTransaction, activePeriodId)
                .onFailure {
                    _effects.value = SubscriptionsUiEffect.ShowSnackbar(
                        context.getString(R.string.subscriptions_snackbar_confirm_failed)
                    )
                }
        }
    }

    fun onSkip(transaction: Transaction, occurrenceDate: LocalDate) {
        viewModelScope.launch {
            try {
                val realId = transaction.sourceTransactionId ?: transaction.id
                budgetRepository.markRecurrentOccurrencePaid(realId, occurrenceDate)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorLogRecorder.record("SubscriptionsViewModel.onSkip id=${transaction.id}", e)
                _effects.value = SubscriptionsUiEffect.ShowSnackbar(
                    context.getString(R.string.subscriptions_snackbar_skip_failed)
                )
            }
        }
    }

    fun consumeEffect() {
        _effects.value = null
    }
}
