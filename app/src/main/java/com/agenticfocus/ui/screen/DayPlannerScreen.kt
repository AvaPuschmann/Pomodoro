package com.agenticfocus.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agenticfocus.R
import com.agenticfocus.data.entity.DomainEntity
import com.agenticfocus.data.entity.TaskTemplateEntity
import com.agenticfocus.ui.components.DayTaskRow
import com.agenticfocus.ui.theme.GlassWhite
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.ui.theme.TomatoRed
import com.agenticfocus.viewmodel.DayPlannerViewModel
import com.agenticfocus.viewmodel.LibraryViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.material3.Icon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPlannerScreen(
    viewModel: DayPlannerViewModel,
    libraryViewModel: LibraryViewModel,
    contentPadding: PaddingValues = PaddingValues()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val libraryState by libraryViewModel.state.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.reorderTasks(from.index, to.index)
    }

    var newTaskText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val totalPlanned = state.tasks.sumOf { it.plannedPomodoros }
    val isOverCapacity = totalPlanned > DayPlannerViewModel.DAILY_CAPACITY

    var showLibrarySheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
        ) {
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
                            }
                        }
                    }
                }
            }

            // Input bar: TextField + 📚 bouton + "+" bouton
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
                // Bibliothèque button
                Button(
                    onClick = { showLibrarySheet = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Bibliothèque",
                        tint = TextWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
                // Quick add button
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
