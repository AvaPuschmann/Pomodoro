package com.agenticfocus.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// NOTE: @ColumnInfo(defaultValue=…) and indices declarations must mirror what
// MIGRATION_15_16 + MIGRATION_16_17 create in SQL — otherwise Room's runtime
// schema validation crashes the app at startup with "Migration didn't properly
// handle: projects" on devices upgrading.
// Story 16-4 / Sprint 16 / 2026-05-11 (initial).
// Story 22-1 / Sprint 20 / 2026-05-14 — added tag_ids + started_at + finished_at.
@Entity(
    tableName = "projects",
    indices = [Index("user_id")]
)
data class ProjectEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id", defaultValue = "''")
    val userId: String = "",
    @ColumnInfo(defaultValue = "''")
    val name: String = "",
    val description: String? = null,
    @ColumnInfo(name = "kanban_status", defaultValue = "'backlog'")
    val kanbanStatus: String = "backlog",
    @ColumnInfo(name = "kanban_position", defaultValue = "0")
    val kanbanPosition: Double = 0.0,
    @ColumnInfo(name = "target_date")
    val targetDate: Long? = null,
    @ColumnInfo(name = "domain_id")
    val domainId: String? = null,
    @ColumnInfo(name = "is_archived", defaultValue = "0")
    val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at", defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at", defaultValue = "0")
    val updatedAt: Long = System.currentTimeMillis(),
    // Sprint 20 / Story 22-1 — Mode Projet v1.1 enrichments (parity with desktop Sprint 19)
    @ColumnInfo(name = "tag_ids", defaultValue = "'[]'")
    val tagIds: List<String> = emptyList(),
    @ColumnInfo(name = "started_at")
    val startedAt: Long? = null,
    @ColumnInfo(name = "finished_at")
    val finishedAt: Long? = null
)
