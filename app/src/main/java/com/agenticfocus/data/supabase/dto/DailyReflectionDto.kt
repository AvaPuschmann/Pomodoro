package com.agenticfocus.data.supabase.dto

import com.agenticfocus.data.entity.DailyReflectionEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sprint 22 / Story 24-1 — Supabase DTO for daily_reflections table.
 *
 * Pattern projet (cf. TagDto, GoalDto, SubtaskDto, ProjectDto, RoutineDto):
 * - Timestamps stored as BIGINT (Unix ms) — NOT TIMESTAMPTZ / ISO8601 String
 * - id as TEXT primary key (UUID generated Kotlin-side, Supabase doesn't auto-gen)
 * - user_id as TEXT (matches auth.uid()::text in RLS policy)
 *
 * Includes all 11 columns (id + user_id + period_key + day_facts + learning + 4 word/char counts
 * + created_at + updated_at). Mapper completeness enforced per feedback_desktop_sync_mappers.md.
 */
@Serializable
data class DailyReflectionDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("period_key") val periodKey: String,
    @SerialName("day_facts") val dayFacts: String = "",
    val learning: String = "",
    @SerialName("day_facts_word_count") val dayFactsWordCount: Int = 0,
    @SerialName("day_facts_char_count") val dayFactsCharCount: Int = 0,
    @SerialName("learning_word_count") val learningWordCount: Int = 0,
    @SerialName("learning_char_count") val learningCharCount: Int = 0,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
) {
    fun toEntity(): DailyReflectionEntity = DailyReflectionEntity(
        id = id,
        userId = userId,
        periodKey = periodKey,
        dayFacts = dayFacts,
        learning = learning,
        dayFactsWordCount = dayFactsWordCount,
        dayFactsCharCount = dayFactsCharCount,
        learningWordCount = learningWordCount,
        learningCharCount = learningCharCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun DailyReflectionEntity.toDto(): DailyReflectionDto = DailyReflectionDto(
    id = id,
    userId = userId,
    periodKey = periodKey,
    dayFacts = dayFacts,
    learning = learning,
    dayFactsWordCount = dayFactsWordCount,
    dayFactsCharCount = dayFactsCharCount,
    learningWordCount = learningWordCount,
    learningCharCount = learningCharCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
