package com.agenticfocus.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agenticfocus.R
import com.agenticfocus.ui.components.DayTaskRow
import com.agenticfocus.ui.theme.GlassWhite
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.ui.theme.TomatoRed
import com.agenticfocus.viewmodel.DayPlannerViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun DayPlannerScreen(
    viewModel: DayPlannerViewModel,
    contentPadding: PaddingValues = PaddingValues()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.reorderTasks(from.index, to.index)
    }

    var newTaskText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val totalPlanned = state.tasks.sumOf { it.plannedPomodoros }
    val isOverCapacity = totalPlanned > DayPlannerViewModel.DAILY_CAPACITY

    Box(modifier = Modifier.fillMaxSize()) {
        // Background — same as PomodoroScreen
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

        // Content column — respects bottom nav bar via contentPadding
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = contentPadding.calculateBottomPadding())
        ) {
            // Capacity indicator header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Day Planner", color = TextWhite, fontSize = 18.sp)
                Text(
                    text = "$totalPlanned / ${DayPlannerViewModel.DAILY_CAPACITY} 🍅",
                    color = if (isOverCapacity) TomatoRed else SubtleWhite,
                    fontSize = 14.sp
                )
            }

            // Reorderable task list
            // SwipeToDismissBox is OUTSIDE ReorderableItem to avoid gesture conflicts
            // (horizontal swipe vs vertical drag are orthogonal — no runtime conflict)
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(state.tasks, key = { _, task -> task.id }) { _, task ->
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
                            // Only show red background when actively swiping
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
                                    onNameChange = { viewModel.updateName(task.id, it) }
                                )
                            }
                            } // Box
                        }
                    }
                }
            }

            // Add task input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTaskText,
                    onValueChange = { newTaskText = it },
                    placeholder = { Text("Nouvelle tâche...", color = SubtleWhite) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TomatoRed,
                        unfocusedBorderColor = GlassWhite,
                        focusedContainerColor = GlassWhite,
                        unfocusedContainerColor = GlassWhite,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = TomatoRed
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
                Button(
                    onClick = {
                        viewModel.addTask(newTaskText)
                        newTaskText = ""
                        focusManager.clearFocus()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TomatoRed)
                ) {
                    Text("+", color = Color.White, fontSize = 20.sp)
                }
            }
        }
    }
}
