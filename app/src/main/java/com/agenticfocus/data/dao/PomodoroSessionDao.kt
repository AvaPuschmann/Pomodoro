package com.agenticfocus.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.agenticfocus.data.entity.PomodoroSessionEntity

@Dao
interface PomodoroSessionDao {

    @Insert
    suspend fun insert(session: PomodoroSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: PomodoroSessionEntity)

    @Query("SELECT * FROM pomodoro_sessions WHERE date = :date ORDER BY start_time ASC")
    suspend fun getSessionsForDate(date: String): List<PomodoroSessionEntity>

    @Query("SELECT updated_at FROM pomodoro_sessions WHERE id = :id")
    suspend fun getUpdatedAtById(id: String): Long?

    @Query("DELETE FROM pomodoro_sessions WHERE id = :id")
    suspend fun deleteById(id: String)
}
