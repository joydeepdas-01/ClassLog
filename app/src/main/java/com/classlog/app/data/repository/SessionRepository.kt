package com.classlog.app.data.repository

import com.classlog.app.data.local.dao.WorkSessionDao
import com.classlog.app.data.local.entity.BatchEntity
import com.classlog.app.data.local.entity.CategoryEntity
import com.classlog.app.data.local.entity.WorkSessionEntity
import com.classlog.app.data.local.model.DayTotal
import com.classlog.app.data.local.model.RecentCombo
import com.classlog.app.domain.model.Issue
import com.classlog.app.domain.validation.SessionValidator
import kotlinx.coroutines.flow.Flow

/**
 * Repository for logging, updating, querying, and duplicating work sessions.
 */
class SessionRepository(
    private val workSessionDao: WorkSessionDao
) {
    fun observeDay(dateEpochDay: Long): Flow<List<WorkSessionEntity>> =
        workSessionDao.observeDay(dateEpochDay)

    fun observeRange(fromEpochDay: Long, toEpochDay: Long): Flow<List<WorkSessionEntity>> =
        workSessionDao.observeRange(fromEpochDay, toEpochDay)

    fun observeFiltered(
        fromEpochDay: Long,
        toEpochDay: Long,
        categoryId: Long?,
        batchId: Long?,
        query: String
    ): Flow<List<WorkSessionEntity>> =
        workSessionDao.observeFiltered(fromEpochDay, toEpochDay, categoryId, batchId, query.trim())

    fun observeRecentCombos(): Flow<List<RecentCombo>> =
        workSessionDao.observeRecentCombos()

    fun observeDayTotals(fromEpochDay: Long, toEpochDay: Long): Flow<List<DayTotal>> =
        workSessionDao.observeDayTotals(fromEpochDay, toEpochDay)

    suspend fun getById(id: Long): WorkSessionEntity? =
        workSessionDao.getById(id)

    suspend fun findOverlaps(
        day: Long,
        start: Int,
        end: Int,
        excludeId: Long = 0L
    ): List<WorkSessionEntity> =
        workSessionDao.findOverlaps(day, start, end, excludeId)

    /**
     * Inserts a new work session after validating hard domain rules.
     * Takes snapshots of category and batch names to ensure historical integrity.
     */
    suspend fun insertSession(
        dateEpochDay: Long,
        startMinutes: Int,
        endMinutes: Int,
        category: CategoryEntity,
        batch: BatchEntity?,
        note: String
    ): Result<Long> {
        val issues = SessionValidator.validate(
            start = startMinutes,
            end = endMinutes,
            isTeaching = category.isTeaching,
            batchId = batch?.id
        )
        if (issues.isNotEmpty()) {
            return Result.failure(IllegalArgumentException(issues.joinToString()))
        }

        val now = System.currentTimeMillis()
        val session = WorkSessionEntity(
            dateEpochDay = dateEpochDay,
            startMinutes = startMinutes,
            endMinutes = endMinutes,
            durationMinutes = endMinutes - startMinutes,
            categoryId = category.id,
            batchId = batch?.id,
            categoryNameSnapshot = category.name,
            batchNameSnapshot = batch?.name,
            isTeachingSnapshot = category.isTeaching,
            note = note.trim(),
            createdAt = now,
            updatedAt = now
        )
        val id = workSessionDao.insert(session)
        return Result.success(id)
    }

    suspend fun updateSession(session: WorkSessionEntity): Result<Unit> {
        val issues = SessionValidator.validate(
            start = session.startMinutes,
            end = session.endMinutes,
            isTeaching = session.isTeachingSnapshot,
            batchId = session.batchId
        )
        if (issues.isNotEmpty()) {
            return Result.failure(IllegalArgumentException(issues.joinToString()))
        }

        workSessionDao.update(
            session.copy(
                durationMinutes = session.endMinutes - session.startMinutes,
                updatedAt = System.currentTimeMillis()
            )
        )
        return Result.success(Unit)
    }

    suspend fun deleteSession(id: Long) {
        workSessionDao.deleteById(id)
    }

    /**
     * Duplicates a session onto a target date (defaults to the same date).
     */
    suspend fun duplicateSession(
        session: WorkSessionEntity,
        targetDateEpochDay: Long = session.dateEpochDay
    ): Long {
        val now = System.currentTimeMillis()
        val copy = session.copy(
            id = 0,
            dateEpochDay = targetDateEpochDay,
            createdAt = now,
            updatedAt = now
        )
        return workSessionDao.insert(copy)
    }

    suspend fun getAllSessions(): List<WorkSessionEntity> =
        workSessionDao.getAll()
}
