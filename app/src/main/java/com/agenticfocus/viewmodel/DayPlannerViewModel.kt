package com.agenticfocus.viewmodel

import android.app.Application
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agenticfocus.data.db.AppDatabase
import com.agenticfocus.data.entity.GoalEntity
import com.agenticfocus.data.entity.SubtaskEntity
import com.agenticfocus.data.entity.TaskTemplateEntity
import com.agenticfocus.data.repository.DayPlannerRepository
import com.agenticfocus.data.repository.GoalRepository
import com.agenticfocus.data.repository.SubtaskRepository
import com.agenticfocus.service.TimerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class DayPlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val repository = DayPlannerRepository(
        dayTaskDao = AppDatabase.getInstance(application).dayTaskDao(),
        sessionDao = AppDatabase.getInstance(application).pomodoroSessionDao()
    )

    private val subtaskRepository = SubtaskRepository(AppDatabase.getInstance(application).subtaskDao())
    private val goalRepository = GoalRepository(AppDatabase.getInstance(application).goalDao())

    val goals: StateFlow<List<GoalEntity>> = goalRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _state = MutableStateFlow(DayPlannerState())
    val state: StateFlow<DayPlannerState> = _state.asStateFlow()

    private val _backlogTasks = MutableStateFlow<List<DayTask>>(emptyList())
    val backlogTasks: StateFlow<List<DayTask>> = _backlogTasks.asStateFlow()

    // Single reactive query for all subtasks of today's tasks (avoids N separate flows)
    val subtasksMap: StateFlow<Map<String, List<SubtaskEntity>>> = _state
        .map { s -> s.tasks.map { it.id } }
        .distinctUntilChanged()
        .flatMapLatest { ids ->
            if (ids.isEmpty()) flowOf(emptyMap())
            else subtaskRepository.observeSubtasksForTasks(ids)
                .map { list -> list.groupBy { it.taskId } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    suspend fun getSubtasksForTask(taskId: String): List<SubtaskEntity> =
        subtaskRepository.getSubtasksForTask(taskId)

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
        // Observe Room DB for the selected date — re-subscribes automatically when date changes
        viewModelScope.launch {
            _selectedDate.flatMapLatest { date ->
                repository.observeTasksForDate(date.toString())
            }.collect { tasks ->
                _state.update { it.copy(tasks = tasks) }
            }
        }

        // Observe backlog tasks
        viewModelScope.launch {
            repository.observeBacklogTasks().collect { tasks ->
                _backlogTasks.value = tasks
            }
        }

        // Observe timer to auto-increment completed pomodoros
        viewModelScope.launch {
            TimerService.timerState.collect { timerState ->
                val activeId = _state.value.activeTaskId
                if (activeId != null && timerState.completedPomodoros > previousCompleted) {
                    val endTime = System.currentTimeMillis()
                    val startTime = endTime - (25 * 60 * 1000L)

                    _state.update { s ->
                        s.copy(tasks = s.tasks.map { task ->
                            if (task.id == activeId) {
                                val newCompleted = task.completedPomodoros + 1
                                val allDone = task.plannedPomodoros > 0 && newCompleted >= task.plannedPomodoros
                                task.copy(
                                    completedPomodoros = newCompleted,
                                    isCompleted = task.isCompleted || allDone
                                )
                            } else task
                        })
                    }

                    persistAll()

                    viewModelScope.launch {
                        repository.recordSession(
                            dayTaskId = activeId,
                            date = _selectedDate.value.toString(),
                            startTime = startTime,
                            endTime = endTime,
                            durationMinutes = 25
                        )
                    }
                }
                previousCompleted = timerState.completedPomodoros
            }
        }
    }

    // ── Goals ─────────────────────────────────────────────────────────────────

    fun saveGoal(goal: GoalEntity) {
        viewModelScope.launch(Dispatchers.IO) { goalRepository.save(goal) }
    }

    fun toggleGoal(goal: GoalEntity) {
        val updated = goal.copy(
            isCompleted = if (goal.isCompleted == 1) 0 else 1,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch(Dispatchers.IO) { goalRepository.save(updated) }
    }

    fun addToBacklog(name: String) {
        if (name.isBlank()) return
        val task = DayTask(name = name.trim())
        viewModelScope.launch { repository.saveTask(task, null, 0) }
    }

    fun scheduleFromBacklog(task: DayTask, date: LocalDate) {
        viewModelScope.launch {
            repository.scheduleTask(task.id, date.toString())
        }
    }

    fun addTaskFromTemplate(template: TaskTemplateEntity) {
        val before = totalPlanned()
        _state.update {
            it.copy(
                tasks = it.tasks + DayTask(
                    name = template.title,
                    plannedPomodoros = template.defaultPomodoros,
                    templateId = template.id,
                    domainId = template.domainId,
                    storyPoints = template.storyPoints,
                    impact = template.impact,
                    urgency = template.urgency,
                    dueDate = template.dueDate
                )
            )
        }
        checkCapacityAlert(before)
        persistAll()
    }

    fun addTask(
        name: String,
        impact: String? = null,
        urgency: String? = null,
        scheduledDate: LocalDate? = null
    ) {
        if (name.isBlank()) return
        val task = DayTask(name = name.trim(), impact = impact, urgency = urgency)
        when {
            scheduledDate == null -> {
                // No date = backlog
                viewModelScope.launch { repository.saveTask(task, null, 0) }
            }
            scheduledDate == _selectedDate.value -> {
                val before = totalPlanned()
                _state.update { it.copy(tasks = it.tasks + task) }
                checkCapacityAlert(before)
                persistAll()
            }
            else -> {
                viewModelScope.launch { repository.saveTask(task, scheduledDate.toString(), 0) }
            }
        }
    }

    fun removeTask(id: String) {
        _state.update { s ->
            s.copy(
                tasks = s.tasks.filter { it.id != id },
                activeTaskId = if (s.activeTaskId == id) null else s.activeTaskId
            )
        }
        viewModelScope.launch { repository.deleteTask(id) }
        persistAll()
    }

    fun reorderTasks(fromIndex: Int, toIndex: Int) {
        _state.update { s ->
            val list = s.tasks.toMutableList()
            list.add(toIndex, list.removeAt(fromIndex))
            s.copy(tasks = list)
        }
        persistAll()
    }

    fun updatePlanned(id: String, delta: Int) {
        val before = totalPlanned()
        _state.update { s ->
            s.copy(tasks = s.tasks.map { task ->
                if (task.id == id) {
                    val min = task.completedPomodoros.coerceAtLeast(0)
                    task.copy(
                        plannedPomodoros = (task.plannedPomodoros + delta)
                            .coerceIn(min, MAX_POMODOROS_PER_TASK)
                    )
                } else task
            })
        }
        checkCapacityAlert(before)
        persistAll()
    }

    fun updateTaskDetails(id: String, name: String, note: String?, impact: String?, urgency: String?, scheduledDate: LocalDate?, newSubtasks: List<SubtaskEntity> = emptyList(), originalSubtaskIds: Set<String> = emptySet()) {
        if (name.isBlank()) return
        val updatedFields: (com.agenticfocus.viewmodel.DayTask) -> com.agenticfocus.viewmodel.DayTask = { task ->
            task.copy(
                name = name.trim(),
                note = note?.takeIf { it.isNotBlank() },
                impact = impact,
                urgency = urgency
            )
        }
        when {
            scheduledDate == null -> {
                // Move to backlog
                val movedTask = _state.value.tasks.find { it.id == id }?.let(updatedFields) ?: return
                _state.update { s ->
                    s.copy(
                        tasks = s.tasks.filter { it.id != id },
                        activeTaskId = if (s.activeTaskId == id) null else s.activeTaskId
                    )
                }
                viewModelScope.launch {
                    repository.deleteTask(id)
                    repository.saveTask(movedTask, null, 0)
                }
            }
            scheduledDate == _selectedDate.value -> {
                _state.update { s -> s.copy(tasks = s.tasks.map { if (it.id == id) updatedFields(it) else it }) }
                persistAll()
            }
            else -> {
                // Move to a different day
                val movedTask = _state.value.tasks.find { it.id == id }?.let(updatedFields) ?: return
                _state.update { s ->
                    s.copy(
                        tasks = s.tasks.filter { it.id != id },
                        activeTaskId = if (s.activeTaskId == id) null else s.activeTaskId
                    )
                }
                viewModelScope.launch {
                    repository.deleteTask(id)
                    repository.saveTask(movedTask, scheduledDate.toString(), 0)
                }
            }
        }

        // Persist subtask changes
        viewModelScope.launch {
            val removedIds = originalSubtaskIds - newSubtasks.map { it.id }.toSet()
            removedIds.forEach { subtaskRepository.deleteSubtask(it) }
            val indexed = newSubtasks.mapIndexed { i, s -> s.copy(position = i, updatedAt = System.currentTimeMillis()) }
            if (indexed.isNotEmpty()) subtaskRepository.saveSubtasks(indexed)
        }
    }

    fun toggleCompletion(id: String) {
        _state.update { s ->
            s.copy(tasks = s.tasks.map { task ->
                if (task.id == id) task.copy(isCompleted = !task.isCompleted) else task
            })
        }
        persistAll()
    }

    fun toggleSubtask(subtaskId: String) {
        viewModelScope.launch {
            val currentMap = subtasksMap.value
            val subtask = currentMap.values.flatten().find { it.id == subtaskId } ?: return@launch
            val updated = subtask.copy(isCompleted = !subtask.isCompleted, updatedAt = System.currentTimeMillis())
            subtaskRepository.saveSubtask(updated)

            // Auto-complete parent when all subtasks done; reactivate if any unchecked
            val taskSubtasks = currentMap[subtask.taskId] ?: return@launch
            val updatedList = taskSubtasks.map { if (it.id == subtaskId) updated else it }
            val allDone = updatedList.all { it.isCompleted }
            val parent = _state.value.tasks.find { it.id == subtask.taskId } ?: return@launch
            if (allDone && !parent.isCompleted) toggleCompletion(subtask.taskId)
            else if (!allDone && parent.isCompleted) toggleCompletion(subtask.taskId)
        }
    }

    fun updateName(id: String, name: String) {
        if (name.isBlank()) return
        _state.update { s ->
            s.copy(tasks = s.tasks.map { task ->
                if (task.id == id) task.copy(name = name.trim()) else task
            })
        }
        persistAll()
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
            putExtra(TimerService.EXTRA_AUTO_START, true)
        }
        getApplication<Application>().startService(intent)
        _navigateToTimerEvent.tryEmit(Unit)
    }

    fun goToPreviousDay() { _selectedDate.update { it.minusDays(1) } }
    fun goToNextDay()     { _selectedDate.update { it.plusDays(1) } }
    fun goToToday()       { _selectedDate.value = LocalDate.now() }

    private fun persistAll() {
        val snapshot = _state.value.tasks
        val date = _selectedDate.value.toString()
        viewModelScope.launch {
            repository.saveAllTasks(snapshot, date)
        }
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
