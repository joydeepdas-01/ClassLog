package com.classlog.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.classlog.app.data.local.entity.WorkSessionEntity
import com.classlog.app.data.local.model.DayTotal
import com.classlog.app.data.local.model.RecentCombo
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(s: WorkSessionEntity): Long

    @Update
    suspend fun update(s: WorkSessionEntity)

    @Query("DELETE FROM work_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM work_sessions WHERE id = :id")
    suspend fun getById(id: Long): WorkSessionEntity?

    @Query("SELECT * FROM work_sessions WHERE dateEpochDay = :day ORDER BY startMinutes")
    fun observeDay(day: Long): Flow<List<WorkSessionEntity>>

    @Query("""
        SELECT * FROM work_sessions
        WHERE dateEpochDay BETWEEN :from AND :to
        ORDER BY dateEpochDay DESC, startMinutes
    """)
    fun observeRange(from: Long, to: Long): Flow<List<WorkSessionEntity>>

    @Query("""
        SELECT * FROM work_sessions
        WHERE dateEpochDay BETWEEN :from AND :to
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:batchId IS NULL OR batchId = :batchId)
          AND (:q = '' OR note LIKE '%' || :q || '%'
               OR categoryNameSnapshot LIKE '%' || :q || '%'
               OR batchNameSnapshot LIKE '%' || :q || '%')
        ORDER BY dateEpochDay DESC, startMinutes
    """)
    fun observeFiltered(
        from: Long,
        to: Long,
        categoryId: Long?,
        batchId: Long?,
        q: String
    ): Flow<List<WorkSessionEntity>>

    /**
     * Sessions that overlap [start, end) on a day. Pass excludeId = 0 when adding a new session.
     */
    @Query("""
        SELECT * FROM work_sessions
        WHERE dateEpochDay = :day AND startMinutes < :end AND endMinutes > :start
          AND id != :excludeId
    """)
    suspend fun findOverlaps(day: Long, start: Int, end: Int, excludeId: Long): List<WorkSessionEntity>

    @Query("""
        SELECT dateEpochDay, SUM(durationMinutes) AS totalMinutes, COUNT(*) AS sessionCount
        FROM work_sessions WHERE dateEpochDay BETWEEN :from AND :to
        GROUP BY dateEpochDay
    """)
    fun observeDayTotals(from: Long, to: Long): Flow<List<DayTotal>>

    @Query("""
        SELECT categoryId, batchId, startMinutes, endMinutes, COUNT(*) AS uses
        FROM work_sessions
        GROUP BY categoryId, batchId, startMinutes, endMinutes
        ORDER BY uses DESC, MAX(updatedAt) DESC LIMIT 6
    """)
    fun observeRecentCombos(): Flow<List<RecentCombo>>

    @Query("SELECT * FROM work_sessions ORDER BY dateEpochDay, startMinutes")
    suspend fun getAll(): List<WorkSessionEntity> // for backup

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<WorkSessionEntity>)

    @Query("DELETE FROM work_sessions")
    suspend fun deleteAll()
}
