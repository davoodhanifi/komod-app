package com.komod.api.data.api.model

/**
 * The storage path to use for this item's normal display thumbnail: the generated
 * catalog image (background removed, deterministically padded) when one exists,
 * falling back to the original uploaded photo only if no cropped image has been
 * produced yet. Never prefers the original over an available cropped image.
 */
fun WardrobeItemDto.preferredThumbnailStoragePath(): String? =
    croppedImageStoragePath?.takeIf { it.isNotBlank() }
        ?: originalImageStoragePath?.takeIf { it.isNotBlank() }
