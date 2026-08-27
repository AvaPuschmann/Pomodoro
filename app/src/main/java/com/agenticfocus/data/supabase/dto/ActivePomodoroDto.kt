package com.agenticfocus.data.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Write DTO — no defaults so every field is always serialized (encodeDefaults=false guard)
@Serializable
data class ActivePomodoroDto(
    @SerialName("user_id") val userId: String,
    @SerialName("task_id") val taskId: String?,
    @SerialName("task_name") val taskName: String?,
    @SerialName("platform") val platform: String,
    @SerialName("session_type") val sessionType: String,
    @SerialName("started_at") val startedAt: Long?,
    @SerialName("planned_duration_ms") val plannedDurationMs: Long?,
    @SerialName("updated_at") val updatedAt: Long,
)

// Read DTO — with defaults so missing/null columns don't crash decodeList
@Serializable
data class ActivePomodoroReadDto(
    @SerialName("user_id") val userId: String,
    @SerialName("task_id") val taskId: String? = null,
    @SerialName("task_name") val taskName: String? = null,
    @SerialName("platform") val platform: String = "",
    @SerialName("session_type") val sessionType: String? = null,
    @SerialName("started_at") val startedAt: Long? = null,
    @SerialName("planned_duration_ms") val plannedDurationMs: Long? = null,
    @SerialName("updated_at") val updatedAt: Long? = null,
)
