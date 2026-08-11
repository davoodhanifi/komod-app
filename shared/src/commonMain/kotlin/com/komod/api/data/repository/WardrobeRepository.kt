package com.komod.api.data.repository

import com.komod.api.domain.model.WardrobeItem

interface WardrobeRepository {
    suspend fun getWardrobeItems(): List<WardrobeItem>

    suspend fun deleteWardrobeItems(ids: List<String>)
}
