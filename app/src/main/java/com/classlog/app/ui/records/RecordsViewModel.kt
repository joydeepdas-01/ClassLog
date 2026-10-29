package com.classlog.app.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.classlog.app.data.datastore.AppSettings
import com.classlog.app.data.local.entity.BatchEntity
import com.classlog.app.data.local.entity.CategoryEntity
import com.classlog.app.data.local.entity.TimeSlotEntity
import com.classlog.app.data.local.entity.WorkSessionEntity
import com.classlog.app.data.repository.ConfigRepository
import com.classlog.app.data.repository.SessionRepository
import com.classlog.app.domain.validation.SessionValidator
import com.classlog.app.util.monthRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * A group of work sessions belonging to a specific day, with aggregated duration.
 */
data class DayGroup(
    val dateEpochDay: Long,
    val date: LocalDate,
    val totalMinutes: Int,
    val sessions: List<WorkSessionEntity>
)

data class RecordsFilter(
    val categoryId: Long? = null,
    val batchId: Long? = null,
    val customDateRange: Pair<Long, Long>? = null
)

data class RecordsUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val searchQuery: String = "",
    val filter: RecordsFilter = RecordsFilter(),
    val dayGroups: List<DayGroup> = emptyList(),
    val totalMonthMinutes: Int = 0,
    val totalSessionsCount: Int = 0,
    val daysWorkedCount: Int = 0,
    val categories: List<CategoryEntity> = emptyList(),
    val batches: List<BatchEntity> = emptyList(),
    val timeSlots: List<TimeSlotEntity> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val editingSession: WorkSessionEntity? = null,
    val showFilterSheet: Boolean = false,
    val isSearchActive: Boolean = false
)

sealed interface RecordsUiEvent {
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null
    ) : RecordsUiEvent
}

class RecordsViewModel(
    private val sessionRepository: SessionRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _searchQuery = MutableStateFlow("")
    private val _filter = MutableStateFlow(RecordsFilter())
    private val _editingSession = MutableStateFlow<WorkSessionEntity?>(null)
    private val _showFilterSheet = MutableStateFlow(false)
    private val _isSearchActive = MutableStateFlow(false)
    private var lastDeletedSession: WorkSessionEntity? = null

    private val _events = MutableSharedFlow<RecordsUiEvent>()
    val events: SharedFlow<RecordsUiEvent> = _events.asSharedFlow()

    private data class FilterQueryParam(
        val month: YearMonth,
        val query: String,
        val filter: RecordsFilter
    )

    private val queryParamFlow = combine(_currentMonth, _searchQuery, _filter) { month, query, filter ->
        FilterQueryParam(month, query, filter)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val filteredSessionsFlow = queryParamFlow.flatMapLatest { param ->
        val range = param.filter.customDateRange ?: monthRange(param.month)
        sessionRepository.observeFiltered(
            fromEpochDay = range.first,
            toEpochDay = range.second,
            categoryId = param.filter.categoryId,
            batchId = param.filter.batchId,
            query = param.query
        )
    }

    private data class ConfigBundle(
        val categories: List<CategoryEntity>,
        val batches: List<BatchEntity>,
        val slots: List<TimeSlotEntity>,
        val settings: AppSettings
    )

    private val configFlow = combine(
        configRepository.observeActiveCategories(),
        configRepository.observeActiveBatches(),
        configRepository.observeActiveTimeSlots(),
        configRepository.settings
    ) { categories, batches, slots, settings ->
        ConfigBundle(categories, batches, slots, settings)
    }

    private data class UiSheetState(
        val editingSession: WorkSessionEntity?,
        val showFilterSheet: Boolean,
        val isSearchActive: Boolean
    )

    private val uiSheetStateFlow = combine(_editingSession, _showFilterSheet, _isSearchActive) { editing, showFilter, isSearch ->
        UiSheetState(editing, showFilter, isSearch)
    }

    val uiState: StateFlow<RecordsUiState> = combine(
        queryParamFlow,
        filteredSessionsFlow,
        configFlow,
        uiSheetStateFlow
    ) { param, sessions, config, sheetState ->
        // Group sessions by day (ordered descending by date)
        val groups = sessions.groupBy { it.dateEpochDay }
            .map { (epochDay, daySessions) ->
                DayGroup(
                    dateEpochDay = epochDay,
                    date = LocalDate.ofEpochDay(epochDay),
                    totalMinutes = daySessions.sumOf { it.durationMinutes },
                    sessions = daySessions.sortedBy { it.startMinutes }
                )
            }
            .sortedByDescending { it.dateEpochDay }

        val totalMinutes = sessions.sumOf { it.durationMinutes }
        val daysWorked = groups.size

        RecordsUiState(
            currentMonth = param.month,
            searchQuery = param.query,
            filter = param.filter,
            dayGroups = groups,
            totalMonthMinutes = totalMinutes,
            totalSessionsCount = sessions.size,
            daysWorkedCount = daysWorked,
            categories = config.categories,
            batches = config.batches,
            timeSlots = config.slots,
            settings = config.settings,
            editingSession = sheetState.editingSession,
            showFilterSheet = sheetState.showFilterSheet,
            isSearchActive = sheetState.isSearchActive
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecordsUiState()
    )

    // --- Month Navigation ---
    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun setMonth(yearMonth: YearMonth) {
        _currentMonth.value = yearMonth
    }

    // --- Search & Filter ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    fun setShowFilterSheet(show: Boolean) {
        _showFilterSheet.value = show
    }

    fun setFilter(categoryId: Long?, batchId: Long?, customRange: Pair<Long, Long>? = null) {
        _filter.value = RecordsFilter(
            categoryId = categoryId,
            batchId = batchId,
            customDateRange = customRange
        )
        _showFilterSheet.value = false
    }

    fun clearFilter() {
        _filter.value = RecordsFilter()
    }

    // --- Session Editing ---
    fun openEditSession(session: WorkSessionEntity) {
        _editingSession.value = session
    }

    fun closeEditSession() {
        _editingSession.value = null
    }

    fun updateSession(
        session: WorkSessionEntity,
        newStart: Int,
        newEnd: Int,
        newCategory: CategoryEntity,
        newBatch: BatchEntity?,
        newNote: String
    ) {
        val issues = SessionValidator.validate(
            start = newStart,
            end = newEnd,
            isTeaching = newCategory.isTeaching,
            batchId = newBatch?.id
        )
        if (issues.isNotEmpty()) return

        viewModelScope.launch {
            val updated = session.copy(
                startMinutes = newStart,
                endMinutes = newEnd,
                durationMinutes = newEnd - newStart,
                categoryId = newCategory.id,
                batchId = newBatch?.id,
                categoryNameSnapshot = newCategory.name,
                batchNameSnapshot = newBatch?.name,
                isTeachingSnapshot = newCategory.isTeaching,
                note = newNote.trim(),
                updatedAt = System.currentTimeMillis()
            )
            sessionRepository.updateSession(updated)
            _editingSession.value = null
            _events.emit(RecordsUiEvent.ShowSnackbar("Record updated"))
        }
    }

    fun duplicateSession(session: WorkSessionEntity) {
        viewModelScope.launch {
            sessionRepository.duplicateSession(session, session.dateEpochDay)
            _events.emit(RecordsUiEvent.ShowSnackbar("Session duplicated"))
        }
    }

    fun deleteSession(session: WorkSessionEntity) {
        viewModelScope.launch {
            lastDeletedSession = session
            sessionRepository.deleteSession(session.id)
            _events.emit(
                RecordsUiEvent.ShowSnackbar(
                    message = "Session deleted",
                    actionLabel = "UNDO",
                    onAction = { undoDelete() }
                )
            )
        }
    }

    fun undoDelete() {
        val toRestore = lastDeletedSession ?: return
        viewModelScope.launch {
            sessionRepository.duplicateSession(toRestore, toRestore.dateEpochDay)
            lastDeletedSession = null
            _events.emit(RecordsUiEvent.ShowSnackbar("Session restored"))
        }
    }

    companion object {
        fun provideFactory(
            sessionRepository: SessionRepository,
            configRepository: ConfigRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RecordsViewModel(sessionRepository, configRepository) as T
                }
            }
    }
}
