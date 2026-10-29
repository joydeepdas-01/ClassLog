package com.classlog.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.classlog.app.data.local.entity.CategoryEntity
import com.classlog.app.data.local.entity.WorkSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkSessionDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        runBlocking {
            db.categoryDao().insert(CategoryEntity(id = 1, name = "Teaching", isTeaching = true))
        }
    }

    @After
    fun tearDown() = db.close()

    private fun session(start: Int, end: Int) = WorkSessionEntity(
        dateEpochDay = 20000,
        startMinutes = start,
        endMinutes = end,
        durationMinutes = end - start,
        categoryId = 1,
        categoryNameSnapshot = "Teaching",
        isTeachingSnapshot = true,
        createdAt = 0,
        updatedAt = 0
    )

    @Test
    fun overlapDetected() = runBlocking {
        db.workSessionDao().insert(session(600, 660))
        assertEquals(1, db.workSessionDao().findOverlaps(20000, 630, 700, 0).size)
    }

    @Test
    fun backToBackIsNotOverlap() = runBlocking {
        db.workSessionDao().insert(session(600, 660))
        assertTrue(db.workSessionDao().findOverlaps(20000, 660, 720, 0).isEmpty())
    }

    @Test
    fun editingSelfIsNotOverlap() = runBlocking {
        val id = db.workSessionDao().insert(session(600, 660))
        assertTrue(db.workSessionDao().findOverlaps(20000, 600, 660, id).isEmpty())
    }

    @Test
    fun monthRangeQuery() = runBlocking {
        db.workSessionDao().insert(session(600, 660))
        val list = db.workSessionDao().observeRange(19990, 20010).first()
        assertEquals(1, list.size)
    }
}
