package com.agenticfocus.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agenticfocus.data.entity.KanbanStatus
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.ui.theme.TomatoRed
import com.agenticfocus.viewmodel.ProjectViewModel

/**
 * Story 18-hotfix 2026-05-13 — Bottom sheet pour configurer les 4 colonnes Kanban :
 * - Nom (label) personnalisé
 * - WIP limit (0 = illimité, pas de warning)
 *
 * Local-only per-device V1 [D25/F4]. Pas de sync Supabase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanConfigSheet(
    projectVM: ProjectViewModel,
    onDismiss: () -> Unit,
) {
    val state by projectVM.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Local edit state per status
    val statuses = KanbanStatus.ALL
    val localLabels = remember {
        mutableStateOf(statuses.associateWith { state.labelsByStatus[it] ?: defaultLabelFor(it) })
    }
    val localLimits = remember {
        mutableStateOf(statuses.associateWith { (state.wipLimitByStatus[it] ?: 0).toString() })
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Configurer le Kanban",
                color = TextWhite,
                fontSize = 18.sp,
            )
            Text(
                text = "Personnalise le nom et la limite WIP de chaque colonne. WIP = 0 → illimité (pas de warning).",
                color = SubtleWhite,
                fontSize = 12.sp,
            )

            Spacer(modifier = Modifier.size(4.dp))

            statuses.forEach { status ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Colonne « ${defaultLabelFor(status)} »",
                        color = SubtleWhite,
                        fontSize = 12.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = localLabels.value[status] ?: "",
                            onValueChange = { new ->
                                localLabels.value = localLabels.value + (status to new)
                            },
                            label = { Text("Nom") },
                            singleLine = true,
                            modifier = Modifier.weight(2f),
                            colors = formColors(),
                        )
                        OutlinedTextField(
                            value = localLimits.value[status] ?: "0",
                            onValueChange = { new ->
                                if (new.isEmpty() || new.all { it.isDigit() }) {
                                    localLimits.value = localLimits.value + (status to new)
                                }
                            },
                            label = { Text("WIP") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = formColors(),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(8.dp))

            Button(
                onClick = {
                    statuses.forEach { status ->
                        val label = localLabels.value[status] ?: defaultLabelFor(status)
                        val limit = (localLimits.value[status] ?: "0").toIntOrNull() ?: 0
                        projectVM.setKanbanColumnConfig(status, label, limit)
                    }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = TomatoRed)
            ) {
                Text("Enregistrer")
            }
            Spacer(modifier = Modifier.size(8.dp))
        }
    }
}

private fun defaultLabelFor(status: String): String = when (status) {
    KanbanStatus.BACKLOG -> "Backlog"
    KanbanStatus.TODO -> "Todo"
    KanbanStatus.DOING -> "Doing"
    KanbanStatus.DONE -> "Done"
    else -> status
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
