package com.agenticfocus.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.agenticfocus.data.entity.DayTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DayTaskDao {

    @Query("SELECT * FROM day_tasks WHERE date = :date ORDER BY position ASC")
    suspend fun getTasksForDate(date: String): List<DayTaskEntity>

    @Query("SELECT * FROM day_tasks WHERE date = :date ORDER BY position ASC")
    fun observeTasksForDate(date: String): Flow<List<DayTaskEntity>>

    @Upsert
    suspend fun upsertAll(tasks: List<DayTaskEntity>)

    @Upsert
    suspend fun upsert(task: DayTaskEntity)

    @Query("SELECT updated_at FROM day_tasks WHERE id = :id")
    suspend fun getUpdatedAtById(id: String): Long?

    @Query("SELECT * FROM day_tasks WHERE date IS NULL ORDER BY position ASC")
    fun observeBacklogTasks(): Flow<List<DayTaskEntity>>

    @Query("UPDATE day_tasks SET date = :date WHERE id = :id")
    suspend fun scheduleTask(id: String, date: String)

    // Story 18-4 — unschedule (date = NULL → tâche revient au backlog).
    @Query("UPDATE day_tasks SET date = NULL WHERE id = :id")
    suspend fun unscheduleTask(id: String)

    // Story 18-4 — observe day_tasks rattachées à un projet (pour ProjectDetailScreen).
    @Query("SELECT * FROM day_tasks WHERE project_id = :projectId ORDER BY position ASC, created_at ASC")
    fun observeTasksForProject(projectId: String): Flow<List<DayTaskEntity>>

    @Query("DELETE FROM day_tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Delete
    suspend fun delete(task: DayTaskEntity)

    // A1 reconciliation — include orphan rows (user_id='') so they can be
    // cleaned up by the reconciliation step (those rows are never in Supabase
    // since Supabase queries filter by user_id, so they will be deleted as
    // "absent from remote").
    @Query("SELECT id FROM day_tasks WHERE user_id = :userId OR user_id = ''")
    suspend fun getAllIdsForUser(userId: String): List<String>

    // Update partiel project_id — utilisé par ProjectRepository.setProjectIdOnTask
    // (Story 17-6) pour lier/délier une tâche sans charger la row entière. Le
    // timestamp est injecté par l'appelant (pattern [F2/D47]) pour cohérence
    // cross-platform et tests déterministes.
    @Query("""
        UPDATE day_tasks
        SET project_id = :projectId, updated_at = :updatedAt
        WHERE id = :taskId
    """)
    suspend fun updateProjectId(taskId: String, projectId: String?, updatedAt: Long)

    // Stats agrégée GROUP BY project_id — Flow réactif [F18/D56, F-G fix N+1].
    // Une seule query plutôt qu'une par projet. Flow émet sur chaque mutation
    // day_tasks (local ou Realtime) via Room InvalidationTracker.
    @Query("""
        SELECT project_id AS projectId,
               COUNT(*) AS taskCount,
               COALESCE(SUM(planned_pomodoros), 0) AS plannedSum,
               COALESCE(SUM(completed_pomodoros), 0) AS completedSum
        FROM day_tasks
        WHERE user_id = :userId AND project_id IS NOT NULL
        GROUP BY project_id
    """)
    fun observeAllProjectStats(userId: String): Flow<List<ProjectStatsRow>>

    @Query("SELECT * FROM day_tasks WHERE id = :id")
    suspend fun getById(id: String): DayTaskEntity?
}

data class ProjectStatsRow(
    val projectId: String,
    val taskCount: Int,
    val plannedSum: Int,
    val completedSum: Int,
)
