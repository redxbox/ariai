package com.ariai.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Type converters
class Converters {
    // Custom headers are stored as a JSON object. Earlier builds used a fragile
    // "key:::value" joined by "|||"; [toStringMap] still understands that format so
    // configurations saved by older installs keep working.
    @TypeConverter
    fun fromStringMap(map: Map<String, String>): String =
        org.json.JSONObject().apply { map.forEach { (key, value) -> put(key, value) } }.toString()

    @TypeConverter
    fun toStringMap(data: String): Map<String, String> {
        if (data.isEmpty()) return emptyMap()
        return runCatching {
            val json = org.json.JSONObject(data)
            json.keys().asSequence().associateWith { json.getString(it) }
        }.getOrElse {
            // Legacy format from before JSON storage.
            buildMap {
                data.split("|||").forEach { entry ->
                    val idx = entry.indexOf(":::")
                    if (idx > 0) put(entry.substring(0, idx), entry.substring(idx + 3))
                }
            }
        }
    }
}

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val baseUrl: String,
    val apiKey: String,
    val enabled: Boolean,
    val customHeaders: String,
    val customBody: String?,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "'[]'") val modelsJson: String = "[]",
    @ColumnInfo(defaultValue = "'API_KEY'") val authType: String = "API_KEY",
    val oauthAuthorizationEndpoint: String? = null,
    val oauthTokenEndpoint: String? = null,
    val oauthClientId: String? = null,
    val oauthScopes: String? = null,
    val oauthRedirectUri: String? = null,
    val oauthState: String? = null
)

@Entity(tableName = "chats", indices = [Index(value = ["updatedAt"], name = "index_chats_updatedAt")])
data class ChatEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val providerId: String?,
    val modelId: String?,
    val agentId: String?,
    val systemPrompt: String?,
    @ColumnInfo(defaultValue = "0.7") val temperature: Float = 0.7f,
    @ColumnInfo(defaultValue = "1.0") val topP: Float = 1.0f,
    val folder: String? = null,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val memoryEnabled: Boolean
)

@Entity(tableName = "messages", indices = [Index(value = ["chatId", "timestamp"], name = "index_messages_chatId_timestamp")])
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val status: String,
    val modelId: String?,
    val providerId: String?,
    val parentId: String?,
    val branchChildren: String,
    val reasoning: String?,
    val imagePath: String? = null,
    @ColumnInfo(defaultValue = "'[]'") val attachmentsJson: String = "[]",
    @ColumnInfo(defaultValue = "0") val promptTokens: Int = 0,
    @ColumnInfo(defaultValue = "0") val completionTokens: Int = 0,
    @ColumnInfo(defaultValue = "0") val totalTokens: Int = 0
)

@Entity(tableName = "agents")
data class AgentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val avatar: String,
    val modelId: String?,
    val providerId: String?,
    val temperature: Float,
    val topP: Float,
    @ColumnInfo(defaultValue = "'[]'") val toolsJson: String = "[]",
    @ColumnInfo(defaultValue = "'{}'") val promptVariablesJson: String = "{}",
    @ColumnInfo(defaultValue = "'[]'") val examplesJson: String = "[]",
    val isBuiltIn: Boolean,
    val createdAt: Long
)

@Dao
interface ProviderDao {
    @Query("SELECT * FROM providers ORDER BY createdAt DESC")
    fun getAllProviders(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers ORDER BY createdAt DESC")
    suspend fun getAllProvidersSync(): List<ProviderEntity>

    @Query("SELECT * FROM providers WHERE id = :id")
    suspend fun getProviderById(id: String): ProviderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: ProviderEntity)

    @Query("""
        UPDATE providers
        SET apiKey = :apiKey,
            customHeaders = :customHeaders,
            customBody = :customBody,
            modelsJson = :modelsJson,
            oauthState = :oauthState
        WHERE id = :id
          AND apiKey = :oldApiKey
          AND customHeaders = :oldCustomHeaders
          AND customBody IS :oldCustomBody
          AND modelsJson = :oldModelsJson
          AND oauthState IS :oldOauthState
    """)
    suspend fun migrateProviderSecretsIfUnchanged(
        id: String,
        oldApiKey: String,
        oldCustomHeaders: String,
        oldCustomBody: String?,
        oldModelsJson: String,
        oldOauthState: String?,
        apiKey: String,
        customHeaders: String,
        customBody: String?,
        modelsJson: String,
        oauthState: String?
    ): Int

    @Query("""
        UPDATE providers SET oauthState = :newCiphertext
        WHERE id = :id
          AND baseUrl = :resourceUri
          AND authType = 'OAUTH2'
          AND oauthAuthorizationEndpoint = :authorizationEndpoint
          AND oauthTokenEndpoint = :tokenEndpoint
          AND oauthClientId = :clientId
          AND oauthScopes = :scopes
          AND oauthRedirectUri = :redirectUri
          AND oauthState IS :oldCiphertext
    """)
    suspend fun updateOAuthStateIfUnchanged(
        id: String,
        resourceUri: String,
        authorizationEndpoint: String,
        tokenEndpoint: String,
        clientId: String,
        scopes: String,
        redirectUri: String,
        oldCiphertext: String?,
        newCiphertext: String
    ): Int

    @Query("""
        UPDATE providers SET oauthState = :ciphertext
        WHERE id = :id
          AND baseUrl = :resourceUri
          AND authType = 'OAUTH2'
          AND oauthAuthorizationEndpoint = :authorizationEndpoint
          AND oauthTokenEndpoint = :tokenEndpoint
          AND oauthClientId = :clientId
          AND oauthScopes = :scopes
          AND oauthRedirectUri = :redirectUri
    """)
    suspend fun replaceOAuthStateForAuthorization(
        id: String,
        resourceUri: String,
        authorizationEndpoint: String,
        tokenEndpoint: String,
        clientId: String,
        scopes: String,
        redirectUri: String,
        ciphertext: String
    ): Int

    @Delete
    suspend fun deleteProvider(provider: ProviderEntity)

    @Query("DELETE FROM providers WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :id")
    suspend fun getChatById(id: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    /** Inserts a recovery row only if no other operation has already created this chat. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChatIfAbsent(chat: ChatEntity): Long

    @Delete
    suspend fun deleteChat(chat: ChatEntity)

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface MessageDao {
    /** Chats with at least one message containing [pattern]. The caller escapes LIKE wildcards. */
    @Query("SELECT DISTINCT chatId FROM messages WHERE content LIKE '%' || :pattern || '%' ESCAPE '\\'")
    suspend fun chatIdsContaining(pattern: String): List<String>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    suspend fun getMessagesForChatSync(chatId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: String): MessageEntity?

    @Query("SELECT * FROM messages")
    suspend fun getAllMessagesSync(): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface AgentDao {
    @Query("SELECT * FROM agents ORDER BY createdAt ASC")
    fun getAllAgents(): Flow<List<AgentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgent(agent: AgentEntity)

    @Query("DELETE FROM agents WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Database(
    entities = [ProviderEntity::class, ChatEntity::class, MessageEntity::class, AgentEntity::class],
    version = 9,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun providerDao(): ProviderDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun agentDao(): AgentDao
}
