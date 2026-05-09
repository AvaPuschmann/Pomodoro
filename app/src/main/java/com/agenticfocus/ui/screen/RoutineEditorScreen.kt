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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agenticfocus.R
import com.agenticfocus.data.entity.RoutineItemEntity
import com.agenticfocus.ui.theme.GlassWhite
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.ui.theme.TomatoRed
import com.agenticfocus.viewmodel.LibraryViewModel
import com.agenticfocus.viewmodel.RoutineViewModel
import kotlinx.coroutines.launch

// Local color constant — TomatoGreen is private in DayTaskRow.kt
private val SwitchOnGreen = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorScreen(
    routineId: String,
    routineViewModel: RoutineViewModel,
    libraryViewModel: LibraryViewModel,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    val state by routineViewModel.state.collectAsStateWithLifecycle()
    val routine = when (routineId) {
        state.morning?.id -> state.morning
        state.evening?.id -> state.evening
        else -> null
    }
    val items = state.itemsByRoutine[routineId] ?: emptyList()

    val isMorning = routine?.type == "morning"
    val emoji = if (isMorning) "☀️" else "🌙"
    val fallbackName = if (isMorning) "Routine matinale" else "Routine du soir"

    // F9 — Local edit buffers, persisted on appropriate triggers (blur/picker/toggle)
    var nameLocal by remember(routine?.id, routine?.name) {
        mutableStateOf(routine?.name?.takeIf { it.isNotBlank() } ?: fallbackName)
    }
    var triggerTimeLocal by remember(routine?.id, routine?.triggerTime) {
        mutableStateOf(routine?.triggerTime ?: "06:00")
    }

    var showTimePicker by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    val templatePickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

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
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = TextWhite)
                }
                Text(
                    text = "$emoji ${if (routine != null) nameLocal else "Chargement…"}",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (routine == null) {
                Text(
                    text = "Routine introuvable.",
                    color = SubtleWhite,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
                return@Column
            }

            // ── Configuration ────────────────────────────────────────────────
            Text("CONFIGURATION", color = SubtleWhite, fontSize = 11.sp, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))

            OutlinedTextField(
                value = nameLocal,
                onValueChange = { nameLocal = it },
                label = { Text("Nom", color = SubtleWhite) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .onFocusChanged { focus ->
                        // F9 — Save on blur, with guard to avoid the initial composition save
                        if (!focus.isFocused && nameLocal != (routine.name.ifBlank { fallbackName })) {
                            routineViewModel.updateRoutine(
                                id = routine.id,
                                name = nameLocal,
                                triggerTime = triggerTimeLocal,
                                isActive = routine.isActive == 1
                            )
                        }
                    },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TomatoRed, unfocusedBorderColor = GlassWhite,
                    focusedContainerColor = GlassWhite, unfocusedContainerColor = GlassWhite,
                    focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, cursorColor = TomatoRed
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { showTimePicker = true },
                color = GlassWhite,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Heure de déclenchement", color = SubtleWhite, fontSize = 13.sp)
                    Text(triggerTimeLocal, color = TextWhite, fontSize = 14.sp)
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                color = GlassWhite,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Active", color = TextWhite, fontSize = 14.sp)
                    Switch(
                        checked = routine.isActive == 1,
                        onCheckedChange = { newActive ->
                            // F7 — User-triggered, immediate save
                            routineViewModel.updateRoutine(
                                id = routine.id,
                                name = nameLocal,
                                triggerTime = triggerTimeLocal,
                                isActive = newActive
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextWhite,
                            checkedTrackColor = SwitchOnGreen,
                            uncheckedThumbColor = SubtleWhite,
                            uncheckedTrackColor = SubtleWhite.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            // ── Items ────────────────────────────────────────────────────────
            Text(
                text = "TÂCHES (${items.size})",
                color = SubtleWhite,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    RoutineItemRow(
                        item = item,
                        onDelete = { routineViewModel.removeItem(item) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    showTemplatePicker = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TomatoRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("+ Ajouter depuis la bibliothèque", color = Color.White, fontSize = 14.sp)
            }
        }

        // Time picker dialog
        if (showTimePicker) {
            val (h, m) = parseTriggerTime(triggerTimeLocal)
            val timePickerState = rememberTimePickerState(initialHour = h, initialMinute = m, is24Hour = true)
            Dialog(
                onDismissRequest = { showTimePicker = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    color = Color(0xFF1C1C1E),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Heure de déclenchement", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
                        TimePicker(state = timePickerState)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                        ) {
                            TextButton(onClick = { showTimePicker = false }) { Text("Annuler", color = SubtleWhite) }
                            TextButton(onClick = {
                                val newTime = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
                                triggerTimeLocal = newTime
                                routineViewModel.updateRoutine(
                                    id = routineId,
                                    name = nameLocal,
                                    triggerTime = newTime,
                                    isActive = routine?.isActive == 1
                                )
                                showTimePicker = false
                            }) { Text("OK", color = TomatoRed) }
                        }
                    }
                }
            }
        }

        // Template picker sheet
        if (showTemplatePicker) {
            ModalBottomSheet(
                onDismissRequest = { showTemplatePicker = false },
                sheetState = templatePickerSheetState,
                containerColor = Color(0xFF1A1A1A)
            ) {
                val libraryState by libraryViewModel.state.collectAsStateWithLifecycle()
                RoutineTemplatePickerSheet(
                    domains = libraryState.domains,
                    templatesByDomain = libraryState.templatesByDomain,
                    onSelect = { template ->
                        routineViewModel.addItemFromTemplate(routineId, template)
                        scope.launch { templatePickerSheetState.hide() }.invokeOnCompletion {
                            showTemplatePicker = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RoutineItemRow(
    item: RoutineItemEntity,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = item.snapshotTitle, color = TextWhite, fontSize = 14.sp)
                if (item.snapshotStoryPoints > 0) {
                    Text(text = "${item.snapshotStoryPoints} pts", color = SubtleWhite, fontSize = 11.sp)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.padding(start = 8.dp)) {
                Text("×", color = SubtleWhite, fontSize = 18.sp)
            }
        }
    }
}

private fun parseTriggerTime(s: String): Pair<Int, Int> {
    val parts = s.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 6
    val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return h to m
}

/**
 * Template picker for routine items — re-uses LibraryPicker pattern with D2 search.
 * Inline composable to keep RoutineEditorScreen self-contained.
 */
@Composable
private fun RoutineTemplatePickerSheet(
    domains: List<com.agenticfocus.data.entity.DomainEntity>,
    templatesByDomain: Map<String, List<com.agenticfocus.data.entity.TaskTemplateEntity>>,
    onSelect: (com.agenticfocus.data.entity.TaskTemplateEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val normalizedQuery = searchQuery.trim().lowercase()
    val isSearching = normalizedQuery.isNotEmpty()

    val allTemplates = templatesByDomain.values.flatten()
    val visibleTemplates = if (isSearching)
        allTemplates.filter { it.title.lowercase().contains(normalizedQuery) }
    else allTemplates

    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = "Ajouter une tâche depuis la bibliothèque",
            color = TextWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Rechercher…", color = SubtleWhite) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TomatoRed, unfocusedBorderColor = GlassWhite,
                focusedContainerColor = GlassWhite, unfocusedContainerColor = GlassWhite,
                focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, cursorColor = TomatoRed
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Text("×", color = SubtleWhite, fontSize = 16.sp)
                    }
                }
            }
        )

        Spacer(Modifier.height(8.dp))

        if (visibleTemplates.isEmpty()) {
            Text(
                text = if (isSearching) "Aucun résultat pour « $searchQuery »"
                       else "Aucune tâche dans la bibliothèque.\nAjoute-en depuis l'onglet Biblio.",
                color = SubtleWhite,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(visibleTemplates, key = { it.id }) { template ->
                    val domain = domains.find { it.id == template.domainId }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .clickable { onSelect(template) },
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(text = template.title, color = TextWhite, fontSize = 14.sp)
                                Text(
                                    text = "${domain?.name ?: "—"} · ${template.defaultPomodoros} 🍅",
                                    color = SubtleWhite,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
