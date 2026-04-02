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

    @Query("DELETE FROM day_tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Delete
    suspend fun delete(task: DayTaskEntity)
}
