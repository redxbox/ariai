package com.ariai.app.data.local

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MigrationChainTest {
    @Test
    fun migratesVersionOneDataThroughEveryMigrationAndMatchesCurrentRoomSchema() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "migration-chain-${UUID.randomUUID()}.db"
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createVersionOneSchema(db)
                        insertVersionOneRows(db)
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )

        try {
            try {
                assertEquals(1, helper.writableDatabase.version)
            } finally {
                helper.close()
            }

            // Room performs the complete registered upgrade path and validates the final entity schema.
            val roomDatabase = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
                .addMigrations(*ALL_MIGRATIONS)
                .allowMainThreadQueries()
                .build()
            try {
                val db = roomDatabase.openHelper.writableDatabase
                assertEquals(9, db.version)
                assertMigratedProvider(db)
                assertMigratedChatsAndMessages(db)
                assertMigratedAgents(db)
            } finally {
                roomDatabase.close()
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

    private fun createVersionOneSchema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE providers (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                baseUrl TEXT NOT NULL,
                apiKey TEXT NOT NULL,
                enabled INTEGER NOT NULL,
                customHeaders TEXT NOT NULL,
                customBody TEXT,
                createdAt INTEGER NOT NULL
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE chats (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                providerId TEXT,
                modelId TEXT,
                agentId TEXT,
                systemPrompt TEXT,
                isPinned INTEGER NOT NULL,
                isArchived INTEGER NOT NULL,
                memoryEnabled INTEGER NOT NULL
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE messages (
                id TEXT NOT NULL PRIMARY KEY,
                chatId TEXT NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                status TEXT NOT NULL,
                modelId TEXT,
                providerId TEXT,
                parentId TEXT,
                branchChildren TEXT NOT NULL,
                reasoning TEXT
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE agents (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                systemPrompt TEXT NOT NULL,
                avatar TEXT NOT NULL,
                modelId TEXT,
                providerId TEXT,
                temperature REAL NOT NULL,
                topP REAL NOT NULL,
                isBuiltIn INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )""".trimIndent()
        )
    }

    private fun insertVersionOneRows(db: SupportSQLiteDatabase) {
        db.execSQL(
            """INSERT INTO providers
                (id, name, type, baseUrl, apiKey, enabled, customHeaders, customBody, createdAt)
                VALUES ('legacy-provider', 'Legacy provider', 'OPENAI_COMPATIBLE',
                    'https://legacy.example.test/v1', 'legacy-key-placeholder', 1, '{"X-Legacy":"kept"}', NULL, 1)""".trimIndent()
        )
        db.execSQL(
            """INSERT INTO chats
                (id, title, createdAt, updatedAt, providerId, modelId, agentId, systemPrompt,
                 isPinned, isArchived, memoryEnabled)
                VALUES ('chat-kept', 'Legacy chat', 1, 2, 'legacy-provider', 'legacy-model', NULL, NULL, 1, 0, 0)""".trimIndent()
        )
        db.execSQL(
            """INSERT INTO chats
                (id, title, createdAt, updatedAt, providerId, modelId, agentId, systemPrompt,
                 isPinned, isArchived, memoryEnabled)
                VALUES ('', 'Recovered blank id', 1, 3, NULL, NULL, NULL, NULL, 0, 0, 0)""".trimIndent()
        )
        db.execSQL(
            """INSERT INTO messages
                (id, chatId, role, content, timestamp, status, modelId, providerId, parentId,
                 branchChildren, reasoning)
                VALUES ('legacy-message', 'chat-kept', 'USER', 'keep this message', 2,
                    'COMPLETE', 'legacy-model', 'legacy-provider', NULL, '', NULL)""".trimIndent()
        )
        db.execSQL(
            """INSERT INTO messages
                (id, chatId, role, content, timestamp, status, modelId, providerId, parentId,
                 branchChildren, reasoning)
                VALUES ('relinked-message', '', 'ASSISTANT', 'relink this message', 3,
                    'COMPLETE', 'legacy-model', 'legacy-provider', NULL, '', NULL)""".trimIndent()
        )

        listOf("general", "coder", "researcher", "translator", "artist", "custom-agent").forEach { id ->
            db.execSQL(
                """INSERT INTO agents
                    (id, name, description, systemPrompt, avatar, modelId, providerId,
                     temperature, topP, isBuiltIn, createdAt)
                    VALUES (?, ?, '', '', '', NULL, NULL, 0.7, 1.0, ?, 1)""",
                arrayOf(id, "Legacy $id", if (id == "custom-agent") 0 else 1)
            )
        }
    }

    private fun assertMigratedProvider(db: SupportSQLiteDatabase) {
        db.query(
            "SELECT apiKey, customHeaders, modelsJson, authType, oauthState FROM providers WHERE id = 'legacy-provider'"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("legacy-key-placeholder", cursor.getString(0))
            assertEquals("{\"X-Legacy\":\"kept\"}", cursor.getString(1))
            assertEquals("[]", cursor.getString(2))
            assertEquals("API_KEY", cursor.getString(3))
            assertTrue(cursor.isNull(4))
        }
    }

    private fun assertMigratedChatsAndMessages(db: SupportSQLiteDatabase) {
        db.query("SELECT title, temperature, topP, folder, isPinned FROM chats WHERE id = 'chat-kept'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Legacy chat", cursor.getString(0))
            assertEquals(0.7, cursor.getDouble(1), 0.001)
            assertEquals(1.0, cursor.getDouble(2), 0.001)
            assertTrue(cursor.isNull(3))
            assertEquals(1, cursor.getInt(4))
        }

        val repairedChatId = db.query("SELECT id FROM chats WHERE title = 'Recovered blank id'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getString(0)
        }
        assertFalse(repairedChatId.isNullOrBlank())
        db.query("SELECT chatId FROM messages WHERE id = 'relinked-message'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(repairedChatId, cursor.getString(0))
        }

        db.query(
            "SELECT content, imagePath, attachmentsJson, promptTokens, completionTokens, totalTokens " +
                "FROM messages WHERE id = 'legacy-message'"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("keep this message", cursor.getString(0))
            assertTrue(cursor.isNull(1))
            assertEquals("[]", cursor.getString(2))
            assertEquals(0, cursor.getInt(3))
            assertEquals(0, cursor.getInt(4))
            assertEquals(0, cursor.getInt(5))
        }
    }

    private fun assertMigratedAgents(db: SupportSQLiteDatabase) {
        val expectedTools = mapOf(
            "general" to "[\"CALCULATOR\",\"WEB_SEARCH\"]",
            "coder" to "[\"WEB_SEARCH\",\"CALCULATOR\"]",
            "researcher" to "[\"WEB_SEARCH\",\"CALCULATOR\"]",
            "translator" to "[\"WEB_SEARCH\"]",
            "artist" to "[\"IMAGE_GENERATION\"]",
            "custom-agent" to "[]"
        )
        expectedTools.forEach { (id, toolsJson) ->
            db.query(
                "SELECT toolsJson, promptVariablesJson, examplesJson FROM agents WHERE id = ?",
                arrayOf(id)
            ).use { cursor ->
                assertTrue("Missing migrated agent $id", cursor.moveToFirst())
                assertEquals(toolsJson, cursor.getString(0))
                assertEquals("{}", cursor.getString(1))
                assertEquals("[]", cursor.getString(2))
            }
        }
    }
}
