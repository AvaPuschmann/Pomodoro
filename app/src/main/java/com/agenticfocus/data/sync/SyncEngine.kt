package com.agenticfocus.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.agenticfocus.data.dao.SyncQueueDao
import com.agenticfocus.data.entity.DayTaskEntity
import com.agenticfocus.data.entity.DomainEntity
import com.agenticfocus.data.entity.GoalEntity
import com.agenticfocus.data.entity.PomodoroSessionEntity
import com.agenticfocus.data.entity.ProjectEntity
import com.agenticfocus.data.entity.RoutineEntity
import com.agenticfocus.data.entity.RoutineItemEntity
import com.agenticfocus.data.entity.SubtaskEntity
import com.agenticfocus.data.entity.SyncQueueEntity
import com.agenticfocus.data.entity.TaskTemplateEntity
import com.agenticfocus.data.supabase.SupabaseClientProvider
import com.agenticfocus.data.supabase.dto.toDto
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SyncEngine {

    private var syncQueueDao: SyncQueueDao? = null
    private var connectivityProvider: () -> Boolean = { isConnectedDefault() }
    private var context: Context? = null
    var currentUserId: String = ""

    fun initialize(
        context: Context?,
        syncQueueDao: SyncQueueDao,
        connectivityProvider: (() -> Boolean)? = null
    ) {
        this.context = context?.applicationContext
        this.syncQueueDao = syncQueueDao
        this.connectivityProvider = connectivityProvider ?: { isConnectedDefault() }
    }

    fun isConnected(): Boolean = connectivityProvider()

    private fun isConnectedDefault(): Boolean {
        val ctx = context ?: return false
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // ── DayTask ───────────────────────────────────────────────────────────────

    suspend fun upsertDayTask(entity: DayTaskEntity) {
        val dto = entity.toDto(currentUserId)
        if (isConnected()) {
            if (!tryUpsert("day_tasks") { SupabaseClientProvider.client.from("day_tasks").upsert(dto) }) {
                enqueue("day_tasks", entity.id, "UPSERT", Json.encodeToString(dto))
            }
        } else {
            enqueue("day_tasks", entity.id, "UPSERT", Json.encodeToString(dto))
        }
    }

    suspend fun deleteDayTask(id: String) {
        if (isConnected()) {
            if (!tryDelete("day_tasks", id)) {
                enqueue("day_tasks", id, "DELETE", "{}")
            }
        } else {
            enqueue("day_tasks", id, "DELETE", "{}")
        }
    }

    // ── PomodoroSession ───────────────────────────────────────────────────────

    suspend fun upsertPomodoroSession(entity: PomodoroSessionEntity) {
        val dto = entity.toDto(currentUserId)
        if (isConnected()) {
            if (!tryUpsert("pomodoro_sessions") { SupabaseClientProvider.client.from("pomodoro_sessions").upsert(dto) }) {
                enqueue("pomodoro_sessions", entity.id, "UPSERT", Json.encodeToString(dto))
            }
        } else {
            enqueue("pomodoro_sessions", entity.id, "UPSERT", Json.encodeToString(dto))
        }
    }

    // ── Domain ────────────────────────────────────────────────────────────────

    suspend fun upsertDomain(entity: DomainEntity) {
        val dto = entity.toDto(currentUserId)
        if (isConnected()) {
            if (!tryUpsert("domains") { SupabaseClientProvider.client.from("domains").upsert(dto) }) {
                enqueue("domains", entity.id, "UPSERT", Json.encodeToString(dto))
            }
        } else {
            enqueue("domains", entity.id, "UPSERT", Json.encodeToString(dto))
        }
    }

    suspend fun deleteDomain(id: String) {
        if (isConnected()) {
            if (!tryDelete("domains", id)) {
                enqueue("domains", id, "DELETE", "{}")
            }
        } else {
            enqueue("domains", id, "DELETE", "{}")
        }
    }

    // ── TaskTemplate ──────────────────────────────────────────────────────────

    suspend fun upsertTemplate(entity: TaskTemplateEntity) {
        val dto = entity.toDto(currentUserId)
        if (isConnected()) {
            if (!tryUpsert("task_templates") { SupabaseClientProvider.client.from("task_templates").upsert(dto) }) {
                enqueue("task_templates", entity.id, "UPSERT", Json.encodeToString(dto))
            }
        } else {
            enqueue("task_templates", entity.id, "UPSERT", Json.encodeToString(dto))
        }
    }

    suspend fun deleteTemplate(id: String) {
        if (isConnected()) {
            if (!tryDelete("task_templates", id)) {
                enqueue("task_templates", id, "DELETE", "{}")
            }
        } else {
            enqueue("task_templates", id, "DELETE", "{}")
        }
    }

    // ── Subtask ───────────────────────────────────────────────────────────────

    suspend fun upsertSubtask(entity: SubtaskEntity) {
        val dto = entity.toDto(currentUserId)
        if (isConnected()) {
            if (!tryUpsert("subtasks") { SupabaseClientProvider.client.from("subtasks").upsert(dto) }) {
                enqueue("subtasks", entity.id, "UPSERT", Json.encodeToString(dto))
            }
        } else {
            enqueue("subtasks", entity.id, "UPSERT", Json.encodeToString(dto))
        }
    }

    suspend fun deleteSubtask(id: String) {
        if (isConnected()) {
            if (!tryDelete("subtasks", id)) {
                enqueue("subtasks", id, "DELETE", "{}")
            }
        } else {
            enqueue("subtasks", id, "DELETE", "{}")
        }
    }

    // ── Goal ──────────────────────────────────────────────────────────────────

    suspend fun upsertGoal(entity: GoalEntity) {
        val dto = entity.toDto(currentUserId)
        if (isConnected()) {
            if (!tryUpsert("goals") { SupabaseClientProvider.client.from("goals").upsert(dto) }) {
                enqueue("goals", entity.id, "UPSERT", Json.encodeToString(dto))
            }
        } else {
            enqueue("goals", entity.id, "UPSERT", Json.encodeToString(dto))
        }
    }

    suspend fun deleteGoal(id: String) {
        if (isConnected()) {
            if (!tryDelete("goals", id)) {
                enqueue("goals", id, "DELETE", "{}")
            }
        } else {
            enqueue("goals", id, "DELETE", "{}")
        }
    }

    // ── Routine ───────────────────────────────────────────────────────────────

    suspend fun upsertRoutine(entity: RoutineEntity) {
        val dto = entity.toDto()
        if (isConnected()) {
            if (!tryUpsert("routines") { SupabaseClientProvider.client.from("routines").upsert(dto) }) {
                enqueue("routines", entity.id, "UPSERT", Json.encodeToString(dto))
            }
        } else {
            enqueue("routines", entity.id, "UPSERT", Json.encodeToString(dto))
        }
    }

    suspend fun deleteRoutine(id: String) {
        if (isConnected()) {
            if (!tryDelete("routines", id)) enqueue("routines", id, "DELETE", "{}")
        } else {
            enqueue("routines", id, "DELETE", "{}")
        }
    }

    // ── RoutineItem ───────────────────────────────────────────────────────────

    suspend fun upsertRoutineItem(entity: RoutineItemEntity) {
        val dto = entity.toDto()
        if (isConnected()) {
            if (!tryUpsert("routine_items") { SupabaseClientProvider.client.from("routine_items").upsert(dto) }) {
                enqueue("routine_items", entity.id, "UPSERT", Json.encodeToString(dto))
            }
        } else {
            enqueue("routine_items", entity.id, "UPSERT", Json.encodeToString(dto))
        }
    }

    suspend fun deleteRoutineItem(id: String) {
        if (isConnected()) {
            if (!tryDelete("routine_items", id)) enqueue("routine_items", id, "DELETE", "{}")
        } else {
            enqueue("routine_items", id, "DELETE", "{}")
        }
    }

    // ── Project — Story 17-8 / Sprint 17 ──────────────────────────────────────

    suspend fun upsertProject(entity: ProjectEntity) {
        val dto = entity.toDto()
        if (isConnected()) {
            if (!tryUpsert("projects") { SupabaseClientProvider.client.from("projects").upsert(dto) }) {
                enqueue("projects", entity.id, "UPSERT", Json.encodeToString(dto))
            }
        } else {
            enqueue("projects", entity.id, "UPSERT", Json.encodeToString(dto))
        }
    }

    suspend fun deleteProject(id: String) {
        if (isConnected()) {
            if (!tryDelete("projects", id)) enqueue("projects", id, "DELETE", "{}")
        } else {
            enqueue("projects", id, "DELETE", "{}")
        }
    }

    // ── Flush (process offline queue) ─────────────────────────────────────────

    suspend fun flush() {
        val dao = syncQueueDao ?: return
        SyncStatusManager.setSyncing()
        try {
            val entries = dao.getAll()
            for (entry in entries) {
                if (entry.retryCount >= MAX_RETRIES) {
                    dao.deleteById(entry.id)
                    continue
                }
                val success = replayEntry(entry)
                if (success) dao.deleteById(entry.id)
                else dao.incrementRetry(entry.id)
            }
            SyncStatusManager.setSynced()
        } catch (_: Exception) {
            SyncStatusManager.setError()
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private suspend fun tryUpsert(table: String, block: suspend () -> Unit): Boolean {
        return try {
            block()
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun tryDelete(table: String, id: String): Boolean {
        return try {
            SupabaseClientProvider.client.from(table).delete {
                filter { eq("id", id) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun enqueue(entityType: String, entityId: String, operation: String, payload: String) {
        syncQueueDao?.insert(
            SyncQueueEntity(
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                payload = payload
            )
        )
    }

    private suspend fun replayEntry(entry: SyncQueueEntity): Boolean {
        return when (entry.operation) {
            "DELETE" -> tryDelete(entry.entityType, entry.entityId)
            "UPSERT" -> replayUpsert(entry)
            else -> true // unknown operation — discard
        }
    }

    private suspend fun replayUpsert(entry: SyncQueueEntity): Boolean {
        return try {
            when (entry.entityType) {
                "day_tasks" -> {
                    val dto = Json.decodeFromString<com.agenticfocus.data.supabase.dto.DayTaskDto>(entry.payload)
                    SupabaseClientProvider.client.from("day_tasks").upsert(dto)
                }
                "pomodoro_sessions" -> {
                    val dto = Json.decodeFromString<com.agenticfocus.data.supabase.dto.PomodoroSessionDto>(entry.payload)
                    SupabaseClientProvider.client.from("pomodoro_sessions").upsert(dto)
                }
                "domains" -> {
                    val dto = Json.decodeFromString<com.agenticfocus.data.supabase.dto.DomainDto>(entry.payload)
                    SupabaseClientProvider.client.from("domains").upsert(dto)
                }
                "task_templates" -> {
                    val dto = Json.decodeFromString<com.agenticfocus.data.supabase.dto.TaskTemplateDto>(entry.payload)
                    SupabaseClientProvider.client.from("task_templates").upsert(dto)
                }
                "subtasks" -> {
                    val dto = Json.decodeFromString<com.agenticfocus.data.supabase.dto.SubtaskDto>(entry.payload)
                    SupabaseClientProvider.client.from("subtasks").upsert(dto)
                }
                "goals" -> {
                    val dto = Json.decodeFromString<com.agenticfocus.data.supabase.dto.GoalDto>(entry.payload)
                    SupabaseClientProvider.client.from("goals").upsert(dto)
                }
                "routines" -> {
                    val dto = Json.decodeFromString<com.agenticfocus.data.supabase.dto.RoutineDto>(entry.payload)
                    SupabaseClientProvider.client.from("routines").upsert(dto)
                }
                "routine_items" -> {
                    val dto = Json.decodeFromString<com.agenticfocus.data.supabase.dto.RoutineItemDto>(entry.payload)
                    SupabaseClientProvider.client.from("routine_items").upsert(dto)
                }
                "projects" -> {
                    val dto = Json.decodeFromString<com.agenticfocus.data.supabase.dto.ProjectDto>(entry.payload)
                    SupabaseClientProvider.client.from("projects").upsert(dto)
                }
                else -> { /* unknown table — discard */ }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    const val MAX_RETRIES = 5
}
