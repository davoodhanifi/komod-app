package com.komod.api.presentation.additem

import com.komod.api.domain.model.BoundingBox
import com.komod.api.platform.PickedImage

/**
 * A photo picked for upload, tracked through the review/crop step. [cropBox] is null until the
 * user confirms a crop for this photo; a null box means the original bytes are uploaded as-is.
 * [croppedBytes] is rasterized as soon as the crop is confirmed (not deferred to upload time) so
 * the review grid can show the user exactly what will be uploaded.
 */
data class ReviewPhoto(
    val id: String,
    val original: PickedImage,
    val cropBox: BoundingBox? = null,
    val croppedBytes: ByteArray? = null,
) {
    val isCropped: Boolean get() = cropBox != null
}
