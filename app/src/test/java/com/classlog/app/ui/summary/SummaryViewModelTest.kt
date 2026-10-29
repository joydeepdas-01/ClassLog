package com.classlog.app.ui.summary

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.classlog.app.data.datastore.SettingsDataStore
import com.classlog.app.data.local.AppDatabase
import com.classlog.app.data.local.entity.BatchEntity
import com.classlog.app.data.local.entity.CategoryEntity
import com.classlog.app.data.repository.ConfigRepository
import com.classlog.app.data.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SummaryViewModelTest {
    private lateinit var db: AppDatabase
    private lateinit var configRepository: ConfigRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var viewModel: SummaryViewModel

    private val teachingCategory = CategoryEntity(id = 1, name = "Teaching", isTeaching = true)
    private val officeCategory = CategoryEntity(id = 2, name = "Office Work", isTeaching = false)
    private val batchKids = BatchEntity(id = 1, name = "Kids")
    private val batchXII = BatchEntity(id = 2, name = "XII")

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val settingsDataStore = SettingsDataStore(context)
        configRepository = ConfigRepository(
            batchDao = db.batchDao(),
            categoryDao = db.categoryDao(),
            timeSlotDao = db.timeSlotDao(),
            settingsDataStore = settingsDataStore
        )
        sessionRepository = SessionRepository(db.workSessionDao())
        viewModel = SummaryViewModel(sessionRepository, configRepository)

        runBlocking {
            db.categoryDao().insert(teachingCategory)
            db.categoryDao().insert(officeCategory)
            db.batchDao().insert(batchKids)
            db.batchDao().insert(batchXII)
        }
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun monthNavigation_updatesCurrentMonth() = runBlocking {
        val job = launch { viewModel.uiState.collect {} }
        val target = YearMonth.of(2026, 10)
        viewModel.setMonth(target)
        val s1 = viewModel.uiState.first { it.currentMonth == target }
        assertEquals(target, s1.currentMonth)

        viewModel.previousMonth()
        val s2 = viewModel.uiState.first { it.currentMonth == YearMonth.of(2026, 9) }
        assertEquals(YearMonth.of(2026, 9), s2.currentMonth)

        viewModel.nextMonth()
        val s3 = viewModel.uiState.first { it.currentMonth == target }
        assertEquals(target, s3.currentMonth)
        job.cancel()
    }

    @Test
    fun monthlyTotals_and_breakdownsCalculated() = runBlocking {
        val job = launch { viewModel.uiState.collect {} }
        val target = YearMonth.of(2026, 10)
        viewModel.setMonth(target)

        val oct14 = LocalDate.of(2026, 10, 14).toEpochDay()
        val oct15 = LocalDate.of(2026, 10, 15).toEpochDay()

        // 1hr Teaching Kids
        sessionRepository.insertSession(oct14, 600, 660, teachingCategory, batchKids, "Class 1")
        // 2hr Teaching XII
        sessionRepository.insertSession(oct14, 660, 780, teachingCategory, batchXII, "Class 2")
        // 30m Office Work
        sessionRepository.insertSession(oct15, 840, 870, officeCategory, null, "Paperwork")

        val state = viewModel.uiState.first { it.summary.sessionCount == 3 }
        assertEquals(210, state.summary.totalMinutes)
        assertEquals(3, state.summary.sessionCount)
        assertEquals(2, state.summary.daysWorked)
        assertEquals(180, state.summary.teachingMinutes)
        assertEquals(30, state.summary.nonTeachingMinutes)

        // byBatch
        assertEquals(60, state.summary.byBatch["Kids"])
        assertEquals(120, state.summary.byBatch["XII"])

        // byCategory
        assertEquals(180, state.summary.byCategory["Teaching"])
        assertEquals(30, state.summary.byCategory["Office Work"])

        // averages
        assertEquals(105, state.averageMinutesPerWorkedDay) // 210m / 2 days = 105m
        assertEquals(180f / 210f, state.teachingRatio, 0.001f)

        job.cancel()
    }

    @Test
    fun dailyActivities_coversAllDaysInMonth() = runBlocking {
        val job = launch { viewModel.uiState.collect {} }
        val target = YearMonth.of(2026, 10)
        viewModel.setMonth(target)

        val oct14 = LocalDate.of(2026, 10, 14).toEpochDay()
        sessionRepository.insertSession(oct14, 600, 720, teachingCategory, batchKids, "Class")

        val state = viewModel.uiState.first { it.summary.sessionCount == 1 }
        // October has 31 days
        assertEquals(31, state.dailyActivities.size)

        val day14 = state.dailyActivities.find { it.dayOfMonth == 14 }!!
        assertEquals(120, day14.minutes)
        assertEquals(1, day14.sessionCount)

        val day1 = state.dailyActivities.find { it.dayOfMonth == 1 }!!
        assertEquals(0, day1.minutes)
        assertEquals(0, day1.sessionCount)

        assertEquals(120, state.maxDailyMinutes)
        job.cancel()
    }
}
