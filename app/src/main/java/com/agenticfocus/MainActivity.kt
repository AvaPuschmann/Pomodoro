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
import com.agenticfocus.ui.screen.PomodoroScreen
import com.agenticfocus.ui.theme.AgenticFocusTheme
import com.agenticfocus.viewmodel.DayPlannerViewModel

private enum class Tab(val label: String, val icon: ImageVector) {
    TIMER("Timer", Icons.Filled.PlayArrow),      // confirmed in material-icons-core
    PLANNER("Planner", Icons.Default.List)       // confirmed in material-icons-core
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
                    // innerPadding passed to screens so content is not occluded
                    // by the NavigationBar or system gesture bar
                    when (selectedTab) {
                        Tab.TIMER   -> PomodoroScreen(contentPadding = innerPadding)
                        Tab.PLANNER -> DayPlannerScreen(dayPlannerVM, contentPadding = innerPadding)
                    }
                }
            }
        }
    }
}
