package com.agenticfocus.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email

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
            // Explicitly refresh the stored session so supabase-kt reloads it from
            // EncryptedSessionManager and renews the access token if expired (1h lifetime).
            // Without this call, currentUserOrNull() returns null after a cold start
            // when the access token has expired, forcing the user to re-enter credentials.
            supabase.auth.refreshCurrentSession()
            val user = supabase.auth.currentUserOrNull() ?: return null
            UserInfo(userId = user.id, email = user.email ?: "")
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } catch (_: Exception) {
            // Best-effort: even if network fails, session is cleared locally via SessionManager
        }
    }
}
