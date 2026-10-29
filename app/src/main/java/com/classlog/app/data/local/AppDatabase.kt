package com.classlog.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.classlog.app.data.local.dao.BatchDao
import com.classlog.app.data.local.dao.CategoryDao
import com.classlog.app.data.local.dao.TimeSlotDao
import com.classlog.app.data.local.dao.WorkSessionDao
import com.classlog.app.data.local.entity.BatchEntity
import com.classlog.app.data.local.entity.CategoryEntity
import com.classlog.app.data.local.entity.TimeSlotEntity
import com.classlog.app.data.local.entity.WorkSessionEntity

/**
 * Main Room database for ClassLog.
 * Persists batches, categories, time slots, and work sessions.
 * Seeds initial categories, sample batches, and time slots on first creation.
 */
@Database(
    entities = [BatchEntity::class, CategoryEntity::class, TimeSlotEntity::class, WorkSessionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workSessionDao(): WorkSessionDao
    abstract fun batchDao(): BatchDao
    abstract fun categoryDao(): CategoryDao
    abstract fun timeSlotDao(): TimeSlotDao

    companion object {
        const val DATABASE_NAME = "classlog.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        listOf(
                            "Teaching" to 1,
                            "Office Work" to 0,
                            "Maintenance" to 0,
                            "Admission" to 0,
                            "Billing" to 0,
                            "Exam Duty" to 0,
                            "Other" to 0
                        ).forEachIndexed { i, (n, t) ->
                            db.execSQL("INSERT INTO categories (name,isTeaching,isActive,sortOrder) VALUES ('$n',$t,1,$i)")
                        }

                        listOf("Kids", "XII").forEachIndexed { i, n ->
                            db.execSQL("INSERT INTO batches (name,isActive,sortOrder) VALUES ('$n',1,$i)")
                        }

                        listOf(
                            "10:00 - 11:00" to (600 to 660),
                            "11:00 - 13:00" to (660 to 780)
                        ).forEachIndexed { i, (l, r) ->
                            db.execSQL(
                                "INSERT INTO time_slots (label,startMinutes,endMinutes,isActive,sortOrder) VALUES ('$l',${r.first},${r.second},1,$i)"
                            )
                        }
                    }
                })
                .build()
    }
}
