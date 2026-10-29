package com.classlog.app.di

import android.content.Context
import com.classlog.app.data.datastore.SettingsDataStore
import com.classlog.app.data.local.AppDatabase
import com.classlog.app.data.repository.ConfigRepository
import com.classlog.app.data.repository.SessionRepository

/**
 * Dependency container for manual dependency injection across the application.
 */
class AppContainer(
    val context: Context,
    val database: AppDatabase
) {
    val settingsDataStore: SettingsDataStore by lazy {
        SettingsDataStore(context)
    }

    val configRepository: ConfigRepository by lazy {
        ConfigRepository(
            batchDao = database.batchDao(),
            categoryDao = database.categoryDao(),
            timeSlotDao = database.timeSlotDao(),
            settingsDataStore = settingsDataStore
        )
    }

    val sessionRepository: SessionRepository by lazy {
        SessionRepository(
            workSessionDao = database.workSessionDao()
        )
    }

    val backupRestoreManager: com.classlog.app.data.export.BackupRestoreManager by lazy {
        com.classlog.app.data.export.BackupRestoreManager(
            database = database,
            settingsDataStore = settingsDataStore
        )
    }
}
