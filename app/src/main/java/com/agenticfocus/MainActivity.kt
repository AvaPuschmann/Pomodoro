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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agenticfocus.ui.screen.DayPlannerScreen
import com.agenticfocus.ui.screen.LibraryScreen
import com.agenticfocus.ui.screen.PomodoroScreen
import com.agenticfocus.ui.theme.AgenticFocusTheme
import com.agenticfocus.viewmodel.DayPlannerViewModel
import com.agenticfocus.viewmodel.LibraryViewModel

private enum class Tab(val label: String, val icon: ImageVector) {
    TIMER("Timer", Icons.Filled.PlayArrow),
    PLANNER("Planner", Icons.Default.List),
    LIBRARY("Biblio", Icons.Default.MenuBook)   // material-icons-extended
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

                val dayPlannerVM: DayPlannerViewModel = viewModel()
                val libraryVM: LibraryViewModel = viewModel()
                var selectedTab by remember { mutableStateOf(Tab.TIMER) }

                LaunchedEffect(Unit) {
                    dayPlannerVM.navigateToTimerEvent.collect {
                        selectedTab = Tab.TIMER
                    }
                }

                Scaffold(
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
                        Tab.TIMER    -> PomodoroScreen(contentPadding = innerPadding)
                        Tab.PLANNER  -> DayPlannerScreen(dayPlannerVM, libraryVM, contentPadding = innerPadding)
                        Tab.LIBRARY  -> LibraryScreen(libraryVM, contentPadding = innerPadding)
                    }
                }
            }
        }
    }
}
