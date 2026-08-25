package com.komod.api.data.billing

import com.komod.api.data.repository.AuthRepository
import com.komod.api.domain.model.User
import com.revenuecat.purchases.kmp.ktx.SuccessfulLogin
import com.revenuecat.purchases.kmp.ktx.SuccessfulPurchase
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.EntitlementInfos
import com.revenuecat.purchases.kmp.models.Offerings
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.VerificationResult
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeAuthRepository : AuthRepository {
    override val sessionStatus: StateFlow<SessionStatus> = MutableStateFlow(SessionStatus.NotAuthenticated())
    private val _callbackError = MutableSharedFlow<Throwable>()
    override val callbackError: SharedFlow<Throwable> = _callbackError.asSharedFlow()
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun setUser(user: User?) {
        _currentUser.value = user
    }

    override suspend fun signInWithGoogle() = Unit
    override suspend fun signInWithApple() = Unit
    override suspend fun handleOAuthCallback(url: String) = Unit
    override fun currentUserOrNull(): User? = _currentUser.value
    override suspend fun signOut() = Unit
}

internal fun fakeCustomerInfo() = CustomerInfo(
    activeSubscriptions = emptySet(),
    allExpirationDateMillis = emptyMap(),
    allPurchaseDateMillis = emptyMap(),
    allPurchasedProductIdentifiers = emptySet(),
    entitlements = EntitlementInfos(all = emptyMap(), verification = VerificationResult.NOT_REQUESTED),
    firstSeenMillis = 0L,
    latestExpirationDateMillis = null,
    managementUrlString = null,
    subscriptionsByProductIdentifier = emptyMap(),
    nonSubscriptionTransactions = emptyList(),
    originalAppUserId = "test-user",
    originalApplicationVersion = null,
    originalPurchaseDateMillis = null,
    requestDateMillis = 0L,
)

private class FakePurchasesService : PurchasesService {
    override var isConfigured: Boolean = true

    val logInCalls = mutableListOf<String>()
    var logInGate: CompletableDeferred<Unit>? = null
    var logInResult: Result<Unit> = Result.success(Unit)

    var logOutCallCount = 0
        private set

    // Records "logIn:<id>" / "logOut" in call order so tests can assert ordering between
    // identify and reset calls, not just each in isolation.
    val callOrder = mutableListOf<String>()

    override suspend fun logIn(supabaseUserId: String): SuccessfulLogin {
        logInCalls.add(supabaseUserId)
        callOrder.add("logIn:$supabaseUserId")
        logInGate?.await()
        logInResult.getOrThrow()
        return SuccessfulLogin(customerInfo = fakeCustomerInfo(), created = false)
    }

    override suspend fun logOut(): CustomerInfo {
        logOutCallCount++
        callOrder.add("logOut")
        return fakeCustomerInfo()
    }

    override suspend fun getOfferings(): Offerings = throw NotImplementedError()
    override suspend fun purchase(rcPackage: Package): SuccessfulPurchase = throw NotImplementedError()
    override suspend fun restorePurchases(): CustomerInfo = throw NotImplementedError()
    override suspend fun getCustomerInfo(): CustomerInfo = fakeCustomerInfo()
}

private fun testUser(id: String) = User(id = id, email = "$id@example.com", displayName = "Test", photoUrl = null)

// See RevenueCatIdentitySync's own doc comment on why UnconfinedTestDispatcher (not
// backgroundScope's default StandardTestDispatcher) is required here: advanceUntilIdle()
// deliberately excludes backgroundScope coroutines from what it considers "idle work", so a
// collector launched on it is never driven by advanceUntilIdle() alone. Unconfined also makes
// the collector start eagerly, matching how it behaves in production (no artificial delay).
private fun TestScope.startIdentitySync(
    authRepository: AuthRepository,
    purchasesService: PurchasesService,
): RevenueCatIdentitySync = RevenueCatIdentitySync(
    authRepository = authRepository,
    purchasesService = purchasesService,
    scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
)

class RevenueCatIdentitySyncTest {

    // 5. A successful login transitions identity to Ready with the exact Supabase UUID.
    @Test
    fun `a successful login transitions identity to Ready with the exact Supabase UUID`() = runTest {
        val authRepository = FakeAuthRepository()
        val purchasesService = FakePurchasesService()
        val sync = startIdentitySync(authRepository, purchasesService)

        authRepository.setUser(testUser(id = "7f123456-aaaa-bbbb-cccc-000000000000"))

        val state = assertIs<RevenueCatIdentityState.Ready>(sync.identityState.value)
        assertEquals("7f123456-aaaa-bbbb-cccc-000000000000", state.appUserId)
        assertEquals(listOf("7f123456-aaaa-bbbb-cccc-000000000000"), purchasesService.logInCalls)
    }

    // 6. A login that fails on every attempt leaves identity Failed — never a fabricated Ready.
    @Test
    fun `a login that fails on every attempt leaves identity Failed rather than Ready`() = runTest {
        val authRepository = FakeAuthRepository()
        val purchasesService = FakePurchasesService()
        purchasesService.logInResult = Result.failure(RuntimeException("network error"))
        val sync = startIdentitySync(authRepository, purchasesService)

        authRepository.setUser(testUser(id = "user-1"))
        // The retry loop's own delay() calls between attempts need virtual time to actually
        // advance — nothing else in this test does that. This isn't polling: it deterministically
        // fast-forwards the clock past the two 1s backoffs so the (already-scheduled) retries run.
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()

        assertIs<RevenueCatIdentityState.Failed>(sync.identityState.value)
        // The existing 3-attempt retry, preserved.
        assertEquals(3, purchasesService.logInCalls.size)
    }

    // 7. retry() eventually reaches Ready once the underlying call starts succeeding — this is
    // also what awaitReadyForCurrentUser() triggers automatically when it finds a Failed state.
    @Test
    fun `retry eventually reaches Ready once the identify call starts succeeding`() = runTest {
        val authRepository = FakeAuthRepository()
        val purchasesService = FakePurchasesService()
        purchasesService.logInResult = Result.failure(RuntimeException("network error"))
        val sync = startIdentitySync(authRepository, purchasesService)
        authRepository.setUser(testUser(id = "user-1"))
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()
        assertIs<RevenueCatIdentityState.Failed>(sync.identityState.value)

        purchasesService.logInResult = Result.success(Unit)
        sync.retry()

        val state = assertIs<RevenueCatIdentityState.Ready>(sync.identityState.value)
        assertEquals("user-1", state.appUserId)
    }

    // 8. Logout (reset) completes before another user's login is attempted — never overlapping.
    @Test
    fun `logout completes before another user's login is attempted`() = runTest {
        val authRepository = FakeAuthRepository()
        val purchasesService = FakePurchasesService()
        startIdentitySync(authRepository, purchasesService)

        authRepository.setUser(testUser(id = "user-a"))
        authRepository.setUser(null)
        authRepository.setUser(testUser(id = "user-b"))

        // The leading "logOut" is the sync started with (no authenticated user yet, at
        // construction) — a correct, harmless reset before user-a ever signs in.
        assertEquals(listOf("logOut", "logIn:user-a", "logOut", "logIn:user-b"), purchasesService.callOrder)
    }

    // User A -> logout -> User B: B is identified with exactly B's own id, never inheriting A's.
    @Test
    fun `user B is identified with their own id after user A logs out and never inherits A's identity`() = runTest {
        val authRepository = FakeAuthRepository()
        val purchasesService = FakePurchasesService()
        val sync = startIdentitySync(authRepository, purchasesService)

        authRepository.setUser(testUser(id = "user-a"))
        authRepository.setUser(null)
        authRepository.setUser(testUser(id = "user-b"))

        assertEquals(listOf("user-a", "user-b"), purchasesService.logInCalls)
        // One reset at construction (no user yet) plus the explicit user-a -> null transition.
        assertEquals(2, purchasesService.logOutCallCount)
        val state = assertIs<RevenueCatIdentityState.Ready>(sync.identityState.value)
        assertEquals("user-b", state.appUserId)
    }

    // 1. An authenticated user's purchase/restore/sync path genuinely *waits* for RevenueCat
    // login to complete — awaitReadyForCurrentUser() doesn't return early just because a user
    // is authenticated Supabase-side. No delay/poll is used to observe this: the gate below
    // only ever unblocks on the login call's own real completion.
    @Test
    fun `awaitReadyForCurrentUser suspends until the in-flight login actually completes`() = runTest {
        val authRepository = FakeAuthRepository()
        val purchasesService = FakePurchasesService()
        val loginGate = CompletableDeferred<Unit>()
        purchasesService.logInGate = loginGate
        val sync = startIdentitySync(authRepository, purchasesService)
        authRepository.setUser(testUser(id = "user-1"))

        // The login call is in flight (gated), so identity must not be Ready yet.
        assertIs<RevenueCatIdentityState.Syncing>(sync.identityState.value)

        var resolvedUserId: String? = null
        val awaiter = launch { resolvedUserId = sync.awaitReadyForCurrentUser() }
        runCurrent()
        assertTrue(awaiter.isActive)
        assertEquals(null, resolvedUserId)

        loginGate.complete(Unit)
        runCurrent()

        assertFalse(awaiter.isActive)
        assertEquals("user-1", resolvedUserId)
    }
}
