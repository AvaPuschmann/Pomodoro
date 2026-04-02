package com.agenticfocus.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.agenticfocus.data.entity.SubtaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {

    @Upsert
    suspend fun upsert(subtask: SubtaskEntity)

    @Upsert
    suspend fun upsertAll(subtasks: List<SubtaskEntity>)

    @Query("SELECT * FROM subtasks WHERE task_id = :taskId ORDER BY position ASC, created_at ASC")
    suspend fun getSubtasksForTask(taskId: String): List<SubtaskEntity>

    @Query("SELECT * FROM subtasks WHERE task_id = :taskId ORDER BY position ASC, created_at ASC")
    fun observeSubtasksForTask(taskId: String): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE task_id IN (:taskIds) ORDER BY task_id, position ASC")
    fun observeSubtasksForTasks(taskIds: List<String>): Flow<List<SubtaskEntity>>

    @Query("DELETE FROM subtasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM subtasks WHERE task_id = :taskId")
    suspend fun deleteByTaskId(taskId: String)
}
