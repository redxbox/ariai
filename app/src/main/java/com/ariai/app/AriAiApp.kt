package com.ariai.app

import android.app.Application
import kotlinx.coroutines.launch
import androidx.room.Room
import com.ariai.app.data.local.AppDatabase
import com.ariai.app.data.local.PreferencesManager
import com.ariai.app.data.remote.AIClient
import com.ariai.app.data.remote.SearchClient
import com.ariai.app.data.repository.ChatRepository
import com.ariai.app.data.repository.WorkspaceRepository

class AriAiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.ariai.app.data.local.ImageStore.init(filesDir)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            repository.migrateProviderKeysToEncrypted()
            try {
                preferencesManager.migrateSearchKeysToEncrypted()
            } catch (e: Exception) {
                com.ariai.app.util.AppLog.warn(e, "search key migration")
            }
        }
    }

    val database by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "ariai_db"
        )
            .addMigrations(
                com.ariai.app.data.local.MIGRATION_1_2,
                com.ariai.app.data.local.MIGRATION_2_3,
                com.ariai.app.data.local.MIGRATION_3_4,
                com.ariai.app.data.local.MIGRATION_4_5,
                com.ariai.app.data.local.MIGRATION_5_6,
                com.ariai.app.data.local.MIGRATION_6_7
            )
            .build()
    }

    val preferencesManager by lazy {
        PreferencesManager(this)
    }

    val aiClient by lazy { AIClient() }
    val searchClient by lazy { SearchClient() }

    val repository by lazy {
        ChatRepository(database, aiClient, searchClient)
    }

    val workspaceRepository by lazy {
        WorkspaceRepository(filesDir)
    }
}
