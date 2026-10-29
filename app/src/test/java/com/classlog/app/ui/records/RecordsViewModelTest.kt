package com.classlog.app.ui.records

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
class RecordsViewModelTest {
    private lateinit var db: AppDatabase
    private lateinit var configRepository: ConfigRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var viewModel: RecordsViewModel

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
        viewModel = RecordsViewModel(sessionRepository, configRepository)

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
        val initial = YearMonth.of(2026, 10)
        viewModel.setMonth(initial)
        val s1 = viewModel.uiState.first { it.currentMonth == initial }
        assertEquals(initial, s1.currentMonth)

        viewModel.previousMonth()
        val s2 = viewModel.uiState.first { it.currentMonth == YearMonth.of(2026, 9) }
        assertEquals(YearMonth.of(2026, 9), s2.currentMonth)

        viewModel.nextMonth()
        val s3 = viewModel.uiState.first { it.currentMonth == initial }
        assertEquals(initial, s3.currentMonth)
        job.cancel()
    }

    @Test
    fun recordsGroupedByDayWithTotals() = runBlocking {
        val job = launch { viewModel.uiState.collect {} }
        val oct14 = LocalDate.of(2026, 10, 14).toEpochDay()
        val oct13 = LocalDate.of(2026, 10, 13).toEpochDay()

        viewModel.setMonth(YearMonth.of(2026, 10))

        // Insert 2 sessions on Oct 14: 60m and 120m
        sessionRepository.insertSession(oct14, 600, 660, teachingCategory, batchKids, "Lesson 1")
        sessionRepository.insertSession(oct14, 660, 780, teachingCategory, batchXII, "Lesson 2")
        // Insert 1 session on Oct 13: 30m
        sessionRepository.insertSession(oct13, 840, 870, officeCategory, null, "Paperwork")

        val state = viewModel.uiState.first { it.dayGroups.size == 2 }
        assertEquals(2, state.dayGroups.size)
        assertEquals(210, state.totalMonthMinutes)
        assertEquals(3, state.totalSessionsCount)
        assertEquals(2, state.daysWorkedCount)

        val day14 = state.dayGroups.find { it.dateEpochDay == oct14 }!!
        assertEquals(180, day14.totalMinutes)
        assertEquals(2, day14.sessions.size)

        val day13 = state.dayGroups.find { it.dateEpochDay == oct13 }!!
        assertEquals(30, day13.totalMinutes)
        assertEquals(1, day13.sessions.size)
        job.cancel()
    }

    @Test
    fun searchAndFilterRecords() = runBlocking {
        val job = launch { viewModel.uiState.collect {} }
        val oct14 = LocalDate.of(2026, 10, 14).toEpochDay()
        viewModel.setMonth(YearMonth.of(2026, 10))

        sessionRepository.insertSession(oct14, 600, 660, teachingCategory, batchKids, "Algebra exam")
        sessionRepository.insertSession(oct14, 700, 760, officeCategory, null, "Admission call")

        // Wait for initial data
        viewModel.uiState.first { it.dayGroups.isNotEmpty() }

        // Filter by Teaching category
        viewModel.setFilter(categoryId = teachingCategory.id, batchId = null)
        val filteredState = viewModel.uiState.first { it.filter.categoryId == teachingCategory.id && it.totalSessionsCount == 1 }
        assertEquals(1, filteredState.totalSessionsCount)
        assertEquals("Algebra exam", filteredState.dayGroups[0].sessions[0].note)

        // Reset filter, search by keyword "Admission"
        viewModel.clearFilter()
        viewModel.setSearchQuery("Admission")
        val searchState = viewModel.uiState.first { state ->
            state.searchQuery == "Admission" &&
            state.totalSessionsCount == 1 &&
            state.dayGroups.firstOrNull()?.sessions?.firstOrNull()?.note == "Admission call"
        }
        assertEquals(1, searchState.totalSessionsCount)
        assertEquals("Admission call", searchState.dayGroups[0].sessions[0].note)
        job.cancel()
    }
}
