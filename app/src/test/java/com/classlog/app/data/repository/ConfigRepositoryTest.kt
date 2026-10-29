package com.classlog.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.classlog.app.data.datastore.SettingsDataStore
import com.classlog.app.data.local.AppDatabase
import com.classlog.app.data.local.entity.BatchEntity
import com.classlog.app.data.local.entity.CategoryEntity
import com.classlog.app.data.local.entity.TimeSlotEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfigRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var repository: ConfigRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settingsDataStore = SettingsDataStore(context)
        repository = ConfigRepository(
            batchDao = db.batchDao(),
            categoryDao = db.categoryDao(),
            timeSlotDao = db.timeSlotDao(),
            settingsDataStore = settingsDataStore
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun saveAndToggleBatch() = runBlocking {
        val id = repository.saveBatch("Class 10", 0)
        assertTrue(id > 0)

        val activeList = repository.observeActiveBatches().first()
        assertEquals(1, activeList.size)
        assertEquals("Class 10", activeList[0].name)

        // Soft delete (hide)
        repository.toggleBatchActive(activeList[0])
        val activeAfterHide = repository.observeActiveBatches().first()
        assertTrue(activeAfterHide.isEmpty())

        val allAfterHide = repository.observeAllBatches().first()
        assertEquals(1, allAfterHide.size)
        assertFalse(allAfterHide[0].isActive)
    }

    @Test
    fun saveAndRenameCategory() = runBlocking {
        val id = repository.saveCategory("Online Tutoring", isTeaching = true, sortOrder = 0)
        assertTrue(id > 0)

        val categories = repository.observeActiveCategories().first()
        assertEquals(1, categories.size)
        assertTrue(categories[0].isTeaching)

        repository.updateCategory(categories[0].copy(name = "Online Coaching", isTeaching = false))
        val updated = repository.observeActiveCategories().first()
        assertEquals("Online Coaching", updated[0].name)
        assertFalse(updated[0].isTeaching)
    }

    @Test
    fun saveAndValidateTimeSlot() = runBlocking {
        val invalidId = repository.saveTimeSlot("Invalid", 600, 500, 0)
        assertEquals(-1L, invalidId)

        val validId = repository.saveTimeSlot("Morning", 540, 660, 0)
        assertTrue(validId > 0)

        val slots = repository.observeActiveTimeSlots().first()
        assertEquals(1, slots.size)
        assertEquals(540, slots[0].startMinutes)
        assertEquals(660, slots[0].endMinutes)
    }

    @Test
    fun updateSettingsPreferences() = runBlocking {
        repository.setUse24HourFormat(true)
        repository.setTeacherName("Prof. Sharma")
        repository.setQuickDurations(listOf(45, 90, 120))

        val settings = repository.settings.first()
        assertTrue(settings.use24HourFormat)
        assertEquals("Prof. Sharma", settings.teacherName)
        assertEquals(listOf(45, 90, 120), settings.quickDurationMinutes)
    }
}
