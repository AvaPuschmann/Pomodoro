package com.agenticfocus.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goals",
    indices = [Index(value = ["user_id", "type", "period_key"], unique = true)]
)
data class GoalEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id", defaultValue = "") val userId: String = "",
    @ColumnInfo(defaultValue = "") val type: String = "day",       // "day" | "week" | "month"
    @ColumnInfo(name = "period_key", defaultValue = "") val periodKey: String = "",
    @ColumnInfo(defaultValue = "") val text: String = "",
    @ColumnInfo(name = "is_completed", defaultValue = "0") val isCompleted: Int = 0,
    @ColumnInfo(name = "is_missed", defaultValue = "0") val isMissed: Int = 0,  // tri-état : raté explicite
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = System.currentTimeMillis()
)
