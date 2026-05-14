package com.agenticfocus.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.agenticfocus.data.entity.TagEntity
import com.agenticfocus.data.entity.TaskTemplateEntity
import com.agenticfocus.ui.components.ToggleChip
import com.agenticfocus.ui.components.formatDueDate
import com.agenticfocus.ui.theme.GlassWhite
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.ui.theme.TomatoRed
import com.agenticfocus.viewmodel.LibraryViewModel
import com.agenticfocus.viewmodel.RoutineViewModel

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
    routineViewModel: RoutineViewModel,
    onOpenSettings: () -> Unit = {},  // Story 18-1 — Settings via ⚙ topbar
    contentPadding: PaddingValues = PaddingValues()
) {
    // D3 — F2: nav swap au ROOT du composable, AVANT le rendu Library, pour éviter
    //        nested LazyColumn et permettre des écrans imbriqués propres.
    var libraryNav by rememberSaveable { mutableStateOf("LIBRARY") }
    var editingRoutineId by rememberSaveable { mutableStateOf<String?>(null) }

    when (libraryNav) {
        "ROUTINES_LIST" -> {
            RoutinesScreen(
                viewModel = routineViewModel,
                onSelectRoutine = { id -> editingRoutineId = id; libraryNav = "ROUTINE_EDITOR" },
                onBack = { libraryNav = "LIBRARY" },
                contentPadding = contentPadding
            )
            return
        }
        "ROUTINE_EDITOR" -> {
            val id = editingRoutineId
            if (id != null) {
                RoutineEditorScreen(
                    routineId = id,
                    routineViewModel = routineViewModel,
                    libraryViewModel = viewModel,
                    onBack = { libraryNav = "ROUTINES_LIST"; editingRoutineId = null },
                    contentPadding = contentPadding
                )
                return
            } else {
                libraryNav = "ROUTINES_LIST"  // safety fallback
            }
        }
        // "LIBRARY" — fall through to existing rendering
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    // Story 18-hotfix : gate Routines section selon AppPreferences.featureRoutines
    // Lecture à chaque recomposition (pas de remember) pour que le toggle Settings
    // prenne effet immédiat sans redémarrer l'app.
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val featureRoutines = com.agenticfocus.data.AppPreferences(ctx).featureRoutines
    var showAddTemplateDialog by rememberSaveable { mutableStateOf(false) }
    var showAddDomainDialog by rememberSaveable { mutableStateOf(false) }
    var showAddTagDialog by rememberSaveable { mutableStateOf(false) }
    var editingDomain by remember { mutableStateOf<DomainEntity?>(null) }
    var editingTemplate by remember { mutableStateOf<TaskTemplateEntity?>(null) }
    var editingTag by remember { mutableStateOf<TagEntity?>(null) }
    val expandedDomains = remember { mutableStateOf(setOf<String>()) }
    // D2 — Full-text search on template titles (parity with desktop LibraryPicker)
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val isSearching = searchQuery.trim().isNotBlank()
    val normalizedQuery = searchQuery.trim().lowercase()
    val filteredTemplatesByDomain: Map<String, List<TaskTemplateEntity>> = remember(state, searchQuery) {
        if (!isSearching) state.templatesByDomain
        else state.templatesByDomain.mapValues { (_, templates) ->
            templates.filter { it.title.lowercase().contains(normalizedQuery) }
        }
    }
    val visibleDomains: List<DomainEntity> = remember(state, searchQuery) {
        if (!isSearching) state.domains
        else state.domains.filter { (filteredTemplatesByDomain[it.id]?.isNotEmpty() == true) }
    }
    // Auto-expand all matching domains while searching
    val displayExpanded: Set<String> = if (isSearching) visibleDomains.map { it.id }.toSet() else expandedDomains.value

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
                // Fix 2026-05-13 : top padding aussi (TopAppBar globale Sprint 18-1bis)
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding()
                )
        ) {
            // Header (Story 18-1 — ⚙ Settings IconButton ajouté à droite)
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
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Paramètres",
                            tint = TextWhite
                        )
                    }
                }
            }

            // D2 — Search bar (full-text filter on template titles)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher…", color = SubtleWhite) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TomatoRed,
                    unfocusedBorderColor = GlassWhite,
                    focusedContainerColor = GlassWhite,
                    unfocusedContainerColor = GlassWhite,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    cursorColor = TomatoRed
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(36.dp)) {
                            Text("×", color = SubtleWhite, fontSize = 16.sp)
                        }
                    }
                }
            )

            if (isSearching && visibleDomains.isEmpty()) {
                Text(
                    text = "Aucun résultat pour « $searchQuery »",
                    color = SubtleWhite,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                // D3 — F1: Routines entry must be wrapped in `item { ... }` since LazyColumn
                //         scope rejects bare composables. Hidden while searching to keep results clean.
                // Story 18-hotfix : aussi caché si feature flag désactivé via Settings.
                if (!isSearching && featureRoutines) {
                    item(key = "routines_entry") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 0.dp, vertical = 6.dp)
                                .clickable { libraryNav = "ROUTINES_LIST" },
                            color = Color.Black.copy(alpha = 0.50f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("☀️🌙  Mes Routines", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Text("›", color = SubtleWhite, fontSize = 18.sp)
                            }
                        }
                    }
                }

                // Tags section — Story 22-2 / Sprint 20 (gated FEATURE_PROJECTS via runtime check
                // commented out for V1 : section visible toujours, useful for Library + projects).
                if (!isSearching) {
                    item(key = "tags_section") {
                        TagsSection(
                            tags = state.tags,
                            onAddTag = { showAddTagDialog = true },
                            onEditTag = { editingTag = it },
                        )
                    }
                }

                items(visibleDomains, key = { it.id }) { domain ->
                    val templates = filteredTemplatesByDomain[domain.id] ?: emptyList()
                    val isExpanded = domain.id in displayExpanded

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

        // Bottom action bar — deux boutons labellisés
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = contentPadding.calculateBottomPadding() + 12.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { showAddDomainDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Domaine", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = { showAddTemplateDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Tâche", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    // Dialog : nouvelle tâche
    if (showAddTemplateDialog) {
        TemplateDialog(
            domains = state.domains,
            initial = null,
            onConfirm = { title, note, domainId, points, pomodoros, impact, urgency, dueDate ->
                viewModel.addTemplate(title, note, domainId, points, pomodoros, impact, urgency, dueDate)
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
            onConfirm = { title, note, domainId, points, pomodoros, impact, urgency, dueDate ->
                viewModel.updateTemplate(template.id, title, note, domainId, points, pomodoros, impact, urgency, dueDate)
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

    // Story 22-2 / Sprint 20 — Tag dialogs
    if (showAddTagDialog) {
        TagDialog(
            initial = null,
            onConfirm = { name, color ->
                viewModel.addTag(name, color)
                showAddTagDialog = false
            },
            onDismiss = { showAddTagDialog = false }
        )
    }
    editingTag?.let { tag ->
        TagDialog(
            initial = tag,
            onConfirm = { name, color ->
                viewModel.updateTag(tag.id, name, color)
                editingTag = null
            },
            onDelete = {
                viewModel.deleteTag(tag.id)
                editingTag = null
            },
            onDismiss = { editingTag = null }
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
    onConfirm: (title: String, note: String?, domainId: String, storyPoints: Int, defaultPomodoros: Int, impact: String?, urgency: String?, dueDate: Long?) -> Unit,
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
    var impact by remember { mutableStateOf(initial?.impact) }
    var urgency by remember { mutableStateOf(initial?.urgency) }
    var dueDate by remember { mutableStateOf(initial?.dueDate) }
    var domainMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
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
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                    minLines = 2,
                    maxLines = 3,
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
                                onClick = { selectedDomain = domain; domainMenuExpanded = false }
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = defaultPomodoros.toString(),
                        onValueChange = { defaultPomodoros = it.toIntOrNull()?.coerceIn(1, 6) ?: defaultPomodoros },
                        label = { Text("🍅 défaut", color = SubtleWhite) },
                        colors = fieldColors,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Impact
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Impact", color = SubtleWhite, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToggleChip("↑ Élevé", impact == "high", androidx.compose.ui.graphics.Color(0xFFE53935)) { impact = if (impact == "high") null else "high" }
                        ToggleChip("↓ Faible", impact == "low", androidx.compose.ui.graphics.Color(0xFF6C6C70)) { impact = if (impact == "low") null else "low" }
                    }
                }

                // Urgence
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Urgence", color = SubtleWhite, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToggleChip("⚡ Urgent", urgency == "urgent", androidx.compose.ui.graphics.Color(0xFFFF9800)) { urgency = if (urgency == "urgent") null else "urgent" }
                        ToggleChip("✓ Pas urgent", urgency == "not_urgent", androidx.compose.ui.graphics.Color(0xFF4CAF50)) { urgency = if (urgency == "not_urgent") null else "not_urgent" }
                    }
                }

                // Date limite
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Date limite", color = SubtleWhite, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToggleChip(
                            label = if (dueDate != null) formatDueDate(dueDate!!) else "Choisir une date",
                            selected = dueDate != null,
                            selectedColor = androidx.compose.ui.graphics.Color(0xFF2196F3),
                            onClick = { showDatePicker = true }
                        )
                        if (dueDate != null) {
                            TextButton(onClick = { dueDate = null }) { Text("×", color = SubtleWhite, fontSize = 16.sp) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank() && selectedDomain != null) {
                        onConfirm(title.trim(), note.takeIf { it.isNotBlank() }, selectedDomain!!.id, storyPoints, defaultPomodoros, impact, urgency, dueDate)
                    }
                }
            ) { Text(if (initial == null) "Ajouter" else "Enregistrer", color = TomatoRed) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = SubtleWhite) }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dueDate = datePickerState.selectedDateMillis; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annuler") } }
        ) { DatePicker(state = datePickerState) }
    }
}

// ── Story 22-2 / Sprint 20 — Tags section + dialog ─────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(
    tags: List<TagEntity>,
    onAddTag: () -> Unit,
    onEditTag: (TagEntity) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        color = Color.Black.copy(alpha = 0.50f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("🏷  Mes Tags", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text("${tags.size}", color = SubtleWhite, fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                tags.forEach { tag ->
                    val tagColor = remember(tag.color) {
                        runCatching { Color(tag.color.toColorInt()) }.getOrDefault(Color.Gray)
                    }
                    Surface(
                        modifier = Modifier.clickable { onEditTag(tag) },
                        color = tagColor,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = tag.name,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                // "+ Tag" button — dashed style
                Surface(
                    modifier = Modifier.clickable { onAddTag() },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter un tag", tint = SubtleWhite, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tag", color = SubtleWhite, fontSize = 12.sp)
                    }
                }
            }
            if (tags.isEmpty()) {
                Text(
                    "Crée des tags pour étiqueter tes projets et templates.",
                    color = SubtleWhite,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun TagDialog(
    initial: TagEntity?,
    onConfirm: (name: String, color: String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var selectedColor by remember {
        mutableStateOf(initial?.color ?: domainColorPalette.first())
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color(0xFF1A1A1A),
            title = { Text("Supprimer le tag ?", color = TextWhite) },
            text = { Text("Les projets/templates ayant ce tag conservent la référence côté serveur jusqu'à leur prochain edit.", color = SubtleWhite) },
            confirmButton = {
                TextButton(onClick = { onDelete?.invoke() }) {
                    Text("Supprimer", color = TomatoRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Annuler", color = SubtleWhite) }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text(if (initial == null) "Nouveau tag" else "Modifier le tag", color = TextWhite) },
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    domainColorPalette.chunked(5).forEach { rowColors ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowColors.forEach { hex ->
                                val c = remember(hex) { Color(hex.toColorInt()) }
                                val isSelected = hex == selectedColor
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(c, CircleShape)
                                        .then(
                                            if (isSelected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier
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
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim(), selectedColor) }) {
                Text(if (initial == null) "Ajouter" else "Enregistrer", color = TomatoRed)
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = TomatoRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Supprimer", color = TomatoRed)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Annuler", color = SubtleWhite) }
            }
        }
    )
}
