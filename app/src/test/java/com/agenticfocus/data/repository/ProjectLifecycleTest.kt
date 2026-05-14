package com.agenticfocus.data.repository

import com.agenticfocus.data.entity.KanbanStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Story 22-4 / Sprint 20 — Lifecycle automation timestamps.
 * Tests the pure transition function computeLifecycleTimestamps that drives
 * started_at / finished_at on Kanban moves.
 * Parity with desktop projectStore.ts moveKanbanStatus.
 */
class ProjectLifecycleTest {

    private val now = 1_715_000_000_000L

    @Test
    fun `Backlog to Todo sets startedAt to now and leaves finishedAt null`() {
        val (started, finished) = ProjectRepository.computeLifecycleTimestamps(
            oldStatus = KanbanStatus.BACKLOG,
            newStatus = KanbanStatus.TODO,
            existingStartedAt = null,
            existingFinishedAt = null,
            now = now,
        )
        assertEquals(now, started)
        assertNull(finished)
    }

    @Test
    fun `Backlog to Doing sets startedAt to now`() {
        val (started, finished) = ProjectRepository.computeLifecycleTimestamps(
            oldStatus = KanbanStatus.BACKLOG,
            newStatus = KanbanStatus.DOING,
            existingStartedAt = null,
            existingFinishedAt = null,
            now = now,
        )
        assertEquals(now, started)
        assertNull(finished)
    }

    @Test
    fun `Todo to Doing preserves existing startedAt`() {
        val priorStart = 1_700_000_000_000L
        val (started, finished) = ProjectRepository.computeLifecycleTimestamps(
            oldStatus = KanbanStatus.TODO,
            newStatus = KanbanStatus.DOING,
            existingStartedAt = priorStart,
            existingFinishedAt = null,
            now = now,
        )
        assertEquals(priorStart, started)
        assertNull(finished)
    }

    @Test
    fun `Doing to Done sets finishedAt to now and preserves startedAt`() {
        val priorStart = 1_700_000_000_000L
        val (started, finished) = ProjectRepository.computeLifecycleTimestamps(
            oldStatus = KanbanStatus.DOING,
            newStatus = KanbanStatus.DONE,
            existingStartedAt = priorStart,
            existingFinishedAt = null,
            now = now,
        )
        assertEquals(priorStart, started)
        assertEquals(now, finished)
    }

    @Test
    fun `Done rollback to Doing resets finishedAt to null but keeps startedAt`() {
        val priorStart = 1_700_000_000_000L
        val priorFinish = 1_710_000_000_000L
        val (started, finished) = ProjectRepository.computeLifecycleTimestamps(
            oldStatus = KanbanStatus.DONE,
            newStatus = KanbanStatus.DOING,
            existingStartedAt = priorStart,
            existingFinishedAt = priorFinish,
            now = now,
        )
        assertEquals(priorStart, started)
        assertNull(finished)
    }

    @Test
    fun `Done rollback to Todo resets finishedAt to null`() {
        val (started, finished) = ProjectRepository.computeLifecycleTimestamps(
            oldStatus = KanbanStatus.DONE,
            newStatus = KanbanStatus.TODO,
            existingStartedAt = 1_700_000_000_000L,
            existingFinishedAt = 1_710_000_000_000L,
            now = now,
        )
        assertEquals(1_700_000_000_000L, started)
        assertNull(finished)
    }

    @Test
    fun `Doing back to Backlog resets both timestamps to null`() {
        val (started, finished) = ProjectRepository.computeLifecycleTimestamps(
            oldStatus = KanbanStatus.DOING,
            newStatus = KanbanStatus.BACKLOG,
            existingStartedAt = 1_700_000_000_000L,
            existingFinishedAt = null,
            now = now,
        )
        assertNull(started)
        assertNull(finished)
    }

    @Test
    fun `Done back to Backlog resets both timestamps to null`() {
        val (started, finished) = ProjectRepository.computeLifecycleTimestamps(
            oldStatus = KanbanStatus.DONE,
            newStatus = KanbanStatus.BACKLOG,
            existingStartedAt = 1_700_000_000_000L,
            existingFinishedAt = 1_710_000_000_000L,
            now = now,
        )
        assertNull(started)
        assertNull(finished)
    }

    @Test
    fun `Idempotent transition same status preserves timestamps`() {
        val priorStart = 1_700_000_000_000L
        val priorFinish = 1_710_000_000_000L
        val (started, finished) = ProjectRepository.computeLifecycleTimestamps(
            oldStatus = KanbanStatus.DONE,
            newStatus = KanbanStatus.DONE,
            existingStartedAt = priorStart,
            existingFinishedAt = priorFinish,
            now = now,
        )
        assertEquals(priorStart, started)
        assertEquals(priorFinish, finished)
    }

    @Test
    fun `Todo to Done sets finishedAt and preserves startedAt`() {
        val priorStart = 1_700_000_000_000L
        val (started, finished) = ProjectRepository.computeLifecycleTimestamps(
            oldStatus = KanbanStatus.TODO,
            newStatus = KanbanStatus.DONE,
            existingStartedAt = priorStart,
            existingFinishedAt = null,
            now = now,
        )
        assertEquals(priorStart, started)
        assertEquals(now, finished)
    }
}
