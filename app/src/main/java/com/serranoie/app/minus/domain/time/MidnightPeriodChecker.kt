package com.serranoie.app.minus.domain.time

import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.budget.BudgetStateCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import logcat.logcat
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

data class MidnightTransitionData(
    val periodStartDate: LocalDate,
    val periodEndDate: LocalDate,
    val totalBudget: BigDecimal,
    val remainingAmount: BigDecimal,
    val totalSpent: BigDecimal,
    val currencyCode: String,
    val shouldNavigateToAnalyticsOnly: Boolean = false,
)

data class DailySurplusData(
    val date: LocalDate,
    val surplusAmount: BigDecimal,
    val currencyCode: String,
)

@Singleton
class MidnightPeriodChecker @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
    private val budgetStateCalculator: BudgetStateCalculator,
) {
    data class EndingPeriodState(
        val shouldHandleEndingPeriod: Boolean,
        val transitionOccurred: Boolean,
        val periodEndDate: LocalDate?,
        val remainingAmount: BigDecimal,
    )

    private val _midnightTransitionData = MutableStateFlow<MidnightTransitionData?>(null)
    val midnightTransitionData: StateFlow<MidnightTransitionData?> =
        _midnightTransitionData.asStateFlow()

    private val _shouldShowTransitionDialog = MutableStateFlow(false)
    val shouldShowTransitionDialog: StateFlow<Boolean> = _shouldShowTransitionDialog.asStateFlow()

    private val _needsBudgetSetup = MutableStateFlow(false)
    val needsBudgetSetup: StateFlow<Boolean> = _needsBudgetSetup.asStateFlow()

    private val _dailySurplusData = MutableStateFlow<DailySurplusData?>(null)
    val dailySurplusData: StateFlow<DailySurplusData?> = _dailySurplusData.asStateFlow()

    private val _shouldShowDailySurplusDialog = MutableStateFlow(false)
    val shouldShowDailySurplusDialog: StateFlow<Boolean> = _shouldShowDailySurplusDialog.asStateFlow()

    suspend fun handleEndingPeriod() {
        val endingPeriodState = resolveEndingPeriodState()
        if (!endingPeriodState.shouldHandleEndingPeriod) {
            logcat { "No ending period to handle" }
            // Check if no budget is set - if so, signal UI to show budget setup
            val budgetSettings = budgetRepository.getBudgetSettingsSync()
            if (budgetSettings == null) {
                logcat { "No budget settings found, triggering budget setup prompt" }
                _needsBudgetSetup.value = true
            }
            return
        }

        settingsRepository.setMidnightTransitionOccurred(false)

        val lastPeriodEndDate = endingPeriodState.periodEndDate ?: run {
            logcat { "No period end date found" }
            return
        }
        persistLastPeriodSnapshot(
            periodEndDate = lastPeriodEndDate,
            remainingAmount = endingPeriodState.remainingAmount,
        )

        val settings = budgetRepository.getBudgetSettingsSync() ?: run {
            logcat { "No budget settings found" }
            return
        }

        settingsRepository.setPeriodEndAlreadyHandled(true)

        if (endingPeriodState.remainingAmount <= BigDecimal.ZERO) {
            val daysInPeriod =
                ChronoUnit.DAYS.between(settings.startDate, settings.getPeriodEndDate()) + 1
            val periodStartDate = lastPeriodEndDate.minusDays(daysInPeriod - 1)
            _midnightTransitionData.value = MidnightTransitionData(
                periodStartDate = periodStartDate,
                periodEndDate = lastPeriodEndDate,
                totalBudget = settings.totalBudget,
                remainingAmount = endingPeriodState.remainingAmount,
                totalSpent = settings.totalBudget.subtract(endingPeriodState.remainingAmount),
                currencyCode = settings.currencyCode,
                shouldNavigateToAnalyticsOnly = true,
            )
            _shouldShowTransitionDialog.value = true
            logcat { "Ending period detected without remaining budget, routing user to analytics" }
            return
        }

        val daysInPeriod =
            ChronoUnit.DAYS.between(settings.startDate, settings.getPeriodEndDate()) + 1
        val periodStartDate = lastPeriodEndDate.minusDays(daysInPeriod - 1)

        val totalSpent = settings.totalBudget.subtract(endingPeriodState.remainingAmount)

        when (settings.remainingBudgetStrategy) {
            RemainingBudgetStrategy.ASK_ALWAYS -> {
                _midnightTransitionData.value = MidnightTransitionData(
                    periodStartDate = periodStartDate,
                    periodEndDate = lastPeriodEndDate,
                    totalBudget = settings.totalBudget,
                    remainingAmount = endingPeriodState.remainingAmount,
                    totalSpent = totalSpent,
                    currencyCode = settings.currencyCode
                )
                _shouldShowTransitionDialog.value = true
                logcat { "Ending period detected, asking user for rollover strategy" }
            }

            RemainingBudgetStrategy.SPLIT_EQUALLY,
            RemainingBudgetStrategy.ADD_TO_FIRST_DAY -> {
                enqueuePendingRollover(
                    strategy = settings.remainingBudgetStrategy,
                    remainingAmount = endingPeriodState.remainingAmount,
                )
                _midnightTransitionData.value = MidnightTransitionData(
                    periodStartDate = periodStartDate,
                    periodEndDate = lastPeriodEndDate,
                    totalBudget = settings.totalBudget,
                    remainingAmount = endingPeriodState.remainingAmount,
                    totalSpent = totalSpent,
                    currencyCode = settings.currencyCode,
                    shouldNavigateToAnalyticsOnly = true,
                )
                _shouldShowTransitionDialog.value = true
                logcat { "Ending period detected, queued pending rollover and routing to analytics" }
            }
        }
    }

    suspend fun resolveEndingPeriodState(): EndingPeriodState {
        val transitionOccurred = settingsRepository.observeMidnightTransitionOccurred().first()
        val today = LocalDate.now()

        val userSettings = settingsRepository.getSettings()

        if (userSettings.earlyFinishActive || userSettings.periodEndAlreadyHandled) {
            if (transitionOccurred) {
                settingsRepository.setMidnightTransitionOccurred(false)
            }
            return EndingPeriodState(
                shouldHandleEndingPeriod = false,
                transitionOccurred = false,
                periodEndDate = null,
                remainingAmount = BigDecimal.ZERO,
            )
        }

        val settings = budgetRepository.getBudgetSettingsSync()
        val settingsEndDate = settings?.getPeriodEndDate()

        val endDateMillis = settingsRepository.observeBudgetEndDate().first()
        val dataStoreEndDate = endDateMillis?.let { millis ->
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        }
        val dataStoreIsInFuture = dataStoreEndDate?.isAfter(today) ?: false
        val effectiveEndDate =
            if (dataStoreIsInFuture) {
                settingsEndDate ?: dataStoreEndDate
            } else {
                dataStoreEndDate
                    ?: settingsEndDate
            }
        val periodEndedBasedOnDate = effectiveEndDate?.let { today.isAfter(it) } ?: false

        val periodEndDate = if (transitionOccurred) {
            settingsRepository.getLastPeriodEnd()?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            } ?: effectiveEndDate
        } else {
            effectiveEndDate
        }

        val remaining = if (transitionOccurred) {
            settingsRepository.getRemainingFromLastPeriod()
        } else {
            computeRemainingFromCurrentPeriod()
        }

        return EndingPeriodState(
            shouldHandleEndingPeriod = transitionOccurred || periodEndedBasedOnDate,
            transitionOccurred = transitionOccurred,
            periodEndDate = periodEndDate,
            remainingAmount = remaining
        )
    }

    private suspend fun computeRemainingFromCurrentPeriod(): BigDecimal {
        val settings = budgetRepository.getBudgetSettingsSync() ?: return BigDecimal.ZERO
        val periodEnd = settings.getPeriodEndDate()
        val transactions = budgetRepository.getTransactions().first()
        val periodTransactions = transactions.filter { transaction ->
            val txDate = transaction.date?.toLocalDate()
            txDate != null && !txDate.isBefore(settings.startDate) && !txDate.isAfter(periodEnd)
        }
        val totalSpent = periodTransactions
            .filter { !it.isDeleted }
            .sumOf { it.amount }
        return settings.totalBudget.subtract(totalSpent)
    }

    fun onTransitionDialogConfirmed() {
        _shouldShowTransitionDialog.value = false
        _midnightTransitionData.value = null
    }

    fun onTransitionDialogDismissed() {
        _shouldShowTransitionDialog.value = false
        _midnightTransitionData.value = null
    }

    fun onBudgetSetupHandled() {
        _needsBudgetSetup.value = false
    }

    suspend fun handleEarlyFinish(settings: BudgetSettings, remainingAmount: BigDecimal) {
        if (remainingAmount <= BigDecimal.ZERO) return

        when (settings.remainingBudgetStrategy) {
            RemainingBudgetStrategy.ASK_ALWAYS -> {
                _midnightTransitionData.value = MidnightTransitionData(
                    periodStartDate = settings.startDate,
                    periodEndDate = LocalDate.now(),
                    totalBudget = settings.totalBudget,
                    remainingAmount = remainingAmount,
                    totalSpent = settings.totalBudget.subtract(remainingAmount),
                    currencyCode = settings.currencyCode,
                )
                _shouldShowTransitionDialog.value = true
                logcat { "Early finish detected, asking user for rollover strategy" }
            }

            RemainingBudgetStrategy.SPLIT_EQUALLY,
            RemainingBudgetStrategy.ADD_TO_FIRST_DAY -> {
                enqueuePendingRollover(
                    strategy = settings.remainingBudgetStrategy,
                    remainingAmount = remainingAmount,
                )
                logcat { "Early finish detected, queued pending rollover amount=$remainingAmount strategy=${settings.remainingBudgetStrategy}" }
            }
        }
    }

    suspend fun rollRemainingSplitEqually() {
        val data = _midnightTransitionData.value ?: return
        enqueuePendingRollover(
            strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
            remainingAmount = data.remainingAmount,
        )
        onTransitionDialogConfirmed()
    }

    suspend fun rollRemainingToFirstDay() {
        val data = _midnightTransitionData.value ?: return
        enqueuePendingRollover(
            strategy = RemainingBudgetStrategy.ADD_TO_FIRST_DAY,
            remainingAmount = data.remainingAmount,
        )
        onTransitionDialogConfirmed()
    }

    private suspend fun persistLastPeriodSnapshot(
        periodEndDate: LocalDate,
        remainingAmount: BigDecimal,
    ) {
        val millis = periodEndDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        settingsRepository.persistLastPeriodSnapshot(millis, remainingAmount)
    }

    private suspend fun enqueuePendingRollover(
        strategy: RemainingBudgetStrategy,
        remainingAmount: BigDecimal,
    ) {
        settingsRepository.setPendingRollover(remainingAmount, strategy)
    }

    /**
     * Within an ongoing period (not period-end, see [handleEndingPeriod]), Dynamic split mode
     * silently folds yesterday's underspend into today's recomputed average with no user
     * involvement at all - regardless of [RemainingBudgetStrategy]. This surfaces that surplus
     * once per day transition when the user asked to always be consulted.
     */
    suspend fun checkDailySurplus() {
        val settings = budgetRepository.getBudgetSettingsSync() ?: return
        if (settings.splitMode != BudgetSplitMode.DYNAMIC) return
        if (settings.remainingBudgetStrategy != RemainingBudgetStrategy.ASK_ALWAYS) return

        val today = LocalDate.now()
        val lastCheckedMillis = settingsRepository.getLastDailySurplusCheckDate()
        if (lastCheckedMillis == null) {
            settingsRepository.setLastDailySurplusCheckDate(today.toStartOfDayMillis())
            return
        }

        val lastChecked = Instant.ofEpochMilli(lastCheckedMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        if (!lastChecked.isBefore(today)) return

        val yesterday = today.minusDays(1)
        val yesterdayInPeriod = !yesterday.isBefore(settings.startDate) &&
            !yesterday.isAfter(settings.getPeriodEndDate())
        if (lastChecked != yesterday || !yesterdayInPeriod) {
            // App wasn't opened yesterday, or yesterday predates this period - nothing to
            // meaningfully compare against, just resync the checkpoint.
            settingsRepository.setLastDailySurplusCheckDate(today.toStartOfDayMillis())
            return
        }

        // Reconstruct the allotment as it stood at the START of yesterday (spend through the
        // day before only) - NOT including yesterday's own spend, which would bias it toward
        // whatever the user already spent that day.
        val allTransactions = budgetRepository.getTransactions().first()
        val throughDayBeforeYesterday = allTransactions.filter { tx ->
            val txDate = tx.date?.toLocalDate()
            !tx.isDeleted && txDate != null && txDate.isBefore(yesterday)
        }
        val yesterdayState = budgetStateCalculator.calculateBudgetState(
            settings = settings,
            transactions = throughDayBeforeYesterday,
            currentDate = yesterday,
        )
        val spentYesterday = allTransactions
            .filter {
                !it.isDeleted && it.date?.toLocalDate() == yesterday && !it.isRecurrent &&
                    it.amount > BigDecimal.ZERO && !it.isAdjustment
            }
            .sumOf { it.amount }
        val surplus = yesterdayState.dailyBudget.subtract(spentYesterday)

        settingsRepository.setLastDailySurplusCheckDate(today.toStartOfDayMillis())

        if (surplus > MIN_SURPLUS_THRESHOLD) {
            _dailySurplusData.value = DailySurplusData(
                date = yesterday,
                surplusAmount = surplus,
                currencyCode = settings.currencyCode,
            )
            _shouldShowDailySurplusDialog.value = true
            logcat { "Daily surplus of $surplus detected for $yesterday, asking user" }
        }
    }

    fun onDailySurplusDialogDismissed() {
        // Also covers "spread across remaining days": Dynamic's live recompute already does
        // this automatically, so there is nothing to persist for that choice.
        _shouldShowDailySurplusDialog.value = false
        _dailySurplusData.value = null
    }

    suspend fun onDailySurplusAddToToday() {
        val data = _dailySurplusData.value ?: return
        val settings = budgetRepository.getBudgetSettingsSync()
        if (settings == null) {
            onDailySurplusDialogDismissed()
            return
        }

        val periodId = settingsRepository.getCurrentPeriodId()
        budgetRepository.addTransaction(
            Transaction.create(
                amount = data.surplusAmount,
                date = data.date.atTime(23, 59),
                periodId = periodId,
                isAdjustment = true,
            ),
        )
        budgetRepository.saveBudgetSettings(
            settings.copy(
                dailyCarryForwardDate = LocalDate.now(),
                dailyCarryForwardAmount = data.surplusAmount,
            ),
        )
        logcat { "Daily surplus of ${data.surplusAmount} added to today" }
        onDailySurplusDialogDismissed()
    }

    private fun LocalDate.toStartOfDayMillis(): Long =
        atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private companion object {
        val MIN_SURPLUS_THRESHOLD: BigDecimal = BigDecimal("0.01")
    }
}
