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
class Migration6To7Test {
    @Test
    fun addsChatOptionsFoldersAndAgentToolDefaults() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-${UUID.randomUUID()}.db"
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE chats (id TEXT NOT NULL PRIMARY KEY)")
                    db.execSQL("INSERT INTO chats (id) VALUES ('legacy-chat')")
                    db.execSQL("CREATE TABLE agents (id TEXT NOT NULL PRIMARY KEY)")
                    db.execSQL("INSERT INTO agents (id) VALUES ('general')")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)

        try {
            val db = helper.writableDatabase
            MIGRATION_6_7.migrate(db)

            db.query("SELECT temperature, topP, folder FROM chats WHERE id = 'legacy-chat'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0.7, cursor.getDouble(0), 0.001)
                assertEquals(1.0, cursor.getDouble(1), 0.001)
                assertTrue(cursor.isNull(2))
            }
            db.query("SELECT toolsJson FROM agents WHERE id = 'general'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("[\"CALCULATOR\",\"WEB_SEARCH\"]", cursor.getString(0))
            }
            db.query("SELECT promptVariablesJson, examplesJson FROM agents WHERE id = 'general'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("{}", cursor.getString(0))
                assertEquals("[]", cursor.getString(1))
            }
        } finally {
            helper.close()
            context.deleteDatabase(name)
        }
    }
}
