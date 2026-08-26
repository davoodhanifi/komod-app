package com.komod.api.presentation.uploadreview

import com.komod.api.core.error.PlanLimitCategory
import com.komod.api.core.error.PlanLimitExceededException
import com.komod.api.core.navigation.PlanLimitNavigatorImpl
import com.komod.api.data.api.model.CreateImageResponse
import com.komod.api.data.api.model.ImageStatus
import com.komod.api.data.api.model.WardrobeItemReviewRequestItem
import com.komod.api.data.repository.AddItemRepository
import com.komod.api.data.repository.AuthRepository
import com.komod.api.data.repository.UploadReviewRepository
import com.komod.api.domain.model.BoundingBox
import com.komod.api.domain.model.UploadedImage
import com.komod.api.domain.model.UploadedImageDetail
import com.komod.api.domain.model.User
import com.komod.api.domain.model.WardrobeItemDetail
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun testWardrobeItemDetail(id: String) = WardrobeItemDetail(
    id = id,
    imageId = "img-1",
    imageUrl = null,
    status = 0,
    category = "top",
    subcategory = null,
    itemName = null,
    brand = null,
    bodyRegion = null,
    layer = null,
    primaryColor = null,
    secondaryColors = null,
    accentColors = null,
    pattern = null,
    material = null,
    texture = null,
    fit = null,
    silhouette = null,
    sleeveLength = null,
    pantLength = null,
    neckline = null,
    collarType = null,
    closure = null,
    formality = null,
    style = null,
    season = null,
    occasion = null,
    genderStyle = null,
    weatherMinTempC = null,
    weatherMaxTempC = null,
    weatherRainFriendly = null,
    warmthLevel = null,
    features = null,
    recommendedPairings = null,
    embeddingDescription = null,
    isFavorite = false,
    confidence = 0.9,
    createdAt = "2026-08-10T00:00:00Z",
    boundingBox = BoundingBox.FullImage,
)

private class FakeUploadReviewRepository(
    private val detail: UploadedImageDetail,
) : UploadReviewRepository {
    var submitError: Throwable? = null
    val submittedItems = mutableListOf<List<WardrobeItemReviewRequestItem>>()

    override suspend fun getUploadedImageDetail(imageId: String): UploadedImageDetail = detail

    override suspend fun submitReview(items: List<WardrobeItemReviewRequestItem>) {
        submittedItems += items
        submitError?.let { throw it }
    }
}

private class FakeAuthRepository : AuthRepository {
    override val sessionStatus: StateFlow<SessionStatus> = MutableStateFlow(SessionStatus.NotAuthenticated())
    private val _callbackError = MutableSharedFlow<Throwable>()
    override val callbackError: SharedFlow<Throwable> = _callbackError.asSharedFlow()
    override val currentUser: StateFlow<User?> = MutableStateFlow(null)
    var signOutCalls = 0

    override suspend fun signInWithGoogle() = Unit
    override suspend fun signInWithApple() = Unit
    override suspend fun handleOAuthCallback(url: String) = Unit
    override fun currentUserOrNull(): User? = null
    override suspend fun signOut() {
        signOutCalls++
    }
}

private class FakeAddItemRepository : AddItemRepository {
    val removedImageIds = mutableListOf<String>()

    override suspend fun createImage(): CreateImageResponse = error("not used by these tests")
    override suspend fun uploadImage(
        image: CreateImageResponse,
        bytes: ByteArray,
        mimeType: String,
        onProgress: (Float) -> Unit,
    ) = error("not used by these tests")

    override suspend fun analyzeWardrobeItems(imageId: String) = error("not used by these tests")
    override fun triggerAnalysisInBackground(imageId: String) = error("not used by these tests")
    override fun saveUploadedImage(image: CreateImageResponse) = error("not used by these tests")
    override fun removeUploadedImage(imageId: String) {
        removedImageIds += imageId
    }

    override suspend fun deleteUploadedImage(imageId: String) = Unit
    override val uploadedImages: StateFlow<List<UploadedImage>> = MutableStateFlow(emptyList())
    override suspend fun getThumbnailUrl(storagePath: String): String? = null
    override suspend fun refreshUploadedImages() = Unit
}

class UploadReviewViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun readyDetail() = UploadedImageDetail(
        imageId = "img-1",
        status = ImageStatus.Analyzed,
        originalImageUrl = null,
        items = listOf(testWardrobeItemDetail("item-1"), testWardrobeItemDetail("item-2")),
    )

    // 1 & 6. Approving wardrobe items when the wardrobe is full: a PlanLimitExceeded from
    // submitReview must not be treated as a generic failure (no ReviewFailed snackbar), must
    // not silently drop/reject the user's selection, and must surface a dedicated effect the
    // Screen renders as its plan-limit dialog — and (this feature) must request the shared
    // Paywall navigation exactly once.
    @Test
    fun `submitReview failing with PlanLimitExceeded emits a dedicated effect and preserves the selection and requests the Paywall`() =
        runTest(UnconfinedTestDispatcher()) {
        val fakeRepository = FakeUploadReviewRepository(readyDetail())
        fakeRepository.submitError = PlanLimitExceededException(PlanLimitCategory.WardrobeCapacity)
        val addItemRepository = FakeAddItemRepository()
        val planLimitNavigator = PlanLimitNavigatorImpl()
        val viewModel = UploadReviewViewModel("img-1", fakeRepository, FakeAuthRepository(), addItemRepository, planLimitNavigator)

        val emittedEffects = mutableListOf<UploadReviewEffect>()
        val job = launch { viewModel.effects.collect { emittedEffects += it } }

        viewModel.toggleItemSelection("item-2") // deselect one, so selection isn't just "everything"
        val selectionBeforeSubmit = (viewModel.uiState.value as UploadReviewUiState.Ready).selectedItemIds

        viewModel.submitReview()

        val state = viewModel.uiState.value as UploadReviewUiState.Ready
        assertEquals(false, state.isSubmitting)
        assertEquals(selectionBeforeSubmit, state.selectedItemIds) // nothing auto-rejected
        assertEquals(listOf<UploadReviewEffect>(UploadReviewEffect.PlanLimitReached), emittedEffects)
        assertTrue(addItemRepository.removedImageIds.isEmpty()) // the upload was not treated as consumed
        assertEquals(true, planLimitNavigator.pendingPaywallRequest.value)
        job.cancel()
    }

    // 5. A test asserting that an unrelated failure still produces a generic ReviewFailed
    // effect is intentionally not included: that branch calls ErrorMapper.toUserMessage(),
    // which logs via AppLogger.e() (android.util.Log) — unmocked in this plain JVM test
    // target, so it throws rather than being swallowed. The same pre-existing limitation is
    // documented in WardrobeViewModelPollingTest/WardrobeViewModelBulkDeleteTest and
    // AddItemViewModelTest. The `when` branch added for PlanLimitExceededException above
    // sits alongside the untouched `else`/`UploadReviewUnauthorizedException` branches (see
    // UploadReviewViewModel.submitItems), and neither of those calls planLimitNavigator —
    // only the PlanLimitExceededException branch does, so a generic failure or a network/auth
    // error never requests the Paywall. (PlanLimitNavigatorTest separately covers that the
    // navigator itself never flips to "pending" except via an explicit requestPaywall() call.)

    // 7. Two unrelated screens both hitting a plan limit at the same time (e.g. two upload
    // reviews open back to back) must still only ever result in one pending Paywall request —
    // this is the shared navigator's coalescing behavior (see PlanLimitNavigatorTest for the
    // full contract), exercised here end-to-end with the real production ViewModel+navigator
    // rather than a fake.
    @Test
    fun `two plan-limit failures from separate ViewModels sharing one navigator still coalesce into one pending request`() =
        runTest(UnconfinedTestDispatcher()) {
        val sharedNavigator = PlanLimitNavigatorImpl()
        var pendingTrueCount = 0
        val collectJob = launch {
            sharedNavigator.pendingPaywallRequest.collect { if (it) pendingTrueCount++ }
        }

        val repository1 = FakeUploadReviewRepository(readyDetail())
        repository1.submitError = PlanLimitExceededException(PlanLimitCategory.WardrobeCapacity)
        val viewModel1 = UploadReviewViewModel("img-1", repository1, FakeAuthRepository(), FakeAddItemRepository(), sharedNavigator)
        viewModel1.submitReview()

        val repository2 = FakeUploadReviewRepository(readyDetail())
        repository2.submitError = PlanLimitExceededException(PlanLimitCategory.WardrobeCapacity)
        val viewModel2 = UploadReviewViewModel("img-2", repository2, FakeAuthRepository(), FakeAddItemRepository(), sharedNavigator)
        viewModel2.submitReview()

        assertEquals(true, sharedNavigator.pendingPaywallRequest.value)
        assertEquals(1, pendingTrueCount) // only one actual transition to "pending", not two
        collectJob.cancel()
    }

    // 8. Returning from the Paywall without upgrading (MainScaffold calling onPaywallShown()
    // once it has navigated) must not, on its own, retry the original blocked action or change
    // anything else about this ViewModel's state — the user stays blocked until they manually
    // retry.
    @Test
    fun `returning from the Paywall without upgrading does not retry the blocked submission`() =
        runTest(UnconfinedTestDispatcher()) {
        val fakeRepository = FakeUploadReviewRepository(readyDetail())
        fakeRepository.submitError = PlanLimitExceededException(PlanLimitCategory.WardrobeCapacity)
        val planLimitNavigator = PlanLimitNavigatorImpl()
        val viewModel = UploadReviewViewModel("img-1", fakeRepository, FakeAuthRepository(), FakeAddItemRepository(), planLimitNavigator)

        viewModel.submitReview()
        assertEquals(1, fakeRepository.submittedItems.size)
        assertEquals(true, planLimitNavigator.pendingPaywallRequest.value)

        // Simulates MainScaffold having navigated to the Paywall and the user coming back
        // without purchasing anything.
        planLimitNavigator.onPaywallShown()

        assertEquals(false, planLimitNavigator.pendingPaywallRequest.value)
        assertEquals(1, fakeRepository.submittedItems.size) // no automatic re-submission
        val state = viewModel.uiState.value as UploadReviewUiState.Ready
        assertEquals(false, state.isSubmitting) // still sitting blocked, ready for a manual retry
    }
}
