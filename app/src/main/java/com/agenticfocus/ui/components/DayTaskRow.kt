package com.agenticfocus.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agenticfocus.ui.theme.GlassWhite
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.ui.theme.TomatoRed
import com.agenticfocus.viewmodel.DayTask

// Local constants — same values as TomatoPlanner.kt, kept private per file
private val TomatoGreen = Color(0xFF4CAF50)

@Composable
fun DayTaskRow(
    task: DayTask,
    isActive: Boolean,
    dragHandleModifier: Modifier,   // Modifier.draggableHandle() from ReorderableItem scope
    onPlay: () -> Unit,
    onIncreasePlanned: () -> Unit,
    onDecreasePlanned: () -> Unit,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(task.name) { mutableStateOf(task.name) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isEditing) {
        if (isEditing) focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Play button — TomatoRed when active, SubtleWhite otherwise
            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Activer la tâche",
                    tint = if (isActive) TomatoRed else SubtleWhite
                )
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
                Text(
                    text = task.name,
                    color = TextWhite,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isEditing = true }
                )
            }

            // − button
            IconButton(onClick = onDecreasePlanned, modifier = Modifier.size(36.dp)) {
                Text("−", fontSize = 18.sp, color = TextWhite)
            }

            // Planned count label
            Text(
                text = "${task.plannedPomodoros}",
                color = SubtleWhite,
                fontSize = 12.sp
            )

            // + button
            IconButton(onClick = onIncreasePlanned, modifier = Modifier.size(36.dp)) {
                Text("+", fontSize = 18.sp, color = TextWhite)
            }

            // Drag handle — DragHandle icon (reorder affordance, not navigation drawer)
            // Icons.Default.DragHandle is in material-icons-core (BOM 2024.09.00+)
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Réordonner",
                tint = SubtleWhite,
                modifier = dragHandleModifier.size(24.dp)
            )
        }

        // Tomato icons row — compact 20dp, no Orange state in Planner
        val totalSlots = maxOf(task.plannedPomodoros, task.completedPomodoros)
        if (totalSlots > 0) {
            Row(
                modifier = Modifier.padding(start = 48.dp, top = 2.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(totalSlots) { i ->
                    val color = if (i < task.completedPomodoros) TomatoRed else TomatoGreen
                    TomatoIcon(color = color, size = 20.dp)
                }
            }
        }
    }
}
