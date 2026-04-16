package com.agenticfocus.data.supabase.dto

import com.agenticfocus.data.entity.RoutineItemEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoutineItemDto(
    val id: String,
    @SerialName("routine_id") val routineId: String,
    @SerialName("template_id") val templateId: String? = null,
    @SerialName("snapshot_title") val snapshotTitle: String,
    @SerialName("snapshot_domain_id") val snapshotDomainId: String? = null,
    @SerialName("snapshot_story_points") val snapshotStoryPoints: Int = 0,
    @SerialName("override_pomodoros") val overridePomodoros: Int? = null,
    @SerialName("is_check_only") val isCheckOnly: Boolean = false,
    val position: Int = 0,
    @SerialName("user_id") val userId: String,
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("updated_at") val updatedAt: Long = 0
) {
    fun toEntity() = RoutineItemEntity(
        id = id,
        routineId = routineId,
        templateId = templateId,
        snapshotTitle = snapshotTitle,
        snapshotDomainId = snapshotDomainId,
        snapshotStoryPoints = snapshotStoryPoints,
        overridePomodoros = overridePomodoros,
        isCheckOnly = if (isCheckOnly) 1 else 0,
        position = position,
        userId = userId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun RoutineItemEntity.toDto() = RoutineItemDto(
    id = id,
    routineId = routineId,
    templateId = templateId,
    snapshotTitle = snapshotTitle,
    snapshotDomainId = snapshotDomainId,
    snapshotStoryPoints = snapshotStoryPoints,
    overridePomodoros = overridePomodoros,
    isCheckOnly = isCheckOnly == 1,
    position = position,
    userId = userId,
    createdAt = createdAt,
    updatedAt = updatedAt
)
