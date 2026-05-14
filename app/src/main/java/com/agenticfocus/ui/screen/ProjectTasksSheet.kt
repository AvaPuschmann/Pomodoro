package com.agenticfocus.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agenticfocus.data.entity.DayTaskEntity
import com.agenticfocus.data.entity.ProjectEntity
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.ui.theme.TomatoRed
import com.agenticfocus.viewmodel.DayPlannerViewModel
import com.agenticfocus.viewmodel.DayTask
import com.agenticfocus.viewmodel.ProjectViewModel

/**
 * Story 22-7 / Sprint 20 — ProjectTasksSheet.
 * ModalBottomSheet expansible depuis ProjectCard (tap icône dédiée).
 *
 * Pattern UX duo mobile :
 * - Tap icône sur card → cette sheet (accès rapide tasks, toggle/stepper/▶ Pomodoro)
 * - Long-press card → menu existant (Move/Modifier vers ProjectDetailScreen complet)
 *
 * Parité fonctionnelle desktop ProjectTasksDropdown.tsx (Sprint 19).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectTasksSheet(
    project: ProjectEntity,
    projectVM: ProjectViewModel,
    dayPlannerVM: DayPlannerViewModel,
    onAddTask: () -> Unit,
    onEditTask: (DayTaskEntity) -> Unit,
    onDismiss: () -> Unit,
    onNavigateToTimer: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tasks by projectVM
        .observeTasksForProject(project.id)
        .collectAsState(initial = emptyList())

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = project.name,
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${tasks.size} tâche${if (tasks.size > 1) "s" else ""}",
                    color = SubtleWhite,
                    fontSize = 12.sp,
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

            if (tasks.isEmpty()) {
                Text(
                    text = "Aucune tâche dans ce projet pour l'instant.",
                    color = SubtleWhite,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(items = tasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onToggle = { projectVM.toggleTaskCompletion(task.id) },
                            onIncrement = { projectVM.updateTaskPlannedPomodoros(task.id, +1) },
                            onDecrement = { projectVM.updateTaskPlannedPomodoros(task.id, -1) },
                            onActivate = {
                                // Reuse DayPlannerViewModel.activateTask which handles
                                // TimerService intent + nav event.
                                dayPlannerVM.activateTask(
                                    DayTask(
                                        id = task.id,
                                        name = task.name,
                                        plannedPomodoros = task.plannedPomodoros,
                                        completedPomodoros = task.completedPomodoros,
                                        isCompleted = task.isCompleted,
                                    )
                                )
                                onNavigateToTimer()
                                onDismiss()
                            },
                            onEdit = { onEditTask(task) },
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

            // Actions row : + Ajouter une tâche (primary) + 📚 Bibliothèque (secondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { onAddTask() },
                    colors = ButtonDefaults.buttonColors(containerColor = TomatoRed),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Ajouter")
                }
                // V1 mobile : la sélection depuis Library est gérée via AddTaskForm classique
                // (depuis Day Planner ou ProjectDetailScreen). On garde ce bouton réservé future
                // implémentation LibraryPicker spécifique ProjectTasksSheet — pour V1, on redirige
                // simplement vers AddTaskForm comme le bouton principal.
            }

            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun TaskRow(
    task: DayTaskEntity,
    onToggle: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
        Text(
            text = task.name,
            color = TextWhite,
            fontSize = 14.sp,
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f).padding(end = 4.dp),
        )
        // Stepper - / count / + sur planned_pomodoros
        Box(modifier = Modifier.padding(horizontal = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "−1 pomo", tint = SubtleWhite, modifier = Modifier.size(14.dp))
                }
                Text(
                    text = "${task.plannedPomodoros}",
                    color = TextWhite,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
                IconButton(onClick = onIncrement, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "+1 pomo", tint = SubtleWhite, modifier = Modifier.size(14.dp))
                }
            }
        }
        IconButton(onClick = onActivate, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Démarrer Pomodoro", tint = TomatoRed, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Éditer", tint = SubtleWhite, modifier = Modifier.size(16.dp))
        }
    }
}
