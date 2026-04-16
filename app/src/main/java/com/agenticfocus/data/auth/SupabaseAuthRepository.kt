package com.agenticfocus.data.auth

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.Flow

/**
 * Real implementation of [AuthRepository] backed by Supabase GoTrue (auth).
 */
class SupabaseAuthRepository(
    private val supabase: SupabaseClient
) : AuthRepository {

    override suspend fun signUp(email: String, password: String): Result<UserInfo> {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val user = supabase.auth.currentUserOrNull()
            if (user != null) {
                Result.success(UserInfo(userId = user.id, email = user.email ?: ""))
            } else {
                // Supabase requires email confirmation — sign up succeeded but no immediate session
                Result.failure(Exception("CONFIRMATION_REQUIRED"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<UserInfo> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val user = supabase.auth.currentUserOrNull()
                ?: return Result.failure(Exception("Session invalide après connexion"))
            Result.success(UserInfo(userId = user.id, email = user.email ?: ""))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreSession(): UserInfo? {
        return try {
            supabase.auth.refreshCurrentSession()
            Log.d(TAG, "Session refreshed successfully")
            val user = supabase.auth.currentUserOrNull() ?: return null
            UserInfo(userId = user.id, email = user.email ?: "")
        } catch (e: Exception) {
            Log.w(TAG, "Refresh failed (${e.message}), trying stored session")
            // Network failure — fall back to stored session (offline mode).
            // The access token may be expired but the user stays authenticated
            // locally. supabase-kt will auto-refresh when connectivity returns.
            val user = supabase.auth.currentUserOrNull() ?: return null
            UserInfo(userId = user.id, email = user.email ?: "")
        }
    }

    fun sessionStatusFlow(): Flow<SessionStatus> = supabase.auth.sessionStatus

    companion object {
        private const val TAG = "SupabaseAuth"
    }

    override suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } catch (_: Exception) {
            // Best-effort: even if network fails, session is cleared locally via SessionManager
        }
    }
}
