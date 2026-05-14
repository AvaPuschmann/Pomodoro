package com.agenticfocus.data.repository

import androidx.room.withTransaction
import com.agenticfocus.data.dao.DayTaskDao
import com.agenticfocus.data.dao.ProjectDao
import com.agenticfocus.data.db.AppDatabase
import com.agenticfocus.data.entity.KanbanStatus
import com.agenticfocus.data.entity.ProjectEntity
import com.agenticfocus.data.sync.SyncEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

data class ProjectStats(
    val taskCount: Int,
    val plannedSum: Int,
    val completedSum: Int,
)

class ProjectRepository(
    private val db: AppDatabase,
    private val projectDao: ProjectDao,
    private val dayTaskDao: DayTaskDao,
) {
    // ── Reads ──────────────────────────────────────────────────────
    fun observeActive(userId: String): Flow<List<ProjectEntity>> =
        projectDao.observeActive(userId)

    fun observeAll(userId: String): Flow<List<ProjectEntity>> =
        projectDao.observeAll(userId)

    // Stats agrégée — Flow réactif [F18/D56, F-G fix N+1].
    fun observeStats(userId: String): Flow<Map<String, ProjectStats>> =
        dayTaskDao.observeAllProjectStats(userId).map { rows ->
            rows.associate { row ->
                row.projectId to ProjectStats(row.taskCount, row.plannedSum, row.completedSum)
            }
        }

    // ── Position helpers — fractional indexing [D24/F-3F/D43] ──────
    suspend fun nextPositionInColumn(userId: String, status: String): Double =
        (projectDao.getMaxPositionInColumn(userId, status) ?: 0.0) + 1024.0

    suspend fun insertBetween(
        userId: String, status: String,
        beforePos: Double, afterPos: Double,
    ): Double {
        val newPos = (beforePos + afterPos) / 2.0
        return if (newPos - beforePos < 1.0 || afterPos - newPos < 1.0) {
            renumberColumn(userId, status)
            (projectDao.getMaxPositionInColumn(userId, status) ?: 0.0) + 1024.0
        } else newPos
    }

    // Transaction critique — si crash en milieu, positions inconsistantes mais
    // récupérables (idempotent : relancer renumber rétablit gaps de 1024).
    suspend fun renumberColumn(userId: String, status: String) {
        db.withTransaction {
            val items = projectDao.getByStatusOrdered(userId, status)
            val now = System.currentTimeMillis()
            items.forEachIndexed { i, p ->
                projectDao.upsert(
                    p.copy(kanbanPosition = (i + 1) * 1024.0, updatedAt = now)
                )
            }
        }
    }

    // ── CRUD avec timestamps injection [F2/D47] ────────────────────
    // Le ViewModel ne doit JAMAIS calculer System.currentTimeMillis() pour
    // created_at/updated_at — toujours ici.
    // Story 22-4 / Sprint 20 : lifecycle automation started_at/finished_at.
    suspend fun addProject(
        userId: String,
        name: String,
        description: String? = null,
        kanbanStatus: String = KanbanStatus.BACKLOG,
        domainId: String? = null,
        targetDate: Long? = null,
    ): ProjectEntity {
        require(KanbanStatus.isValid(kanbanStatus)) {
            "kanbanStatus invalid: '$kanbanStatus', expected one of ${KanbanStatus.ALL}"
        }
        val now = System.currentTimeMillis()
        // If project is created already in Todo/Doing/Done (not Backlog), startedAt = now.
        // Done at creation is unusual but legal — startedAt = finishedAt = now.
        val startedAt = if (kanbanStatus != KanbanStatus.BACKLOG) now else null
        val finishedAt = if (kanbanStatus == KanbanStatus.DONE) now else null
        val entity = ProjectEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = name.trim(),
            description = description?.takeIf { it.isNotBlank() },
            kanbanStatus = kanbanStatus,
            kanbanPosition = nextPositionInColumn(userId, kanbanStatus),
            targetDate = targetDate,
            domainId = domainId,
            isArchived = false,
            createdAt = now,
            updatedAt = now,
            startedAt = startedAt,
            finishedAt = finishedAt,
        )
        projectDao.upsert(entity)
        try { SyncEngine.upsertProject(entity) } catch (_: Exception) {}
        return entity
    }

    suspend fun updateProject(
        id: String,
        name: String? = null,
        description: String? = null,
        kanbanStatus: String? = null,
        domainId: String? = null,
        targetDate: Long? = null,
    ): ProjectEntity {
        kanbanStatus?.let {
            require(KanbanStatus.isValid(it)) {
                "kanbanStatus invalid: '$it', expected one of ${KanbanStatus.ALL}"
            }
        }
        val existing = projectDao.getById(id)
            ?: error("Project $id not found")
        val updated = existing.copy(
            name = name?.trim() ?: existing.name,
            description = description ?: existing.description,
            kanbanStatus = kanbanStatus ?: existing.kanbanStatus,
            domainId = domainId ?: existing.domainId,
            targetDate = targetDate ?: existing.targetDate,
            updatedAt = System.currentTimeMillis(),
            // createdAt inchangé
        )
        projectDao.upsert(updated)
        try { SyncEngine.upsertProject(updated) } catch (_: Exception) {}
        return updated
    }

    suspend fun archiveProject(id: String) {
        val p = projectDao.getById(id) ?: error("Project $id not found")
        val updated = p.copy(isArchived = true, updatedAt = System.currentTimeMillis())
        projectDao.upsert(updated)
        try { SyncEngine.upsertProject(updated) } catch (_: Exception) {}
    }

    suspend fun unarchiveProject(id: String) {
        val p = projectDao.getById(id) ?: error("Project $id not found")
        val updated = p.copy(isArchived = false, updatedAt = System.currentTimeMillis())
        projectDao.upsert(updated)
        try { SyncEngine.upsertProject(updated) } catch (_: Exception) {}
    }

    // Pas de cascade locale projects → day_tasks [F1/D46] — Supabase FK SET NULL
    // côté serveur applique cascade et émet Realtime UPDATE day_tasks (project_id=null)
    // que mobile reçoit via 17-9. Latency ~500ms acceptable.
    suspend fun deleteProject(id: String) {
        projectDao.deleteById(id)
        try { SyncEngine.deleteProject(id) } catch (_: Exception) {}
    }

    // ── moveKanbanStatus avec calcul fractional indexing position ──
    suspend fun moveKanbanStatus(
        projectId: String,
        newStatus: String,
        beforeId: String? = null,
        afterId: String? = null,
    ) {
        require(KanbanStatus.isValid(newStatus)) {
            "newStatus invalid: '$newStatus', expected one of ${KanbanStatus.ALL}"
        }
        val existing = projectDao.getById(projectId)
            ?: error("Project $projectId not found")
        val userId = existing.userId
        val newPosition = when {
            beforeId == null && afterId == null ->
                nextPositionInColumn(userId, newStatus)
            beforeId != null && afterId != null -> {
                val before = projectDao.getById(beforeId)
                    ?: error("Before project $beforeId not found")
                val after = projectDao.getById(afterId)
                    ?: error("After project $afterId not found")
                insertBetween(userId, newStatus, before.kanbanPosition, after.kanbanPosition)
            }
            beforeId != null -> {
                val before = projectDao.getById(beforeId)
                    ?: error("Before project $beforeId not found")
                before.kanbanPosition + 1024.0
            }
            else /* afterId != null */ -> {
                val after = projectDao.getById(afterId!!)
                    ?: error("After project $afterId not found")
                insertBetween(userId, newStatus, 0.0, after.kanbanPosition)
            }
        }
        // Story 22-4 / Sprint 20 — lifecycle automation [parity desktop projectStore.ts].
        val now = System.currentTimeMillis()
        val (newStartedAt, newFinishedAt) = computeLifecycleTimestamps(
            oldStatus = existing.kanbanStatus,
            newStatus = newStatus,
            existingStartedAt = existing.startedAt,
            existingFinishedAt = existing.finishedAt,
            now = now,
        )
        val updated = existing.copy(
            kanbanStatus = newStatus,
            kanbanPosition = newPosition,
            updatedAt = now,
            startedAt = newStartedAt,
            finishedAt = newFinishedAt,
        )
        projectDao.upsert(updated)
        try { SyncEngine.upsertProject(updated) } catch (_: Exception) {}
    }

    // ── Lier / délier tâche ↔ projet [F3] ──────────────────────────
    suspend fun setProjectIdOnTask(taskId: String, projectId: String?) {
        val now = System.currentTimeMillis()
        dayTaskDao.updateProjectId(taskId, projectId, now)
        dayTaskDao.getById(taskId)?.let {
            try { SyncEngine.upsertDayTask(it) } catch (_: Exception) {}
        }
    }

    companion object {
        // Pure function for testability — same logic used inline in moveKanbanStatus.
        // Returns (newStartedAt, newFinishedAt) given the transition.
        // Story 22-4 / Sprint 20.
        fun computeLifecycleTimestamps(
            oldStatus: String,
            newStatus: String,
            existingStartedAt: Long?,
            existingFinishedAt: Long?,
            now: Long,
        ): Pair<Long?, Long?> {
            val newStartedAt: Long? = when {
                newStatus == KanbanStatus.BACKLOG -> null
                existingStartedAt == null -> now
                else -> existingStartedAt
            }
            val newFinishedAt: Long? = when {
                newStatus == KanbanStatus.BACKLOG -> null
                newStatus == KanbanStatus.DONE && oldStatus != KanbanStatus.DONE -> now
                oldStatus == KanbanStatus.DONE && newStatus != KanbanStatus.DONE -> null
                else -> existingFinishedAt
            }
            return newStartedAt to newFinishedAt
        }
    }
}
