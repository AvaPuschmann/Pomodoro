package com.agenticfocus.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agenticfocus.data.entity.DayTaskEntity
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.ui.theme.TomatoRed
import com.agenticfocus.viewmodel.LibraryViewModel
import com.agenticfocus.viewmodel.ProjectViewModel
import com.agenticfocus.viewmodel.SortOrder
import kotlinx.coroutines.flow.emptyFlow
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Locale

/**
 * Mode Projet — Story 18-4 / Sprint 18.
 * Detail projet : header editable inline + tasks list avec badges date + sort per-projet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    projectVM: ProjectViewModel,
    libraryVM: LibraryViewModel,
    onClose: () -> Unit,
    onEditProject: () -> Unit,
    onAddTask: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by projectVM.state.collectAsStateWithLifecycle()
    val project = state.projects.find { it.id == projectId }
        ?: return EmptyProject(onClose, contentPadding)

    val tasks by projectVM
        .observeTasksForProject(projectId)
        .collectAsState(initial = emptyList())

    val stats = state.statsByProject[projectId]
    val progressPct = stats?.let {
        if (it.plannedSum > 0) (it.completedSum * 100 / it.plannedSum) else 0
    } ?: 0

    // Local state pour inline edit (blur-save)
    var nameLocal by remember(project.id) { mutableStateOf(project.name) }
    var descLocal by remember(project.id) { mutableStateOf(project.description ?: "") }

    // Story 18-4 — Sort persistance per-projet [F8/D53]
    var sortMode by rememberSaveable(projectId) {
        mutableStateOf(projectVM.getSortOrderForProject(projectId))
    }
    LaunchedEffect(sortMode) {
        projectVM.setSortOrderForProject(projectId, sortMode)
    }
    val sortedTasks = remember(tasks, sortMode) {
        when (sortMode) {
            SortOrder.NAME_ASC -> tasks.sortedBy { it.name.lowercase() }
            SortOrder.CREATED_DESC -> tasks.sortedByDescending { it.createdAt }
            SortOrder.TARGET_DATE_ASC -> tasks.sortedBy { it.date ?: "9999-99-99" }
            SortOrder.UPDATED_DESC -> tasks.sortedByDescending { it.updatedAt }
        }
    }

    var sortMenuOpen by remember { mutableStateOf(false) }
    val todayStr = remember { todayDateString() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        // TopAppBar custom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = TextWhite)
            }
            Text(
                text = "Détail projet",
                color = TextWhite,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onEditProject) {
                Text("Modifier", color = TomatoRed)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header section
            item {
                OutlinedTextField(
                    value = nameLocal,
                    onValueChange = { nameLocal = it },
                    label = { Text("Nom") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused && nameLocal != project.name && nameLocal.isNotBlank()) {
                                projectVM.updateProject(project.id, name = nameLocal)
                            }
                        },
                    colors = formColors(),
                )
                Spacer(modifier = Modifier.size(8.dp))
                OutlinedTextField(
                    value = descLocal,
                    onValueChange = { descLocal = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused && descLocal != (project.description ?: "")) {
                                projectVM.updateProject(project.id, description = descLocal.ifBlank { "" })
                            }
                        },
                    colors = formColors(),
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = "Progression : $progressPct% (${stats?.completedSum ?: 0}/${stats?.plannedSum ?: 0} 🍅)",
                    color = SubtleWhite,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.size(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.size(12.dp))
            }

            // Tasks header + sort menu
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tâches (${tasks.size})",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        TextButton(onClick = { sortMenuOpen = true }) {
                            Text("Trier : ${labelForSort(sortMode)}", color = SubtleWhite, fontSize = 12.sp)
                        }
                        DropdownMenu(
                            expanded = sortMenuOpen,
                            onDismissRequest = { sortMenuOpen = false }
                        ) {
                            SortOrder.entries.forEach { so ->
                                DropdownMenuItem(
                                    text = { Text(labelForSort(so)) },
                                    onClick = { sortMode = so; sortMenuOpen = false }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.size(8.dp))
            }

            // Tasks list
            if (sortedTasks.isEmpty()) {
                item {
                    Text(
                        text = "Aucune tâche dans ce projet pour l'instant",
                        color = SubtleWhite,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(items = sortedTasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        todayStr = todayStr,
                        onPlanForToday = { projectVM.planTaskForToday(task.id) },
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                }
            }

            // Add task button
            item {
                Spacer(modifier = Modifier.size(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = TomatoRed,
                    onClick = { onAddTask(projectId) },
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Ajouter une tâche", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: DayTaskEntity,
    todayStr: String,
    onPlanForToday: () -> Unit,
) {
    val (badge, badgeColor) = when {
        task.isCompleted -> "✓ Faite" to Color(0xFF4CAF50)
        task.date == null -> "🗓 Backlog" to Color(0xFF6C6C70)
        task.date == todayStr -> "📅 Aujourd'hui" to Color(0xFF2196F3)
        else -> "📅 ${formatShortDate(task.date)}" to Color(0xFF6C6C70)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = task.isCompleted, onCheckedChange = null)
        Spacer(modifier = Modifier.size(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.name,
                color = TextWhite,
                fontSize = 14.sp,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = badgeColor.copy(alpha = 0.18f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = badge,
                        color = badgeColor,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "🍅 ${task.completedPomodoros}/${task.plannedPomodoros}",
                    color = SubtleWhite,
                    fontSize = 11.sp,
                )
            }
        }
        TextButton(onClick = onPlanForToday) {
            Text(
                text = if (task.date == todayStr) "✓ Auj." else "+ Auj.",
                color = TomatoRed,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun EmptyProject(onClose: () -> Unit, contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Projet introuvable", color = TextWhite, fontSize = 16.sp)
        Spacer(modifier = Modifier.size(16.dp))
        TextButton(onClick = onClose) { Text("Retour") }
    }
}

private fun labelForSort(s: SortOrder): String = when (s) {
    SortOrder.UPDATED_DESC -> "Modifiée récemment"
    SortOrder.CREATED_DESC -> "Créée récemment"
    SortOrder.NAME_ASC -> "Nom"
    SortOrder.TARGET_DATE_ASC -> "Date"
}

private fun todayDateString(): String {
    val now = LocalDate.now()
    return "%04d-%02d-%02d".format(now.year, now.monthValue, now.dayOfMonth)
}

private fun formatShortDate(date: String): String {
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.FRENCH).parse(date)
        SimpleDateFormat("EEE d MMM", Locale.FRENCH).format(parsed!!)
    } catch (_: Exception) {
        date
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun formColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TomatoRed,
    focusedContainerColor = Color.White.copy(alpha = 0.08f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite,
    cursorColor = TomatoRed,
    focusedLabelColor = TomatoRed,
    unfocusedLabelColor = SubtleWhite,
)
