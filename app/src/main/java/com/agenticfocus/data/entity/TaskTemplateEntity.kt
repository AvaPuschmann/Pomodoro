package com.agenticfocus.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_templates")
data class TaskTemplateEntity(
    @PrimaryKey val id: String,
    val title: String,
    val note: String? = null,
    @ColumnInfo(name = "domain_id") val domainId: String,
    @ColumnInfo(name = "story_points") val storyPoints: Int,
    @ColumnInfo(name = "default_pomodoros") val defaultPomodoros: Int,
    @ColumnInfo(name = "user_id") val userId: String = "",
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    val impact: String? = null,       // "high" | "low" | null
    val urgency: String? = null,      // "urgent" | "not_urgent" | null
    @ColumnInfo(name = "due_date") val dueDate: Long? = null
)
