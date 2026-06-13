package com.agenticfocus.data.repository

import com.agenticfocus.data.dao.DailyReflectionDao
import com.agenticfocus.data.entity.DailyReflectionEntity
import com.agenticfocus.data.sync.SyncEngine
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Repository for daily reflections (Story 24-1 Sprint 22 / Epic 24).
 *
 * Patterns followed:
 * - Last-write-wins sync (Party Mode 2026-05-21 decision Q1) — `try { SyncEngine.upsertDailyReflection(...) } catch (_) {}`.
 *   JWT expired / network down → silently swallowed, sync queue retries later (mirror GoalRepository/SubtaskRepository pattern).
 *
 * - Word/char counts auto-computed at save time (Party Mode decision Mary) — Phase 3 Analytics anticipation.
 *
 * - applyRemoteUpsert handles index unique conflict (Party Mode decision Q2):
 *   if local entry exists for same (user_id, period_key) but with a different id, delete local stale
 *   then apply remote. Prevents SQLiteConstraintException on dual-origin race.
 *
 * @see com.agenticfocus.data.entity.DailyReflectionEntity
 * @see com.agenticfocus.data.dao.DailyReflectionDao
 */
class DailyReflectionRepository(private val dao: DailyReflectionDao) {

    /**
     * Save a daily reflection — creates or updates the entry for (userId, periodKey).
     * If an entry exists, its id is reused (idempotent update). Otherwise a new UUID is generated.
     *
     * Word/char counts are computed automatically before persist.
     * SyncEngine push is best-effort — failures are swallowed (sync queue handles retry).
     */
    suspend fun saveReflection(
        userId: String,
        periodKey: String,
        dayFacts: String,
        learning: String,
    ): DailyReflectionEntity {
        val now = System.currentTimeMillis()
        val existing = dao.getByPeriodKey(userId, periodKey)
        val entity = DailyReflectionEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            userId = userId,
            periodKey = periodKey,
            dayFacts = dayFacts,
            learning = learning,
            dayFactsWordCount = wordCount(dayFacts),
            dayFactsCharCount = dayFacts.length,
            learningWordCount = wordCount(learning),
            learningCharCount = learning.length,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        dao.upsert(entity)
        try { SyncEngine.upsertDailyReflection(entity) } catch (_: Exception) {}
        return entity
    }

    fun observeReflection(userId: String, periodKey: String): Flow<DailyReflectionEntity?> =
        dao.observeByPeriodKey(userId, periodKey)

    fun observeAllForUser(userId: String): Flow<List<DailyReflectionEntity>> =
        dao.observeAllForUser(userId)

    suspend fun getReflection(userId: String, periodKey: String): DailyReflectionEntity? =
        dao.getByPeriodKey(userId, periodKey)

    /**
     * Apply a remote-originated entity locally.
     *
     * Index unique conflict handling (Party Mode Q2): if a local entry exists for the same
     * (user_id, period_key) but with a different id (cross-device race), delete the stale
     * local entry first to avoid SQLiteConstraintException on unique index.
     *
     * Called by RealtimeSyncManager and PullSync — does NOT push back to Supabase.
     */
    suspend fun applyRemoteUpsert(entity: DailyReflectionEntity) {
        val local = dao.getByPeriodKey(entity.userId, entity.periodKey)
        if (local != null && local.id != entity.id) {
            dao.deleteById(local.id)
        }
        dao.upsertFromRemote(entity)
    }

    suspend fun deleteReflection(id: String) {
        dao.deleteById(id)
        try { SyncEngine.deleteDailyReflection(id) } catch (_: Exception) {}
    }

    private fun wordCount(text: String): Int =
        text.split(Regex("\\s+")).count { it.isNotBlank() }
}
