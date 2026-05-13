package com.agenticfocus.data.sync

import android.util.Log
import androidx.room.withTransaction
import com.agenticfocus.BuildConfig
import com.agenticfocus.data.db.AppDatabase
import com.agenticfocus.data.supabase.SupabaseClientProvider
import com.agenticfocus.data.supabase.dto.DayTaskDto
import com.agenticfocus.data.supabase.dto.DomainDto
import com.agenticfocus.data.supabase.dto.GoalDto
import com.agenticfocus.data.supabase.dto.PomodoroSessionDto
import com.agenticfocus.data.supabase.dto.ProjectDto
import com.agenticfocus.data.supabase.dto.RoutineDto
import com.agenticfocus.data.supabase.dto.RoutineItemDto
import com.agenticfocus.data.supabase.dto.SubtaskDto
import com.agenticfocus.data.supabase.dto.TaskTemplateDto
import com.agenticfocus.data.supabase.dto.toEntity
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeOldRecord
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

object RealtimeSyncManager {

    private const val TAG = "RealtimeSyncManager"
    private const val PAGE_SIZE = 1000
    private const val PERIODIC_INTERVAL_MS = 300_000L // 5 minutes (was 30s before A1 fix)

    // A2.5 — Swallows uncaught exceptions from Realtime subscription cleanup.
    // supabase-kt 2.6.1 has a race condition in CallbackManagerImpl.removeCallbackById
    // (AtomicMutableList not thread-safe for concurrent removes). Without this handler,
    // an IndexOutOfBoundsException during awaitClose of any postgresChangeFlow propagates
    // as FATAL EXCEPTION and kills the process — observed historically across April 2026.
    // SupervisorJob isolates failures but does NOT catch them ; we need this handler.
    private val swallowRealtimeRaces = CoroutineExceptionHandler { _, e ->
        Log.w(TAG, "Realtime subscription cleanup error swallowed: ${e.message}")
    }

    private var db: AppDatabase? = null
    private var syncScope: CoroutineScope? = null
    private var realtimeChannel: RealtimeChannel? = null
    private var currentSyncUserId: String? = null

    fun initialize(db: AppDatabase) {
        this.db = db
    }

    fun startSync(userId: String) {
        // Idempotent guard: AuthViewModel calls startSync from both restoreSession()
        // and sessionStatusFlow.collect — they can fire within milliseconds of each
        // other during app cold-start. Without this guard, the 2nd call's internal
        // stopSync() cancels the 1st run's in-flight subscriptions+pullSync (Job was
        // cancelled cascade), leaving the channel in an inconsistent state on the
        // server (Realtime events stop arriving on mobile even though the websocket
        // is connected). Skipping the duplicate call preserves the stable subscription.
        if (currentSyncUserId == userId && syncScope?.isActive == true) {
            Log.d(TAG, "startSync($userId) skipped — already running with same user")
            return
        }
        stopSync()
        currentSyncUserId = userId
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + swallowRealtimeRaces)
        syncScope = scope
        scope.launch {
            try {
                val ch = SupabaseClientProvider.client.channel("android-sync-$userId")
                realtimeChannel = ch

                subscribeToTable<DayTaskDto>(ch, "day_tasks", userId, scope,
                    onUpsert = { dto -> db?.dayTaskDao()?.upsert(dto.toEntity()) },
                    onDeleteById = { id -> db?.dayTaskDao()?.deleteById(id) }
                )
                subscribeToTable<PomodoroSessionDto>(ch, "pomodoro_sessions", userId, scope,
                    onUpsert = { dto -> db?.pomodoroSessionDao()?.upsert(dto.toEntity()) },
                    onDeleteById = { id -> db?.pomodoroSessionDao()?.deleteById(id) }
                )
                subscribeToTable<DomainDto>(ch, "domains", userId, scope,
                    onUpsert = { dto -> db?.domainDao()?.insert(dto.toEntity()) },
                    onDeleteById = { id -> db?.domainDao()?.deleteById(id) }
                )
                subscribeToTable<TaskTemplateDto>(ch, "task_templates", userId, scope,
                    onUpsert = { dto -> db?.taskTemplateDao()?.insert(dto.toEntity()) },
                    onDeleteById = { id -> db?.taskTemplateDao()?.deleteById(id) }
                )
                subscribeToTable<SubtaskDto>(ch, "subtasks", userId, scope,
                    onUpsert = { dto -> db?.subtaskDao()?.upsert(dto.toEntity()) },
                    onDeleteById = { id -> db?.subtaskDao()?.deleteById(id) }
                )
                subscribeToTable<GoalDto>(ch, "goals", userId, scope,
                    onUpsert = { dto -> db?.goalDao()?.upsert(dto.toEntity()) },
                    onDeleteById = { id -> db?.goalDao()?.deleteById(id) }
                )
                // A1 — Realtime subscriptions for routines + routine_items
                subscribeToTable<RoutineDto>(ch, "routines", userId, scope,
                    onUpsert = { dto -> db?.routineDao()?.upsertRoutine(dto.toEntity()) },
                    onDeleteById = { id -> db?.routineDao()?.deleteRoutine(id) }
                )
                subscribeToTable<RoutineItemDto>(ch, "routine_items", userId, scope,
                    onUpsert = { dto -> db?.routineDao()?.upsertRoutineItem(dto.toEntity()) },
                    onDeleteById = { id -> db?.routineDao()?.deleteRoutineItem(id) }
                )
                // Mode Projet — Story 17-9. Gated FEATURE_PROJECTS (16-1).
                // Listener registered BEFORE channel.subscribe() per supabase-kt pitfall #4.
                if (BuildConfig.FEATURE_PROJECTS) {
                    subscribeToTable<ProjectDto>(ch, "projects", userId, scope,
                        onUpsert = { dto -> db?.projectDao()?.upsert(dto.toEntity()) },
                        onDeleteById = { id -> db?.projectDao()?.deleteById(id) }
                    )
                }

                // Belt-and-suspenders: even though subscribeToTable now suspends until
                // its onStart fires, the upstream callbackFlow producer (which calls
                // addPostgresChange to register the filter on the channel) runs slightly
                // after onStart due to Kotlin Flow operator ordering. A short delay gives
                // the producer block time to finish registration before SUBSCRIBE is sent.
                delay(300)
                val subCount = if (BuildConfig.FEATURE_PROJECTS) 9 else 8
                Log.d(TAG, "All $subCount Realtime subscriptions registered, calling channel.subscribe()")
                ch.subscribe()

                // A1 (F6) — One-shot startup cleanup of stale sync_queue entries for routines.
                // Was previously inside RoutineRepository.pullRoutinesFromSupabase (one-shot at app start).
                // Must NOT be moved into pullSync() because pullSync() now runs every 5 min
                // (and on reconnect, onResume) — this would nuke pending offline routine mutations
                // (relevant once D3 ships routine creation on mobile).
                try {
                    db?.syncQueueDao()?.deleteByEntityType("routines")
                    db?.syncQueueDao()?.deleteByEntityType("routine_items")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear stale sync_queue routines: ${e.message}")
                }

                pullSync(userId)
                monitorReconnect(userId, scope)
                startPeriodicSync(userId, scope)
            } catch (e: Exception) {
                Log.w(TAG, "startSync failed: ${e.message}")
            }
        }
    }

    fun stopSync() {
        val scope = syncScope
        val ch = realtimeChannel
        // A2.5 — Unsubscribe BEFORE cancelling scope. If we cancel the scope first,
        // all 8 subscription flows' awaitClose handlers race to call removeCallbackById
        // concurrently and the underlying AtomicMutableList in supabase-kt 2.6.1 trips
        // an IndexOutOfBoundsException. Sequencing the unsubscribe first lets the
        // channel close cleanly before the flow scope is torn down.
        if (ch != null) {
            try {
                // Fire-and-forget on a temporary scope outside the cancelled one,
                // so the unsubscribe survives the scope teardown.
                CoroutineScope(Dispatchers.IO + swallowRealtimeRaces).launch {
                    try { ch.unsubscribe() } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
        scope?.cancel()
        syncScope = null
        realtimeChannel = null
        currentSyncUserId = null
        SyncStatusManager.resetFirstPullCompleted()
    }

    /** A1 (F8) — Manual pull trigger, called from MainActivity.onResume to catch up
     *  on Realtime events potentially missed while the app was in background. */
    fun triggerPull(userId: String) {
        val scope = syncScope ?: return
        scope.launch {
            if (SyncStatusManager.status.value != SyncStatusManager.SyncStatus.OFFLINE) {
                pullSync(userId)
            }
        }
    }

    // ── Paginated fetch helper ────────────────────────────────────────────────
    // A1 (F3) — CRITICAL: rethrow on error. NEVER return a partial dataset.
    // The caller pullSync() must skip reconciliation if this throws, otherwise
    // a truncated fetch silently deletes local rows beyond the truncation point
    // (the bug pagination was meant to prevent — see desktop feedback_desktop_sync_pagination).
    private suspend inline fun <reified T : Any> fetchAllUserRows(
        table: String,
        userId: String
    ): List<T> {
        val all = mutableListOf<T>()
        var from = 0
        while (true) {
            val page = try {
                SupabaseClientProvider.client.from(table)
                    .select {
                        filter { eq("user_id", userId) }
                        range(from.toLong(), (from + PAGE_SIZE - 1).toLong())
                    }
                    .decodeList<T>()
            } catch (e: Exception) {
                Log.e(TAG, "fetchAllUserRows failed for $table at offset $from: ${e.message}")
                throw e
            }
            if (page.isEmpty()) break
            all.addAll(page)
            if (page.size < PAGE_SIZE) break
            from += PAGE_SIZE
        }
        return all
    }

    // ── Pull sync (catch-up on startup, reconnect, onResume, and periodic) ────

    private suspend fun pullSync(userId: String) {
        SyncStatusManager.setSyncing()
        val db = db ?: return
        try {
            // ── Domains: clear-then-replace (preserve invariant for orphan cleanup, F5) ─
            // C1 follow-up : wrap in withTransaction so Room Flow observers don't see
            // an intermediate empty state between deleteAll() and the inserts (was
            // causing Library tab + EditTaskForm domain dropdown to flash empty for
            // hundreds of ms after every pullSync — every 5 min, onResume, reconnect).
            val tDomains = System.currentTimeMillis()
            val domains = fetchAllUserRows<DomainDto>("domains", userId)
            db.withTransaction {
                db.domainDao().deleteAll()
                domains.forEach { db.domainDao().insert(it.toEntity()) }
            }
            Log.d(TAG, "Pulled ${domains.size} domains in ${System.currentTimeMillis() - tDomains}ms")

            // ── Task templates: clear-then-replace (referential integrity with domains, F5) ─
            // Same withTransaction guarantee as domains (atomic visibility).
            val tTemplates = System.currentTimeMillis()
            val templates = fetchAllUserRows<TaskTemplateDto>("task_templates", userId)
            db.withTransaction {
                db.taskTemplateDao().deleteAll()
                templates.forEach { db.taskTemplateDao().insert(it.toEntity()) }
            }
            Log.d(TAG, "Pulled ${templates.size} task_templates in ${System.currentTimeMillis() - tTemplates}ms")

            // ── Pomodoro sessions: upsert-only ─────────────────────────────────
            val tSessions = System.currentTimeMillis()
            val sessions = fetchAllUserRows<PomodoroSessionDto>("pomodoro_sessions", userId)
            sessions.forEach { db.pomodoroSessionDao().upsert(it.toEntity()) }
            Log.d(TAG, "Pulled ${sessions.size} pomodoro_sessions in ${System.currentTimeMillis() - tSessions}ms")

            // ── Subtasks: upsert-only ──────────────────────────────────────────
            val tSubtasks = System.currentTimeMillis()
            val subtasks = fetchAllUserRows<SubtaskDto>("subtasks", userId)
            subtasks.forEach { db.subtaskDao().upsert(it.toEntity()) }
            Log.d(TAG, "Pulled ${subtasks.size} subtasks in ${System.currentTimeMillis() - tSubtasks}ms")

            // ── Goals: upsert-only ─────────────────────────────────────────────
            val tGoals = System.currentTimeMillis()
            val goals = fetchAllUserRows<GoalDto>("goals", userId)
            goals.forEach { db.goalDao().upsert(it.toEntity()) }
            Log.d(TAG, "Pulled ${goals.size} goals in ${System.currentTimeMillis() - tGoals}ms")

            // ── Routines: clear-then-replace per id (forces NULL overwrite for
            //    fields that were null on Supabase but non-null locally — pattern
            //    inherited from former RoutineRepository.pullRoutinesFromSupabase).
            //    Wrapped in withTransaction so Flow observers see only the final state.
            val tRoutines = System.currentTimeMillis()
            val routines = fetchAllUserRows<RoutineDto>("routines", userId)
            db.withTransaction {
                for (dto in routines) db.routineDao().deleteRoutine(dto.id)
                for (dto in routines) db.routineDao().upsertRoutine(dto.toEntity())
            }
            Log.d(TAG, "Pulled ${routines.size} routines in ${System.currentTimeMillis() - tRoutines}ms")

            // ── Routine items: upsert-only ─────────────────────────────────────
            val tRoutineItems = System.currentTimeMillis()
            val routineItems = fetchAllUserRows<RoutineItemDto>("routine_items", userId)
            routineItems.forEach { db.routineDao().upsertRoutineItem(it.toEntity()) }
            Log.d(TAG, "Pulled ${routineItems.size} routine_items in ${System.currentTimeMillis() - tRoutineItems}ms")

            // ── Projects: clear-then-replace (referential integrity with day_tasks.project_id)
            // Story 17-9 / Sprint 17. Must be pulled BEFORE day_tasks so no transient
            // orphan project_id in UI [D28/F7/F-B]. Gated FEATURE_PROJECTS (16-1).
            if (BuildConfig.FEATURE_PROJECTS) {
                val tProjects = System.currentTimeMillis()
                val projects = fetchAllUserRows<ProjectDto>("projects", userId)
                db.withTransaction {
                    db.projectDao().deleteAll()
                    projects.forEach { db.projectDao().upsert(it.toEntity()) }
                }
                Log.d(TAG, "Pulled ${projects.size} projects in ${System.currentTimeMillis() - tProjects}ms")
            }

            // ── Day tasks: reconciliation (atomic, sync_queue protected, F1+F2+F3) ─
            val tTasks = System.currentTimeMillis()
            val tasks = fetchAllUserRows<DayTaskDto>("day_tasks", userId)
            val remoteIds = tasks.map { it.id }.toSet()

            db.withTransaction {
                // F1 — Mobile only ever enqueues "UPSERT" or "DELETE" (verified in SyncEngine).
                //      "INSERT" filter from desktop port was wrong.
                val pending = db.syncQueueDao().getAll().filter { it.entityType == "day_tasks" }
                val pendingUpsertIds = pending.filter { it.operation == "UPSERT" }.map { it.entityId }.toSet()
                val pendingDeleteIds = pending.filter { it.operation == "DELETE" }.map { it.entityId }.toSet()

                val localIds = db.dayTaskDao().getAllIdsForUser(userId)

                // Delete locals absent from remote, protecting pending upserts (local newer)
                // and pending deletes (already removed locally, awaiting flush).
                for (localId in localIds) {
                    if (localId !in remoteIds &&
                        localId !in pendingUpsertIds &&
                        localId !in pendingDeleteIds
                    ) {
                        db.dayTaskDao().deleteById(localId)
                    }
                }

                // Upsert remote rows, skipping any with pending local mutations
                // (UPSERT = local more recent ; DELETE = must not re-insert).
                for (dto in tasks) {
                    if (dto.id in pendingUpsertIds || dto.id in pendingDeleteIds) continue
                    db.dayTaskDao().upsert(dto.toEntity())
                }
            }
            Log.d(TAG, "Pulled ${tasks.size} day_tasks in ${System.currentTimeMillis() - tTasks}ms (reconciled)")

            SyncStatusManager.setSynced()
            // F7 — Signal first successful pull completion so MainActivity can gate
            //      its second routine injection deterministically.
            SyncStatusManager.markFirstPullCompleted()
        } catch (e: Exception) {
            // F3 — On any fetch error: NO partial reconciliation, NO firstPullCompleted.
            Log.e(TAG, "pullSync failed: ${e.message}", e)
            SyncStatusManager.setError()
        }
    }

    // ── Reconnection monitoring ────────────────────────────────────────────────

    private fun monitorReconnect(userId: String, scope: CoroutineScope) {
        scope.launch {
            var wasConnected = false
            SupabaseClientProvider.client.realtime.status.collect { status ->
                val isNowConnected = status == Realtime.Status.CONNECTED
                if (!isNowConnected && wasConnected) {
                    SyncStatusManager.setOffline()
                }
                if (isNowConnected && wasConnected) {
                    pullSync(userId)
                }
                wasConnected = isNowConnected
            }
        }
    }

    // ── Conflict message builder ───────────────────────────────────────────────

    internal fun buildConflictMessage(deltaMs: Long): String {
        val twentyFourHoursMs = 24L * 60 * 60 * 1000
        return if (deltaMs > twentyFourHoursMs) {
            val totalHours = deltaMs / (60 * 60 * 1000)
            val days = totalHours / 24
            val remainingHours = totalHours % 24
            val deltaStr = if (days > 0) "${days}j ${remainingHours}h" else "${totalHours}h"
            "Conflit résolu : version plus récente conservée (ancienne version +$deltaStr)"
        } else {
            "Conflit résolu : version la plus récente conservée"
        }
    }

    // ── Periodic pull sync (fallback for missed Realtime events) ─────────────

    private fun startPeriodicSync(userId: String, scope: CoroutineScope) {
        scope.launch {
            while (true) {
                delay(PERIODIC_INTERVAL_MS)
                if (SyncStatusManager.status.value != SyncStatusManager.SyncStatus.OFFLINE) {
                    pullSync(userId)
                }
            }
        }
    }

    // ── Per-table subscription helper ─────────────────────────────────────────

    /**
     * Sets up a Realtime postgresChangeFlow listener and AWAITS its registration
     * before returning. This is critical: supabase-kt requires all postgresChange
     * listeners to be registered BEFORE channel.subscribe() is called (the docs
     * say so, and registration after subscribe() throws). The previous version
     * launched the collect inside scope.launch { }, which started async — so
     * ch.subscribe() ran before listeners were registered → SUBSCRIBE message
     * sent with no filters → mobile received no Realtime events from the server
     * (especially desktop → mobile direction was broken).
     *
     * Uses .onSubscription { signal.complete() } to detect when the inner
     * callbackFlow producer has started (i.e. addPostgresChange has been called),
     * then suspends caller until that signal fires.
     */
    /**
     * `onDelete` takes only the row ID (String) instead of the decoded DTO.
     *
     * Why: Supabase Realtime DELETE events only carry the row's primary key
     * (REPLICA IDENTITY DEFAULT). Trying to decode the partial JSON into a DTO
     * with required non-null fields (e.g. DayTaskDto.name, plannedPomodoros)
     * throws SerializationException → caught silently → DELETE never applied.
     * Extracting the ID directly from action.oldRecord["id"] avoids this and
     * is sufficient since DAO deletes only need the ID.
     *
     * Setting `ALTER TABLE … REPLICA IDENTITY FULL` on Supabase would also
     * fix it server-side, but this client-side fix is more robust.
     */
    private suspend inline fun <reified Dto> subscribeToTable(
        channel: RealtimeChannel,
        table: String,
        userId: String,
        scope: CoroutineScope,
        crossinline onUpsert: suspend (Dto) -> Unit,
        crossinline onDeleteById: suspend (String) -> Unit
    ) {
        val ready = CompletableDeferred<Unit>()
        scope.launch {
            try {
                channel.postgresChangeFlow<PostgresAction>("public") {
                    this.table = table
                    filter("user_id", FilterOperator.EQ, userId)
                }
                    .onStart { ready.complete(Unit) }
                    .catch { e ->
                        Log.w(TAG, "Realtime flow error on $table: ${e.message}")
                    }
                    .collect { action ->
                        try {
                            when (action) {
                                is PostgresAction.Insert -> {
                                    Log.d(TAG, "Realtime INSERT on $table")
                                    onUpsert(action.decodeRecord())
                                }
                                is PostgresAction.Update -> {
                                    Log.d(TAG, "Realtime UPDATE on $table")
                                    onUpsert(action.decodeRecord())
                                }
                                is PostgresAction.Delete -> {
                                    val id = action.oldRecord["id"]?.jsonPrimitive?.contentOrNull
                                    Log.d(TAG, "Realtime DELETE on $table id=$id")
                                    if (id != null) onDeleteById(id)
                                    else Log.w(TAG, "DELETE on $table missing id in oldRecord: ${action.oldRecord}")
                                }
                                else -> {}
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to apply Realtime action on $table: ${e.message}")
                        }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Subscription to $table errored: ${e.message}")
                // Unblock caller even on errors so startSync doesn't hang forever.
                ready.complete(Unit)
            }
        }
        ready.await()
    }
}
