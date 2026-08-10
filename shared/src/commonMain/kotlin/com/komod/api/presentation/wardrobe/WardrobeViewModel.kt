package com.komod.api.presentation.wardrobe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.core.error.AppLogger
import com.komod.api.core.error.ErrorMapper
import com.komod.api.data.api.model.ImageStatus
import com.komod.api.data.repository.AddItemRepository
import com.komod.api.data.repository.WardrobeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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
