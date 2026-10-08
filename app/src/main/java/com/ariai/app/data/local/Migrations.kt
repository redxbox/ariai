package com.ariai.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds the saved model list to providers. Keeps existing chats and providers. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE providers ADD COLUMN modelsJson TEXT NOT NULL DEFAULT '[]'")
    }
}
