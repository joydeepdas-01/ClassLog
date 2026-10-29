package com.classlog.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.classlog.app.data.local.entity.TimeSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeSlotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(t: TimeSlotEntity): Long

    @Update
    suspend fun update(t: TimeSlotEntity)

    @Query("SELECT * FROM time_slots WHERE isActive = 1 ORDER BY sortOrder, startMinutes")
    fun observeActive(): Flow<List<TimeSlotEntity>>

    @Query("SELECT * FROM time_slots ORDER BY sortOrder, startMinutes")
    fun observeAll(): Flow<List<TimeSlotEntity>>

    @Query("SELECT * FROM time_slots ORDER BY startMinutes")
    suspend fun getAll(): List<TimeSlotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(slots: List<TimeSlotEntity>)

    @Query("DELETE FROM time_slots")
    suspend fun deleteAll()
}
