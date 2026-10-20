package com.fileora.app

import android.app.Application
import com.fileora.app.data.local.AppPreferences
import com.fileora.app.data.repository.HistoryRepository

class FileoraApp : Application() {

    lateinit var preferences: AppPreferences
        private set

    lateinit var historyRepository: HistoryRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferences = AppPreferences(this)
        historyRepository = HistoryRepository(this)
    }

    companion object {
        lateinit var instance: FileoraApp
            private set
    }
}
