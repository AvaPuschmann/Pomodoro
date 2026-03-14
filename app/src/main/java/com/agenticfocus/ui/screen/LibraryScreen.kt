package com.agenticfocus.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.agenticfocus.data.entity.TaskTemplateEntity
import com.agenticfocus.ui.theme.GlassWhite
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.ui.theme.TomatoRed
import com.agenticfocus.viewmodel.LibraryViewModel

// Palette de couleurs proposées pour les domaines
private val domainColorPalette = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#673AB7",
    "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
    "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
    "#FFC107", "#FF9800", "#FF5722", "#795548",
    "#66BB6A", "#26A69A", "#42A5F5", "#AB47BC"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    contentPadding: PaddingValues = PaddingValues()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddTemplateDialog by rememberSaveable { mutableStateOf(false) }
    var showAddDomainDialog by rememberSaveable { mutableStateOf(false) }
    var editingDomain by remember { mutableStateOf<DomainEntity?>(null) }
    var editingTemplate by remember { mutableStateOf<TaskTemplateEntity?>(null) }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${state.templatesByDomain.values.sumOf { it.size }} tâches",
                        color = SubtleWhite,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { showAddDomainDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Ajouter un domaine",
                            tint = SubtleWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
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
                                IconButton(
                                    onClick = { editingDomain = domain },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Modifier",
                                        tint = SubtleWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
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
                                            .padding(start = 32.dp, end = 4.dp, bottom = 8.dp),
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
                                            onClick = { editingTemplate = template },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Modifier",
                                                tint = SubtleWhite,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteTemplate(template.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Supprimer",
                                                tint = TomatoRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        // FAB — ajouter une tâche
        FloatingActionButton(
            onClick = { showAddTemplateDialog = true },
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

    // Dialog : nouvelle tâche
    if (showAddTemplateDialog) {
        TemplateDialog(
            domains = state.domains,
            initial = null,
            onConfirm = { title, note, domainId, points, pomodoros ->
                viewModel.addTemplate(title, note, domainId, points, pomodoros)
                showAddTemplateDialog = false
            },
            onDismiss = { showAddTemplateDialog = false }
        )
    }

    // Dialog : éditer une tâche
    editingTemplate?.let { template ->
        TemplateDialog(
            domains = state.domains,
            initial = template,
            onConfirm = { title, note, domainId, points, pomodoros ->
                viewModel.updateTemplate(template.id, title, note, domainId, points, pomodoros)
                editingTemplate = null
            },
            onDismiss = { editingTemplate = null }
        )
    }

    // Dialog : nouveau domaine
    if (showAddDomainDialog) {
        DomainDialog(
            initial = null,
            onConfirm = { name, color ->
                viewModel.addDomain(name, color)
                showAddDomainDialog = false
            },
            onDismiss = { showAddDomainDialog = false }
        )
    }

    // Dialog : éditer domaine
    editingDomain?.let { domain ->
        val templateCount = state.templatesByDomain[domain.id]?.size ?: 0
        DomainDialog(
            initial = domain,
            onConfirm = { name, color ->
                viewModel.updateDomain(domain.id, name, color)
                editingDomain = null
            },
            onDelete = {
                viewModel.deleteDomain(domain.id)
                editingDomain = null
            },
            templateCount = templateCount,
            onDismiss = { editingDomain = null }
        )
    }
}

@Composable
private fun DomainDialog(
    initial: DomainEntity?,
    onConfirm: (name: String, color: String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    templateCount: Int = 0
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var selectedColor by remember {
        mutableStateOf(
            if (initial != null) initial.color
            else domainColorPalette.first()
        )
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Supprimer le domaine ?", color = TextWhite) },
            text = {
                val msg = if (templateCount > 0)
                    "Ce domaine contient $templateCount tâche(s) qui seront également supprimées."
                else
                    "Cette action est irréversible."
                Text(msg, color = SubtleWhite)
            },
            confirmButton = {
                TextButton(onClick = { onDelete?.invoke() }) {
                    Text("Supprimer", color = TomatoRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annuler", color = SubtleWhite)
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = {
            Text(
                if (initial == null) "Nouveau domaine" else "Modifier le domaine",
                color = TextWhite
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom *", color = SubtleWhite) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TomatoRed,
                        unfocusedBorderColor = GlassWhite,
                        focusedContainerColor = Color.Black.copy(alpha = 0.6f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.6f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = TomatoRed
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Couleur", color = SubtleWhite, fontSize = 13.sp)
                // Color grid — 4 columns
                val chunked = domainColorPalette.chunked(4)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunked.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { hex ->
                                val c = remember(hex) {
                                    runCatching { Color(hex.toColorInt()) }.getOrDefault(Color.Gray)
                                }
                                val isSelected = hex == selectedColor
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(c, CircleShape)
                                        .then(
                                            if (isSelected) Modifier.border(
                                                2.dp, Color.White, CircleShape
                                            ) else Modifier
                                        )
                                        .clickable { selectedColor = hex }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name.trim(), selectedColor)
                }
            ) { Text(if (initial == null) "Ajouter" else "Enregistrer", color = TomatoRed) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = TomatoRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Supprimer", color = TomatoRed)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Annuler", color = SubtleWhite) }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateDialog(
    domains: List<DomainEntity>,
    initial: TaskTemplateEntity?,
    onConfirm: (title: String, note: String?, domainId: String, storyPoints: Int, defaultPomodoros: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var selectedDomain by remember {
        mutableStateOf(
            if (initial != null) domains.find { it.id == initial.domainId } ?: domains.firstOrNull()
            else domains.firstOrNull()
        )
    }
    var storyPoints by remember { mutableIntStateOf(initial?.storyPoints ?: 20) }
    var defaultPomodoros by remember { mutableIntStateOf(initial?.defaultPomodoros ?: 1) }
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
        title = { Text(if (initial == null) "Nouvelle tâche" else "Modifier la tâche", color = TextWhite) },
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
            ) { Text(if (initial == null) "Ajouter" else "Enregistrer", color = TomatoRed) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = SubtleWhite) }
        }
    )
}
