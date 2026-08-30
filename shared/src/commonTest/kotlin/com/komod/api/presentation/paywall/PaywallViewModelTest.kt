package com.komod.api.presentation.paywall

import com.komod.api.data.repository.BillingRepository
import com.komod.api.data.repository.SubscriptionSyncRepository
import com.komod.api.domain.model.BillingPeriod
import com.komod.api.domain.model.KomodSubscriptionState
import com.komod.api.domain.model.PaywallPlan
import com.komod.api.domain.model.SubscriptionPlan
import com.komod.api.domain.model.SubscriptionStatus
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PackageType
import com.revenuecat.purchases.kmp.models.PresentedOfferingContext
import com.revenuecat.purchases.kmp.models.Price
import com.revenuecat.purchases.kmp.models.ProductType
import com.revenuecat.purchases.kmp.models.PurchasingData
import com.revenuecat.purchases.kmp.models.StoreProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun fakePackage(identifier: String): Package = object : Package {
    override val identifier: String = identifier
    override val packageType: PackageType = PackageType.CUSTOM
    override val storeProduct: StoreProduct = object : StoreProduct {
        override val id: String = identifier
        override val type: ProductType = ProductType.SUBS
        override val category: com.revenuecat.purchases.kmp.models.ProductCategory? = null
        override val price: Price = Price(formatted = "$9.99", amountMicros = 9_990_000, currencyCode = "USD")
        override val title: String = identifier
        override val localizedDescription: String? = null
        override val period: com.revenuecat.purchases.kmp.models.Period? = null
        override val subscriptionOptions: com.revenuecat.purchases.kmp.models.SubscriptionOptions? = null
        override val defaultOption: com.revenuecat.purchases.kmp.models.SubscriptionOption? = null
        override val discounts: List<com.revenuecat.purchases.kmp.models.StoreProductDiscount> = emptyList()
        override val introductoryDiscount: com.revenuecat.purchases.kmp.models.StoreProductDiscount? = null
        override val purchasingData: PurchasingData = object : PurchasingData {
            override val productId: String = identifier
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

private fun testPlan(plan: SubscriptionPlan) = PaywallPlan(
    plan = plan,
    monthlyPackage = fakePackage("${plan.name}_monthly"),
    yearlyPackage = fakePackage("${plan.name}_yearly"),
)

private fun testSubscriptionState(
    plan: SubscriptionPlan,
    status: SubscriptionStatus = SubscriptionStatus.Active,
) = KomodSubscriptionState(
    plan = plan,
    status = status,
    expiresAt = null,
    willRenew = status == SubscriptionStatus.Active,
    store = "APP_STORE",
    productId = "komod_3doors_monthly",
)

private class FakeBillingRepository : BillingRepository {
    override val isBillingAvailable: Boolean = true

    var paywallPlansResult: Result<List<PaywallPlan>> = Result.success(listOf(testPlan(SubscriptionPlan.ThreeDoors)))
    var purchaseResult: Result<Unit> = Result.success(Unit)
    var restoreResult: Result<Unit> = Result.success(Unit)
    var activeProductIdentifiersResult: Result<Set<String>> = Result.success(emptySet())

    val purchaseCalls = mutableListOf<Package>()
    var restoreCallCount = 0
        private set
    var refreshCustomerInfoCallCount = 0
        private set
    var getActiveProductIdentifiersCallCount = 0
        private set

    override suspend fun getPaywallPlans(): List<PaywallPlan> = paywallPlansResult.getOrThrow()

    override suspend fun purchase(rcPackage: Package) {
        purchaseCalls.add(rcPackage)
        purchaseResult.getOrThrow()
    }

    override suspend fun restorePurchases() {
        restoreCallCount++
        restoreResult.getOrThrow()
    }

    override suspend fun refreshCustomerInfo() {
        refreshCustomerInfoCallCount++
    }

    override suspend fun getActiveProductIdentifiers(): Set<String> {
        getActiveProductIdentifiersCallCount++
        return activeProductIdentifiersResult.getOrThrow()
    }
}

private class FakeSubscriptionSyncRepository : SubscriptionSyncRepository {
    private val _subscriptionState = MutableStateFlow<KomodSubscriptionState?>(null)
    override val subscriptionState: StateFlow<KomodSubscriptionState?> = _subscriptionState.asStateFlow()

    var syncResult: Result<KomodSubscriptionState> = Result.success(testSubscriptionState(SubscriptionPlan.ThreeDoors))
    var syncCallCount = 0
        private set

    override suspend fun sync(): KomodSubscriptionState {
        syncCallCount++
        val state = syncResult.getOrThrow()
        _subscriptionState.value = state
        return state
    }
}

class PaywallViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Purchase -> backend sync: a successful purchase followed by a successful sync updates
    // the displayed plan from the *sync response* and only then dismisses the paywall.
    @Test
    fun `a successful purchase syncs with the backend and emits PurchaseCompleted`() = runTest(testDispatcher) {
        val billingRepository = FakeBillingRepository()
        val syncRepository = FakeSubscriptionSyncRepository()
        syncRepository.syncResult = Result.success(testSubscriptionState(SubscriptionPlan.ThreeDoors, SubscriptionStatus.Active))
        val viewModel = PaywallViewModel(billingRepository, syncRepository)
        advanceUntilIdle()
        // The ViewModel's init already triggers one sync (its own foreground/initial-load
        // refresh) — capture that baseline rather than assuming a fresh count of zero.
        val syncCallsBeforeAction = syncRepository.syncCallCount
        val refreshCallsBeforeAction = billingRepository.refreshCustomerInfoCallCount
        viewModel.selectPlan(SubscriptionPlan.ThreeDoors)

        val effects = mutableListOf<PaywallEffect>()
        val collectJob = launch { viewModel.effects.collect { effects.add(it) } }

        viewModel.purchaseSelectedPlan()
        advanceUntilIdle()

        assertEquals(1, billingRepository.purchaseCalls.size)
        assertEquals(syncCallsBeforeAction + 1, syncRepository.syncCallCount)
        assertEquals(refreshCallsBeforeAction + 1, billingRepository.refreshCustomerInfoCallCount)
        assertEquals(SubscriptionPlan.ThreeDoors, viewModel.uiState.value.subscriptionState?.plan)
        assertNull(viewModel.uiState.value.syncFailedMessage)
        assertTrue(effects.contains(PaywallEffect.PurchaseCompleted))
        collectJob.cancel()
    }

    // Restore -> backend sync: same reconciliation flow, but on success it stays on screen
    // with a confirmation snackbar rather than dismissing.
    @Test
    fun `a successful restore syncs with the backend and updates the subscription state`() = runTest(testDispatcher) {
        val billingRepository = FakeBillingRepository()
        val syncRepository = FakeSubscriptionSyncRepository()
        syncRepository.syncResult = Result.success(testSubscriptionState(SubscriptionPlan.WalkIn, SubscriptionStatus.Active))
        val viewModel = PaywallViewModel(billingRepository, syncRepository)
        advanceUntilIdle()
        val syncCallsBeforeAction = syncRepository.syncCallCount

        val effects = mutableListOf<PaywallEffect>()
        val collectJob = launch { viewModel.effects.collect { effects.add(it) } }

        viewModel.restorePurchases()
        advanceUntilIdle()

        assertEquals(1, billingRepository.restoreCallCount)
        assertEquals(syncCallsBeforeAction + 1, syncRepository.syncCallCount)
        assertEquals(SubscriptionPlan.WalkIn, viewModel.uiState.value.subscriptionState?.plan)
        assertTrue(effects.any { it is PaywallEffect.ShowSnackbar && it.message.contains("restored") })
        collectJob.cancel()
    }

    // Foreground -> backend sync: refreshSubscriptionState() (driven by PaywallScreen's
    // ON_RESUME observer) refreshes RevenueCat's CustomerInfo (via activeProductIdentifiers,
    // the same call also feeding the auto-select match), syncs, and adopts the response.
    @Test
    fun `refreshSubscriptionState refreshes RevenueCat's cache then syncs and adopts the response`() = runTest(testDispatcher) {
        val billingRepository = FakeBillingRepository()
        val syncRepository = FakeSubscriptionSyncRepository()
        syncRepository.syncResult = Result.success(testSubscriptionState(SubscriptionPlan.TwoDoors, SubscriptionStatus.Active))
        val viewModel = PaywallViewModel(billingRepository, syncRepository)
        advanceUntilIdle()
        val syncCallsBeforeExplicitRefresh = syncRepository.syncCallCount
        val activeIdCallsBeforeExplicitRefresh = billingRepository.getActiveProductIdentifiersCallCount

        viewModel.refreshSubscriptionState()
        advanceUntilIdle()

        assertEquals(syncCallsBeforeExplicitRefresh + 1, syncRepository.syncCallCount)
        assertTrue(billingRepository.getActiveProductIdentifiersCallCount > activeIdCallsBeforeExplicitRefresh)
        assertEquals(SubscriptionPlan.TwoDoors, viewModel.uiState.value.subscriptionState?.plan)
    }

    // A cancelled-but-not-yet-expired plan from the backend is shown as-is — the ViewModel
    // performs no local downgrade based on status.
    @Test
    fun `a cancelled plan from the backend is still shown as the current plan`() = runTest(testDispatcher) {
        val billingRepository = FakeBillingRepository()
        val syncRepository = FakeSubscriptionSyncRepository()
        syncRepository.syncResult = Result.success(testSubscriptionState(SubscriptionPlan.ThreeDoors, SubscriptionStatus.Cancelled))
        val viewModel = PaywallViewModel(billingRepository, syncRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value.subscriptionState
        assertEquals(SubscriptionPlan.ThreeDoors, state?.plan)
        assertEquals(SubscriptionStatus.Cancelled, state?.status)
    }

    // Once the backend reports Expired (and, per its own contract, flips the plan to Rack),
    // the ViewModel reflects Rack immediately — again, a straight passthrough.
    @Test
    fun `an expired plan from the backend is shown as KomodRack`() = runTest(testDispatcher) {
        val billingRepository = FakeBillingRepository()
        val syncRepository = FakeSubscriptionSyncRepository()
        syncRepository.syncResult = Result.success(testSubscriptionState(SubscriptionPlan.Rack, SubscriptionStatus.Expired))
        val viewModel = PaywallViewModel(billingRepository, syncRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value.subscriptionState
        assertEquals(SubscriptionPlan.Rack, state?.plan)
        assertEquals(SubscriptionStatus.Expired, state?.status)
    }

    // A user-cancelling the App Store sheet must not attempt a backend sync at all — there is
    // nothing new to reconcile, and per Phase 1 this isn't reported as an error either.
    @Test
    fun `cancelling the purchase does not trigger a backend sync`() = runTest(testDispatcher) {
        val billingRepository = FakeBillingRepository()
        billingRepository.purchaseResult = Result.failure(
            com.revenuecat.purchases.kmp.models.PurchasesTransactionException(
                purchasesError = com.revenuecat.purchases.kmp.models.PurchasesError(
                    code = com.revenuecat.purchases.kmp.models.PurchasesErrorCode.PurchaseCancelledError,
                ),
                userCancelled = true,
            ),
        )
        val syncRepository = FakeSubscriptionSyncRepository()
        val viewModel = PaywallViewModel(billingRepository, syncRepository)
        advanceUntilIdle()
        val syncCallsBeforeAction = syncRepository.syncCallCount
        viewModel.selectPlan(SubscriptionPlan.ThreeDoors)

        viewModel.purchaseSelectedPlan()
        advanceUntilIdle()

        assertEquals(syncCallsBeforeAction, syncRepository.syncCallCount)
        assertEquals(false, viewModel.uiState.value.isPurchasing)
    }

    // Yearly is the default so users open the Paywall already on the plan we want to nudge
    // them toward.
    @Test
    fun `Yearly is the default billing period on open`() = runTest(testDispatcher) {
        val viewModel = PaywallViewModel(FakeBillingRepository(), FakeSubscriptionSyncRepository())
        advanceUntilIdle()

        assertEquals(BillingPeriod.Yearly, viewModel.uiState.value.billingPeriod)
    }

    // Users can still switch to Monthly (and back) normally — the new default doesn't lock
    // the toggle.
    @Test
    fun `selecting Monthly switches away from the Yearly default and back again`() = runTest(testDispatcher) {
        val viewModel = PaywallViewModel(FakeBillingRepository(), FakeSubscriptionSyncRepository())
        advanceUntilIdle()

        viewModel.selectBillingPeriod(BillingPeriod.Monthly)
        assertEquals(BillingPeriod.Monthly, viewModel.uiState.value.billingPeriod)

        viewModel.selectBillingPeriod(BillingPeriod.Yearly)
        assertEquals(BillingPeriod.Yearly, viewModel.uiState.value.billingPeriod)
    }

    // Opening the paywall should land on the plan the user is already subscribed to, not the
    // first one in the offering — matched via RevenueCat's own CustomerInfo.activeSubscriptions
    // product IDs against the loaded packages, never the backend's KomodSubscriptionState.
    @Test
    fun `opening the paywall auto-selects the plan matching the current active subscription`() = runTest(testDispatcher) {
        val billingRepository = FakeBillingRepository()
        billingRepository.paywallPlansResult = Result.success(
            listOf(testPlan(SubscriptionPlan.OneDoor), testPlan(SubscriptionPlan.TwoDoors), testPlan(SubscriptionPlan.ThreeDoors)),
        )
        // Matches TwoDoors' yearly package's product ID (see testPlan/fakePackage).
        billingRepository.activeProductIdentifiersResult = Result.success(setOf("TwoDoors_yearly"))
        val viewModel = PaywallViewModel(billingRepository, FakeSubscriptionSyncRepository())
        advanceUntilIdle()

        assertEquals(SubscriptionPlan.TwoDoors, viewModel.uiState.value.selectedPlan)
    }

    // No active subscription at all — the pre-existing "first plan" default applies instead.
    @Test
    fun `no active subscription keeps the existing default selection`() = runTest(testDispatcher) {
        val billingRepository = FakeBillingRepository()
        billingRepository.paywallPlansResult = Result.success(
            listOf(testPlan(SubscriptionPlan.OneDoor), testPlan(SubscriptionPlan.TwoDoors)),
        )
        billingRepository.activeProductIdentifiersResult = Result.success(emptySet())
        val viewModel = PaywallViewModel(billingRepository, FakeSubscriptionSyncRepository())
        advanceUntilIdle()

        assertEquals(SubscriptionPlan.OneDoor, viewModel.uiState.value.selectedPlan)
    }

    // A current subscription for a plan that dropped out of the current offering (e.g. a
    // discontinued tier) must not crash or leave nothing selected — same "first plan" fallback.
    @Test
    fun `a current subscription not present in the offering keeps the existing default selection`() = runTest(testDispatcher) {
        val billingRepository = FakeBillingRepository()
        billingRepository.paywallPlansResult = Result.success(
            listOf(testPlan(SubscriptionPlan.OneDoor), testPlan(SubscriptionPlan.TwoDoors)),
        )
        // A product ID for a plan that isn't part of the loaded offering at all.
        billingRepository.activeProductIdentifiersResult = Result.success(setOf("WalkIn_monthly"))
        val viewModel = PaywallViewModel(billingRepository, FakeSubscriptionSyncRepository())
        advanceUntilIdle()

        assertEquals(SubscriptionPlan.OneDoor, viewModel.uiState.value.selectedPlan)
    }

    // A failure fetching RevenueCat's active product IDs must not block selection forever —
    // it degrades to the pre-existing "first plan" default, same as "no active subscription".
    @Test
    fun `a failure reading RevenueCat's active subscriptions keeps the existing default selection`() = runTest(testDispatcher) {
        val billingRepository = FakeBillingRepository()
        billingRepository.paywallPlansResult = Result.success(
            listOf(testPlan(SubscriptionPlan.OneDoor), testPlan(SubscriptionPlan.TwoDoors)),
        )
        billingRepository.activeProductIdentifiersResult = Result.failure(RuntimeException("network error"))
        val viewModel = PaywallViewModel(billingRepository, FakeSubscriptionSyncRepository())
        advanceUntilIdle()

        assertEquals(SubscriptionPlan.OneDoor, viewModel.uiState.value.selectedPlan)
    }

    // Once the user taps a plan themselves, a later subscription re-sync (e.g. ON_RESUME) must
    // never yank the selection back to their current plan.
    @Test
    fun `a manual plan selection is never overridden by a later subscription sync`() = runTest(testDispatcher) {
        val billingRepository = FakeBillingRepository()
        billingRepository.paywallPlansResult = Result.success(
            listOf(testPlan(SubscriptionPlan.OneDoor), testPlan(SubscriptionPlan.TwoDoors)),
        )
        billingRepository.activeProductIdentifiersResult = Result.success(setOf("OneDoor_monthly"))
        val viewModel = PaywallViewModel(billingRepository, FakeSubscriptionSyncRepository())
        advanceUntilIdle()
        assertEquals(SubscriptionPlan.OneDoor, viewModel.uiState.value.selectedPlan)

        viewModel.selectPlan(SubscriptionPlan.TwoDoors)
        viewModel.refreshSubscriptionState()
        advanceUntilIdle()

        assertEquals(SubscriptionPlan.TwoDoors, viewModel.uiState.value.selectedPlan)
    }

    // 8/9. Backend sync failure after a successful purchase/restore is intentionally not
    // covered here: reaching that branch calls ErrorMapper.toUserMessage(), which logs via
    // AppLogger.e() (android.util.Log) — unmocked in this plain JVM test target, so it throws
    // rather than being swallowed (same pre-existing limitation documented in
    // ProfileViewModelTest/WardrobeViewModelPollingTest/AddItemViewModelTest/LoginViewModelTest).
    // The guarantee that a failed sync never fabricates/persists a plan is instead covered at
    // the repository layer (SubscriptionSyncRepositoryImplTest), which doesn't touch
    // AppLogger — there is no code path in PaywallViewModel that could synthesize a
    // subscriptionState from a sync failure either way (see syncAfterEntitlementChange).
}
