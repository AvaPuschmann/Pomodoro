package com.agenticfocus.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Sprint 20 / Story 22-2 — Tag entity mobile.
 * Mirror Supabase + desktop SQLite tags table.
 * Schema cohérent : id (uuid), user_id, name, color (#RRGGBB), position, timestamps.
 *
 * tag_ids stockés sur projects pointent vers Tag.id (multi-sélection).
 * Aussi utilisé par TaskTemplate.tag_ids (Library desktop).
 */
@Entity(
    tableName = "tags",
    indices = [Index("user_id")]
)
data class TagEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id", defaultValue = "''")
    val userId: String = "",
    @ColumnInfo(defaultValue = "''")
    val name: String = "",
    @ColumnInfo(defaultValue = "'#6C6C70'")
    val color: String = "#6C6C70",
    @ColumnInfo(defaultValue = "0")
    val position: Int = 0,
    @ColumnInfo(name = "created_at", defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at", defaultValue = "0")
    val updatedAt: Long = System.currentTimeMillis(),
)
