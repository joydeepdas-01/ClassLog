package com.classlog.app.data.export

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.classlog.app.data.datastore.SettingsDataStore
import com.classlog.app.data.local.AppDatabase
import com.classlog.app.data.local.entity.BatchEntity
import com.classlog.app.data.local.entity.CategoryEntity
import com.classlog.app.data.local.entity.TimeSlotEntity
import com.classlog.app.data.local.entity.WorkSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRestoreManagerTest {

    private lateinit var db: AppDatabase
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var manager: BackupRestoreManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settingsDataStore = SettingsDataStore(context)
        manager = BackupRestoreManager(db, settingsDataStore)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun createBackupJson_containsAllEntitiesAndSettings() = runBlocking {
        // Prepare data
        db.batchDao().insert(BatchEntity(id = 1, name = "Physics JEE", isActive = true, sortOrder = 0))
        db.categoryDao().insert(CategoryEntity(id = 1, name = "Doubt Session", isTeaching = true, isActive = true, sortOrder = 0))
        db.timeSlotDao().insert(TimeSlotEntity(id = 1, label = "Slot 1", startMinutes = 600, endMinutes = 690, isActive = true, sortOrder = 0))
        db.workSessionDao().insert(
            WorkSessionEntity(
                id = 1,
                dateEpochDay = 20741,
                startMinutes = 600,
                endMinutes = 690,
                durationMinutes = 90,
                categoryId = 1,
                batchId = 1,
                categoryNameSnapshot = "Doubt Session",
                batchNameSnapshot = "Physics JEE",
                isTeachingSnapshot = true,
                note = "Electrostatics",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )
        settingsDataStore.setTeacherName("Dr. Roy")
        settingsDataStore.setInstitutionName("Apex Institute")
        settingsDataStore.setUse24HourFormat(true)

        // Generate JSON
        val jsonString = manager.createBackupJson()
        val json = JSONObject(jsonString)

        assertEquals(1, json.getInt("version"))
        assertTrue(json.has("exportedAt"))

        // Check settings
        val settingsObj = json.getJSONObject("settings")
        assertEquals("Dr. Roy", settingsObj.getString("teacherName"))
        assertEquals("Apex Institute", settingsObj.getString("institutionName"))
        assertTrue(settingsObj.getBoolean("use24HourFormat"))

        // Check entities array lengths
        assertEquals(1, json.getJSONArray("batches").length())
        assertEquals(1, json.getJSONArray("categories").length())
        assertEquals(1, json.getJSONArray("timeSlots").length())
        assertEquals(1, json.getJSONArray("workSessions").length())

        val sessionObj = json.getJSONArray("workSessions").getJSONObject(0)
        assertEquals("Doubt Session", sessionObj.getString("categoryNameSnapshot"))
        assertEquals("Physics JEE", sessionObj.getString("batchNameSnapshot"))
        assertEquals("Electrostatics", sessionObj.getString("note"))
    }

    @Test
    fun restoreFromJson_atomicallyRestoresDataAndSettings() = runBlocking {
        val backupJson = """
        {
            "version": 1,
            "exportedAt": 1700000000000,
            "settings": {
                "use24HourFormat": false,
                "teacherName": "Prof. Verma",
                "institutionName": "Allen Career Institute",
                "isAppLockEnabled": true,
                "quickDurations": [45, 90]
            },
            "batches": [
                { "id": 10, "name": "Class 11 Science", "isActive": true, "sortOrder": 1 }
            ],
            "categories": [
                { "id": 20, "name": "Lab Practical", "isTeaching": true, "isActive": true, "sortOrder": 1 }
            ],
            "timeSlots": [
                { "id": 30, "label": "Evening", "startMinutes": 960, "endMinutes": 1080, "isActive": true, "sortOrder": 0 }
            ],
            "workSessions": [
                {
                    "id": 100,
                    "dateEpochDay": 20745,
                    "startMinutes": 960,
                    "endMinutes": 1080,
                    "durationMinutes": 120,
                    "categoryId": 20,
                    "batchId": 10,
                    "categoryNameSnapshot": "Lab Practical",
                    "batchNameSnapshot": "Class 11 Science",
                    "isTeachingSnapshot": true,
                    "note": "Optics experiment",
                    "createdAt": 1000,
                    "updatedAt": 1000
                }
            ]
        }
        """.trimIndent()

        // Populate dirty data before restore
        db.batchDao().insert(BatchEntity(id = 999, name = "Old Class", isActive = false, sortOrder = 0))

        val result = manager.restoreFromJson(backupJson)
        assertTrue(result.isSuccess)
        val restoreResult = result.getOrThrow()

        assertEquals(1, restoreResult.batchCount)
        assertEquals(1, restoreResult.categoryCount)
        assertEquals(1, restoreResult.slotCount)
        assertEquals(1, restoreResult.sessionCount)

        // Verify old batch was purged
        val batches = db.batchDao().getAll()
        assertEquals(1, batches.size)
        assertEquals("Class 11 Science", batches[0].name)

        // Verify session was restored
        val sessions = db.workSessionDao().getAll()
        assertEquals(1, sessions.size)
        assertEquals("Optics experiment", sessions[0].note)

        // Verify settings were restored
        val settings = settingsDataStore.settingsFlow.first()
        assertEquals("Prof. Verma", settings.teacherName)
        assertEquals("Allen Career Institute", settings.institutionName)
        assertFalse(settings.use24HourFormat)
        assertTrue(settings.isAppLockEnabled)
        assertEquals(listOf(45, 90), settings.quickDurationMinutes)
    }

    @Test
    fun restoreFromJson_invalidVersion_returnsFailure() = runBlocking {
        val invalidJson = """{ "version": 99 }"""
        val result = manager.restoreFromJson(invalidJson)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Unsupported backup version") == true)
    }
}
