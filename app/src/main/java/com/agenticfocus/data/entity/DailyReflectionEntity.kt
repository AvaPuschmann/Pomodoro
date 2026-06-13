package com.agenticfocus.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Daily reflection entry — Bilan du Jour (Story 24-1 Sprint 22).
 *
 * Captures Philippe's end-of-day ritual: 2 narrative fields per day (Day Facts + Learning).
 * 1 entry per (user_id, period_key) — enforced via unique index.
 *
 * Word/char counts are computed Kotlin-side at save time (Repository.saveReflection)
 * to enable Phase 3 Analytics queries without recomputing on read.
 *
 * @see com.agenticfocus.data.dao.DailyReflectionDao
 * @see com.agenticfocus.data.repository.DailyReflectionRepository
 */
@Entity(
    tableName = "daily_reflections",
    indices = [Index(value = ["user_id", "period_key"], unique = true)]
)
data class DailyReflectionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id", defaultValue = "") val userId: String = "",
    @ColumnInfo(name = "period_key", defaultValue = "") val periodKey: String = "", // YYYY-MM-DD
    @ColumnInfo(name = "day_facts", defaultValue = "") val dayFacts: String = "",
    @ColumnInfo(name = "learning", defaultValue = "") val learning: String = "",
    @ColumnInfo(name = "day_facts_word_count", defaultValue = "0") val dayFactsWordCount: Int = 0,
    @ColumnInfo(name = "day_facts_char_count", defaultValue = "0") val dayFactsCharCount: Int = 0,
    @ColumnInfo(name = "learning_word_count", defaultValue = "0") val learningWordCount: Int = 0,
    @ColumnInfo(name = "learning_char_count", defaultValue = "0") val learningCharCount: Int = 0,
    @ColumnInfo(name = "created_at", defaultValue = "0") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = System.currentTimeMillis()
)
