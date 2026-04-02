package com.agenticfocus.data.supabase.dto

import com.agenticfocus.data.entity.PomodoroSessionEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PomodoroSessionDto(
    val id: String,
    @SerialName("day_task_id") val dayTaskId: String,
    val date: String,
    @SerialName("start_time") val startTime: Long,
    @SerialName("end_time") val endTime: Long,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("user_id") val userId: String,
    @SerialName("updated_at") val updatedAt: Long
)

fun PomodoroSessionDto.toEntity() = PomodoroSessionEntity(
    id = id, dayTaskId = dayTaskId, date = date,
    startTime = startTime, endTime = endTime, durationMinutes = durationMinutes,
    userId = userId, updatedAt = updatedAt
)

fun PomodoroSessionEntity.toDto(userId: String) = PomodoroSessionDto(
    id = id,
    dayTaskId = dayTaskId,
    date = date,
    startTime = startTime,
    endTime = endTime,
    durationMinutes = durationMinutes,
    userId = userId,
    updatedAt = System.currentTimeMillis()
)
