package com.ariai.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds the generated-media path to messages. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE messages ADD COLUMN imagePath TEXT")
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE providers ADD COLUMN modelsJson TEXT NOT NULL DEFAULT '[]'")
    }
}

/** Keeps attachment metadata (file paths) with messages, so photos survive reloads. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE messages ADD COLUMN attachmentsJson TEXT NOT NULL DEFAULT '[]'")
    }
}

/** Adds indexes for ordered chat history and chat-list queries. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_chats_updatedAt ON chats(updatedAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_chatId_timestamp ON messages(chatId, timestamp)")
    }
}

/** Repairs legacy chats with an empty ID before the drawer can navigate to `chat/`. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val invalidIds = buildList {
            db.query("SELECT id FROM chats WHERE id IS NULL OR TRIM(id) = ''").use { cursor ->
                while (cursor.moveToNext()) add(cursor.getString(0).orEmpty())
            }
        }
        invalidIds.forEach { oldId ->
            val repairedId = java.util.UUID.randomUUID().toString()
            db.execSQL("UPDATE chats SET id = ? WHERE id = ?", arrayOf(repairedId, oldId))
            db.execSQL("UPDATE messages SET chatId = ? WHERE chatId = ?", arrayOf(repairedId, oldId))
        }
    }
}
