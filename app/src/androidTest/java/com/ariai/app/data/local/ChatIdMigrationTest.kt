package com.ariai.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ChatIdMigrationTest {
    @Test
    fun blankChatIdIsReplacedAndExistingMessagesStayLinked() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "migration-${UUID.randomUUID()}.db"
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE chats (id TEXT NOT NULL PRIMARY KEY)")
                        db.execSQL("CREATE TABLE messages (id TEXT NOT NULL PRIMARY KEY, chatId TEXT NOT NULL)")
                        db.execSQL("INSERT INTO chats (id) VALUES ('')")
                        db.execSQL("INSERT INTO messages (id, chatId) VALUES ('message-1', '')")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )

        try {
            val db = helper.writableDatabase
            MIGRATION_5_6.migrate(db)

            val chatId = db.query("SELECT id FROM chats").use { cursor ->
                check(cursor.moveToFirst())
                cursor.getString(0)
            }
            val messageChatId = db.query("SELECT chatId FROM messages WHERE id = 'message-1'").use { cursor ->
                check(cursor.moveToFirst())
                cursor.getString(0)
            }

            assertFalse(chatId.isBlank())
            assertEquals(chatId, messageChatId)
        } finally {
            helper.close()
            context.deleteDatabase(databaseName)
        }
    }
}
