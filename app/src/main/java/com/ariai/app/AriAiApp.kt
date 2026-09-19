package com.ariai.app

import android.app.Application
import androidx.room.Room
import com.ariai.app.data.local.AppDatabase
import com.ariai.app.data.local.PreferencesManager
import com.ariai.app.data.remote.AIClient
import com.ariai.app.data.remote.SearchClient
import com.ariai.app.data.repository.ChatRepository

class AriAiApp : Application() {
    val database by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "ariai_db"
        ).fallbackToDestructiveMigration().build()
    }

    val preferencesManager by lazy {
        PreferencesManager(this)
    }

    val aiClient by lazy { AIClient() }
    val searchClient by lazy { SearchClient() }

    val repository by lazy {
        ChatRepository(database, aiClient, searchClient)
    }
}
