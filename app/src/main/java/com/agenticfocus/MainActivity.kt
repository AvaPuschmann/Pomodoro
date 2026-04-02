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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings

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
import com.agenticfocus.data.sync.SyncStatusManager
import com.agenticfocus.ui.screen.DayPlannerScreen
import com.agenticfocus.ui.screen.LibraryScreen
import com.agenticfocus.ui.screen.LoginScreen
import com.agenticfocus.ui.screen.PomodoroScreen
import com.agenticfocus.ui.screen.SettingsScreen
import com.agenticfocus.ui.screen.StatsScreen
import com.agenticfocus.ui.theme.AgenticFocusTheme
import com.agenticfocus.viewmodel.AuthState
import com.agenticfocus.viewmodel.AuthViewModel
import com.agenticfocus.viewmodel.AuthViewModelFactory
import com.agenticfocus.viewmodel.DayPlannerViewModel
import com.agenticfocus.viewmodel.LibraryViewModel
import com.agenticfocus.viewmodel.StatsViewModel
import kotlinx.coroutines.launch

private enum class Tab(val label: String, val icon: ImageVector) {
    TIMER("Timer", Icons.Filled.PlayArrow),
    PLANNER("Planner", Icons.Default.List),
    LIBRARY("Biblio", Icons.Default.MenuBook),
    STATS("Stats", Icons.Default.BarChart),
    SETTINGS("Réglages", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
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
                            val dayPlannerVM: DayPlannerViewModel = viewModel()
                            val libraryVM: LibraryViewModel = viewModel()
                            val statsVM: StatsViewModel = viewModel()
                            var selectedTab by remember { mutableStateOf(Tab.TIMER) }

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

                            Scaffold(
                                snackbarHost = { SnackbarHost(snackbarHostState) },
                                bottomBar = {
                                    NavigationBar {
                                        Tab.entries.forEach { tab ->
                                            NavigationBarItem(
                                                selected = selectedTab == tab,
                                                onClick = { selectedTab = tab },
                                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                                label = { Text(tab.label) }
                                            )
                                        }
                                    }
                                }
                            ) { innerPadding ->
                                when (selectedTab) {
                                    Tab.TIMER    -> PomodoroScreen(
                                        dayPlannerViewModel = dayPlannerVM,
                                        contentPadding = innerPadding
                                    )
                                    Tab.PLANNER  -> DayPlannerScreen(dayPlannerVM, libraryVM, contentPadding = innerPadding)
                                    Tab.LIBRARY  -> LibraryScreen(libraryVM, contentPadding = innerPadding)
                                    Tab.STATS    -> StatsScreen(statsVM, contentPadding = innerPadding)
                                    Tab.SETTINGS -> SettingsScreen(
                                        onSignOut = { authVM.signOut() },
                                        contentPadding = innerPadding
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
