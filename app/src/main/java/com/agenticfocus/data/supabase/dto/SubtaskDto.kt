package com.agenticfocus.data.supabase.dto

import com.agenticfocus.data.entity.SubtaskEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubtaskDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("task_id") val taskId: String,
    val title: String,
    @SerialName("is_completed") val isCompleted: Boolean,
    val position: Int,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

fun SubtaskDto.toEntity() = SubtaskEntity(
    id = id,
    userId = userId,
    taskId = taskId,
    title = title,
    isCompleted = isCompleted,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun SubtaskEntity.toDto(userId: String) = SubtaskDto(
    id = id,
    userId = userId,
    taskId = taskId,
    title = title,
    isCompleted = isCompleted,
    position = position,
    createdAt = createdAt,
    updatedAt = System.currentTimeMillis()
)
