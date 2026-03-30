package com.serranoie.app.minus.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AppDatabaseMigrations {

    val MIGRATION_6_7: Migration = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN clientGeneratedId TEXT")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_clientGeneratedId ON transactions(clientGeneratedId)")
        }
    }
}
