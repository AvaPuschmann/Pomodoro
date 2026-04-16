package com.agenticfocus.data.repository

import android.util.Log
import com.agenticfocus.data.dao.DayTaskDao
import com.agenticfocus.data.dao.RoutineDao
import com.agenticfocus.data.entity.DayTaskEntity
import com.agenticfocus.data.entity.RoutineEntity
import com.agenticfocus.data.entity.RoutineItemEntity
import com.agenticfocus.data.supabase.SupabaseClientProvider
import com.agenticfocus.data.supabase.dto.RoutineDto
import com.agenticfocus.data.supabase.dto.RoutineItemDto
import com.agenticfocus.data.sync.SyncEngine
import io.github.jan.supabase.postgrest.from
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class RoutineRepository(
    private val routineDao: RoutineDao,
    private val dayTaskDao: DayTaskDao,
    private val syncQueueDao: com.agenticfocus.data.dao.SyncQueueDao? = null
) {
    private val isInjecting = AtomicBoolean(false)

    suspend fun pullRoutinesFromSupabase(userId: String) {
        try {
            // Clear stale sync_queue FIRST — before pulling, to prevent
            // SyncEngine.flush() from pushing old data back to Supabase.
            syncQueueDao?.deleteByEntityType("routines")
            syncQueueDao?.deleteByEntityType("routine_items")

            val routinesResponse = SupabaseClientProvider.client.from("routines")
                .select { filter { eq("user_id", userId) } }
            Log.d(TAG, "routines raw: ${routinesResponse.data}")
            val routines = routinesResponse.decodeList<RoutineDto>()
            // Delete then re-insert to ensure NULL fields from Supabase overwrite local values.
            // @Upsert (INSERT OR IGNORE + UPDATE) may skip NULL→non-NULL overwrites.
            // Safe because we pull ALL user routines — no data loss.
            for (dto in routines) {
                routineDao.deleteRoutine(dto.id)
            }
            for (dto in routines) {
                routineDao.upsertRoutine(dto.toEntity())
            }
            val itemsResponse = SupabaseClientProvider.client.from("routine_items")
                .select { filter { eq("user_id", userId) } }
            Log.d(TAG, "routine_items raw response: ${itemsResponse.data}")
            val items = itemsResponse.decodeList<RoutineItemDto>()
            for (dto in items) {
                routineDao.upsertRoutineItem(dto.toEntity())
            }
            Log.d(TAG, "Pulled ${routines.size} routines, ${items.size} items from Supabase")
        } catch (e: Exception) {
            Log.w(TAG, "Pull routines failed (offline?): ${e.message}")
        }
    }

    suspend fun injectRoutinesForToday(userId: String): Int {
        if (!isInjecting.compareAndSet(false, true)) return 0

        var injectedCount = 0
        try {
            val today = LocalDate.now()
            val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val now = LocalTime.now()

            val activeRoutines = routineDao.getActiveRoutines(userId)
            Log.d(TAG, "Active routines: ${activeRoutines.size}, today=$todayStr, now=$now")
            for (r in activeRoutines) {
                Log.d(TAG, "  ${r.type} trigger=${r.triggerTime} lastInjected=${r.lastInjectedDate} shouldInject=${shouldInject(r, todayStr, now)}")
            }

            for (routine in activeRoutines) {
                if (!shouldInject(routine, todayStr, now)) continue

                val items = routineDao.getRoutineItems(routine.id)
                for (item in items) {
                    if (routineDao.taskExistsForRoutineItem(item.id, todayStr)) continue

                    val plannedPomodoros = when {
                        item.isCheckOnly == 1 -> 0
                        item.overridePomodoros != null -> item.overridePomodoros
                        else -> 1
                    }

                    val nowMs = System.currentTimeMillis()
                    val entity = DayTaskEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        date = todayStr,
                        name = item.snapshotTitle,
                        plannedPomodoros = plannedPomodoros,
                        completedPomodoros = 0,
                        position = 0,
                        templateId = item.templateId,
                        domainId = item.snapshotDomainId,
                        storyPoints = item.snapshotStoryPoints,
                        createdAt = nowMs,
                        updatedAt = nowMs,
                        routineItemId = item.id
                    )

                    dayTaskDao.upsert(entity)
                    try { SyncEngine.upsertDayTask(entity) } catch (_: Exception) {}
                    injectedCount++
                }

                routineDao.setLastInjectedDate(routine.id, todayStr, System.currentTimeMillis())
                try {
                    val updated = routine.copy(lastInjectedDate = todayStr, updatedAt = System.currentTimeMillis())
                    SyncEngine.upsertRoutine(updated)
                } catch (_: Exception) {}
            }

            if (injectedCount > 0) {
                Log.d(TAG, "Injected $injectedCount routine tasks for $todayStr")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Injection failed", e)
        } finally {
            isInjecting.set(false)
        }
        return injectedCount
    }

    private fun shouldInject(routine: RoutineEntity, todayStr: String, now: LocalTime): Boolean {
        if (routine.isActive != 1) return false
        if (routine.lastInjectedDate == todayStr) return false
        val triggerTime = parseTriggerTime(routine.triggerTime) ?: return false
        return now >= triggerTime
    }

    private fun parseTriggerTime(time: String): LocalTime? {
        return try {
            LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "RoutineRepo"
    }
}
