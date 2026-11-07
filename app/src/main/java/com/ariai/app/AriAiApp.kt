package com.ariai.app

import android.app.Application
import kotlinx.coroutines.launch
import androidx.room.Room
import com.ariai.app.data.local.ALL_MIGRATIONS
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
                preferencesManager.migrateLocalSearchDefaultToBrave()
                preferencesManager.migrateSearchKeysToEncrypted()
            } catch (e: Exception) {
                com.ariai.app.util.AppLog.warn(e, "search preferences migration")
            }
        }
    }

    val database by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "ariai_db"
        )
            .addMigrations(*ALL_MIGRATIONS)
            .build()
    }

    val preferencesManager by lazy {
        PreferencesManager(this)
    }

    val oauthStateStore by lazy {
        com.ariai.app.data.local.OAuthStateStore(database, preferencesManager)
    }
    val oauthSessionManager by lazy {
        com.ariai.app.data.remote.OAuthSessionManager(oauthStateStore, this)
    }

    val aiClient by lazy { AIClient(oauthSessionManager) }
    val searchClient by lazy { SearchClient() }

    val repository by lazy {
        ChatRepository(database, aiClient, searchClient)
    }

    val workspaceRepository by lazy {
        WorkspaceRepository(filesDir)
    }
}
