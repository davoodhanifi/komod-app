package com.komod.api.presentation.auth

import com.komod.api.data.auth.AppleSignInCancelledException
import com.komod.api.data.repository.AuthRepository
import com.komod.api.domain.model.User
import io.github.jan.supabase.auth.status.SessionStatus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

// Records calls and lets a test gate a sign-in call mid-flight (via [googleGate]/[appleGate])
// to observe the transient loading state, mirroring FakeAddItemRepository's style elsewhere
// in this codebase.
private class FakeAuthRepository : AuthRepository {
    override val sessionStatus: StateFlow<SessionStatus> = MutableStateFlow(SessionStatus.NotAuthenticated())
    private val _callbackError = MutableSharedFlow<Throwable>()
    override val callbackError: SharedFlow<Throwable> = _callbackError.asSharedFlow()
    override val currentUser: StateFlow<User?> = MutableStateFlow(null)

    var googleGate: CompletableDeferred<Unit>? = null
    var signInWithGoogleResult: Result<Unit> = Result.success(Unit)
    var signInWithGoogleCallCount = 0
        private set

    override suspend fun signInWithGoogle() {
        signInWithGoogleCallCount++
        googleGate?.await()
        signInWithGoogleResult.getOrThrow()
    }

    var appleGate: CompletableDeferred<Unit>? = null
    var signInWithAppleResult: Result<Unit> = Result.success(Unit)
    var signInWithAppleCallCount = 0
        private set

    override suspend fun signInWithApple() {
        signInWithAppleCallCount++
        appleGate?.await()
        signInWithAppleResult.getOrThrow()
    }

    override suspend fun handleOAuthCallback(url: String) = Unit

    override fun currentUserOrNull(): User? = null

    override suspend fun signOut() = Unit

    suspend fun emitCallbackError(error: Throwable) {
        _callbackError.emit(error)
    }
}

// Failure-path tests (a real signInWithGoogle/signInWithApple/callbackError failure surfacing
// uiState.error) are intentionally not included here: they'd exercise ErrorMapper.toUserMessage(),
// which throws via AppLogger.e() (unmocked android.util.Log) on this JVM test target — the same
// pre-existing limitation documented in AddItemViewModelTest/WardrobeViewModelPollingTest. What's
// covered instead: that a failure result reaches signInWithApple()/signInWithGoogle() at all
// (exercised via AppleSignInCancelledException below, which is caught before ever reaching
// ErrorMapper) and that the loading state always resolves one way or another.
class LoginViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `tapping Continue with Apple starts Apple sign-in exactly once`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = LoginViewModel(repository)

        viewModel.signInWithApple()

        assertEquals(1, repository.signInWithAppleCallCount)
        assertEquals(0, repository.signInWithGoogleCallCount)
    }

    @Test
    fun `Apple sign-in shows a loading state while in flight then clears on success`() = runTest {
        val repository = FakeAuthRepository()
        repository.appleGate = CompletableDeferred()
        val viewModel = LoginViewModel(repository)

        viewModel.signInWithApple()

        assertEquals(AuthProvider.Apple, viewModel.uiState.value.loadingProvider)
        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)

        repository.appleGate?.complete(Unit)

        assertEquals(LoginUiState(), viewModel.uiState.value)
    }

    @Test
    fun `Apple sign-in cancellation returns to idle without showing an error`() = runTest {
        val repository = FakeAuthRepository()
        repository.signInWithAppleResult = Result.failure(AppleSignInCancelledException())
        val viewModel = LoginViewModel(repository)

        viewModel.signInWithApple()

        assertEquals(LoginUiState(), viewModel.uiState.value)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `a second Apple tap while one is already in flight does not start another`() = runTest {
        val repository = FakeAuthRepository()
        repository.appleGate = CompletableDeferred()
        val viewModel = LoginViewModel(repository)

        viewModel.signInWithApple()
        viewModel.signInWithApple()
        viewModel.signInWithApple()

        assertEquals(1, repository.signInWithAppleCallCount)
    }

    @Test
    fun `tapping Apple while Google is already in flight does not also start Apple`() = runTest {
        val repository = FakeAuthRepository()
        repository.googleGate = CompletableDeferred()
        val viewModel = LoginViewModel(repository)

        viewModel.signInWithGoogle()
        viewModel.signInWithApple()

        assertEquals(1, repository.signInWithGoogleCallCount)
        assertEquals(0, repository.signInWithAppleCallCount)
        assertEquals(AuthProvider.Google, viewModel.uiState.value.loadingProvider)
    }

    @Test
    fun `Google sign-in still shows a loading state while in flight then clears on success`() = runTest {
        val repository = FakeAuthRepository()
        repository.googleGate = CompletableDeferred()
        val viewModel = LoginViewModel(repository)

        viewModel.signInWithGoogle()

        assertEquals(AuthProvider.Google, viewModel.uiState.value.loadingProvider)
        assertTrue(viewModel.uiState.value.isLoading)

        repository.googleGate?.complete(Unit)

        assertEquals(LoginUiState(), viewModel.uiState.value)
    }

    @Test
    fun `a second Google tap while one is already in flight does not start another`() = runTest {
        val repository = FakeAuthRepository()
        repository.googleGate = CompletableDeferred()
        val viewModel = LoginViewModel(repository)

        viewModel.signInWithGoogle()
        viewModel.signInWithGoogle()

        assertEquals(1, repository.signInWithGoogleCallCount)
    }

    @Test
    fun `resuming the screen while idle does not disturb the state`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = LoginViewModel(repository)

        viewModel.onScreenResumed()

        assertEquals(LoginUiState(), viewModel.uiState.value)
    }

    @Test
    fun `resuming the screen while a sign-in never completed clears the stuck loading state`() = runTest {
        val repository = FakeAuthRepository()
        repository.appleGate = CompletableDeferred() // never completed — simulates an abandoned browser/session
        val viewModel = LoginViewModel(repository)

        viewModel.signInWithApple()
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.onScreenResumed()

        assertEquals(LoginUiState(), viewModel.uiState.value)
    }
}
