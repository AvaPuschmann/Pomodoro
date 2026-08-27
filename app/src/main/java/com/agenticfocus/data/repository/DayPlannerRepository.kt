package com.agenticfocus.data.repository

import androidx.room.withTransaction
import com.agenticfocus.data.dao.DayTaskDao
import com.agenticfocus.data.dao.PomodoroSessionDao
import com.agenticfocus.data.db.AppDatabase
import com.agenticfocus.data.entity.DayTaskEntity
import com.agenticfocus.data.entity.PomodoroSessionEntity
import com.agenticfocus.data.sync.SyncEngine
import com.agenticfocus.viewmodel.DayTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DayPlannerRepository(
    private val db: AppDatabase,
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
        val now = System.currentTimeMillis()
        // Transaction atomique : Room's InvalidationTracker n'émet qu'UNE FOIS à la fin,
        // au lieu de N fois (une par updatePlannerFields). Sans transaction, chaque écriture
        // déclenchait une émission du Flow avec des positions incohérentes (ex. deux tâches
        // à position=0 pendant un réordonnancement) → liste visible qui saute N fois.
        // Mise à jour partielle via updatePlannerFields : ne touche JAMAIS completed_pomodoros.
        // Ce champ appartient exclusivement au coroutine IO du TimerService (auto-incrément).
        // Fallback upsert pour les tâches nouvelles pas encore en DB (ex. addTask optimiste).
        db.withTransaction {
            tasks.forEachIndexed { index, task ->
                val rowsUpdated = dayTaskDao.updatePlannerFields(
                    id = task.id, position = index, planned = task.plannedPomodoros,
                    isCompleted = task.isCompleted, name = task.name,
                    impact = task.impact, urgency = task.urgency, dueDate = task.dueDate,
                    note = task.note, storyPoints = task.storyPoints,
                    domainId = task.domainId, projectId = task.projectId, updatedAt = now
                )
                if (rowsUpdated == 0) {
                    // Nouvelle tâche pas encore en DB — upsert complet (completedPomodoros = 0, sûr)
                    dayTaskDao.upsert(task.toEntity(date, index))
                }
            }
        }
        // SyncEngine hors transaction : pas besoin de tenir le verrou DB pour les appels réseau.
        // Lecture DB après les écritures : les entities reflètent le completedPomodoros correct
        // (potentiellement mis à jour par le coroutine IO du TimerService pendant nos écritures).
        val dbMap = dayTaskDao.getTasksForDate(date).associateBy { it.id }
        tasks.forEach { task ->
            val entityForSync = dbMap[task.id] ?: return@forEach
            try { SyncEngine.upsertDayTask(entityForSync) } catch (_: Exception) {}
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
    routineItemId = routineItemId,
    projectId = projectId
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
    routineItemId = null,
    projectId = projectId  // Story 18-7 — persist project_id at creation
)
