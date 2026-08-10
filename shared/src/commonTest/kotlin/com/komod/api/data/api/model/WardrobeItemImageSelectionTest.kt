package com.komod.api.data.api.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private fun testWardrobeItemDto(
    croppedImageStoragePath: String? = null,
    originalImageStoragePath: String? = null,
): WardrobeItemDto = WardrobeItemDto(
    id = "item-1",
    imageId = "image-1",
    status = 0,
    category = "top",
    confidence = 1.0,
    metadataJson = "{}",
    createdAt = "2026-08-10T00:00:00Z",
    croppedImageStoragePath = croppedImageStoragePath,
    originalImageStoragePath = originalImageStoragePath,
)

class WardrobeItemImageSelectionTest {
    @Test
    fun `prefers the cropped catalog image over the original photo when both exist`() {
        val dto = testWardrobeItemDto(
            croppedImageStoragePath = "cropped/item-1.jpg",
            originalImageStoragePath = "originals/image-1.jpg",
        )

        assertEquals("cropped/item-1.jpg", dto.preferredThumbnailStoragePath())
    }

    @Test
    fun `falls back to the original photo when no cropped image exists yet`() {
        val dto = testWardrobeItemDto(
            croppedImageStoragePath = null,
            originalImageStoragePath = "originals/image-1.jpg",
        )

        assertEquals("originals/image-1.jpg", dto.preferredThumbnailStoragePath())
    }

    @Test
    fun `treats a blank cropped path the same as a missing one`() {
        val dto = testWardrobeItemDto(
            croppedImageStoragePath = "",
            originalImageStoragePath = "originals/image-1.jpg",
        )

        assertEquals("originals/image-1.jpg", dto.preferredThumbnailStoragePath())
    }

    @Test
    fun `returns null when neither image is available`() {
        val dto = testWardrobeItemDto(croppedImageStoragePath = null, originalImageStoragePath = null)

        assertNull(dto.preferredThumbnailStoragePath())
    }
}
