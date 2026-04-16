package com.agenticfocus.data.repository

import com.agenticfocus.data.dao.DayTaskDao
import com.agenticfocus.data.dao.PomodoroSessionDao
import com.agenticfocus.data.entity.DayTaskEntity
import com.agenticfocus.data.entity.PomodoroSessionEntity
import com.agenticfocus.data.sync.SyncEngine
import com.agenticfocus.viewmodel.DayTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DayPlannerRepository(
    private val dayTaskDao: DayTaskDao,
    private val sessionDao: PomodoroSessionDao
) {

    suspend fun getTasksForDate(date: String): List<DayTask> =
        dayTaskDao.getTasksForDate(date).map { it.toDayTask() }

    fun observeTasksForDate(date: String): Flow<List<DayTask>> =
        dayTaskDao.observeTasksForDate(date).map { entities -> entities.map { it.toDayTask() } }

    fun observeBacklogTasks(): Flow<List<DayTask>> =
        dayTaskDao.observeBacklogTasks().map { entities -> entities.map { it.toDayTask() } }

    suspend fun scheduleTask(id: String, date: String) {
        dayTaskDao.scheduleTask(id, date)
        val entity = dayTaskDao.getTasksForDate(date).firstOrNull { it.id == id }
        if (entity != null) {
            try { SyncEngine.upsertDayTask(entity) } catch (_: Exception) {}
        }
    }

    suspend fun saveAllTasks(tasks: List<DayTask>, date: String) {
        val entities = tasks.mapIndexed { index, task -> task.toEntity(date, index) }
        dayTaskDao.upsertAll(entities)
        entities.forEach { entity ->
            try { SyncEngine.upsertDayTask(entity) } catch (_: Exception) {}
        }
    }

    suspend fun saveTask(task: DayTask, date: String?, position: Int = 0) {
        val entity = task.toEntity(date, position)
        dayTaskDao.upsert(entity)
        try { SyncEngine.upsertDayTask(entity) } catch (_: Exception) {}
    }

    suspend fun deleteTask(id: String) {
        dayTaskDao.deleteById(id)
        try { SyncEngine.deleteDayTask(id) } catch (_: Exception) {}
    }

    suspend fun recordSession(dayTaskId: String, date: String, startTime: Long, endTime: Long, durationMinutes: Int) {
        val entity = PomodoroSessionEntity(
            id = java.util.UUID.randomUUID().toString(),
            dayTaskId = dayTaskId,
            date = date,
            startTime = startTime,
            endTime = endTime,
            durationMinutes = durationMinutes
        )
        sessionDao.insert(entity)
        try { SyncEngine.upsertPomodoroSession(entity) } catch (_: Exception) {}
    }
}

private fun DayTaskEntity.toDayTask() = DayTask(
    id = id,
    name = name,
    plannedPomodoros = plannedPomodoros,
    completedPomodoros = completedPomodoros,
    templateId = templateId,
    domainId = domainId,
    storyPoints = storyPoints,
    impact = impact,
    urgency = urgency,
    dueDate = dueDate,
    note = note,
    isCompleted = isCompleted,
    routineItemId = routineItemId
)

private fun DayTask.toEntity(date: String?, position: Int) = DayTaskEntity(
    id = id,
    date = date,
    name = name,
    plannedPomodoros = plannedPomodoros,
    completedPomodoros = completedPomodoros,
    position = position,
    templateId = templateId,
    domainId = domainId,
    storyPoints = storyPoints,
    impact = impact,
    urgency = urgency,
    dueDate = dueDate,
    note = note,
    isCompleted = isCompleted,
    routineItemId = null
)
