package com.agenticfocus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agenticfocus.data.AppPreferences
import com.agenticfocus.data.db.AppDatabase
import com.agenticfocus.data.entity.ProjectEntity
import com.agenticfocus.data.repository.ProjectRepository
import com.agenticfocus.data.repository.ProjectStats
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectUiState(
    val projects: List<ProjectEntity> = emptyList(),
    val statsByProject: Map<String, ProjectStats> = emptyMap(),
    val wipLimitDoing: Int = 3,  // back-compat (Sprint 17 17-10)
    // Story 18-hotfix 2026-05-13 : config Kanban par colonne (0 = illimité)
    val wipLimitByStatus: Map<String, Int> = mapOf(
        com.agenticfocus.data.entity.KanbanStatus.BACKLOG to 0,
        com.agenticfocus.data.entity.KanbanStatus.TODO to 0,
        com.agenticfocus.data.entity.KanbanStatus.DOING to 3,
        com.agenticfocus.data.entity.KanbanStatus.DONE to 0,
    ),
    val labelsByStatus: Map<String, String> = mapOf(
        com.agenticfocus.data.entity.KanbanStatus.BACKLOG to "Backlog",
        com.agenticfocus.data.entity.KanbanStatus.TODO to "Todo",
        com.agenticfocus.data.entity.KanbanStatus.DOING to "Doing",
        com.agenticfocus.data.entity.KanbanStatus.DONE to "Done",
    ),
    val sortOrder: SortOrder = SortOrder.UPDATED_DESC,
    val showArchived: Boolean = false,
    val isLoading: Boolean = false,
)

enum class SortOrder { UPDATED_DESC, CREATED_DESC, NAME_ASC, TARGET_DATE_ASC }

@OptIn(FlowPreview::class)
class ProjectViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = ProjectRepository(
        db = db,
        projectDao = db.projectDao(),
        dayTaskDao = db.dayTaskDao(),
    )
    private val prefs = AppPreferences(application)

    private val _state = MutableStateFlow(
        ProjectUiState(
            wipLimitDoing = prefs.wipLimitDoing,
            wipLimitByStatus = readWipLimitsFromPrefs(),
            labelsByStatus = readLabelsFromPrefs(),
        )
    )

    private fun readWipLimitsFromPrefs(): Map<String, Int> = mapOf(
        com.agenticfocus.data.entity.KanbanStatus.BACKLOG to prefs.wipLimitBacklog,
        com.agenticfocus.data.entity.KanbanStatus.TODO to prefs.wipLimitTodo,
        com.agenticfocus.data.entity.KanbanStatus.DOING to prefs.wipLimitDoing,
        com.agenticfocus.data.entity.KanbanStatus.DONE to prefs.wipLimitDone,
    )

    private fun readLabelsFromPrefs(): Map<String, String> = mapOf(
        com.agenticfocus.data.entity.KanbanStatus.BACKLOG to prefs.labelBacklog,
        com.agenticfocus.data.entity.KanbanStatus.TODO to prefs.labelTodo,
        com.agenticfocus.data.entity.KanbanStatus.DOING to prefs.labelDoing,
        com.agenticfocus.data.entity.KanbanStatus.DONE to prefs.labelDone,
    )
    val state: StateFlow<ProjectUiState> = _state.asStateFlow()

    private var currentUserId: String? = null
    private var observeJob: Job? = null

    // Debounce moveKanbanStatus [F-L/F-3E/D42] : drag-drop rapide en cascade
    // (Backlog → Todo → Doing → Done en 200ms) ne fait qu'un seul push Supabase.
    private data class MoveCommand(
        val id: String,
        val newStatus: String,
        val beforeId: String?,
        val afterId: String?,
    )
    private val moveStatusFlow = MutableSharedFlow<MoveCommand>(extraBufferCapacity = 16)

    init {
        viewModelScope.launch {
            moveStatusFlow
                .debounce(300)
                .collect { cmd ->
                    try {
                        repository.moveKanbanStatus(cmd.id, cmd.newStatus, cmd.beforeId, cmd.afterId)
                    } catch (_: Exception) { /* validation invalide silencieusement */ }
                }
        }
    }

    fun setUserId(userId: String) {
        if (currentUserId == userId) return  // idempotent — évite double-collect au re-login
        currentUserId = userId
        startObserving(userId)
    }

    private fun startObserving(userId: String) {
        observeJob?.cancel()
        observeJob = combine(
            repository.observeAll(userId),
            repository.observeStats(userId),
        ) { projects, stats -> projects to stats }
            .onEach { (projects, stats) ->
                val currentState = _state.value
                val filtered = if (currentState.showArchived) projects else projects.filter { !it.isArchived }
                val sorted = applySortOrder(filtered, currentState.sortOrder)
                _state.update {
                    it.copy(projects = sorted, statsByProject = stats)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun applySortOrder(list: List<ProjectEntity>, order: SortOrder): List<ProjectEntity> =
        when (order) {
            SortOrder.UPDATED_DESC -> list.sortedByDescending { it.updatedAt }
            SortOrder.CREATED_DESC -> list.sortedByDescending { it.createdAt }
            SortOrder.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            SortOrder.TARGET_DATE_ASC -> list.sortedWith(
                compareBy(nullsLast()) { it.targetDate }
            )
        }

    // ── UI intents — Repository writes ─────────────────────────────
    fun addProject(
        name: String,
        description: String? = null,
        kanbanStatus: String = com.agenticfocus.data.entity.KanbanStatus.BACKLOG,
        domainId: String? = null,
        targetDate: Long? = null,
        tagIds: List<String> = emptyList(),  // Story 22-2c / Sprint 20
    ) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            try {
                repository.addProject(userId, name, description, kanbanStatus, domainId, targetDate, tagIds)
            } catch (_: Exception) { /* validation invalide silencieusement */ }
        }
    }

    fun updateProject(
        id: String,
        name: String? = null,
        description: String? = null,
        kanbanStatus: String? = null,
        domainId: String? = null,
        targetDate: Long? = null,
        tagIds: List<String>? = null,  // Story 22-2c / Sprint 20
    ) {
        viewModelScope.launch {
            try {
                repository.updateProject(id, name, description, kanbanStatus, domainId, targetDate, tagIds)
            } catch (_: Exception) {}
        }
    }

    fun archiveProject(id: String) {
        viewModelScope.launch { runCatching { repository.archiveProject(id) } }
    }

    fun unarchiveProject(id: String) {
        viewModelScope.launch { runCatching { repository.unarchiveProject(id) } }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch { runCatching { repository.deleteProject(id) } }
    }

    fun moveKanbanStatus(id: String, newStatus: String, beforeId: String? = null, afterId: String? = null) {
        viewModelScope.launch {
            moveStatusFlow.emit(MoveCommand(id, newStatus, beforeId, afterId))
        }
    }

    fun setProjectIdOnTask(taskId: String, projectId: String?) {
        viewModelScope.launch { runCatching { repository.setProjectIdOnTask(taskId, projectId) } }
    }

    // ── Tasks observation for ProjectDetailScreen — Story 18-4 ─────
    fun observeTasksForProject(projectId: String): kotlinx.coroutines.flow.Flow<List<com.agenticfocus.data.entity.DayTaskEntity>> =
        db.dayTaskDao().observeTasksForProject(projectId)

    /**
     * Toggle bidirectionnel [D31] :
     *  - date == null (backlog) → set today
     *  - date == today          → set null (retour backlog)
     *  - sinon (passé/futur)    → set today
     * Push sync via SyncEngine.upsertDayTask post-update.
     */
    fun planTaskForToday(taskId: String) {
        viewModelScope.launch {
            runCatching {
                val task = db.dayTaskDao().getById(taskId) ?: return@launch
                val today = todayDateString()
                val dao = db.dayTaskDao()
                if (task.date == today) {
                    dao.unscheduleTask(taskId)
                } else {
                    dao.scheduleTask(taskId, today)
                }
                dao.getById(taskId)?.let {
                    try { com.agenticfocus.data.sync.SyncEngine.upsertDayTask(it) } catch (_: Exception) {}
                }
            }
        }
    }

    private fun todayDateString(): String {
        val now = java.time.LocalDate.now()
        return "%04d-%02d-%02d".format(now.year, now.monthValue, now.dayOfMonth)
    }

    // ── Story 22-7 / Sprint 20 — Task mutations from ProjectTasksSheet ──────
    // Direct DAO mutations + SyncEngine push, scope-independent (works pour tasks
    // de n'importe quel projet, n'importe quelle date).

    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            runCatching {
                val task = db.dayTaskDao().getById(taskId) ?: return@launch
                val updated = task.copy(
                    isCompleted = !task.isCompleted,
                    updatedAt = System.currentTimeMillis()
                )
                db.dayTaskDao().upsert(updated)
                try { com.agenticfocus.data.sync.SyncEngine.upsertDayTask(updated) } catch (_: Exception) {}
            }
        }
    }

    fun updateTaskPlannedPomodoros(taskId: String, delta: Int) {
        viewModelScope.launch {
            runCatching {
                val task = db.dayTaskDao().getById(taskId) ?: return@launch
                val min = task.completedPomodoros.coerceAtLeast(0)
                val newPlanned = (task.plannedPomodoros + delta).coerceIn(min, 99)
                if (newPlanned == task.plannedPomodoros) return@launch
                val updated = task.copy(
                    plannedPomodoros = newPlanned,
                    updatedAt = System.currentTimeMillis()
                )
                db.dayTaskDao().upsert(updated)
                try { com.agenticfocus.data.sync.SyncEngine.upsertDayTask(updated) } catch (_: Exception) {}
            }
        }
    }

    // ── Filters / sort / preferences ───────────────────────────────
    fun setSortOrder(order: SortOrder) {
        _state.update { it.copy(sortOrder = order) }
        currentUserId?.let { startObserving(it) }
    }

    fun setShowArchived(show: Boolean) {
        _state.update { it.copy(showArchived = show) }
        currentUserId?.let { startObserving(it) }
    }

    fun setWipLimitDoing(n: Int) {
        prefs.wipLimitDoing = n
        _state.update {
            it.copy(
                wipLimitDoing = n,
                wipLimitByStatus = it.wipLimitByStatus + (com.agenticfocus.data.entity.KanbanStatus.DOING to n)
            )
        }
    }

    /** Story 18-hotfix 2026-05-13 — set label + WIP limit for one Kanban column. */
    fun setKanbanColumnConfig(status: String, label: String, wipLimit: Int) {
        val clean = label.trim().ifBlank {
            when (status) {
                com.agenticfocus.data.entity.KanbanStatus.BACKLOG -> "Backlog"
                com.agenticfocus.data.entity.KanbanStatus.TODO -> "Todo"
                com.agenticfocus.data.entity.KanbanStatus.DOING -> "Doing"
                com.agenticfocus.data.entity.KanbanStatus.DONE -> "Done"
                else -> status
            }
        }
        val limit = wipLimit.coerceAtLeast(0)
        when (status) {
            com.agenticfocus.data.entity.KanbanStatus.BACKLOG -> { prefs.labelBacklog = clean; prefs.wipLimitBacklog = limit }
            com.agenticfocus.data.entity.KanbanStatus.TODO -> { prefs.labelTodo = clean; prefs.wipLimitTodo = limit }
            com.agenticfocus.data.entity.KanbanStatus.DOING -> { prefs.labelDoing = clean; prefs.wipLimitDoing = limit }
            com.agenticfocus.data.entity.KanbanStatus.DONE -> { prefs.labelDone = clean; prefs.wipLimitDone = limit }
        }
        _state.update {
            it.copy(
                labelsByStatus = it.labelsByStatus + (status to clean),
                wipLimitByStatus = it.wipLimitByStatus + (status to limit),
                wipLimitDoing = if (status == com.agenticfocus.data.entity.KanbanStatus.DOING) limit else it.wipLimitDoing,
            )
        }
    }

    // ── ProjectDetailScreen sort per-projet [F8/D53] ───────────────
    fun setSortOrderForProject(projectId: String, order: SortOrder) {
        prefs.setProjectDetailSortOrder(projectId, order.name)
    }

    fun getSortOrderForProject(projectId: String): SortOrder =
        prefs.getProjectDetailSortOrder(projectId)
            ?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() }
            ?: SortOrder.UPDATED_DESC
}
