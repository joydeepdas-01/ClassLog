package com.classlog.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "classlog_settings")

/**
 * Manages persisted user settings using Jetpack DataStore Preferences.
 */
class SettingsDataStore(private val context: Context) {

    private object PreferencesKeys {
        val USE_24_HOUR_FORMAT = booleanPreferencesKey("use_24_hour_format")
        val QUICK_DURATIONS = stringPreferencesKey("quick_durations")
        val TEACHER_NAME = stringPreferencesKey("teacher_name")
        val INSTITUTION_NAME = stringPreferencesKey("institution_name")
        val PROFILE_IMAGE_PATH = stringPreferencesKey("profile_image_path")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val use24h = preferences[PreferencesKeys.USE_24_HOUR_FORMAT] ?: false
            val durationsStr = preferences[PreferencesKeys.QUICK_DURATIONS] ?: "60,90,120,180"
            val durations = durationsStr.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .ifEmpty { listOf(60, 90, 120, 180) }
            val teacherName = preferences[PreferencesKeys.TEACHER_NAME] ?: ""
            val institutionName = preferences[PreferencesKeys.INSTITUTION_NAME] ?: ""
            val profileImagePath = preferences[PreferencesKeys.PROFILE_IMAGE_PATH]
            val lockEnabled = preferences[PreferencesKeys.APP_LOCK_ENABLED] ?: false

            AppSettings(
                use24HourFormat = use24h,
                quickDurationMinutes = durations,
                teacherName = teacherName,
                institutionName = institutionName,
                profileImagePath = profileImagePath,
                isAppLockEnabled = lockEnabled
            )
        }

    suspend fun setUse24HourFormat(use24Hour: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_24_HOUR_FORMAT] = use24Hour
        }
    }

    suspend fun setQuickDurations(durations: List<Int>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.QUICK_DURATIONS] = durations.joinToString(",")
        }
    }

    suspend fun setTeacherName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TEACHER_NAME] = name
        }
    }

    suspend fun setInstitutionName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.INSTITUTION_NAME] = name
        }
    }

    suspend fun setProfileImagePath(path: String?) {
        context.dataStore.edit { preferences ->
            if (path != null) {
                preferences[PreferencesKeys.PROFILE_IMAGE_PATH] = path
            } else {
                preferences.remove(PreferencesKeys.PROFILE_IMAGE_PATH)
            }
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LOCK_ENABLED] = enabled
        }
    }
}
