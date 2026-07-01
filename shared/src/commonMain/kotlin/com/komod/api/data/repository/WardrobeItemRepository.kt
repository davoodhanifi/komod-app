package com.komod.api.data.repository

import com.komod.api.domain.model.WardrobeItemDetail

interface WardrobeItemRepository {
    suspend fun getWardrobeItem(id: String): WardrobeItemDetail
}
