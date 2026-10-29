package com.classlog.app.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.classlog.app.data.datastore.AppSettings
import com.classlog.app.data.local.entity.WorkSessionEntity
import com.classlog.app.data.repository.ConfigRepository
import com.classlog.app.data.repository.SessionRepository
import com.classlog.app.domain.calculator.SummaryCalculator
import com.classlog.app.domain.model.MonthSummary
import com.classlog.app.util.monthRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class DailyActivity(
    val dayOfMonth: Int,
    val dateEpochDay: Long,
    val minutes: Int,
    val sessionCount: Int
)

data class SummaryUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val summary: MonthSummary = MonthSummary(
        totalMinutes = 0,
        sessionCount = 0,
        daysWorked = 0,
        teachingMinutes = 0,
        nonTeachingMinutes = 0,
        byCategory = emptyMap(),
        byBatch = emptyMap()
    ),
    val dailyActivities: List<DailyActivity> = emptyList(),
    val maxDailyMinutes: Int = 0,
    val averageMinutesPerWorkedDay: Int = 0,
    val teachingRatio: Float = 0f,
    val sessions: List<WorkSessionEntity> = emptyList(),
    val settings: AppSettings = AppSettings()
)

class SummaryViewModel(
    private val sessionRepository: SessionRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthSessionsFlow = _currentMonth.flatMapLatest { ym ->
        val range = monthRange(ym)
        sessionRepository.observeRange(range.first, range.second)
    }

    val uiState: StateFlow<SummaryUiState> = combine(
        _currentMonth,
        monthSessionsFlow,
        configRepository.settings
    ) { ym, sessions, settings ->
        val summary = SummaryCalculator.summarise(sessions)

        val sessionsByDay = sessions.groupBy { it.dateEpochDay }
        val daysInMonth = ym.lengthOfMonth()

        val dailyList = (1..daysInMonth).map { day ->
            val date = ym.atDay(day)
            val epochDay = date.toEpochDay()
            val daySessions = sessionsByDay[epochDay].orEmpty()
            val minutes = daySessions.sumOf { it.durationMinutes }
            DailyActivity(
                dayOfMonth = day,
                dateEpochDay = epochDay,
                minutes = minutes,
                sessionCount = daySessions.size
            )
        }

        val maxMinutes = dailyList.maxOfOrNull { it.minutes } ?: 0
        val avgMinutes = if (summary.daysWorked > 0) summary.totalMinutes / summary.daysWorked else 0
        val ratio = if (summary.totalMinutes > 0) summary.teachingMinutes.toFloat() / summary.totalMinutes else 0f

        SummaryUiState(
            currentMonth = ym,
            summary = summary,
            dailyActivities = dailyList,
            maxDailyMinutes = maxMinutes,
            averageMinutesPerWorkedDay = avgMinutes,
            teachingRatio = ratio,
            sessions = sessions,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SummaryUiState()
    )

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun setMonth(yearMonth: YearMonth) {
        _currentMonth.value = yearMonth
    }

    companion object {
        fun provideFactory(
            sessionRepository: SessionRepository,
            configRepository: ConfigRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SummaryViewModel(sessionRepository, configRepository) as T
                }
            }
    }
}
