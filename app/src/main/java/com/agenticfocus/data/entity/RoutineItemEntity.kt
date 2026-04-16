package com.agenticfocus.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routine_items",
    indices = [
        Index("routine_id", "position"),
        Index("template_id")
    ],
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RoutineItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "routine_id") val routineId: String,
    @ColumnInfo(name = "template_id") val templateId: String?,
    @ColumnInfo(name = "snapshot_title") val snapshotTitle: String,
    @ColumnInfo(name = "snapshot_domain_id") val snapshotDomainId: String?,
    @ColumnInfo(name = "snapshot_story_points", defaultValue = "0") val snapshotStoryPoints: Int,
    @ColumnInfo(name = "override_pomodoros") val overridePomodoros: Int?,
    @ColumnInfo(name = "is_check_only", defaultValue = "0") val isCheckOnly: Int,
    @ColumnInfo(name = "position", defaultValue = "0") val position: Int,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long,
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long
)
