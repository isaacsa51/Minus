package com.serranoie.app.minus.data.repository

import android.util.Log
import com.serranoie.app.minus.data.local.dao.BudgetSettingsDao
import com.serranoie.app.minus.data.local.dao.TransactionDao
import com.serranoie.app.minus.data.local.entity.BudgetSettingsEntity
import com.serranoie.app.minus.data.local.entity.TransactionEntity
import com.serranoie.app.minus.domain.calculator.BudgetCalculator
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BudgetRepositoryImpl - ISAAC"

/**
 * Implementation of BudgetRepository using Room database.
 * Handles mapping between domain models and entities.
 */
@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val settingsDao: BudgetSettingsDao,
    private val budgetCalculator: BudgetCalculator
) : BudgetRepository {

    //region Mappers
    private fun TransactionEntity.toDomain(): Transaction = Transaction(
        id = this.id,
        amount = BigDecimal(this.amount),
        comment = this.comment,
        date = LocalDateTime.ofEpochSecond(this.date / 1000, 0, ZoneOffset.UTC),
        createdAt = this.createdAt,
        periodId = this.periodId,
        isDeleted = false,
        isRecurrent = this.isRecurrent,
        recurrentFrequency = this.recurrentFrequency?.let { 
            try { 
                com.serranoie.app.minus.domain.model.RecurrentFrequency.valueOf(it) 
            } catch (_: Exception) { 
                null 
            } 
        },
        recurrentEndDate = this.recurrentEndDate?.let { 
            LocalDateTime.ofEpochSecond(it / 1000, 0, ZoneOffset.UTC) 
        },
        subscriptionDay = this.subscriptionDay
    )

    private fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
        id = if (this.id == 0L) 0 else this.id,
        amount = this.amount.toPlainString(),
        comment = this.comment,
        date = this.date!!.toEpochSecond(ZoneOffset.UTC) * 1000,
        createdAt = this.createdAt,
        periodId = this.periodId,
        isRecurrent = this.isRecurrent,
        recurrentFrequency = this.recurrentFrequency?.name,
        recurrentEndDate = this.recurrentEndDate?.toEpochSecond(ZoneOffset.UTC)?.times(1000),
        subscriptionDay = this.subscriptionDay
    )

    private fun BudgetSettingsEntity.toDomain(): BudgetSettings {
        val domain = BudgetSettings(
            totalBudget = BigDecimal(this.totalBudget),
            period = BudgetPeriod.valueOf(this.period),
            startDate = LocalDate.ofEpochDay(this.startDate / 86400000),
            endDate = this.endDate?.let { LocalDate.ofEpochDay(it / 86400000) },
            currencyCode = this.currencyCode,
            daysInPeriod = this.daysInPeriod,
            rollOverEnabled = this.rollOverEnabled,
            rollOverCarryForward = this.rollOverCarryForward,
            remainingBudgetStrategy = try {
                RemainingBudgetStrategy.valueOf(this.remainingBudgetStrategy)
            } catch (_: Exception) {
                RemainingBudgetStrategy.ASK_ALWAYS
            }
        )
        Log.d(TAG, "toDomain: entity=$this -> domain=$domain")
        return domain
    }

    private fun BudgetSettings.toEntity(): BudgetSettingsEntity {
        val entity = BudgetSettingsEntity(
            id = 1,
            totalBudget = this.totalBudget.toPlainString(),
            period = this.period.name,
            startDate = this.startDate.toEpochDay() * 86400000,
            endDate = this.endDate?.toEpochDay()?.times(86400000),
            currencyCode = this.currencyCode,
            daysInPeriod = this.daysInPeriod,
            rollOverEnabled = this.rollOverEnabled,
            rollOverCarryForward = this.rollOverCarryForward,
            remainingBudgetStrategy = this.remainingBudgetStrategy.name
        )
        Log.d(TAG, "toEntity: domain=$this -> entity=$entity")
        return entity
    }
    //endregion

    //region Transactions
    override fun getTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions()
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getTransactionsForPeriod(start: LocalDate, end: LocalDate): Flow<List<Transaction>> {
        val startMillis = start.toEpochDay() * 86400000
        val endMillis = end.toEpochDay() * 86400000
        return transactionDao.getTransactionsForDateRange(startMillis, endMillis)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun addTransaction(transaction: Transaction) {
        transactionDao.insert(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.update(transaction.toEntity())
    }

    override suspend fun upsertTransactions(transactions: List<Transaction>) {
        val entities = transactions.map { it.toEntity() }
        transactionDao.insertAllOrReplace(entities)
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.delete(transaction.toEntity())
    }

    override fun getSpentForDate(date: LocalDate): Flow<BigDecimal> {
        val startOfDay = date.toEpochDay() * 86400000
        val endOfDay = (date.plusDays(1)).toEpochDay() * 86400000
        return transactionDao.getTotalSpentForDay(startOfDay, endOfDay)
            .map { it?.let { BigDecimal(it) } ?: BigDecimal.ZERO }
    }

    override fun getSpentForPeriod(start: LocalDate, end: LocalDate): Flow<BigDecimal> {
        val startMillis = start.toEpochDay() * 86400000
        val endMillis = end.toEpochDay() * 86400000
        return transactionDao.getTotalSpentForPeriod(startMillis, endMillis)
            .map { it?.let { BigDecimal(it) } ?: BigDecimal.ZERO }
    }
    //endregion

    //region Budget Settings
    override fun getBudgetSettings(): Flow<BudgetSettings?> {
        return settingsDao.getSettings()
            .map { it?.toDomain() }
    }

    override suspend fun saveBudgetSettings(settings: BudgetSettings) {
        Log.d(TAG, "saveBudgetSettings: settings=$settings")
        val entity = settings.toEntity()
        Log.d(TAG, "saveBudgetSettings: inserting entity with endDate=${entity.endDate}")
        settingsDao.insert(entity)
        Log.d(TAG, "saveBudgetSettings: insert complete")
    }

    override suspend fun getBudgetSettingsSync(): BudgetSettings? {
        return settingsDao.getSettingsSync()?.toDomain()
    }
    //endregion

    //region Budget Calculations
    override fun calculateBudgetState(
        settings: BudgetSettings,
        currentDate: LocalDate
    ): Flow<BudgetState> {
        // Get period end date
        val periodEnd = when (settings.period) {
            BudgetPeriod.DAILY -> settings.startDate
            BudgetPeriod.WEEKLY -> settings.startDate.plusWeeks(1)
            BudgetPeriod.BIWEEKLY -> settings.startDate.plusWeeks(2)
            BudgetPeriod.MONTHLY -> settings.startDate.plusMonths(1)
        }

        // Combine transactions for the period with real-time updates
        return getTransactionsForPeriod(settings.startDate, periodEnd)
            .map { transactions ->
                budgetCalculator.calculate(settings, transactions, currentDate)
            }
    }
    //endregion
}
