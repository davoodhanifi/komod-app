package com.komod.api.presentation.wardrobe

import androidx.lifecycle.viewModelScope
import com.komod.api.data.api.model.CreateImageResponse
import com.komod.api.data.repository.AddItemRepository
import com.komod.api.data.repository.WardrobeRepository
import com.komod.api.domain.model.UploadedImage
import com.komod.api.domain.model.WardrobeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun wardrobeItem(id: String) = WardrobeItem(
    id = id,
    imageId = "image-$id",
    itemName = "Item $id",
    category = "shirt",
    subcategory = null,
    primaryColor = null,
    dominantColorHex = null,
    style = null,
    season = null,
    occasion = null,
    material = null,
    formality = null,
    isFavorite = false,
    imageUrl = null,
    createdAt = "2026-01-01T00:00:00Z",
)

private class FakeBulkDeleteWardrobeRepository(initialItems: List<WardrobeItem>) : WardrobeRepository {
    var items: List<WardrobeItem> = initialItems
    var deleteError: Throwable? = null
    val deleteCalls = mutableListOf<List<String>>()
    var getWardrobeItemsCallCount = 0
        private set

    override suspend fun getWardrobeItems(): List<WardrobeItem> {
        getWardrobeItemsCallCount++
        return items
    }

    override suspend fun deleteWardrobeItems(ids: List<String>) {
        deleteCalls += ids
        deleteError?.let { throw it }
        items = items.filterNot { it.id in ids }
    }
}

// No uploads involved in these tests — every method here is either unused or a
// trivial no-op/empty-flow stand-in so WardrobeViewModel's init{} (which observes and
// polls uploads) has something safe to collect.
private class NoOpAddItemRepository : AddItemRepository {
    override val uploadedImages: StateFlow<List<UploadedImage>> = MutableStateFlow(emptyList<UploadedImage>()).asStateFlow()

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
    override fun removeUploadedImage(imageId: String) = Unit
    override suspend fun deleteUploadedImage(imageId: String) = error("not used by these tests")
    override suspend fun getThumbnailUrl(storagePath: String): String? = null
    override suspend fun refreshUploadedImages() = Unit
}

class WardrobeViewModelBulkDeleteTest {
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
    fun `entering selection mode and toggling items tracks the selection`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            FakeBulkDeleteWardrobeRepository(listOf(wardrobeItem("a"), wardrobeItem("b"))),
            NoOpAddItemRepository(),
        )
        runCurrent()

        assertEquals(false, viewModel.selectionMode.value)

        viewModel.enterSelectionMode()
        assertEquals(true, viewModel.selectionMode.value)
        assertEquals(emptySet(), viewModel.selectedItemIds.value)

        viewModel.toggleItemSelection("a")
        assertEquals(setOf("a"), viewModel.selectedItemIds.value)

        viewModel.toggleItemSelection("b")
        assertEquals(setOf("a", "b"), viewModel.selectedItemIds.value)

        viewModel.toggleItemSelection("a")
        assertEquals(setOf("b"), viewModel.selectedItemIds.value)
    }

    @Test
    fun `cancelling selection mode clears the selection`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            FakeBulkDeleteWardrobeRepository(listOf(wardrobeItem("a"))),
            NoOpAddItemRepository(),
        )
        runCurrent()

        viewModel.enterSelectionMode()
        viewModel.toggleItemSelection("a")
        viewModel.exitSelectionMode()

        assertEquals(false, viewModel.selectionMode.value)
        assertEquals(emptySet(), viewModel.selectedItemIds.value)
    }

    @Test
    fun `requestBulkDelete is a no-op with nothing selected`() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            FakeBulkDeleteWardrobeRepository(listOf(wardrobeItem("a"))),
            NoOpAddItemRepository(),
        )
        runCurrent()

        viewModel.enterSelectionMode()
        viewModel.requestBulkDelete()

        assertEquals(false, viewModel.bulkDeleteConfirmVisible.value)
    }

    @Test
    fun `confirming a bulk delete calls the repository once with all selected ids, removes them locally, and exits selection mode`() =
        runTest(testDispatcher) {
            val repository = FakeBulkDeleteWardrobeRepository(listOf(wardrobeItem("a"), wardrobeItem("b"), wardrobeItem("c")))
            val viewModel = createViewModel(repository, NoOpAddItemRepository())
            runCurrent()

            viewModel.enterSelectionMode()
            viewModel.toggleItemSelection("a")
            viewModel.toggleItemSelection("b")
            viewModel.requestBulkDelete()
            assertEquals(true, viewModel.bulkDeleteConfirmVisible.value)

            viewModel.confirmBulkDelete()
            runCurrent()

            // Exactly one bulk call, never the single-item endpoint repeated.
            assertEquals(1, repository.deleteCalls.size)
            assertEquals(setOf("a", "b"), repository.deleteCalls.single().toSet())

            val state = viewModel.uiState.value
            assertTrue(state is WardrobeUiState.Success)
            assertEquals(listOf("c"), state.items.map { it.id })

            assertEquals(false, viewModel.selectionMode.value)
            assertEquals(emptySet(), viewModel.selectedItemIds.value)
            assertEquals(false, viewModel.bulkDeleteConfirmVisible.value)
            assertEquals(false, viewModel.isBulkDeleting.value)

            // Reconciliation refetch happened (init's + this one).
            assertEquals(2, repository.getWardrobeItemsCallCount)
        }

    // A failure-path test for confirmBulkDelete's .onFailure branch is intentionally not
    // included: it calls ErrorMapper.toUserMessage(), which throws via AppLogger.e()
    // (unmocked android.util.Log) in this JVM test target — the same pre-existing
    // limitation documented in WardrobeViewModelPollingTest, which this branch also hits.

    @Test
    fun `duplicate ids in the selection are sent only once`() = runTest(testDispatcher) {
        val repository = FakeBulkDeleteWardrobeRepository(listOf(wardrobeItem("a")))
        val viewModel = createViewModel(repository, NoOpAddItemRepository())
        runCurrent()

        viewModel.enterSelectionMode()
        viewModel.toggleItemSelection("a")
        viewModel.toggleItemSelection("a") // deselect
        viewModel.toggleItemSelection("a") // reselect — still just one id in the Set
        viewModel.confirmBulkDelete()
        runCurrent()

        assertEquals(listOf("a"), repository.deleteCalls.single())
    }
}
