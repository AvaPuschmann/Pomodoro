package com.agenticfocus.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.agenticfocus.data.entity.DailyReflectionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for daily reflections (Story 24-1 Sprint 22).
 *
 * Two upsert flavors:
 * - [upsert] — local write that should be pushed to Supabase (Repository calls SyncEngine after).
 * - [upsertFromRemote] — used by RealtimeSyncManager + PullSync, no sync-queue push.
 *
 * @see com.agenticfocus.data.entity.DailyReflectionEntity
 */
@Dao
interface DailyReflectionDao {

    @Upsert
    suspend fun upsert(entity: DailyReflectionEntity)

    /**
     * Apply a remote-originated entity without re-pushing to Supabase.
     * Used by RealtimeSyncManager and PullSync to avoid infinite loops.
     */
    @Upsert
    suspend fun upsertFromRemote(entity: DailyReflectionEntity)

    @Query("SELECT * FROM daily_reflections WHERE user_id = :userId AND period_key = :periodKey LIMIT 1")
    suspend fun getByPeriodKey(userId: String, periodKey: String): DailyReflectionEntity?

    @Query("SELECT * FROM daily_reflections WHERE user_id = :userId AND period_key = :periodKey LIMIT 1")
    fun observeByPeriodKey(userId: String, periodKey: String): Flow<DailyReflectionEntity?>

    @Query("SELECT * FROM daily_reflections WHERE user_id = :userId ORDER BY period_key DESC")
    fun observeAllForUser(userId: String): Flow<List<DailyReflectionEntity>>

    @Query("DELETE FROM daily_reflections WHERE id = :id")
    suspend fun deleteById(id: String)
}
