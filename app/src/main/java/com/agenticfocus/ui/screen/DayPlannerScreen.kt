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
import com.agenticfocus.ui.components.ToggleChip
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
    var routineTypeMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.Dispatchers.IO.let { io ->
            kotlinx.coroutines.withContext(io) {
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
    val isOverCapacity = totalPlanned > DayPlannerViewModel.DAILY_CAPACITY

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
                .padding(bottom = contentPadding.calculateBottomPadding())
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
                    text = "$totalPlanned / ${DayPlannerViewModel.DAILY_CAPACITY} 🍅",
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

            // Input bar: TextField full-width, then 3 buttons below
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = newTaskText,
                    onValueChange = { newTaskText = it },
                    placeholder = { Text("Nouvelle tâche...", color = SubtleWhite) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.60f),
                        focusedContainerColor = Color.Black.copy(alpha = 0.50f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.50f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        viewModel.addTask(newTaskText)
                        newTaskText = ""
                        focusManager.clearFocus()
                    })
                )
                Spacer(Modifier.height(8.dp))
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
                loadSubtasks = { taskId -> viewModel.getSubtasksForTask(taskId) },
                onSave = { name, note, impact, urgency, scheduledDate, subtasks, originalIds ->
                    viewModel.updateTaskDetails(task.id, name, note, impact, urgency, scheduledDate, subtasks, originalIds)
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
                onAdd = { name, impact, urgency, scheduledDate ->
                    viewModel.addTask(name, impact, urgency, scheduledDate)
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
private fun AddTaskForm(
    initialName: String,
    initialDate: LocalDate,
    onAdd: (name: String, impact: String?, urgency: String?, scheduledDate: LocalDate?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var impact by remember { mutableStateOf<String?>(null) }
    var urgency by remember { mutableStateOf<String?>(null) }
    var scheduledDate by remember { mutableStateOf<LocalDate?>(initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }

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

        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
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
                onClick = { if (name.isNotBlank()) onAdd(name.trim(), impact, urgency, scheduledDate) },
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTaskForm(
    task: com.agenticfocus.viewmodel.DayTask,
    currentDate: LocalDate,
    domains: List<DomainEntity> = emptyList(),
    loadSubtasks: suspend (String) -> List<SubtaskEntity>,
    onSave: (name: String, note: String?, impact: String?, urgency: String?, scheduledDate: LocalDate?, subtasks: List<SubtaskEntity>, originalSubtaskIds: Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(task.name) }
    var note by remember { mutableStateOf(task.note ?: "") }
    var impact by remember { mutableStateOf(task.impact) }
    var urgency by remember { mutableStateOf(task.urgency) }
    var scheduledDate by remember { mutableStateOf<LocalDate?>(currentDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    var subtasks by remember { mutableStateOf<List<SubtaskEntity>>(emptyList()) }
    var originalSubtaskIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(task.id) {
        val loaded = loadSubtasks(task.id)
        subtasks = loaded
        originalSubtaskIds = loaded.map { it.id }.toSet()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Modifier la tâche", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

        // Domain + story points — read-only card
        val templateDomain = domains.firstOrNull { it.id == task.domainId }
        val templateDomainColor = templateDomain?.let {
            runCatching { Color(it.color.toColorInt()) }.getOrDefault(SubtleWhite)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassWhite, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Domain
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Domaine", color = SubtleWhite, fontSize = 11.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (templateDomainColor != null) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(templateDomainColor, RoundedCornerShape(5.dp))
                        )
                    }
                    Text(
                        text = templateDomain?.name ?: "—",
                        color = TextWhite,
                        fontSize = 14.sp
                    )
                }
            }
            // Story points
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text("Points de valeur", color = SubtleWhite, fontSize = 11.sp)
                Text(
                    text = if (task.storyPoints > 0) "${task.storyPoints} pts" else "—",
                    color = TextWhite,
                    fontSize = 14.sp
                )
            }
        }

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
                onClick = { if (name.isNotBlank()) onSave(name.trim(), note.takeIf { it.isNotBlank() }, impact, urgency, scheduledDate, subtasks, originalSubtaskIds) },
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

    val visibleTemplates = if (selectedDomainId == null)
        templatesByDomain.values.flatten()
    else
        templatesByDomain[selectedDomainId] ?: emptyList()

    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = "Ajouter depuis la bibliothèque",
            color = TextWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

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
    onScheduleToday: (com.agenticfocus.viewmodel.DayTask) -> Unit,
    onScheduleDate: (com.agenticfocus.viewmodel.DayTask, LocalDate) -> Unit,
    onDelete: (com.agenticfocus.viewmodel.DayTask) -> Unit
) {
    var schedulingTask by remember { mutableStateOf<com.agenticfocus.viewmodel.DayTask?>(null) }

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

        if (tasks.isEmpty()) {
            Text(
                text = "Aucune tâche en attente.\nUtilise 📋 → Backlog pour ajouter des idées sans date.",
                color = SubtleWhite,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
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
