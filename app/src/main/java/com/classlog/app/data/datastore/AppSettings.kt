package com.classlog.app.data.datastore

/**
 * Immutable data holder for user preferences.
 */
data class AppSettings(
    val use24HourFormat: Boolean = false,
    val quickDurationMinutes: List<Int> = listOf(60, 90, 120, 180), // 1h, 1.5h, 2h, 3h
    val teacherName: String = "",
    val institutionName: String = "",
    val profileImagePath: String? = null,
    val isAppLockEnabled: Boolean = false
)
