package com.komod.api.presentation.wardrobe

import androidx.lifecycle.viewModelScope
import com.komod.api.data.api.model.CreateImageResponse
import com.komod.api.data.repository.AddItemRepository
import com.komod.api.data.repository.WardrobeRepository
import com.komod.api.domain.model.UploadedImage
import com.komod.api.domain.model.WardrobeItem
import com.komod.api.domain.model.WardrobeItemsPage
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

// Mimics the backend's real pagination contract: pageNumber/pageSize slice allItems and
// hasNextPage reflects whether the slice reached the end; omitting both params (null,
// null) returns everything in one page, matching the pre-pagination behavior.
private class FakePaginatedWardrobeRepository(
    private val allItems: List<WardrobeItem>,
) : WardrobeRepository {
    val calls = mutableListOf<Pair<Int?, Int?>>()
    var errorOnPage: Int? = null

    override suspend fun getWardrobeItems(pageNumber: Int?, pageSize: Int?): WardrobeItemsPage {
        calls += pageNumber to pageSize
        if (pageNumber != null && pageNumber == errorOnPage) {
            throw RuntimeException("boom")
        }
        if (pageNumber == null || pageSize == null) {
            return WardrobeItemsPage(items = allItems, hasNextPage = false, totalCount = allItems.size)
        }
        val startIndex = (pageNumber - 1) * pageSize
        val page = allItems.drop(startIndex).take(pageSize)
        val hasNext = startIndex + pageSize < allItems.size
        return WardrobeItemsPage(items = page, hasNextPage = hasNext, totalCount = allItems.size)
    }

    override suspend fun deleteWardrobeItems(ids: List<String>) = error("not used by these tests")
}

// A page(1, 20) helper so expected-call assertions line up with FakePaginatedWardrobeRepository's
// Pair<Int?, Int?> calls list without needing an explicit type argument on every listOf(...).
private fun page(pageNumber: Int, pageSize: Int): Pair<Int?, Int?> = pageNumber to pageSize

// Same reasoning as the NoOpAddItemRepository in WardrobeViewModelBulkDeleteTest: every
// method here is unused or a trivial stand-in so WardrobeViewModel's init{} (which
// observes and polls uploads) has something safe to collect. Named distinctly (not
// NoOpAddItemRepository) because a private top-level class isn't name-mangled per file —
// unlike private top-level functions — so it would otherwise collide at the JVM class
// level with the same-named private class in WardrobeViewModelBulkDeleteTest.kt.
private class PaginationNoOpAddItemRepository : AddItemRepository {
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

class WardrobeViewModelPaginationTest {
    private val testDispatcher = StandardTestDispatcher()

    // See WardrobeViewModelBulkDeleteTest for why created ViewModels must be cancelled:
    // init{}'s pollWhileUploadsActive() starts an unbounded collectLatest that would
    // otherwise leak across tests sharing Dispatchers.Main.
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

    private fun createViewModel(wardrobeRepository: WardrobeRepository): WardrobeViewModel {
        val viewModel = WardrobeViewModel(wardrobeRepository, PaginationNoOpAddItemRepository())
        createdViewModels += viewModel
        return viewModel
    }

    @Test
    fun `loadItems fetches only the first page of 20 on init`() = runTest(testDispatcher) {
        val repository = FakePaginatedWardrobeRepository((1..45).map { wardrobeItem("item-$it") })
        val viewModel = createViewModel(repository)
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is WardrobeUiState.Success)
        assertEquals(20, state.items.size)
        assertEquals(listOf(page(1, 20)), repository.calls)
    }

    @Test
    fun `loadMoreItems appends the next page and keeps requesting pageSize 20`() = runTest(testDispatcher) {
        val repository = FakePaginatedWardrobeRepository((1..45).map { wardrobeItem("item-$it") })
        val viewModel = createViewModel(repository)
        runCurrent()

        viewModel.loadMoreItems()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is WardrobeUiState.Success)
        assertEquals(40, state.items.size)
        assertEquals(false, state.isLoadingMore)
        assertEquals(listOf(page(1, 20), page(2, 20)), repository.calls)
    }

    @Test
    fun `loadMoreItems sets isLoadingMore synchronously before the page resolves`() = runTest(testDispatcher) {
        val repository = FakePaginatedWardrobeRepository((1..45).map { wardrobeItem("item-$it") })
        val viewModel = createViewModel(repository)
        runCurrent()

        viewModel.loadMoreItems()
        // Deliberately not calling runCurrent() yet: the isLoadingMore flag flips
        // synchronously on the caller's stack, before the launched coroutine runs.
        val state = viewModel.uiState.value
        assertTrue(state is WardrobeUiState.Success)
        assertEquals(true, state.isLoadingMore)
        assertEquals(20, state.items.size) // page 2 hasn't landed yet

        runCurrent()
    }

    @Test
    fun `loadMoreItems is a no-op once hasNextPage is false`() = runTest(testDispatcher) {
        val repository = FakePaginatedWardrobeRepository((1..10).map { wardrobeItem("item-$it") })
        val viewModel = createViewModel(repository)
        runCurrent()

        viewModel.loadMoreItems()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is WardrobeUiState.Success)
        assertEquals(10, state.items.size)
        // Only the init page-1 fetch happened — loadMoreItems bailed out before calling
        // the repository again.
        assertEquals(listOf(page(1, 20)), repository.calls)
    }

    @Test
    fun `a second loadMoreItems call while one is already in flight is ignored`() = runTest(testDispatcher) {
        val repository = FakePaginatedWardrobeRepository((1..45).map { wardrobeItem("item-$it") })
        val viewModel = createViewModel(repository)
        runCurrent()

        viewModel.loadMoreItems()
        viewModel.loadMoreItems() // still in flight — must be dropped
        runCurrent()

        assertEquals(listOf(page(1, 20), page(2, 20)), repository.calls)
    }

    // A failure-path test for loadMoreItems (preserving existing items and clearing
    // isLoadingMore on error) is intentionally not included: its onFailure branch calls
    // ErrorMapper.toUserMessage(), which throws via AppLogger.e() (unmocked
    // android.util.Log) in this JVM test target — the same pre-existing limitation
    // documented in WardrobeViewModelPollingTest and WardrobeViewModelBulkDeleteTest.

    @Test
    fun `refresh resets pagination back to page 1`() = runTest(testDispatcher) {
        val repository = FakePaginatedWardrobeRepository((1..45).map { wardrobeItem("item-$it") })
        val viewModel = createViewModel(repository)
        runCurrent()

        viewModel.loadMoreItems()
        runCurrent()
        assertEquals(40, (viewModel.uiState.value as WardrobeUiState.Success).items.size)

        viewModel.refresh()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state is WardrobeUiState.Success)
        assertEquals(20, state.items.size)

        // A subsequent loadMoreItems should fetch page 2 again, not page 3 — confirming
        // loadedPageCount was reset by refresh(), not left at 2.
        viewModel.loadMoreItems()
        runCurrent()
        assertEquals(page(2, 20), repository.calls.last())
    }
}
