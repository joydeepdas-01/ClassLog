package com.classlog.app.domain.model

/**
 * Summary metrics of work logged over a given month or period.
 */
data class MonthSummary(
    val totalMinutes: Int,
    val sessionCount: Int,
    val daysWorked: Int,
    val teachingMinutes: Int,
    val nonTeachingMinutes: Int,
    val byCategory: Map<String, Int>,
    val byBatch: Map<String, Int>
)
