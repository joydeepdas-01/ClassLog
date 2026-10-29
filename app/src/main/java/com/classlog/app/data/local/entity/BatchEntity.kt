package com.classlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a student batch or group (e.g., "Kids", "XII", "Grade 10").
 */
@Entity(tableName = "batches", indices = [Index(value = ["name"], unique = true)])
data class BatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)
