package com.agenticfocus.data.supabase.dto

import com.agenticfocus.data.entity.RoutineEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoutineDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: String,
    val name: String = "",
    @SerialName("trigger_time") val triggerTime: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("streak_count") val streakCount: Int = 0,
    @SerialName("streak_last_completed_date") val streakLastCompletedDate: String? = null,
    @SerialName("last_injected_date") val lastInjectedDate: String? = null,
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("updated_at") val updatedAt: Long = 0
) {
    fun toEntity() = RoutineEntity(
        id = id,
        userId = userId,
        type = type,
        name = name,
        triggerTime = triggerTime,
        isActive = if (isActive) 1 else 0,
        streakCount = streakCount,
        streakLastCompletedDate = streakLastCompletedDate,
        lastInjectedDate = lastInjectedDate,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun RoutineEntity.toDto() = RoutineDto(
    id = id,
    userId = userId,
    type = type,
    name = name,
    triggerTime = triggerTime,
    isActive = isActive == 1,
    streakCount = streakCount,
    streakLastCompletedDate = streakLastCompletedDate,
    lastInjectedDate = lastInjectedDate,
    createdAt = createdAt,
    updatedAt = updatedAt
)
