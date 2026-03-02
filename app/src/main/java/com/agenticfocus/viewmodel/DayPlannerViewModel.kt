package com.agenticfocus.viewmodel

import android.app.Application
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agenticfocus.service.TimerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DayPlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(DayPlannerState())
    val state: StateFlow<DayPlannerState> = _state.asStateFlow()

    private val _navigateToTimerEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToTimerEvent: SharedFlow<Unit> = _navigateToTimerEvent.asSharedFlow()

    // Guard value: set to Int.MAX_VALUE in activateTask() to block spurious increments
    // during the race window between activeTaskId update and service reset propagation.
    // Resets naturally when service emits completedPomodoros = 0 (0 > MAX_VALUE is false).
    // Threading: viewModelScope uses Dispatchers.Main.immediate; activateTask() is called
    // from UI (Main thread). Both accesses are serialized on Main — no data race.
    // Constraint: DayPlannerViewModel MUST be created on the Main thread (guaranteed when
    // using viewModel() in Compose setContent, as per AndroidViewModel lifecycle contract).
    private var previousCompleted = 0

    init {
        viewModelScope.launch {
            TimerService.timerState.collect { timerState ->
                val activeId = _state.value.activeTaskId
                if (activeId != null && timerState.completedPomodoros > previousCompleted) {
                    _state.update { s ->
                        s.copy(tasks = s.tasks.map { task ->
                            if (task.id == activeId)
                                task.copy(completedPomodoros = task.completedPomodoros + 1)
                            else task
                        })
                    }
                }
                previousCompleted = timerState.completedPomodoros
            }
        }
    }

    fun addTask(name: String) {
        if (name.isBlank()) return
        val before = totalPlanned()
        _state.update { it.copy(tasks = it.tasks + DayTask(name = name.trim())) }
        checkCapacityAlert(before)
    }

    fun removeTask(id: String) {
        _state.update { s ->
            s.copy(
                tasks = s.tasks.filter { it.id != id },
                activeTaskId = if (s.activeTaskId == id) null else s.activeTaskId
            )
        }
    }

    fun reorderTasks(fromIndex: Int, toIndex: Int) {
        _state.update { s ->
            val list = s.tasks.toMutableList()
            list.add(toIndex, list.removeAt(fromIndex))
            s.copy(tasks = list)
        }
    }

    fun updatePlanned(id: String, delta: Int) {
        val before = totalPlanned()
        _state.update { s ->
            s.copy(tasks = s.tasks.map { task ->
                if (task.id == id) {
                    val min = task.completedPomodoros.coerceAtLeast(1)
                    task.copy(
                        plannedPomodoros = (task.plannedPomodoros + delta)
                            .coerceIn(min, MAX_POMODOROS_PER_TASK)
                    )
                } else task
            })
        }
        checkCapacityAlert(before)
    }

    fun updateName(id: String, name: String) {
        if (name.isBlank()) return
        _state.update { s ->
            s.copy(tasks = s.tasks.map { task ->
                if (task.id == id) task.copy(name = name.trim()) else task
            })
        }
    }

    fun activateTask(task: DayTask) {
        // Set guard BEFORE updating activeTaskId to prevent spurious increments
        // during the window between state update and service reset propagation.
        previousCompleted = Int.MAX_VALUE
        _state.update { it.copy(activeTaskId = task.id) }
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_ACTIVATE_TASK
            putExtra(TimerService.EXTRA_TASK_ID, task.id)
            putExtra(TimerService.EXTRA_TASK_NAME, task.name)
            putExtra(TimerService.EXTRA_PLANNED_POMODOROS, task.plannedPomodoros)
        }
        getApplication<Application>().startService(intent)
        _navigateToTimerEvent.tryEmit(Unit)
    }

    private fun totalPlanned(): Int = _state.value.tasks.sumOf { it.plannedPomodoros }

    private fun checkCapacityAlert(totalBefore: Int) {
        val totalAfter = totalPlanned()
        if (totalBefore <= DAILY_CAPACITY && totalAfter > DAILY_CAPACITY) {
            viewModelScope.launch(Dispatchers.Main) {
                try {
                    val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
                    delay(350) // Wait for tone to complete before releasing
                    tg.release()
                } catch (e: Exception) {
                    Log.w("DayPlannerVM", "Capacity alert tone failed: ${e.message}")
                }
            }
        }
    }

    companion object {
        // Business rule defined by user: ~6h of focused work per day.
        // completedPomodoros will never approach Int.MAX_VALUE in normal use
        // (MAX_VALUE = 2.1 billion; realistic daily max is O(20)).
        const val DAILY_CAPACITY = 12
        const val MAX_POMODOROS_PER_TASK = 6
    }
}
