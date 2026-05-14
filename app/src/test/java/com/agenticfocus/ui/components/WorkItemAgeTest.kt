package com.agenticfocus.ui.components

import com.agenticfocus.data.entity.KanbanStatus
import com.agenticfocus.data.entity.ProjectEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Story 22-3 / Sprint 20 — Pure tests for WorkItemAge helpers.
 */
class WorkItemAgeTest {

    private val now = 1_715_000_000_000L
    private val MS_DAY = 1000L * 60 * 60 * 24

    @Test
    fun `ageBucket green when days less than threshold`() {
        val (bucket, days) = ageBucket(3 * MS_DAY, WiaThresholds(green = 7, yellow = 21, orange = 45))
        assertEquals(AgeBucket.GREEN, bucket)
        assertEquals(3, days)
    }

    @Test
    fun `ageBucket yellow in green-yellow range`() {
        val (bucket, days) = ageBucket(10 * MS_DAY, WiaThresholds(green = 7, yellow = 21, orange = 45))
        assertEquals(AgeBucket.YELLOW, bucket)
        assertEquals(10, days)
    }

    @Test
    fun `ageBucket orange in yellow-orange range`() {
        val (bucket, _) = ageBucket(30 * MS_DAY, WiaThresholds(green = 7, yellow = 21, orange = 45))
        assertEquals(AgeBucket.ORANGE, bucket)
    }

    @Test
    fun `ageBucket red above orange threshold`() {
        val (bucket, _) = ageBucket(60 * MS_DAY, WiaThresholds(green = 7, yellow = 21, orange = 45))
        assertEquals(AgeBucket.RED, bucket)
    }

    @Test
    fun `formatDays returns less than 1h for sub-hour duration`() {
        assertEquals("< 1h", formatDays(30 * 60 * 1000L))
    }

    @Test
    fun `formatDays returns hours for sub-day duration`() {
        assertEquals("3h", formatDays(3 * 60 * 60 * 1000L))
    }

    @Test
    fun `formatDays returns days for multi-day duration`() {
        assertEquals("12j", formatDays(12 * MS_DAY))
    }

    @Test
    fun `computeAge for Backlog uses now minus createdAt`() {
        val project = ProjectEntity(
            id = "p1", kanbanStatus = KanbanStatus.BACKLOG,
            createdAt = now - 5 * MS_DAY, updatedAt = now,
        )
        assertEquals(5 * MS_DAY, computeAge(project, now))
    }

    @Test
    fun `computeAge for Doing uses now minus startedAt`() {
        val project = ProjectEntity(
            id = "p1", kanbanStatus = KanbanStatus.DOING,
            createdAt = now - 10 * MS_DAY, updatedAt = now,
            startedAt = now - 3 * MS_DAY,
        )
        assertEquals(3 * MS_DAY, computeAge(project, now))
    }

    @Test
    fun `computeAge for Done uses finishedAt minus startedAt (cycle time)`() {
        val project = ProjectEntity(
            id = "p1", kanbanStatus = KanbanStatus.DONE,
            createdAt = now - 10 * MS_DAY, updatedAt = now,
            startedAt = now - 8 * MS_DAY,
            finishedAt = now - 3 * MS_DAY,
        )
        // Cycle time = 8 - 3 = 5 days
        assertEquals(5 * MS_DAY, computeAge(project, now))
    }

    @Test
    fun `computeAge for Doing fallback createdAt when startedAt null`() {
        val project = ProjectEntity(
            id = "p1", kanbanStatus = KanbanStatus.DOING,
            createdAt = now - 4 * MS_DAY, updatedAt = now,
            startedAt = null,
        )
        assertEquals(4 * MS_DAY, computeAge(project, now))
    }

    @Test
    fun `computeColumnStats handles empty list`() {
        val stats = computeColumnStats(emptyList(), now)
        assertEquals(0, stats.count)
        assertEquals(0.0, stats.avgDays, 0.001)
    }

    @Test
    fun `computeColumnStats computes avg and oldest correctly`() {
        val projects = listOf(
            ProjectEntity(id = "p1", kanbanStatus = KanbanStatus.DOING, createdAt = now - 10 * MS_DAY, updatedAt = now, startedAt = now - 2 * MS_DAY),
            ProjectEntity(id = "p2", kanbanStatus = KanbanStatus.DOING, createdAt = now - 10 * MS_DAY, updatedAt = now, startedAt = now - 8 * MS_DAY),
            ProjectEntity(id = "p3", kanbanStatus = KanbanStatus.DOING, createdAt = now - 10 * MS_DAY, updatedAt = now, startedAt = now - 5 * MS_DAY),
        )
        val stats = computeColumnStats(projects, now)
        assertEquals(3, stats.count)
        // ages = 2, 5, 8 — avg = 5.0, oldest = 8.0
        assertEquals(5.0, stats.avgDays, 0.01)
        assertEquals(8.0, stats.oldestDays, 0.01)
        assertEquals(5.0, stats.p50Days, 0.01)
        // p95 interpolated between idx 1.9 = 0.9 between 5 and 8 = 7.7
        assertTrue("p95 should be ~7.7", stats.p95Days in 7.0..8.0)
    }
}
