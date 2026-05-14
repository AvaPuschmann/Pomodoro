package com.agenticfocus.data.supabase.dto

import com.agenticfocus.data.entity.ProjectEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProjectDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val description: String? = null,
    @SerialName("kanban_status") val kanbanStatus: String,
    @SerialName("kanban_position") val kanbanPosition: Double,
    @SerialName("target_date") val targetDate: Long? = null,
    @SerialName("domain_id") val domainId: String? = null,
    @SerialName("is_archived") val isArchived: Boolean,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    // Sprint 20 / Story 22-1 — Mode Projet v1.1 enrichments
    @SerialName("tag_ids") val tagIds: List<String> = emptyList(),
    @SerialName("started_at") val startedAt: Long? = null,
    @SerialName("finished_at") val finishedAt: Long? = null,
) {
    fun toEntity(): ProjectEntity = ProjectEntity(
        id = id,
        userId = userId,
        name = name,
        description = description,
        kanbanStatus = kanbanStatus,
        kanbanPosition = kanbanPosition,
        targetDate = targetDate,
        domainId = domainId,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt,
        tagIds = tagIds,
        startedAt = startedAt,
        finishedAt = finishedAt,
    )
}

fun ProjectEntity.toDto(): ProjectDto = ProjectDto(
    id = id,
    userId = userId,
    name = name,
    description = description,
    kanbanStatus = kanbanStatus,
    kanbanPosition = kanbanPosition,
    targetDate = targetDate,
    domainId = domainId,
    isArchived = isArchived,
    createdAt = createdAt,
    updatedAt = updatedAt,
    tagIds = tagIds,
    startedAt = startedAt,
    finishedAt = finishedAt,
)
