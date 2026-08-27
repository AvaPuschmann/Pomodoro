package com.agenticfocus.viewmodel

import app.cash.turbine.test
import com.agenticfocus.data.auth.AuthRepository
import com.agenticfocus.data.auth.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---------------------------------------------------------------------------
    // signIn — success
    // ---------------------------------------------------------------------------

    @Test
    fun `signIn success transitions state to Authenticated`() = runTest {
        val repo = FakeAuthRepository(shouldSucceed = true)
        val vm = AuthViewModel(repo)

        vm.authState.test {
            assertEquals(AuthState.Loading, awaitItem()) // initial Loading state

            vm.signIn("user@example.com", "secret")
            testDispatcher.scheduler.advanceUntilIdle()

            // StateFlow deduplicates: signIn sets Loading again (no new emission), then Authenticated
            val state = awaitItem()
            assertTrue(state is AuthState.Authenticated)
            assertEquals("user@example.com", (state as AuthState.Authenticated).email)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `signIn success clears errorMessage`() = runTest {
        val repo = FakeAuthRepository(shouldSucceed = true)
        val vm = AuthViewModel(repo)

        vm.signIn("user@example.com", "secret")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.errorMessage.value)
    }

    // ---------------------------------------------------------------------------
    // signIn — failure
    // ---------------------------------------------------------------------------

    @Test
    fun `signIn failure transitions state to Unauthenticated`() = runTest {
        val repo = FakeAuthRepository(shouldSucceed = false, errorMessage = "Invalid login credentials")
        val vm = AuthViewModel(repo)

        vm.signIn("user@example.com", "wrong")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthState.Unauthenticated, vm.authState.value)
    }

    @Test
    fun `signIn failure with invalid credentials shows French error`() = runTest {
        val repo = FakeAuthRepository(shouldSucceed = false, errorMessage = "Invalid login credentials")
        val vm = AuthViewModel(repo)

        vm.signIn("user@example.com", "wrong")
        testDispatcher.scheduler.advanceUntilIdle()

        // Story 31-3 — le message de production se termine par un point.
        assertEquals("Email ou mot de passe incorrect.", vm.errorMessage.value)
    }

    @Test
    fun `signIn failure with network error shows French error`() = runTest {
        val repo = FakeAuthRepository(shouldSucceed = false, errorMessage = "network error")
        val vm = AuthViewModel(repo)

        vm.signIn("user@example.com", "secret")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Impossible de se connecter. Vérifiez votre connexion.", vm.errorMessage.value)
    }

    // ---------------------------------------------------------------------------
    // restoreSession
    // ---------------------------------------------------------------------------

    @Test
    fun `restoreSession with stored user transitions to Authenticated without network call`() = runTest {
        val repo = FakeAuthRepository(storedUser = UserInfo("user-123", "cached@example.com"))
        val vm = AuthViewModel(repo)

        vm.restoreSession()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.authState.value
        assertTrue(state is AuthState.Authenticated)
        assertEquals("cached@example.com", (state as AuthState.Authenticated).email)
    }

    @Test
    fun `restoreSession with no stored session transitions to Unauthenticated`() = runTest {
        val repo = FakeAuthRepository(storedUser = null)
        val vm = AuthViewModel(repo)

        vm.restoreSession()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthState.Unauthenticated, vm.authState.value)
    }

    // ---------------------------------------------------------------------------
    // signOut
    // ---------------------------------------------------------------------------

    @Test
    fun `signOut transitions state to Unauthenticated and calls repository`() = runTest {
        val repo = FakeAuthRepository(
            shouldSucceed = true,
            storedUser = UserInfo("user-123", "user@example.com")
        )
        val vm = AuthViewModel(repo)

        // First sign in
        vm.signIn("user@example.com", "secret")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then sign out
        vm.signOut()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AuthState.Unauthenticated, vm.authState.value)
        assertTrue(repo.signOutCalled)
        assertNull(repo.storedUser)
    }
}

// ---------------------------------------------------------------------------
// Fake implementation — no mockk required
// ---------------------------------------------------------------------------

private class FakeAuthRepository(
    var shouldSucceed: Boolean = true,
    var errorMessage: String = "Invalid login credentials",
    var storedUser: UserInfo? = null,
    var signOutCalled: Boolean = false
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): Result<UserInfo> {
        return if (shouldSucceed) {
            val user = UserInfo(userId = "user-123", email = email)
            storedUser = user
            Result.success(user)
        } else {
            Result.failure(Exception(errorMessage))
        }
    }

    override suspend fun signUp(email: String, password: String): Result<UserInfo> {
        return if (shouldSucceed) {
            val user = UserInfo(userId = "user-123", email = email)
            storedUser = user
            Result.success(user)
        } else {
            Result.failure(Exception(errorMessage))
        }
    }

    override suspend fun restoreSession(): UserInfo? = storedUser

    override suspend fun signOut() {
        signOutCalled = true
        storedUser = null
    }
}
