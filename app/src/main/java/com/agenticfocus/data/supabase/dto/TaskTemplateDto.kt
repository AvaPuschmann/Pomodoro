package com.agenticfocus.data.supabase.dto

import com.agenticfocus.data.entity.TaskTemplateEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskTemplateDto(
    val id: String,
    val title: String,
    val note: String?,
    @SerialName("domain_id") val domainId: String,
    @SerialName("story_points") val storyPoints: Int,
    @SerialName("default_pomodoros") val defaultPomodoros: Int,
    @SerialName("user_id") val userId: String,
    @SerialName("updated_at") val updatedAt: Long
)

fun TaskTemplateDto.toEntity() = TaskTemplateEntity(
    id = id, title = title, note = note, domainId = domainId,
    storyPoints = storyPoints, defaultPomodoros = defaultPomodoros,
    userId = userId, updatedAt = updatedAt
)

fun TaskTemplateEntity.toDto(userId: String) = TaskTemplateDto(
    id = id,
    title = title,
    note = note,
    domainId = domainId,
    storyPoints = storyPoints,
    defaultPomodoros = defaultPomodoros,
    userId = userId,
    updatedAt = System.currentTimeMillis()
)
