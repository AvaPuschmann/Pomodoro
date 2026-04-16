package com.agenticfocus.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_tasks")
data class DayTaskEntity(
    @PrimaryKey val id: String,
    val date: String?,             // "2026-03-02" or null = backlog
    val name: String,
    @ColumnInfo(name = "planned_pomodoros") val plannedPomodoros: Int,
    @ColumnInfo(name = "completed_pomodoros") val completedPomodoros: Int,
    val position: Int,
    @ColumnInfo(name = "template_id") val templateId: String? = null,
    @ColumnInfo(name = "domain_id") val domainId: String? = null,
    @ColumnInfo(name = "story_points") val storyPoints: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "user_id") val userId: String = "",
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    val impact: String? = null,
    val urgency: String? = null,
    @ColumnInfo(name = "due_date") val dueDate: Long? = null,
    val note: String? = null,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    @ColumnInfo(name = "source", defaultValue = "NULL") val source: String? = null,
    @ColumnInfo(name = "routine_item_id", defaultValue = "NULL") val routineItemId: String? = null
)
