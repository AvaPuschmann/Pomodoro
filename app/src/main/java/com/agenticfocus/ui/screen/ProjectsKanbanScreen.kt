package com.agenticfocus.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agenticfocus.data.entity.KanbanStatus
import com.agenticfocus.data.entity.ProjectEntity
import com.agenticfocus.ui.components.ProjectCard
import com.agenticfocus.ui.theme.SubtleWhite
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.viewmodel.LibraryViewModel
import com.agenticfocus.viewmodel.ProjectViewModel
import com.agenticfocus.viewmodel.SortOrder
import kotlinx.coroutines.launch

/**
 * Mode Projet — Story 18-2 / Sprint 18.
 * Vue 2 — 4 onglets swipeables Backlog/Todo/Doing/Done.
 * - HorizontalPager + TabRow sync [F-3D/D41]
 * - WIP badge orange si Doing > limit (warning visuel, pas bloquant)
 * - Empty state si projects.isEmpty() [F-I]
 * - Sort options + filter archived dans overflow menu [F-M, F-P]
 * - Tap card → onOpenProject(id), long-press → ModalBottomSheet actions
 *
 * Pas de drag-drop V1 [D20 mobile] — déplacer = long-press → menu "Déplacer vers..."
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProjectsKanbanScreen(
    projectVM: ProjectViewModel,
    libraryVM: LibraryViewModel,
    onOpenProject: (String) -> Unit,
    onCreateProject: () -> Unit,
    onEditProject: (ProjectEntity) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by projectVM.state.collectAsStateWithLifecycle()
    val libraryState by libraryVM.state.collectAsStateWithLifecycle()
    val domains = libraryState.domains

    val tabs = listOf("Backlog", "Todo", "Doing", "Done")
    val statuses = KanbanStatus.ALL  // ["backlog","todo","doing","done"]
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    var menuExpanded by remember { mutableStateOf(false) }
    var actionsForProject by remember { mutableStateOf<ProjectEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        // TopAppBar custom : titre + ⊕ + ⋮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Projets",
                color = TextWhite,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onCreateProject) {
                Icon(Icons.Default.Add, contentDescription = "Nouveau projet", tint = TextWhite)
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Plus", tint = TextWhite)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    Text(
                        text = "Trier par",
                        color = SubtleWhite,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    DropdownMenuItem(
                        text = { Text("Modifié récemment") },
                        onClick = { projectVM.setSortOrder(SortOrder.UPDATED_DESC); menuExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Créé récemment") },
                        onClick = { projectVM.setSortOrder(SortOrder.CREATED_DESC); menuExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Nom A-Z") },
                        onClick = { projectVM.setSortOrder(SortOrder.NAME_ASC); menuExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Date cible") },
                        onClick = { projectVM.setSortOrder(SortOrder.TARGET_DATE_ASC); menuExpanded = false }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = state.showArchived, onCheckedChange = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Archivés")
                            }
                        },
                        onClick = { projectVM.setShowArchived(!state.showArchived); menuExpanded = false }
                    )
                }
            }
        }

        // Empty state ou contenu Kanban
        if (state.projects.filter { state.showArchived || !it.isArchived }.isEmpty()) {
            EmptyState(onCreateProject = onCreateProject)
        } else {
            // Sync TabRow ↔ Pager [F-3D/D41]
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }.collect { /* no-op : TabRow se met à jour via selectedTabIndex */ }
            }

            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { i, label ->
                    Tab(
                        selected = i == pagerState.currentPage,
                        onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
                        text = { Text(label, fontSize = 13.sp) }
                    )
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val status = statuses[page]
                val columnProjects = state.projects.filter { it.kanbanStatus == status }
                KanbanColumn(
                    status = status,
                    projects = columnProjects,
                    statsByProject = state.statsByProject,
                    domains = domains,
                    wipLimitDoing = state.wipLimitDoing,
                    onOpenProject = onOpenProject,
                    onLongPress = { p -> actionsForProject = p }
                )
            }
        }
    }

    // Actions bottom sheet (long-press card)
    val actionSheetState = rememberModalBottomSheetState()
    actionsForProject?.let { p ->
        ModalBottomSheet(
            sheetState = actionSheetState,
            onDismissRequest = { actionsForProject = null }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = p.name,
                    color = TextWhite,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                statuses.forEach { st ->
                    if (st != p.kanbanStatus) {
                        DropdownMenuItem(
                            text = { Text("Déplacer vers ${labelFor(st)}") },
                            onClick = {
                                projectVM.moveKanbanStatus(p.id, st)
                                actionsForProject = null
                            }
                        )
                    }
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Modifier") },
                    onClick = { onEditProject(p); actionsForProject = null }
                )
                if (p.isArchived) {
                    DropdownMenuItem(
                        text = { Text("Désarchiver") },
                        onClick = { projectVM.unarchiveProject(p.id); actionsForProject = null }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Archiver") },
                        onClick = { projectVM.archiveProject(p.id); actionsForProject = null }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Supprimer", color = Color(0xFFE53935)) },
                    onClick = { projectVM.deleteProject(p.id); actionsForProject = null }
                )
            }
        }
    }
}

private fun labelFor(status: String): String = when (status) {
    KanbanStatus.BACKLOG -> "Backlog"
    KanbanStatus.TODO -> "Todo"
    KanbanStatus.DOING -> "Doing"
    KanbanStatus.DONE -> "Done"
    else -> status
}

@Composable
private fun KanbanColumn(
    status: String,
    projects: List<ProjectEntity>,
    statsByProject: Map<String, com.agenticfocus.data.repository.ProjectStats>,
    domains: List<com.agenticfocus.data.entity.DomainEntity>,
    wipLimitDoing: Int,
    onOpenProject: (String) -> Unit,
    onLongPress: (ProjectEntity) -> Unit,
) {
    val isDoing = status == KanbanStatus.DOING
    val overLimit = isDoing && projects.size > wipLimitDoing
    val bgColor = if (overLimit) Color(0xFFFFF3E0) else Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        if (overLimit) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFF9800)
            ) {
                Text(
                    text = "⚠ WIP ${projects.size}/$wipLimitDoing",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucun projet ${labelFor(status).lowercase()}",
                    color = SubtleWhite,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = projects, key = { it.id }) { p ->
                    ProjectCard(
                        project = p,
                        stats = statsByProject[p.id],
                        domains = domains,
                        onClick = { onOpenProject(p.id) },
                        onLongPress = { onLongPress(p) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onCreateProject: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ViewKanban,
            contentDescription = null,
            tint = SubtleWhite,
            modifier = Modifier.size(96.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Aucun projet pour l'instant",
            color = TextWhite,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Crée ton premier projet pour structurer un objectif long terme",
            color = SubtleWhite,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Surface(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFE53935),
            onClick = onCreateProject
        ) {
            Text(
                text = "⊕ Mon premier projet",
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 14.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
