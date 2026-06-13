package com.agenticfocus.data.supabase.dto

import com.agenticfocus.data.entity.DailyReflectionEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Story 24-1 / Sprint 22 / Epic 24 — Test mapper round-trip (Party Mode 2026-05-21 décision Q-Murat).
 *
 * Empêche le bug type `feedback_desktop_sync_mappers.md` (451 routines dupliquées via mapper foireux).
 *
 * Coverage :
 * - Entity → DTO → Entity équivalence (tous les 11 champs)
 * - Variantes : vides/nulls/edges, valeurs typiques, valeurs extrêmes
 * - JSON round-trip (encode/decode kotlinx.serialization) — celui qu'utilise SyncEngine.replayUpsert
 */
class DailyReflectionDtoMapperTest {

    @Test
    fun `entity to dto to entity roundtrip preserves all fields`() {
        val original = DailyReflectionEntity(
            id = "550e8400-e29b-41d4-a716-446655440000",
            userId = "d9dd9a28-ec9c-4a67-9253-f673021c1611",
            periodKey = "2026-05-21",
            dayFacts = "Le moment où l'équipe a éclaté de rire pendant la revue de sprint. Ma fille m'a appelé en pleine session de focus, j'ai répondu — et c'était la bonne décision.",
            learning = "J'ai compris que StateFlow ne ré-émet pas la même valeur — explique pourquoi le test Turbine échouait depuis 3 jours.",
            dayFactsWordCount = 30,
            dayFactsCharCount = 173,
            learningWordCount = 21,
            learningCharCount = 131,
            createdAt = 1779349439_000L,
            updatedAt = 1779353039_000L,
        )

        val roundtrip = original.toDto().toEntity()

        assertEquals(original, roundtrip)
    }

    @Test
    fun `roundtrip preserves all fields with empty narrative values`() {
        val original = DailyReflectionEntity(
            id = "11111111-2222-3333-4444-555555555555",
            userId = "user-1",
            periodKey = "2026-05-21",
            dayFacts = "",
            learning = "",
            dayFactsWordCount = 0,
            dayFactsCharCount = 0,
            learningWordCount = 0,
            learningCharCount = 0,
            createdAt = 1L,
            updatedAt = 2L,
        )

        val roundtrip = original.toDto().toEntity()

        assertEquals(original, roundtrip)
    }

    @Test
    fun `roundtrip preserves unicode emojis and multiline whitespace`() {
        val original = DailyReflectionEntity(
            id = "uuid-emoji-test",
            userId = "user-2",
            periodKey = "2026-05-21",
            dayFacts = "Aujourd'hui 🌅\n\n- Course Maxi Race 🏃\n- POC MCP livré 🚀🚀\n\nÉmotionnel : fierté.",
            learning = "@modelcontextprotocol/sdk supporte stdio + HTTP/SSE 🔌\n\tIndentations préservées.",
            dayFactsWordCount = 12,
            dayFactsCharCount = 90,
            learningWordCount = 8,
            learningCharCount = 70,
            createdAt = 1779_000_000_000L,
            updatedAt = 1779_001_000_000L,
        )

        val roundtrip = original.toDto().toEntity()

        assertEquals(original, roundtrip)
    }

    @Test
    fun `JSON encode-decode roundtrip preserves all fields (SyncEngine replayUpsert path)`() {
        val original = DailyReflectionEntity(
            id = "json-test",
            userId = "user-3",
            periodKey = "2026-05-21",
            dayFacts = "Day facts simple",
            learning = "Learning simple",
            dayFactsWordCount = 3,
            dayFactsCharCount = 16,
            learningWordCount = 2,
            learningCharCount = 15,
            createdAt = 100L,
            updatedAt = 200L,
        )

        val dto = original.toDto()
        val json = Json.encodeToString(dto)
        val decodedDto = Json.decodeFromString<DailyReflectionDto>(json)
        val roundtrip = decodedDto.toEntity()

        assertEquals(original, roundtrip)
    }

    @Test
    fun `dto field defaults are zero-safe for new untouched entries`() {
        val dto = DailyReflectionDto(
            id = "default-test",
            userId = "user-4",
            periodKey = "2026-05-21",
        )

        // Default values per @Serializable should be safe defaults (empty strings, 0 counts, 0 timestamps)
        assertEquals("", dto.dayFacts)
        assertEquals("", dto.learning)
        assertEquals(0, dto.dayFactsWordCount)
        assertEquals(0, dto.dayFactsCharCount)
        assertEquals(0, dto.learningWordCount)
        assertEquals(0, dto.learningCharCount)
        assertEquals(0L, dto.createdAt)
        assertEquals(0L, dto.updatedAt)
    }
}
