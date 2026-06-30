package com.komod.api.presentation.additem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komod.api.data.repository.AddItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddItemViewModel(
    private val addItemRepository: AddItemRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AddItemUiState>(AddItemUiState.Initial)
    val uiState: StateFlow<AddItemUiState> = _uiState.asStateFlow()

    private var lastImageBytes: ByteArray? = null
    private var lastMimeType: String = "image/jpeg"

    fun onImageSelected(bytes: ByteArray, mimeType: String) {
        lastImageBytes = bytes
        lastMimeType = mimeType
        processImage(bytes, mimeType)
    }

    fun retry() {
        val bytes = lastImageBytes ?: return
        processImage(bytes, lastMimeType)
    }

    fun reset() {
        _uiState.value = AddItemUiState.Initial
    }

    private fun processImage(bytes: ByteArray, mimeType: String) {
        viewModelScope.launch {
            // Step 1: Create image record — shown as upload in-progress (fast backend call)
            _uiState.value = AddItemUiState.Uploading(
                imageThumbnail = bytes,
                storagePath = "",
                overallProgress = 0f,
                steps = steps(
                    upload = StepStatus.InProgress,
                    analyze = StepStatus.Pending,
                    create = StepStatus.Pending,
                ),
            )

            runCatching {
                val imageInfo = addItemRepository.createImage()

                updateUploadingState {
                    copy(storagePath = imageInfo.storagePath)
                }

                // Step 2: Upload bytes to Supabase Storage
                addItemRepository.uploadImage(imageInfo.storagePath, bytes, mimeType) { fraction ->
                    updateUploadingState {
                        copy(
                            overallProgress = fraction.coerceIn(0f, 1f) * 0.5f,
                            steps = steps(
                                upload = StepStatus.InProgress,
                                analyze = StepStatus.Pending,
                                create = StepStatus.Pending,
                            ),
                        )
                    }
                }

                // Step 3: Trigger AI analysis (backend also creates the wardrobe item)
                updateUploadingState {
                    copy(
                        overallProgress = 0.5f,
                        steps = steps(
                            upload = StepStatus.Completed,
                            analyze = StepStatus.InProgress,
                            create = StepStatus.Pending,
                        ),
                    )
                }

                addItemRepository.analyzeWardrobeItems(imageInfo.imageId)

                // Step 4: Analysis complete — backend has created the wardrobe item
                updateUploadingState {
                    copy(
                        overallProgress = 1f,
                        steps = steps(
                            upload = StepStatus.Completed,
                            analyze = StepStatus.Completed,
                            create = StepStatus.Completed,
                        ),
                    )
                }

                _uiState.value = AddItemUiState.Success
            }.onFailure { throwable ->
                _uiState.value = AddItemUiState.Error(
                    message = throwable.message ?: "Something went wrong. Please try again.",
                    lastImageBytes = bytes,
                    lastMimeType = mimeType,
                )
            }
        }
    }

    private fun updateUploadingState(transform: AddItemUiState.Uploading.() -> AddItemUiState.Uploading) {
        val current = _uiState.value as? AddItemUiState.Uploading ?: return
        _uiState.value = current.transform()
    }

    private fun steps(
        upload: StepStatus,
        analyze: StepStatus,
        create: StepStatus,
    ): List<UploadStep> {
        return listOf(
            UploadStep(label = "Uploading to storage", status = upload),
            UploadStep(label = "Analyzing with AI", status = analyze),
            UploadStep(label = "Creating wardrobe item", status = create),
        )
    }
}
