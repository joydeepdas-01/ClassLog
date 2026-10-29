package com.classlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Reusable pre-configured time slots for one-tap entry (e.g. "10:00 - 11:00").
 */
@Entity(tableName = "time_slots")
data class TimeSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,                // "10:00 - 11:00"
    val startMinutes: Int,
    val endMinutes: Int,
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)
