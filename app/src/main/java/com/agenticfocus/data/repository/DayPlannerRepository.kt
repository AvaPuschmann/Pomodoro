package com.agenticfocus.data.repository

import com.agenticfocus.data.dao.DayTaskDao
import com.agenticfocus.data.dao.PomodoroSessionDao
import com.agenticfocus.data.entity.DayTaskEntity
import com.agenticfocus.data.entity.PomodoroSessionEntity
import com.agenticfocus.viewmodel.DayTask

class DayPlannerRepository(
    private val dayTaskDao: DayTaskDao,
    private val sessionDao: PomodoroSessionDao
) {

    suspend fun getTasksForDate(date: String): List<DayTask> =
        dayTaskDao.getTasksForDate(date).map { it.toDayTask() }

    suspend fun saveAllTasks(tasks: List<DayTask>, date: String) =
        dayTaskDao.upsertAll(tasks.mapIndexed { index, task -> task.toEntity(date, index) })

    suspend fun deleteTask(id: String) =
        dayTaskDao.deleteById(id)

    suspend fun recordSession(dayTaskId: String, date: String, startTime: Long, endTime: Long, durationMinutes: Int) =
        sessionDao.insert(
            PomodoroSessionEntity(
                dayTaskId = dayTaskId,
                date = date,
                startTime = startTime,
                endTime = endTime,
                durationMinutes = durationMinutes
            )
        )
}

private fun DayTaskEntity.toDayTask() = DayTask(
    id = id,
    name = name,
    plannedPomodoros = plannedPomodoros,
    completedPomodoros = completedPomodoros
)

private fun DayTask.toEntity(date: String, position: Int) = DayTaskEntity(
    id = id,
    date = date,
    name = name,
    plannedPomodoros = plannedPomodoros,
    completedPomodoros = completedPomodoros,
    position = position
)
