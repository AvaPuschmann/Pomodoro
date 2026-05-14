package com.agenticfocus.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agenticfocus.data.entity.KanbanStatus
import com.agenticfocus.data.entity.ProjectEntity
import com.agenticfocus.ui.components.ToggleChip
import com.agenticfocus.ui.components.formatDueDate
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.ui.theme.TomatoRed
import com.agenticfocus.viewmodel.LibraryViewModel
import com.agenticfocus.viewmodel.ProjectViewModel

/**
 * Mode Projet — Story 18-5 / Sprint 18.
 * ModalBottomSheet création/édition d'un projet.
 * - name (required, isNotBlank)
 * - description (optional)
 * - kanban_status (ToggleChip parmi backlog/todo/doing/done)
 * - domain (ToggleChip parmi domains library)
 * - target_date (DatePicker optionnel)
 *
 * Scrollable [memory item 8 — verticalScroll obligatoire pour bottom sheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectFormSheet(
    project: ProjectEntity?,  // null = création
    projectVM: ProjectViewModel,
    libraryVM: LibraryViewModel,
    onDismiss: () -> Unit,
) {
    val libraryState by libraryVM.state.collectAsStateWithLifecycle()
    val domains = libraryState.domains

    var name by remember { mutableStateOf(project?.name ?: "") }
    var description by remember { mutableStateOf(project?.description ?: "") }
    var kanbanStatus by remember { mutableStateOf(project?.kanbanStatus ?: KanbanStatus.BACKLOG) }
    var domainId by remember { mutableStateOf<String?>(project?.domainId) }
    var targetDate by remember { mutableStateOf<Long?>(project?.targetDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (project == null) "Nouveau projet" else "Modifier le projet",
                color = TextWhite,
                fontSize = 18.sp,
            )

            // Name (required)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom du projet") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = formColors(),
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optionnel)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                colors = formColors(),
            )

            // Kanban status
            Text("Statut", color = SubtleWhite, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KanbanStatus.ALL.forEach { st ->
                    ToggleChip(
                        label = labelForStatus(st),
                        selected = kanbanStatus == st,
                        selectedColor = TomatoRed,
                        onClick = { kanbanStatus = st }
                    )
                }
            }

            // Domain selector — combo box ExposedDropdownMenu (gain de place vs chips)
            if (domains.isNotEmpty()) {
                Text("Domaine", color = SubtleWhite, fontSize = 13.sp)
                var domainMenuExpanded by remember { mutableStateOf(false) }
                val selectedDomain = domains.find { it.id == domainId }
                ExposedDropdownMenuBox(
                    expanded = domainMenuExpanded,
                    onExpandedChange = { domainMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedDomain?.name ?: "Aucun",
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            // Pastille couleur du domaine sélectionné
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(
                                        color = if (selectedDomain != null) parseHexColorOr(selectedDomain.color, Color(0xFF6C6C70)) else Color(0xFF6C6C70),
                                        shape = CircleShape
                                    )
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = domainMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = formColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = domainMenuExpanded,
                        onDismissRequest = { domainMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Aucun") },
                            leadingIcon = {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(Color(0xFF6C6C70), CircleShape)
                                )
                            },
                            onClick = { domainId = null; domainMenuExpanded = false }
                        )
                        domains.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d.name) },
                                leadingIcon = {
                                    androidx.compose.foundation.layout.Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(parseHexColorOr(d.color, Color(0xFF6C6C70)), CircleShape)
                                    )
                                },
                                onClick = { domainId = d.id; domainMenuExpanded = false }
                            )
                        }
                    }
                }
            }

            // Target date
            Text("Date cible", color = SubtleWhite, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToggleChip(
                    label = if (targetDate != null) formatDueDate(targetDate!!) else "Choisir une date",
                    selected = targetDate != null,
                    selectedColor = Color(0xFF2196F3),
                    onClick = { showDatePicker = true }
                )
                if (targetDate != null) {
                    ToggleChip(
                        label = "Effacer",
                        selected = false,
                        selectedColor = Color(0xFF6C6C70),
                        onClick = { targetDate = null }
                    )
                }
            }

            Spacer(modifier = Modifier.size(8.dp))

            // Submit button
            Button(
                onClick = {
                    if (project == null) {
                        projectVM.addProject(
                            name = name,
                            description = description.takeIf { it.isNotBlank() },
                            kanbanStatus = kanbanStatus,
                            domainId = domainId,
                            targetDate = targetDate,
                        )
                    } else {
                        projectVM.updateProject(
                            id = project.id,
                            name = name,
                            description = description.takeIf { it.isNotBlank() },
                            kanbanStatus = kanbanStatus,
                            domainId = domainId,
                            targetDate = targetDate,
                        )
                    }
                    onDismiss()
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = TomatoRed)
            ) {
                Text(if (project == null) "Créer" else "Enregistrer")
            }

            Spacer(modifier = Modifier.size(8.dp))
        }
    }

    // DatePicker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = targetDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    targetDate = datePickerState.selectedDateMillis
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

private fun labelForStatus(status: String): String = when (status) {
    KanbanStatus.BACKLOG -> "Backlog"
    KanbanStatus.TODO -> "Todo"
    KanbanStatus.DOING -> "Doing"
    KanbanStatus.DONE -> "Done"
    else -> status
}

private fun parseHexColorOr(hex: String?, fallback: Color): Color {
    if (hex == null) return fallback
    return try {
        val clean = hex.removePrefix("#")
        val rgb = clean.toLong(16)
        Color(0xFF000000 or (rgb and 0xFFFFFF))
    } catch (_: Exception) {
        fallback
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
