package com.agenticfocus.data.repository

import android.util.Log
import com.agenticfocus.data.dao.DayTaskDao
import com.agenticfocus.data.dao.RoutineDao
import com.agenticfocus.data.entity.DayTaskEntity
import com.agenticfocus.data.entity.RoutineEntity
import com.agenticfocus.data.supabase.SupabaseClientProvider
import com.agenticfocus.data.supabase.dto.toEntity
import com.agenticfocus.data.sync.SyncEngine
import io.github.jan.supabase.postgrest.from
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class RoutineRepository(
    private val routineDao: RoutineDao,
    private val dayTaskDao: DayTaskDao
) {
    private val isInjecting = AtomicBoolean(false)

    /** Pull today's routine-injected tasks from Supabase to local DB so the
     *  exists check catches tasks created by the other device. */
    private suspend fun pullTodayRoutineTasks(userId: String, todayStr: String) {
        try {
            val tasks = SupabaseClientProvider.client.from("day_tasks")
                .select { filter { eq("user_id", userId); eq("date", todayStr); neq("routine_item_id", "null") } }
                .decodeList<com.agenticfocus.data.supabase.dto.DayTaskDto>()
            for (dto in tasks) {
                val entity = dto.toEntity()
                dayTaskDao.upsert(entity)
            }
            if (tasks.isNotEmpty()) Log.d(TAG, "Pulled ${tasks.size} routine tasks for $todayStr from Supabase")
        } catch (e: Exception) {
            Log.w(TAG, "pullTodayRoutineTasks failed (offline?): ${e.message}")
        }
    }

    suspend fun injectRoutinesForToday(userId: String): Int {
        if (!isInjecting.compareAndSet(false, true)) return 0

        var injectedCount = 0
        try {
            val today = LocalDate.now()
            val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val now = LocalTime.now()

            // Pull today's routine tasks from Supabase first (prevents multi-device duplication)
            pullTodayRoutineTasks(userId, todayStr)

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
