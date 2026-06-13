package com.agenticfocus.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.agenticfocus.MainActivity
import com.agenticfocus.R
import com.agenticfocus.data.AppPreferences

/**
 * Story 24-6 Sprint 22 Epic 24 — Daily reflection reminder notification.
 *
 * Triggered by [ReflectionReminderScheduler] (PeriodicWorkRequest 24h).
 *
 * Behavior :
 * - Checks user opt-out (AppPreferences.reflectionNotifEnabled) — silent return if disabled
 * - Builds and displays a notification with title + text
 * - Tap → Intent vers MainActivity with extra EXTRA_OPEN_DAILY_REFLECTION = true
 *   → MainActivity onCreate / onNewIntent ouvre DailyReflectionScreen sur today
 *
 * Channel : created at app startup in AgenticFocusApp.onCreate() (IMPORTANCE_DEFAULT — son par défaut, pas urgent).
 */
class DailyReflectionReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext

        // Respect user opt-out (Settings switch — Story 24-6 Bloc C)
        val prefs = AppPreferences(context)
        if (!prefs.reflectionNotifEnabled) {
            return Result.success()  // silent no-op, no notification posted
        }

        // Build PendingIntent → MainActivity with deep link extra
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_DAILY_REFLECTION, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Build and post the notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("📔 Comment s'est passée ta journée ?")
            .setContentText("Day Facts • Learning")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission refused (Android 13+) — fail gracefully
            return Result.success()
        }

        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "reflection_reminder"
        const val NOTIF_ID = 22_001  // unique vs TimerService notification (which uses a different ID)
        const val EXTRA_OPEN_DAILY_REFLECTION = "open_daily_reflection"
        private const val REQUEST_CODE = 24_006
    }
}
