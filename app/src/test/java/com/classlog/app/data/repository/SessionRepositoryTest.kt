package com.classlog.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.classlog.app.data.local.AppDatabase
import com.classlog.app.data.local.entity.BatchEntity
import com.classlog.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var sessionRepository: SessionRepository

    private val teachingCategory = CategoryEntity(id = 1, name = "Teaching", isTeaching = true)
    private val officeCategory = CategoryEntity(id = 2, name = "Office Work", isTeaching = false)
    private val batchKids = BatchEntity(id = 1, name = "Kids")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionRepository = SessionRepository(db.workSessionDao())

        runBlocking {
            db.categoryDao().insert(teachingCategory)
            db.categoryDao().insert(officeCategory)
            db.batchDao().insert(batchKids)
        }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertSession_valid_createsWithSnapshots() = runBlocking {
        val result = sessionRepository.insertSession(
            dateEpochDay = 20000,
            startMinutes = 600,
            endMinutes = 660,
            category = teachingCategory,
            batch = batchKids,
            note = "Chapter 4 Test"
        )
        assertTrue(result.isSuccess)
        val id = result.getOrThrow()

        val saved = sessionRepository.getById(id)
        assertNotNull(saved)
        assertEquals(60, saved!!.durationMinutes)
        assertEquals("Teaching", saved.categoryNameSnapshot)
        assertEquals("Kids", saved.batchNameSnapshot)
        assertTrue(saved.isTeachingSnapshot)
        assertEquals("Chapter 4 Test", saved.note)
    }

    @Test
    fun insertSession_teachingWithoutBatch_fails() = runBlocking {
        val result = sessionRepository.insertSession(
            dateEpochDay = 20000,
            startMinutes = 600,
            endMinutes = 660,
            category = teachingCategory,
            batch = null,
            note = ""
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun findOverlaps_findsCorrectOverlap() = runBlocking {
        sessionRepository.insertSession(
            dateEpochDay = 20000,
            startMinutes = 600,
            endMinutes = 660,
            category = officeCategory,
            batch = null,
            note = ""
        )

        val overlaps = sessionRepository.findOverlaps(
            day = 20000,
            start = 630,
            end = 720
        )
        assertEquals(1, overlaps.size)
        assertEquals("Office Work", overlaps[0].categoryNameSnapshot)

        // Exclude own ID
        val overlapsExcluded = sessionRepository.findOverlaps(
            day = 20000,
            start = 630,
            end = 720,
            excludeId = overlaps[0].id
        )
        assertTrue(overlapsExcluded.isEmpty())
    }

    @Test
    fun duplicateSession_copiesWithTargetDate() = runBlocking {
        val insertResult = sessionRepository.insertSession(
            dateEpochDay = 20000,
            startMinutes = 600,
            endMinutes = 660,
            category = officeCategory,
            batch = null,
            note = "Maintenance"
        )
        val origId = insertResult.getOrThrow()
        val original = sessionRepository.getById(origId)!!

        val dupId = sessionRepository.duplicateSession(original, targetDateEpochDay = 20001)
        assertTrue(dupId > 0)

        val duplicated = sessionRepository.getById(dupId)!!
        assertEquals(20001, duplicated.dateEpochDay)
        assertEquals(original.startMinutes, duplicated.startMinutes)
        assertEquals(original.durationMinutes, duplicated.durationMinutes)
    }

    @Test
    fun deleteSession_removesSession() = runBlocking {
        val insertResult = sessionRepository.insertSession(
            dateEpochDay = 20000,
            startMinutes = 600,
            endMinutes = 660,
            category = officeCategory,
            batch = null,
            note = ""
        )
        val id = insertResult.getOrThrow()

        sessionRepository.deleteSession(id)
        val list = sessionRepository.observeDay(20000).first()
        assertTrue(list.isEmpty())
    }
}
