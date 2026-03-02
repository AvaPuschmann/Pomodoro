package com.agenticfocus.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.agenticfocus.data.entity.DayTaskEntity

@Dao
interface DayTaskDao {

    @Query("SELECT * FROM day_tasks WHERE date = :date ORDER BY position ASC")
    suspend fun getTasksForDate(date: String): List<DayTaskEntity>

    @Upsert
    suspend fun upsertAll(tasks: List<DayTaskEntity>)

    @Query("DELETE FROM day_tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Delete
    suspend fun delete(task: DayTaskEntity)
}
