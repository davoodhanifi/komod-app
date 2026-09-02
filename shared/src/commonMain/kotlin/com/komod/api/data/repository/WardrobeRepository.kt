package com.komod.api.data.repository

import com.komod.api.domain.model.WardrobeItem
import com.komod.api.domain.model.WardrobeItemsPage

interface WardrobeRepository {
    // pageNumber/pageSize are opt-in together — omitting both returns the full wardrobe
    // (hasNextPage = false) in one call, matching the pre-pagination behavior.
    suspend fun getWardrobeItems(pageNumber: Int? = null, pageSize: Int? = null): WardrobeItemsPage

    suspend fun deleteWardrobeItems(ids: List<String>)
}
