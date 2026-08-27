package com.agenticfocus

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.agenticfocus.data.db.AppDatabase
import com.agenticfocus.data.supabase.SupabaseClientProvider
import com.agenticfocus.data.sync.RealtimeSyncManager
import com.agenticfocus.data.sync.SyncEngine
import com.agenticfocus.data.sync.SyncStatusManager
import com.agenticfocus.worker.DailyReflectionReminderWorker
import com.agenticfocus.worker.ReflectionReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AgenticFocusApp : Application() {

    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        // Heavy initialization (Supabase client, Room DB) runs on IO to avoid blocking
        // the main thread, which would show a black window background for 3–5 seconds.
        // Story 31-3 — synchrone et avant tout le reste : restoreSession() consulte
        // StandaloneMode dès le premier frame, il ne peut pas attendre la coroutine IO.
        // Opération trivial (une affectation), aucun coût de démarrage.
        com.agenticfocus.data.auth.StandaloneMode.init(applicationContext)

        syncScope.launch {
            SupabaseClientProvider.initialize(applicationContext)
            val db = AppDatabase.getInstance(applicationContext)
            SyncEngine.initialize(applicationContext, db.syncQueueDao())
            RealtimeSyncManager.initialize(db)
        }

        registerNetworkCallback()

        // Story 24-6 Sprint 22 Epic 24 — Daily reflection notification
        createDailyReflectionNotificationChannel()
        ReflectionReminderScheduler.scheduleFromPrefs(applicationContext)
    }

    private fun createDailyReflectionNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DailyReflectionReminderWorker.CHANNEL_ID,
                "Bilan du jour",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Rappel quotidien pour écrire votre bilan du jour (Day Facts + Learning)"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun registerNetworkCallback() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                SyncStatusManager.setSyncing()
                syncScope.launch { SyncEngine.flush() }
            }
            override fun onLost(network: Network) {
                SyncStatusManager.setOffline()
            }
        })
    }
}
