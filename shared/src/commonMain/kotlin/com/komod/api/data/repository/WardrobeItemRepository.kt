package com.komod.api.data.repository

import com.komod.api.domain.model.BoundingBox
import com.komod.api.domain.model.WardrobeItemDetail
import com.komod.api.data.api.model.WardrobeItemUpdateRequest

interface WardrobeItemRepository {
    suspend fun getWardrobeItem(id: String): WardrobeItemDetail
    suspend fun setWardrobeItemFavorite(id: String, favorite: Boolean)
    suspend fun deleteWardrobeItem(id: String)
    suspend fun updateWardrobeItem(
        id: String,
        request: WardrobeItemUpdateRequest,
    ): WardrobeItemDetail
    suspend fun updateWardrobeItemBoundingBox(
        id: String,
        boundingBox: BoundingBox,
    )
}
