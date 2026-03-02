package com.agenticfocus.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_tasks")
data class DayTaskEntity(
    @PrimaryKey val id: String,
    val date: String,              // "2026-03-02"
    val name: String,
    val plannedPomodoros: Int,
    val completedPomodoros: Int,
    val position: Int,
    val templateId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
