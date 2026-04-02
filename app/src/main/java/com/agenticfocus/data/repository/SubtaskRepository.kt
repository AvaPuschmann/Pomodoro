package com.agenticfocus.data.repository

import com.agenticfocus.data.dao.SubtaskDao
import com.agenticfocus.data.entity.SubtaskEntity
import com.agenticfocus.data.sync.SyncEngine
import kotlinx.coroutines.flow.Flow

class SubtaskRepository(private val subtaskDao: SubtaskDao) {

    fun observeSubtasksForTask(taskId: String): Flow<List<SubtaskEntity>> =
        subtaskDao.observeSubtasksForTask(taskId)

    fun observeSubtasksForTasks(taskIds: List<String>): Flow<List<SubtaskEntity>> =
        subtaskDao.observeSubtasksForTasks(taskIds)

    suspend fun getSubtasksForTask(taskId: String): List<SubtaskEntity> =
        subtaskDao.getSubtasksForTask(taskId)

    suspend fun saveSubtask(subtask: SubtaskEntity) {
        subtaskDao.upsert(subtask)
        try { SyncEngine.upsertSubtask(subtask) } catch (_: Exception) {}
    }

    suspend fun saveSubtasks(subtasks: List<SubtaskEntity>) {
        subtaskDao.upsertAll(subtasks)
        subtasks.forEach { subtask ->
            try { SyncEngine.upsertSubtask(subtask) } catch (_: Exception) {}
        }
    }

    suspend fun deleteSubtask(id: String) {
        subtaskDao.deleteById(id)
        try { SyncEngine.deleteSubtask(id) } catch (_: Exception) {}
    }

    suspend fun deleteSubtasksForTask(taskId: String) {
        val subtasks = subtaskDao.getSubtasksForTask(taskId)
        subtaskDao.deleteByTaskId(taskId)
        subtasks.forEach { subtask ->
            try { SyncEngine.deleteSubtask(subtask.id) } catch (_: Exception) {}
        }
    }
}
