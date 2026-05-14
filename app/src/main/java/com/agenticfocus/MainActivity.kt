package com.agenticfocus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agenticfocus.ui.theme.TextWhite
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewKanban
import com.agenticfocus.ui.screen.AddTaskForm
import com.agenticfocus.ui.screen.ProjectDetailScreen
import com.agenticfocus.ui.screen.ProjectFormSheet
import com.agenticfocus.ui.screen.ProjectsKanbanScreen
import com.agenticfocus.data.entity.ProjectEntity
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Sync
import androidx.compose.ui.graphics.Color
import java.time.LocalDate

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agenticfocus.data.auth.SupabaseAuthRepository
import com.agenticfocus.data.supabase.SupabaseClientProvider
import com.agenticfocus.data.sync.RealtimeSyncManager
import com.agenticfocus.data.sync.SyncStatusManager
import kotlinx.coroutines.flow.first
import com.agenticfocus.ui.screen.DayPlannerScreen
import com.agenticfocus.ui.screen.LibraryScreen
import com.agenticfocus.ui.screen.LoginScreen
import com.agenticfocus.ui.screen.PomodoroScreen
import com.agenticfocus.ui.screen.SettingsScreen
import com.agenticfocus.ui.screen.StatsScreen
import com.agenticfocus.ui.theme.AgenticFocusTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.saveable.rememberSaveable
import com.agenticfocus.BuildConfig
import com.agenticfocus.viewmodel.AuthState
import com.agenticfocus.viewmodel.AuthViewModel
import com.agenticfocus.viewmodel.AuthViewModelFactory
import com.agenticfocus.viewmodel.DayPlannerViewModel
import com.agenticfocus.viewmodel.LibraryViewModel
import com.agenticfocus.viewmodel.ProjectViewModel
import com.agenticfocus.viewmodel.RoutineViewModel
import com.agenticfocus.viewmodel.StatsViewModel
import com.agenticfocus.data.db.AppDatabase
import com.agenticfocus.data.repository.RoutineRepository
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

private enum class Tab(val label: String, val icon: ImageVector) {
    TIMER("Timer", Icons.Filled.PlayArrow),
    PLANNER("Planner", Icons.Default.List),
    // Mode Projet — Story 18-1 / Sprint 18. Gated FEATURE_PROJECTS (16-1).
    PROJECTS("Projets", Icons.Default.ViewKanban),
    LIBRARY("Biblio", Icons.Default.MenuBook),
    STATS("Stats", Icons.Default.BarChart)
    // SETTINGS retiré du bottom nav Sprint 18 — déplacé en topbar Library via ⚙ IconButton.
}

class MainActivity : ComponentActivity() {
    private var routineRepo: RoutineRepository? = null
    private var authenticatedUserId: String? = null

    override fun onResume() {
        super.onResume()
        val userId = authenticatedUserId ?: return
        val repo = routineRepo ?: return
        // A1 (F8) — Catch-up on Realtime events potentially missed while in background.
        // Periodic pull is now 5 min, so without this we'd wait up to 5 min after
        // foreground for stale data to reconcile.
        RealtimeSyncManager.triggerPull(userId)
        lifecycleScope.launch { repo.injectRoutinesForToday(userId) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen must be called before super.onCreate.
        // The splash stays visible until isAuthChecked=true (after restoreSession completes),
        // giving JIT time to warm up ViewRootImpl.performTraversals() while the system-rendered
        // splash is on screen — eliminating the black window background flash.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition {
            !SupabaseClientProvider.isReady.value || !StartupState.isAuthChecked
        }
        enableEdgeToEdge()
        setContent {
            AgenticFocusTheme {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val alreadyGranted = ContextCompat.checkSelfPermission(
                        this, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!alreadyGranted) {
                        val launcher = rememberLauncherForActivityResult(
                            ActivityResultContracts.RequestPermission()
                        ) {}
                        LaunchedEffect(Unit) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val supabaseReady by SupabaseClientProvider.isReady.collectAsStateWithLifecycle()

                if (!supabaseReady) {
                    // Supabase client is initialising on IO thread — show spinner instead of
                    // black window background.
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Client is ready — build auth layer.
                    val authRepository = remember {
                        SupabaseAuthRepository(SupabaseClientProvider.client)
                    }
                    val authVM: AuthViewModel = viewModel(
                        factory = AuthViewModelFactory(authRepository)
                    )

                    LaunchedEffect(Unit) {
                        authVM.restoreSession()
                    }

                    val authState by authVM.authState.collectAsStateWithLifecycle()

                    when (authState) {
                        is AuthState.Loading,
                        is AuthState.Unauthenticated -> {
                            LoginScreen(authViewModel = authVM)
                        }
                        is AuthState.Authenticated -> {
                            val userId = (authState as AuthState.Authenticated).userId
                            LaunchedEffect(userId) {
                                try {
                                    val db = AppDatabase.getInstance(this@MainActivity)
                                    // A1 (F6) — sync_queue cleanup for routines/routine_items now lives
                                    // in RealtimeSyncManager.startSync (one-shot, before first pullSync).
                                    // Centralised pullSync (incl. routines + routine_items) runs via
                                    // AuthViewModel.startSync in parallel of this LaunchedEffect.
                                    val repo = RoutineRepository(db.routineDao(), db.dayTaskDao())
                                    this@MainActivity.routineRepo = repo
                                    this@MainActivity.authenticatedUserId = userId
                                    // 1. Inject from LOCAL data immediately (no network needed)
                                    android.util.Log.d("MainActivity", "Launching routine injection (local) for $userId")
                                    repo.injectRoutinesForToday(userId)
                                    // 2. A1 (F7) — Wait for the first centralised pullSync to complete
                                    //    deterministically (replaces the old fixed delay(5000)).
                                    SyncStatusManager.firstPullCompleted.first { it }
                                    // 3. Re-inject now that routines + routine_items are in cache.
                                    repo.injectRoutinesForToday(userId)
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Routine injection failed", e)
                                }
                            }

                            val dayPlannerVM: DayPlannerViewModel = viewModel()
                            val libraryVM: LibraryViewModel = viewModel()
                            val routineVM: RoutineViewModel = viewModel()
                            val statsVM: StatsViewModel = viewModel()
                            // Mode Projet — Story 18-1. Instancié uniquement si FEATURE_PROJECTS=true
                            // (évite charger sync inutilement).
                            val projectVM: ProjectViewModel? =
                                if (BuildConfig.FEATURE_PROJECTS) viewModel() else null
                            var selectedTab by remember { mutableStateOf(Tab.TIMER) }
                            // Story 18-1 — Settings accessible via overlay déclenché depuis Library topbar.
                            var showSettings by rememberSaveable { mutableStateOf(false) }
                            // Story 18-5 — state pour ProjectFormSheet (création ou édition).
                            var projectFormVisible by remember { mutableStateOf(false) }
                            var projectFormEditing by remember { mutableStateOf<ProjectEntity?>(null) }
                            // Story 22-7 / Sprint 20 — ProjectTasksSheet trigger state
                            var tasksSheetForProject by remember { mutableStateOf<ProjectEntity?>(null) }
                            // Story 18-4 — state pour ProjectDetailScreen (id = visible si non null)
                            var openedProjectId by rememberSaveable { mutableStateOf<String?>(null) }
                            // Story 18-7 — state pour AddTaskForm pre-fill depuis ProjectDetailScreen
                            var addTaskForProjectId by remember { mutableStateOf<String?>(null) }

                            // D3 — Wire RoutineViewModel to current user (gates auto-create on firstPullCompleted)
                            LaunchedEffect(userId) {
                                routineVM.setUserId(userId)
                            }
                            // Story 18-1 — Wire ProjectViewModel au user courant (gated)
                            LaunchedEffect(userId) {
                                projectVM?.setUserId(userId)
                            }

                            val snackbarHostState = remember { SnackbarHostState() }
                            val coroutineScope = rememberCoroutineScope()

                            LaunchedEffect(Unit) {
                                dayPlannerVM.navigateToTimerEvent.collect {
                                    selectedTab = Tab.TIMER
                                }
                            }

                            LaunchedEffect(Unit) {
                                SyncStatusManager.conflictEvents.collect { message ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            }

                            // Story 18-1 — liste tabs dynamique gated FEATURE_PROJECTS.
                            // SETTINGS retiré du bottom nav, accessible via drawer hamburger ≡ topbar globale.
                            val visibleTabs = remember {
                                buildList {
                                    add(Tab.TIMER)
                                    add(Tab.PLANNER)
                                    if (BuildConfig.FEATURE_PROJECTS) add(Tab.PROJECTS)
                                    add(Tab.LIBRARY)
                                    add(Tab.STATS)
                                }
                            }

                            // Story 18-1bis — Drawer slide-in pour Settings + actions globales.
                            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                            ModalNavigationDrawer(
                                drawerState = drawerState,
                                drawerContent = {
                                    ModalDrawerSheet {
                                        Spacer(modifier = Modifier.size(24.dp))
                                        Text(
                                            "AgenticFocus",
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                            color = TextWhite,
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        NavigationDrawerItem(
                                            label = { Text("Réglages") },
                                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                            selected = false,
                                            onClick = {
                                                showSettings = true
                                                coroutineScope.launch { drawerState.close() }
                                            },
                                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                        )
                                        NavigationDrawerItem(
                                            label = { Text("Synchroniser") },
                                            icon = { Icon(Icons.Default.Sync, contentDescription = null) },
                                            selected = false,
                                            onClick = {
                                                this@MainActivity.authenticatedUserId?.let { uid ->
                                                    RealtimeSyncManager.triggerPull(uid)
                                                }
                                                coroutineScope.launch { drawerState.close() }
                                            },
                                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                        )
                                        NavigationDrawerItem(
                                            label = { Text("Déconnexion") },
                                            icon = { Icon(Icons.Default.Logout, contentDescription = null) },
                                            selected = false,
                                            onClick = {
                                                coroutineScope.launch { drawerState.close() }
                                                authVM.signOut()
                                            },
                                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                        )
                                    }
                                }
                            ) {

                            // Story 22-5 / Sprint 20 — WIA thresholds from AppPreferences, live update on Save.
                            val prefsForWia = remember { com.agenticfocus.data.AppPreferences(this@MainActivity) }
                            var wiaThresholds by remember {
                                mutableStateOf(
                                    com.agenticfocus.ui.components.WiaThresholds(
                                        green = prefsForWia.wiaThresholdsGreen,
                                        yellow = prefsForWia.wiaThresholdsYellow,
                                        orange = prefsForWia.wiaThresholdsOrange,
                                    )
                                )
                            }
                            androidx.compose.runtime.CompositionLocalProvider(
                                com.agenticfocus.ui.components.LocalWiaThresholds provides wiaThresholds
                            ) {
                            Scaffold(
                                snackbarHost = { SnackbarHost(snackbarHostState) },
                                topBar = {
                                    // Story 18-1bis — TopAppBar globale avec hamburger ≡ seul (drawer slide-in).
                                    // Title vide pour éviter superposition avec headers per-écran (date Day
                                    // Planner, "Ma Bibliothèque" Library, etc.). Cachée si overlay Settings.
                                    if (!showSettings) {
                                        TopAppBar(
                                            title = {},  // intentionnel — chaque écran a son propre header
                                            navigationIcon = {
                                                IconButton(onClick = {
                                                    coroutineScope.launch { drawerState.open() }
                                                }) {
                                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                                }
                                            },
                                            colors = TopAppBarDefaults.topAppBarColors(
                                                containerColor = Color.Transparent
                                            )
                                        )
                                    }
                                },
                                bottomBar = {
                                    // Cache le bottom nav si l'overlay Settings est actif (UX cohérence)
                                    if (!showSettings) {
                                        NavigationBar {
                                            visibleTabs.forEach { tab ->
                                                NavigationBarItem(
                                                    selected = selectedTab == tab,
                                                    onClick = { selectedTab = tab },
                                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                                    label = { Text(tab.label) }
                                                )
                                            }
                                        }
                                    }
                                }
                            ) { innerPadding ->
                                if (showSettings) {
                                    // Story 18-1 — Settings overlay full-screen avec callback close
                                    SettingsScreen(
                                        onSignOut = { authVM.signOut() },
                                        onManualSync = {
                                            this@MainActivity.authenticatedUserId?.let { uid ->
                                                RealtimeSyncManager.triggerPull(uid)
                                            }
                                        },
                                        onClose = { showSettings = false },
                                        onSaveWiaThresholds = { newThresholds ->
                                            // Story 22-5 — Live update propagation via CompositionLocal.
                                            wiaThresholds = newThresholds
                                        },
                                        contentPadding = innerPadding
                                    )
                                } else {
                                    when (selectedTab) {
                                        Tab.TIMER    -> PomodoroScreen(
                                            dayPlannerViewModel = dayPlannerVM,
                                            contentPadding = innerPadding
                                        )
                                        Tab.PLANNER  -> DayPlannerScreen(
                                            viewModel = dayPlannerVM,
                                            libraryViewModel = libraryVM,
                                            projectViewModel = projectVM,
                                            contentPadding = innerPadding
                                        )
                                        Tab.PROJECTS -> {
                                            // Story 18-2 — ProjectsKanbanScreen wired
                                            // onCreateProject / onEditProject / onOpenProject sont stub V1
                                            // jusqu'à ce que 18-5 (FormSheet) et 18-4 (DetailScreen) soient implémentés.
                                            if (projectVM != null) {
                                                val openedId = openedProjectId
                                                if (openedId != null) {
                                                    // Story 18-4 — ProjectDetailScreen overlay
                                                    ProjectDetailScreen(
                                                        projectId = openedId,
                                                        projectVM = projectVM,
                                                        libraryVM = libraryVM,
                                                        onClose = { openedProjectId = null },
                                                        onEditProject = {
                                                            val p = projectVM.state.value.projects.find { it.id == openedId }
                                                            if (p != null) {
                                                                projectFormEditing = p
                                                                projectFormVisible = true
                                                            }
                                                        },
                                                        onAddTask = { projectId -> addTaskForProjectId = projectId },
                                                        contentPadding = innerPadding
                                                    )
                                                } else {
                                                    ProjectsKanbanScreen(
                                                        projectVM = projectVM,
                                                        libraryVM = libraryVM,
                                                        onOpenProject = { id -> openedProjectId = id },
                                                        onCreateProject = {
                                                            projectFormEditing = null
                                                            projectFormVisible = true
                                                        },
                                                        onEditProject = { p ->
                                                            projectFormEditing = p
                                                            projectFormVisible = true
                                                        },
                                                        onOpenTasksSheet = { p -> tasksSheetForProject = p },  // Story 22-7
                                                        contentPadding = innerPadding
                                                    )
                                                }
                                                // Story 22-7 / Sprint 20 — ProjectTasksSheet overlay
                                                tasksSheetForProject?.let { project ->
                                                    com.agenticfocus.ui.screen.ProjectTasksSheet(
                                                        project = project,
                                                        projectVM = projectVM,
                                                        dayPlannerVM = dayPlannerVM,
                                                        onAddTask = {
                                                            addTaskForProjectId = project.id
                                                            tasksSheetForProject = null
                                                        },
                                                        onEditTask = { _ ->
                                                            // V1 mobile : édition simple = passer à ProjectDetailScreen
                                                            // qui contient sort + edit complet par tâche.
                                                            // Future : EditTaskForm directe ouverte ici.
                                                            openedProjectId = project.id
                                                            tasksSheetForProject = null
                                                        },
                                                        onDismiss = { tasksSheetForProject = null },
                                                        onNavigateToTimer = { selectedTab = Tab.TIMER },
                                                    )
                                                }
                                                // Story 18-5 — ProjectFormSheet overlay (création ou édition)
                                                if (projectFormVisible) {
                                                    ProjectFormSheet(
                                                        project = projectFormEditing,
                                                        projectVM = projectVM,
                                                        libraryVM = libraryVM,
                                                        onDismiss = {
                                                            projectFormVisible = false
                                                            projectFormEditing = null
                                                        }
                                                    )
                                                }
                                                // Story 18-7 — AddTaskForm overlay avec project_id pré-rempli
                                                val pendingProjectId = addTaskForProjectId
                                                if (pendingProjectId != null) {
                                                    val pendingProject = projectVM.state.value.projects.find { it.id == pendingProjectId }
                                                    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                                                    val libState = libraryVM.state.collectAsStateWithLifecycle().value
                                                    ModalBottomSheet(
                                                        sheetState = addSheetState,
                                                        onDismissRequest = { addTaskForProjectId = null },
                                                        containerColor = Color(0xFF1A1A1A)
                                                    ) {
                                                        AddTaskForm(
                                                            initialName = "",
                                                            initialDate = LocalDate.now(),
                                                            domains = libState.domains,
                                                            prefilledProjectName = pendingProject?.name,
                                                            onAdd = { name, impact, urgency, scheduledDate, note, plannedPomodoros, storyPoints, domainId, dueDate ->
                                                                dayPlannerVM.addTask(
                                                                    name = name,
                                                                    impact = impact,
                                                                    urgency = urgency,
                                                                    scheduledDate = scheduledDate,
                                                                    note = note,
                                                                    plannedPomodoros = plannedPomodoros,
                                                                    storyPoints = storyPoints,
                                                                    domainId = domainId,
                                                                    dueDate = dueDate,
                                                                    projectId = pendingProjectId
                                                                )
                                                                addTaskForProjectId = null
                                                            },
                                                            onDismiss = { addTaskForProjectId = null }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Tab.LIBRARY  -> LibraryScreen(
                                            libraryVM,
                                            routineVM,
                                            onOpenSettings = { showSettings = true },
                                            contentPadding = innerPadding
                                        )
                                        Tab.STATS    -> StatsScreen(statsVM, contentPadding = innerPadding)
                                    }
                                }
                            }
                            } // CompositionLocalProvider close — Story 22-5
                            } // ModalNavigationDrawer content lambda close — Story 18-1bis
                        }
                    }
                }
            }
        }
    }
}
