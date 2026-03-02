package com.agenticfocus.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pomodoro_sessions")
data class PomodoroSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayTaskId: String,
    val date: String,              // "2026-03-02"
    val startTime: Long,           // Unix timestamp ms
    val endTime: Long,             // Unix timestamp ms
    val durationMinutes: Int = 25
)
