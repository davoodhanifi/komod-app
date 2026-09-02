package com.komod.api.presentation.profile

import com.komod.api.data.repository.SubscriptionRepository
import com.komod.api.domain.model.CurrentSubscription
import com.komod.api.domain.model.SubscriptionPlan
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// A fresh CompletableDeferred per call lets each test control exactly when
// getCurrentSubscription() resolves, so the ViewModel's Loading state can be observed
// before it settles into Success — including across a second, later call, via
// prepareNext().
private class FakeSubscriptionRepository : SubscriptionRepository {
    private var deferred = CompletableDeferred<Result<CurrentSubscription>>()

    override suspend fun getCurrentSubscription(): CurrentSubscription = deferred.await().getOrThrow()

    fun complete(subscription: CurrentSubscription) {
        deferred.complete(Result.success(subscription))
    }

    // Call between two loadSubscription() invocations so the second call's
    // getCurrentSubscription() suspends on its own fresh deferred instead of
    // immediately resuming with the first call's already-completed result.
    fun prepareNext() {
        deferred = CompletableDeferred()
    }
}

private fun testSubscription(
    plan: SubscriptionPlan = SubscriptionPlan.TwoDoors,
    wardrobeItemLimit: Int? = 150,
    dailyOutfitGenerationLimit: Int? = 30,
    currentWardrobeItemCount: Int = 87,
    todayOutfitGenerationCount: Int = 12,
) = CurrentSubscription(
    plan = plan,
    wardrobeItemLimit = wardrobeItemLimit,
    dailyOutfitGenerationLimit = dailyOutfitGenerationLimit,
    currentWardrobeItemCount = currentWardrobeItemCount,
    todayOutfitGenerationCount = todayOutfitGenerationCount,
)

class ProfileViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 5. Loading state. loadSubscription() is called explicitly here — unlike before,
    // ProfileViewModel no longer auto-fetches from init{}; that's now the caller's job
    // (ProfileScreen's LaunchedEffect(Unit), which fires on every visit to the Profile
    // tab so returning to it always picks up fresh usage numbers).
    @Test
    fun `starts in Loading until the repository call resolves`() = runTest(testDispatcher) {
        val repository = FakeSubscriptionRepository()
        val viewModel = ProfileViewModel(repository)

        viewModel.loadSubscription()
        assertIs<SubscriptionUiState.Loading>(viewModel.uiState.value.subscriptionState)

        repository.complete(testSubscription())
        advanceUntilIdle()

        assertIs<SubscriptionUiState.Success>(viewModel.uiState.value.subscriptionState)
    }

    // 1 & 4. Rack (or any plan) with its exact API-provided limits and usage counts.
    @Test
    fun `a successful load exposes the exact plan and usage the repository returned`() = runTest(testDispatcher) {
        val repository = FakeSubscriptionRepository()
        val viewModel = ProfileViewModel(repository)
        val subscription = testSubscription(
            plan = SubscriptionPlan.Rack,
            wardrobeItemLimit = 15,
            dailyOutfitGenerationLimit = 5,
            currentWardrobeItemCount = 3,
            todayOutfitGenerationCount = 1,
        )

        viewModel.loadSubscription()
        repository.complete(subscription)
        advanceUntilIdle()

        val state = assertIs<SubscriptionUiState.Success>(viewModel.uiState.value.subscriptionState)
        assertEquals(subscription, state.subscription)
    }

    // 3. Walk-in with null limits passes straight through to the UI state untouched.
    @Test
    fun `a successful load preserves null limits for an unlimited plan`() = runTest(testDispatcher) {
        val repository = FakeSubscriptionRepository()
        val viewModel = ProfileViewModel(repository)
        val subscription = testSubscription(
            plan = SubscriptionPlan.WalkIn,
            wardrobeItemLimit = null,
            dailyOutfitGenerationLimit = null,
        )

        viewModel.loadSubscription()
        repository.complete(subscription)
        advanceUntilIdle()

        val state = assertIs<SubscriptionUiState.Success>(viewModel.uiState.value.subscriptionState)
        assertEquals(null, state.subscription.wardrobeItemLimit)
        assertEquals(null, state.subscription.dailyOutfitGenerationLimit)
    }

    // A repeat call to loadSubscription() (simulating a return visit to Profile) must not
    // blank an already-successful state back to Loading — that's the flicker this design
    // exists to avoid, now that every visit triggers a real refetch. runCurrent() (not
    // advanceUntilIdle()) is used after the second call specifically so this observes the
    // state *before* the new network call resolves, which is exactly the moment a flicker
    // would show up.
    @Test
    fun `a second load while already showing a successful result does not flash back to Loading`() = runTest(testDispatcher) {
        val repository = FakeSubscriptionRepository()
        val viewModel = ProfileViewModel(repository)

        viewModel.loadSubscription()
        repository.complete(testSubscription())
        advanceUntilIdle()
        assertIs<SubscriptionUiState.Success>(viewModel.uiState.value.subscriptionState)

        repository.prepareNext()
        viewModel.loadSubscription()
        runCurrent()
        assertIs<SubscriptionUiState.Success>(viewModel.uiState.value.subscriptionState)

        val updated = testSubscription(currentWardrobeItemCount = 99)
        repository.complete(updated)
        advanceUntilIdle()
        val state = assertIs<SubscriptionUiState.Success>(viewModel.uiState.value.subscriptionState)
        assertEquals(updated, state.subscription)
    }

    // 6. A test asserting that an API failure surfaces SubscriptionUiState.Error (rather
    // than a fake default) is intentionally not included here: that path calls
    // ErrorMapper.toUserMessage(), which logs via AppLogger.e() (android.util.Log) —
    // unmocked in this plain JVM test target, so it throws rather than being swallowed.
    // The same pre-existing limitation is documented in WardrobeViewModelPollingTest/
    // AddItemViewModelTest/LoginViewModelTest. "No fake defaults on failure" is instead
    // covered at the repository/API layer (SubscriptionApiServiceTest,
    // SubscriptionRepositoryImplTest), which simply propagates the exception — there is no
    // code path in ProfileViewModel that could synthesize a Success state from a failure.
}
