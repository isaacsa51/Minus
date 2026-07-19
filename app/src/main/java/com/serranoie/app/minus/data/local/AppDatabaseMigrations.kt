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
}
