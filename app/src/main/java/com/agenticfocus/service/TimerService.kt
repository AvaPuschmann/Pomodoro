package com.agenticfocus.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.agenticfocus.MainActivity
import com.agenticfocus.R
import com.agenticfocus.viewmodel.Phase
import com.agenticfocus.viewmodel.PomodoroState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TimerService : Service() {

    companion object {
        const val ACTION_START = "com.agenticfocus.ACTION_START"
        const val ACTION_PAUSE = "com.agenticfocus.ACTION_PAUSE"
        const val ACTION_RESET = "com.agenticfocus.ACTION_RESET"
        const val ACTION_UPDATE_TASK = "com.agenticfocus.ACTION_UPDATE_TASK"
        const val ACTION_UPDATE_PLANNED = "com.agenticfocus.UPDATE_PLANNED"
        const val EXTRA_PHASE = "extra_phase"
        const val EXTRA_TASK_NAME = "extra_task_name"
        const val EXTRA_PLANNED_DELTA = "planned_delta"   // Int: +1 or -1
        const val ACTION_ACTIVATE_TASK      = "com.agenticfocus.ACTIVATE_TASK"
        const val EXTRA_TASK_ID             = "extra_task_id"
        const val EXTRA_PLANNED_POMODOROS   = "extra_planned_pomodoros"
        // Note: EXTRA_TASK_NAME already declared above at line 38 — do NOT redeclare

        private const val NOTIFICATION_ID = 1001
        private const val TIMER_CHANNEL = "timer_channel"
        private const val TAG = "TimerService"

        /** Maps remaining-time thresholds (seconds) to their res/raw sound file name. */
        private val ALERT_SOUNDS = mapOf(
            600 to "tenminutes",
            300 to "fiveminutes",
            180 to "threeminutes",
            60 to "oneminute"
        )

        private val _timerState = MutableStateFlow(
            PomodoroState(
                phase = Phase.FOCUS,
                totalSeconds = Phase.FOCUS.durationSeconds,
                remainingSeconds = Phase.FOCUS.durationSeconds,
                isRunning = false,
                taskName = "",
                completedPomodoros = 0
            )
        )
        val timerState = _timerState.asStateFlow()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null
    private var startedAt: Long = 0L
    private var pausedRemaining: Int = 0
    // F4 fix: track last second notified to avoid 4x/sec notification updates
    private var lastNotifiedSecond: Int = -1

    /** Thresholds already triggered this session — prevents double-firing. */
    private val triggeredAlerts = mutableSetOf<Int>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart()
            ACTION_PAUSE -> handlePause()
            ACTION_RESET -> {
                val phaseName = intent.getStringExtra(EXTRA_PHASE)
                val phase = phaseName?.let { Phase.valueOf(it) } ?: Phase.FOCUS
                handleReset(phase)
            }
            ACTION_UPDATE_TASK -> {
                val name = intent.getStringExtra(EXTRA_TASK_NAME) ?: ""
                // isEmpty() only — whitespace-only strings do NOT trigger reset
                _timerState.value = _timerState.value.copy(
                    taskName = name,
                    completedPomodoros = if (name.isEmpty()) 0 else _timerState.value.completedPomodoros
                )
                updateNotification()
            }
            ACTION_UPDATE_PLANNED -> {
                val delta = intent.getIntExtra(EXTRA_PLANNED_DELTA, 0)
                val current = _timerState.value.plannedPomodoros
                val minPlanned = _timerState.value.completedPomodoros.coerceAtLeast(1)
                _timerState.value = _timerState.value.copy(
                    plannedPomodoros = (current + delta).coerceIn(minPlanned, 6)
                )
                // updateNotification() not called — notification does not display plannedPomodoros
            }
            ACTION_ACTIVATE_TASK -> {
                val taskId  = intent.getStringExtra(EXTRA_TASK_ID) ?: ""
                val name    = intent.getStringExtra(EXTRA_TASK_NAME) ?: ""
                val planned = intent.getIntExtra(EXTRA_PLANNED_POMODOROS, 1).coerceAtLeast(1)
                tickJob?.cancel()
                tickJob = null
                pausedRemaining = 0
                lastNotifiedSecond = -1
                triggeredAlerts.clear()
                _timerState.value = _timerState.value.copy(
                    taskName           = name,
                    activeTaskId       = taskId,
                    completedPomodoros = 0,
                    plannedPomodoros   = planned,
                    phase              = Phase.FOCUS,
                    totalSeconds       = Phase.FOCUS.durationSeconds,
                    remainingSeconds   = Phase.FOCUS.durationSeconds,
                    isRunning          = false
                )
                // stopForeground is a no-op if service was not in foreground (safe, API 31+)
                stopForeground(STOP_FOREGROUND_REMOVE)
                updateNotification()
            }
        }
        return START_STICKY
    }

    private fun handleStart() {
        val state = _timerState.value
        startForeground(NOTIFICATION_ID, buildNotification())

        // If resuming from pause, use saved remaining; otherwise use state remaining
        val isResumingFromPause = pausedRemaining > 0
        val totalSecs = state.totalSeconds
        val remainingSecs = if (isResumingFromPause) pausedRemaining else state.remainingSeconds
        pausedRemaining = 0
        lastNotifiedSecond = -1
        if (!isResumingFromPause) {
            triggeredAlerts.clear()
            Log.d(TAG, "handleStart: fresh start — triggeredAlerts cleared")
        } else {
            Log.d(TAG, "handleStart: resuming from pause — triggeredAlerts preserved: $triggeredAlerts")
        }

        startedAt = System.currentTimeMillis() - ((totalSecs - remainingSecs) * 1000L)
        _timerState.value = state.copy(isRunning = true)

        tickJob?.cancel()
        tickJob = serviceScope.launch {
            while (true) {
                delay(250)
                val elapsed = ((System.currentTimeMillis() - startedAt) / 1000).toInt()
                val remaining = (totalSecs - elapsed).coerceAtLeast(0)

                _timerState.value = _timerState.value.copy(remainingSeconds = remaining)

                // Milestone alerts — FOCUS phase only, once per threshold per session
                val currentPhase = _timerState.value.phase
                if (currentPhase == Phase.FOCUS) {
                    for ((threshold, soundName) in ALERT_SOUNDS) {
                        if (remaining <= threshold && threshold !in triggeredAlerts) {
                            Log.d(TAG, "MILESTONE: threshold=$threshold remaining=$remaining → playing $soundName")
                            triggeredAlerts.add(threshold)
                            playMilestoneAlert(soundName)
                        }
                    }
                } else {
                    // Only log once per second to avoid spam
                    if (remaining != lastNotifiedSecond) {
                        Log.d(TAG, "MILESTONE: skipped — phase=$currentPhase remaining=$remaining")
                    }
                }

                // F4 fix: only notify when the displayed second actually changes
                if (remaining != lastNotifiedSecond) {
                    lastNotifiedSecond = remaining
                    updateNotification()
                }

                if (remaining == 0) {
                    onSessionComplete()
                    break
                }
            }
        }
    }

    private fun handlePause() {
        tickJob?.cancel()
        tickJob = null
        pausedRemaining = _timerState.value.remainingSeconds
        _timerState.value = _timerState.value.copy(isRunning = false)
        // Keep foreground for simplicity while paused
        updateNotification()
    }

    private fun handleReset(phase: Phase) {
        tickJob?.cancel()
        tickJob = null
        pausedRemaining = 0
        lastNotifiedSecond = -1
        triggeredAlerts.clear()
        _timerState.value = _timerState.value.copy(
            phase = phase,
            totalSeconds = phase.durationSeconds,
            remainingSeconds = phase.durationSeconds,
            isRunning = false
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    // F2 fix: sound and vibration dispatched on Main thread for OEM compatibility
    private suspend fun onSessionComplete() {
        val currentState = _timerState.value
        // Capture context reference outside withContext to avoid lambda receiver ambiguity
        val ctx = this

        withContext(Dispatchers.Main) {
            // Play sound
            try {
                val resId: Int = R.raw.timer_end
                MediaPlayer.create(ctx, resId)?.also { mp ->
                    mp.start()
                    mp.setOnCompletionListener { player -> player.release() }
                }
            } catch (e: Exception) {
                Log.e(TAG, "MediaPlayer failed: ${e.message}")
            }

            // Vibrate
            try {
                val vibrator = ctx.getSystemService(Vibrator::class.java)
                vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } catch (e: Exception) {
                Log.e(TAG, "Vibration failed: ${e.message}")
            }
        }

        // Increment counter and auto-advance phase
        val newCount = currentState.completedPomodoros + 1
        val nextPhase = if (currentState.phase == Phase.FOCUS) {
            if (newCount % 4 == 0) Phase.LONG_BREAK else Phase.SHORT_BREAK
        } else {
            Phase.FOCUS
        }

        // Play pause announcement when transitioning to short break (5 min)
        if (nextPhase == Phase.SHORT_BREAK) {
            withContext(Dispatchers.Main) {
                try {
                    MediaPlayer.create(ctx, R.raw.pause)?.also { mp ->
                        mp.start()
                        mp.setOnCompletionListener { player -> player.release() }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "MediaPlayer pause failed: ${e.message}")
                }
            }
        }

        // Play restart sound when short break (5 min) ends — LONG_BREAK excluded intentionally
        if (currentState.phase == Phase.SHORT_BREAK) {
            withContext(Dispatchers.Main) {
                try {
                    val mp = MediaPlayer.create(ctx, R.raw.restart)
                    if (mp == null) {
                        Log.e(TAG, "MediaPlayer restart: create() returned null")
                    } else {
                        mp.start()
                        mp.setOnCompletionListener { player -> player.release() }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "MediaPlayer restart failed: ${e.message}")
                }
            }
        }

        _timerState.value = currentState.copy(
            phase = nextPhase,
            totalSeconds = nextPhase.durationSeconds,
            remainingSeconds = nextPhase.durationSeconds,
            isRunning = false,
            completedPomodoros = if (currentState.phase == Phase.FOCUS) newCount else currentState.completedPomodoros
        )
        updateNotification()
    }

    /** Non-blocking milestone alert — resolves sound file by name from res/raw/. */
    private fun playMilestoneAlert(soundName: String) {
        Log.d(TAG, "playMilestoneAlert: launching $soundName on Main thread")
        serviceScope.launch(Dispatchers.Main) {
            try {
                val resId = resources.getIdentifier(soundName, "raw", packageName)
                if (resId == 0) {
                    Log.e(TAG, "playMilestoneAlert: resource not found — $soundName")
                    return@launch
                }
                val mp = MediaPlayer.create(this@TimerService, resId)
                if (mp == null) {
                    Log.e(TAG, "playMilestoneAlert: MediaPlayer.create() returned NULL for $soundName")
                } else {
                    Log.d(TAG, "playMilestoneAlert: playing $soundName")
                    mp.start()
                    mp.setOnCompletionListener {
                        Log.d(TAG, "playMilestoneAlert: $soundName complete, releasing")
                        it.release()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "playMilestoneAlert: EXCEPTION — ${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            TIMER_CHANNEL,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): android.app.Notification {
        val state = _timerState.value
        val timeText = formatTime(state.remainingSeconds)
        val phaseText = when (state.phase) {
            Phase.FOCUS -> "Focus"
            Phase.SHORT_BREAK -> "Short Break"
            Phase.LONG_BREAK -> "Long Break"
        }
        val contentText = if (state.taskName.isNotBlank()) {
            "$phaseText — ${state.taskName}"
        } else {
            phaseText
        }

        val tapIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, TIMER_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(timeText)
            .setContentText(contentText)
            .setContentIntent(tapIntent)
            .setOngoing(state.isRunning)
            .setSilent(true)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun formatTime(seconds: Int): String =
        "%02d:%02d".format(seconds / 60, seconds % 60)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
