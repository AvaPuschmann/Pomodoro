package com.agenticfocus.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import com.agenticfocus.service.TimerService
import kotlinx.coroutines.flow.StateFlow

enum class Phase(val durationSeconds: Int) {
    FOCUS(1500),
    SHORT_BREAK(300),
    LONG_BREAK(900)
}

data class PomodoroState(
    val phase: Phase = Phase.FOCUS,
    val totalSeconds: Int = Phase.FOCUS.durationSeconds,
    val remainingSeconds: Int = Phase.FOCUS.durationSeconds,
    val isRunning: Boolean = false,
    val taskName: String = "",
    val completedPomodoros: Int = 0,
    val plannedPomodoros: Int = 1,
    val activeTaskId: String? = null   // ← NEW
)

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    val uiState: StateFlow<PomodoroState> = TimerService.timerState

    init {
        // If service is already running (e.g., user navigated away and returned),
        // the companion object StateFlow already holds the live state — no action needed.
    }

    fun startTimer() {
        sendIntent(TimerService.ACTION_START)
    }

    fun pauseTimer() {
        sendIntent(TimerService.ACTION_PAUSE)
    }

    fun resetToPhase(phase: Phase) {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_RESET
            putExtra(TimerService.EXTRA_PHASE, phase.name)
        }
        getApplication<Application>().startService(intent)
    }

    fun updateTaskName(name: String) {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_UPDATE_TASK
            putExtra(TimerService.EXTRA_TASK_NAME, name)
        }
        getApplication<Application>().startService(intent)
    }

    fun increasePlanned() = sendPlannedDelta(+1)
    fun decreasePlanned() = sendPlannedDelta(-1)

    private fun sendPlannedDelta(delta: Int) {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_UPDATE_PLANNED
            putExtra(TimerService.EXTRA_PLANNED_DELTA, delta)
        }
        getApplication<Application>().startService(intent)
    }

    private fun sendIntent(action: String) {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            this.action = action
        }
        if (action == TimerService.ACTION_START) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }
}
