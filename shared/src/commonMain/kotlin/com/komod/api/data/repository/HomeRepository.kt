package com.komod.api.data.repository

import com.komod.api.domain.model.RecentItem
import com.komod.api.domain.model.SavedOutfitsPage
import com.komod.api.domain.model.WardrobeSummary

interface HomeRepository {
    suspend fun getWardrobeSummary(): WardrobeSummary
    suspend fun getRecentItems(): List<RecentItem>

    // pageNumber/pageSize are opt-in together — omitting both returns every saved outfit
    // (hasNextPage = false) in one call, matching the pre-pagination behavior.
    suspend fun getSavedOutfitsPage(pageNumber: Int? = null, pageSize: Int? = null): SavedOutfitsPage
}
