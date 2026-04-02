package com.agenticfocus.data.supabase.dto

import com.agenticfocus.data.entity.DomainEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DomainDto(
    val id: String,
    val name: String,
    val color: String,
    @SerialName("user_id") val userId: String,
    @SerialName("updated_at") val updatedAt: Long
)

fun DomainDto.toEntity() = DomainEntity(
    id = id, name = name, color = color, userId = userId, updatedAt = updatedAt
)

fun DomainEntity.toDto(userId: String) = DomainDto(
    id = id,
    name = name,
    color = color,
    userId = userId,
    updatedAt = System.currentTimeMillis()
)
