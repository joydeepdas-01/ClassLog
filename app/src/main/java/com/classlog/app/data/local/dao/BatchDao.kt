package com.classlog.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.classlog.app.data.local.entity.BatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatchDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(b: BatchEntity): Long

    @Update
    suspend fun update(b: BatchEntity)

    @Delete
    suspend fun delete(b: BatchEntity)

    @Query("SELECT * FROM batches WHERE isActive = 1 ORDER BY sortOrder, name")
    fun observeActive(): Flow<List<BatchEntity>>

    @Query("SELECT * FROM batches ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<BatchEntity>>

    @Query("SELECT * FROM batches ORDER BY name")
    suspend fun getAll(): List<BatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(batches: List<BatchEntity>)

    @Query("DELETE FROM batches")
    suspend fun deleteAll()
}
