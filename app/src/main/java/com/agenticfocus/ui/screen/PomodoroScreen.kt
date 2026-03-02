package com.agenticfocus.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.agenticfocus.R
import com.agenticfocus.ui.components.SessionButtons
import com.agenticfocus.ui.components.TaskInput
import com.agenticfocus.ui.components.TimerDial
import com.agenticfocus.ui.components.TomatoPlanner
import com.agenticfocus.viewmodel.Phase
import com.agenticfocus.viewmodel.PomodoroViewModel

@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel = viewModel(),
    contentPadding: PaddingValues = PaddingValues()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        // Nature background photo (grass + sky)
        Image(
            painter = painterResource(id = R.drawable.nature_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Semi-transparent overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.30f))
        )

        // Main content — verticalScroll handles small screens (5") and keyboard overlap
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(bottom = contentPadding.calculateBottomPadding()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // App title
            Text(
                text = "Agentic Focus",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Timer dial
            TimerDial(
                progress = if (state.totalSeconds > 0) {
                    state.remainingSeconds.toFloat() / state.totalSeconds.toFloat()
                } else 0f,
                phase = state.phase,
                modifier = Modifier.width(320.dp).height(320.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Timer countdown text
            Text(
                text = formatTime(state.remainingSeconds),
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Task name input
            TaskInput(
                value = state.taskName,
                onValueChange = viewModel::updateTaskName,
                modifier = Modifier.width(280.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pomodoro planner — tomato icons + +/- buttons
            TomatoPlanner(
                plannedPomodoros   = state.plannedPomodoros,
                completedPomodoros = state.completedPomodoros,
                phase              = state.phase,
                onIncrease         = viewModel::increasePlanned,
                onDecrease         = viewModel::decreasePlanned,
                modifier           = Modifier.width(280.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Focus / Break / Play buttons
            SessionButtons(
                isRunning = state.isRunning,
                onFocusClick = { viewModel.resetToPhase(Phase.FOCUS) },
                onBreakClick = { viewModel.resetToPhase(Phase.SHORT_BREAK) },
                onTogglePlay = {
                    if (state.isRunning) viewModel.pauseTimer() else viewModel.startTimer()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatTime(seconds: Int): String =
    "%02d:%02d".format(seconds / 60, seconds % 60)
