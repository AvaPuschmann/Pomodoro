package com.agenticfocus.data.auth

/**
 * Contract for authentication operations.
 * Decouples AuthViewModel from Supabase internals — testable with FakeAuthRepository.
 */
interface AuthRepository {
    /** Sign in with email and password. Returns [UserInfo] on success. */
    suspend fun signIn(email: String, password: String): Result<UserInfo>

    /** Create a new account with email and password. Returns [UserInfo] on success. */
    suspend fun signUp(email: String, password: String): Result<UserInfo>

    /** Restore session from secure storage. Returns [UserInfo] if a valid session exists, null otherwise. */
    suspend fun restoreSession(): UserInfo?

    /** Sign out and clear stored session tokens. */
    suspend fun signOut()
}

data class UserInfo(val userId: String, val email: String)
