package com.komod.api.presentation.wardrobe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private suspend fun fetchItems() {
        runCatching { wardrobeRepository.getWardrobeItems() }
            .onSuccess { items -> _uiState.value = WardrobeUiState.Success(items) }
            .onFailure { error ->
                _uiState.value = WardrobeUiState.Error(
                    message = ErrorMapper.toUserMessage(error, tag = "WardrobeViewModel"),
                )
            }
    }

    // Best-effort: the wardrobe grid's own loading/error state must not depend on this.
    private suspend fun refreshUploadedImages() {
        runCatching { addItemRepository.refreshUploadedImages() }
    }
}
