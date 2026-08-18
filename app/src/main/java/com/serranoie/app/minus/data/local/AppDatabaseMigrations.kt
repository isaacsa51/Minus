package com.serranoie.app.minus.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AppDatabaseMigrations {

    val MIGRATION_6_7: Migration = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN clientGeneratedId TEXT")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_clientGeneratedId ON transactions(clientGeneratedId)"
            )
        }
    }

    val MIGRATION_8_9: Migration = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS queued_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    amount TEXT NOT NULL,
                    comment TEXT NOT NULL,
                    date INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    categoryId INTEGER
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_9_10: Migration = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN isCredit INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE queued_transactions ADD COLUMN isCredit INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE budget_settings ADD COLUMN creditCardCutoffDay INTEGER")
        }
    }

    val MIGRATION_10_11: Migration = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE budget_settings ADD COLUMN splitMode TEXT NOT NULL DEFAULT 'STATIC'"
            )
        }
    }

    val MIGRATION_11_12: Migration = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN isCreditPaid INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                "ALTER TABLE queued_transactions ADD COLUMN isCreditPaid INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    val MIGRATION_12_13: Migration = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `archived_budgets` (
                    `periodId` INTEGER NOT NULL, 
                    `totalBudget` TEXT NOT NULL, 
                    `spentAmount` TEXT NOT NULL, 
                    `startDate` INTEGER NOT NULL, 
                    `endDate` INTEGER NOT NULL, 
                    `currencyCode` TEXT NOT NULL, 
                    `periodType` TEXT NOT NULL, 
                    `createdAt` INTEGER NOT NULL, 
                    PRIMARY KEY(`periodId`)
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_13_14: Migration = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE budget_settings ADD COLUMN rollOverLimit TEXT")
        }
    }

    val MIGRATION_14_15: Migration = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `paid_recurrent_occurrences` (
                    `transactionId` INTEGER NOT NULL,
                    `occurrenceDateEpochDay` INTEGER NOT NULL,
                    `paidAt` INTEGER NOT NULL,
                    PRIMARY KEY(`transactionId`, `occurrenceDateEpochDay`)
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_15_16: Migration = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Transactions created before category auto-resolution existed (e.g. CSV imports,
            // which never called findOrCreateCategory) are left with categoryId = NULL even
            // though their comment holds the category name, so they always render as
            // "Uncategorized" in analytics. Back-fill a category per distinct comment and link it.
            db.execSQL(
                """
                INSERT OR IGNORE INTO category (name, isHidden, usageCount, lastUsedAt, createdAt)
                SELECT DISTINCT TRIM(comment), 0, 0, NULL, ${System.currentTimeMillis()}
                FROM transactions
                WHERE categoryId IS NULL AND TRIM(comment) != ''
                """.trimIndent()
            )
            db.execSQL(
                """
                UPDATE transactions
                SET categoryId = (
                    SELECT id FROM category WHERE category.name = TRIM(transactions.comment)
                )
                WHERE categoryId IS NULL AND TRIM(comment) != ''
                """.trimIndent()
            )
        }
    }
}
