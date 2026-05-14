package com.agenticfocus.data.supabase.dto

import com.agenticfocus.data.entity.TagEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sprint 20 / Story 22-2 — Supabase DTO for tags table.
 * Cohérent avec desktop Tag interface (types/Domain.ts).
 */
@Serializable
data class TagDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val color: String,
    val position: Int = 0,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
) {
    fun toEntity(): TagEntity = TagEntity(
        id = id,
        userId = userId,
        name = name,
        color = color,
        position = position,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun TagEntity.toDto(): TagDto = TagDto(
    id = id,
    userId = userId,
    name = name,
    color = color,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
