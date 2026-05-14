package com.agenticfocus.data.db

import com.agenticfocus.data.entity.DayTaskEntity
import com.agenticfocus.data.entity.DomainEntity
import com.agenticfocus.data.entity.PomodoroSessionEntity
import com.agenticfocus.data.entity.ProjectEntity
import com.agenticfocus.data.entity.TaskTemplateEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class MigrationSchemaTest {

    @Test
    fun `DayTaskEntity has user_id defaulting to empty string`() {
        val entity = DayTaskEntity(
            id = "task-1",
            date = "2026-03-19",
            name = "Test Task",
            plannedPomodoros = 2,
            completedPomodoros = 0,
            position = 0
        )
        assertEquals("", entity.userId)
    }

    @Test
    fun `DayTaskEntity has updated_at defaulting to non-zero`() {
        val before = System.currentTimeMillis()
        val entity = DayTaskEntity(
            id = "task-1",
            date = "2026-03-19",
            name = "Test Task",
            plannedPomodoros = 2,
            completedPomodoros = 0,
            position = 0
        )
        val after = System.currentTimeMillis()
        assertTrue(entity.updatedAt in before..after)
    }

    @Test
    fun `PomodoroSessionEntity id is String UUID by default`() {
        val entity = PomodoroSessionEntity(
            dayTaskId = "task-1",
            date = "2026-03-19",
            startTime = 1_000_000L,
            endTime = 1_025_000L
        )
        // Must be a valid UUID — fromString throws IllegalArgumentException if not
        val parsed = UUID.fromString(entity.id)
        assertNotNull(parsed)
        assertEquals(36, entity.id.length) // UUID format: 8-4-4-4-12 = 36 chars
    }

    @Test
    fun `PomodoroSessionEntity has user_id defaulting to empty string`() {
        val entity = PomodoroSessionEntity(
            dayTaskId = "task-1",
            date = "2026-03-19",
            startTime = 0L,
            endTime = 1000L
        )
        assertEquals("", entity.userId)
    }

    @Test
    fun `Two PomodoroSessionEntity instances have distinct default ids`() {
        val e1 = PomodoroSessionEntity(dayTaskId = "t1", date = "2026-03-19", startTime = 0L, endTime = 1L)
        val e2 = PomodoroSessionEntity(dayTaskId = "t1", date = "2026-03-19", startTime = 0L, endTime = 1L)
        assertTrue(e1.id != e2.id)
    }

    @Test
    fun `DomainEntity has user_id and updated_at with defaults`() {
        val entity = DomainEntity(id = "d1", name = "Sport", color = "#4CAF50")
        assertEquals("", entity.userId)
        assertTrue(entity.updatedAt > 0L)
    }

    @Test
    fun `TaskTemplateEntity has user_id and updated_at with defaults`() {
        val entity = TaskTemplateEntity(
            id = "t1",
            title = "Run 5km",
            domainId = "d1",
            storyPoints = 3,
            defaultPomodoros = 2
        )
        assertEquals("", entity.userId)
        assertTrue(entity.updatedAt > 0L)
    }

    @Test
    fun `TaskTemplateEntity Kotlin property names unchanged for repository compatibility`() {
        val entity = TaskTemplateEntity(
            id = "t1",
            title = "Run",
            domainId = "d1",
            storyPoints = 5,
            defaultPomodoros = 3
        )
        // Verify camelCase property names still accessible (repository compatibility)
        assertEquals("d1", entity.domainId)
        assertEquals(5, entity.storyPoints)
        assertEquals(3, entity.defaultPomodoros)
    }

    @Test
    fun `DayTaskEntity Kotlin property names unchanged for repository compatibility`() {
        val entity = DayTaskEntity(
            id = "task-1", date = "2026-03-19", name = "Test",
            plannedPomodoros = 4, completedPomodoros = 2, position = 0,
            templateId = "tmpl-1"
        )
        assertEquals(4, entity.plannedPomodoros)
        assertEquals(2, entity.completedPomodoros)
        assertEquals("tmpl-1", entity.templateId)
    }

    // ── Story 22-1 / Sprint 20 — ProjectEntity v17 schema additions ─────────

    @Test
    fun `ProjectEntity v17 defaults tag_ids to empty list`() {
        val entity = ProjectEntity(id = "p1")
        assertEquals(emptyList<String>(), entity.tagIds)
    }

    @Test
    fun `ProjectEntity v17 defaults started_at to null`() {
        val entity = ProjectEntity(id = "p1")
        assertEquals(null, entity.startedAt)
    }

    @Test
    fun `ProjectEntity v17 defaults finished_at to null`() {
        val entity = ProjectEntity(id = "p1")
        assertEquals(null, entity.finishedAt)
    }

    @Test
    fun `ProjectEntity v17 preserves all v16 fields`() {
        val entity = ProjectEntity(
            id = "p1", userId = "u1", name = "Mon projet",
            description = "Desc", kanbanStatus = "doing", kanbanPosition = 1024.0,
            targetDate = 1_700_000_000_000L, domainId = "d1", isArchived = false,
            tagIds = listOf("t1", "t2"), startedAt = 1_710_000_000_000L,
            finishedAt = null
        )
        assertEquals("p1", entity.id)
        assertEquals("u1", entity.userId)
        assertEquals("Mon projet", entity.name)
        assertEquals("doing", entity.kanbanStatus)
        assertEquals(1024.0, entity.kanbanPosition, 0.001)
        assertEquals(listOf("t1", "t2"), entity.tagIds)
        assertEquals(1_710_000_000_000L, entity.startedAt)
        assertEquals(null, entity.finishedAt)
    }

    // ── TagIdsListConverter round-trip ──────────────────────────────────────

    @Test
    fun `TagIdsListConverter roundtrip preserves list`() {
        val conv = TagIdsListConverter()
        val original = listOf("uuid-1", "uuid-2", "uuid-3")
        val json = conv.fromList(original)
        val back = conv.toList(json)
        assertEquals(original, back)
    }

    @Test
    fun `TagIdsListConverter handles empty list`() {
        val conv = TagIdsListConverter()
        assertEquals(emptyList<String>(), conv.toList(conv.fromList(emptyList())))
    }

    @Test
    fun `TagIdsListConverter returns empty list on blank input`() {
        val conv = TagIdsListConverter()
        assertEquals(emptyList<String>(), conv.toList(""))
    }

    @Test
    fun `TagIdsListConverter returns empty list on malformed input`() {
        val conv = TagIdsListConverter()
        assertEquals(emptyList<String>(), conv.toList("not-json"))
    }
}
