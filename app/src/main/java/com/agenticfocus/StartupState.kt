package com.agenticfocus

/**
 * Tracks whether the initial auth check (restoreSession) has completed.
 * Read from the SplashScreen keep-on-screen condition to delay splash exit
 * until the app has determined auth state, giving JIT time to warm up while
 * the system-rendered splash is visible.
 */
object StartupState {
    @Volatile var isAuthChecked: Boolean = false
}
