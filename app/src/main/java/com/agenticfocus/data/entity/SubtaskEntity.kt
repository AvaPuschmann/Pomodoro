package com.agenticfocus.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subtasks",
    indices = [Index(value = ["task_id"])]
)
data class SubtaskEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id", defaultValue = "") val userId: String = "",
    @ColumnInfo(name = "task_id") val taskId: String,
    val title: String,
    @ColumnInfo(name = "is_completed", defaultValue = "0") val isCompleted: Boolean = false,
    @ColumnInfo(defaultValue = "0") val position: Int = 0,
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = System.currentTimeMillis()
)
