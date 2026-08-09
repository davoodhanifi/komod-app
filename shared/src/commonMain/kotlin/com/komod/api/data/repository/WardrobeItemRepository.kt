package com.komod.api.data.repository

import com.komod.api.domain.model.BoundingBox
import com.komod.api.domain.model.WardrobeItemDetail
import com.komod.api.data.api.model.WardrobeItemUpdateRequest

interface WardrobeItemRepository {
    suspend fun getWardrobeItem(id: String): WardrobeItemDetail
    suspend fun getOriginalImageUrl(imageId: String): String?
    suspend fun createSignedUrl(storagePath: String): String?
    suspend fun setWardrobeItemFavorite(id: String, favorite: Boolean)
    suspend fun deleteWardrobeItem(id: String)
    suspend fun updateWardrobeItem(
        id: String,
        request: WardrobeItemUpdateRequest,
    ): WardrobeItemDetail
    /**
     * Downloads the full-resolution original image from [originalImageUrl], crops the
     * normalized [boundingBox] region out of it locally, and uploads the result as the
     * item's new cropped image. Never operates on a thumbnail or preview.
     */
    suspend fun uploadCroppedImage(
        id: String,
        originalImageUrl: String,
        boundingBox: BoundingBox,
    )
}
