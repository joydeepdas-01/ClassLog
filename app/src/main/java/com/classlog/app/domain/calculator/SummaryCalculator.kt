package com.classlog.app.domain.calculator

import com.classlog.app.data.local.entity.WorkSessionEntity
import com.classlog.app.domain.model.MonthSummary

/**
 * Pure Kotlin calculator that processes work sessions into monthly aggregate summaries.
 */
object SummaryCalculator {
    /**
     * Aggregates a list of work sessions into a [MonthSummary].
     */
    fun summarise(sessions: List<WorkSessionEntity>): MonthSummary = MonthSummary(
        totalMinutes = sessions.sumOf { it.durationMinutes },
        sessionCount = sessions.size,
        daysWorked = sessions.map { it.dateEpochDay }.distinct().size,
        teachingMinutes = sessions.filter { it.isTeachingSnapshot }.sumOf { it.durationMinutes },
        nonTeachingMinutes = sessions.filterNot { it.isTeachingSnapshot }.sumOf { it.durationMinutes },
        byCategory = sessions.groupBy { it.categoryNameSnapshot }
            .mapValues { (_, v) -> v.sumOf { it.durationMinutes } },
        byBatch = sessions.filter { it.batchNameSnapshot != null }
            .groupBy { it.batchNameSnapshot!! }
            .mapValues { (_, v) -> v.sumOf { it.durationMinutes } }
    )
}
