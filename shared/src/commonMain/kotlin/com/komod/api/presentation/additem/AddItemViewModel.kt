package com.komod.api.presentation.additem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.core.error.ErrorContext
import com.komod.api.core.error.ErrorMapper
import com.komod.api.data.repository.AddItemRepository
import com.komod.api.domain.model.BoundingBox
import com.komod.api.platform.PickedImage
import com.komod.api.platform.cropToSquareJpeg
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val MaxPhotosPerUpload = 5

class AddItemViewModel(
    private val addItemRepository: AddItemRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AddItemUiState>(AddItemUiState.Initial)
    val uiState: StateFlow<AddItemUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AddItemEffect>()
    val effects: SharedFlow<AddItemEffect> = _effects.asSharedFlow()

    fun onImagesSelected(images: List<PickedImage>) {
        if (images.isEmpty()) return

        if (images.size > MaxPhotosPerUpload) {
            viewModelScope.launch { _effects.emit(AddItemEffect.SelectionLimitExceeded) }
            return
        }

        val photos = images.mapIndexed { index, image -> ReviewPhoto(id = "photo_$index", original = image) }

        // A single photo skips the review grid entirely and opens straight into the crop
        // editor — there's nothing to "review" when there's only one photo to look at.
        _uiState.value = AddItemUiState.Reviewing(
            photos = photos,
            activeCropPhotoId = if (photos.size == 1) photos.first().id else null,
        )
    }

    fun openCrop(photoId: String) {
        val state = _uiState.value as? AddItemUiState.Reviewing ?: return
        _uiState.value = state.copy(activeCropPhotoId = photoId)
    }

    /** Dismisses the crop editor without changing that photo's crop. */
    fun closeCrop() {
        val state = _uiState.value as? AddItemUiState.Reviewing ?: return
        _uiState.value = state.copy(activeCropPhotoId = null)
    }

    /**
     * Confirms a crop from the review grid. Rasterizes it before returning to the grid, so the
     * tile the user lands back on already shows the real cropped image instead of a placeholder.
     */
    fun applyCrop(photoId: String, boundingBox: BoundingBox) {
        val state = _uiState.value as? AddItemUiState.Reviewing ?: return
        val photo = state.photos.firstOrNull { it.id == photoId } ?: return

        if (boundingBox == BoundingBox.FullImage) {
            val updatedPhotos = state.photos.map { p ->
                if (p.id == photoId) p.copy(cropBox = null, croppedBytes = null) else p
            }
            _uiState.value = state.copy(photos = updatedPhotos, activeCropPhotoId = null, savingCropPhotoId = null)
            return
        }

        _uiState.value = state.copy(savingCropPhotoId = photoId)
        viewModelScope.launch {
            val croppedBytes = computeCroppedBytes(photo, boundingBox)
            val current = _uiState.value as? AddItemUiState.Reviewing ?: return@launch
            val updatedPhotos = current.photos.map { p ->
                if (p.id == photoId) p.copy(cropBox = boundingBox, croppedBytes = croppedBytes) else p
            }
            _uiState.value = current.copy(photos = updatedPhotos, activeCropPhotoId = null, savingCropPhotoId = null)
        }
    }

    /** Single-photo flow: rasterize the crop (if any), then go straight to uploading. */
    fun confirmCropAndContinue(photoId: String, boundingBox: BoundingBox) {
        val state = _uiState.value as? AddItemUiState.Reviewing ?: return
        val photo = state.photos.firstOrNull { it.id == photoId } ?: return

        _uiState.value = state.copy(savingCropPhotoId = photoId)
        viewModelScope.launch {
            val croppedBytes = computeCroppedBytes(photo, boundingBox)
            val cropBox = boundingBox.takeIf { it != BoundingBox.FullImage }
            uploadPhotos(listOf(photo.copy(cropBox = cropBox, croppedBytes = croppedBytes)))
        }
    }

    /** Removes a photo from the pending upload batch. Emptying it out cancels the whole flow. */
    fun removePhoto(photoId: String) {
        val state = _uiState.value as? AddItemUiState.Reviewing ?: return
        val remainingPhotos = state.photos.filterNot { it.id == photoId }

        _uiState.value = if (remainingPhotos.isEmpty()) {
            AddItemUiState.Initial
        } else {
            state.copy(
                photos = remainingPhotos,
                activeCropPhotoId = state.activeCropPhotoId.takeIf { id -> remainingPhotos.any { it.id == id } },
            )
        }
    }

    fun continueUploading() {
        val state = _uiState.value as? AddItemUiState.Reviewing ?: return
        uploadPhotos(state.photos)
    }

    fun retryFailed() {
        val failedPhotos = (_uiState.value as? AddItemUiState.Error)?.failedPhotos ?: return
        uploadPhotos(failedPhotos)
    }

    fun reset() {
        _uiState.value = AddItemUiState.Initial
    }

    private fun uploadPhotos(photos: List<ReviewPhoto>) {
        viewModelScope.launch {
            val total = photos.size
            val failed = mutableListOf<ReviewPhoto>()
            var completedCount = 0

            fun emitProgress(currentFraction: Float) {
                _uiState.value = AddItemUiState.Uploading(
                    total = total,
                    completed = completedCount,
                    progress = ((completedCount + currentFraction) / total).coerceIn(0f, 1f),
                )
            }

            emitProgress(0f)

            for (photo in photos) {
                val uploaded = runCatching {
                    val (bytes, mimeType) = resolveUploadBytes(photo)
                    val imageInfo = addItemRepository.createImage()

                    // Success is determined by the storage upload alone — analysis runs
                    // asynchronously on the backend and must not block or fail this upload.
                    addItemRepository.uploadImage(imageInfo, bytes, mimeType) { fraction ->
                        emitProgress(fraction.coerceIn(0f, 1f))
                    }

                    // Persist locally so the Upload Queue can show this image right away.
                    addItemRepository.saveUploadedImage(imageInfo)

                    // Fire-and-forget on a scope that outlives this screen: uploading
                    // navigates back immediately, which would otherwise cancel this
                    // request mid-flight if it were tied to viewModelScope.
                    addItemRepository.triggerAnalysisInBackground(imageInfo.imageId)
                }.onFailure { error ->
                    ErrorMapper.toUserMessage(error, tag = "AddItemViewModel", context = ErrorContext.Upload)
                }.isSuccess

                if (!uploaded) failed += photo
                completedCount += 1
                emitProgress(0f)
            }

            if (failed.isEmpty()) {
                _uiState.value = AddItemUiState.Initial
                _effects.emit(AddItemEffect.UploadsSucceeded(count = total))
            } else {
                _uiState.value = AddItemUiState.Error(
                    message = "Some photos couldn't be uploaded. Please try again.",
                    failedPhotos = failed,
                )
            }
        }
    }

    /** The bytes to upload for [photo]: its already-rasterized crop, or the original untouched. */
    private suspend fun resolveUploadBytes(photo: ReviewPhoto): Pair<ByteArray, String> {
        val cropped = photo.croppedBytes ?: return photo.original.bytes() to photo.original.mimeType
        return cropped to "image/jpeg"
    }

    /**
     * Rasterizes [boundingBox] against [photo]'s original bytes. Falls back to null (i.e. upload
     * the original untouched) if cropping fails, so a decode error never blocks the upload.
     */
    private suspend fun computeCroppedBytes(photo: ReviewPhoto, boundingBox: BoundingBox): ByteArray? {
        return runCatching {
            cropToSquareJpeg(photo.original.bytes(), boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height)
        }.getOrElse {
            _effects.emit(AddItemEffect.CropFailed)
            null
        }
    }
}
