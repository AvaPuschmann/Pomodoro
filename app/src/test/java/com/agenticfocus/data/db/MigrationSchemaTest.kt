package com.agenticfocus.data.db

import com.agenticfocus.data.entity.DayTaskEntity
import com.agenticfocus.data.entity.DomainEntity
import com.agenticfocus.data.entity.PomodoroSessionEntity
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
}
