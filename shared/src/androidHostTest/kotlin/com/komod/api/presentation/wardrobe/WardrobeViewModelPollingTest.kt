package com.komod.api.presentation.wardrobe

import androidx.lifecycle.viewModelScope
import com.komod.api.data.api.model.CreateImageResponse
import com.komod.api.data.api.model.ImageStatus
import com.komod.api.data.repository.AddItemRepository
import com.komod.api.data.repository.WardrobeRepository
import com.komod.api.domain.model.UploadedImage
import com.komod.api.domain.model.WardrobeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private fun uploadedImage(id: String, status: ImageStatus) = UploadedImage(
    imageId = id,
    storagePath = "originals/$id.jpg",
    thumbnailStoragePath = "thumbs/$id.jpg",
    status = status,
)

private class FakeWardrobeRepository : WardrobeRepository {
    override suspend fun getWardrobeItems(): List<WardrobeItem> = emptyList()
    override suspend fun deleteWardrobeItems(ids: List<String>) = error("not used by these tests")
}

// A FIFO queue of outcomes for refreshUploadedImages(): each call pops one entry and
// either applies its result (a status transition) or throws (a transient failure), so
// tests can script exactly what each successive GET /images/uploaded poll returns —
// including the *first* call, which is WardrobeViewModel's immediate on-init refresh,
// not yet a poll-loop tick.
private class FakeAddItemRepository(initialImages: List<UploadedImage>) : AddItemRepository {
    private val _uploadedImages = MutableStateFlow(initialImages)
    override val uploadedImages: StateFlow<List<UploadedImage>> = _uploadedImages.asStateFlow()

    val refreshQueue = mutableListOf<() -> List<UploadedImage>>()
    var refreshCallCount = 0
        private set

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
        _uploadedImages.update { current -> current.filterNot { it.imageId == imageId } }
    }

    var deleteUploadedImageError: Throwable? = null
    val deleteUploadedImageCalls = mutableListOf<String>()

    override suspend fun deleteUploadedImage(imageId: String) {
        deleteUploadedImageCalls += imageId
        deleteUploadedImageError?.let { throw it }
        _uploadedImages.update { current -> current.filterNot { it.imageId == imageId } }
    }

    override suspend fun getThumbnailUrl(storagePath: String): String? = null

    override suspend fun refreshUploadedImages() {
        refreshCallCount++
        val next = refreshQueue.removeFirstOrNull() ?: return
        _uploadedImages.value = next()
    }
}

class WardrobeViewModelPollingTest {
    private val testDispatcher = StandardTestDispatcher()

    // WardrobeViewModel's init{} starts an unbounded collectLatest (pollWhileUploadsActive)
    // that never completes on its own. Dispatchers.Main is a process-global redirect, so a
    // ViewModel left running past its own test can resurface against a *different* test's
    // scheduler once that test calls Dispatchers.setMain — cancelling here prevents that
    // cross-test leak.
    private val createdViewModels = mutableListOf<WardrobeViewModel>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        createdViewModels.forEach { it.viewModelScope.cancel() }
        createdViewModels.clear()
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        wardrobeRepository: WardrobeRepository,
        addItemRepository: AddItemRepository,
    ): WardrobeViewModel {
        val viewModel = WardrobeViewModel(wardrobeRepository, addItemRepository)
        createdViewModels += viewModel
        return viewModel
    }

    @Test
    fun `polling transitions a Processing image to Analyzed and then stops`() = runTest(testDispatcher) {
        val addItemRepository = FakeAddItemRepository(listOf(uploadedImage("img-1", ImageStatus.Processing)))
        createViewModel(FakeWardrobeRepository(), addItemRepository)
        // NOT advanceUntilIdle(): the poll loop's while(true) { delay(5000); ... } is
        // already active once the seeded image is Pending/Processing, and it reschedules
        // itself forever — advanceUntilIdle() would try to drain that queue and never
        // return. runCurrent() only runs what's immediately ready (init{}'s non-delayed
        // launch), which is enough to settle startup without touching the recurring timer.
        runCurrent()

        addItemRepository.refreshQueue += { listOf(uploadedImage("img-1", ImageStatus.Analyzed)) }
        advanceTimeBy(5_001)
        runCurrent()

        assertEquals(ImageStatus.Analyzed, addItemRepository.uploadedImages.value.single().status)

        val countOnceAnalyzed = addItemRepository.refreshCallCount
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(countOnceAnalyzed, addItemRepository.refreshCallCount) // no further polling
    }

    @Test
    fun `polling transitions a Processing image to Failed and then stops`() = runTest(testDispatcher) {
        val addItemRepository = FakeAddItemRepository(listOf(uploadedImage("img-2", ImageStatus.Processing)))
        createViewModel(FakeWardrobeRepository(), addItemRepository)
        runCurrent()

        addItemRepository.refreshQueue += { listOf(uploadedImage("img-2", ImageStatus.Failed)) }
        advanceTimeBy(5_001)
        runCurrent()

        assertEquals(ImageStatus.Failed, addItemRepository.uploadedImages.value.single().status)

        val countOnceFailed = addItemRepository.refreshCallCount
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(countOnceFailed, addItemRepository.refreshCallCount) // no further polling
    }

    @Test
    fun `a temporary poll failure does not mark the image Failed and polling continues`() = runTest(testDispatcher) {
        val addItemRepository = FakeAddItemRepository(listOf(uploadedImage("img-3", ImageStatus.Processing)))
        createViewModel(FakeWardrobeRepository(), addItemRepository)
        runCurrent()

        addItemRepository.refreshQueue += { throw RuntimeException("transient network blip") }
        addItemRepository.refreshQueue += { listOf(uploadedImage("img-3", ImageStatus.Analyzed)) }

        advanceTimeBy(5_001) // first poll tick: throws, swallowed
        runCurrent()
        assertEquals(ImageStatus.Processing, addItemRepository.uploadedImages.value.single().status)

        advanceTimeBy(5_000) // second poll tick: succeeds
        runCurrent()
        assertEquals(ImageStatus.Analyzed, addItemRepository.uploadedImages.value.single().status)
    }

    @Test
    fun `polling does not run more than one loop at a time`() = runTest(testDispatcher) {
        val addItemRepository = FakeAddItemRepository(listOf(uploadedImage("img-4", ImageStatus.Processing)))
        createViewModel(FakeWardrobeRepository(), addItemRepository)
        runCurrent()
        val countAfterInit = addItemRepository.refreshCallCount

        // Empty queue: every poll tick is a no-op, so the image stays Processing and the
        // loop keeps running for the whole window. A duplicate loop would double the count.
        advanceTimeBy(15_000)
        runCurrent()

        assertEquals(countAfterInit + 3, addItemRepository.refreshCallCount)

        // Bring the loop to a terminal status before the test ends — runTest's cleanup
        // drains the shared TestCoroutineScheduler, and an eternally-recurring
        // while(true) { delay(5000) } left active past the end of the test body would
        // hang that drain forever.
        addItemRepository.refreshQueue += { listOf(uploadedImage("img-4", ImageStatus.Analyzed)) }
        advanceTimeBy(5_000)
        runCurrent()
    }

    @Test
    fun `confirming a delete removes the upload and closes the dialog`() = runTest(testDispatcher) {
        val addItemRepository = FakeAddItemRepository(listOf(uploadedImage("img-5", ImageStatus.Processing)))
        val viewModel = createViewModel(FakeWardrobeRepository(), addItemRepository)
        runCurrent()

        viewModel.requestDeleteUpload("img-5")
        assertEquals("img-5", viewModel.pendingDeleteUploadId.value)

        viewModel.confirmDeleteUpload()
        runCurrent()

        assertEquals(listOf("img-5"), addItemRepository.deleteUploadedImageCalls)
        assertEquals(emptyList(), addItemRepository.uploadedImages.value)
        assertEquals(null, viewModel.pendingDeleteUploadId.value)
    }

    // A failure-path test (confirmDeleteUpload's .onFailure branch) is intentionally not
    // included here: that branch calls ErrorMapper.toUserMessage(), which logs via
    // AppLogger.e() (android.util.Log) — unmocked in this plain JVM test target, so it
    // throws and fails the coroutine outright rather than being swallowed. No other
    // ViewModel failure path that calls ErrorMapper is covered in this test suite either,
    // for the same reason.
}
