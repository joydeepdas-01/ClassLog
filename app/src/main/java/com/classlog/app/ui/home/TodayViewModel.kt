package com.classlog.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.classlog.app.data.datastore.AppSettings
import com.classlog.app.data.local.entity.BatchEntity
import com.classlog.app.data.local.entity.CategoryEntity
import com.classlog.app.data.local.entity.TimeSlotEntity
import com.classlog.app.data.local.entity.WorkSessionEntity
import com.classlog.app.data.local.model.RecentCombo
import com.classlog.app.data.repository.ConfigRepository
import com.classlog.app.data.repository.SessionRepository
import com.classlog.app.domain.model.Issue
import com.classlog.app.domain.validation.SessionValidator
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayFormState(
    val selectedCategory: CategoryEntity? = null,
    val selectedBatch: BatchEntity? = null,
    val startMinutes: Int = 600, // 10:00 AM
    val endMinutes: Int = 660,   // 11:00 AM
    val note: String = "",
    val editingSessionId: Long? = null,
    val overlapSession: WorkSessionEntity? = null
)

data class TodayUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val categories: List<CategoryEntity> = emptyList(),
    val batches: List<BatchEntity> = emptyList(),
    val timeSlots: List<TimeSlotEntity> = emptyList(),
    val recentCombos: List<RecentCombo> = emptyList(),
    val sessions: List<WorkSessionEntity> = emptyList(),
    val selectedCategory: CategoryEntity? = null,
    val selectedBatch: BatchEntity? = null,
    val startMinutes: Int = 600, // 10:00 AM
    val endMinutes: Int = 660,   // 11:00 AM
    val note: String = "",
    val editingSessionId: Long? = null,
    val overlapSession: WorkSessionEntity? = null,
    val validationIssues: List<Issue> = emptyList(),
    val settings: AppSettings = AppSettings()
)

sealed interface TodayUiEvent {
    data class ShowSnackbar(val message: String, val actionLabel: String? = null, val onAction: (() -> Unit)? = null) : TodayUiEvent
}

class TodayViewModel(
    private val sessionRepository: SessionRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _formState = MutableStateFlow(TodayFormState())
    private var lastDeletedSession: WorkSessionEntity? = null

    private val _events = MutableSharedFlow<TodayUiEvent>()
    val events: SharedFlow<TodayUiEvent> = _events.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val sessionsForDay = _selectedDate.flatMapLatest { date ->
        sessionRepository.observeDay(date.toEpochDay())
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

    val uiState: StateFlow<TodayUiState> = combine(
        _selectedDate,
        sessionsForDay,
        sessionRepository.observeRecentCombos(),
        configFlow,
        _formState
    ) { date, sessions, combos, config, form ->
        val categories = config.categories
        val batches = config.batches
        val slots = config.slots
        val settings = config.settings

        // Auto-select first category if none selected
        val currentCategory = form.selectedCategory ?: categories.firstOrNull()

        val issues = SessionValidator.validate(
            start = form.startMinutes,
            end = form.endMinutes,
            isTeaching = currentCategory?.isTeaching ?: false,
            batchId = form.selectedBatch?.id
        )

        TodayUiState(
            selectedDate = date,
            categories = categories,
            batches = batches,
            timeSlots = slots,
            recentCombos = combos,
            sessions = sessions,
            selectedCategory = currentCategory,
            selectedBatch = form.selectedBatch,
            startMinutes = form.startMinutes,
            endMinutes = form.endMinutes,
            note = form.note,
            editingSessionId = form.editingSessionId,
            overlapSession = form.overlapSession,
            validationIssues = issues,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayUiState()
    )

    init {
        checkOverlap()
    }

    // --- Date Navigation ---
    fun previousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
        checkOverlap()
    }

    fun nextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
        checkOverlap()
    }

    fun setToday() {
        _selectedDate.value = LocalDate.now()
        checkOverlap()
    }

    fun setDate(date: LocalDate) {
        _selectedDate.value = date
        checkOverlap()
    }

    // --- Selection & Form Inputs ---
    fun selectCategory(category: CategoryEntity) {
        _formState.update { current ->
            current.copy(
                selectedCategory = category,
                selectedBatch = if (!category.isTeaching) null else current.selectedBatch
            )
        }
        checkOverlap()
    }

    fun selectBatch(batch: BatchEntity) {
        _formState.update { it.copy(selectedBatch = batch) }
    }

    fun setTimeSlot(slot: TimeSlotEntity) {
        _formState.update { it.copy(startMinutes = slot.startMinutes, endMinutes = slot.endMinutes) }
        checkOverlap()
    }

    fun setFromMinutes(minutes: Int) {
        val safeStart = minutes.coerceIn(0, 1439)
        _formState.update { current ->
            val safeEnd = if (current.endMinutes <= safeStart) {
                (safeStart + 60).coerceAtMost(1440)
            } else {
                current.endMinutes
            }
            current.copy(startMinutes = safeStart, endMinutes = safeEnd)
        }
        checkOverlap()
    }

    fun setToMinutes(minutes: Int) {
        val safeEnd = minutes.coerceIn(5, 1440)
        _formState.update { current ->
            val safeStart = if (current.startMinutes >= safeEnd) {
                (safeEnd - 60).coerceAtLeast(0)
            } else {
                current.startMinutes
            }
            current.copy(startMinutes = safeStart, endMinutes = safeEnd)
        }
        checkOverlap()
    }

    fun setQuickDuration(durationMinutes: Int) {
        _formState.update { current ->
            val safeDur = durationMinutes.coerceAtLeast(5)
            val newEnd = (current.startMinutes + safeDur).coerceAtMost(1440)
            current.copy(endMinutes = newEnd)
        }
        checkOverlap()
    }

    fun setNote(text: String) {
        _formState.update { it.copy(note = text) }
    }

    fun selectQuickRepeat(combo: RecentCombo) {
        val category = uiState.value.categories.find { it.id == combo.categoryId }
        val batch = combo.batchId?.let { bId -> uiState.value.batches.find { it.id == bId } }

        if (category != null) {
            _formState.update { current ->
                current.copy(
                    selectedCategory = category,
                    selectedBatch = batch,
                    startMinutes = combo.startMinutes,
                    endMinutes = combo.endMinutes
                )
            }
            checkOverlap()
        }
    }

    private fun checkOverlap() {
        // Prevent false overlap warnings
        _formState.update { it.copy(overlapSession = null) }
    }

    fun showValidationError(message: String) {
        viewModelScope.launch {
            _events.emit(TodayUiEvent.ShowSnackbar(message))
        }
    }

    // --- Batch & Category Management ---
    fun createBatch(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val newId = configRepository.saveBatch(trimmed)
            if (newId > 0) {
                val newBatch = BatchEntity(id = newId, name = trimmed, isActive = true)
                _formState.update { it.copy(selectedBatch = newBatch) }
                _events.emit(TodayUiEvent.ShowSnackbar("Batch '$trimmed' added"))
            }
        }
    }

    fun updateBatch(batch: BatchEntity, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val updated = batch.copy(name = trimmed)
            configRepository.updateBatch(updated)
            if (_formState.value.selectedBatch?.id == batch.id) {
                _formState.update { it.copy(selectedBatch = updated) }
            }
            _events.emit(TodayUiEvent.ShowSnackbar("Batch updated"))
        }
    }

    fun deleteBatch(batch: BatchEntity) {
        viewModelScope.launch {
            configRepository.deleteBatch(batch)
            if (_formState.value.selectedBatch?.id == batch.id) {
                _formState.update { it.copy(selectedBatch = null) }
            }
            _events.emit(TodayUiEvent.ShowSnackbar("Batch '${batch.name}' deleted"))
        }
    }

    fun createCategory(name: String, isTeaching: Boolean) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val newId = configRepository.saveCategory(trimmed, isTeaching)
            if (newId > 0) {
                val newCat = CategoryEntity(id = newId, name = trimmed, isTeaching = isTeaching, isActive = true)
                _formState.update { it.copy(selectedCategory = newCat) }
                _events.emit(TodayUiEvent.ShowSnackbar("Work type '$trimmed' added"))
            }
        }
    }

    fun updateCategory(category: CategoryEntity, newName: String, isTeaching: Boolean) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val updated = category.copy(name = trimmed, isTeaching = isTeaching)
            configRepository.updateCategory(updated)
            if (_formState.value.selectedCategory?.id == category.id) {
                _formState.update { it.copy(selectedCategory = updated) }
            }
            _events.emit(TodayUiEvent.ShowSnackbar("Work type updated"))
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            configRepository.deleteCategory(category)
            if (_formState.value.selectedCategory?.id == category.id) {
                _formState.update { it.copy(selectedCategory = null) }
            }
            _events.emit(TodayUiEvent.ShowSnackbar("Work type '${category.name}' deleted"))
        }
    }

    // --- Save Actions ---
    fun saveSession() {
        val form = _formState.value
        val category = form.selectedCategory ?: uiState.value.categories.firstOrNull() ?: return
        val batch = form.selectedBatch

        viewModelScope.launch {
            val editingId = form.editingSessionId
            if (editingId != null) {
                val existing = sessionRepository.getById(editingId)
                if (existing != null) {
                    sessionRepository.updateSession(
                        existing.copy(
                            dateEpochDay = _selectedDate.value.toEpochDay(),
                            startMinutes = form.startMinutes,
                            endMinutes = form.endMinutes,
                            categoryId = category.id,
                            batchId = batch?.id,
                            categoryNameSnapshot = category.name,
                            batchNameSnapshot = batch?.name,
                            isTeachingSnapshot = category.isTeaching,
                            note = form.note.trim()
                        )
                    )
                    clearForm()
                    _events.emit(TodayUiEvent.ShowSnackbar("Session updated successfully"))
                }
            } else {
                val result = sessionRepository.insertSession(
                    dateEpochDay = _selectedDate.value.toEpochDay(),
                    startMinutes = form.startMinutes,
                    endMinutes = form.endMinutes,
                    category = category,
                    batch = batch,
                    note = form.note.trim()
                )
                if (result.isSuccess) {
                    clearForm()
                    _events.emit(TodayUiEvent.ShowSnackbar("Session saved"))
                }
            }
        }
    }

    fun saveAndAddAnother() {
        val form = _formState.value
        val category = form.selectedCategory ?: uiState.value.categories.firstOrNull() ?: return
        val batch = form.selectedBatch
        val previousDuration = form.endMinutes - form.startMinutes
        val previousEnd = form.endMinutes

        viewModelScope.launch {
            val result = sessionRepository.insertSession(
                dateEpochDay = _selectedDate.value.toEpochDay(),
                startMinutes = form.startMinutes,
                endMinutes = form.endMinutes,
                category = category,
                batch = batch,
                note = form.note.trim()
            )
            if (result.isSuccess) {
                val newStart = previousEnd
                val newEnd = (previousEnd + previousDuration).coerceAtMost(1440)
                _formState.update { current ->
                    current.copy(
                        note = "",
                        editingSessionId = null,
                        startMinutes = newStart,
                        endMinutes = newEnd
                    )
                }
                checkOverlap()
                _events.emit(TodayUiEvent.ShowSnackbar("Saved! Ready for next session"))
            }
        }
    }

    // --- Existing Session Operations ---
    fun editSession(session: WorkSessionEntity) {
        val matchingCategory = uiState.value.categories.find { it.id == session.categoryId }
        val matchingBatch = session.batchId?.let { bId -> uiState.value.batches.find { it.id == bId } }

        _formState.update { current ->
            current.copy(
                editingSessionId = session.id,
                startMinutes = session.startMinutes,
                endMinutes = session.endMinutes,
                note = session.note,
                selectedCategory = matchingCategory ?: current.selectedCategory,
                selectedBatch = matchingBatch
            )
        }
        checkOverlap()
    }

    fun cancelEdit() {
        clearForm()
    }

    fun duplicateSession(session: WorkSessionEntity) {
        viewModelScope.launch {
            sessionRepository.duplicateSession(session, _selectedDate.value.toEpochDay())
            _events.emit(TodayUiEvent.ShowSnackbar("Session duplicated"))
        }
    }

    fun deleteSession(session: WorkSessionEntity) {
        viewModelScope.launch {
            lastDeletedSession = session
            sessionRepository.deleteSession(session.id)
            _events.emit(
                TodayUiEvent.ShowSnackbar(
                    message = "Session deleted",
                    actionLabel = "UNDO",
                    onAction = { undoDelete() }
                )
            )
        }
    }

    fun undoDelete() {
        val sessionToRestore = lastDeletedSession ?: return
        viewModelScope.launch {
            sessionRepository.duplicateSession(sessionToRestore, sessionToRestore.dateEpochDay)
            lastDeletedSession = null
            _events.emit(TodayUiEvent.ShowSnackbar("Session restored"))
        }
    }

    private fun clearForm() {
        _formState.update { current ->
            val prevDuration = (current.endMinutes - current.startMinutes).let { if (it > 0) it else 60 }
            val nextStart = current.endMinutes.coerceIn(0, 1439)
            val nextEnd = (nextStart + prevDuration).coerceAtMost(1440)
            current.copy(
                editingSessionId = null,
                note = "",
                startMinutes = nextStart,
                endMinutes = nextEnd,
                overlapSession = null
            )
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
                    return TodayViewModel(sessionRepository, configRepository) as T
                }
            }
    }
}
