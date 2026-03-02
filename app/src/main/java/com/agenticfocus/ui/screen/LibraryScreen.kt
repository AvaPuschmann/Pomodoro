package com.agenticfocus.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agenticfocus.R
import com.agenticfocus.data.entity.DomainEntity
import com.agenticfocus.ui.theme.GlassWhite
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.ui.theme.TomatoRed
import com.agenticfocus.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    contentPadding: PaddingValues = PaddingValues()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    // Track which domains are expanded
    val expandedDomains = remember { mutableStateOf(setOf<String>()) }

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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ma Bibliothèque", color = TextWhite, fontSize = 18.sp)
                Text(
                    "${state.templatesByDomain.values.sumOf { it.size }} tâches",
                    color = SubtleWhite,
                    fontSize = 14.sp
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                items(state.domains, key = { it.id }) { domain ->
                    val templates = state.templatesByDomain[domain.id] ?: emptyList()
                    val isExpanded = domain.id in expandedDomains.value

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        color = Color.Black.copy(alpha = 0.50f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                    ) {
                        Column {
                            // Domain header row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Domain color dot
                                val dotColor = remember(domain.color) {
                                    runCatching { Color(domain.color.toColorInt()) }
                                        .getOrDefault(Color.Gray)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(dotColor, CircleShape)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = domain.name,
                                    color = TextWhite,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${templates.size}",
                                    color = SubtleWhite,
                                    fontSize = 13.sp
                                )
                                IconButton(onClick = {
                                    expandedDomains.value = if (isExpanded)
                                        expandedDomains.value - domain.id
                                    else
                                        expandedDomains.value + domain.id
                                }) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = SubtleWhite
                                    )
                                }
                            }

                            // Templates (expanded)
                            if (isExpanded) {
                                templates.forEach { template ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 32.dp, end = 12.dp, bottom = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = template.title,
                                                color = TextWhite,
                                                fontSize = 14.sp
                                            )
                                            if (!template.note.isNullOrBlank()) {
                                                Text(
                                                    text = template.note,
                                                    color = SubtleWhite,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${template.storyPoints}pts",
                                            color = SubtleWhite,
                                            fontSize = 12.sp
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "🍅×${template.defaultPomodoros}",
                                            color = SubtleWhite,
                                            fontSize = 12.sp
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteTemplate(template.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Supprimer",
                                                tint = TomatoRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) } // FAB clearance
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = TomatoRed,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = contentPadding.calculateBottomPadding() + 16.dp
                )
        ) {
            Icon(Icons.Default.Add, contentDescription = "Ajouter", tint = Color.White)
        }
    }

    // Add template dialog
    if (showAddDialog) {
        AddTemplateDialog(
            domains = state.domains,
            onConfirm = { title, note, domainId, points, pomodoros ->
                viewModel.addTemplate(title, note, domainId, points, pomodoros)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTemplateDialog(
    domains: List<DomainEntity>,
    onConfirm: (title: String, note: String?, domainId: String, storyPoints: Int, defaultPomodoros: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedDomain by remember { mutableStateOf(domains.firstOrNull()) }
    var storyPoints by remember { mutableIntStateOf(20) }
    var defaultPomodoros by remember { mutableIntStateOf(1) }
    var domainMenuExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = TomatoRed,
        unfocusedBorderColor = GlassWhite,
        focusedContainerColor = Color.Black.copy(alpha = 0.6f),
        unfocusedContainerColor = Color.Black.copy(alpha = 0.6f),
        focusedTextColor = TextWhite,
        unfocusedTextColor = TextWhite,
        cursorColor = TomatoRed
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text("Nouvelle tâche", color = TextWhite) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre *", color = SubtleWhite) },
                    colors = fieldColors,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optionnel)", color = SubtleWhite) },
                    colors = fieldColors,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                // Domain picker
                ExposedDropdownMenuBox(
                    expanded = domainMenuExpanded,
                    onExpandedChange = { domainMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedDomain?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Domaine", color = SubtleWhite) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = domainMenuExpanded) },
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = domainMenuExpanded,
                        onDismissRequest = { domainMenuExpanded = false }
                    ) {
                        domains.forEach { domain ->
                            DropdownMenuItem(
                                text = { Text(domain.name) },
                                onClick = {
                                    selectedDomain = domain
                                    domainMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                // Story points + pomodoros
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = storyPoints.toString(),
                        onValueChange = { storyPoints = it.toIntOrNull()?.coerceIn(1, 999) ?: storyPoints },
                        label = { Text("Points", color = SubtleWhite) },
                        colors = fieldColors,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = defaultPomodoros.toString(),
                        onValueChange = { defaultPomodoros = it.toIntOrNull()?.coerceIn(1, 6) ?: defaultPomodoros },
                        label = { Text("🍅 défaut", color = SubtleWhite) },
                        colors = fieldColors,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank() && selectedDomain != null) {
                        onConfirm(title.trim(), note.takeIf { it.isNotBlank() }, selectedDomain!!.id, storyPoints, defaultPomodoros)
                    }
                }
            ) { Text("Ajouter", color = TomatoRed) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = SubtleWhite) }
        }
    )
}
