package com.agenticfocus.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agenticfocus.data.entity.SubtaskEntity
import com.agenticfocus.ui.theme.GlassWhite
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.ui.theme.TomatoRed
import com.agenticfocus.viewmodel.DayTask

// Local constants — same values as TomatoPlanner.kt, kept private per file
private val TomatoGreen  = Color(0xFF4CAF50)
private val TomatoOrange = Color(0xFFFF9800)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayTaskRow(
    task: DayTask,
    isActive: Boolean,
    dragHandleModifier: Modifier,   // Modifier.draggableHandle() from ReorderableItem scope
    onPlay: () -> Unit,
    onIncreasePlanned: () -> Unit,
    onDecreasePlanned: () -> Unit,
    onNameChange: (String) -> Unit,
    onEdit: (() -> Unit)? = null,
    onToggleComplete: (() -> Unit)? = null,
    subtasks: List<SubtaskEntity> = emptyList(),
    onToggleSubtask: ((subtaskId: String) -> Unit)? = null,
    domainColor: Color? = null,
    routineType: String? = null,
    modifier: Modifier = Modifier,
    showDragHandle: Boolean = true
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(task.name) { mutableStateOf(task.name) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isEditing) {
        if (isEditing) focusRequester.requestFocus()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Domain color bar — left edge, full height
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .background(domainColor ?: Color.Transparent)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
            // Checkbox — always visible, marks task done/undone without timer
            if (onToggleComplete != null) {
                IconButton(onClick = onToggleComplete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = if (task.isCompleted) "Marquer non terminée" else "Marquer terminée",
                        tint = if (task.isCompleted) TomatoGreen else SubtleWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Play button — hidden for 0-pomodoro tasks (no focus session needed)
            if (task.plannedPomodoros > 0) {
                IconButton(onClick = onPlay) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Activer la tâche",
                        tint = if (isActive) TomatoRed else SubtleWhite
                    )
                }
            }

            // Task name or inline editor
            if (isEditing) {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TomatoRed,
                        unfocusedBorderColor = GlassWhite,
                        focusedContainerColor = GlassWhite,
                        unfocusedContainerColor = GlassWhite,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = TomatoRed
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        onNameChange(editText)
                        isEditing = false
                        focusManager.clearFocus()
                    })
                )
            } else {
                if (routineType != null) {
                    Text(
                        text = if (routineType == "morning") "☀️" else "🌙",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                Text(
                    text = task.name,
                    color = TextWhite,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = { isEditing = true },
                            onLongClick = { onEdit?.invoke() }
                        )
                )
            }

            // − button — disabled at 0
            IconButton(
                onClick = onDecreasePlanned,
                modifier = Modifier.size(36.dp),
                enabled = task.plannedPomodoros > 0
            ) {
                Text(
                    "−",
                    fontSize = 18.sp,
                    color = if (task.plannedPomodoros > 0) TextWhite else SubtleWhite.copy(alpha = 0.3f)
                )
            }

            // Planned count label — show "—" for 0
            Text(
                text = if (task.plannedPomodoros == 0) "—" else "${task.plannedPomodoros}",
                color = SubtleWhite,
                fontSize = 12.sp
            )

            // + button
            IconButton(onClick = onIncreasePlanned, modifier = Modifier.size(36.dp)) {
                Text("+", fontSize = 18.sp, color = TextWhite)
            }

            // Drag handle — hidden for done tasks
            if (showDragHandle) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Réordonner",
                    tint = SubtleWhite,
                    modifier = dragHandleModifier.size(24.dp)
                )
            }
        }

        // Tomato icons row — compact 20dp, no Orange state in Planner
        val totalSlots = maxOf(task.plannedPomodoros, task.completedPomodoros)
        if (totalSlots > 0) {
            Row(
                modifier = Modifier.padding(start = 48.dp, top = 2.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(totalSlots) { i ->
                    val color = when {
                        i < task.completedPomodoros  -> TomatoGreen   // done
                        i == task.completedPomodoros && isActive -> TomatoOrange  // in progress
                        else                         -> TomatoRed     // not started
                    }
                    TomatoIcon(color = color, size = 20.dp)
                }
            }
        }

        // Subtasks — collapsible list
        if (subtasks.isNotEmpty()) {
            val completedCount = subtasks.count { it.isCompleted }
            var expanded by remember { mutableStateOf(false) }

            // Collapse toggle row
            Row(
                modifier = Modifier
                    .padding(start = 44.dp, top = 2.dp)
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Réduire" else "Développer",
                    tint = SubtleWhite,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "$completedCount/${subtasks.size} sous-tâches",
                    color = SubtleWhite,
                    fontSize = 11.sp
                )
            }

            if (expanded) {
                val visible = subtasks.take(5)
                val overflow = subtasks.size - 5
                Column(
                    modifier = Modifier.padding(start = 44.dp, top = 2.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    visible.forEach { subtask ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = { onToggleSubtask?.invoke(subtask.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (subtask.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (subtask.isCompleted) TomatoGreen else SubtleWhite,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = subtask.title,
                                color = if (subtask.isCompleted) SubtleWhite else TextWhite,
                                fontSize = 12.sp,
                                textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (overflow > 0) {
                        Text(
                            text = "et $overflow de plus...",
                            color = SubtleWhite,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 30.dp, bottom = 2.dp)
                        )
                    }
                }
            }
        }
        } // end inner Column
    } // end outer Row
}
