package com.komod.api.data.api.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the actual wire shape the backend sends — a mismatch here (a field rename or an
 * enum encoded as an int instead of its name) would silently break every GET
 * /images/uploaded poll and the POST /wardrobe-items/analyze response, without any Kotlin
 * compiler error to catch it.
 */
class UploadedImageWireFormatTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `analyze response decodes the queued status as Processing`() {
        val body = """{"imageId":"11111111-1111-1111-1111-111111111111","status":"Processing"}"""

        val result = json.decodeFromString<AnalyzeWardrobeImageResultDto>(body)

        assertEquals("11111111-1111-1111-1111-111111111111", result.imageId)
        assertEquals(ImageStatus.Processing, result.status)
    }

    @Test
    fun `pending or processing uploaded image decodes with an empty items list`() {
        val body = """
            {
              "id": "img-1",
              "uploadedAt": "2026-08-10T00:00:00Z",
              "storagePath": "originals/img-1.jpg",
              "thumbnailStoragePath": "thumbs/img-1.jpg",
              "status": "Processing",
              "itemCount": 0,
              "items": []
            }
        """.trimIndent()

        val dto = json.decodeFromString<UploadedImageDto>(body)

        assertEquals("img-1", dto.imageId)
        assertEquals("originals/img-1.jpg", dto.originalImagePath)
        assertEquals(ImageStatus.Processing, dto.status)
        assertEquals(0, dto.extractedItemCount)
        assertTrue(dto.items.isEmpty())
    }

    @Test
    fun `pending uploaded image with no items field at all still decodes via the default`() {
        // The backend's own docs note Items "may be empty" for Pending/Processing/Failed —
        // this also covers the field being omitted entirely rather than sent as [].
        val body = """
            {
              "id": "img-2",
              "uploadedAt": "2026-08-10T00:00:00Z",
              "storagePath": "originals/img-2.jpg",
              "thumbnailStoragePath": "thumbs/img-2.jpg",
              "status": "Pending",
              "itemCount": 0
            }
        """.trimIndent()

        val dto = json.decodeFromString<UploadedImageDto>(body)

        assertEquals(ImageStatus.Pending, dto.status)
        assertTrue(dto.items.isEmpty())
    }

    @Test
    fun `failed uploaded image decodes with an empty items list`() {
        val body = """
            {
              "id": "img-3",
              "uploadedAt": "2026-08-10T00:00:00Z",
              "storagePath": "originals/img-3.jpg",
              "thumbnailStoragePath": "thumbs/img-3.jpg",
              "status": "Failed",
              "itemCount": 0,
              "items": []
            }
        """.trimIndent()

        val dto = json.decodeFromString<UploadedImageDto>(body)

        assertEquals(ImageStatus.Failed, dto.status)
        assertTrue(dto.items.isEmpty())
    }

    @Test
    fun `analyzed uploaded image decodes with its extracted items`() {
        val body = """
            {
              "id": "img-4",
              "uploadedAt": "2026-08-10T00:00:00Z",
              "storagePath": "originals/img-4.jpg",
              "thumbnailStoragePath": "thumbs/img-4.jpg",
              "status": "Analyzed",
              "itemCount": 1,
              "items": [
                {
                  "id": "item-1",
                  "imageId": "img-4",
                  "status": 0,
                  "category": "top",
                  "confidence": 0.92,
                  "metadataJson": "{}",
                  "createdAt": "2026-08-10T00:00:00Z",
                  "croppedImageStoragePath": "cropped/item-1.jpg",
                  "originalImageStoragePath": "originals/img-4.jpg"
                }
              ]
            }
        """.trimIndent()

        val dto = json.decodeFromString<UploadedImageDto>(body)

        assertEquals(ImageStatus.Analyzed, dto.status)
        assertEquals(1, dto.extractedItemCount)
        assertEquals(1, dto.items.size)
        assertEquals("cropped/item-1.jpg", dto.items.first().croppedImageStoragePath)
    }
}
