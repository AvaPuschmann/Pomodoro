package com.agenticfocus.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.agenticfocus.data.auth.AuthRepository
import com.agenticfocus.data.auth.SupabaseAuthRepository
import com.agenticfocus.data.auth.UserInfo
import com.agenticfocus.data.sync.RealtimeSyncManager
import com.agenticfocus.data.sync.SyncEngine
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val userId: String, val email: String) : AuthState()
    object Unauthenticated : AuthState()
}

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Called on app start to restore a previously stored session. */
    fun restoreSession() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val user: UserInfo? = authRepository.restoreSession()
                if (user != null) {
                    SyncEngine.currentUserId = user.userId
                    RealtimeSyncManager.startSync(user.userId)
                    _authState.value = AuthState.Authenticated(user.userId, user.email)
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            } catch (_: Exception) {
                _authState.value = AuthState.Unauthenticated
            } finally {
                com.agenticfocus.StartupState.isAuthChecked = true
            }
        }
        startSessionMonitor()
    }

    private var monitorStarted = false

    private fun startSessionMonitor() {
        if (monitorStarted) return
        val repo = authRepository
        if (repo !is SupabaseAuthRepository) return
        monitorStarted = true
        viewModelScope.launch {
            repo.sessionStatusFlow().collect { status ->
                Log.d(TAG, "[auth] sessionStatus → $status")
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val user = UserInfo(
                            userId = status.session.user?.id ?: return@collect,
                            email = status.session.user?.email ?: ""
                        )
                        SyncEngine.currentUserId = user.userId
                        if (_authState.value !is AuthState.Authenticated) {
                            RealtimeSyncManager.startSync(user.userId)
                        }
                        _authState.value = AuthState.Authenticated(user.userId, user.email)
                    }
                    is SessionStatus.NotAuthenticated -> {
                        if (_authState.value is AuthState.Authenticated) {
                            Log.w(TAG, "Session lost — signing out")
                            SyncEngine.currentUserId = ""
                            RealtimeSyncManager.stopSync()
                            _authState.value = AuthState.Unauthenticated
                        }
                    }
                    is SessionStatus.LoadingFromStorage -> { /* wait */ }
                    is SessionStatus.NetworkError -> {
                        Log.w(TAG, "Network error during auth — staying authenticated (offline mode)")
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }

    /** Sign in with email and password. Updates [authState] and [errorMessage] accordingly. */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _errorMessage.value = null
            authRepository.signIn(email, password).fold(
                onSuccess = { user ->
                    SyncEngine.currentUserId = user.userId
                    RealtimeSyncManager.startSync(user.userId)
                    _authState.value = AuthState.Authenticated(user.userId, user.email)
                },
                onFailure = { e ->
                    _authState.value = AuthState.Unauthenticated
                    _errorMessage.value = mapErrorToFrench(e)
                }
            )
        }
    }

    /** Create a new account with email and password. */
    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _errorMessage.value = null
            authRepository.signUp(email, password).fold(
                onSuccess = { user ->
                    SyncEngine.currentUserId = user.userId
                    RealtimeSyncManager.startSync(user.userId)
                    _authState.value = AuthState.Authenticated(user.userId, user.email)
                },
                onFailure = { e ->
                    _authState.value = AuthState.Unauthenticated
                    _errorMessage.value = mapErrorToFrench(e)
                }
            )
        }
    }

    /** Sign out and clear stored session. */
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            SyncEngine.currentUserId = ""
            RealtimeSyncManager.stopSync()
            _authState.value = AuthState.Unauthenticated
            _errorMessage.value = null
        }
    }

    private fun mapErrorToFrench(e: Throwable): String = when {
        e.message == "CONFIRMATION_REQUIRED" ->
            "Compte créé ! Vérifiez votre email pour confirmer votre inscription."
        e.message?.contains("already registered", ignoreCase = true) == true ||
        e.message?.contains("already exists", ignoreCase = true) == true ||
        e.message?.contains("User already", ignoreCase = true) == true ->
            "Un compte existe déjà avec cet email."
        e.message?.contains("Invalid login credentials", ignoreCase = true) == true ||
        e.message?.contains("invalid_credentials", ignoreCase = true) == true ||
        e.message?.contains("400", ignoreCase = true) == true ->
            "Email ou mot de passe incorrect."
        e.message?.contains("Password should be", ignoreCase = true) == true ||
        e.message?.contains("weak_password", ignoreCase = true) == true ->
            "Mot de passe trop faible (6 caractères minimum)."
        e.message?.contains("expired", ignoreCase = true) == true ->
            "Session expirée. Veuillez vous reconnecter."
        e.message?.contains("network", ignoreCase = true) == true ||
        e.message?.contains("connect", ignoreCase = true) == true ->
            "Impossible de se connecter. Vérifiez votre connexion."
        else -> "Une erreur est survenue : ${e.message}"
    }
}

class AuthViewModelFactory(
    private val repository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AuthViewModel(repository) as T
    }
}
