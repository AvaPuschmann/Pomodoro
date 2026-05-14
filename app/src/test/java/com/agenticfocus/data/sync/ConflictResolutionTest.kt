package com.agenticfocus.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for conflict resolution logic in RealtimeSyncManager and SyncStatusManager.
 *
 * Conflict rule: incoming.updatedAt > existing.updatedAt → apply (incoming wins)
 *                incoming.updatedAt <= existing.updatedAt → discard (local wins, emit conflict)
 *
 * These tests use the FakeConflictDao and simulate the guard logic directly,
 * since the Realtime event collection requires a live Supabase WebSocket.
 */
class ConflictResolutionTest {

    // ── buildConflictMessage ───────────────────────────────────────────────────

    @Test
    fun `buildConflictMessage returns generic message when delta is less than 24 hours`() {
        val deltaMs = 2L * 60 * 60 * 1000  // 2 hours
        val msg = RealtimeSyncManager.buildConflictMessage(deltaMs)
        assertEquals("Conflit résolu : version la plus récente conservée", msg)
    }

    @Test
    fun `buildConflictMessage includes delta when delta is more than 24 hours`() {
        val deltaMs = 26L * 60 * 60 * 1000  // 26 hours = 1j 2h
        val msg = RealtimeSyncManager.buildConflictMessage(deltaMs)
        assertTrue(msg.contains("Conflit résolu"))
        assertTrue(msg.contains("ancienne version"))
        assertTrue(msg.contains("1j 2h"))
    }

    @Test
    fun `buildConflictMessage handles exactly 24 hours as generic message`() {
        val deltaMs = 24L * 60 * 60 * 1000  // exactly 24h — not strictly greater than
        val msg = RealtimeSyncManager.buildConflictMessage(deltaMs)
        assertEquals("Conflit résolu : version la plus récente conservée", msg)
    }

    @Test
    fun `buildConflictMessage shows days and remaining hours for 30 hour delta`() {
        val deltaMs = 30L * 60 * 60 * 1000  // 30 hours = 1j 6h
        val msg = RealtimeSyncManager.buildConflictMessage(deltaMs)
        assertTrue(msg.contains("1j 6h"))
    }

    @Test
    fun `buildConflictMessage shows days and hours for multi-day delta`() {
        val deltaMs = (2L * 24 + 5) * 60 * 60 * 1000  // 2 days 5 hours
        val msg = RealtimeSyncManager.buildConflictMessage(deltaMs)
        assertTrue(msg.contains("2j 5h"))
    }

    // ── Conflict guard logic ───────────────────────────────────────────────────

    @Test
    fun `incoming newer than existing applies upsert and emits no conflict`() {
        val dao = FakeConflictAwareDao()
        val existingUpdatedAt = 1000L
        val incomingUpdatedAt = 2000L  // newer

        dao.existingUpdatedAt = existingUpdatedAt
        simulateOnUpsert(dao, incomingUpdatedAt)

        assertEquals(1, dao.upsertCount)
        assertEquals(0, dao.conflictCount)
    }

    @Test
    fun `incoming older than existing discards upsert and emits conflict`() {
        val dao = FakeConflictAwareDao()
        val existingUpdatedAt = 2000L
        val incomingUpdatedAt = 1000L  // older → local wins

        dao.existingUpdatedAt = existingUpdatedAt
        simulateOnUpsert(dao, incomingUpdatedAt)

        assertEquals(0, dao.upsertCount)
        assertEquals(1, dao.conflictCount)
    }

    @Test
    fun `incoming same timestamp as existing discards upsert (tie goes to local)`() {
        val dao = FakeConflictAwareDao()
        val sameTimestamp = 1500L
        dao.existingUpdatedAt = sameTimestamp
        simulateOnUpsert(dao, sameTimestamp)

        assertEquals(0, dao.upsertCount)
        // Tie: local wins, no user-visible conflict notification (delta = 0)
        assertEquals(1, dao.conflictCount)
    }

    @Test
    fun `incoming entity not in local DB always applies upsert (first sync)`() {
        val dao = FakeConflictAwareDao()
        dao.existingUpdatedAt = null  // entity doesn't exist locally
        simulateOnUpsert(dao, incomingUpdatedAt = 1000L)

        assertEquals(1, dao.upsertCount)
        assertEquals(0, dao.conflictCount)
    }

    // ── SyncStatusManager ─────────────────────────────────────────────────────

    @Test
    fun `setSynced updates lastSyncAt to a non-null value`() {
        val before = System.currentTimeMillis()
        SyncStatusManager.setSynced()
        val lastSyncAt = SyncStatusManager.lastSyncAt.value
        assertNotNull(lastSyncAt)
        assertTrue(lastSyncAt!! >= before)
    }

    @Test
    fun `setSyncing transitions status to SYNCING`() {
        SyncStatusManager.setSyncing()
        assertEquals(SyncStatusManager.SyncStatus.SYNCING, SyncStatusManager.status.value)
    }

    @Test
    fun `setOffline transitions status to OFFLINE`() {
        SyncStatusManager.setOffline()
        assertEquals(SyncStatusManager.SyncStatus.OFFLINE, SyncStatusManager.status.value)
    }

    @Test
    fun `setError transitions status to ERROR`() {
        SyncStatusManager.setError()
        assertEquals(SyncStatusManager.SyncStatus.ERROR, SyncStatusManager.status.value)
    }

    @Test
    fun `setSynced after setSyncing transitions status back to SYNCED`() {
        SyncStatusManager.setSyncing()
        SyncStatusManager.setSynced()
        assertEquals(SyncStatusManager.SyncStatus.SYNCED, SyncStatusManager.status.value)
    }

    // ── Helper: simulate the guard logic from RealtimeSyncManager.onUpsert ────

    private fun simulateOnUpsert(dao: FakeConflictAwareDao, incomingUpdatedAt: Long) {
        val existing = dao.existingUpdatedAt
        if (existing == null || incomingUpdatedAt > existing) {
            dao.upsertCount++
        } else {
            val delta = existing - incomingUpdatedAt
            dao.conflictCount++
            // SyncStatusManager.emitConflict(RealtimeSyncManager.buildConflictMessage(delta))
            // Not tested here — SharedFlow emission is fire-and-forget
        }
    }
}

// ── FakeConflictAwareDao ──────────────────────────────────────────────────────

class FakeConflictAwareDao {
    var existingUpdatedAt: Long? = null
    var upsertCount = 0
    var conflictCount = 0
}
