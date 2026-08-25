package com.komod.api.data.repository

import com.komod.api.data.billing.PurchasesService
import com.komod.api.data.billing.RevenueCatIdentitySync
import com.komod.api.data.billing.fakeCustomerInfo
import com.komod.api.domain.model.User
import com.revenuecat.purchases.kmp.ktx.SuccessfulLogin
import com.revenuecat.purchases.kmp.ktx.SuccessfulPurchase
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Offerings
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PackageType
import com.revenuecat.purchases.kmp.models.PresentedOfferingContext
import com.revenuecat.purchases.kmp.models.Price
import com.revenuecat.purchases.kmp.models.ProductCategory
import com.revenuecat.purchases.kmp.models.ProductType
import com.revenuecat.purchases.kmp.models.PurchasingData
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreTransaction
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
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

private fun fakePackage(): Package = object : Package {
    override val identifier: String = "test_package"
    override val packageType: PackageType = PackageType.CUSTOM
    override val storeProduct: StoreProduct = object : StoreProduct {
        override val id: String = "test_product"
        override val type: ProductType = ProductType.SUBS
        override val category: ProductCategory? = null
        override val price: Price = Price(formatted = "$9.99", amountMicros = 9_990_000, currencyCode = "USD")
        override val title: String = "test"
        override val localizedDescription: String? = null
        override val period: com.revenuecat.purchases.kmp.models.Period? = null
        override val subscriptionOptions: com.revenuecat.purchases.kmp.models.SubscriptionOptions? = null
        override val defaultOption: com.revenuecat.purchases.kmp.models.SubscriptionOption? = null
        override val discounts: List<com.revenuecat.purchases.kmp.models.StoreProductDiscount> = emptyList()
        override val introductoryDiscount: com.revenuecat.purchases.kmp.models.StoreProductDiscount? = null
        override val purchasingData: PurchasingData = object : PurchasingData {
            override val productId: String = "test_product"
            override val productType: ProductType = ProductType.SUBS
        }
        override val presentedOfferingContext: PresentedOfferingContext? = null
        override val pricePerWeek: Price? = null
        override val pricePerMonth: Price? = null
        override val pricePerYear: Price? = null
    }
    override val presentedOfferingContext: PresentedOfferingContext =
        PresentedOfferingContext(offeringIdentifier = "default", placementIdentifier = null, targetingContext = null)
    override val webCheckoutUrl: String? = null
}

private class FakePurchasesService : PurchasesService {
    override var isConfigured: Boolean = true

    var logInGate: CompletableDeferred<Unit>? = null

    val callOrder = mutableListOf<String>()

    var purchaseCallCount = 0
        private set
    var restoreCallCount = 0
        private set

    override suspend fun logIn(supabaseUserId: String): SuccessfulLogin {
        callOrder.add("logIn:$supabaseUserId")
        logInGate?.await()
        return SuccessfulLogin(customerInfo = fakeCustomerInfo(), created = false)
    }

    override suspend fun logOut(): CustomerInfo {
        callOrder.add("logOut")
        return fakeCustomerInfo()
    }

    override suspend fun purchase(rcPackage: Package): SuccessfulPurchase {
        purchaseCallCount++
        callOrder.add("purchase")
        return SuccessfulPurchase(
            storeTransaction = StoreTransaction(transactionId = null, productIds = emptyList(), purchaseTime = 0L),
            customerInfo = fakeCustomerInfo(),
        )
    }

    override suspend fun restorePurchases(): CustomerInfo {
        restoreCallCount++
        callOrder.add("restore")
        return fakeCustomerInfo()
    }

    override suspend fun getOfferings(): Offerings = throw NotImplementedError()
    override suspend fun getCustomerInfo(): CustomerInfo = fakeCustomerInfo()
}

private fun testUser(id: String) = User(id = id, email = "$id@example.com", displayName = "Test", photoUrl = null)

// See RevenueCatIdentitySyncTest for why UnconfinedTestDispatcher(testScheduler) rather than
// backgroundScope's default dispatcher is required to drive this deterministically under
// advanceUntilIdle()/runCurrent().
private fun TestScope.newIdentitySync(
    authRepository: AuthRepository,
    purchasesService: PurchasesService,
): RevenueCatIdentitySync = RevenueCatIdentitySync(
    authRepository = authRepository,
    purchasesService = purchasesService,
    scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)),
)

class BillingRepositoryImplTest {

    // 2. Purchase must not call RevenueCat's purchase API until identity is confirmed Ready
    // for the current Supabase user.
    @Test
    fun `purchase does not call RevenueCat until identity is Ready`() = runTest {
        val authRepository = FakeAuthRepository()
        val purchasesService = FakePurchasesService()
        val loginGate = CompletableDeferred<Unit>()
        purchasesService.logInGate = loginGate
        val identitySync = newIdentitySync(authRepository, purchasesService)
        val billingRepository = BillingRepositoryImpl(purchasesService, identitySync)
        authRepository.setUser(testUser(id = "user-1"))

        var purchaseCompleted = false
        val job = launch {
            billingRepository.purchase(fakePackage())
            purchaseCompleted = true
        }
        runCurrent()

        assertEquals(0, purchasesService.purchaseCallCount)
        assertFalse(purchaseCompleted)

        loginGate.complete(Unit)
        runCurrent()

        assertEquals(1, purchasesService.purchaseCallCount)
        assertTrue(purchaseCompleted)
        // The leading "logOut" is the sync started with (no authenticated user yet, at
        // construction) — a correct, harmless reset before this test's user ever signs in.
        assertEquals(listOf("logOut", "logIn:user-1", "purchase"), purchasesService.callOrder)
        job.cancel()
    }

    // 3. Restore must not call RevenueCat's restore API until identity is confirmed Ready for
    // the current Supabase user.
    @Test
    fun `restore does not call RevenueCat until identity is Ready`() = runTest {
        val authRepository = FakeAuthRepository()
        val purchasesService = FakePurchasesService()
        val loginGate = CompletableDeferred<Unit>()
        purchasesService.logInGate = loginGate
        val identitySync = newIdentitySync(authRepository, purchasesService)
        val billingRepository = BillingRepositoryImpl(purchasesService, identitySync)
        authRepository.setUser(testUser(id = "user-1"))

        var restoreCompleted = false
        val job = launch {
            billingRepository.restorePurchases()
            restoreCompleted = true
        }
        runCurrent()

        assertEquals(0, purchasesService.restoreCallCount)
        assertFalse(restoreCompleted)

        loginGate.complete(Unit)
        runCurrent()

        assertEquals(1, purchasesService.restoreCallCount)
        assertTrue(restoreCompleted)
        assertEquals(listOf("logOut", "logIn:user-1", "restore"), purchasesService.callOrder)
        job.cancel()
    }
}
