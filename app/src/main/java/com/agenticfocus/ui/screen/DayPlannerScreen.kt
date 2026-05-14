package com.agenticfocus.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.widthIn
import com.agenticfocus.ui.components.ToggleChip
import com.agenticfocus.ui.components.formatDueDate
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agenticfocus.R
import com.agenticfocus.data.entity.DomainEntity
import com.agenticfocus.data.entity.SubtaskEntity
import com.agenticfocus.data.entity.TaskTemplateEntity
import com.agenticfocus.data.sync.SyncEngine
import com.agenticfocus.data.sync.SyncStatusManager
import com.agenticfocus.ui.components.DayTaskRow
import com.agenticfocus.ui.components.GoalsCard
import com.agenticfocus.ui.theme.GlassWhite
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.ui.theme.TomatoRed
import com.agenticfocus.viewmodel.DayPlannerViewModel
import com.agenticfocus.viewmodel.LibraryViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val frenchDateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPlannerScreen(
    viewModel: DayPlannerViewModel,
    libraryViewModel: LibraryViewModel,
    projectViewModel: com.agenticfocus.viewmodel.ProjectViewModel? = null,  // Story 18-6 — null = FEATURE_PROJECTS off
    contentPadding: PaddingValues = PaddingValues()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val backlogTasks by viewModel.backlogTasks.collectAsStateWithLifecycle()
    val subtasksMap by viewModel.subtasksMap.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.state.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current
    val goalsEnabled = remember { com.agenticfocus.data.AppPreferences(context).featureGoals }

    // Routine type lookup: routineItemId → "morning" | "evening"
    // Reloads periodically to catch data from Supabase pull (runs 5s after mount)
    var routineTypeMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var routineLoadTick by remember { mutableStateOf(0) }
    LaunchedEffect(routineLoadTick) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val db = com.agenticfocus.data.db.AppDatabase.getInstance(context)
            val dao = db.routineDao()
            val userId = com.agenticfocus.data.sync.SyncEngine.currentUserId
            if (userId.isNotEmpty()) {
                val routines = dao.getActiveRoutines(userId)
                val items = dao.getAllRoutineItems(userId)
                val routineTypeById = routines.associate { it.id to it.type }
                routineTypeMap = items.mapNotNull { item ->
                    routineTypeById[item.routineId]?.let { type -> item.id to type }
                }.toMap()
            }
        }
        if (routineLoadTick == 0) {
            kotlinx.coroutines.delay(8000)
            routineLoadTick = 1
        }
    }

    val lazyListState = rememberLazyListState()

    // Split tasks: active vs done (isCompleted overrides pomodoro count)
    val activeTasks = state.tasks.filter {
        !it.isCompleted && (it.plannedPomodoros == 0 || it.completedPomodoros < it.plannedPomodoros)
    }
    val doneTasks = state.tasks.filter {
        it.isCompleted || (it.plannedPomodoros > 0 && it.completedPomodoros >= it.plannedPomodoros)
    }

    // Reorder maps LazyColumn index (active-only list) → original state.tasks index
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (from.index < activeTasks.size && to.index < activeTasks.size) {
            val fromId = activeTasks[from.index].id
            val toId = activeTasks[to.index].id
            val fromIdx = state.tasks.indexOfFirst { it.id == fromId }
            val toIdx = state.tasks.indexOfFirst { it.id == toId }
            if (fromIdx >= 0 && toIdx >= 0) viewModel.reorderTasks(fromIdx, toIdx)
        }
    }

    var newTaskText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val totalPlanned = state.tasks.sumOf { it.plannedPomodoros }
    val totalCompleted = state.tasks.sumOf { it.completedPomodoros }
    val isOverCapacity = totalPlanned > DayPlannerViewModel.DAILY_CAPACITY
    val totalStoryPointsPlanned = state.tasks.sumOf { it.storyPoints }
    val totalStoryPointsCompleted = state.tasks
        .filter { it.isCompleted || (it.plannedPomodoros > 0 && it.completedPomodoros >= it.plannedPomodoros) }
        .sumOf { it.storyPoints }

    val eodTime by remember { derivedStateOf { computeEndOfDay(state.tasks) } }

    var showLibrarySheet by rememberSaveable { mutableStateOf(false) }
    var showAddTaskSheet by rememberSaveable { mutableStateOf(false) }
    var showBacklogSheet by rememberSaveable { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<com.agenticfocus.viewmodel.DayTask?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val addTaskSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editTaskSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val backlogSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.nature_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.30f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                // Fix 2026-05-13 : appliquer le TOP padding aussi (TopAppBar globale
                // ajoutée Sprint 18-1bis Drawer) sinon DateNavigationHeader caché
                // sous la topbar = boutons gauche/droite inaccessibles.
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding()
                )
                .imePadding()
        ) {
            SyncStatusRow()
            DateNavigationHeader(viewModel = viewModel)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildString {
                        append("$totalCompleted / $totalPlanned 🍅")
                        if (totalStoryPointsPlanned > 0) {
                            append(" • $totalStoryPointsCompleted / $totalStoryPointsPlanned 💎")
                        }
                    },
                    color = if (isOverCapacity) TomatoRed else SubtleWhite,
                    fontSize = 14.sp
                )
            }

            if (eodTime != null) {
                Text(
                    text = "Fin de journée estimée : $eodTime",
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (goalsEnabled) {
                GoalsCard(
                    goals = goals,
                    selectedDate = selectedDate,
                    userId = SyncEngine.currentUserId,
                    onSave = { goal -> viewModel.saveGoal(goal) },
                    onToggle = { goal -> viewModel.toggleGoal(goal) }
                )
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f)
            ) {
                // ── Section active ─────────────────────────────────────────
                itemsIndexed(activeTasks, key = { _, task -> task.id }) { _, task ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.StartToEnd) {
                                viewModel.removeTask(task.id)
                                true
                            } else false
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(TomatoRed)
                                        .padding(start = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text("Supprimer", color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                    ) {
                        ReorderableItem(reorderableState, key = task.id) { isDragging ->
                            Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Surface(
                                    color = if (isDragging)
                                        Color.White.copy(alpha = 0.22f)
                                    else
                                        Color.Black.copy(alpha = 0.50f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                                ) {
                                    DayTaskRow(
                                        task = task,
                                        isActive = task.id == state.activeTaskId,
                                        dragHandleModifier = Modifier.draggableHandle(),
                                        onPlay = { viewModel.activateTask(task) },
                                        onIncreasePlanned = { viewModel.updatePlanned(task.id, +1) },
                                        onDecreasePlanned = { viewModel.updatePlanned(task.id, -1) },
                                        onNameChange = { viewModel.updateName(task.id, it) },
                                        onEdit = { editingTask = task },
                                        onToggleComplete = { viewModel.toggleCompletion(task.id) },
                                        subtasks = subtasksMap[task.id] ?: emptyList(),
                                        onToggleSubtask = { viewModel.toggleSubtask(it) },
                                        domainColor = task.domainId?.let { id ->
                                            libraryState.domains.find { it.id == id }
                                                ?.let { runCatching { Color(it.color.toColorInt()) }.getOrNull() }
                                        },
                                        routineType = task.routineItemId?.let { routineTypeMap[it] },
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Section "Terminées" ─────────────────────────────────────
                if (doneTasks.isNotEmpty()) {
                    item(key = "done_section_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(alpha = 0.20f)
                            )
                            Text(
                                text = "Terminées (${doneTasks.size})",
                                color = SubtleWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(alpha = 0.20f)
                            )
                        }
                    }

                    items(doneTasks, key = { it.id }) { task ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.StartToEnd) {
                                    viewModel.removeTask(task.id)
                                    true
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(TomatoRed)
                                            .padding(start = 16.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text("Supprimer", color = Color.White, fontSize = 14.sp)
                                    }
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .alpha(0.55f)
                            ) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.50f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                                ) {
                                    DayTaskRow(
                                        task = task,
                                        isActive = false,
                                        dragHandleModifier = Modifier,
                                        showDragHandle = false,
                                        onPlay = { viewModel.activateTask(task) },
                                        onIncreasePlanned = { viewModel.updatePlanned(task.id, +1) },
                                        onDecreasePlanned = { viewModel.updatePlanned(task.id, -1) },
                                        onNameChange = { viewModel.updateName(task.id, it) },
                                        onEdit = { editingTask = task },
                                        onToggleComplete = { viewModel.toggleCompletion(task.id) },
                                        subtasks = subtasksMap[task.id] ?: emptyList(),
                                        onToggleSubtask = { viewModel.toggleSubtask(it) },
                                        domainColor = task.domainId?.let { id ->
                                            libraryState.domains.find { it.id == id }
                                                ?.let { runCatching { Color(it.color.toColorInt()) }.getOrNull() }
                                        },
                                        routineType = task.routineItemId?.let { routineTypeMap[it] },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // C3 — Bottom action bar: only 3 buttons (Backlog, Biblio, Tâche)
            // Removed: free-form "Nouvelle tâche..." text input — task creation now
            // goes through the dedicated AddTaskForm sheet (Tâche button) for richer
            // metadata (impact/urgence/date), or the Backlog sheet for backlog tasks.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Backlog button — bleu
                    Button(
                        onClick = { showBacklogSheet = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Text("Backlog", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    // Bibliothèque button — violet
                    Button(
                        onClick = { showLibrarySheet = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                    ) {
                        Text("Biblio", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    // Tâche button — vert
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            showAddTaskSheet = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
                    ) {
                        Text("Tâche", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    // Backlog sheet
    if (showBacklogSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBacklogSheet = false },
            sheetState = backlogSheetState,
            containerColor = Color(0xFF1A1A1A)
        ) {
            BacklogSheet(
                tasks = backlogTasks,
                selectedDate = selectedDate,
                onAdd = { name -> viewModel.addToBacklog(name) },
                onScheduleToday = { task ->
                    viewModel.scheduleFromBacklog(task, selectedDate)
                },
                onScheduleDate = { task, date ->
                    viewModel.scheduleFromBacklog(task, date)
                },
                onDelete = { task -> viewModel.removeTask(task.id) }
            )
        }
    }

    // Library bottom sheet
    if (showLibrarySheet) {
        ModalBottomSheet(
            onDismissRequest = { showLibrarySheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1A1A1A)
        ) {
            LibraryPicker(
                domains = libraryState.domains,
                templatesByDomain = libraryState.templatesByDomain,
                onSelect = { template ->
                    viewModel.addTaskFromTemplate(template)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showLibrarySheet = false
                    }
                }
            )
        }
    }

    // Edit task sheet
    editingTask?.let { task ->
        ModalBottomSheet(
            onDismissRequest = { editingTask = null },
            sheetState = editTaskSheetState,
            containerColor = Color(0xFF1A1A1A)
        ) {
            EditTaskForm(
                task = task,
                currentDate = selectedDate,
                domains = libraryState.domains,
                projects = projectViewModel?.state?.collectAsStateWithLifecycle()?.value?.projects ?: emptyList(),
                onProjectChanged = { newProjectId -> projectViewModel?.setProjectIdOnTask(task.id, newProjectId) },
                loadSubtasks = { taskId -> viewModel.getSubtasksForTask(taskId) },
                onSave = { name, note, impact, urgency, scheduledDate, plannedPomodoros, storyPoints, domainId, dueDate, subtasks, originalIds ->
                    viewModel.updateTaskDetails(
                        id = task.id,
                        name = name,
                        note = note,
                        impact = impact,
                        urgency = urgency,
                        scheduledDate = scheduledDate,
                        plannedPomodoros = plannedPomodoros,
                        storyPoints = storyPoints,
                        domainId = domainId,
                        dueDate = dueDate,
                        newSubtasks = subtasks,
                        originalSubtaskIds = originalIds
                    )
                    scope.launch { editTaskSheetState.hide() }.invokeOnCompletion { editingTask = null }
                },
                onDismiss = {
                    scope.launch { editTaskSheetState.hide() }.invokeOnCompletion { editingTask = null }
                }
            )
        }
    }

    // Add task form sheet
    if (showAddTaskSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddTaskSheet = false },
            sheetState = addTaskSheetState,
            containerColor = Color(0xFF1A1A1A)
        ) {
            AddTaskForm(
                initialName = newTaskText,
                initialDate = selectedDate,
                domains = libraryState.domains,
                onAdd = { name, impact, urgency, scheduledDate, note, plannedPomodoros, storyPoints, domainId, dueDate ->
                    viewModel.addTask(
                        name = name,
                        impact = impact,
                        urgency = urgency,
                        scheduledDate = scheduledDate,
                        note = note,
                        plannedPomodoros = plannedPomodoros,
                        storyPoints = storyPoints,
                        domainId = domainId,
                        dueDate = dueDate
                    )
                    newTaskText = ""
                    scope.launch { addTaskSheetState.hide() }.invokeOnCompletion {
                        showAddTaskSheet = false
                    }
                },
                onDismiss = {
                    scope.launch { addTaskSheetState.hide() }.invokeOnCompletion {
                        showAddTaskSheet = false
                    }
                }
            )
        }
    }
}

@Composable
private fun DateNavigationHeader(viewModel: DayPlannerViewModel) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val isToday = selectedDate == LocalDate.now()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { viewModel.goToPreviousDay() }) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Jour précédent",
                tint = TextWhite
            )
        }
        Text(
            text = selectedDate.format(frenchDateFormatter).replaceFirstChar { it.uppercase() },
            color = if (isToday) TextWhite else SubtleWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { viewModel.goToToday() }
        )
        IconButton(onClick = { viewModel.goToNextDay() }) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Jour suivant",
                tint = TextWhite
            )
        }
    }
}

@Composable
private fun SyncStatusRow() {
    val status by SyncStatusManager.status.collectAsStateWithLifecycle()
    val lastSyncAt by SyncStatusManager.lastSyncAt.collectAsStateWithLifecycle()

    val (emoji, label) = when (status) {
        SyncStatusManager.SyncStatus.SYNCED -> {
            val timeStr = lastSyncAt?.let { formatRelativeTime(it) } ?: ""
            "✅" to if (timeStr.isNotEmpty()) "Synchronisé · $timeStr" else "Synchronisé"
        }
        SyncStatusManager.SyncStatus.SYNCING -> "🔄" to "Synchronisation…"
        SyncStatusManager.SyncStatus.OFFLINE -> "📴" to "Hors ligne"
        SyncStatusManager.SyncStatus.ERROR   -> "❌" to "Erreur sync"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "$emoji $label", fontSize = 12.sp, color = SubtleWhite)
    }
}

private fun formatRelativeTime(timestampMs: Long): String {
    val deltaMs = System.currentTimeMillis() - timestampMs
    val minutes = deltaMs / 60_000
    val hours = deltaMs / 3_600_000
    return when {
        minutes < 1  -> "à l'instant"
        minutes < 60 -> "il y a ${minutes}m"
        hours < 24   -> "il y a ${hours}h"
        else         -> "il y a ${hours / 24}j"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddTaskForm(
    initialName: String,
    initialDate: LocalDate,
    domains: List<DomainEntity> = emptyList(),
    // Story 18-7 — pre-fill depuis ProjectDetailScreen. null = workflow Day Planner classique.
    prefilledProjectName: String? = null,
    onAdd: (
        name: String,
        impact: String?,
        urgency: String?,
        scheduledDate: LocalDate?,
        note: String?,
        plannedPomodoros: Int,
        storyPoints: Int,
        domainId: String?,
        dueDate: Long?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var note by remember { mutableStateOf("") }
    var impact by remember { mutableStateOf<String?>(null) }
    var urgency by remember { mutableStateOf<String?>(null) }
    var scheduledDate by remember { mutableStateOf<LocalDate?>(initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Bug 2 — fields aligned with EditTaskForm
    var plannedPomodoros by remember { mutableStateOf(1) }
    var storyPointsText by remember { mutableStateOf("0") }
    var selectedDomainId by remember { mutableStateOf<String?>(null) }
    var domainMenuExpanded by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    val selectedDomain = domains.firstOrNull { it.id == selectedDomainId }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Nouvelle tâche",
            color = TextWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )

        // Story 18-7 — Chip non-interactif indiquant le projet pré-rempli depuis ProjectDetailScreen
        if (prefilledProjectName != null) {
            Surface(
                color = Color(0xFFE53935).copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = "📁 Projet : $prefilledProjectName",
                    color = TomatoRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nom *", color = SubtleWhite) },
            placeholder = { Text("Nom de la tâche", color = SubtleWhite) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TomatoRed,
                unfocusedBorderColor = GlassWhite,
                focusedContainerColor = GlassWhite,
                unfocusedContainerColor = GlassWhite,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                cursorColor = TomatoRed
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Note (parity with EditTaskForm)
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Note (optionnel)", color = SubtleWhite) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TomatoRed, unfocusedBorderColor = GlassWhite,
                focusedContainerColor = GlassWhite, unfocusedContainerColor = GlassWhite,
                focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, cursorColor = TomatoRed
            ),
            shape = RoundedCornerShape(12.dp),
            minLines = 2,
            maxLines = 4
        )

        // Impact
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Impact", color = SubtleWhite, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToggleChip(
                    label = "↑ Élevé",
                    selected = impact == "high",
                    selectedColor = Color(0xFFE53935),
                    onClick = { impact = if (impact == "high") null else "high" }
                )
                ToggleChip(
                    label = "↓ Faible",
                    selected = impact == "low",
                    selectedColor = Color(0xFF6C6C70),
                    onClick = { impact = if (impact == "low") null else "low" }
                )
            }
        }

        // Urgence
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Urgence", color = SubtleWhite, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToggleChip(
                    label = "⚡ Urgent",
                    selected = urgency == "urgent",
                    selectedColor = Color(0xFFFF9800),
                    onClick = { urgency = if (urgency == "urgent") null else "urgent" }
                )
                ToggleChip(
                    label = "✓ Pas urgent",
                    selected = urgency == "not_urgent",
                    selectedColor = Color(0xFF4CAF50),
                    onClick = { urgency = if (urgency == "not_urgent") null else "not_urgent" }
                )
            }
        }

        // Pomodoros planifiés (parity with EditTaskForm)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Pomodoros planifiés", color = SubtleWhite, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(
                    onClick = { plannedPomodoros = (plannedPomodoros - 1).coerceAtLeast(0) },
                    enabled = plannedPomodoros > 0
                ) {
                    Text("−", color = TextWhite, fontSize = 18.sp)
                }
                Text(
                    text = if (plannedPomodoros == 0) "—" else "$plannedPomodoros 🍅",
                    color = TextWhite,
                    fontSize = 14.sp,
                    modifier = Modifier.widthIn(min = 60.dp)
                )
                IconButton(
                    onClick = { plannedPomodoros = (plannedPomodoros + 1).coerceAtMost(6) },
                    enabled = plannedPomodoros < 6
                ) {
                    Text("+", color = TextWhite, fontSize = 18.sp)
                }
            }
        }

        // Story points (parity with EditTaskForm)
        OutlinedTextField(
            value = storyPointsText,
            onValueChange = { newVal ->
                if (newVal.isEmpty() || newVal.all { it.isDigit() }) {
                    storyPointsText = newVal.take(4)
                }
            },
            label = { Text("Story points", color = SubtleWhite) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TomatoRed, unfocusedBorderColor = GlassWhite,
                focusedContainerColor = GlassWhite, unfocusedContainerColor = GlassWhite,
                focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, cursorColor = TomatoRed
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
        )

        // Domaine (parity with EditTaskForm — clickable Surface + DropdownMenu, robust in BottomSheet)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Domaine", color = SubtleWhite, fontSize = 13.sp)
            Box {
                Surface(
                    color = GlassWhite,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { domainMenuExpanded = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedDomain != null) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            runCatching { Color(selectedDomain.color.toColorInt()) }
                                                .getOrDefault(SubtleWhite),
                                            RoundedCornerShape(5.dp)
                                        )
                                )
                            }
                            Text(
                                text = selectedDomain?.name ?: "Aucun domaine",
                                color = TextWhite,
                                fontSize = 14.sp
                            )
                        }
                        Text("▾", color = SubtleWhite, fontSize = 14.sp)
                    }
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = domainMenuExpanded,
                    onDismissRequest = { domainMenuExpanded = false },
                    modifier = Modifier.background(Color(0xFF2C2C2E))
                ) {
                    DropdownMenuItem(
                        text = { Text("Aucun domaine", color = TextWhite) },
                        onClick = { selectedDomainId = null; domainMenuExpanded = false }
                    )
                    domains.forEach { d ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(
                                                runCatching { Color(d.color.toColorInt()) }
                                                    .getOrDefault(SubtleWhite),
                                                RoundedCornerShape(5.dp)
                                            )
                                    )
                                    Text(d.name, color = TextWhite)
                                }
                            },
                            onClick = { selectedDomainId = d.id; domainMenuExpanded = false }
                        )
                    }
                }
            }
        }

        // Date limite (parity with EditTaskForm)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Date limite", color = SubtleWhite, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToggleChip(
                    label = dueDate?.let { formatDueDate(it) } ?: "Aucune",
                    selected = dueDate != null,
                    selectedColor = Color(0xFFE53935),
                    onClick = { showDueDatePicker = true }
                )
                if (dueDate != null) {
                    TextButton(onClick = { dueDate = null }) { Text("×", color = SubtleWhite, fontSize = 16.sp) }
                }
            }
        }

        // Planifier pour
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Planifier pour", color = SubtleWhite, fontSize = 13.sp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToggleChip(
                    label = scheduledDate?.format(frenchDateFormatter)?.replaceFirstChar { it.uppercase() } ?: "Choisir une date",
                    selected = scheduledDate != null,
                    selectedColor = Color(0xFF2196F3),
                    onClick = { showDatePicker = true }
                )
                if (scheduledDate != null) {
                    TextButton(onClick = { scheduledDate = null }) {
                        Text("×", color = SubtleWhite, fontSize = 16.sp)
                    }
                }
            }
        }

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = SubtleWhite)
            }
            Button(
                onClick = {
                    if (name.isNotBlank()) onAdd(
                        name.trim(),
                        impact,
                        urgency,
                        scheduledDate,
                        note.takeIf { it.isNotBlank() },
                        plannedPomodoros,
                        storyPointsText.toIntOrNull()?.coerceIn(0, 9999) ?: 0,
                        selectedDomainId,
                        dueDate
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TomatoRed),
                enabled = name.isNotBlank()
            ) {
                Text("Ajouter", color = Color.White)
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = scheduledDate?.atStartOfDay(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    scheduledDate = datePickerState.selectedDateMillis?.let {
                        java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annuler") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Bug 2 — Date limite picker (parity with EditTaskForm)
    if (showDueDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = datePickerState.selectedDateMillis
                    showDueDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDueDatePicker = false }) { Text("Annuler") } }
        ) { DatePicker(state = datePickerState) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun EditTaskForm(
    task: com.agenticfocus.viewmodel.DayTask,
    currentDate: LocalDate,
    domains: List<DomainEntity> = emptyList(),
    // Story 18-6 — selector "Lier à un projet" (gated FEATURE_PROJECTS via projects.isNotEmpty())
    projects: List<com.agenticfocus.data.entity.ProjectEntity> = emptyList(),
    onProjectChanged: (String?) -> Unit = {},
    loadSubtasks: suspend (String) -> List<SubtaskEntity>,
    onSave: (
        name: String,
        note: String?,
        impact: String?,
        urgency: String?,
        scheduledDate: LocalDate?,
        plannedPomodoros: Int,
        storyPoints: Int,
        domainId: String?,
        dueDate: Long?,
        subtasks: List<SubtaskEntity>,
        originalSubtaskIds: Set<String>
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(task.name) }
    var note by remember { mutableStateOf(task.note ?: "") }
    var impact by remember { mutableStateOf(task.impact) }
    var urgency by remember { mutableStateOf(task.urgency) }
    var scheduledDate by remember { mutableStateOf<LocalDate?>(currentDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    // C1 — fields previously read-only on mobile, now editable to align with desktop
    var plannedPomodoros by remember { mutableStateOf(task.plannedPomodoros) }
    var storyPointsText by remember { mutableStateOf(task.storyPoints.toString()) }
    var selectedDomainId by remember { mutableStateOf(task.domainId) }
    var domainMenuExpanded by remember { mutableStateOf(false) }
    var dueDate by remember { mutableStateOf(task.dueDate) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    var subtasks by remember { mutableStateOf<List<SubtaskEntity>>(emptyList()) }
    var originalSubtaskIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(task.id) {
        val loaded = loadSubtasks(task.id)
        subtasks = loaded
        originalSubtaskIds = loaded.map { it.id }.toSet()
    }

    val selectedDomain = domains.firstOrNull { it.id == selectedDomainId }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Modifier la tâche", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nom *", color = SubtleWhite) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TomatoRed, unfocusedBorderColor = GlassWhite,
                focusedContainerColor = GlassWhite, unfocusedContainerColor = GlassWhite,
                focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, cursorColor = TomatoRed
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Note (optionnel)", color = SubtleWhite) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TomatoRed, unfocusedBorderColor = GlassWhite,
                focusedContainerColor = GlassWhite, unfocusedContainerColor = GlassWhite,
                focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, cursorColor = TomatoRed
            ),
            shape = RoundedCornerShape(12.dp),
            minLines = 2,
            maxLines = 4
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Impact", color = SubtleWhite, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToggleChip("↑ Élevé", impact == "high", Color(0xFFE53935)) { impact = if (impact == "high") null else "high" }
                ToggleChip("↓ Faible", impact == "low", Color(0xFF6C6C70)) { impact = if (impact == "low") null else "low" }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Urgence", color = SubtleWhite, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToggleChip("⚡ Urgent", urgency == "urgent", Color(0xFFFF9800)) { urgency = if (urgency == "urgent") null else "urgent" }
                ToggleChip("✓ Pas urgent", urgency == "not_urgent", Color(0xFF4CAF50)) { urgency = if (urgency == "not_urgent") null else "not_urgent" }
            }
        }

        // C1 — Pomodoros planifiés (editable, parity with desktop)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Pomodoros planifiés", color = SubtleWhite, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(
                    onClick = { plannedPomodoros = (plannedPomodoros - 1).coerceAtLeast(task.completedPomodoros.coerceAtLeast(0)) },
                    enabled = plannedPomodoros > task.completedPomodoros && plannedPomodoros > 0
                ) {
                    Text("−", color = TextWhite, fontSize = 18.sp)
                }
                Text(
                    text = if (plannedPomodoros == 0) "—" else "$plannedPomodoros 🍅",
                    color = TextWhite,
                    fontSize = 14.sp,
                    modifier = Modifier.widthIn(min = 60.dp)
                )
                IconButton(
                    onClick = { plannedPomodoros = (plannedPomodoros + 1).coerceAtMost(6) },
                    enabled = plannedPomodoros < 6
                ) {
                    Text("+", color = TextWhite, fontSize = 18.sp)
                }
            }
        }

        // C1 — Story points (editable number input, parity with desktop)
        OutlinedTextField(
            value = storyPointsText,
            onValueChange = { newVal ->
                if (newVal.isEmpty() || newVal.all { it.isDigit() }) {
                    storyPointsText = newVal.take(4)
                }
            },
            label = { Text("Story points", color = SubtleWhite) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TomatoRed, unfocusedBorderColor = GlassWhite,
                focusedContainerColor = GlassWhite, unfocusedContainerColor = GlassWhite,
                focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, cursorColor = TomatoRed
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
        )

        // C1 — Domain dropdown (editable, parity with desktop)
        // Note: ExposedDropdownMenu has known focus issues inside ModalBottomSheet.
        // Pattern: clickable Surface + standard DropdownMenu — robust in any container.
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Domaine", color = SubtleWhite, fontSize = 13.sp)
            Box {
                Surface(
                    color = GlassWhite,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { domainMenuExpanded = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedDomain != null) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            runCatching { Color(selectedDomain.color.toColorInt()) }
                                                .getOrDefault(SubtleWhite),
                                            RoundedCornerShape(5.dp)
                                        )
                                )
                            }
                            Text(
                                text = selectedDomain?.name ?: "Aucun domaine",
                                color = TextWhite,
                                fontSize = 14.sp
                            )
                        }
                        Text("▾", color = SubtleWhite, fontSize = 14.sp)
                    }
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = domainMenuExpanded,
                    onDismissRequest = { domainMenuExpanded = false },
                    modifier = Modifier.background(Color(0xFF2C2C2E))
                ) {
                    DropdownMenuItem(
                        text = { Text("Aucun domaine", color = TextWhite) },
                        onClick = { selectedDomainId = null; domainMenuExpanded = false }
                    )
                    domains.forEach { d ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(
                                                runCatching { Color(d.color.toColorInt()) }
                                                    .getOrDefault(SubtleWhite),
                                                RoundedCornerShape(5.dp)
                                            )
                                    )
                                    Text(d.name, color = TextWhite)
                                }
                            },
                            onClick = { selectedDomainId = d.id; domainMenuExpanded = false }
                        )
                    }
                }
            }
        }

        // C1 — Date limite (editable date picker, parity with desktop)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Date limite", color = SubtleWhite, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToggleChip(
                    label = dueDate?.let { formatDueDate(it) } ?: "Aucune",
                    selected = dueDate != null,
                    selectedColor = Color(0xFFE53935),
                    onClick = { showDueDatePicker = true }
                )
                if (dueDate != null) {
                    TextButton(onClick = { dueDate = null }) { Text("×", color = SubtleWhite, fontSize = 16.sp) }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Planifier pour", color = SubtleWhite, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToggleChip(
                    label = scheduledDate?.format(frenchDateFormatter)?.replaceFirstChar { it.uppercase() } ?: "Choisir une date",
                    selected = scheduledDate != null,
                    selectedColor = Color(0xFF2196F3),
                    onClick = { showDatePicker = true }
                )
                if (scheduledDate != null) {
                    TextButton(onClick = { scheduledDate = null }) { Text("×", color = SubtleWhite, fontSize = 16.sp) }
                }
            }
        }

        // Mode Projet — Story 18-6. Selector "Lier à un projet" (visible uniquement
        // si projects non vide → gated FEATURE_PROJECTS via list filtering côté caller).
        if (projects.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Lier à un projet", color = SubtleWhite, fontSize = 13.sp)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToggleChip(
                        label = "Aucun",
                        selected = task.projectId == null,
                        selectedColor = Color(0xFF6C6C70),
                        onClick = { onProjectChanged(null) }
                    )
                    projects.filter { !it.isArchived }.forEach { p ->
                        ToggleChip(
                            label = p.name,
                            selected = task.projectId == p.id,
                            selectedColor = Color(0xFFE53935),
                            onClick = { onProjectChanged(p.id) }
                        )
                    }
                }
            }
        }

        SubtaskSection(
            subtasks = subtasks,
            onAdd = { title ->
                subtasks = subtasks + SubtaskEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = SyncEngine.currentUserId,
                    taskId = task.id,
                    title = title,
                    position = subtasks.size
                )
            },
            onDelete = { id -> subtasks = subtasks.filter { it.id != id } },
            onTitleChange = { id, newTitle ->
                subtasks = subtasks.map { if (it.id == id) it.copy(title = newTitle) else it }
            }
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            TextButton(onClick = onDismiss) { Text("Annuler", color = SubtleWhite) }
            Button(
                onClick = {
                    if (name.isNotBlank()) onSave(
                        name.trim(),
                        note.takeIf { it.isNotBlank() },
                        impact,
                        urgency,
                        scheduledDate,
                        plannedPomodoros,
                        storyPointsText.toIntOrNull()?.coerceIn(0, 9999) ?: 0,
                        selectedDomainId,
                        dueDate,
                        subtasks,
                        originalSubtaskIds
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TomatoRed),
                enabled = name.isNotBlank()
            ) { Text("Enregistrer", color = Color.White) }
        }
    }

    if (showDatePicker) {
        val initialMillis = scheduledDate?.atStartOfDay(java.time.ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    scheduledDate = datePickerState.selectedDateMillis?.let {
                        java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annuler") } }
        ) { DatePicker(state = datePickerState) }
    }

    // C1 — Date limite picker (parity with desktop)
    if (showDueDatePicker) {
        val initialMillis = dueDate
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = datePickerState.selectedDateMillis
                    showDueDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDueDatePicker = false }) { Text("Annuler") } }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
private fun SubtaskSection(
    subtasks: List<SubtaskEntity>,
    onAdd: (title: String) -> Unit,
    onDelete: (id: String) -> Unit,
    onTitleChange: (id: String, newTitle: String) -> Unit
) {
    var newSubtaskText by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Sous-tâches", color = SubtleWhite, fontSize = 13.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newSubtaskText,
                onValueChange = { newSubtaskText = it },
                placeholder = { Text("Nouvelle sous-tâche", color = SubtleWhite) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TomatoRed, unfocusedBorderColor = GlassWhite,
                    focusedContainerColor = GlassWhite, unfocusedContainerColor = GlassWhite,
                    focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, cursorColor = TomatoRed
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (newSubtaskText.isNotBlank()) { onAdd(newSubtaskText.trim()); newSubtaskText = "" }
                })
            )
            TextButton(
                onClick = { if (newSubtaskText.isNotBlank()) { onAdd(newSubtaskText.trim()); newSubtaskText = "" } }
            ) { Text("+", fontSize = 20.sp, color = Color.White) }
        }

        subtasks.forEach { subtask ->
            key(subtask.id) {
                var isEditing by remember { mutableStateOf(false) }
                var editText by remember { mutableStateOf(subtask.title) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isEditing) {
                        OutlinedTextField(
                            value = editText,
                            onValueChange = { editText = it },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TomatoRed, unfocusedBorderColor = GlassWhite,
                                focusedContainerColor = GlassWhite, unfocusedContainerColor = GlassWhite,
                                focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, cursorColor = TomatoRed
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (editText.isNotBlank()) onTitleChange(subtask.id, editText.trim())
                                isEditing = false
                            })
                        )
                    } else {
                        Text(
                            text = subtask.title,
                            color = TextWhite,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isEditing = true }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                    TextButton(onClick = { onDelete(subtask.id) }) {
                        Text("×", color = SubtleWhite, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

/**
 * Estimates the wall-clock time when all remaining planned Pomodoros will be done.
 *
 * Rules:
 * - Only tasks with plannedPomodoros > 0 and !isCompleted contribute.
 * - 5-min break between each Pomodoro (not after the last one).
 * - If the sequence crosses 12:00, a 90-min lunch window is added once.
 */
private fun computeEndOfDay(tasks: List<com.agenticfocus.viewmodel.DayTask>): String? {
    val remaining = tasks
        .filter { !it.isCompleted && it.plannedPomodoros > 0 }
        .sumOf { maxOf(0, it.plannedPomodoros - it.completedPomodoros) }
    if (remaining == 0) return null

    val pomoDurationMs = 25 * 60 * 1000L
    val breakDurationMs = 5 * 60 * 1000L
    val lunchDurationMs = 90 * 60 * 1000L

    val todayCal = java.util.Calendar.getInstance()
    val lunchStart = (todayCal.clone() as java.util.Calendar).apply {
        set(java.util.Calendar.HOUR_OF_DAY, 12); set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis

    var cursor = System.currentTimeMillis()
    var lunchAdded = false

    for (i in 0 until remaining) {
        cursor += pomoDurationMs
        if (i < remaining - 1) cursor += breakDurationMs
        if (!lunchAdded && cursor > lunchStart) {
            cursor += lunchDurationMs
            lunchAdded = true
        }
    }

    val endCal = java.util.Calendar.getInstance().apply { timeInMillis = cursor }
    val h = endCal.get(java.util.Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val m = endCal.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
    return "${h}h${m}"
}

@Composable
private fun LibraryPicker(
    domains: List<DomainEntity>,
    templatesByDomain: Map<String, List<TaskTemplateEntity>>,
    onSelect: (TaskTemplateEntity) -> Unit
) {
    var selectedDomainId by remember { mutableStateOf<String?>(null) }
    // D2 — full-text search on template titles (parity with desktop LibraryPicker)
    var searchQuery by remember { mutableStateOf("") }

    val normalizedQuery = searchQuery.trim().lowercase()
    val isSearching = normalizedQuery.isNotEmpty()

    val baseTemplates = if (selectedDomainId == null)
        templatesByDomain.values.flatten()
    else
        templatesByDomain[selectedDomainId] ?: emptyList()

    val visibleTemplates = if (isSearching)
        baseTemplates.filter { it.title.lowercase().contains(normalizedQuery) }
    else baseTemplates

    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = "Ajouter depuis la bibliothèque",
            color = TextWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // D2 — Search bar (full-text filter on template titles)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Rechercher…", color = SubtleWhite) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TomatoRed,
                unfocusedBorderColor = GlassWhite,
                focusedContainerColor = GlassWhite,
                unfocusedContainerColor = GlassWhite,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                cursorColor = TomatoRed
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(36.dp)) {
                        Text("×", color = SubtleWhite, fontSize = 16.sp)
                    }
                }
            }
        )

        Spacer(Modifier.height(4.dp))

        // Domain filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedDomainId == null,
                    onClick = { selectedDomainId = null },
                    label = { Text("Tous", color = TextWhite) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TomatoRed,
                        containerColor = Color.White.copy(alpha = 0.15f)
                    )
                )
            }
            items(domains, key = { it.id }) { domain ->
                val chipColor = remember(domain.color) {
                    runCatching { Color(domain.color.toColorInt()) }.getOrDefault(Color.Gray)
                }
                FilterChip(
                    selected = selectedDomainId == domain.id,
                    onClick = {
                        selectedDomainId = if (selectedDomainId == domain.id) null else domain.id
                    },
                    label = { Text(domain.name, color = TextWhite, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipColor.copy(alpha = 0.8f),
                        containerColor = Color.White.copy(alpha = 0.15f)
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (visibleTemplates.isEmpty()) {
            Text(
                text = "Aucune tâche dans cette catégorie.\nAjoute-en depuis l'onglet Biblio.",
                color = SubtleWhite,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.height(320.dp)
            ) {
                items(visibleTemplates, key = { it.id }) { template ->
                    val domain = domains.find { it.id == template.domainId }
                    val domainColor = remember(domain?.color) {
                        runCatching { Color(domain?.color?.toColorInt() ?: 0xFF888888.toInt()) }
                            .getOrDefault(Color.Gray)
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { onSelect(template) },
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(domainColor, CircleShape)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(template.title, color = TextWhite, fontSize = 14.sp)
                                if (domain != null) {
                                    Text(domain.name, color = SubtleWhite, fontSize = 12.sp)
                                }
                            }
                            Text(
                                "${template.storyPoints}pts",
                                color = SubtleWhite,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "🍅×${template.defaultPomodoros}",
                                color = SubtleWhite,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BacklogSheet(
    tasks: List<com.agenticfocus.viewmodel.DayTask>,
    selectedDate: LocalDate,
    onAdd: (String) -> Unit,
    onScheduleToday: (com.agenticfocus.viewmodel.DayTask) -> Unit,
    onScheduleDate: (com.agenticfocus.viewmodel.DayTask, LocalDate) -> Unit,
    onDelete: (com.agenticfocus.viewmodel.DayTask) -> Unit
) {
    var schedulingTask by remember { mutableStateOf<com.agenticfocus.viewmodel.DayTask?>(null) }
    var newBacklogTitle by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Backlog (${tasks.size})",
            color = TextWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // C2 — Add new backlog task input (parity with desktop)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newBacklogTitle,
                onValueChange = { newBacklogTitle = it },
                placeholder = { Text("Nouvelle idée à reporter…", color = SubtleWhite) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TomatoRed, unfocusedBorderColor = GlassWhite,
                    focusedContainerColor = GlassWhite, unfocusedContainerColor = GlassWhite,
                    focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, cursorColor = TomatoRed
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val trimmed = newBacklogTitle.trim()
                    if (trimmed.isNotEmpty()) {
                        onAdd(trimmed)
                        newBacklogTitle = ""
                    }
                    focusManager.clearFocus()
                })
            )
            Button(
                onClick = {
                    val trimmed = newBacklogTitle.trim()
                    if (trimmed.isNotEmpty()) {
                        onAdd(trimmed)
                        newBacklogTitle = ""
                        focusManager.clearFocus()
                    }
                },
                enabled = newBacklogTitle.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TomatoRed,
                    disabledContainerColor = TomatoRed.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text("Ajouter", color = Color.White, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (tasks.isEmpty()) {
            Text(
                text = "Aucune tâche en attente.",
                color = SubtleWhite,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = task.name,
                                color = TextWhite,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { onScheduleToday(task) }) {
                                Text("Aujourd'hui", color = TomatoRed, fontSize = 12.sp)
                            }
                            TextButton(onClick = { schedulingTask = task }) {
                                Text("📅", fontSize = 14.sp)
                            }
                            IconButton(
                                onClick = { onDelete(task) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("×", color = SubtleWhite, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    schedulingTask?.let { task ->
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { schedulingTask = null },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        val picked = java.time.Instant.ofEpochMilli(ms)
                            .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                        onScheduleDate(task, picked)
                    }
                    schedulingTask = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { schedulingTask = null }) { Text("Annuler") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}
