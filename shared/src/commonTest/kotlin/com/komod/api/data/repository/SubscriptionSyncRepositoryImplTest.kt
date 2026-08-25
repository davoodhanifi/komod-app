package com.komod.api.data.repository

import com.komod.api.data.api.SubscriptionApiService
import com.komod.api.data.billing.PurchasesService
import com.komod.api.data.billing.RevenueCatIdentitySync
import com.komod.api.data.billing.fakeCustomerInfo
import com.komod.api.domain.model.SubscriptionPlan
import com.komod.api.domain.model.SubscriptionStatus
import com.komod.api.domain.model.User
import com.revenuecat.purchases.kmp.ktx.SuccessfulLogin
import com.revenuecat.purchases.kmp.ktx.SuccessfulPurchase
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Offerings
import com.revenuecat.purchases.kmp.models.Package
import io.github.jan.supabase.auth.status.SessionStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

private val testJson = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

private class SyncFakeAuthRepository : AuthRepository {
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

private fun testUser(id: String) = User(id = id, email = "$id@example.com", displayName = "Test", photoUrl = null)

private class SyncFakePurchasesService : PurchasesService {
    override var isConfigured: Boolean = true

    var logInGate: CompletableDeferred<Unit>? = null
    val callOrder = mutableListOf<String>()

    override suspend fun logIn(supabaseUserId: String): SuccessfulLogin {
        callOrder.add("logIn:$supabaseUserId")
        logInGate?.await()
        return SuccessfulLogin(customerInfo = fakeCustomerInfo(), created = false)
    }

    override suspend fun logOut(): CustomerInfo {
        callOrder.add("logOut")
        return fakeCustomerInfo()
    }

    override suspend fun getOfferings(): Offerings = throw NotImplementedError()
    override suspend fun purchase(rcPackage: Package): SuccessfulPurchase = throw NotImplementedError()
    override suspend fun restorePurchases(): CustomerInfo = throw NotImplementedError()
    override suspend fun getCustomerInfo(): CustomerInfo = fakeCustomerInfo()
}

// A RevenueCatIdentitySync that's already Ready for [userId] by the time this returns — the
// user is set *before* construction, and (with the default, real-dispatcher scope) the identify
// call is a trivial in-memory fake, so awaitReadyForCurrentUser() inside sync() resolves near
// instantly via real suspension, not a delay. Used by the tests below that aren't specifically
// about identity gating.
private fun readyIdentitySync(userId: String = "test-user"): RevenueCatIdentitySync {
    val authRepository = SyncFakeAuthRepository()
    authRepository.setUser(testUser(userId))
    return RevenueCatIdentitySync(authRepository = authRepository, purchasesService = SyncFakePurchasesService())
}

private fun buildRepository(
    identitySync: RevenueCatIdentitySync = readyIdentitySync(),
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): SubscriptionSyncRepositoryImpl {
    val client = HttpClient(MockEngine { request -> handler(request) }) {
        install(ContentNegotiation) { json(testJson) }
        defaultRequest { url("https://test.local/api/v1/") }
    }
    return SubscriptionSyncRepositoryImpl(SubscriptionApiService(client), identitySync)
}

private fun respondSync(status: String, plan: String = "Komod3Doors", willRenew: Boolean = false) = """
    {"data":{"plan":"$plan","status":"$status","willRenew":$willRenew}}
""".trimIndent()

class SubscriptionSyncRepositoryImplTest {

    // 9. A cancelled subscription must keep showing the paid plan — mobile never downgrades
    // this locally; whatever plan the backend returns is what's shown, regardless of status.
    @Test
    fun `a cancelled-but-not-yet-expired sync keeps the paid plan rather than Rack`() = runBlocking {
        val repository = buildRepository {
            respond(
                content = respondSync(status = "Cancelled", plan = "Komod3Doors"),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val state = repository.sync()

        assertEquals(SubscriptionPlan.ThreeDoors, state.plan)
        assertEquals(SubscriptionStatus.Cancelled, state.status)
        assertEquals(state, repository.subscriptionState.value)
    }

    // 10. Once the backend itself reports Expired (and, per its contract, flips the plan to
    // KomodRack), mobile shows Rack immediately — again, straight passthrough, no local logic.
    @Test
    fun `an expired sync reflects KomodRack immediately`() = runBlocking {
        val repository = buildRepository {
            respond(
                content = respondSync(status = "Expired", plan = "KomodRack"),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val state = repository.sync()

        assertEquals(SubscriptionPlan.Rack, state.plan)
        assertEquals(SubscriptionStatus.Expired, state.status)
    }

    // 8. A failed sync must never fabricate/persist a plan — the exception propagates and the
    // shared subscriptionState is left exactly as it was (null here, since nothing succeeded
    // yet).
    @Test
    fun `a failed sync throws and never publishes a fabricated subscriptionState`() = runBlocking {
        val repository = buildRepository {
            respond(
                content = """{"type":"about:blank","title":"boom","status":500}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        assertFailsWith<ServerResponseException> { repository.sync() }
        assertNull(repository.subscriptionState.value)
    }

    // 4. Sync must not call POST /subscription/sync at all until RevenueCat identity is
    // confirmed Ready for the current Supabase user. Uses runBlocking (real dispatchers, like
    // the other tests above) rather than runTest's virtual time: MockEngine's request pipeline
    // doesn't hop through the TestCoroutineScheduler, so a real (bounded) wait via
    // withTimeoutOrNull is what actually proves "hasn't completed yet" here, not a polling loop.
    @Test
    fun `sync does not call the backend until identity is Ready`() = runBlocking {
        val authRepository = SyncFakeAuthRepository()
        val purchasesService = SyncFakePurchasesService()
        val loginGate = CompletableDeferred<Unit>()
        purchasesService.logInGate = loginGate
        val identitySync = RevenueCatIdentitySync(authRepository = authRepository, purchasesService = purchasesService)
        authRepository.setUser(testUser("user-1"))

        var backendCallCount = 0
        val repository = buildRepository(identitySync) {
            backendCallCount++
            respond(
                content = respondSync(status = "Active", plan = "KomodRack"),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val syncDeferred = async { repository.sync() }

        // The sync call must still be suspended (waiting on identity) after a real but short
        // window — it must not race ahead and call the backend regardless.
        val completedEarly = withTimeoutOrNull(300) { syncDeferred.await() }
        assertEquals(null, completedEarly)
        assertEquals(0, backendCallCount)

        loginGate.complete(Unit)
        val state = withTimeout(5_000) { syncDeferred.await() }

        assertEquals(SubscriptionPlan.Rack, state.plan)
        assertEquals(1, backendCallCount)
    }
}
