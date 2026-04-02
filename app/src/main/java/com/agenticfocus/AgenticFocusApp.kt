package com.agenticfocus

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.agenticfocus.data.db.AppDatabase
import com.agenticfocus.data.supabase.SupabaseClientProvider
import com.agenticfocus.data.sync.RealtimeSyncManager
import com.agenticfocus.data.sync.SyncEngine
import com.agenticfocus.data.sync.SyncStatusManager
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
        syncScope.launch {
            SupabaseClientProvider.initialize(applicationContext)
            val db = AppDatabase.getInstance(applicationContext)
            SyncEngine.initialize(applicationContext, db.syncQueueDao())
            RealtimeSyncManager.initialize(db)
        }

        registerNetworkCallback()
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
