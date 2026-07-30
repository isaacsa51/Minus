package com.serranoie.app.minus.data.repository

import androidx.room.withTransaction
import com.serranoie.app.minus.data.local.AppDatabase
import com.serranoie.app.minus.data.local.dao.ArchivedBudgetDao
import com.serranoie.app.minus.data.local.dao.BudgetSettingsDao
import com.serranoie.app.minus.data.local.dao.CategoryDao
import com.serranoie.app.minus.data.local.dao.QueuedTransactionDao
import com.serranoie.app.minus.data.local.dao.TransactionDao
import com.serranoie.app.minus.data.local.entity.ArchivedBudgetEntity
import com.serranoie.app.minus.data.local.entity.BudgetSettingsEntity
import com.serranoie.app.minus.data.local.entity.CategoryEntity
import com.serranoie.app.minus.data.local.entity.QueuedTransactionEntity
import com.serranoie.app.minus.data.local.entity.TransactionEntity
import com.serranoie.app.minus.domain.calculator.BudgetCalculator
import com.serranoie.app.minus.domain.model.ArchivedBudget
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import logcat.logcat
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val transactionDao: TransactionDao,
    private val settingsDao: BudgetSettingsDao,
    private val archivedBudgetDao: ArchivedBudgetDao,
    private val categoryDao: CategoryDao,
    private val queuedTransactionDao: QueuedTransactionDao,
    private val budgetCalculator: BudgetCalculator
) : BudgetRepository {
    private fun TransactionEntity.toDomain(): Transaction = Transaction(
        id = this.id,
        amount = BigDecimal(this.amount),
        comment = this.comment,
        date = LocalDateTime.ofEpochSecond(this.date / 1000, 0, ZoneOffset.UTC),
        createdAt = this.createdAt,
        clientGeneratedId = this.clientGeneratedId,
        periodId = this.periodId,
        isDeleted = false,
        isRecurrent = this.isRecurrent,
        recurrentFrequency = this.recurrentFrequency?.let {
            try {
                RecurrentFrequency.valueOf(it)
            } catch (_: Exception) {
                null
            }
        },
        recurrentEndDate = this.recurrentEndDate?.let {
            LocalDateTime.ofEpochSecond(it / 1000, 0, ZoneOffset.UTC)
        },
        subscriptionDay = this.subscriptionDay,
        categoryId = this.categoryId,
        isCredit = this.isCredit,
        isCreditPaid = this.isCreditPaid
    )

    private fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
        id = if (this.id == 0L) 0 else this.id,
        amount = this.amount.toPlainString(),
        comment = this.comment,
        date = this.date!!.toEpochSecond(ZoneOffset.UTC) * 1000,
        createdAt = this.createdAt,
        clientGeneratedId = this.clientGeneratedId,
        periodId = this.periodId,
        isRecurrent = this.isRecurrent,
        recurrentFrequency = this.recurrentFrequency?.name,
        recurrentEndDate = this.recurrentEndDate?.toEpochSecond(ZoneOffset.UTC)?.times(1000),
        subscriptionDay = this.subscriptionDay,
        categoryId = this.categoryId,
        isCredit = this.isCredit,
        isCreditPaid = this.isCreditPaid
    )

    private fun QueuedTransactionEntity.toDomain(): Transaction = Transaction(
        id = this.id,
        amount = BigDecimal(this.amount),
        comment = this.comment,
        date = LocalDateTime.ofEpochSecond(this.date / 1000, 0, ZoneOffset.UTC),
        createdAt = this.createdAt,
        clientGeneratedId = null,
        periodId = 0L,
        isDeleted = false,
        isRecurrent = false,
        recurrentFrequency = null,
        recurrentEndDate = null,
        subscriptionDay = null,
        categoryId = this.categoryId,
        isCredit = this.isCredit,
        isCreditPaid = this.isCreditPaid
    )

    private fun Transaction.toQueuedEntity(): QueuedTransactionEntity = QueuedTransactionEntity(
        id = if (this.id == 0L) 0 else this.id,
        amount = this.amount.toPlainString(),
        comment = this.comment,
        date = this.date!!.toEpochSecond(ZoneOffset.UTC) * 1000,
        createdAt = this.createdAt,
        categoryId = this.categoryId,
        isCredit = this.isCredit,
        isCreditPaid = this.isCreditPaid
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
            },
            creditCardCutoffDay = this.creditCardCutoffDay,
            splitMode = try {
                BudgetSplitMode.valueOf(this.splitMode)
            } catch (_: Exception) {
                BudgetSplitMode.STATIC
            }
        )
        logcat { "toDomain: entity=$this -> domain=$domain" }
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
            remainingBudgetStrategy = this.remainingBudgetStrategy.name,
            creditCardCutoffDay = this.creditCardCutoffDay,
            splitMode = this.splitMode.name
        )
        logcat { "toEntity: domain=$this -> entity=$entity" }
        return entity
    }

    private fun CategoryEntity.toDomain(): Category = Category(
        id = this.id,
        name = this.name,
        isHidden = this.isHidden,
        usageCount = this.usageCount,
        lastUsedAt = this.lastUsedAt,
        createdAt = this.createdAt
    )

    private fun Category.toEntity(): CategoryEntity = CategoryEntity(
        id = if (this.id == 0L) 0 else this.id,
        name = this.name,
        isHidden = this.isHidden,
        usageCount = this.usageCount,
        lastUsedAt = this.lastUsedAt,
        createdAt = this.createdAt
    )

    private fun ArchivedBudgetEntity.toDomain(): ArchivedBudget = ArchivedBudget(
        periodId = this.periodId,
        totalBudget = BigDecimal(this.totalBudget),
        spentAmount = BigDecimal(this.spentAmount),
        startDate = LocalDate.ofEpochDay(this.startDate / 86400000),
        endDate = LocalDate.ofEpochDay(this.endDate / 86400000),
        currencyCode = this.currencyCode,
        periodType = BudgetPeriod.valueOf(this.periodType),
        createdAt = this.createdAt
    )

    private fun ArchivedBudget.toEntity(): ArchivedBudgetEntity = ArchivedBudgetEntity(
        periodId = periodId,
        totalBudget = totalBudget.toPlainString(),
        spentAmount = spentAmount.toPlainString(),
        startDate = startDate.toEpochDay() * 86400000,
        endDate = endDate.toEpochDay() * 86400000,
        currencyCode = currencyCode,
        periodType = periodType.name,
        createdAt = createdAt
    )

    override fun getTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions()
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getQueuedTransactions(): Flow<List<Transaction>> {
        return queuedTransactionDao.getAllQueuedTransactions()
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getTransactionsForPeriod(
        start: LocalDate,
        end: LocalDate
    ): Flow<List<Transaction>> {
        val startMillis = start.toEpochDay() * 86400000
        val endMillis = end.toEpochDay() * 86400000
        return transactionDao.getTransactionsForDateRange(startMillis, endMillis)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun addTransaction(transaction: Transaction) {
        transactionDao.insert(transaction.toEntity())
    }

    override suspend fun addQueuedTransaction(transaction: Transaction) {
        queuedTransactionDao.insert(transaction.toQueuedEntity())
    }

    override suspend fun addTransactionIfAbsent(transaction: Transaction): Boolean {
        val rowId = transactionDao.insertIgnore(transaction.toEntity())
        return rowId != -1L
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.update(transaction.toEntity())
    }

    override suspend fun assignQueuedTransactionsToPeriod(periodId: Long) {
        appDatabase.withTransaction {
            val queuedTransactions = queuedTransactionDao.getAllQueuedTransactionsSync()
            if (queuedTransactions.isEmpty()) return@withTransaction

            val transactions = queuedTransactions.map { queued ->
                queued.toDomain().copy(
                    id = 0L,
                    periodId = periodId
                )
            }

            transactionDao.insertAllOrReplace(transactions.map { it.toEntity() })
            queuedTransactionDao.clearAll()
        }
    }

    override suspend fun upsertTransactions(transactions: List<Transaction>) {
        val entities = transactions.map { it.toEntity() }
        transactionDao.insertAllOrReplace(entities)
    }

    override suspend fun existsTransactionByClientGeneratedId(clientGeneratedId: String): Boolean {
        return transactionDao.existsByClientGeneratedId(clientGeneratedId)
    }

    override suspend fun getRecentTransactions(limit: Int): List<Transaction> {
        return transactionDao.getRecentTransactions(limit).map { it.toDomain() }
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

    override fun getBudgetSettings(): Flow<BudgetSettings?> {
        return settingsDao.getSettings().map { it?.toDomain() }
    }

    override suspend fun saveBudgetSettings(settings: BudgetSettings) {
        logcat { "saveBudgetSettings: settings=$settings" }
        val entity = settings.toEntity()
        logcat { "saveBudgetSettings: inserting entity with endDate=${entity.endDate}" }
        settingsDao.insert(entity)
        logcat { "saveBudgetSettings: insert complete" }
    }

    override suspend fun getBudgetSettingsSync(): BudgetSettings? {
        return settingsDao.getSettingsSync()?.toDomain()
    }

    override suspend fun getTransactionById(transactionId: Long): Transaction? {
        return transactionDao.getTransactionById(transactionId)?.toDomain()
    }

    override fun calculateBudgetState(
        settings: BudgetSettings,
        currentDate: LocalDate
    ): Flow<BudgetState> {
        val periodEnd = when (settings.period) {
            BudgetPeriod.DAILY -> settings.startDate
            BudgetPeriod.WEEKLY -> settings.startDate.plusWeeks(1)
            BudgetPeriod.BIWEEKLY -> settings.startDate.plusWeeks(2)
            BudgetPeriod.MONTHLY -> settings.startDate.plusMonths(1)
        }

        return getTransactionsForPeriod(settings.startDate, periodEnd).map { transactions ->
            budgetCalculator.calculate(settings, transactions, currentDate)
        }
    }

    override fun getActiveCategories(): Flow<List<Category>> {
        return categoryDao.getActiveCategories().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun findOrCreateCategory(name: String): Category {
        val existing = categoryDao.getCategoryByName(name)
        return if (existing != null) {
            // If hidden, unhide it
            if (existing.isHidden) {
                categoryDao.unhideCategory(name)
            }
            // Increment usage count
            categoryDao.incrementUsage(name)
            // Return updated category
            categoryDao.getCategoryByName(name)?.toDomain() ?: existing.toDomain()
                .copy(usageCount = existing.usageCount + 1)
        } else {
            val newCategory = CategoryEntity(
                name = name,
                isHidden = false,
                usageCount = 1,
                lastUsedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
            val id = categoryDao.insertCategory(newCategory)
            newCategory.copy(id = id).toDomain()
        }
    }

    override suspend fun hideCategory(name: String) {
        categoryDao.hideCategory(name)
    }

    override suspend fun incrementCategoryUsage(name: String) {
        categoryDao.incrementUsage(name)
    }

    override suspend fun getPeriodCount(): Int {
        return transactionDao.countDistinctPeriods()
    }

    override suspend fun markCreditTransactionsAsPaid(start: LocalDate, end: LocalDate) {
        val startMillis = start.toEpochDay() * 86400000
        val endMillis = end.toEpochDay() * 86400000
        transactionDao.markCreditAsPaidInRange(startMillis, endMillis)
    }

    override suspend fun markAllCreditTransactionsAsPaid() {
        transactionDao.markAllCreditAsPaid()
    }

    override suspend fun markTransactionAsPaid(transactionId: Long) {
        transactionDao.markTransactionAsPaid(transactionId)
    }

    override fun getArchivedBudgets(): Flow<List<ArchivedBudget>> {
        return archivedBudgetDao.getAllArchivedBudgets().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun upsertArchivedBudgets(archivedBudgets: List<ArchivedBudget>) {
        val entities = archivedBudgets.map { it.toEntity() }
        archivedBudgetDao.insertAll(entities)
    }

    override suspend fun archiveCurrentPeriod(
        periodId: Long,
        settings: BudgetSettings,
        spentAmount: BigDecimal
    ) {
        val entity = ArchivedBudgetEntity(
            periodId = periodId,
            totalBudget = settings.totalBudget.toPlainString(),
            spentAmount = spentAmount.toPlainString(),
            startDate = settings.startDate.toEpochDay() * 86400000,
            endDate = settings.getPeriodEndDate().toEpochDay() * 86400000,
            currencyCode = settings.currencyCode,
            periodType = settings.period.name
        )
        archivedBudgetDao.insert(entity)
        logcat { "Archived period $periodId: total=${settings.totalBudget}, spent=$spentAmount" }
    }

    override suspend fun deleteArchivedBudget(periodId: Long) {
        archivedBudgetDao.deleteById(periodId)
    }
}
