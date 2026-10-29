package com.classlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents an individual logged work session.
 * Stores immutable historical snapshots of category and batch names to ensure historical integrity.
 */
@Entity(
    tableName = "work_sessions",
    foreignKeys = [
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(BatchEntity::class, ["id"], ["batchId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("dateEpochDay"), Index("categoryId"), Index("batchId")]
)
data class WorkSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val startMinutes: Int,
    val endMinutes: Int,
    val durationMinutes: Int,
    val categoryId: Long,
    val batchId: Long? = null,
    val categoryNameSnapshot: String,
    val batchNameSnapshot: String? = null,
    val isTeachingSnapshot: Boolean,
    val note: String = "",
    val createdAt: Long,
    val updatedAt: Long
)
