package com.agenticfocus.data.supabase.dto

import com.agenticfocus.data.entity.GoalEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoalDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: String,
    @SerialName("period_key") val periodKey: String,
    val text: String,
    // Supabase stores this as BOOLEAN — convert to 0/1 for Room
    @SerialName("is_completed") val isCompleted: Boolean,
    // PAS de valeur par défaut : sinon kotlinx omet le champ quand il vaut false
    // (encodeDefaults=false) → l'upsert ne réinitialise jamais is_missed côté Supabase
    // → un objectif "raté" reste bloqué en rouge. La colonne est NOT NULL DEFAULT false.
    @SerialName("is_missed") val isMissed: Boolean,  // tri-état raté
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

fun GoalDto.toEntity() = GoalEntity(
    id = id,
    userId = userId,
    type = type,
    periodKey = periodKey,
    text = text,
    isCompleted = if (isCompleted) 1 else 0,
    isMissed = if (isMissed) 1 else 0,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun GoalEntity.toDto(userId: String) = GoalDto(
    id = id,
    userId = userId,
    type = type,
    periodKey = periodKey,
    text = text,
    isCompleted = isCompleted == 1,
    isMissed = isMissed == 1,
    createdAt = createdAt,
    updatedAt = System.currentTimeMillis()
)
