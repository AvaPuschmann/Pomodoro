package com.agenticfocus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agenticfocus.data.db.AppDatabase
import com.agenticfocus.data.repository.StatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DailyStatsPoint(
    val date: String,
    val shortLabel: String,
    val pomodoros: Int,
    val points: Int
)

enum class StatsPeriod(val days: Int, val label: String) {
    WEEK(7, "7j"),
    MONTH(30, "30j")
}

enum class StatsMetric(val label: String) {
    POMODOROS("🍅 Pomodoros"),
    POINTS("⭐ Points")
}

data class StatsState(
    val period: StatsPeriod = StatsPeriod.WEEK,
    val metric: StatsMetric = StatsMetric.POMODOROS,
    val dataPoints: List<DailyStatsPoint> = emptyList()
) {
    val values: List<Float>
        get() = dataPoints.map {
            when (metric) {
                StatsMetric.POMODOROS -> it.pomodoros.toFloat()
                StatsMetric.POINTS    -> it.points.toFloat()
            }
        }

    val total: Int get() = values.sumOf { it.toInt() }

    val bestValue: Int
        get() = when (metric) {
            StatsMetric.POMODOROS -> dataPoints.maxOfOrNull { it.pomodoros } ?: 0
            StatsMetric.POINTS    -> dataPoints.maxOfOrNull { it.points } ?: 0
        }

    val average: Float
        get() = if (dataPoints.isEmpty()) 0f else values.sum() / period.days
}

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StatsRepository(
        AppDatabase.getInstance(application).statsDao()
    )

    private val _state = MutableStateFlow(StatsState())
    val state: StateFlow<StatsState> = _state.asStateFlow()

    init { loadStats() }

    fun setPeriod(period: StatsPeriod) {
        _state.update { it.copy(period = period) }
        loadStats()
    }

    fun setMetric(metric: StatsMetric) {
        _state.update { it.copy(metric = metric) }
    }

    private fun loadStats() {
        viewModelScope.launch {
            val data = repository.getStats(_state.value.period)
            _state.update { it.copy(dataPoints = data) }
        }
    }
}
