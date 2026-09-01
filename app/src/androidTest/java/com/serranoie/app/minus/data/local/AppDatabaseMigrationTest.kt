package com.serranoie.app.minus.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.local.AppDatabaseMigrations.MIGRATION_13_14
import com.serranoie.app.minus.data.local.AppDatabaseMigrations.MIGRATION_14_15
import com.serranoie.app.minus.data.local.AppDatabaseMigrations.MIGRATION_15_16
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private val testDb = "minus-migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    private fun SupportSQLiteDatabase.columnNames(table: String): Set<String> =
        query("PRAGMA table_info(`$table`)").use { c ->
            buildSet {
                val nameIdx = c.getColumnIndex("name")
                while (c.moveToNext()) add(c.getString(nameIdx))
            }
        }

    private fun SupportSQLiteDatabase.tableExists(table: String): Boolean =
        query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(table),
        ).use { it.count > 0 }

    private fun SupportSQLiteDatabase.longOf(sql: String): Long =
        query(sql).use { c ->
            c.moveToFirst()
            c.getLong(0)
        }

    @Test
    fun migrate13To14_addsRollOverLimitColumn_andPreservesExistingRow() {
        helper.createDatabase(testDb, 13).apply {
            execSQL(
                "INSERT INTO budget_settings " +
                    "(id, totalBudget, period, startDate, endDate, currencyCode, daysInPeriod, " +
                    "rollOverEnabled, rollOverCarryForward, remainingBudgetStrategy, creditCardCutoffDay, splitMode) " +
                    "VALUES (1, '1000.00', 'MONTHLY', 0, NULL, 'USD', 30, 0, 0, 'ASK_ALWAYS', NULL, 'STATIC')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDb, 14, true, MIGRATION_13_14)

        assertThat(db.columnNames("budget_settings")).contains("rollOverLimit")
        db.query("SELECT rollOverLimit, totalBudget FROM budget_settings WHERE id = 1").use { c ->
            assertThat(c.moveToFirst()).isTrue()
            assertThat(c.isNull(0)).isTrue()
            assertThat(c.getString(1)).isEqualTo("1000.00")
        }
    }

    @Test
    fun migrate14To15_createsPaidRecurrentOccurrencesTable() {
        helper.createDatabase(testDb, 14).close()

        val db = helper.runMigrationsAndValidate(testDb, 15, true, MIGRATION_14_15)

        assertThat(db.tableExists("paid_recurrent_occurrences")).isTrue()
        assertThat(db.columnNames("paid_recurrent_occurrences"))
            .containsExactly("transactionId", "occurrenceDateEpochDay", "paidAt")
    }

    @Test
    fun migrate15To16_backfillsCategoriesFromComments_andLinksUncategorizedTransactions() {
        helper.createDatabase(testDb, 15).apply {
            fun insertTx(comment: String, categoryId: Long?) = execSQL(
                "INSERT INTO transactions " +
                    "(amount, comment, date, createdAt, periodId, isRecurrent, isCredit, isCreditPaid, categoryId) " +
                    "VALUES ('10.00', ?, 0, 0, 1, 0, 0, 0, ?)",
                arrayOf<Any?>(comment, categoryId),
            )
            execSQL(
                "INSERT INTO category (id, name, isHidden, usageCount, lastUsedAt, createdAt) " +
                    "VALUES (1, 'Groceries', 0, 0, NULL, 0)"
            )
            insertTx("Coffee", null)
            insertTx("Coffee", null)
            insertTx("Rent", null)
            insertTx("   ", null)        // blank comment -> must stay uncategorized
            insertTx("Groceries", 1L)    // already linked -> must stay untouched
            close()
        }

        val db = helper.runMigrationsAndValidate(testDb, 16, true, MIGRATION_15_16)

        assertThat(db.longOf("SELECT COUNT(*) FROM category")).isEqualTo(3)
        assertThat(db.longOf("SELECT COUNT(*) FROM category WHERE name = 'Coffee'")).isEqualTo(1)
        assertThat(db.longOf("SELECT COUNT(*) FROM category WHERE name = 'Rent'")).isEqualTo(1)

        val coffeeCategoryId = db.longOf("SELECT id FROM category WHERE name = 'Coffee'")
        db.query("SELECT DISTINCT categoryId FROM transactions WHERE comment = 'Coffee'").use { c ->
            assertThat(c.count).isEqualTo(1)
            c.moveToFirst()
            assertThat(c.getLong(0)).isEqualTo(coffeeCategoryId)
        }

        assertThat(
            db.longOf("SELECT COUNT(*) FROM transactions WHERE TRIM(comment) = '' AND categoryId IS NOT NULL")
        ).isEqualTo(0)
        assertThat(db.longOf("SELECT categoryId FROM transactions WHERE comment = 'Groceries'")).isEqualTo(1)
    }

    @Test
    fun migrateAllFrom13_runsTheWholeChain_andValidatesTheCurrentSchema() {
        helper.createDatabase(testDb, 13).close()

        helper.runMigrationsAndValidate(
            testDb, 17, true,
            MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16,
        )
    }
}
