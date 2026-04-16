package com.agenticfocus.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routines",
    indices = [Index("user_id", "type")]
)
data class RoutineEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "name", defaultValue = "") val name: String,
    @ColumnInfo(name = "trigger_time") val triggerTime: String,
    @ColumnInfo(name = "is_active", defaultValue = "1") val isActive: Int,
    @ColumnInfo(name = "streak_count", defaultValue = "0") val streakCount: Int,
    @ColumnInfo(name = "streak_last_completed_date") val streakLastCompletedDate: String?,
    @ColumnInfo(name = "last_injected_date") val lastInjectedDate: String?,
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long
)
