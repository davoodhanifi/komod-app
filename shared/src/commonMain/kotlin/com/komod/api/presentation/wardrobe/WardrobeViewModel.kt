package com.komod.api.presentation.wardrobe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.core.error.AppLogger
import com.komod.api.core.error.ErrorMapper
import com.komod.api.data.api.model.ImageStatus
import com.komod.api.data.repository.AddItemRepository
import com.komod.api.data.repository.WardrobeRepository
import com.komod.api.domain.model.WardrobeItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

sealed interface WardrobeEffect {
    data class ShowMessage(val message: String) : WardrobeEffect
}

private const val UploadPollIntervalMs = 5_000L
private const val WardrobePageSize = 20

internal const val AllCategoriesLabel = "All"

class WardrobeViewModel(
    private val wardrobeRepository: WardrobeRepository,
    private val addItemRepository: AddItemRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<WardrobeUiState>(WardrobeUiState.Loading)
    val uiState: StateFlow<WardrobeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _recentUploads = MutableStateFlow<List<RecentUploadUi>>(emptyList())
    val recentUploads: StateFlow<List<RecentUploadUi>> = _recentUploads.asStateFlow()

    private val _selectedCategory = MutableStateFlow(AllCategoriesLabel)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // The upload pending a delete confirmation (long-pressed, dialog visible); null
    // means no dialog is showing.
    private val _pendingDeleteUploadId = MutableStateFlow<String?>(null)
    val pendingDeleteUploadId: StateFlow<String?> = _pendingDeleteUploadId.asStateFlow()

    private val _deletingUploadIds = MutableStateFlow<Set<String>>(emptySet())
    val deletingUploadIds: StateFlow<Set<String>> = _deletingUploadIds.asStateFlow()

    private val _effects = MutableSharedFlow<WardrobeEffect>()
    val effects: SharedFlow<WardrobeEffect> = _effects.asSharedFlow()

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    private val _selectedItemIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedItemIds: StateFlow<Set<String>> = _selectedItemIds.asStateFlow()

    private val _isBulkDeleting = MutableStateFlow(false)
    val isBulkDeleting: StateFlow<Boolean> = _isBulkDeleting.asStateFlow()

    private val _bulkDeleteConfirmVisible = MutableStateFlow(false)
    val bulkDeleteConfirmVisible: StateFlow<Boolean> = _bulkDeleteConfirmVisible.asStateFlow()

    // Keyed by storage path (not imageId) so a path change from the backend
    // is picked up as a cache miss and re-resolved into a fresh signed URL.
    private val thumbnailUrlCache = mutableMapOf<String, String?>()

    // Number of pages already fetched into the current Success.items list, and whether
    // the backend reported more pages beyond that. Reset to 0/false on every full reload
    // (loadItems/refresh) and advanced by one on each successful loadMoreItems().
    private var loadedPageCount = 0
    private var hasNextPage = false
    private var isLoadingNextPage = false

    init {
        loadItems()
        observeUploadedImages()
        viewModelScope.launch { refreshUploadedImages() }
        pollWhileUploadsActive()
    }

    private fun observeUploadedImages() {
        viewModelScope.launch {
            addItemRepository.uploadedImages.collect { uploadedImages ->
                _recentUploads.value = uploadedImages.map { image ->
                    val thumbnailUrl = thumbnailUrlCache.getOrPut(image.thumbnailStoragePath) {
                        addItemRepository.getThumbnailUrl(image.thumbnailStoragePath)
                    }
                    RecentUploadUi(imageId = image.imageId, thumbnailUrl = thumbnailUrl, status = image.status)
                }
            }
        }
    }

    // Polls GET /images/uploaded every 5s while any image is still Pending/Processing,
    // and automatically stops as soon as every image reaches a terminal status.
    private fun pollWhileUploadsActive() {
        viewModelScope.launch {
            addItemRepository.uploadedImages
                .map { images -> images.any { it.status == ImageStatus.Pending || it.status == ImageStatus.Processing } }
                .distinctUntilChanged()
                .collectLatest { hasActiveUploads ->
                    if (!hasActiveUploads) return@collectLatest
                    while (true) {
                        delay(UploadPollIntervalMs)
                        refreshUploadedImages()
                    }
                }
        }
    }

    fun loadItems() {
        viewModelScope.launch {
            _uiState.value = WardrobeUiState.Loading
            fetchFirstPage()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchFirstPage()
            refreshUploadedImages()
            _isRefreshing.value = false
        }
    }

    // Called by the screen as the grid scrolls near the end. No-op while a page is
    // already loading or the backend reported no further pages.
    fun loadMoreItems() {
        if (isLoadingNextPage || !hasNextPage) return
        val current = _uiState.value
        if (current !is WardrobeUiState.Success) return

        isLoadingNextPage = true
        _uiState.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            runCatching {
                wardrobeRepository.getWardrobeItems(pageNumber = loadedPageCount + 1, pageSize = WardrobePageSize)
            }
                .onSuccess { page ->
                    loadedPageCount += 1
                    hasNextPage = page.hasNextPage
                    val existing = (_uiState.value as? WardrobeUiState.Success)?.items.orEmpty()
                    _uiState.value = WardrobeUiState.Success(items = existing + page.items, isLoadingMore = false)
                }
                .onFailure { error ->
                    (_uiState.value as? WardrobeUiState.Success)?.let {
                        _uiState.value = it.copy(isLoadingMore = false)
                    }
                    _effects.emit(
                        WardrobeEffect.ShowMessage(
                            ErrorMapper.toUserMessage(error, tag = "WardrobeViewModel"),
                        ),
                    )
                }
            isLoadingNextPage = false
        }
    }

    // Called every time the Wardrobe screen (re)enters composition — including
    // returning from Add Item — independent of the nav-level savedStateHandle signal
    // that also triggers refresh(). Deliberately narrow (just the uploads list, not the
    // full wardrobe-items fetch) so it's cheap enough to fire on every entry as a
    // guaranteed, low-risk backstop.
    fun refreshRecentUploads() {
        viewModelScope.launch { refreshUploadedImages() }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun requestDeleteUpload(imageId: String) {
        _pendingDeleteUploadId.value = imageId
    }

    fun dismissDeleteUpload() {
        val imageId = _pendingDeleteUploadId.value ?: return
        if (imageId in _deletingUploadIds.value) return
        _pendingDeleteUploadId.value = null
    }

    fun confirmDeleteUpload() {
        val imageId = _pendingDeleteUploadId.value ?: return
        if (imageId in _deletingUploadIds.value) return

        _deletingUploadIds.value += imageId
        viewModelScope.launch {
            runCatching { addItemRepository.deleteUploadedImage(imageId) }
                .onSuccess {
                    _deletingUploadIds.value -= imageId
                    _pendingDeleteUploadId.value = null
                }
                .onFailure { error ->
                    _deletingUploadIds.value -= imageId
                    _pendingDeleteUploadId.value = null
                    _effects.emit(
                        WardrobeEffect.ShowMessage(
                            ErrorMapper.toUserMessage(error, tag = "WardrobeViewModel"),
                        ),
                    )
                    // Covers the race where the image finished analyzing (or was already
                    // deleted) between the long-press and this call — refetch so the
                    // Upload Queue reflects reality instead of a stale, now-wrong tile.
                    refreshUploadedImages()
                }
        }
    }

    fun enterSelectionMode() {
        _selectionMode.value = true
    }

    fun exitSelectionMode() {
        if (_isBulkDeleting.value) return
        _selectionMode.value = false
        _selectedItemIds.value = emptySet()
        _bulkDeleteConfirmVisible.value = false
    }

    fun toggleItemSelection(itemId: String) {
        val current = _selectedItemIds.value
        _selectedItemIds.value = if (itemId in current) current - itemId else current + itemId
    }

    fun requestBulkDelete() {
        if (_selectedItemIds.value.isEmpty() || _isBulkDeleting.value) return
        _bulkDeleteConfirmVisible.value = true
    }

    fun dismissBulkDeleteConfirm() {
        if (_isBulkDeleting.value) return
        _bulkDeleteConfirmVisible.value = false
    }

    fun confirmBulkDelete() {
        if (_isBulkDeleting.value) return
        val ids = _selectedItemIds.value
        if (ids.isEmpty()) return

        _isBulkDeleting.value = true
        viewModelScope.launch {
            runCatching { wardrobeRepository.deleteWardrobeItems(ids.toList()) }
                .onSuccess {
                    val current = _uiState.value
                    if (current is WardrobeUiState.Success) {
                        _uiState.value = current.copy(items = current.items.filterNot { it.id in ids })
                    }
                    _isBulkDeleting.value = false
                    _bulkDeleteConfirmVisible.value = false
                    _selectionMode.value = false
                    _selectedItemIds.value = emptySet()
                    // Quiet reconciliation with the backend — reloadLoadedPages() doesn't
                    // touch Loading/isRefreshing, so this can't flash the skeleton or
                    // disrupt the grid the user is already looking at.
                    reloadLoadedPages()
                }
                .onFailure { error ->
                    // Selection and selected items are left untouched so the user can
                    // retry via the same Delete action, matching how confirmDeleteUpload
                    // handles failure above.
                    _isBulkDeleting.value = false
                    _bulkDeleteConfirmVisible.value = false
                    _effects.emit(
                        WardrobeEffect.ShowMessage(
                            ErrorMapper.toUserMessage(error, tag = "WardrobeViewModel"),
                        ),
                    )
                }
        }
    }

    private suspend fun fetchFirstPage() {
        runCatching { wardrobeRepository.getWardrobeItems(pageNumber = 1, pageSize = WardrobePageSize) }
            .onSuccess { page ->
                loadedPageCount = 1
                hasNextPage = page.hasNextPage
                _uiState.value = WardrobeUiState.Success(items = page.items)
            }
            .onFailure { error ->
                loadedPageCount = 0
                hasNextPage = false
                _uiState.value = WardrobeUiState.Error(
                    message = ErrorMapper.toUserMessage(error, tag = "WardrobeViewModel"),
                )
            }
    }

    // Re-fetches exactly the pages already loaded (1..loadedPageCount) so a mutation
    // like bulk delete reconciles with the backend without collapsing the user's scroll
    // position back down to a single page. Best-effort: the optimistic local filter in
    // confirmBulkDelete already reflects the mutation, so a failure here is silently
    // ignored rather than surfaced as an error state.
    private suspend fun reloadLoadedPages() {
        val pagesToReload = maxOf(loadedPageCount, 1)
        val aggregated = mutableListOf<WardrobeItem>()
        var latestHasNextPage = false
        for (page in 1..pagesToReload) {
            val result = runCatching {
                wardrobeRepository.getWardrobeItems(pageNumber = page, pageSize = WardrobePageSize)
            }.getOrNull() ?: return
            aggregated += result.items
            latestHasNextPage = result.hasNextPage
            if (!result.hasNextPage) break
        }
        loadedPageCount = pagesToReload
        hasNextPage = latestHasNextPage
        _uiState.value = WardrobeUiState.Success(items = aggregated)
    }

    // Best-effort: the wardrobe grid's own loading/error state must not depend on this,
    // and a transient failure (network blip, or a malformed response) must not mark any
    // upload Failed — only an explicit Status == Failed from the backend does that. The
    // failure is still logged, though, so it doesn't silently disappear; polling retries
    // on the next tick regardless.
    private suspend fun refreshUploadedImages() {
        runCatching { addItemRepository.refreshUploadedImages() }
            .onFailure { error ->
                // A logging failure must never propagate out of here — this runs inside
                // the poll loop, and an uncaught exception here would silently kill it.
                runCatching { AppLogger.e("WardrobeViewModel", "Failed to refresh uploaded images", error) }
            }
    }
}
