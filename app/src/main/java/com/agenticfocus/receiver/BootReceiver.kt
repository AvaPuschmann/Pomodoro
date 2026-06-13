package com.agenticfocus.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.agenticfocus.worker.ReflectionReminderScheduler

/**
 * Story 24-6 Sprint 22 Epic 24 — Re-schedule daily reflection reminder after device reboot.
 *
 * WorkManager scheduled work is **lost on reboot** (Android limitation). This receiver listens to
 * `android.intent.action.BOOT_COMPLETED` (requires permission RECEIVE_BOOT_COMPLETED in manifest)
 * and re-reads user prefs to schedule the reminder again.
 *
 * Idempotent : if user has reflectionNotifEnabled=false, scheduleFromPrefs cancels instead of scheduling.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReflectionReminderScheduler.scheduleFromPrefs(context.applicationContext)
        }
    }
}
