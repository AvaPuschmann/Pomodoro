package com.agenticfocus.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// NOTE: @ColumnInfo(defaultValue=…) and indices declarations must mirror what the
// migrations actually created in SQL — otherwise Room's runtime schema validation
// crashes the app at startup with "Migration didn't properly handle: day_tasks".
// Defaults from MIGRATION_2_3 / 6_7 / 7_8 / 9_10 / 11_12 / 12_13.
// Indices: idx_day_tasks_routine_item_date from MIGRATION_12_13/14_15 ;
//          index_day_tasks_project_id from MIGRATION_15_16 (Story 16-4 / D26 / F5).
@Entity(
    tableName = "day_tasks",
    indices = [
        Index(
            value = ["routine_item_id", "date"],
            unique = true,
            name = "idx_day_tasks_routine_item_date"
        ),
        Index(
            value = ["project_id"],
            name = "index_day_tasks_project_id"
        )
    ]
)
data class DayTaskEntity(
    @PrimaryKey val id: String,
    val date: String?,             // "2026-03-02" or null = backlog
    val name: String,
    @ColumnInfo(name = "planned_pomodoros") val plannedPomodoros: Int,
    @ColumnInfo(name = "completed_pomodoros") val completedPomodoros: Int,
    val position: Int,
    @ColumnInfo(name = "template_id") val templateId: String? = null,
    @ColumnInfo(name = "domain_id") val domainId: String? = null,
    @ColumnInfo(name = "story_points", defaultValue = "0") val storyPoints: Int = 0,
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "user_id", defaultValue = "''") val userId: String = "",
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = System.currentTimeMillis(),
    val impact: String? = null,
    val urgency: String? = null,
    @ColumnInfo(name = "due_date") val dueDate: Long? = null,
    val note: String? = null,
    @ColumnInfo(name = "is_completed", defaultValue = "0") val isCompleted: Boolean = false,
    @ColumnInfo(name = "source", defaultValue = "NULL") val source: String? = null,
    @ColumnInfo(name = "routine_item_id", defaultValue = "NULL") val routineItemId: String? = null,
    @ColumnInfo(name = "project_id") val projectId: String? = null
)
