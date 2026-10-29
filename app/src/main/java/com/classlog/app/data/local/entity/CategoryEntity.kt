package com.classlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a work category (e.g., "Teaching", "Office Work", "Maintenance").
 * When [isTeaching] is true, a batch assignment is required for work sessions.
 */
@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isTeaching: Boolean,          // true: needs a batch
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)
