package com.ariai.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class Migration7To8Test {
    @Test
    fun addsTokenUsageColumnsWithZeroDefaultsAndPreservesExistingMessages() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "migration-${UUID.randomUUID()}.db"
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(7) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE messages (id TEXT NOT NULL PRIMARY KEY, content TEXT NOT NULL)")
                        db.execSQL("INSERT INTO messages (id, content) VALUES ('legacy', 'kept')")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )

        try {
            val db = helper.writableDatabase
            MIGRATION_7_8.migrate(db)

            db.query("SELECT content, promptTokens, completionTokens, totalTokens FROM messages WHERE id = 'legacy'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("kept", cursor.getString(0))
                assertEquals(0, cursor.getInt(1))
                assertEquals(0, cursor.getInt(2))
                assertEquals(0, cursor.getInt(3))
            }
            db.execSQL("UPDATE messages SET promptTokens = 12, completionTokens = 4, totalTokens = 16 WHERE id = 'legacy'")
            db.query("SELECT promptTokens, completionTokens, totalTokens FROM messages WHERE id = 'legacy'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(12, cursor.getInt(0))
                assertEquals(4, cursor.getInt(1))
                assertEquals(16, cursor.getInt(2))
            }
        } finally {
            helper.close()
            context.deleteDatabase(databaseName)
        }
    }
}
