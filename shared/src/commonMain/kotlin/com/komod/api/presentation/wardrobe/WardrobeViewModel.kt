package com.komod.api.presentation.wardrobe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.core.error.AppLogger
import com.komod.api.core.error.ErrorMapper
import com.komod.api.data.api.model.ImageStatus
import com.komod.api.data.repository.AddItemRepository
import com.komod.api.data.repository.WardrobeRepository
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
            fetchItems()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchItems()
            refreshUploadedImages()
            _isRefreshing.value = false
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
                    // Quiet reconciliation with the backend — fetchItems() doesn't touch
                    // Loading/isRefreshing, so this can't flash the skeleton or disrupt
                    // the grid the user is already looking at.
                    fetchItems()
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

    private suspend fun fetchItems() {
        runCatching { wardrobeRepository.getWardrobeItems() }
            .onSuccess { items -> _uiState.value = WardrobeUiState.Success(items) }
            .onFailure { error ->
                _uiState.value = WardrobeUiState.Error(
                    message = ErrorMapper.toUserMessage(error, tag = "WardrobeViewModel"),
                )
            }
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
