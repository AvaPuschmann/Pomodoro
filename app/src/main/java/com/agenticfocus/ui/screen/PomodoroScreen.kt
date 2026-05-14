package com.agenticfocus.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import com.agenticfocus.R
import com.agenticfocus.data.AppPreferences
import com.agenticfocus.ui.components.SessionButtons
import com.agenticfocus.ui.components.TaskInput
import com.agenticfocus.ui.components.TimerDial
import com.agenticfocus.ui.components.TomatoPlanner
import com.agenticfocus.ui.components.dayKey
import com.agenticfocus.ui.theme.TextWhite
import com.agenticfocus.viewmodel.DayPlannerViewModel
import com.agenticfocus.viewmodel.Phase
import com.agenticfocus.viewmodel.PomodoroViewModel
import java.time.LocalDate

@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel = viewModel(),
    dayPlannerViewModel: DayPlannerViewModel? = null,
    contentPadding: PaddingValues = PaddingValues()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val goalsEnabled = remember { AppPreferences(context).featureGoals }
    val emptyGoalsFlow = remember { kotlinx.coroutines.flow.MutableStateFlow<List<com.agenticfocus.data.entity.GoalEntity>>(emptyList()) }
    val goals by (dayPlannerViewModel?.goals ?: emptyGoalsFlow)
        .collectAsStateWithLifecycle()
    val todayKey = remember { dayKey(LocalDate.now()) }
    val dayGoal = goals.find { it.type == "day" && it.periodKey == todayKey }

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
                // Fix 2026-05-13 : top padding aussi (TopAppBar globale Sprint 18-1bis)
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding()
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // App title
            Text(
                text = "Agentic Focus",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Day goal block — only when feature is enabled
            if (goalsEnabled) {
                if (dayGoal != null) {
                    Column(
                        modifier = Modifier
                            .width(300.dp)
                            .background(Color(0xFF1A1A1A).copy(alpha = 0.75f), RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "☀️ Objectif du jour",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dayGoal.text,
                            color = if (dayGoal.isCompleted == 1) Color.White.copy(alpha = 0.45f) else Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (dayGoal.isCompleted == 1) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = "🎯 Définir l'objectif du jour",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

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

            // Estimated end time — visible only when running (focus or break)
            if (state.isRunning && (state.phase == Phase.FOCUS || state.phase == Phase.SHORT_BREAK)) {
                Text(
                    text = "Fin estimée : ${computeEndTime(state)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    color = TextWhite
                )
            }

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

            // Early completion — visible only when running in FOCUS phase
            if (state.isRunning && state.phase == Phase.FOCUS) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = viewModel::completeEarly,
                    border = BorderStroke(1.dp, TextWhite.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite)
                ) {
                    Text("✓ Valider ce pomodoro")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatTime(seconds: Int): String =
    "%02d:%02d".format(seconds / 60, seconds % 60)

/**
 * Estimates the wall-clock time when all remaining Pomodoros will be done.
 *
 * FOCUS phase  : remainingSeconds covers the current pomo; (pomosLeft-1) full pomo+break cycles follow.
 * BREAK phase  : remainingSeconds covers the current break; pomosLeft full pomo cycles follow,
 *                with (pomosLeft-1) breaks between them.
 */
private fun computeEndTime(state: com.agenticfocus.viewmodel.PomodoroState): String {
    val pomoDuration = Phase.FOCUS.durationSeconds
    val breakDuration = Phase.SHORT_BREAK.durationSeconds
    val pomosLeft = (state.plannedPomodoros - state.completedPomodoros).coerceAtLeast(0)

    val totalSeconds = if (state.phase == Phase.FOCUS) {
        // current pomo is included in pomosLeft
        state.remainingSeconds + maxOf(0, pomosLeft - 1) * (pomoDuration + breakDuration)
    } else {
        // SHORT_BREAK: break is running, pomosLeft full pomos remain after the break
        state.remainingSeconds + pomosLeft * pomoDuration + maxOf(0, pomosLeft - 1) * breakDuration
    }

    val endMs = System.currentTimeMillis() + totalSeconds * 1000L
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = endMs }
    val h = cal.get(java.util.Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val m = cal.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
    return "${h}h${m}"
}
