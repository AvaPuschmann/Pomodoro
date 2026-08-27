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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.agenticfocus.data.supabase.SupabaseClientProvider
import com.agenticfocus.data.supabase.dto.ActivePomodoroDto
import com.agenticfocus.data.supabase.dto.ActivePomodoroReadDto
import com.agenticfocus.data.sync.RealtimeSyncManager
import com.agenticfocus.ui.components.SessionButtons
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
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

    // Epic 30 — découverte du pomodoro distant (desktop → mobile) pour afficher le bandeau.
    //
    // 2026-08-06 : poll porté de 5 s à 30 s, et suspendu pendant que le timer local tourne.
    // `active_pomodoro` ne contient qu'UNE ligne par user (PK user_id) : dès que le timer
    // mobile démarre, TimerService y écrit platform='mobile' et écrase la ligne desktop.
    // Poller pendant ce temps ne peut donc renvoyer que notre propre ligne, filtrée plus bas.
    //
    // La latence de découverte n'affecte pas la précision : le chrono du bandeau est calculé
    // localement à partir de startedAt. `key(state.isRunning)` relance l'effet à l'arrêt du
    // timer local, donc le bandeau distant réapparaît sans attendre.
    var remoteDesktopSession by remember { mutableStateOf<ActivePomodoroDto?>(null) }
    LaunchedEffect(state.isRunning) {
        if (state.isRunning) {
            remoteDesktopSession = null
            return@LaunchedEffect
        }
        while (true) {
            // App en arrière-plan : ce LaunchedEffect survit à la mise en tâche de fond
            // (la composition est conservée), il pollait donc toute la nuit. Même garde
            // que le pull périodique — voir RealtimeSyncManager.isForeground.
            if (!RealtimeSyncManager.isForeground) {
                delay(30_000)
                continue
            }
            try {
                val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                if (userId != null) {
                    val rows = SupabaseClientProvider.client
                        .from("active_pomodoro")
                        .select { filter { eq("user_id", userId) } }
                        .decodeList<ActivePomodoroReadDto>()
                    val row = rows.firstOrNull()
                    remoteDesktopSession = if (row?.platform == "desktop" && row.taskId != null)
                        ActivePomodoroDto(
                            userId = row.userId,
                            taskId = row.taskId,
                            taskName = row.taskName,
                            platform = row.platform,
                            sessionType = row.sessionType ?: "work",
                            startedAt = row.startedAt,
                            plannedDurationMs = row.plannedDurationMs,
                            updatedAt = row.updatedAt ?: 0L,
                        ) else null
                }
            } catch (_: Exception) { /* non-critique */ }
            delay(30_000)
        }
    }

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

            // Epic 30 — desktop active pomodoro banner
            if (remoteDesktopSession != null) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .background(Color(0xFFFBB924).copy(alpha = 0.13f), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFFBB924).copy(alpha = 0.32f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🖥️", fontSize = 14.sp)
                    Text(
                        text = "Desktop",
                        color = Color(0xFFFBB924),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("·", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                    Text(
                        text = "🍅 ${remoteDesktopSession!!.taskName ?: "Pomodoro en cours"}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

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
