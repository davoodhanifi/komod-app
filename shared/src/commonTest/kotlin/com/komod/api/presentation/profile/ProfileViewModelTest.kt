package com.komod.api.presentation.profile

import com.komod.api.data.repository.SubscriptionRepository
import com.komod.api.domain.model.CurrentSubscription
import com.komod.api.domain.model.SubscriptionPlan
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
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

// A fresh CompletableDeferred per fake instance lets each test control exactly when
// getCurrentSubscription() resolves, so the ViewModel's Loading state can be observed
// before it settles into Success.
private class FakeSubscriptionRepository : SubscriptionRepository {
    private val result = CompletableDeferred<Result<CurrentSubscription>>()

    override suspend fun getCurrentSubscription(): CurrentSubscription = result.await().getOrThrow()

    fun complete(subscription: CurrentSubscription) {
        result.complete(Result.success(subscription))
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

    // 5. Loading state.
    @Test
    fun `starts in Loading until the repository call resolves`() = runTest(testDispatcher) {
        val repository = FakeSubscriptionRepository()
        val viewModel = ProfileViewModel(repository)

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
            wardrobeItemLimit = 10,
            dailyOutfitGenerationLimit = 5,
            currentWardrobeItemCount = 3,
            todayOutfitGenerationCount = 1,
        )

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

        repository.complete(subscription)
        advanceUntilIdle()

        val state = assertIs<SubscriptionUiState.Success>(viewModel.uiState.value.subscriptionState)
        assertEquals(null, state.subscription.wardrobeItemLimit)
        assertEquals(null, state.subscription.dailyOutfitGenerationLimit)
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
