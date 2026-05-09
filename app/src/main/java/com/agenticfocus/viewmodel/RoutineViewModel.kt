package com.agenticfocus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agenticfocus.data.db.AppDatabase
import com.agenticfocus.data.entity.RoutineEntity
import com.agenticfocus.data.entity.RoutineItemEntity
import com.agenticfocus.data.entity.TaskTemplateEntity
import com.agenticfocus.data.sync.SyncEngine
import com.agenticfocus.data.sync.SyncStatusManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

data class RoutineState(
    val morning: RoutineEntity? = null,
    val evening: RoutineEntity? = null,
    val itemsByRoutine: Map<String, List<RoutineItemEntity>> = emptyMap()
)

/**
 * D3 — Routines management ViewModel.
 *
 * Responsibilities:
 * - Observe local Room state for the 2 user routines (morning/evening) + their items
 *   reactively, so Realtime updates from desktop propagate to UI.
 * - Auto-create the 2 routines on first login (F4: deterministic IDs prevent
 *   cross-device duplication ; F8: gated on `firstPullCompleted` to wait for
 *   the initial pull to land before creating).
 * - Expose CRUD methods for UI (updateRoutine, addItemFromTemplate, removeItem).
 *
 * F7 anti-feedback-loop rule: mutation methods are triggered ONLY by user
 * actions. The state Flow is observation-only ; never call updateRoutine from
 * a `LaunchedEffect(state.morning) { ... }`. Mutations include a no-op guard
 * when the new value equals the current persisted value.
 */
class RoutineViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val routineDao = db.routineDao()

    private val _state = MutableStateFlow(RoutineState())
    val state: StateFlow<RoutineState> = _state.asStateFlow()

    private var userJob: Job? = null
    private var currentUserId: String = ""

    /**
     * Wire the VM to a specific user. Idempotent for same user. On user switch:
     * cancels previous observation, clears state, restarts.
     *
     * Auto-create of the 2 routines is gated on `SyncStatusManager.firstPullCompleted`
     * so we don't race the initial pull and end up with duplicate entries.
     */
    fun setUserId(userId: String) {
        if (userId == currentUserId && userJob?.isActive == true) return
        userJob?.cancel()
        currentUserId = userId
        _state.value = RoutineState()
        if (userId.isEmpty()) return

        userJob = viewModelScope.launch {
            // F4+F8 — Wait for first centralised pull to complete before auto-creating.
            // Avoids "mobile creates morning+evening with id_X while desktop creates
            // with id_Y" race producing 4 routines instead of 2.
            SyncStatusManager.firstPullCompleted.first { it }
            ensureMorningEveningRoutinesExist(userId)
            startObserving(userId)
        }
    }

    /**
     * F4 — Deterministic IDs (`${userId}::morning` / `${userId}::evening`).
     * If mobile and desktop both run this concurrently for a brand-new user,
     * they each upsert the same primary key → idempotent on Supabase. Result:
     * exactly 2 routines, never 4.
     */
    private suspend fun ensureMorningEveningRoutinesExist(userId: String) {
        val existing = routineDao.getAllRoutines(userId)
        val existingTypes = existing.map { it.type }.toSet()
        val now = System.currentTimeMillis()
        if ("morning" !in existingTypes) {
            val r = RoutineEntity(
                id = "${userId}::morning",
                userId = userId,
                type = "morning",
                name = "Routine matinale",
                triggerTime = "06:00",
                isActive = 1,
                streakCount = 0,
                streakLastCompletedDate = null,
                lastInjectedDate = null,
                createdAt = now,
                updatedAt = now
            )
            routineDao.upsertRoutine(r)
            try { SyncEngine.upsertRoutine(r) } catch (_: Exception) {}
        }
        if ("evening" !in existingTypes) {
            val r = RoutineEntity(
                id = "${userId}::evening",
                userId = userId,
                type = "evening",
                name = "Routine du soir",
                triggerTime = "20:00",
                isActive = 1,
                streakCount = 0,
                streakLastCompletedDate = null,
                lastInjectedDate = null,
                createdAt = now,
                updatedAt = now
            )
            routineDao.upsertRoutine(r)
            try { SyncEngine.upsertRoutine(r) } catch (_: Exception) {}
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun startObserving(userId: String) {
        viewModelScope.launch {
            routineDao.observeRoutines(userId)
                .flatMapLatest { routines ->
                    val morning = routines.firstOrNull { it.type == "morning" }
                    val evening = routines.firstOrNull { it.type == "evening" }
                    val ids = routines.map { it.id }
                    if (ids.isEmpty()) {
                        flowOf(Triple(morning, evening, emptyList<RoutineItemEntity>()))
                    } else {
                        routineDao.observeRoutineItemsForRoutines(ids)
                            .map { items -> Triple(morning, evening, items) }
                    }
                }
                .collect { (morning, evening, items) ->
                    _state.value = RoutineState(
                        morning = morning,
                        evening = evening,
                        itemsByRoutine = items.groupBy { it.routineId }
                    )
                }
        }
    }

    // ── Mutations (F7: triggered by user actions only) ────────────────────────

    fun updateRoutine(id: String, name: String, triggerTime: String, isActive: Boolean) {
        viewModelScope.launch {
            val current = routineDao.getAllRoutines(currentUserId).firstOrNull { it.id == id } ?: return@launch
            val newActive = if (isActive) 1 else 0
            // No-op guard: avoid gratuitous round-trip + Realtime echo storm.
            if (current.name == name && current.triggerTime == triggerTime && current.isActive == newActive) return@launch
            val updated = current.copy(
                name = name,
                triggerTime = triggerTime,
                isActive = newActive,
                updatedAt = System.currentTimeMillis()
            )
            routineDao.upsertRoutine(updated)
            try { SyncEngine.upsertRoutine(updated) } catch (_: Exception) {}
        }
    }

    fun addItemFromTemplate(routineId: String, template: TaskTemplateEntity) {
        viewModelScope.launch {
            val nextPosition = (_state.value.itemsByRoutine[routineId]?.maxOfOrNull { it.position } ?: -1) + 1
            val now = System.currentTimeMillis()
            val item = RoutineItemEntity(
                id = UUID.randomUUID().toString(),
                routineId = routineId,
                templateId = template.id,
                snapshotTitle = template.title,
                snapshotDomainId = template.domainId,
                snapshotStoryPoints = template.storyPoints,
                overridePomodoros = null, // use template.defaultPomodoros at injection time
                isCheckOnly = 0,
                position = nextPosition,
                userId = currentUserId,
                createdAt = now,
                updatedAt = now
            )
            routineDao.upsertRoutineItem(item)
            try { SyncEngine.upsertRoutineItem(item) } catch (_: Exception) {}
        }
    }

    fun removeItem(item: RoutineItemEntity) {
        viewModelScope.launch {
            routineDao.deleteRoutineItem(item.id)
            try { SyncEngine.deleteRoutineItem(item.id) } catch (_: Exception) {}
        }
    }
}
