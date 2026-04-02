package com.agenticfocus.data.supabase

import android.content.Context
import android.content.SharedPreferences
import io.github.jan.supabase.gotrue.SessionManager
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persists the full Supabase UserSession as JSON in SharedPreferences (MODE_PRIVATE).
 *
 * Storing only the raw tokens (previous approach) lost fields like expiresAt,
 * causing supabase-kt to treat every restored session as expired and clear it.
 * Serialising the whole object preserves all fields the library needs.
 */
class EncryptedSessionManager(context: Context) : SessionManager {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("supabase_session_v2", Context.MODE_PRIVATE)

    override suspend fun saveSession(session: UserSession) {
        val json = Json.encodeToString(session)
        prefs.edit().putString(KEY_SESSION, json).apply()
    }

    override suspend fun loadSession(): UserSession? {
        val json = prefs.getString(KEY_SESSION, null) ?: return null
        return try {
            Json.decodeFromString<UserSession>(json)
        } catch (_: Exception) {
            prefs.edit().remove(KEY_SESSION).apply()
            null
        }
    }

    override suspend fun deleteSession() {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    companion object {
        private const val KEY_SESSION = "session"
    }
}
