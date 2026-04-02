package com.agenticfocus.data.sync

import com.agenticfocus.data.db.AppDatabase
import com.agenticfocus.data.supabase.SupabaseClientProvider
import com.agenticfocus.data.supabase.dto.DayTaskDto
import com.agenticfocus.data.supabase.dto.DomainDto
import com.agenticfocus.data.supabase.dto.GoalDto
import com.agenticfocus.data.supabase.dto.PomodoroSessionDto
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

object RealtimeSyncManager {

    private var db: AppDatabase? = null
    private var syncScope: CoroutineScope? = null
    private var realtimeChannel: RealtimeChannel? = null

    fun initialize(db: AppDatabase) {
        this.db = db
    }

    fun startSync(userId: String) {
        stopSync()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        syncScope = scope
        scope.launch {
            try {
                val ch = SupabaseClientProvider.client.channel("android-sync-$userId")
                realtimeChannel = ch

                subscribeToTable<DayTaskDto>(ch, "day_tasks", userId, scope,
                    onUpsert = { dto -> db?.dayTaskDao()?.upsert(dto.toEntity()) },
                    onDelete = { dto -> db?.dayTaskDao()?.deleteById(dto.id) }
                )
                subscribeToTable<PomodoroSessionDto>(ch, "pomodoro_sessions", userId, scope,
                    onUpsert = { dto -> db?.pomodoroSessionDao()?.upsert(dto.toEntity()) },
                    onDelete = { dto -> db?.pomodoroSessionDao()?.deleteById(dto.id) }
                )
                subscribeToTable<DomainDto>(ch, "domains", userId, scope,
                    onUpsert = { dto -> db?.domainDao()?.insert(dto.toEntity()) },
                    onDelete = { dto -> db?.domainDao()?.deleteById(dto.id) }
                )
                subscribeToTable<TaskTemplateDto>(ch, "task_templates", userId, scope,
                    onUpsert = { dto -> db?.taskTemplateDao()?.insert(dto.toEntity()) },
                    onDelete = { dto -> db?.taskTemplateDao()?.deleteById(dto.id) }
                )
                subscribeToTable<SubtaskDto>(ch, "subtasks", userId, scope,
                    onUpsert = { dto -> db?.subtaskDao()?.upsert(dto.toEntity()) },
                    onDelete = { dto -> db?.subtaskDao()?.deleteById(dto.id) }
                )
                subscribeToTable<GoalDto>(ch, "goals", userId, scope,
                    onUpsert = { dto -> db?.goalDao()?.upsert(dto.toEntity()) },
                    onDelete = { dto -> db?.goalDao()?.deleteById(dto.id) }
                )

                ch.subscribe()
                pullSync(userId)
                monitorReconnect(userId, scope)
                startPeriodicSync(userId, scope)
            } catch (_: Exception) {}
        }
    }

    fun stopSync() {
        val scope = syncScope
        val ch = realtimeChannel
        if (scope != null && ch != null) {
            scope.launch {
                try { ch.unsubscribe() } catch (_: Exception) {}
            }
        }
        scope?.cancel()
        syncScope = null
        realtimeChannel = null
    }

    // ── Pull sync (catch-up on startup and reconnect) ─────────────────────────

    private suspend fun pullSync(userId: String) {
        SyncStatusManager.setSyncing()
        val db = db ?: return
        try {
            val tasks = SupabaseClientProvider.client.from("day_tasks")
                .select { filter { eq("user_id", userId) } }
                .decodeList<DayTaskDto>()
            // Supabase is authoritative — always upsert all remote rows
            for (dto in tasks) {
                db.dayTaskDao().upsert(dto.toEntity())
            }

            val sessions = SupabaseClientProvider.client.from("pomodoro_sessions")
                .select { filter { eq("user_id", userId) } }
                .decodeList<PomodoroSessionDto>()
            sessions.forEach { db.pomodoroSessionDao().upsert(it.toEntity()) }

            val domains = SupabaseClientProvider.client.from("domains")
                .select { filter { eq("user_id", userId) } }
                .decodeList<DomainDto>()
            // Always clear + replace to avoid stale/orphaned rows
            db.domainDao().deleteAll()
            domains.forEach { db.domainDao().insert(it.toEntity()) }

            val templates = SupabaseClientProvider.client.from("task_templates")
                .select { filter { eq("user_id", userId) } }
                .decodeList<TaskTemplateDto>()
            // Always clear templates when domains are refreshed (referential integrity)
            db.taskTemplateDao().deleteAll()
            templates.forEach { db.taskTemplateDao().insert(it.toEntity()) }

            val subtasks = SupabaseClientProvider.client.from("subtasks")
                .select { filter { eq("user_id", userId) } }
                .decodeList<SubtaskDto>()
            subtasks.forEach { db.subtaskDao().upsert(it.toEntity()) }

            val goals = SupabaseClientProvider.client.from("goals")
                .select { filter { eq("user_id", userId) } }
                .decodeList<GoalDto>()
            goals.forEach { db.goalDao().upsert(it.toEntity()) }

            SyncStatusManager.setSynced()
        } catch (_: Exception) {
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
                delay(30_000L) // every 30 seconds
                if (SyncStatusManager.status.value != SyncStatusManager.SyncStatus.OFFLINE) {
                    pullSync(userId)
                }
            }
        }
    }

    // ── Per-table subscription helper ─────────────────────────────────────────

    private inline fun <reified Dto> subscribeToTable(
        channel: RealtimeChannel,
        table: String,
        userId: String,
        scope: CoroutineScope,
        crossinline onUpsert: suspend (Dto) -> Unit,
        crossinline onDelete: suspend (Dto) -> Unit
    ) {
        scope.launch {
            channel.postgresChangeFlow<PostgresAction>("public") {
                this.table = table
                filter("user_id", FilterOperator.EQ, userId)
            }.collect { action ->
                try {
                    when (action) {
                        is PostgresAction.Insert -> onUpsert(action.decodeRecord())
                        is PostgresAction.Update -> onUpsert(action.decodeRecord())
                        is PostgresAction.Delete -> onDelete(action.decodeOldRecord())
                        else -> {}
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
