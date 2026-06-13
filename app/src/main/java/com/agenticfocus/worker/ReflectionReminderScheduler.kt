package com.agenticfocus.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.agenticfocus.data.AppPreferences
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Story 24-6 Sprint 22 Epic 24 — Scheduler for daily reflection reminder.
 *
 * Wraps WorkManager [PeriodicWorkRequest] (24h period) for [DailyReflectionReminderWorker].
 * Initial delay computed to fire at the next user-configured (hour:minute).
 *
 * Called from :
 * - AgenticFocusApp.onCreate (re-schedule on app start, idempotent via UPDATE policy)
 * - BootReceiver.onReceive (re-schedule after device reboot — WorkManager schedule loss recovery)
 * - SettingsScreen toggle ON / heure changée (re-schedule with new params)
 *
 * Test helper [scheduleOneTimeDebug] available for BuildConfig.DEBUG builds — fires once after N minutes.
 */
object ReflectionReminderScheduler {

    private const val UNIQUE_WORK_NAME = "daily_reflection_reminder"
    private const val DEBUG_WORK_NAME = "daily_reflection_reminder_debug"

    /**
     * Schedule (or re-schedule) the daily periodic reminder at the given (hour, minute).
     * Uses ExistingPeriodicWorkPolicy.UPDATE — overrides any previously scheduled work.
     */
    fun schedule(context: Context, hour: Int, minute: Int) {
        val initialDelayMs = computeInitialDelayMillis(hour, minute)
        val request = PeriodicWorkRequestBuilder<DailyReflectionReminderWorker>(
            24, TimeUnit.HOURS,
        )
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .addTag(UNIQUE_WORK_NAME)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
    }

    /**
     * Cancel any scheduled daily reminder (called when user toggles OFF in Settings).
     */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    /**
     * Convenience : read user prefs and schedule accordingly (or cancel if disabled).
     * Called from AgenticFocusApp.onCreate + BootReceiver.
     */
    fun scheduleFromPrefs(context: Context) {
        val prefs = AppPreferences(context)
        if (prefs.reflectionNotifEnabled) {
            schedule(context, prefs.reflectionNotifHour, prefs.reflectionNotifMinute)
        } else {
            cancel(context)
        }
    }

    /**
     * BuildConfig.DEBUG ONLY — schedule a one-time test fire after [delayMinutes] minutes.
     * Used by SettingsScreen "🧪 Tester dans 1 min" button to validate the notification flow quickly.
     */
    fun scheduleOneTimeDebug(context: Context, delayMinutes: Long = 1L) {
        val request = OneTimeWorkRequestBuilder<DailyReflectionReminderWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .addTag(DEBUG_WORK_NAME)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                DEBUG_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
    }

    /**
     * Compute the milliseconds delay until the next occurrence of (hour, minute) in the local timezone.
     * If the target time has already passed today, schedule for the same time tomorrow.
     */
    private fun computeInitialDelayMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
