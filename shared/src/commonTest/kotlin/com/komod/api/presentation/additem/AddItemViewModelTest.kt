package com.komod.api.presentation.additem

import com.komod.api.core.navigation.PlanLimitNavigator
import com.komod.api.data.api.model.CreateImageResponse
import com.komod.api.data.repository.AddItemRepository
import com.komod.api.domain.model.BoundingBox
import com.komod.api.domain.model.UploadedImage
import com.komod.api.platform.PickedImage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

private class FakeAddItemRepository : AddItemRepository {
    var nextImageId = 0
    val createdImages = mutableListOf<CreateImageResponse>()
    val uploadedBytes = mutableListOf<Pair<String, ByteArray>>()
    val analyzedImageIds = mutableListOf<String>()

    override suspend fun createImage(): CreateImageResponse {
        val response = CreateImageResponse(
            imageId = "image-${nextImageId++}",
            storagePath = "storage/path",
            thumbnailStoragePath = "storage/thumb",
        )
        createdImages += response
        return response
    }

    override suspend fun uploadImage(
        image: CreateImageResponse,
        bytes: ByteArray,
        mimeType: String,
        onProgress: (Float) -> Unit,
    ) {
        onProgress(1f)
        uploadedBytes += image.imageId to bytes
    }

    override suspend fun analyzeWardrobeItems(imageId: String) {
        analyzedImageIds += imageId
    }

    override fun triggerAnalysisInBackground(imageId: String) {
        analyzedImageIds += imageId
    }

    override fun saveUploadedImage(image: CreateImageResponse) = Unit

    override fun removeUploadedImage(imageId: String) = Unit

    override suspend fun deleteUploadedImage(imageId: String) = Unit

    override val uploadedImages: StateFlow<List<UploadedImage>> = MutableStateFlow(emptyList())

    override suspend fun getThumbnailUrl(storagePath: String): String? = null

    override suspend fun refreshUploadedImages() = Unit
}

private class FakePlanLimitNavigator : PlanLimitNavigator {
    private val _pendingPaywallRequest = MutableStateFlow(false)
    override val pendingPaywallRequest: StateFlow<Boolean> = _pendingPaywallRequest
    var requestPaywallCallCount = 0
        private set

    override fun requestPaywall() {
        requestPaywallCallCount++
        _pendingPaywallRequest.value = true
    }

    override fun onPaywallShown() {
        _pendingPaywallRequest.value = false
    }
}

// A real, minimal 1x1 JPEG — applyCrop/confirmCropAndContinue now rasterize the crop eagerly
// (so the review grid can preview it), which means any test that confirms a crop exercises the
// real platform decoder. Arbitrary text bytes would fail to decode there.
private const val MinimalJpegBase64 =
    "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAMCAgICAgMCAgIDAwMDBAYEBAQEBAgGBgUGCQgKCgkICQkKDA8MCgsOCwkJDRENDg8QEBEQCgwSExIQEw8QEBD/2wBDAQMDAwQDBAgEBAgQCwkLEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBD/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAj/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCdABmX/9k="

@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
private val minimalJpegBytes: ByteArray = kotlin.io.encoding.Base64.decode(MinimalJpegBase64)

private fun pickedImage(tag: String) = PickedImage(mimeType = "image/jpeg") { minimalJpegBytes }

class AddItemViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selecting a single photo opens the crop editor immediately skipping the review grid`() {
        val viewModel = AddItemViewModel(FakeAddItemRepository(), FakePlanLimitNavigator())

        viewModel.onImagesSelected(listOf(pickedImage("only")))

        val state = assertIs<AddItemUiState.Reviewing>(viewModel.uiState.value)
        assertEquals(1, state.photos.size)
        assertEquals(state.photos.first().id, state.activeCropPhotoId)
    }

    @Test
    fun `selecting photos enters Reviewing synchronously without reading any photo's bytes`() {
        // onImagesSelected must never call a PickedImage's lazy loader itself — reading bytes
        // is exactly the work that used to delay navigation to Review Photos, and it must now
        // only ever happen later (thumbnail, crop, or upload), on demand.
        var loadCalls = 0
        val images = (1..3).map {
            PickedImage(mimeType = "image/jpeg") {
                loadCalls++
                minimalJpegBytes
            }
        }
        val viewModel = AddItemViewModel(FakeAddItemRepository(), FakePlanLimitNavigator())

        viewModel.onImagesSelected(images)

        val state = assertIs<AddItemUiState.Reviewing>(viewModel.uiState.value)
        assertEquals(3, state.photos.size)
        assertEquals(0, loadCalls)
    }

    @Test
    fun `selecting multiple photos shows the review grid with no crop editor open`() {
        val viewModel = AddItemViewModel(FakeAddItemRepository(), FakePlanLimitNavigator())

        viewModel.onImagesSelected(listOf(pickedImage("a"), pickedImage("b"), pickedImage("c")))

        val state = assertIs<AddItemUiState.Reviewing>(viewModel.uiState.value)
        assertEquals(3, state.photos.size)
        assertNull(state.activeCropPhotoId)
        assertTrue(state.photos.none { it.isCropped })
    }

    @Test
    fun `tapping crop then closing without confirming leaves that photo uncropped`() {
        val viewModel = AddItemViewModel(FakeAddItemRepository(), FakePlanLimitNavigator())
        viewModel.onImagesSelected(listOf(pickedImage("a"), pickedImage("b")))
        val photoId = (viewModel.uiState.value as AddItemUiState.Reviewing).photos.first().id

        viewModel.openCrop(photoId)
        assertEquals(photoId, (viewModel.uiState.value as AddItemUiState.Reviewing).activeCropPhotoId)

        viewModel.closeCrop()
        val state = viewModel.uiState.value as AddItemUiState.Reviewing
        assertNull(state.activeCropPhotoId)
        assertTrue(state.photos.none { it.isCropped })
    }

    @Test
    fun `confirming a crop marks only that photo as cropped and returns to the grid`() = runTest {
        val viewModel = AddItemViewModel(FakeAddItemRepository(), FakePlanLimitNavigator())
        viewModel.onImagesSelected(listOf(pickedImage("a"), pickedImage("b")))
        val photos = (viewModel.uiState.value as AddItemUiState.Reviewing).photos
        val (croppedId, untouchedId) = photos[0].id to photos[1].id

        viewModel.openCrop(croppedId)
        viewModel.applyCrop(croppedId, BoundingBox(x = 0.1f, y = 0.1f, width = 0.5f, height = 0.5f))

        // Rasterizing the crop genuinely hops onto Dispatchers.Default (a real background
        // thread, not the test's virtual-time scheduler), so wait for it to actually land
        // instead of reading the StateFlow's current value immediately. The rasterized bytes
        // themselves aren't asserted on here: cropToSquareJpeg's Android implementation calls
        // android.graphics.BitmapFactory, which — like android.util.Log elsewhere in this test
        // target — is an unmocked stub that throws regardless of input, the same pre-existing
        // limitation documented on the WardrobeViewModel tests. isCropped reflects the user's
        // crop choice independent of whether rasterization itself succeeds.
        val state = viewModel.uiState.first { it is AddItemUiState.Reviewing && it.activeCropPhotoId == null }
            as AddItemUiState.Reviewing

        assertTrue(state.photos.first { it.id == croppedId }.isCropped)
        assertTrue(state.photos.first { it.id == untouchedId }.isCropped.not())
    }

    @Test
    fun `removing a photo drops it from the batch but keeps the review grid open`() {
        val viewModel = AddItemViewModel(FakeAddItemRepository(), FakePlanLimitNavigator())
        viewModel.onImagesSelected(listOf(pickedImage("a"), pickedImage("b"), pickedImage("c")))
        val photos = (viewModel.uiState.value as AddItemUiState.Reviewing).photos
        val (removedId, keptIds) = photos[0].id to photos.drop(1).map { it.id }

        viewModel.removePhoto(removedId)

        val state = assertIs<AddItemUiState.Reviewing>(viewModel.uiState.value)
        assertEquals(keptIds, state.photos.map { it.id })
    }

    @Test
    fun `removing the last remaining photo closes the review screen entirely`() {
        val viewModel = AddItemViewModel(FakeAddItemRepository(), FakePlanLimitNavigator())
        viewModel.onImagesSelected(listOf(pickedImage("a"), pickedImage("b")))
        val photos = (viewModel.uiState.value as AddItemUiState.Reviewing).photos

        viewModel.removePhoto(photos[0].id)
        assertIs<AddItemUiState.Reviewing>(viewModel.uiState.value)

        viewModel.removePhoto(photos[1].id)
        assertEquals(AddItemUiState.Initial, viewModel.uiState.value)
    }

    @Test
    fun `removing the photo whose crop editor is open also closes the crop editor`() {
        val viewModel = AddItemViewModel(FakeAddItemRepository(), FakePlanLimitNavigator())
        viewModel.onImagesSelected(listOf(pickedImage("a"), pickedImage("b")))
        val photos = (viewModel.uiState.value as AddItemUiState.Reviewing).photos
        viewModel.openCrop(photos[0].id)

        viewModel.removePhoto(photos[0].id)

        val state = assertIs<AddItemUiState.Reviewing>(viewModel.uiState.value)
        assertNull(state.activeCropPhotoId)
        assertEquals(listOf(photos[1].id), state.photos.map { it.id })
    }

    @Test
    fun `confirming a crop that matches the full image is treated as no crop`() {
        val viewModel = AddItemViewModel(FakeAddItemRepository(), FakePlanLimitNavigator())
        viewModel.onImagesSelected(listOf(pickedImage("only")))
        val photoId = (viewModel.uiState.value as AddItemUiState.Reviewing).photos.first().id

        viewModel.applyCrop(photoId, BoundingBox.FullImage)

        val state = viewModel.uiState.value as AddItemUiState.Reviewing
        assertTrue(state.photos.first().isCropped.not())
    }

    @Test
    fun `continuing without cropping uploads the original bytes untouched`() = runTest {
        val repository = FakeAddItemRepository()
        val viewModel = AddItemViewModel(repository, FakePlanLimitNavigator())
        val original = pickedImage("original-bytes")
        viewModel.onImagesSelected(listOf(original))

        viewModel.continueUploading()

        assertEquals(1, repository.uploadedBytes.size)
        assertTrue(repository.uploadedBytes.single().second.contentEquals(minimalJpegBytes))
        assertEquals(AddItemUiState.Initial, viewModel.uiState.value)
        assertEquals(1, repository.analyzedImageIds.size)
    }

    @Test
    fun `each photo is uploaded exactly once even when several are selected`() = runTest {
        val repository = FakeAddItemRepository()
        val viewModel = AddItemViewModel(repository, FakePlanLimitNavigator())
        viewModel.onImagesSelected(listOf(pickedImage("a"), pickedImage("b"), pickedImage("c")))

        viewModel.continueUploading()

        assertEquals(3, repository.createdImages.size)
        assertEquals(3, repository.uploadedBytes.size)
        assertEquals(repository.createdImages.map { it.imageId }.toSet(), repository.uploadedBytes.map { it.first }.toSet())
        assertEquals(3, repository.analyzedImageIds.size)
    }

    // A failure-path test for uploadPhotos's .onFailure branch (verifying the failed photo is
    // offered for retry with its crop choice preserved, and never analyzed) is intentionally
    // not included: it calls ErrorMapper.toUserMessage(), which throws via AppLogger.e()
    // (unmocked android.util.Log) in this JVM test target — the same pre-existing limitation
    // documented in WardrobeViewModelPollingTest/WardrobeViewModelBulkDeleteTest.

    @Test
    fun `selecting more than five photos emits a selection limit effect and does not enter review`() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = AddItemViewModel(FakeAddItemRepository(), FakePlanLimitNavigator())
            val images = (1..6).map { pickedImage("photo-$it") }

            var emitted = false
            val job = launch { viewModel.effects.collect { if (it is AddItemEffect.SelectionLimitExceeded) emitted = true } }

            viewModel.onImagesSelected(images)

            assertEquals(AddItemUiState.Initial, viewModel.uiState.value)
            assertTrue(emitted)
            job.cancel()
        }
}
