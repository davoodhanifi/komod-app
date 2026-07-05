package com.komod.api.data.repository

import com.komod.api.domain.model.RecentItem
import com.komod.api.domain.model.WardrobeSummary

interface HomeRepository {
    suspend fun getWardrobeSummary(): WardrobeSummary
    suspend fun getRecentItems(): List<RecentItem>
}
