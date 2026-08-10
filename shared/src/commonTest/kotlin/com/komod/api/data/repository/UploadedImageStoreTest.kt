package com.komod.api.data.repository

import com.komod.api.data.api.model.ImageStatus
import com.komod.api.domain.model.UploadedImage
import kotlin.test.Test
import kotlin.test.assertEquals

private fun uploadedImage(id: String, status: ImageStatus = ImageStatus.Pending) = UploadedImage(
    imageId = id,
    storagePath = "originals/$id.jpg",
    thumbnailStoragePath = "thumbs/$id.jpg",
    status = status,
)

class UploadedImageStoreTest {
    @Test
    fun `a newly added image is prepended, not appended`() {
        val store = UploadedImageStore()
        store.add(uploadedImage("img-1"))
        store.add(uploadedImage("img-2"))
        store.add(uploadedImage("img-3"))

        // Most recent upload first, so it's visible at the left of a long queue
        // without the user having to scroll to find it.
        assertEquals(listOf("img-3", "img-2", "img-1"), store.uploadedImages.value.map { it.imageId })
    }

    @Test
    fun `updating an existing image's status keeps its position`() {
        val store = UploadedImageStore()
        store.add(uploadedImage("img-1"))
        store.add(uploadedImage("img-2"))
        store.add(uploadedImage("img-3"))

        store.updateStatus("img-1", ImageStatus.Analyzed)

        assertEquals(listOf("img-3", "img-2", "img-1"), store.uploadedImages.value.map { it.imageId })
        assertEquals(ImageStatus.Analyzed, store.uploadedImages.value.first { it.imageId == "img-1" }.status)
    }

    @Test
    fun `a backend refresh mixing new and known images prepends only the new ones`() {
        val store = UploadedImageStore()
        store.add(uploadedImage("img-1"))
        store.add(uploadedImage("img-2"))

        // Simulates a GET /images/uploaded response: img-2 already tracked (status
        // changed), img-3 is brand new.
        store.upsertAll(
            listOf(
                uploadedImage("img-2", ImageStatus.Analyzed),
                uploadedImage("img-3"),
            ),
        )

        assertEquals(listOf("img-3", "img-2", "img-1"), store.uploadedImages.value.map { it.imageId })
        assertEquals(ImageStatus.Analyzed, store.uploadedImages.value.first { it.imageId == "img-2" }.status)
    }
}
