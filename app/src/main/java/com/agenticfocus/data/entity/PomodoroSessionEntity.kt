package com.agenticfocus.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "pomodoro_sessions")
data class PomodoroSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "day_task_id") val dayTaskId: String,
    val date: String,              // "2026-03-02"
    @ColumnInfo(name = "start_time") val startTime: Long,   // Unix timestamp ms
    @ColumnInfo(name = "end_time") val endTime: Long,       // Unix timestamp ms
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int = 25,
    @ColumnInfo(name = "user_id") val userId: String = "",
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
