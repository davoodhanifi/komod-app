package com.komod.api.domain.model

/**
 * The in-progress crop adjustment for a single wardrobe item, tracking both the
 * AI-generated box and whatever the user has dragged/resized it to.
 */
data class CropEditorState(
    val originalBoundingBox: BoundingBox,
    val currentBoundingBox: BoundingBox,
) {
    val isModified: Boolean get() = currentBoundingBox != originalBoundingBox

    fun withBoundingBox(boundingBox: BoundingBox): CropEditorState = copy(currentBoundingBox = boundingBox)

    fun reset(): CropEditorState = copy(currentBoundingBox = originalBoundingBox)
}
