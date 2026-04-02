package com.agenticfocus.data.supabase

import android.content.Context
import com.agenticfocus.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton holder for the Supabase client.
 * [initialize] must be called from a background coroutine (see AgenticFocusApp).
 * Observe [isReady] before accessing [client].
 */
object SupabaseClientProvider {

    private var _client: SupabaseClient? = null

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    val client: SupabaseClient
        get() = _client ?: error("SupabaseClientProvider not initialized.")

    fun initialize(context: Context) {
        if (_client != null) {
            _isReady.value = true
            return
        }
        _client = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                sessionManager = EncryptedSessionManager(context.applicationContext)
                alwaysAutoRefresh = true
            }
            install(Postgrest)
            install(Realtime)
        }
        _isReady.value = true
    }
}
