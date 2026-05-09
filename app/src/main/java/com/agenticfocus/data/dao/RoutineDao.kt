package com.agenticfocus.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.agenticfocus.data.entity.RoutineEntity
import com.agenticfocus.data.entity.RoutineItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Query("SELECT * FROM routines WHERE user_id = :userId ORDER BY type, created_at")
    fun observeRoutines(userId: String): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE user_id = :userId AND is_active = 1 ORDER BY type")
    suspend fun getActiveRoutines(userId: String): List<RoutineEntity>

    // D3 — F4: needed by ensureMorningEveningRoutinesExist; getActiveRoutines filters is_active=1
    //         and would miss inactive routines, leading to duplicate creation.
    @Query("SELECT * FROM routines WHERE user_id = :userId ORDER BY type")
    suspend fun getAllRoutines(userId: String): List<RoutineEntity>

    // D3 — F10: reactive observation of items for given routines so the UI auto-updates
    //          when Realtime delivers an INSERT/UPDATE/DELETE on routine_items.
    @Query("SELECT * FROM routine_items WHERE routine_id IN (:routineIds) ORDER BY routine_id, position")
    fun observeRoutineItemsForRoutines(routineIds: List<String>): Flow<List<RoutineItemEntity>>

    @Upsert
    suspend fun upsertRoutine(routine: RoutineEntity)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutine(id: String)

    @Query("UPDATE routines SET last_injected_date = :date, updated_at = :updatedAt WHERE id = :routineId")
    suspend fun setLastInjectedDate(routineId: String, date: String, updatedAt: Long)

    @Query("SELECT * FROM routine_items WHERE routine_id = :routineId ORDER BY position")
    suspend fun getRoutineItems(routineId: String): List<RoutineItemEntity>

    @Query("SELECT * FROM routine_items WHERE user_id = :userId ORDER BY routine_id, position")
    suspend fun getAllRoutineItems(userId: String): List<RoutineItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutineItem(item: RoutineItemEntity)

    @Query("DELETE FROM routine_items WHERE id = :id")
    suspend fun deleteRoutineItem(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM day_tasks WHERE routine_item_id = :routineItemId AND date = :plannedDate)")
    suspend fun taskExistsForRoutineItem(routineItemId: String, plannedDate: String): Boolean
}
