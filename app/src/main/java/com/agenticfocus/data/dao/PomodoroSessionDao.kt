package com.agenticfocus.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.agenticfocus.data.entity.PomodoroSessionEntity

@Dao
interface PomodoroSessionDao {

    @Insert
    suspend fun insert(session: PomodoroSessionEntity)

    @Query("SELECT * FROM pomodoro_sessions WHERE date = :date ORDER BY startTime ASC")
    suspend fun getSessionsForDate(date: String): List<PomodoroSessionEntity>
}
