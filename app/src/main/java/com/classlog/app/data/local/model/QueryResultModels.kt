package com.classlog.app.data.local.model

/**
 * Aggregated total duration and count of sessions for a specific day.
 */
data class DayTotal(
    val dateEpochDay: Long,
    val totalMinutes: Int,
    val sessionCount: Int
)

/**
 * Frequency aggregation of work categories, batches, and time ranges to power Quick Repeat chips.
 */
data class RecentCombo(
    val categoryId: Long,
    val batchId: Long?,
    val startMinutes: Int,
    val endMinutes: Int,
    val uses: Int
)
