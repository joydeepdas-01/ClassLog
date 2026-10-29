package com.classlog.app.data.export

import androidx.room.withTransaction
import com.classlog.app.data.datastore.AppSettings
import com.classlog.app.data.datastore.SettingsDataStore
import com.classlog.app.data.local.AppDatabase
import com.classlog.app.data.local.entity.BatchEntity
import com.classlog.app.data.local.entity.CategoryEntity
import com.classlog.app.data.local.entity.TimeSlotEntity
import com.classlog.app.data.local.entity.WorkSessionEntity
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

data class RestoreResult(
    val sessionCount: Int,
    val batchCount: Int,
    val categoryCount: Int,
    val slotCount: Int
)

class BackupRestoreManager(
    private val database: AppDatabase,
    private val settingsDataStore: SettingsDataStore
) {

    /**
     * Creates a complete JSON snapshot of all configuration, settings, and logged sessions.
     */
    suspend fun createBackupJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        // 1. Settings
        val settings = settingsDataStore.settingsFlow.first()
        val settingsObj = JSONObject().apply {
            put("use24HourFormat", settings.use24HourFormat)
            put("teacherName", settings.teacherName)
            put("institutionName", settings.institutionName)
            put("isAppLockEnabled", settings.isAppLockEnabled)
            val durationsArr = JSONArray()
            settings.quickDurationMinutes.forEach { durationsArr.put(it) }
            put("quickDurations", durationsArr)
        }
        root.put("settings", settingsObj)

        // 2. Batches
        val batches = database.batchDao().getAll()
        val batchesArr = JSONArray()
        for (b in batches) {
            batchesArr.put(JSONObject().apply {
                put("id", b.id)
                put("name", b.name)
                put("isActive", b.isActive)
                put("sortOrder", b.sortOrder)
            })
        }
        root.put("batches", batchesArr)

        // 3. Categories
        val categories = database.categoryDao().getAll()
        val categoriesArr = JSONArray()
        for (c in categories) {
            categoriesArr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("isTeaching", c.isTeaching)
                put("isActive", c.isActive)
                put("sortOrder", c.sortOrder)
            })
        }
        root.put("categories", categoriesArr)

        // 4. Time Slots
        val slots = database.timeSlotDao().getAll()
        val slotsArr = JSONArray()
        for (s in slots) {
            slotsArr.put(JSONObject().apply {
                put("id", s.id)
                put("label", s.label)
                put("startMinutes", s.startMinutes)
                put("endMinutes", s.endMinutes)
                put("isActive", s.isActive)
                put("sortOrder", s.sortOrder)
            })
        }
        root.put("timeSlots", slotsArr)

        // 5. Work Sessions
        val sessions = database.workSessionDao().getAll()
        val sessionsArr = JSONArray()
        for (s in sessions) {
            sessionsArr.put(JSONObject().apply {
                put("id", s.id)
                put("dateEpochDay", s.dateEpochDay)
                put("startMinutes", s.startMinutes)
                put("endMinutes", s.endMinutes)
                put("durationMinutes", s.durationMinutes)
                put("categoryId", s.categoryId)
                if (s.batchId != null) put("batchId", s.batchId)
                put("categoryNameSnapshot", s.categoryNameSnapshot)
                if (s.batchNameSnapshot != null) put("batchNameSnapshot", s.batchNameSnapshot)
                put("isTeachingSnapshot", s.isTeachingSnapshot)
                put("note", s.note)
                put("createdAt", s.createdAt)
                put("updatedAt", s.updatedAt)
            })
        }
        root.put("workSessions", sessionsArr)

        return root.toString(2)
    }

    /**
     * Atomically restores database and settings from a JSON backup.
     */
    suspend fun restoreFromJson(jsonString: String): Result<RestoreResult> {
        return try {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", -1)
            if (version != 1) {
                return Result.failure(IllegalArgumentException("Unsupported backup version: $version"))
            }

            // Parse Batches
            val batchesArr = root.optJSONArray("batches") ?: JSONArray()
            val batches = mutableListOf<BatchEntity>()
            for (i in 0 until batchesArr.length()) {
                val obj = batchesArr.getJSONObject(i)
                batches.add(
                    BatchEntity(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        isActive = obj.optBoolean("isActive", true),
                        sortOrder = obj.optInt("sortOrder", 0)
                    )
                )
            }

            // Parse Categories
            val categoriesArr = root.optJSONArray("categories") ?: JSONArray()
            val categories = mutableListOf<CategoryEntity>()
            for (i in 0 until categoriesArr.length()) {
                val obj = categoriesArr.getJSONObject(i)
                categories.add(
                    CategoryEntity(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        isTeaching = obj.optBoolean("isTeaching", true),
                        isActive = obj.optBoolean("isActive", true),
                        sortOrder = obj.optInt("sortOrder", 0)
                    )
                )
            }

            // Parse Time Slots
            val slotsArr = root.optJSONArray("timeSlots") ?: JSONArray()
            val slots = mutableListOf<TimeSlotEntity>()
            for (i in 0 until slotsArr.length()) {
                val obj = slotsArr.getJSONObject(i)
                slots.add(
                    TimeSlotEntity(
                        id = obj.optLong("id", 0L),
                        label = obj.getString("label"),
                        startMinutes = obj.getInt("startMinutes"),
                        endMinutes = obj.getInt("endMinutes"),
                        isActive = obj.optBoolean("isActive", true),
                        sortOrder = obj.optInt("sortOrder", 0)
                    )
                )
            }

            // Parse Work Sessions
            val sessionsArr = root.optJSONArray("workSessions") ?: JSONArray()
            val sessions = mutableListOf<WorkSessionEntity>()
            for (i in 0 until sessionsArr.length()) {
                val obj = sessionsArr.getJSONObject(i)
                sessions.add(
                    WorkSessionEntity(
                        id = obj.optLong("id", 0L),
                        dateEpochDay = obj.getLong("dateEpochDay"),
                        startMinutes = obj.getInt("startMinutes"),
                        endMinutes = obj.getInt("endMinutes"),
                        durationMinutes = obj.getInt("durationMinutes"),
                        categoryId = obj.getLong("categoryId"),
                        batchId = if (obj.has("batchId")) obj.getLong("batchId") else null,
                        categoryNameSnapshot = obj.getString("categoryNameSnapshot"),
                        batchNameSnapshot = if (obj.has("batchNameSnapshot")) obj.getString("batchNameSnapshot") else null,
                        isTeachingSnapshot = obj.optBoolean("isTeachingSnapshot", true),
                        note = obj.optString("note", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            // Parse Settings
            val settingsObj = root.optJSONObject("settings")

            // Atomic database restore
            database.withTransaction {
                database.workSessionDao().deleteAll()
                database.timeSlotDao().deleteAll()
                database.batchDao().deleteAll()
                database.categoryDao().deleteAll()

                database.categoryDao().insertAll(categories)
                database.batchDao().insertAll(batches)
                database.timeSlotDao().insertAll(slots)
                database.workSessionDao().insertAll(sessions)
            }

            // Restore settings
            if (settingsObj != null) {
                if (settingsObj.has("use24HourFormat")) {
                    settingsDataStore.setUse24HourFormat(settingsObj.getBoolean("use24HourFormat"))
                }
                if (settingsObj.has("teacherName")) {
                    settingsDataStore.setTeacherName(settingsObj.getString("teacherName"))
                }
                if (settingsObj.has("institutionName")) {
                    settingsDataStore.setInstitutionName(settingsObj.getString("institutionName"))
                }
                if (settingsObj.has("isAppLockEnabled")) {
                    settingsDataStore.setAppLockEnabled(settingsObj.getBoolean("isAppLockEnabled"))
                }
                if (settingsObj.has("quickDurations")) {
                    val arr = settingsObj.getJSONArray("quickDurations")
                    val list = mutableListOf<Int>()
                    for (k in 0 until arr.length()) {
                        list.add(arr.getInt(k))
                    }
                    if (list.isNotEmpty()) {
                        settingsDataStore.setQuickDurations(list)
                    }
                }
            }

            Result.success(
                RestoreResult(
                    sessionCount = sessions.size,
                    batchCount = batches.size,
                    categoryCount = categories.size,
                    slotCount = slots.size
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
